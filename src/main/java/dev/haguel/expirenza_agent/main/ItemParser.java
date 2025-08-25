package dev.haguel.expirenza_agent.main;

import dev.haguel.expirenza_agent.exception.InvalidParseException;

public interface ItemParser<T, V> {
    V parse(T item) throws InvalidParseException;
}
