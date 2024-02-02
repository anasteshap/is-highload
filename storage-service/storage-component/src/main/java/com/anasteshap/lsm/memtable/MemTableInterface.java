package com.anasteshap.lsm.memtable;

public interface MemTableInterface {
    void add(String key, String value);
    String get(String key);
}
