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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.rtgroupingandsharecorrelation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionAlgorithm;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RtGroupingAndShapeCorrelationAlgorithm implements SpectralDeconvolutionAlgorithm {

  private final RTTolerance rtTolerance;
  private final int minNumberOfSignals;
  private final double minR;

  public RtGroupingAndShapeCorrelationAlgorithm() {
    this(null, 1, 0.8);
  }

  public RtGroupingAndShapeCorrelationAlgorithm(RTTolerance rtTolerance, int minNumberOfSignals,
      double minR) {
    this.rtTolerance = rtTolerance;
    this.minNumberOfSignals = minNumberOfSignals;
    this.minR = minR;
  }

  @Override
  public SpectralDeconvolutionAlgorithm create(ParameterSet parameters) {
    var rtTolerance = parameters.getValue(RtGroupingAndShapeCorrelationParameters.RT_TOLERANCE);
    var minNumberOfSignals = parameters.getValue(
        RtGroupingAndShapeCorrelationParameters.MIN_NUMBER_OF_SIGNALS);
    var minR = parameters.getValue(RtGroupingAndShapeCorrelationParameters.MIN_R);
    return new RtGroupingAndShapeCorrelationAlgorithm(rtTolerance, minNumberOfSignals, minR);
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
    //find data points with shared rt
    Range<Float> overlappingRtRange = getOverlap(feature1.getRawDataPointsRTRange(),
        feature2.getRawDataPointsRTRange());
    if (overlappingRtRange == null) {
      return 0.0;
    }
    int numberOfDpsFeature1 = feature1.getNumberOfDataPoints();
    List<Double> feature1Profile = new ArrayList<>();
    for (int i = 0; i < numberOfDpsFeature1; i++) {
      if (overlappingRtRange.contains(feature1.getFeatureData().getRetentionTime(i))) {
        feature1Profile.add(feature1.getFeatureData().getIntensity(i));
      }
    }

    List<Double> feature2Profile = new ArrayList<>();
    int numberOfDpsFeature2 = feature2.getNumberOfDataPoints();
    for (int i = 0; i < numberOfDpsFeature2; i++) {
      if (overlappingRtRange.contains(feature2.getFeatureData().getRetentionTime(i))) {
        feature2Profile.add(feature2.getFeatureData().getIntensity(i));
      }
    }

    if (feature1Profile.size() != feature2Profile.size()) {
      throw new IllegalArgumentException("Profiles must have the same length");
    }

    double mean1 = feature1Profile.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double mean2 = feature2Profile.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    double numerator = 0.0;
    double denominator1 = 0.0;
    double denominator2 = 0.0;

    for (int i = 0; i < feature1Profile.size(); i++) {
      double diff1 = feature1Profile.get(i) - mean1;
      double diff2 = feature2Profile.get(i) - mean2;
      numerator += diff1 * diff2;
      denominator1 += diff1 * diff1;
      denominator2 += diff2 * diff2;
    }

    double denominator = Math.sqrt(denominator1) * Math.sqrt(denominator2);
    return denominator == 0 ? 0 : numerator / denominator;
  }

  private Range<Float> getOverlap(Range<Float> range1, Range<Float> range2) {
    if (range1 != null && range2 != null && range1.isConnected(range2)) {
      return range1.intersection(range2);
    } else {
      return null; // Indicating no overlap
    }
  }
}
