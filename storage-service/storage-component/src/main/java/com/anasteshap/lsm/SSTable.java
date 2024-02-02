package com.anasteshap.lsm;

import com.anasteshap.lsm.iterator.IteratorMerger;
import com.anasteshap.lsm.iterator.UniqueSortedIterator;
import com.anasteshap.lsm.memtable.MemTable;
import com.anasteshap.lsm.utils.KeyValuePair;
import com.anasteshap.lsm.utils.SparseIndex;
import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


@Getter
public class SSTable implements Iterable<KeyValuePair> {
    public static final String DATA_FILE_EXTENSION = ".data";
    public static final String INDEX_FILE_EXTENSION = ".index";

    private final String filename;
    private final String journalPath;
    private long size;
    private final Map<String, SparseIndex> sparseIndexes = new TreeMap<>();

    public SSTable(String filename, String journalPath) {
        this.filename = filename;
        this.journalPath = journalPath;
        readSparseIndexesFromFile();
    }

    public SSTable(String filename, String journalPath, MemTable memTable, int sampleSize) {
        this(filename, journalPath, memTable.getItems(), sampleSize);
    }

    public SSTable(String filename, String journalPath, List<KeyValuePair> items, int sampleSize) {
        this.filename = filename;
        this.journalPath = journalPath;
        this.size = 0;
        writeItems(items, sampleSize);
    }

    private void readSparseIndexesFromFile() {
        try (var reader = new BufferedReader(new FileReader(journalPath + INDEX_FILE_EXTENSION))) {
            var buffer = new char[1];
            var indexPart = new StringBuilder();
            List<String> index = new ArrayList<>();
            while ((reader.read(buffer)) != -1) {
                if (buffer[0] == ':') {
                    index.add(indexPart.toString());
                    indexPart = new StringBuilder();
                    continue;
                }

                if (buffer[0] == ';') {
                    if (index.size() == 3) {
                        long offset = Long.parseLong(index.get(1));
                        long segLen = Long.parseLong(index.get(2));
                        sparseIndexes.put(index.get(0), new SparseIndex(offset, segLen));
                    }
                    indexPart = new StringBuilder();
                    continue;
                }
                indexPart.append(buffer[0]);
            }

            index.add(indexPart.toString());
            if (index.size() == 3) {
                long offset = Long.parseLong(index.get(1));
                long segLen = Long.parseLong(index.get(2));
                sparseIndexes.put(index.get(0), new SparseIndex(offset, segLen));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeItems(List<KeyValuePair> items, int sampleSize) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Пустой список key-value пар при создании SSTable");
        }

        // sampleSize - размер выборки (макс кол-во записей в сегменте)
        try (var fos = new FileOutputStream(filename + DATA_FILE_EXTENSION, true);
             var gzipOutputStream = new GZIPOutputStream(fos);
             var writer = new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8)) {

            // итерация по сегментам
            var offset = 0L;
            var isFirst = true;
            for (int i = 0; i < items.size(); i += sampleSize) {
                var line = new StringBuilder();
                var limit = Math.min(i + sampleSize, items.size());

                // итерация по kvp из текущего сегмента
                int j = i;
                for (; j < limit; j++) {
                    var item = items.get(j);
                    var curItemLine = ";" + item.getKey() + ":" + item.getValue();
                    if (isFirst) {
                        curItemLine = curItemLine.substring(1);
                        isFirst = false;
                    }
//                    var curItemLine = item.getKey() + ":" + item.getValue() + ";";
                    line.append(curItemLine);
//                    bloomFilter.add(item.getKey());
                }

                writer.write(line.toString());

                // если это начало нового сегмента
                var data = line.toString().getBytes();
                if (size % sampleSize == 0) {
                    var item = items.get(i);
                    sparseIndexes.put(item.getKey(), new SparseIndex(offset, offset + data.length)); // size - кол-во предыдущих kvp (не включая kvp из текущего сегмента)
                }

                offset += data.length;
                size += j - i;
            }

            var iterator = items.iterator();
            if (!iterator.hasNext()) {
                throw new RuntimeException("Список KeyValuePair пустой");
            }

            isFirst = true;
            try (var journal = new FileOutputStream(journalPath + INDEX_FILE_EXTENSION, true)) {
//                var data = ";" + size + ":" + sparseIndexes.size(); // возможно не нужно
//                journal.write(data.getBytes());
                for (var entry : sparseIndexes.entrySet()) {
                    var line = ";" + entry.getKey() + ":" + entry.getValue().getOffset() + ":" + entry.getValue().getSegLen();
                    if (isFirst) {
                        line = line.substring(1);
                        isFirst = false;
                    }
                    journal.write(line.getBytes());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SSTable merge(String filename, String journalPath, int sampleSize, LinkedList<SSTable> tableLinkedList) {
        var iterableTables = tableLinkedList.toArray(new Iterable[]{});

        // возвращает соответствующий итератор для каждого элемента Iterable
        var itArray = Arrays.stream(iterableTables).map(Iterable::iterator).toArray(Iterator[]::new);

        var merger = new IteratorMerger<KeyValuePair>(itArray);
        var uniqueSortedIterator = new UniqueSortedIterator<>(merger);

        List<KeyValuePair> actualList = new ArrayList<>();
        while (uniqueSortedIterator.hasNext()) {
            actualList.add(uniqueSortedIterator.next());
        }
        return new SSTable(filename, journalPath, actualList, sampleSize);
    }

    // через итератор можно попробовать
    public String get(String key) {
//        if (!bloomFilter.mightContain(key))
//            return null;

        var sparseIndex = ((TreeMap<String, SparseIndex>) sparseIndexes).floorEntry(key);
        if (sparseIndex == null) return null;
        var offset = sparseIndex.getValue().getOffset();
        var segmentLen = sparseIndex.getValue().getSegLen();

        try (var fis = new FileInputStream(filename + DATA_FILE_EXTENSION);
             var gzipInputStream = new GZIPInputStream(fis)) {

//            gzipInputStream.skip(offset);
            var data = new byte[(int) segmentLen];
            var n = gzipInputStream.read(data, (int) offset, (int) segmentLen); // читаем сегмент данных

            if (n == -1) {
                throw new RuntimeException("SSTable::get - невозможно прочитать данные - конец файла");
            }

            var segment = new String(data);
            var keyValuePairs = Arrays.stream(segment.split(";")).filter(kvp -> !kvp.isEmpty()).toList();
            for (var kvp : keyValuePairs) {
                var pair = kvp.split(":");
                if (pair[0].equals(key)) {
                    return pair[1];
                }
            }

            throw new RuntimeException(String.format("В SSTable (filename: %s) ключ не найден", filename + DATA_FILE_EXTENSION));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeAndDelete() {
        for (var file : List.of(filename + DATA_FILE_EXTENSION, journalPath + INDEX_FILE_EXTENSION)) {
            if (!new File(file).delete()) {
                throw new RuntimeException("Problems with deleting SSTable. Filename: " + file);
            }
        }
    }

    @Override
    public Iterator<KeyValuePair> iterator() {
        return new SSTableIterator(this);
    }

    private static class SSTableIterator implements Iterator<KeyValuePair> {
        private long tableSize;
        private int count = 1;
        //        private final BufferedReader bufferedReader;
        private final InputStreamReader inputStreamReader;
//        private final byte[] bufferedReader;

        public SSTableIterator(SSTable table) {
//            this.tableSize = table.size;
            try {
                var fis = new FileInputStream(table.filename + SSTable.DATA_FILE_EXTENSION);
                var gzipInputStream = new GZIPInputStream(fis);

                var byteArrayOutputStream = new ByteArrayOutputStream();
                var buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                var byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                inputStreamReader = new InputStreamReader(byteArrayInputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            return count != -1;
//            return tableSize > 0L;
        }

        @Override
        public KeyValuePair next() {
            // чтение следующей пары key-value
            var pair = readNextLine().split(":");
            if (pair.length != 2) {
                throw new RuntimeException("Проблемы с чтением файла. Невозможно идентифицировать ключ и значение");
            }
//            tableSize--;
            return new KeyValuePair(pair[0], pair[1]);
        }

        private String readNextLine() {
            var buffer = new char[1];
            try {
                var stringBuilder = new StringBuilder();
                while ((count = inputStreamReader.read(buffer)) != -1) {
                    if (buffer[0] == ';') {
                        break;
                    }
                    stringBuilder.append(buffer[0]);
                }
                return stringBuilder.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

