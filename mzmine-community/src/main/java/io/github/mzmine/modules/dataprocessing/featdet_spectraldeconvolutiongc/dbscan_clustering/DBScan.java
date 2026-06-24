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

import io.github.mzmine.util.collections.CollectionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Density-based clustering algorithm changed for 1D double values.
 * <p>
 * Use as:
 * {@snippet :
 * List<Feature> features = new ArrayList<>(); // needs content
 * DBScan<ModularFeature> dbScan = new DBScan<>( minNumberOfSignals, ModularFeature::getRT);
 * dbScan.cluster(features);
 *}
 *
 * @param <TVALUE>
 */
public class DBScan<TVALUE> {

  private final double epsilon;
  private final int minPts;
  private final @NotNull ToDoubleFunction<TVALUE> valueFunction;

  public DBScan(final double epsilon, final int minPts,
      final @NotNull ToDoubleFunction<TVALUE> valueFunction) {
    this.epsilon = epsilon;
    this.minPts = minPts;
    this.valueFunction = valueFunction;
  }

  /**
   * Cluster and sort result. Clusters will be sorted by value and also internally
   */
  public List<List<TVALUE>> clusterSorted(List<TVALUE> list) {
    final List<List<TVALUE>> clusters = cluster(list);
    for (final List<TVALUE> cluster : clusters) {
      cluster.sort(Comparator.comparingDouble(valueFunction));
    }
    clusters.sort(
        Comparator.comparingDouble(cluster -> valueFunction.applyAsDouble(cluster.getFirst())));
    return clusters;
  }

  public List<List<TVALUE>> cluster(List<TVALUE> unsortedPoints) {
    var sorted = unsortedPoints.stream()
        .map(p -> new DBScanValue1D<>(p, valueFunction.applyAsDouble(p)))
        .sorted(Comparator.comparingDouble(DBScanValue1D::getValue))
        .collect(CollectionUtils.toArrayList());

    // Step 1: Compute neighbors for all points
    setAllNeighbors(sorted);

    List<List<TVALUE>> clusters = new ArrayList<>();

    // Step 2: Identify core points and expand clusters
    // does not matter which point starts
    for (DBScanValue1D<TVALUE> point : sorted) {
      if (point.getCluster() != -1) {
        continue; // Skip already classified points
      }

      List<DBScanValue1D<TVALUE>> neighbors = point.getNeighbors();
      if (neighbors == null || neighbors.size() < minPts) {
        continue; // found border point to skip for now
      }
      // major point with many neighbors
      final List<TVALUE> cluster = new ArrayList<>();
      expandCluster(cluster, point, clusters.size());
      clusters.add(cluster);
    }

    // Step 3: handle border points that do not have enough neighbors to propagate itself
    for (DBScanValue1D<TVALUE> point : sorted) {
      // only work on border points
      if (point.getCluster() != -1) {
        continue;  // Skip already classified points
      }

      // border point with few neighbors
      // find neighbor that has the highest number of neighbors to put border point to dense area
      final DBScanValue1D<TVALUE> bestNeighbor = point.findDensestNeighbor();
      if (bestNeighbor != null && bestNeighbor.getCluster() > -1) {
        clusters.get(bestNeighbor.getCluster()).add(point.getParent());
      }
    }

    return clusters;
  }

  private void setAllNeighbors(ArrayList<DBScanValue1D<TVALUE>> sorted) {
    for (int i = 0; i < sorted.size(); i++) {
      final DBScanValue1D<TVALUE> target = sorted.get(i);
      setTargetNeighbors(sorted, target, i);
    }
  }

  private void expandCluster(List<TVALUE> cluster, DBScanValue1D<TVALUE> point, int clusterId) {
    cluster.add(point.getParent());
    point.setCluster(clusterId);
    final List<DBScanValue1D<TVALUE>> neighbors = point.getNeighbors();
    if (neighbors == null) {
      return;
    }

    for (DBScanValue1D<TVALUE> current : neighbors) {
      if (current.getCluster() != -1) {
        continue; // already in cluster
      }
      if (current.getNumNeighbors() >= minPts) {
        // is a core point so further expand
        expandCluster(cluster, current, clusterId);
      }
      // else border points are not used for expansion - they are added later
    }
  }

  private void setTargetNeighbors(List<DBScanValue1D<TVALUE>> sorted, DBScanValue1D<TVALUE> target,
      int targetIndex) {
    List<DBScanValue1D<TVALUE>> neighbors = new ArrayList<>();
    double sumDistance = 0;
    // left side
    for (int i = targetIndex - 1; i >= 0; i--) {
      final DBScanValue1D<TVALUE> p = sorted.get(i);
      final double distance = target.getValue() - p.getValue();
      if (distance <= epsilon) {
        sumDistance += distance;
        neighbors.add(p);
      } else {
        break; // early break
      }
    }

    for (int i = targetIndex + 1; i < sorted.size(); i++) {
      final DBScanValue1D<TVALUE> p = sorted.get(i);
      final double distance = p.getValue() - target.getValue();
      if (distance <= epsilon) {
        sumDistance += distance;
        neighbors.add(p);
      } else {
        break; // early break
      }
    }

    target.setNeighbors(neighbors);
    target.setSumDistance(sumDistance);
  }

}


