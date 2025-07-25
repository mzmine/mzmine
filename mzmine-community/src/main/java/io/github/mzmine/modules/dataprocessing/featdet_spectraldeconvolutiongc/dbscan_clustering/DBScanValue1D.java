/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.dbscan_clustering;

import java.util.List;
import org.jetbrains.annotations.Nullable;

class DBScanValue1D<T> {

  private final T parent;
  private final double value;

  private List<DBScanValue1D<T>> neighbors;
  private int cluster = -1; // -1 = unclassified, 0 = noise, >0 = cluster ID
  private double sumDistance; // sum of all neighbor distances


  DBScanValue1D(T parent, double value) {
    this.parent = parent;
    this.value = value;
  }

  public T getParent() {
    return parent;
  }

  public double getValue() {
    return value;
  }

  public int getCluster() {
    return cluster;
  }

  public void setCluster(int cluster) {
    this.cluster = cluster;
  }

  @Nullable
  public List<DBScanValue1D<T>> getNeighbors() {
    return neighbors;
  }

  public void setNeighbors(List<DBScanValue1D<T>> neighbors) {
    this.neighbors = neighbors;
  }

  public int getNumNeighbors() {
    return neighbors == null ? 0 : neighbors.size();
  }

  @Nullable
  public DBScanValue1D<T> findDensestNeighbor() {
    if (neighbors == null) {
      return null;
    }
    int maxN = 0;
    DBScanValue1D<T> best = null;

    for (DBScanValue1D<T> neighbor : neighbors) {
      if (neighbor.getCluster() == -1) {
        continue;
      }
      final int numNeighbors = neighbor.getNumNeighbors();
      if (maxN < numNeighbors) {
        maxN = numNeighbors;
        best = neighbor;
      }
    }
    return best;
  }

  public void setSumDistance(double sumDistance) {
    this.sumDistance = sumDistance;
  }
}
