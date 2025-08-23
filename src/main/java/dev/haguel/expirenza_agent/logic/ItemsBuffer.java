package dev.haguel.expirenza_agent.logic;

public interface ItemsBuffer<T> {
    void put(T item);
    T take();
}
