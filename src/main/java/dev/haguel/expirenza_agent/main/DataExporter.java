package dev.haguel.expirenza_agent.main;

public interface DataExporter<T> {
    void export(T data);
}
