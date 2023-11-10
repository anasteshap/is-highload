package com.anasteshap.lms.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KeyValuePair implements Comparable<KeyValuePair> {
    private String key;
    private String value;

    @Override
    public int compareTo(KeyValuePair kvp) {
        return key.compareTo(kvp.getKey());
    }
}

