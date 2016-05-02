package ru.edwgiz.test1.largeFileSorting;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.*;

/**
 * Splits source unsorted file to several sorted files,
 * and merge them to one sorted destination file.
 */
public class Main {

    private static final int BATCH_SIZE = 100000;
    private static final int FILE_BUFFER_SIZE = 128 * 1024;
    private static final Charset CHARSET = ISO_8859_1;

    public static void main(String[] args) throws URISyntaxException, IOException {
        Path src = Paths.get(Main.class.getResource("raw.txt").toURI());
        Path dst = src.resolveSibling("sorted.txt");
        sort(src, dst);
    }


    private static void sort(Path src, Path dst) throws IOException {
        List<Path> sortedPaths = toSortedFragments(src, dst);
        try {
            List<SortedFragment> allSortedFragments = new ArrayList<>();
            try {
                PriorityQueue<SortedFragment> sortedFragments = new PriorityQueue<>(
                        sortedPaths.size(), new SortedFragmentComparator());
                for (Path sortedFragmentPath : sortedPaths) {
                    SortedFragment e = new SortedFragment();
                    allSortedFragments.add(e);
                    e.inputStream = Files.newInputStream(sortedFragmentPath);
                    e.reader = new BufferedReader(new InputStreamReader(
                            e.inputStream, CHARSET), FILE_BUFFER_SIZE);
                    nextValue(e);
                    if (e.value != null) {
                        sortedFragments.add(e);
                    }
                }
                write(sortedFragments, dst);
            } finally {
                for (SortedFragment f : allSortedFragments) {
                    if (f.inputStream != null) {
                        close(f);
                    }
                }
            }
        } finally {
            for (Path sortedPath : sortedPaths) {
                try {
                    Files.delete(sortedPath);
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    private static void write(PriorityQueue<SortedFragment> sortedFragments, Path dst) throws IOException {
        OutputStream os = Files.newOutputStream(dst, CREATE, TRUNCATE_EXISTING, WRITE);
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(os, FILE_BUFFER_SIZE), CHARSET);
            while (sortedFragments.size() > 0) {
                SortedFragment e = sortedFragments.poll();
                writer.write(e.value);
                writer.write('\n');
                writer.write('\r');
                nextValue(e);
                if (e.value != null) {
                    sortedFragments.add(e);
                }
            }
            writer.flush();
        } finally {
            closeQuietly(os);
        }
    }

    private static void closeQuietly(Closeable os) {
        try {
            os.close();
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    private static void nextValue(SortedFragment e) throws IOException {
        e.value = e.reader.readLine();
        if (e.value == null) {
            close(e);
        }
    }

    private static void close(SortedFragment e) {
        closeQuietly(e.inputStream);
        e.reader = null;
        e.inputStream = null;
    }

    private static List<Path> toSortedFragments(Path src, Path dst) throws IOException {
        List<Path> sortedFragmentPaths = new ArrayList<>();
        InputStream is = Files.newInputStream(src);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, CHARSET), FILE_BUFFER_SIZE);
            List<String> values = new ArrayList<>(BATCH_SIZE); // reused value buffer
            for (; ; ) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                values.add(line);
                if (values.size() > BATCH_SIZE) {
                    writeSortedFragment(values, dst, sortedFragmentPaths);
                    values.clear();
                }
            }
            if(values.size() > 0) {
                writeSortedFragment(values, dst, sortedFragmentPaths);
            }
        } finally {
            closeQuietly(is);
        }
        return sortedFragmentPaths;
    }

    private static void writeSortedFragment(List<String> values, Path dst, List<Path> sortedFragmentPaths) throws IOException {
        Collections.sort(values);

        Path sortedFragmentPath = dst.resolveSibling(sortedFragmentPaths.size() + "_" + dst.getName(dst.getNameCount() - 1));
        sortedFragmentPaths.add(sortedFragmentPath);

        Files.write(sortedFragmentPath, values, CHARSET, CREATE, TRUNCATE_EXISTING, WRITE);
    }

}
