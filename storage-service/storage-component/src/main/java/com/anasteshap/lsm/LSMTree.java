package com.anasteshap.lsm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

//@Component
public class LSMTree {
    static final int DEFAULT_MEMTABLE_MAX_SIZE = 1 << 3; // для бд - 1 << 10
    static final int DEFAULT_TABLE_LEVEL_MAX_SIZE = 5;
    static final int DEFAULT_SSTABLE_SAMPLE_SIZE = 1 << 10;
    static final String DEFAULT_DATA_DIRECTORY = "LSM_data";

    private final Object mutableMemTableLock = new Object();
    private final Object immutableMemTablesLock = new Object();
    private final Object tableLock = new Object();

    private final int mutableMemTableMaxSize;
    private final int tableLevelMaxSize;
    private final String directory;

    MemTable mutableMemtable;
    LinkedList<MemTable> immutableMemTables;
    List<LinkedList<SSTable>> tables; // список уровней, каждый из которых - список SSTables
    ExecutorService memTableFlusher;
    ExecutorService tableCompactor;

    public LSMTree() {
        this(DEFAULT_MEMTABLE_MAX_SIZE, DEFAULT_TABLE_LEVEL_MAX_SIZE, DEFAULT_DATA_DIRECTORY);
    }

    public LSMTree(String directory) {
        this(DEFAULT_MEMTABLE_MAX_SIZE, DEFAULT_TABLE_LEVEL_MAX_SIZE, directory);
    }

    public LSMTree(int mutableMemTableMaxSize, int tableLevelMaxSize, String directory) {
        this.mutableMemTableMaxSize = mutableMemTableMaxSize;
        this.tableLevelMaxSize = tableLevelMaxSize;
        this.directory = directory;
        try {
            Files.createDirectory(Path.of(directory));
        } catch (Exception e) {
            throw new RuntimeException("Не удается создать директорию для LSMTree", e);
        }
    }

    public void set(String key, String value) {
        synchronized (mutableMemTableLock) {
            mutableMemtable.add(key, value);
            checkMemTableSize();
        }
    }

//    public void delete(String key) {
//        synchronized (mutableMemTableLock) {
//            mutableMemTable.(key);
//            checkMemTableSize();
//        }
//    }

    public String get(String key) {
        String result;

        synchronized (mutableMemTableLock) {
            if ((result = mutableMemtable.get(key)) != null)
                return result;
        }

        synchronized (immutableMemTablesLock) {
            for (var memTable : immutableMemTables) {
                if ((result = memTable.get(key)) != null)
                    return result;
            }
        }

        synchronized (tableLock) {
            for (var level : tables) {
                for (var table : level) {
                    if ((result = table.get(key)) != null)
                        return result;
                }
            }
        }

        return null;
    }

    private void checkMemTableSize() {
        if (mutableMemtable.getCurrSize() <= mutableMemTableMaxSize)
            return;

        synchronized (immutableMemTablesLock) {
            immutableMemTables.addFirst(mutableMemtable);
            mutableMemtable = new MemTable(mutableMemTableMaxSize);
            memTableFlusher.execute(this::flushLastMemTable);
        }
    }

    private void flushLastMemTable() {
        MemTable memTableToFlush;

        synchronized (immutableMemTablesLock) {
            if (immutableMemTables.isEmpty())
                return;

            memTableToFlush = immutableMemTables.getLast();
        }

        var ssTableId = System.currentTimeMillis();
        String filename = String.format("%s/sst_%d", directory, ssTableId);
        String journalPath = String.format("%s/journal_%d", directory, ssTableId);

        synchronized (tableLock) {
            tables.get(0).addFirst(new SSTable(filename, journalPath, memTableToFlush, DEFAULT_SSTABLE_SAMPLE_SIZE));
            tableCompactor.execute(this::compactTables);
        }

        // удаляем flushed memTable из immutable memTables
        synchronized (immutableMemTablesLock) {
            immutableMemTables.removeLast();
        }
    }

    private void compactTables() {
        synchronized (tableLock) {
            var n = tables.size(); // кол-во уровней

            for (var i = 0; i < n; i++) {
                var level = tables.get(i);
                if (level.size() <= tableLevelMaxSize)
                    continue;

                var ssTableId = System.currentTimeMillis();
                String filename = String.format("%s/sst_%d", directory, ssTableId);
                String journalPath = String.format("%s/journal_%d", directory, ssTableId);

                var table = SSTable.merge(filename, journalPath, DEFAULT_SSTABLE_SAMPLE_SIZE, level);

                if (i == n - 1) { // добавление нового уровня, если это последний
                    tables.add(new LinkedList<>());
                }

                tables.get(i + 1).addFirst(table);
                level.forEach(SSTable::closeAndDelete); // удаление старых SSTables
                level.clear();
            }
        }
    }
}
