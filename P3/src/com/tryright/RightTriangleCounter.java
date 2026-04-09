/*************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 3
 * Class: CSC-4180 Operating Systems
 *
 ************************************/

package com.tryright;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for counting right triangles from point data.
 *
 * <p>A triangle is counted when, for some pivot point P, there exist
 * two distinct points Q and R such that the vectors PQ and PR are perpendicular.</p>
 *
 * <p>This implementation uses normalized direction vectors to efficiently
 * detect perpendicular relationships.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @author Jeremiah McDonald
 */
public final class RightTriangleCounter {

    /**
     * Prevents instantiation.
     */
    private RightTriangleCounter() {
        // Intentionally empty
    }

    /**
     * Counts the total number of right triangles using a {@link PointStore}.
     *
     * <p>Each point is treated as a potential right-angle vertex.</p>
     *
     * <p>This method delegates to the range-based overload.</p>
     *
     * @param store point storage abstraction (must not be {@code null})
     * @return total number of right triangles
     * @throws NullPointerException if {@code store} is {@code null}
     */
    public static long countRightTriangles(final PointStore store) {
        Objects.requireNonNull(store, "store cannot be null.");
        return countRightTriangles(store, 0, store.numPoints());
    }

    /**
     * Counts right triangles using a {@link PointStore}, restricting the
     * pivot index to the range {@code [startInclusive, endExclusive)}.
     *
     * @param store point storage abstraction (must not be {@code null})
     * @param startInclusive first pivot index (inclusive)
     * @param endExclusive last pivot index (exclusive)
     * @return number of right triangles in the specified pivot range
     * @throws NullPointerException if {@code store} is {@code null}
     * @throws IllegalArgumentException if the pivot index range is invalid
     */
    public static long countRightTriangles(final PointStore store,
                                           final int startInclusive,
                                           final int endExclusive) {

        Objects.requireNonNull(store, "store cannot be null");

        if (startInclusive < 0 ||
                endExclusive < startInclusive ||
                endExclusive > store.numPoints()) {
            throw new IllegalArgumentException("invalid pivot index range.");
        }

        long total = 0;

        // Treat each point as a pivot (right-angle vertex) and count
        // perpendicular direction pairs formed with other points
        for (int i = startInclusive; i < endExclusive; i++) {

            final int px = store.getX(i);
            final int py = store.getY(i);

            // HashMap provides o(1) average lookup time for direction counts,
            // enabling efficient detection of perpendicular direction pairs.
            final Map<Dir, Integer> counts = new HashMap<>();

            // Compute direction vectors from pivot to every other point
            for (int j = 0; j < store.numPoints(); j++) {
                if (i == j) {
                    continue;
                }

                final int dx = store.getX(j) - px;
                final int dy = store.getY(j) - py;

                // Normalize direction so collinear points share identical vectors.
                final Dir d = normalize(dx, dy);
                counts.put(d, counts.getOrDefault(d, 0) + 1);
            }

            long local = 0;

            // For each direction, count pairs formed with perpendicular directions
            for (Map.Entry<Dir, Integer> entry : counts.entrySet()) {

                final Dir d = entry.getKey();
                final int countD = entry.getValue();

                final Dir perp1 = new Dir(-d.dy, d.dx);
                final Dir perp2 = new Dir(d.dy, -d.dx);

                final Integer countPerp1 = counts.get(perp1);
                final Integer countPerp2 = counts.get(perp2);

                if (countPerp1 != null) {
                    local += (long) countD * countPerp1;
                }
                if (countPerp2 != null) {
                    local += (long) countD * countPerp2;
                }
            }

            // Each perpendicular pair counted twice, so divide by 2
            total += local / 2;
        }

        return total;
    }

    /**
     * Normalizes a direction vector by dividing both components
     * by the greatest common divisor so that vectors with the same
     * slope map to identical representations.
     *
     * @param dx x-change
     * @param dy y-change
     * @return normalized direction vector
     */
    private static Dir normalize(int dx, int dy) {
        final int g = gcd(Math.abs(dx), Math.abs(dy));
        dx /= g;
        dy /= g;
        return new Dir(dx, dy);
    }

    /**
     * Computes the GCD using the Euclidean algorithm.
     *
     * @param a first non-negative integer
     * @param b second non-negative integer
     * @return {@code gcd(a, b)}, or 1 if both are 0
     */
    private static int gcd(int a, int b) {
        while (b != 0) {
            final int t = a % b;
            a = b;
            b = t;
        }

        // If both components are 0, return 1 to avoid division by zero.
        return (a == 0) ? 1 : a;
    }
}
