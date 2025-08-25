package dev.haguel.expirenza_agent.main;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemsBufferParser<T, V> {
    protected final ItemsBuffer<T> itemsBuffer;

    public abstract V parseItemsFromBuffer();
}
