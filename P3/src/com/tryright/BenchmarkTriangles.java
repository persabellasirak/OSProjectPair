/***********************************************************************
 *
 * Authors: Jeremiah McDonald, Persabella Sirak, Christina Clements
 * Assignment: OS Project Part 3
 * Class: Operating Systems
 *
 ************************************************************************/

package com.tryright;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * Runs the triangle counting implementations and compares their output.
 *
 * <p>This benchmark executes the existing single-process, multithreaded,
 * and multiprocess programs as separate Java commands. Measuring each mode
 * this way matches how the programs are normally run from the command line.</p>
 *
 * <p>The benchmark reports average runtime, best runtime, speedup compared
 * to the single-process version, and whether all implementations produced
 * the same triangle count.</p>
 *
 * @author Jeremiah McDonald
 * @author Persabella Sirak
 * @author Christina Clements
 */

public class BenchmarkTriangles {

    /**
     * Number of required command-line arguments.
     */
    private static final int REQUIRED_ARG_COUNT = 2;

    /**
     * Number of command-line arguments when the optional trial count is supplied.
     */
    private static final int OPTIONAL_ARG_COUNT = 3;

    /**
     * Default number of benchmark trials for each execution mode.
     */
    private static final int DEFAULT_TRIALS = 3;

    /**
     * Worker count displayed for the single-process baseline.
     */
    private static final int SINGLE_PROCESS_WORKERS = 1;

    /**
     * Exit code for invalid command-line arguments.
     */
    private static final int EXIT_BAD_ARGS = 1;

    /**
     * Exit code for invalid or unreadable input files.
     */
    private static final int EXIT_INPUT_FILE_ERROR = 2;

    /**
     * Exit code for failures while executing a benchmark command.
     */
    private static final int EXIT_BENCHMARK_ERROR = 3;

    /**
     * Exit code for command output that cannot be parsed as a triangle count.
     */
    private static final int EXIT_OUTPUT_ERROR = 4;

    /**
     * Exit code used when the benchmark runner is interrupted.
     */
    private static final int EXIT_INTERRUPTED = 5;

    /**
     * Exit code used when implementations produce different triangle counts.
     */
    private static final int EXIT_VALIDATION_ERROR = 6;

    /**
     * Number of nanoseconds in one millisecond.
     */
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    /**
     * Number of milliseconds in one second.
     */
    private static final double MILLIS_PER_SECOND = 1_000.0;

    /**
     * Divider used to make the benchmark output easier to read.
     */
    private static final String DIVIDER =
            "------------------------------------------------------------------------------------";

    /**
     * Java executable used to launch each benchmark command.
     */
    private static final String JAVA_COMMAND = "java";

    /**
     * Command-line flag for supplying the Java class path.
     */
    private static final String CLASS_PATH_FLAG = "-cp";

    /**
     * Class name for the single-process implementation.
     */
    private static final String SINGLE_PROCESS_CLASS = "Triangles";

    /**
     * Class name for the multithreaded implementation.
     */
    private static final String THREAD_CLASS = "ThreadTriangles";

    /**
     * Class name for the multiprocess implementation.
     */
    private static final String PROCESS_CLASS = "ProcessTriangles";

    /**
     * Prevents construction of this utility class.
     */
    private BenchmarkTriangles() {
        throw new AssertionError("BenchmarkTriangles cannot be instantiated");
    }

    /**
     * Runs the benchmark from the command line.
     *
     * @param args command-line arguments in the form
     *             {@code <inputFile> <workers> [trials]}
     *
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args, "args cannot be null");

        if (args.length != REQUIRED_ARG_COUNT && args.length != OPTIONAL_ARG_COUNT) {
            System.err.println("Error: expected 2 or 3 arguments, got " + args.length);
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        final Path inputFile = new File(args[0]).toPath().toAbsolutePath().normalize();

        if (!Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
            System.err.println("Error: cannot read input file: " + inputFile);
            printUsage();
            System.exit(EXIT_INPUT_FILE_ERROR);
            return;
        }

        final int workers;
        final int trials;

        try {
            workers = parsePositiveInt(args[1], "workers");
            trials = args.length == OPTIONAL_ARG_COUNT
                    ? parsePositiveInt(args[2], "trials")
                    : DEFAULT_TRIALS;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(EXIT_BAD_ARGS);
            return;
        }

        try {
            final List<BenchmarkResult> results = runBenchmarks(inputFile, workers, trials);
            final boolean matchingCounts = printReport(inputFile, workers, trials, results);

            if (!matchingCounts) {
                System.exit(EXIT_VALIDATION_ERROR);
            }
        } catch (BenchmarkOutputException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_OUTPUT_ERROR);
        } catch (IOException e) {
            System.err.println("Error: benchmark execution failed: " + e.getMessage());
            System.exit(EXIT_BENCHMARK_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: benchmark interrupted while waiting for a command");
            System.exit(EXIT_INTERRUPTED);
        }
    }


    /**
     * Prints the expected command-line format.
     */
    private static void printUsage() {
        System.err.println("Usage: BenchmarkTriangles <inputFile> <workers> [trials]");
        System.err.println("Example: BenchmarkTriangles points.txt 4 3");
    }

    /**
     * Parses a command-line argument as a positive integer.
     *
     * @param value raw command-line value
     * @param name  name of the value being parsed
     * @return parsed positive integer
     * @throws IllegalArgumentException if the value is not a positive integer
     */
    private static int parsePositiveInt(final String value, final String name) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        final int parsed;

        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer: " + value, e);
        }

        if (parsed <= 0) {
            throw new IllegalArgumentException(name + " must be a positive integer: " + parsed);
        }

        return parsed;
    }

    /**
     * Executes each benchmark case and returns the summarized results.
     *
     * @param inputFile point file used for all benchmark modes
     * @param workers   number of workers for threaded and process modes
     * @param trials    number of times each mode is executed
     * @return summarized results for each benchmark mode
     * @throws IOException              if a benchmark command cannot be executed successfully
     * @throws InterruptedException     if interrupted while waiting for a benchmark command
     * @throws BenchmarkOutputException if a command does not print a valid triangle count
     */
    private static List<BenchmarkResult> runBenchmarks(final Path inputFile,
                                                       final int workers,
                                                       final int trials) throws IOException, InterruptedException, BenchmarkOutputException {
        final String classPath = System.getProperty("java.class.path");
        final String filename = inputFile.toString();

        final List<BenchmarkCase> benchmarkCases = buildBenchmarkCases(classPath, filename, workers);

        final List<BenchmarkResult> results = new ArrayList<>();

        for (BenchmarkCase benchmarkCase : benchmarkCases) {
            results.add(runBenchmarkCase(benchmarkCase, trials));
        }

        return results;
    }

    /**
     * Builds the set of benchmark commands to execute.
     *
     * @param classPath class path used by the current Java process
     * @param filename  input file passed to each triangle-counting program
     * @param workers   number of workers for threaded and process modes
     * @return benchmark cases for all supported execution modes
     */
    private static List<BenchmarkCase> buildBenchmarkCases(final String classPath,
                                                           final String filename,
                                                           final int workers) {
        Objects.requireNonNull(classPath, "classPath cannot be null");
        Objects.requireNonNull(filename, "filename cannot be null");

        final List<BenchmarkCase> benchmarkCases = new ArrayList<>();

        benchmarkCases.add(new BenchmarkCase(
                "Single process",
                SINGLE_PROCESS_WORKERS,
                buildJavaCommand(classPath, SINGLE_PROCESS_CLASS, filename)
        ));

        benchmarkCases.add(new BenchmarkCase(
                "Threads",
                workers,
                buildJavaCommand(classPath, THREAD_CLASS, filename, String.valueOf(workers))
        ));

        benchmarkCases.add(new BenchmarkCase(
                "Processes",
                workers,
                buildJavaCommand(classPath, PROCESS_CLASS, filename, String.valueOf(workers))
        ));

        return benchmarkCases;
    }

    /**
     * Builds a command that launches a Java class with the current class path.
     *
     * @param classPath   class path supplied to the child Java process
     * @param className   class name to execute
     * @param programArgs arguments passed to the executed class
     * @return command list ready for ProcessBuilder
     */
    private static List<String> buildJavaCommand(final String classPath,
                                                 final String className,
                                                 final String... programArgs) {
        Objects.requireNonNull(classPath, "classPath cannot be null");
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(programArgs, "programArgs cannot be null");

        final List<String> command = new ArrayList<>();

        command.add(JAVA_COMMAND);
        command.add(CLASS_PATH_FLAG);
        command.add(classPath);
        command.add(className);

        for (String argument : programArgs) {
            command.add(argument);
        }

        return command;
    }

    /**
     * Runs one benchmark case for the requested number of trials.
     *
     * @param benchmarkCase benchmark command and display information
     * @param trials        number of times the command is executed
     * @return summarized runtime and count information
     * @throws IOException              if the command exits unsuccessfully
     * @throws InterruptedException     if interrupted while waiting for the command
     * @throws BenchmarkOutputException if the command prints invalid output
     */
    private static BenchmarkResult runBenchmarkCase(final BenchmarkCase benchmarkCase,
                                                    final int trials) throws IOException, InterruptedException, BenchmarkOutputException {
        Objects.requireNonNull(benchmarkCase, "benchmarkCase cannot be null");

        Long expectedCount = null;
        final List<Double> timesMillis = new ArrayList<>();

        for (int i = 0; i < trials; i++) {
            final TimedCommandResult commandResult = runCommand(benchmarkCase.command);

            if (expectedCount == null) {
                expectedCount = commandResult.count;
            } else if (expectedCount.longValue() != commandResult.count) {
                throw new BenchmarkOutputException(benchmarkCase.name + " produced inconsistent counts across trials");
            }

            timesMillis.add(commandResult.elapsedMillis);
        }

        return new BenchmarkResult(benchmarkCase.name, benchmarkCase.workers,
                expectedCount.longValue(), average(timesMillis), best(timesMillis));
    }

    /**
     * Runs one command and captures its elapsed time and final triangle count.
     *
     * @param command command to execute
     * @return parsed triangle count and elapsed runtime
     * @throws IOException              if the command cannot start or exits unsuccessfully
     * @throws InterruptedException     if interrupted while waiting for the command
     * @throws BenchmarkOutputException if the command output has no valid count
     */
    private static TimedCommandResult runCommand(final List<String> command) throws
            IOException, InterruptedException, BenchmarkOutputException {

        Objects.requireNonNull(command, "command cannot be null");

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        final long startTime = System.nanoTime();
        final Process process = processBuilder.start();

        final List<String> outputLines = readProcessOutput(process);
        final int exitCode = process.waitFor();

        final long endTime = System.nanoTime();

        if (exitCode != 0) {
            throw new IOException("command exited with code " + exitCode + ": "
                    + formatCommand(command) + System.lineSeparator() + formatOutput(outputLines));
        }

        final long count = parseLastLong(outputLines);
        final double elapsedMillis = (endTime - startTime) / NANOS_PER_MILLISECOND;

        return new TimedCommandResult(count, elapsedMillis);
    }

    /**
     * Reads all output produced by a benchmark command.
     *
     * @param process process whose combined output is read
     * @return captured output lines
     * @throws IOException if process output cannot be read
     */
    private static List<String> readProcessOutput(final Process process) throws IOException {
        Objects.requireNonNull(process, "process cannot be null");

        final List<String> outputLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine();

            while (line != null) {
                outputLines.add(line);
                line = reader.readLine();
            }
        }

        return outputLines;
    }

    /**
     * Finds the final long value printed by a benchmark command.
     *
     * @param outputLines command output lines
     * @return final long value printed by the command
     * @throws BenchmarkOutputException if no valid long value appears in the output
     */
    private static long parseLastLong(final List<String> outputLines) throws BenchmarkOutputException {

        Objects.requireNonNull(outputLines, "outputLines cannot be null");

        for (int i = outputLines.size() - 1; i >= 0; i--) {
            final String line = outputLines.get(i).trim();

            if (isLongLiteral(line)) {
                try {
                    return Long.parseLong(line);
                } catch (NumberFormatException e) {
                    throw new BenchmarkOutputException("triangle count is too large: " + line, e);
                }
            }
        }

        throw new BenchmarkOutputException(
                "no triangle count found in command output" + System.lineSeparator() + formatOutput(outputLines));
    }

    /**
     * Determines whether a string can represent a long integer literal.
     *
     * @param value string to inspect
     * @return true if the value contains only an optional minus sign and digits
     */
    private static boolean isLongLiteral(final String value) {
        Objects.requireNonNull(value, "value cannot be null");

        if (value.isEmpty()) {
            return false;
        }

        int startIndex = 0;

        if (value.charAt(0) == '-') {
            startIndex = 1;
        }

        if (startIndex == value.length()) {
            return false;
        }

        for (int i = startIndex; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Prints a formatted benchmark report.
     *
     * @param inputFile file used for the benchmark
     * @param workers   requested worker count
     * @param trials    number of executions per mode
     * @param results   summarized benchmark results
     * @return true if all modes reported the same triangle count
     */
    private static boolean printReport(final Path inputFile,
                                       final int workers,
                                       final int trials,
                                       final List<BenchmarkResult> results) {
        Objects.requireNonNull(inputFile, "inputFile cannot be null");
        Objects.requireNonNull(results, "results cannot be null");

        final long expectedCount = results.get(0).count;
        final double baselineMillis = results.get(0).averageMillis;

        boolean allMatch = true;

        printDivider();

        System.out.println("Triangle Counter Performance Benchmark");
        printDivider();
        System.out.println("Input file : " + inputFile);
        System.out.println("Workers    : " + workers);
        System.out.println("Trials     : " + trials);
        System.out.println("Note       : Times measure full command runtime for each mode.");
        printDivider();

        System.out.printf(
                Locale.US,
                "%-18s %8s %16s %14s %14s %10s %8s%n",
                "Mode",
                "Workers",
                "Count",
                "Avg Time",
                "Best Time",
                "Speedup",
                "Status"
        );

        printDivider();

        for (BenchmarkResult result : results) {
            final boolean matchesExpected = result.count == expectedCount;
            final double speedup = baselineMillis / result.averageMillis;

            allMatch = allMatch && matchesExpected;

            System.out.printf(
                    Locale.US,
                    "%-18s %8s %16s %14s %14s %10s %8s%n",
                    result.name,
                    result.workers,
                    result.count,
                    formatDuration(result.averageMillis),
                    formatDuration(result.bestMillis),
                    formatSpeedup(speedup),
                    matchesExpected ? "PASS" : "FAIL"
            );
        }

        printDivider();

        if (allMatch) {
            System.out.println("Validation: PASS - all modes produced the same triangle count.");
        } else {
            System.out.println("Validation: FAIL - at least one mode produced a different count.");
        }

        printDivider();

        return allMatch;
    }

    /**
     * Formats a duration in milliseconds for display.
     *
     * @param millis duration in milliseconds
     * @return formatted duration using milliseconds or seconds
     */
    private static String formatDuration(final double millis) {
        if (millis >= MILLIS_PER_SECOND) {
            return String.format(Locale.US, "%.3f s", millis / MILLIS_PER_SECOND);
        }

        return String.format(Locale.US, "%.2f ms", millis);
    }

    /**
     * Formats a speedup ratio for display.
     *
     * @param speedup ratio relative to the single-process baseline
     * @return formatted speedup value
     */
    private static String formatSpeedup(final double speedup) {
        return String.format(Locale.US, "%.2fx", speedup);
    }

    /**
     * Computes the arithmetic mean of the provided values.
     *
     * @param values values to average
     * @return arithmetic mean
     */
    private static double average(final List<Double> values) {
        Objects.requireNonNull(values, "values cannot be null");

        double total = 0.0;

        for (Double value : values) {
            total += value;
        }

        return total / values.size();
    }

    /**
     * Finds the smallest value in a list.
     *
     * @param values values to inspect
     * @return smallest value
     */
    private static double best(final List<Double> values) {
        Objects.requireNonNull(values, "values cannot be null");

        double bestValue = Double.MAX_VALUE;

        for (Double value : values) {
            bestValue = Math.min(bestValue, value);
        }

        return bestValue;
    }

    /**
     * Converts a command list into a readable command string.
     *
     * @param command command arguments to format
     * @return readable command string
     */
    private static String formatCommand(final List<String> command) {
        Objects.requireNonNull(command, "command cannot be null");
        return String.join(" ", command);
    }

    /**
     * Converts output lines into a readable block of text.
     *
     * @param outputLines output lines to format
     * @return readable output block
     */
    private static String formatOutput(final List<String> outputLines) {
        Objects.requireNonNull(outputLines, "outputLines cannot be null");

        if (outputLines.isEmpty()) {
            return "<no output>";
        }

        return String.join(System.lineSeparator(), outputLines);
    }

    /**
     * Prints the standard benchmark divider line.
     */
    private static void printDivider() {
        System.out.println(DIVIDER);
    }

    /**
     * Represents one benchmark command and its display information.
     */
    private static final class BenchmarkCase {

        /**
         * Display name for the benchmark mode.
         */
        private final String name;

        /**
         * Worker count associated with this benchmark mode.
         */
        private final int workers;

        /**
         * Command used to execute this benchmark mode.
         */
        private final List<String> command;

        /**
         * Creates a benchmark case.
         *
         * @param name    display name for the benchmark mode
         * @param workers worker count associated with the mode
         * @param command command used to execute the mode
         */
        private BenchmarkCase(final String name, final int workers, final List<String> command) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.workers = workers;
            this.command = Objects.requireNonNull(command, "command cannot be null");
        }
    }

    /**
     * Stores the output from one timed command execution.
     */
    private static final class TimedCommandResult {

        /**
         * Triangle count printed by the command.
         */
        private final long count;

        /**
         * Elapsed command runtime in milliseconds.
         */
        private final double elapsedMillis;

        /**
         * Creates a timed command result.
         *
         * @param count         triangle count printed by the command
         * @param elapsedMillis elapsed command runtime in milliseconds
         */
        private TimedCommandResult(final long count, final double elapsedMillis) {
            this.count = count;
            this.elapsedMillis = elapsedMillis;
        }
    }

    /**
     * Stores the summarized result for one benchmark mode.
     */
    private static final class BenchmarkResult {

        /**
         * Display name for the benchmark mode.
         */
        private final String name;

        /**
         * Worker count associated with the benchmark mode.
         */
        private final int workers;

        /**
         * Triangle count produced by the benchmark mode.
         */
        private final long count;

        /**
         * Average runtime across all trials in milliseconds.
         */
        private final double averageMillis;

        /**
         * Fastest runtime across all trials in milliseconds.
         */
        private final double bestMillis;

        private BenchmarkResult(final String name, final int workers, final long count, final double averageMillis, final double bestMillis) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.workers = workers;
            this.count = count;
            this.averageMillis = averageMillis;
            this.bestMillis = bestMillis;
        }
    }

    private static final class BenchmarkOutputException extends Exception {

        /**
         * Creates an exception with a detailed message.
         *
         * @param message explanation of the output failure
         */
        private BenchmarkOutputException(final String message) {
            super(message);
        }

        /**
         * Creates an exception with a detailed message and cause.
         *
         * @param message explanation of the output failure
         * @param cause   original exception that caused the failure
         */
        private BenchmarkOutputException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
