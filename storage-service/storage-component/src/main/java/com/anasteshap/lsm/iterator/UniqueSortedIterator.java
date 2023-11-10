package com.anasteshap.lsm.iterator;

import java.util.Iterator;

// пропускает дубликаты в отсортированном итераторе, сохраняя только первый
public class UniqueSortedIterator<T extends Comparable<T>> implements Iterator<T> {
    Iterator<T> iterator;
    private T last;

    public UniqueSortedIterator(Iterator<T> iterator) {
        this.iterator = iterator;
        last = iterator.next();
    }

    @Override
    public boolean hasNext() {
        return last != null;
    }

    @Override
    public T next() {
        T next = iterator.next();
        while (next != null && last.compareTo(next) == 0)
            next = iterator.next();

        T toReturn = last;
        last = next;

        return toReturn;
    }

}
