package dev.haguel.expirenza_agent.main;

public interface ItemsBuffer<T> {
    void put(T item);
    T take();
}
