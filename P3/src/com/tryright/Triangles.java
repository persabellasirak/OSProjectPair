/*************************************
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *************************************/

package com.tryright;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Entry point for single-process right triangle counting.
 *
 * <p>This class program operates in two modes:</p>
 * <ul>
 *     <li><b>Normal mode:</b> {@code <filename>}</li>
 *     <li><b>Child mode:</b> {@code --child <startInclusive> <endExclusive>}</li>
 * </ul>
 *
 * <p>In normal mode, points are loaded through the {@link PointStore}
 * abstraction and the total number of right triangles is printed.</p>
 *
 * <p>In child mode, points are read from {@code stdin} and only a subset
 * of pivot indices are evaluated. This mode is intended for use by
 * {@code ProcessTriangles}.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author Jeremiah McDonald
 */
public final class Triangles {

    /**
     * Exit code for invalid command-line arguments.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code for invalid point file format.
     */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Exit code for invalid child execution parameters.
     */
    private static final int EXIT_CHILD_ERROR = 4;

    /**
     * Prevents instantiation.
     */
    private Triangles() {
        // Utility class
    }

    /**
     * Prints the normal mode usage information to {@code stderr}.
     */
    private static void printUsage(){

        System.err.println("Usage: <filename>");
    }

    /**
     * Prints child mode usage information to {@code stderr}.
     */
    private static void printChildUsage(){
        System.err.println("Usage: --child <startInclusive> <endExclusive>");
    }

    /**
     * Program entry point.
     *
     * <p>Normal mode expects one parameter: the input filename.</p>
     *
     * <p>Child mode expects three parameters:</p>
     * <ul>
     *     <li>{@code --child}</li>
     *     <li>{@code startInclusive}</li>
     *     <li>{@code endExclusive}</li>
     * </ul>
     *
     * @param args command-line arguments (must not be {@code null})
     */
    public static void main(final String[] args) {

        Objects.requireNonNull(args, "args cannot be null");

        // Child mode detection
        if (args.length >= 1 && "--child".equals(args[0])) {
            runChild(args);
            return;
        }

        // Normal mode requires exactly one argument
        if (args.length != 1) {
            System.err.println("Error: expected exactly 1 parameter.");
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        final String filename = args[0];
        final PointStore store;

        // Open point store
        try {
            store = PointStoreFactory.open(filename);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_FORMAT_ERROR);
            return;
        }

        // Perform computation
        try {
            final long answer =
                    RightTriangleCounter.countRightTriangles(store);
            System.out.println(answer);
        } finally {
            store.close();
        }
    }

    /**
     * Executes child mode.
     *
     * <p>Reads points from {@code stdin}, evaluates pivot indices in the range
     * {@code [startInclusive, endExclusive)}, and prints the partial result.</p>
     *
     * <p>Points are stored in memory and accessed via the PointStore interface.</p>
     *
     * @param args expected format:
     *             {@code --child <startInclusive> <endExclusive>}
     */
    private static void runChild(final String[] args) {

        if (args.length != 3) {
            System.err.println("Error: invalid child parameters.");
            printChildUsage();
            System.exit(EXIT_CHILD_ERROR);
            return;
        }

        final int start;
        final int end;

        try {
            start = Integer.parseInt(args[1]);
            end = Integer.parseInt(args[2]);
        } catch (final NumberFormatException e) {
            System.err.println("Error: child range must be integers.");
            printChildUsage();
            System.exit(EXIT_CHILD_ERROR);
            return;
        }

        final ArrayList<Integer> xs = new ArrayList<>();
        final ArrayList<Integer> ys = new ArrayList<>();

        try (Scanner scanner = new Scanner(System.in, "US-ASCII")) {

            while (scanner.hasNext()) {
                if (!scanner.hasNextInt()) {
                    throw new IllegalArgumentException("Invalid input: expected integer");
                }
                int x = scanner.nextInt();

                if (!scanner.hasNextInt()) {
                    throw new IllegalArgumentException("Invalid input: expected integer");
                }
                int y = scanner.nextInt();

                xs.add(x);
                ys.add(y);
            }
        } catch (final IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_FORMAT_ERROR);
            return;
        }

        final PointStore store = new InMemoryPointStore(xs, ys);

        try {
            final long partial =
                    RightTriangleCounter.countRightTriangles(store, start, end);
            System.out.println(partial);
        } catch (final IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_CHILD_ERROR);
        }
    }

    /**
     * Lightweight in-memory {@link PointStore} implementation used by
     * child process execution.
     *
     * <p>This avoids temporary files and allows point data received via
     * stdin to be processed through the PointStore abstraction.</p>
     */
    private static final class InMemoryPointStore implements PointStore {

        private final int[] xs;
        private final int[] ys;

        InMemoryPointStore(List<Integer> xsList, List<Integer> ysList) {
            xs = new int[xsList.size()];
            ys = new int[ysList.size()];

            // Copy values into arrays to reduce memory overhead
            for (int i = 0; i < xs.length; i++) {
                xs[i] = xsList.get(i);
                ys[i] = ysList.get(i);
            }
        }

        @Override
        public int getX(int idx) {
            return xs[idx];
        }

        @Override
        public int getY(int idx) {
            return ys[idx];
        }

        @Override
        public int numPoints() {
            return xs.length;
        }

        @Override
        public void close() {
            // nothing to release
        }
    }
}

