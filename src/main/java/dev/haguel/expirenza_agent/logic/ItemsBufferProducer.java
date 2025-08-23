package dev.haguel.expirenza_agent.logic;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemsBufferProducer<T> {
    private final ItemsBuffer<T> itemsBuffer;

    public abstract void produce();
}
