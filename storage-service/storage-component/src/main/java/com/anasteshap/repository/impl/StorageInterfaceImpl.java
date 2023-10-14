package com.anasteshap.repository.impl;

import com.anasteshap.repository.StorageInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Repository
public class StorageInterfaceImpl implements StorageInterface {
    @Value("${data.file.filename}")
    private String filename;
    private final Map<String, String> data = new HashMap<>();

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public void set(String key, String value) {
        data.put(key, value);
        saveDataToFile();
    }

    private void loadDataFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename)) ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    data.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void saveDataToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (var entry: data.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
//            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
