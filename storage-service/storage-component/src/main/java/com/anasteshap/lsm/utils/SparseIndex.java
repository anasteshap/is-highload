package com.anasteshap.lsm.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SparseIndex {
    private long offset;
    private long segLen;
}
