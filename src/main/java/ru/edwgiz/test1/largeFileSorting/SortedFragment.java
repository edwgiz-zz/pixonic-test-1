package ru.edwgiz.test1.largeFileSorting;

import java.io.BufferedReader;
import java.io.InputStream;


final class SortedFragment {

    /**
     * Current line from {@link #reader}
     */
    String value;
    /**
     * Source of values
     */
    InputStream inputStream;
    BufferedReader reader;

    @Override
    public String toString() {
        return value;
    }
}
