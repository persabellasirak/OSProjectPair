/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 1
 * Class: CSC-4180 Operating Systems
 *
 *************************************/

package com.tryright;

import java.util.Objects;

/**
 * Represents an integer direction vector {@code (dx, dy)}.
 *
 * <p>This class is primarily used as a normalized direction (slope) key for
 * counting right triangles.</p>
 *
 * @author Jeremiah McDonald
 */
public final class Dir {

    /**
     * X-component of the direction vector.
     */
    public final int dx;

    /**
     * Y-component of the direction vector.
     */
    public final int dy;

    /**
     * Creates a direction vector.
     *
     * @param dx x-component of the direction vector
     * @param dy y-component of the direction vector
     */
    public Dir(final int dx, final int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Compares this direction to another object for equality.
     *
     * @param o object to compare against
     * @return {@code true} if the other object is a {@link Dir} with the same components
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Dir)) {
            return false;
        }

        final Dir other = (Dir) o;
        return dx == other.dx && dy == other.dy;
    }

    /**
     * Computes a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code for this direction vector
     */
    @Override
    public int hashCode() {
        return Objects.hash(dx, dy);
    }

    /**
     * Returns a readable string representation of this direction vector.
     *
     * @return formatted string representation
     */
    @Override
    public String toString() {
        return "Dir{dx=" + dx + ", dy=" + dy + "}";
    }
}
