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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class SpectralDeconvolutionTools {

  public static List<List<ModularFeature>> groupFeatures(
      SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm, List<ModularFeature> features,
      RTTolerance rtTolerance, int minNumberOfSignals) {
    switch (spectralDeconvolutionAlgorithm) {
      case HIERARCHICAL_CLUSTERING -> {
        return SpectralDeconvolutionTools.clusterFeaturesByRTHierarchical(features, rtTolerance,
            minNumberOfSignals);
      }
      case RT_GROUPING_AND_SHAPE_CORRELATION -> {
        return SpectralDeconvolutionTools.clusterFeaturesByRTAndCorrelation(features, rtTolerance,
            minNumberOfSignals);
      }
    }
    return SpectralDeconvolutionTools.clusterFeaturesByRTAndCorrelation(features, rtTolerance,
        minNumberOfSignals);
  }

  private static List<List<ModularFeature>> clusterFeaturesByRT(List<ModularFeature> features,
      RTTolerance rtTolerance, int minNumberOfSignals) {
    features.sort(
        Comparator.comparingDouble(ModularFeature::getHeight).reversed());  // Sort by area

    List<List<ModularFeature>> clusters = new ArrayList<>();
    for (ModularFeature feature : features) {
      List<ModularFeature> bestCluster = null;
      double smallestDistance = Double.MAX_VALUE;

      for (List<ModularFeature> cluster : clusters) {
        ModularFeature clusterRepFeature = cluster.getFirst();  // Get representative feature (first in cluster)
        double distance = Math.abs(clusterRepFeature.getRT() - feature.getRT());
        if (rtTolerance.checkWithinTolerance(clusterRepFeature.getRT(), feature.getRT())
            && distance < smallestDistance) {
          bestCluster = cluster;
          smallestDistance = distance;
        }
      }

      if (bestCluster != null) {
        bestCluster.add(feature);
      } else {
        List<ModularFeature> newCluster = new ArrayList<>();
        newCluster.add(feature);
        clusters.add(newCluster);
      }
    }
    clusters.removeIf(cluster -> cluster.size() < minNumberOfSignals);
    return clusters;
  }

  public static List<List<ModularFeature>> clusterFeaturesByRTAndCorrelation(
      List<ModularFeature> features, RTTolerance rtTolerance, int minNumberOfSignals) {
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
        potentialClusters.getFirst().add(feature);
      } else {
        // Multiple matching clusters, use correlation to determine the best one
        List<ModularFeature> bestCluster = null;
        double highestCorrelation = Double.NEGATIVE_INFINITY;

        for (List<ModularFeature> potentialCluster : potentialClusters) {
          ModularFeature clusterRepFeature = potentialCluster.get(0); // Representative feature
          double correlation = calculateCorrelation(clusterRepFeature, feature);
          if (correlation > highestCorrelation) {
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

  private static double calculateCorrelation(ModularFeature feature1, ModularFeature feature2) {
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

  private static Range<Float> getOverlap(Range<Float> range1, Range<Float> range2) {
    if (range1 != null && range2 != null && range1.isConnected(range2)) {
      return range1.intersection(range2);
    } else {
      return null; // Indicating no overlap
    }
  }

  private static List<List<ModularFeature>> clusterFeaturesByRTHierarchical(
      List<ModularFeature> features,
      RTTolerance rtTolerance, int minNumberOfSignals) {
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

  public static List<FeatureListRow> generatePseudoSpectra(List<ModularFeature> features,
      FeatureList featureList, RTTolerance rtTolerance, int minNumberOfSignals,
      SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm) {
    List<FeatureListRow> deconvolutedFeatureListRowsByRtOnly = new ArrayList<>();
    List<List<ModularFeature>> groupedFeatures = SpectralDeconvolutionTools.groupFeatures(
        spectralDeconvolutionAlgorithm, features, rtTolerance, minNumberOfSignals);
    for (List<ModularFeature> group : groupedFeatures) {
      if (group.size() < minNumberOfSignals) {
        continue;
      }

      // is already sorted by intensity best first
      ModularFeature mainFeature = group.getFirst();

      group.sort(Comparator.comparingDouble(ModularFeature::getMZ));
      double[] mzs = new double[group.size()];
      double[] intensities = new double[group.size()];
      for (int i = 0; i < group.size(); i++) {
        mzs[i] = group.get(i).getMZ();
        intensities[i] = group.get(i).getHeight();
      }

      // Create PseudoSpectrum, take first feature to ensure most intense is representative feature
      PseudoSpectrum pseudoSpectrum = new SimplePseudoSpectrum(featureList.getRawDataFile(0), 1,
          // MS Level
          mainFeature.getRT(), null, // No MsMsInfo for pseudo spectrum
          mzs, intensities, mainFeature.getRepresentativeScan().getPolarity(),
          "Correlated Features Pseudo Spectrum", PseudoSpectrumType.GC_EI);

      mainFeature.setAllMS2FragmentScans(List.of(pseudoSpectrum));
      deconvolutedFeatureListRowsByRtOnly.add(mainFeature.getRow());
    }
    return deconvolutedFeatureListRowsByRtOnly;
  }

}
