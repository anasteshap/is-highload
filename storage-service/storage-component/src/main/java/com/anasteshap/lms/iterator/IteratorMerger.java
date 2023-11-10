package com.anasteshap.lms.iterator;

import java.util.Iterator;
import java.util.PriorityQueue;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static java.util.Comparator.comparing;

public class IteratorMerger<T extends Comparable<T>> implements Iterator<T> {
    Iterator<T>[] iterators;
    PriorityQueue<Pair<T, Integer>> queue; // мин-куча

    @SafeVarargs
    public IteratorMerger(Iterator<T>... iterators) {
        // iterators - список итераторов, каждый из которых указывает на SSTable
        this.iterators = iterators;
        queue = new PriorityQueue<>(comparing((Pair<T, Integer> a) -> a.getLeft()).thenComparingInt(Pair::getRight));

        for (int i = 0; i < iterators.length; i++) {
            if (iterators[i].hasNext()) // если в SSTable есть элемент - добавляем один - самый первый
                queue.add(new ImmutablePair<>(iterators[i].next(), i));
        }
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public T next() {
        if (queue.isEmpty())
            return null;

        var top = queue.poll();

        var result = top.getLeft();
        int index = top.getRight();

        // итератор достиг конца, возвращается текущий элемент
        if (index == -1)
            return result;

        // итератор, который соответствует текущему элементу с наивысшим приоритетом
        T next = iterators[index].next();
        int newIndex = iterators[index].hasNext() ? index : -1;

        // добавляем следующий элемент в queue из SSTable, откуда только что был взят элемент
        queue.add(new ImmutablePair<>(next, newIndex));

        return result;
    }
}
