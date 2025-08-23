package dev.haguel.expirenza_agent.logic;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemsBufferParser<T, V> {
    private final ItemsBuffer<T> itemsBuffer;

    public abstract V parse();
}
