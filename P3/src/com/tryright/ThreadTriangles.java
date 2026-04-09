/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Multithreaded right triangle counter using PointStore.
 *
 * <p>This program computes the number of right triangles using multiple
 * threads that share memory within a single Java Virtual Machine.</p>
 *
 * <p>The PointStore abstraction is used to support both and
 * binary encoded point files while providing efficient indexed access
 * to point data.</p>
 *
 * @author Jeremiah McDonald
 */
public class ThreadTriangles {

    /**
     * Exit code for invalid arguments.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code for I/O errors.
     */
    private static final int EXIT_IO_ERROR = 2;

    /**
     * Exit code for file format errors.
     */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Program entry point.
     *
     * @param args command-line arguments:
     *             args[0] input file
     *             args[1] number of threads
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args, "args cannot be null");

        if (args.length != 2) {
            System.err.println("Error: expected 2 arguments, got " + args.length);
            printUsage();
            System.exit(EXIT_BAD_ARGS);
        }

        final File inputFile = new File(args[0]);

        if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
            System.err.println("Error: cannot read input file: " + inputFile);
            System.exit(EXIT_IO_ERROR);
        }

        final int threadCount;
        try {
            threadCount = parseThreadCount(args[1]);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;     // unreachable, but documents control flow
        }

        final PointStore store;
        try {
            store = PointStoreFactory.open(inputFile.getPath());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_FORMAT_ERROR);
            return;     // unreachable
        }

        try {
            final int totalPoints = store.numPoints();

            if (totalPoints == 0) {
                System.err.println("Error: input file contains no points");
                System.exit(EXIT_FORMAT_ERROR);
            }

            // Don't create more threads than pivot points
            final int actualThreads = Math.min(threadCount, totalPoints);

            // Each thread writes to its own slot, so result aggregation does not
            // require synchronization
            final long[] partialCounts = new long[actualThreads];
            final Thread[] threads = new Thread[actualThreads];

            // Split the pivot range into contiguous batches so each thread handles
            // a predictable slice of the search space
            final int batchSize = (totalPoints + actualThreads - 1) / actualThreads;

            int taskIndex = 0;

            for (int i = 0; i < actualThreads; i++) {
                final int start = i * batchSize;
                if (start >= totalPoints) {
                    break;
                }

                final int end = Math.min(start + batchSize, totalPoints);

                final TriangleCounterTask task =
                        new TriangleCounterTask(store, start, end, partialCounts, taskIndex);

                threads[taskIndex] = new Thread(task, "TriangleCounter-" + taskIndex);
                threads[taskIndex].start();
                taskIndex++;
            }

            // Join all threads and aggregate partial results
            long total = 0;

            for (int j = 0; j < taskIndex; j++) {
                try {
                    threads[j].join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Error: main thread interrupted while waiting for workers: "
                            + e.getMessage());
                    System.exit(EXIT_IO_ERROR);
                }
                total += partialCounts[j];
            }

            System.out.println(total);
        } finally {
            store.close();
        }
    }

    /**
     * Prints program usage instructions to the error stream.
     */
    private static void printUsage() {
        System.err.println("Usage: ThreadTriangles <input file> <num_threads>");
    }

    /**
     * Parses and validates the number of threads.
     *
     * @param s thread count string
     * @return parsed thread count
     * @throws IllegalArgumentException if the value is not an integer
     *                                  or less than 1
     */
    private static int parseThreadCount(final String s){
        Objects.requireNonNull(s, "thread count string cannot be null");

        final int threadCount;
        try {
            threadCount = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Thread count must be an integer: " + s,
                    e
            );
        }

        if (threadCount < 1) {
            throw new IllegalArgumentException(
                    "Thread count must be greater than zero: " + threadCount
            );
        }

        return threadCount;
    }
}
