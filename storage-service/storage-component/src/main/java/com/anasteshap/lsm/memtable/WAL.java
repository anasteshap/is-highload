package com.anasteshap.lsm.memtable;

import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

public class WAL implements MemTableInterface {
    public static final String WAL_FILE_EXTENSION = ".wal";
    @Getter
    private final MemTable memTable;
    @Getter
    private final String filepath;
//    private final OutputStreamWriter writer;
    private boolean isFirst = true;

    public WAL(MemTable memTable, String filepath) {
        this.memTable = memTable;
        this.filepath = filepath + File.separator + UUID.randomUUID();
    }

    @Override
    public void add(String key, String value) {
        saveKeyValueToDisk(key, value);
        memTable.add(key, value);
    }

    @Override
    public String get(String key) {
        return memTable.get(key);
    }

    private void saveKeyValueToDisk(String key, String value) {
        try (var fos = new FileOutputStream(this.filepath + WAL_FILE_EXTENSION, true);
             var gzipOutputStream = new GZIPOutputStream(fos);
             var writer = new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8)) {
            var curItemLine = ";" + key + ":" + value;
            if (isFirst) {
                curItemLine = curItemLine.substring(1);
                isFirst = false;
            }
            writer.write(curItemLine);
        } catch (Exception e) {
            throw new RuntimeException("Error with saving to WAL");
        }
    }
}
