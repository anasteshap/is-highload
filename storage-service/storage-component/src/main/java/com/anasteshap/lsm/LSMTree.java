package com.anasteshap.lsm;

import com.anasteshap.lsm.memtable.MemTable;
import com.anasteshap.lsm.memtable.MemTableInterface;
import com.anasteshap.lsm.memtable.WAL;
import com.anasteshap.lsm.utils.KeyValuePair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

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

    WAL mutableMemtable;
    LinkedList<WAL> immutableMemTables = new LinkedList<>();
    List<LinkedList<SSTable>> tables; // список уровней, каждый из которых - список SSTables
    ExecutorService memTableFlusher = Executors.newFixedThreadPool(2);
    ExecutorService tableCompactor = Executors.newFixedThreadPool(2);

    public LSMTree() {
        this(DEFAULT_MEMTABLE_MAX_SIZE, DEFAULT_TABLE_LEVEL_MAX_SIZE, DEFAULT_DATA_DIRECTORY);
    }

    public LSMTree(String directory) {
        this(DEFAULT_MEMTABLE_MAX_SIZE, DEFAULT_TABLE_LEVEL_MAX_SIZE, directory);
    }

    public LSMTree(int tableLevelMaxSize) {
        this(DEFAULT_MEMTABLE_MAX_SIZE, tableLevelMaxSize, DEFAULT_DATA_DIRECTORY);
    }

    public LSMTree(int mutableMemTableMaxSize, int tableLevelMaxSize, String directory) {
        this.mutableMemTableMaxSize = mutableMemTableMaxSize;
//        this.mutableMemtable = new MemTable(mutableMemTableMaxSize);
        this.tableLevelMaxSize = tableLevelMaxSize;
        this.directory = directory;
        this.tables = new ArrayList<>();
        Path path = Path.of(directory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                this.mutableMemtable = new WAL(new MemTable(mutableMemTableMaxSize), path.toString());
                this.tables.add(new LinkedList<>()); // в другое место
                System.out.println("Директория успешно создана.");
            } catch (Exception e) {
                throw new RuntimeException("Не удается создать директорию для LSMTree", e);
            }
        } else {
            System.out.println("Директория уже существует.");

            List<String> walFilePaths = new ArrayList<>();
            try {
                Files.walk(path, FOLLOW_LINKS)
                        .forEach(file -> {
                            if(file.toFile().isFile() && file.toFile().getPath().endsWith(WAL.WAL_FILE_EXTENSION)){
                                walFilePaths.add(file.toFile().getAbsolutePath());
                            }
                        });

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!walFilePaths.isEmpty()) {
                Path walPath = Path.of(walFilePaths.get(0));
                var data = getDataFromWAL(walPath);
                this.mutableMemtable = new WAL(new MemTable(mutableMemTableMaxSize, data), path.toString());
                memTableFlusher.execute(this::checkMemTableSize);
            } else {
                this.mutableMemtable = new WAL(new MemTable(mutableMemTableMaxSize), path.toString());
            }
            checkExistedSSTables(path);
            tableCompactor.execute(this::compactTables);
        }
    }

    private Map<String, String> getDataFromWAL(Path walPath) {
        Map<String, String> avlTree = new TreeMap<>();

        try (var fis = new FileInputStream(walPath.toString());
             var gzipInputStream = new GZIPInputStream(fis)) {

            if (gzipInputStream.available() == 0) {
                return avlTree;
            }

            var byteArrayOutputStream = new ByteArrayOutputStream();
            var buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            var byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            var inputStreamReader = new InputStreamReader(byteArrayInputStream);

            int charsRead = 0;
            var charBuffer = new char[1];
            var stringBuilder = new StringBuilder();
            while (charsRead != -1) {
                while ((charsRead = inputStreamReader.read(charBuffer)) != -1) {
                    if (charBuffer[0] == ';') {
                        break;
                    }
                    stringBuilder.append(charBuffer[0]);
                }
                var pair = stringBuilder.toString().split(":");
                if (pair.length != 2) {
                    throw new RuntimeException("Проблемы с чтением файла WAL. Невозможно идентифицировать ключ и значение");
                }
                avlTree.put(pair[0], pair[1]);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return avlTree;
    }

    private void checkExistedSSTables(Path path) {
        var dir = new File(String.valueOf(path));
        var subDirectories = dir.listFiles(File::isDirectory);
        if (subDirectories == null) {
            return;
        }

        var subDirectoriesList = new ArrayList<>(List.of(subDirectories));
        Collections.sort(subDirectoriesList);
        for (var subDir : subDirectoriesList) {
            System.out.println("+++++++++++++++++++++++++" + subDir.getName());
            Map<String, List<File>> fileMap = new HashMap<>();
            var files = subDir.listFiles(File::isFile);
            if (files == null) continue;

            // Фильтруем файлы по их расширению и группируем по префиксу
            for (var file : Arrays.stream(files).toList()) {
                var fileName = file.getName();
                var prefix = fileName.substring(fileName.indexOf('_'), fileName.lastIndexOf('.'));
                fileMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(file);
            }
            var newFileMap = fileMap.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 2)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();

            LinkedList<SSTable> newLevel = new LinkedList<>();
            for (var fileName : newFileMap) {
                var parentDir = String.format("%s%s%s%s", dir.getName(), File.separator, subDir.getName(), File.separator);
                newLevel.addFirst(new SSTable(parentDir + "sst" + fileName, parentDir + "journal" + fileName)); // или не абсолют
            }
            this.tables.add(newLevel);
        }
    }

    public void set(String key, String value) {
        synchronized (mutableMemTableLock) {
            mutableMemtable.add(key, value);
            checkMemTableSize();
        }
    }

    public String get(String key) {
        String result;

        try {
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
        } catch (Exception ignored) {
        }
        return null;
    }

    private void checkMemTableSize() {
//        if (mutableMemtable.getCurrSize() <= mutableMemTableMaxSize)
//            return;
        if (mutableMemtable.getMemTable().getCurrSize() < mutableMemTableMaxSize)
            return;

        synchronized (immutableMemTablesLock) {
            immutableMemTables.addFirst(mutableMemtable);
            mutableMemtable = new WAL(new MemTable(mutableMemTableMaxSize), Path.of(directory).toString());
            memTableFlusher.execute(this::flushLastMemTable); // помещает задачу в очередь на выполнение в пуле потоков
        }
    }

    private void flushLastMemTable() {
        WAL memTableToFlush;

        synchronized (immutableMemTablesLock) {
            if (immutableMemTables.isEmpty())
                return;

            memTableToFlush = immutableMemTables.getLast();
        }

        var ssTableId = System.currentTimeMillis();

        Path path = Path.of(String.format("%s%s%s", directory, File.separator, "level_0"));
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                System.out.println("Директория успешно создана.");
            } catch (Exception e) {
                throw new RuntimeException("Не удается создать директорию для уровня 0", e);
            }
        }

        String filename = String.format("%s%ssst_%d", path, File.separator, ssTableId);
        String journalPath = String.format("%s%sjournal_%d", path, File.separator, ssTableId);

        synchronized (tableLock) {
            tables.get(0).addFirst(new SSTable(filename, journalPath, memTableToFlush.getMemTable(), DEFAULT_SSTABLE_SAMPLE_SIZE));
            tableCompactor.execute(this::compactTables);
        }

        var walFileName = memTableToFlush.getFilepath() + WAL.WAL_FILE_EXTENSION;
        if (!new File(walFileName).delete()) {
            throw new RuntimeException("Problems with deleting SSTable. Filename: " + walFileName);
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
                if (level.size() < tableLevelMaxSize)
                    continue;

                var ssTableId = System.currentTimeMillis();

                Path path = Path.of(String.format("%s%s%s%d", directory, File.separator, "level_", i + 1));
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectory(path);
                        System.out.println("Директория успешно создана.");
                    } catch (Exception e) {
                        throw new RuntimeException("Не удается создать директорию для уровня", e);
                    }
                }

                String filename = String.format("%s%ssst_%d", path, File.separator, ssTableId);
                String journalPath = String.format("%s%sjournal_%d", path, File.separator, ssTableId);

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
