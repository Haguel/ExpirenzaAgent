package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.main.ItemProducer;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ExpirenzaMenuURLProducer extends ItemProducer<LinkedBlockingQueue<String>, String> {
    public ExpirenzaMenuURLProducer() {
        super(new LinkedBlockingQueue<>());
        dataSource.add("https://expz.menu/edf7ccc5-db00-45f1-b425-e3d1e5ff556a");
        dataSource.add("https://expz.menu/a10b7226-4923-4fdf-853a-ab10e7886871");
    }

    @Override
    public String produce() {
        return dataSource.poll();
    }
}
