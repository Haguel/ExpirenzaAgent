package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.main.ItemProducer;

import java.util.concurrent.LinkedBlockingQueue;

public class ExpirenzaMenuURLProducer extends ItemProducer<LinkedBlockingQueue<String>, String> {
    public ExpirenzaMenuURLProducer() {
        super(new LinkedBlockingQueue<>());
        dataSource.add("https://expz.menu/edf7ccc5-db00-45f1-b425-e3d1e5ff556a");
        dataSource.add("https://expz.menu/a10b7226-4923-4fdf-853a-ab10e7886871");
        dataSource.add("https://expz.menu/6f72f6db-e19b-42a9-94f2-3de5b4680ed0");
        dataSource.add("https://expz.menu/dcb797dd-7b3e-4120-b4a4-f874d23390ff");
    }

    @Override
    public String produce() {
        return dataSource.poll();
    }
}
