/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.tryright;

import java.io.IOException;

/**
 * Factory for creating PointStore implementations based on input file type.
 *
 * <p>This class centralizes the policy for selecting an appropriate
 * PointStore implementation, allowing storage decisions to remain
 * decoupled from computational logic.</p>
 *
 * @author Jeremiah McDonald
 */
public final class PointStoreFactory {

    /**
     * Prevent instantiation of this utility class.
     *
     * <p>All functionality is provided via static methods.</p>
     */
    private PointStoreFactory() {
        throw new AssertionError("PointStoreFactory cannot be instantiated");
    }

    /**
     * Create an appropriate PointStore implementation based on the file name.
     *
     * @param filename path to the point file
     * @return a PointStore implementation capable of accessing the file
     * @throws IOException if filename cannot be opened or validated
     */
    public static PointStore open(final String filename) throws IOException {
        if (filename == null) {
            throw new IOException("Filename cannot be null");
        }

        // Files ending in .dat are treated as binary point files
        if (filename.endsWith(".dat")) {
            return new BinPointStore(filename);
        }

        // All other files are assumed to be text-encoded point lists
        return new TextPointStore(filename);
    }
}
