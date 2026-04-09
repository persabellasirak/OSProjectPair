package com.tryright;

/**
 * Point storage interface
 * @version 1.1
 */
public interface PointStore {
  /**
   * Get X value at given index
   *
   * @param idx index from which to fetch values
   * @return X value at given index
   * @throws IndexOutOfBoundsException if idx < 0 or idx >= numPoints
   */
  int getX(int idx);

  /**
   * Get Y value at given index
   *
   * @param idx index from which to fetch values
   * @return Y value at given index
   * @throws IndexOutOfBoundsException if idx < 0 or idx >= numPoints
   */
  int getY(int idx);

  /**
   * Get number of points in store
   *
   * @return number of points in store
   */
  int numPoints();

  /**
   * Close point store
   */
  void close();
}
