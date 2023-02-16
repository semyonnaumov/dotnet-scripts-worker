package com.naumov.dotnetscriptsworker.kafka;

public interface Reporter<T> {
    void report(T object);
}