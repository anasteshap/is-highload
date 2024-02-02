package com.anasteshap.lsm.memtable;

import com.anasteshap.lsm.utils.KeyValuePair;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MemTable implements MemTableInterface {
    private final Map<String, String> avlTree;
    @Getter
    private final long maxSize;
    @Getter
    private long currSize;

    public MemTable(long maxSize) {
        this(maxSize, new TreeMap<>());
    }

    public MemTable(long maxSize, Map<String, String> avlTree) {
        this.maxSize = maxSize;
        this.avlTree = avlTree;
        this.currSize = 0L;
        for (var entry : avlTree.entrySet()) {
            this.currSize += entry.getKey().length() * 2L + entry.getValue().length() * 2L;
        }
    }

//    public MemTable(long maxSize) {
//        this.maxSize = maxSize;
//    }

    public List<KeyValuePair> getItems() {
        return avlTree.entrySet().stream().map(item -> new KeyValuePair(item.getKey(), item.getValue())).toList();
    }

    public void add(String key, String value) {
        var valueSize = key.length() * 2L + value.length() * 2L; // Размер нового значения в байтах

        var oldValue = avlTree.get(key);
        var oldSize = oldValue != null ? key.length() * 2L + oldValue.length() * 2L : 0L;

        var newSize = currSize - oldSize + valueSize;
//        if (newSize > maxSize) {
//            throw new RuntimeException("Превышен размер MemTable");
//        }

        avlTree.put(key, value);
        currSize = newSize;
    }

    public String get(String key) {
        var val = avlTree.get(key);
//        if (val == null) {
//            throw new RuntimeException("Ключ не найден");
//        }
        return val;
    }

    public void clear() {
        avlTree.clear();
        currSize = 0;
    }
}
