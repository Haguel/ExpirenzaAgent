package dev.haguel.expirenza_agent.main;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemProducer<T, V> {
    protected final T dataSource;

    public abstract V produce();
}
