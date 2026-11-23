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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.rtgroupingandsharecorrelation;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionAlgorithm;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation runs out of memory and does not perform well
 */
@Deprecated
public class RtGroupingAndShapeCorrelationAlgorithm implements SpectralDeconvolutionAlgorithm {

  private final RTTolerance rtTolerance;
  private final int minNumberOfSignals;
  private final double minR;

  // needs default constructor for config
  public RtGroupingAndShapeCorrelationAlgorithm() {
    this(null, 1, 0.8);
  }

  public RtGroupingAndShapeCorrelationAlgorithm(ParameterSet parameters) {
    this.rtTolerance = parameters.getValue(RtGroupingAndShapeCorrelationParameters.RT_TOLERANCE);
    this.minNumberOfSignals = parameters.getValue(
        RtGroupingAndShapeCorrelationParameters.MIN_NUMBER_OF_SIGNALS);
    this.minR = parameters.getValue(RtGroupingAndShapeCorrelationParameters.MIN_R);
  }

  public RtGroupingAndShapeCorrelationAlgorithm(RTTolerance rtTolerance, int minNumberOfSignals,
      double minR) {
    this.rtTolerance = rtTolerance;
    this.minNumberOfSignals = minNumberOfSignals;
    this.minR = minR;
  }

  @Override
  public List<List<ModularFeature>> groupFeatures(List<ModularFeature> features) {
    features.sort(Comparator.comparingDouble(ModularFeature::getHeight).reversed());
    List<List<ModularFeature>> clusters = new ArrayList<>();

    for (ModularFeature feature : features) {
      List<List<ModularFeature>> potentialClusters = new ArrayList<>();

      // Find all clusters that the feature can potentially fit into
      for (List<ModularFeature> cluster : clusters) {
        ModularFeature clusterRepFeature = cluster.getFirst(); // Get the first feature as the representative
        if (rtTolerance.checkWithinTolerance(clusterRepFeature.getRT(), feature.getRT())) {
          potentialClusters.add(cluster);
        }
      }

      if (potentialClusters.isEmpty()) {
        // No matching clusters, create a new one
        List<ModularFeature> newCluster = new ArrayList<>();
        newCluster.add(feature);
        clusters.add(newCluster);
      } else if (potentialClusters.size() == 1) {
        // Only one matching cluster, add the feature to it
        double correlation = calculateCorrelation(potentialClusters.getFirst().getFirst(), feature);
        if (correlation >= minR) {
          potentialClusters.getFirst().add(feature);
        }
      } else {
        // Multiple matching clusters, use correlation to determine the best one
        List<ModularFeature> bestCluster = null;
        double highestCorrelation = Double.NEGATIVE_INFINITY;

        for (List<ModularFeature> potentialCluster : potentialClusters) {
          ModularFeature clusterRepFeature = potentialCluster.getFirst(); // Representative feature
          double correlation = calculateCorrelation(clusterRepFeature, feature);
          if (correlation > highestCorrelation && correlation >= minR) {
            bestCluster = potentialCluster;
            highestCorrelation = correlation;
          }
        }

        // Add the feature to the best cluster based on correlation
        if (bestCluster != null) {
          bestCluster.add(feature);
        }
      }
    }

    // Remove clusters that do not meet the minimum number of signals
    clusters.removeIf(cluster -> cluster.size() < minNumberOfSignals);

    return clusters;
  }

  @Override
  public RTTolerance getRtTolerance() {
    return rtTolerance;
  }

  @Override
  public @NotNull String getName() {
    return "rt grouping and shape correlation";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return RtGroupingAndShapeCorrelationParameters.class;
  }

  private double calculateCorrelation(ModularFeature feature1, ModularFeature feature2) {
    // shared with the compound member scores of the deconvolution
    return SpectralDeconvolutionUtils.calculateShapeCorrelation(feature1, feature2);
  }
}
