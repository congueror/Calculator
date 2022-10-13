package net.congueror.calculator.structure;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtendedList<E> extends ArrayList<E> {

    public ExtendedList() {
    }

    public ExtendedList(Collection<? extends E> c) {
        super(c);
    }

    public E getOr(int index, E orValue) {
        if (this.size() > index && this.get(index) != null)
            return this.get(index);
        return orValue;
    }

    public void removeRange(int index, int amount) {
        for (int i = 0; i < amount; i++) {
            this.remove(index);
        }
    }

    public void removeIndices(List<Integer> indexes) {
        int[] ints = indexes.stream().mapToInt(Integer::intValue).toArray();
        removeIndices(ints);
    }

    public void removeIndices(int... indexes) {
        Arrays.sort(indexes);
        for (int j = indexes.length - 1; j >= 0; j--) {
            int i = indexes[j];
            this.remove(i);
        }
    }

    public Stream<E> get(Predicate<E> predicate) {
        return this.stream().filter(predicate);
    }

    public void forI(Consumer<Integer> action) {
        for (int i = 0; i < this.size(); i++)
            action.accept(i);
    }

    public static <T> Collector<T, ?, ExtendedList<T>> toList() {
        return Collectors.toCollection(ExtendedList::new);
    }
}