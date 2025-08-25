package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.main.ItemsBuffer;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class ExpirenzaMenuURLBuffer implements ItemsBuffer<String> {
    private final BlockingQueue<String> queue;

    public ExpirenzaMenuURLBuffer(BlockingDeque<String> collection) {
        this.queue = collection;
    }

    @Override
    public void put(String item) {
        queue.add(item);
    }

    @Override
    public String take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            System.out.println("EspirenzaMenuURLBuffer interrupted");
        }

        return null;
    }
}
