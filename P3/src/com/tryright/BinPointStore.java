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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * PointStore implementation for binary-encoded point files
 * accessed via memory-mapped I/O.
 *
 * <p>Each point is stored as a pair of 4-byte big-endian integers:
 * X followed by Y.</p>
 *
 * <p>The file must contain complete (x,y) pairs, and its size must be
 * a multiple of 8 bytes.</p>
 *
 * @author Jeremiah McDonald
 */
public class BinPointStore implements PointStore {

    /**
     * Number of bytes per integer.
     */
    private static final int INT_BYTES = 4;

    /**
     * Number of bytes per point (x, y).
     */
    private static final int POINT_BYTES = 8;

    /**
     * Memory-mapped buffer for file contents.
     */
    private final MappedByteBuffer mappedByteBuffer;

    /**
     * File channel backing the mapped buffer.
     */
    private final FileChannel fileChannel;

    /**
     * RandomAccessFile backing the channel.
     */
    private final RandomAccessFile randomAccessFile;

    /**
     * Total number of points in the file.
     */
    private final int pointCount;

    /**
     * Constructs a BinPointStore backed by a binary file.
     *
     * @param filename path to the binary-encoded point file
     * @throws IOException if filename is null, unreadable, malformed, or cannot be mapped
     */
    public BinPointStore(final String filename) throws IOException {
        if (filename == null) {
            throw new IOException("Filename cannot be null");
        }

        final File file = new File(filename);

        if (!file.exists()) {
            throw new IOException("File does not exist: " + filename);
        }
        if (!file.isFile()) {
            throw new IOException("Not a file: " + filename);
        }
        if (!file.canRead()) {
            throw new IOException("File cannot be read: " + filename);
        }

        RandomAccessFile raf = null;
        FileChannel channel = null;

        try {
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();

            final long fileSize = channel.size();

            // Each point is stored as two 4-byte integers (x, y)
            // If the file size is not divisible by 8 bytes, the file
            // cannot contain a complete set of point pairs, and is malformed
            if (fileSize % POINT_BYTES != 0) {
                throw new IOException(
                        "Invalid binary point file: size not multiple of " + POINT_BYTES + " bytes");
            }

            if (fileSize > Integer.MAX_VALUE) {
                throw new IOException("File too large to memory-map");
            }

            this.pointCount = (int) (fileSize / POINT_BYTES);

            // Map the file directly into memory instead of reading it into
            // Java objects. The operating system manages paging and caching,
            // allowing very fast random access to large datasets without
            // copying the file contents into the JVM heap
            this.mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            this.fileChannel = channel;
            this.randomAccessFile = raf;

            // Ensure partially initialized resources are closed
        } catch (IOException e) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException suppressed) {
                    e.addSuppressed(suppressed);
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException suppressed) {
                    e.addSuppressed(suppressed);
                }
            }
            throw e;
        }
    }

    /**
     * Returns the X coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return X coordinate
     * @throws IndexOutOfBoundsException if idx is invalid
     */
    @Override
    public int getX(final int idx) {
        checkIndex(idx);

        // Each point occupies 8 bytes. The X coordinate begins at
        // the start of the point's record: offset = index * 8
        final int position = idx * POINT_BYTES;
        return mappedByteBuffer.getInt(position);
    }

    /**
     * Returns the Y coordinate of the point at the specified index.
     *
     * @param idx index of the point
     * @return Y coordinate
     * @throws IndexOutOfBoundsException if idx is invalid
     */
    @Override
    public int getY(final int idx) {
        checkIndex(idx);

        // Y coordinate immediately follows X in the record.
        // Offset = (index * 8) + 4 bytes
        final int position = idx * POINT_BYTES + INT_BYTES;
        return mappedByteBuffer.getInt(position);
    }

    /**
     * Returns the number of points stored in the file.
     *
     * @return number of points available
     */
    @Override
    public int numPoints() {
        return pointCount;
    }

    /**
     * Releases system resources associated with this store.
     *
     * @throws RuntimeException if an I/O error occurs while closing resources
     */
    @Override
    public void close() {
        IOException closeError = null;

        try {
            fileChannel.close();
        } catch (IOException e) {
            closeError = e;
        }

        try {
            randomAccessFile.close();
        } catch (IOException e) {
            if (closeError == null) {
                closeError = e;
            } else {
                closeError.addSuppressed(e);
            }
        }

        if (closeError != null) {
            throw new RuntimeException("Error closing binary point store", closeError);
        }
    }

    /**
     * Validates that the index is within the valid range.
     *
     * @param idx index to validate
     * @throws IndexOutOfBoundsException if index is out of range
     */
    private void checkIndex(final int idx) {
        if (idx < 0 || idx >= pointCount) {
            throw new IndexOutOfBoundsException(
                    "Index " + idx + " out of bounds for " + pointCount + " points");
        }
    }
}
