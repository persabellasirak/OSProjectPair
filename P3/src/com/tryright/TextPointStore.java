/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * PointStore implementation for text-encoded point files.
 *
 * <p>The first non-empty line contains the number of points in the file.
 * Each following non-empty line must contain a pair of integers representing
 * the x and y coordinates of a point.</p>
 *
 * <p>Points are stored internally in arrays to allow fast indexed access
 * during triangle counting.</p>
 *
 * @author Jeremiah McDonald
 */
public final class TextPointStore implements PointStore {

    /**
     * X-coordinates of all points in file order.
     */
    private final int[] xs;

    /**
     * Y-coordinates of points in file order.
     */
    private final int[] ys;

    /**
     * Constructs a TextPointStore backed by a text point file.
     *
     * <p>The file format requires the first non-empty line to contain the
     * number of points in the file. Each subsequent non-empty line must
     * contain two integer representing the x and y coordinates of a point.</p>
     *
     * <p>All parsing and validation is performed during construction.
     * Points are stored internally in arrays to allow efficient indexed
     * access during triangle counting.</p>
     *
     * <p>This implementation loads all points into memory to provide
     * fast indexed access during triangle counting.</p>
     *
     * @param filename path to the text-encoded point file
     * @throws IOException if filename is null, unreadable, or contains malformed data
     */
    public TextPointStore(final String filename) throws IOException {
        if (filename == null) {
            throw new IOException("Filename cannot be null");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            int pointCount = -1;

            /*
             * Read the first non-empty line as the declared number of points.
             */
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                try {
                    pointCount = Integer.parseInt(trimmed);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid integer in point at line " + lineNumber, e);
                }

                if (pointCount < 0) {
                    throw new IOException("Negative point count at line " + lineNumber);
                }

                break;
            }

            if (pointCount < 0) {
                throw new IOException("Missing point count");
            }

            xs = new int[pointCount];
            ys = new int[pointCount];

            int index = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (index >= pointCount) {
                    throw new IOException("Too many points in file");
                }

                final String[] tokens = trimmed.split("\\s+");
                if (tokens.length != 2) {
                    throw new IOException(
                            "Invalid point at line " + lineNumber + ": expected 2 integers"
                    );
                }

                try {
                    xs[index] = Integer.parseInt(tokens[0]);
                    ys[index] = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Invalid integer in point at line " + lineNumber,
                            e
                    );
                }

                index++;
            }

            if (index != pointCount) {
                throw new IOException("Point count mismatch: expected " + pointCount + " but found " + index);
            }
        }
    }

    /**
     * Returns the X coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return X coordinate
     * @throws IndexOutOfBoundsException if idx is outside valid range
     */
    @Override
    public int getX(final int idx) {
        checkIndex(idx);
        return xs[idx];
    }

    /**
     * Returns the Y coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return Y coordinate
     * @throws IndexOutOfBoundsException if idx is outside valid range
     */
    @Override
    public int getY(final int idx) {
        checkIndex(idx);
        return ys[idx];
    }

    /**
     * Returns the number of points stored in the data store.
     *
     * @return number of stored points
     */
    @Override
    public int numPoints() {
        return xs.length;
    }

    /**
     * Releases any system resources associated with this store.
     *
     * <p>No resources need to be released for text implementation.</p>
     */
    @Override
    public void close() {
        // nothing to close for text implementation
    }

    /**
     * Validates that the index is within the valid range.
     *
     * @param idx index to validate
     * @throws IndexOutOfBoundsException if index is out of range
     */
    private void checkIndex(final int idx) {
        if (idx < 0 || idx >= xs.length) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds");
        }
    }
}
