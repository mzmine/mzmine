/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.hierarchicalclustering;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionAlgorithm;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HierarchicalClusteringAlgorithm implements SpectralDeconvolutionAlgorithm {

  private final RTTolerance rtTolerance;
  private final int minNumberOfSignals;

  public HierarchicalClusteringAlgorithm() {
    this(null, 1);
  }

  public HierarchicalClusteringAlgorithm(RTTolerance rtTolerance, int minNumberOfSignals) {
    this.rtTolerance = rtTolerance;
    this.minNumberOfSignals = minNumberOfSignals;
  }

  @Override
  public SpectralDeconvolutionAlgorithm create(ParameterSet parameters) {
    var rtTolerance = parameters.getValue(HierarchicalClusteringParameters.RT_TOLERANCE);
    var minNumberOfSignals = parameters.getValue(
        HierarchicalClusteringParameters.MIN_NUMBER_OF_SIGNALS);
    return new HierarchicalClusteringAlgorithm(rtTolerance, minNumberOfSignals);
  }

  @Override
  public List<List<ModularFeature>> groupFeatures(List<ModularFeature> features) {
    features.sort(Comparator.comparingDouble(ModularFeature::getHeight).reversed());

    List<Cluster> clusters = new ArrayList<>();
    for (ModularFeature feature : features) {
      clusters.add(new Cluster(feature));
    }

    PriorityQueue<ClusterPair> queue = new PriorityQueue<>();
    for (int i = 0; i < clusters.size(); i++) {
      for (int j = i + 1; j < clusters.size(); j++) {
        float distance = clusters.get(i).getRTDistance(clusters.get(j));
        queue.add(new ClusterPair(clusters.get(i), clusters.get(j), distance));
      }
    }

    while (!queue.isEmpty()) {
      ClusterPair pair = queue.poll();
      if (clusters.contains(pair.cluster1) && clusters.contains(pair.cluster2)) {
        // Check if all features in pair.cluster2 are within rtTolerance of the representative feature of pair.cluster1
        boolean withinTolerance = pair.cluster2.features.stream().allMatch(
            feature -> rtTolerance.checkWithinTolerance(
                pair.cluster1.getRepresentativeFeature().getRT(), feature.getRT()));
        if (withinTolerance) {
          pair.cluster1.addFeatures(pair.cluster2);
          clusters.remove(pair.cluster2);

          for (Cluster other : clusters) {
            if (other != pair.cluster1) {
              float distance = pair.cluster1.getRTDistance(other);
              queue.add(new ClusterPair(pair.cluster1, other, distance));
            }
          }
        }
      }
    }

    List<List<ModularFeature>> result = new ArrayList<>();
    for (Cluster cluster : clusters) {
      if (cluster.features.size() >= minNumberOfSignals) {
        cluster.features.sort(Comparator.comparingDouble(ModularFeature::getHeight).reversed());
        result.add(cluster.features);
      }
    }
    return result;
  }

  @Override
  public RTTolerance getRtTolerance() {
    return rtTolerance;
  }

  @Override
  public @NotNull String getName() {
    return "hierarchical clustering";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return HierarchicalClusteringParameters.class;
  }
}
