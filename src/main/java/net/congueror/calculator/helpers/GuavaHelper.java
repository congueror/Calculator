package net.congueror.calculator.helpers;

import com.google.common.collect.HashMultiset;

import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class GuavaHelper {

    private GuavaHelper() {
    }

    @SafeVarargs
    public static <T> HashMultiset<T> ofMultiset(T... objs) {
        return HashMultiset.create(Arrays.stream(objs).toList());
    }

    public static <T> Collector<T, ?, HashMultiset<T>> toHashMultiset() {
        return Collectors.toCollection(HashMultiset::create);
    }
}
