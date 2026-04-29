# OS Final Project

## Team Members

- Jeremiah McDonald
- Persabella Sirak
- Christina Clements

## Overview

TryRight is an Operating Systems project that counts right triangles from a set of 2D points as efficiently as possible.

The program solves the same computational problem using multiple implementations so the results can be compared and evaluated for performance.

The project includes:

- Single-process execution
- Multithreaded execution
- Multiprocess execution
- Memory-mapped I/O for binary point files
- Inter-process communication through standard input and output
- Performance comparison across execution models

## Project Structure

```text
src/
  PointStore.java
  TextPointStore.java
  BinPointStore.java
  PointStoreFactory.java
  Point.java
  Dir.java
  RightTriangleCounter.java
  Triangles.java
  ThreadTriangles.java
  TriangleCounterTask.java
  ProcessTriangles.java
  BenchmarkTriangles.java
  MMIOReader.java

test/
  PointStoreTest.java
```

## Input Formats

The project supports two input formats.

### Text Files

Text files begin with the number of points, followed by one point per line.

Example:

```text
5
3 4
0 0
4 2
8 3
82 7
```

### Binary Files

Binary files store each point as two 4-byte integers:

```text
x y
```

Each point takes 8 bytes total.

Binary files are read using memory-mapped I/O.

## Running the Programs

Compile the project:

```bash
javac -d out/production src/*.java
```

Run the single-process version:

```bash
java -cp out/production com.tryright.Triangles points.txt
```

Run the multithreaded version:

```bash
java -cp out/production com.tryright.ThreadTriangles points.txt 4
```

Run the multiprocess version:

```bash
java -cp out/production com.tryright.ProcessTriangles points.txt 4
```

## Performance Benchmark

Based on feedback from the project review, we added a benchmark mode so users can compare the performance of each execution 
model and verify that each implementation produces the same triangle count.

The benchmark runs the same input file through:

- Single-process mode
- Multithreaded mode
- Multiprocess mode

It reports:

- Triangle count
- Number of workers
- Average runtime
- Best runtime
- Speedup compared to single-process execution
- Whether all modes produced the same result

Run the benchmark:

```bash
java -cp out/production com.tryright.BenchmarkTriangles points.txt 4 3
```

Arguments:

```text
points.txt = input file
4          = number of worker threads/processes
3          = number of benchmark trials
```

## Testing

The project includes tests for the `PointStore` implementations.

The tests verify:

- Correct text file loading
- Correct binary file loading
- Correct point counts
- Correct X and Y coordinate access
- Out-of-bounds index handling
- Invalid file format handling

## Notes

The main goal of this project is efficient right-triangle counting. The different execution models allow the same problem
to be solved in different ways, while the benchmark runner provides a clear way to compare performance and confirm that each
implementation produces the same result.

The single-process version provides the baseline result. The multithreaded version uses shared-memory parallelism. The
multiprocess version uses separate JVM processes and communicates through standard input and output.