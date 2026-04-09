/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 *************************************/

package com.tryright;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Multiprocess right triangle counter using {@link ProcessBuilder}
 * and pipes for Inter-Process Communication (IPC).
 *
 * <p>This program loads points using the {@link PointStore} abstraction,
 * partitions the pivot indices across multiple child JVM processes,
 * and streams the point list to each child process through stdin.</p>
 *
 * <p>Each child process executes {@link Triangles} in child mode
 * using the arguments {@code --child <startInclusive> <endExclusive>.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author Jeremiah McDonald
 */
public final class ProcessTriangles {

    /**
     * Exit code for invalid command-line parameters.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code for I/O-related failures.
     */
    private static final int EXIT_IO_ERROR = 2;

    /**
     * Exit code for invalid input format.
      */
    private static final int EXIT_FORMAT_ERROR = 3;

    /**
     * Prevents instantiation.
     */
    private ProcessTriangles() {
        // Utility class
    }

    /**
     * Prints usage information to {@code stderr}.
     */
    private static void printUsage() {

        System.err.println("Usage: <filename> <numProcesses>");
    }

    /**
     * Parses and validates the number of child processes.
     *
     * @param arg string representation of process count
     * @return validated positive process count
     * @throws IllegalArgumentException if the argument is invalid
     */
    private static int parseProcessCount(final String arg) {
        final int count;

        try {
            count = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("numProcesses must be an integer.", e);
        }

        if (count <= 0) {
            throw new IllegalArgumentException("numProcesses must be a positive integer.");
        }

        return count;
    }

    /**
     * Partitions pivot indices {@code [0, total)} into contiguous ranges.
     *
     * <p>Each returned row represents a half-open interval
     * {@code [startInclusive, endExclusive)}.</p>
     *
     * @param total total number of pivot indices
     * @param numProcesses number of processes
     * @return a 2D array of index ranges
     */
    private static int[][] partitionIndices(int total, int numProcesses) {

        final int[][] ranges = new int[numProcesses][2];

        // Compute ceiling(total / numProcesses) to evenly distribute work
        final int boxSize = (total + numProcesses - 1) / numProcesses;

        for (int i = 0; i < numProcesses; i++) {
            int start = i * boxSize;
            int end = Math.min(start + boxSize, total);
            ranges[i][0] = start;
            ranges[i][1] = end;
        }
        return ranges;
    }

    /**
     * Program entry point.
     *
     * @param args command-line arguments:
     *             {@code <filename> <numProcesses>}
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args, "args cannot be null.");

        if (args.length != 2) {
            System.err.println("Error: expected exactly 2 parameters.");
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        final int numProcesses;

        try{
            numProcesses = parseProcessCount(args[1]);
        } catch (IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        PointStore store = null;

        try {
            // Load points through the PointStore
            store = PointStoreFactory.open(args[0]);

            // Partition the pivot indices across child processes
            final int[][] ranges = partitionIndices(store.numPoints(), numProcesses);

            // Maintain references to child processes and output readers
            final List<Process> processes = new ArrayList<>();
            final List<BufferedReader> readers = new ArrayList<>();

            // Launch child JVM processes
            for (int i = 0; i < numProcesses; i++) {

                final int start = ranges[i][0];
                final int end = ranges[i][1];

                // Skip empty ranges to avoid spawning unnecessary processes
                if (start >= end) {
                    continue;
                }

                final String classPath = System.getProperty("java.class.path");

                /*
                 * Launch a child JVM running Triangles in child mode.
                 *
                 * Each child receives:
                 *  --child <startInclusive> <endExclusive>
                 *
                 * The parent streams the full point list through stdin.
                 * The child reads this list and computes triangles only
                 * for the pivot index range it was assigned
                 */
                final ProcessBuilder processBuilder = new ProcessBuilder(
                        "java",
                        "-cp",
                        classPath,
                        "com.tryright.Triangles",
                        "--child",
                        String.valueOf(start),
                        String.valueOf(end)
                );

                // Forward child stderr to parent stderr for visibility
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

                final Process process = processBuilder.start();
                processes.add(process);

                // Send full point list to child process via stdin
                try (PrintWriter writer =
                             new PrintWriter(new OutputStreamWriter(process.getOutputStream()))) {

                    // Stream the entire point list to the child process
                    for (int j = 0; j < store.numPoints(); j++) {
                        writer.println(store.getX(j) + " " + store.getY(j));
                    }

                    // Ensure data is flushed to the child before closing the pipe
                    writer.flush();
                }

                // Capture child's stdout to read the partial triangle count
                readers.add(new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                ));
            }

            long total = 0;

            // Read triangle counts returned by each child process
            for (BufferedReader reader : readers) {

                final String line = reader.readLine();

                if (line == null) {
                    throw new IOException("Child process produced no output.");
                }

                try {
                    total += Long.parseLong(line.trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Child process returned invalid count: " + line, e);
                }
            }

            // Wait for all child processes to finish execution
            for (Process p : processes) {
                final int exitCode = p.waitFor();
                if (exitCode != 0) {
                    throw new IOException("Child process exited with code " + exitCode);
                }
            }

            // Print final triangle count to stdout
            System.out.println(total);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_IO_ERROR);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_FORMAT_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: interrupted while waiting for child process");
            System.exit(EXIT_IO_ERROR);
        } finally {
            if (store != null) {
                store.close();
            }
        }
    }
}
