package dev.haguel.expirenza_agent.main;

import dev.haguel.expirenza_agent.exception.InvalidParseException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ItemsBufferParser<T, V> {
    protected final ItemsBuffer<T> itemsBuffer;

    public abstract V parseItemFromBuffer() throws InvalidParseException;
}
