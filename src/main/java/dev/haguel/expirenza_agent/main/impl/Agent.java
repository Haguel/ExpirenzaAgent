package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.entity.Restaurant;
import dev.haguel.expirenza_agent.exception.InvalidParseException;
import dev.haguel.expirenza_agent.main.*;
import dev.haguel.expirenza_agent.utils.ExecutorUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class Agent implements Scheduler {
    private final DataExporter<Restaurant> exporter;
    private final ItemParser<String, Restaurant> itemParser;

    private ItemProducer<LinkedBlockingQueue<String>, String> itemProducer;

    private BlockingQueue<String> urlsQueue;
    private BlockingQueue<Restaurant> restaurantsQueue;

    private ExecutorService producerExecutor;
    private ExecutorService parserExecutor;
    private ExecutorService exporterExecutor;

    private void setupExecutors() {
        int singleThreadProcessesCount = 1;
        int processesCount = 3;

        int availableThreadsCount = Runtime.getRuntime().availableProcessors() - singleThreadProcessesCount;
        int threadsPerProcess = (int) Math.ceil((double) availableThreadsCount / processesCount);

        producerExecutor = Executors.newSingleThreadExecutor();
        parserExecutor = Executors.newFixedThreadPool(threadsPerProcess * 2); // needs more threads
        exporterExecutor = Executors.newFixedThreadPool(threadsPerProcess);

        ExecutorUtil.addShutdownHook(producerExecutor);
        ExecutorUtil.addShutdownHook(parserExecutor);
        ExecutorUtil.addShutdownHook(exporterExecutor);
    }

    @PostConstruct
    public void init() {
        setupExecutors();
        urlsQueue = new LinkedBlockingQueue<>();
        restaurantsQueue = new LinkedBlockingQueue<>();
    }

    @Override
    @Scheduled(fixedRate = 1000 * 60 * 3)
    public void schedule() {
        itemProducer = new ExpirenzaMenuURLProducer();
        producerExecutor.submit(this::produce);
    }

    private void produce() {
        String url = itemProducer.produce();
        if(url != null) {
            try {
                urlsQueue.put(url);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            parserExecutor.submit(this::parse);

            produce();
        }
    }

    private void parse() {
        try {
            String url = urlsQueue.take();
            Restaurant restaurant = itemParser.parse(url);
            restaurantsQueue.put(restaurant);
        } catch (InvalidParseException e) {
            System.err.println("An error occurred during the parsing of the restaurant: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        exporterExecutor.submit(this::export);
    }

    private void export() {
        try {
            Restaurant restaurant = restaurantsQueue.take();
            exporter.export(restaurant);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
