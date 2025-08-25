package dev.haguel.expirenza_agent.main;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemsBufferProducer<T> {
    protected final ItemsBuffer<T> itemsBuffer;

    public abstract void produceItemsToBuffer();
}
