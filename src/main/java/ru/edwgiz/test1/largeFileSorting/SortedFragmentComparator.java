package ru.edwgiz.test1.largeFileSorting;

import java.util.Comparator;

/**
 * Compares by {@link SortedFragment#value} ascending.
 */
final class SortedFragmentComparator implements Comparator<SortedFragment> {

    @Override
    public int compare(SortedFragment o1, SortedFragment o2) {
        return o1.value.compareTo(o2.value);
    }
}
