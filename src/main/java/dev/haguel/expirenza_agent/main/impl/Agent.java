package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.entity.Dish;
import dev.haguel.expirenza_agent.exception.InvalidParseException;
import dev.haguel.expirenza_agent.main.*;
import dev.haguel.expirenza_agent.utils.ExecutorUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class Agent implements Scheduler {
    private final DataExporter<List<Dish>> exporter;
    private final ItemsBufferProducer<String> itemsBufferProducer;
    private final ItemsBufferParser<String, List<Dish>> itemsBufferParser;

    private ExecutorService producerExecutor;
    private ExecutorService parserExecutor;
    private ExecutorService exporterExecutor;

    private void setupExecutors() {
        int singleThreadProcessesCount = 1;
        int processesCount = 3;

        int availableThreadsCount = Runtime.getRuntime().availableProcessors() - singleThreadProcessesCount;
        int threadsPerProcess = (int) Math.ceil((double) availableThreadsCount / processesCount);

        producerExecutor = Executors.newSingleThreadExecutor();
        parserExecutor = Executors.newFixedThreadPool(threadsPerProcess + 1); // needs more threads
        exporterExecutor = Executors.newFixedThreadPool(threadsPerProcess);

        ExecutorUtil.addShutdownHook(producerExecutor);
        ExecutorUtil.addShutdownHook(parserExecutor);
        ExecutorUtil.addShutdownHook(exporterExecutor);
    }

    @PostConstruct
    public void init() {
        setupExecutors();
    }

    @Override
    @Scheduled(fixedRate = 1000 * 60 * 3)
    public void schedule() {
        CompletableFuture
                .runAsync(itemsBufferProducer::produceItemsToBuffer, producerExecutor)
                .thenApplyAsync((Void) -> {
                    try {
                        return itemsBufferParser.parseItemsFromBuffer();
                    } catch (InvalidParseException e) {
                        throw new RuntimeException(e);
                    }
                }, parserExecutor)
                .thenAcceptAsync(exporter::export, exporterExecutor);
    }
}
