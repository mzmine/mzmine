/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.maths.CenterFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class FeatureConvertorIonMobility {

  private static final FixedSizeHashMap
      <ModularFeature, Map<Integer, Set<RetentionTimeMobilityDataPoint>>> cache =
      new FixedSizeHashMap<>();

  /**
   * @param originalFeature The feature to collapse
   * @return A rudimentary modular feature with just scan numbers, data points, mz and intensity
   * assigned.
   */
  public static ModularFeature collapseMobilityDimensionOfModularFeature(
      final ModularFeature originalFeature) {
    ModularFeatureList flist = (ModularFeatureList) originalFeature.getFeatureList();
    if (flist == null) {
      throw new NullPointerException("Feature list of the ModularFeature is null.");
    }

    ModularFeature newFeature = new ModularFeature(flist);
    newFeature.set(RawFileType.class, originalFeature.getRawDataFile());
    Map<Integer, Set<RetentionTimeMobilityDataPoint>> sortedDataPoints = groupDataPointsByFrameId(
        originalFeature);

    double maxIntensity = 0;
    // sum intensity over mobility dimension
    for (Entry<Integer, Set<RetentionTimeMobilityDataPoint>> entry : sortedDataPoints.entrySet()) {
      int frameNumber = entry.getKey();
      double mz = 0;
      double intensity = 0;
      for (RetentionTimeMobilityDataPoint dp : entry.getValue()) {
        mz += dp.getMZ();
        intensity += dp.getIntensity();
        if (intensity > maxIntensity) {
          maxIntensity = intensity;
        }
      }
      DataPoint summedDataPoint = new SimpleDataPoint(mz / entry.getValue().size(), intensity);
      newFeature.getScanNumbers().add(frameNumber);
      newFeature.getDataPoints().add(summedDataPoint);
    }
    newFeature.setHeight((float) maxIntensity);

    double mz = 0;
    final double totalIntensity = newFeature.getDataPoints().stream()
        .mapToDouble(DataPoint::getIntensity).sum();
    for (DataPoint dp : newFeature.getDataPoints()) {
      mz += dp.getMZ() * (dp.getIntensity() / totalIntensity);
    }
    newFeature.setMZ(mz);
    // i don't think we need anything else to rt-resolve a feature ~SteffenHeu

    return newFeature;
  }

  /**
   * Converts a mobility-collapsed resolved feature from a {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.PeakResolver#resolvePeaks(Feature,
   * ParameterSet, RSessionWrapper, CenterFunction, double, float)} back to a mobility resolved
   * feature by mapping scan numbers in the resolved features to the original frame numbers and
   * replacing the data points.
   *
   * @param processedFeatures Mobility-compressed resolved features. To be converted via {@link
   *                          FeatureConvertors#ResolvedPeakToMoularFeature(ResolvedPeak)} first.
   *                          Will be modified in this method and returned.
   * @param originalFeature   Mobility-uncompressed original feature passed to the deconvolution
   *                          task.
   * @return
   */
  public static Set<ModularFeature> mapResolvedCollapsedFeaturesToImsFeature(
      Set<ModularFeature> processedFeatures, final ModularFeature originalFeature,
      final CenterFunction centerFunction) {

    /**
     * What to recalculate after expanding:
     * height, area, intensity range, mzrange, mz, ms2 scan numbers
     */

    // replace datapoints in mobility-collapsed resolved feature with original data points
    Map<Integer, Set<RetentionTimeMobilityDataPoint>> originalDataPoints = groupDataPointsByFrameId(
        originalFeature);
    for (ModularFeature processedFeature : processedFeatures) {
      processedFeature.getDataPoints().clear();
      // scan/frame numbers are still saved in the feature, so we can assign the original data
      // points from there.
      processedFeature.getScanNumbers().forEach(
          frameNum -> processedFeature.getDataPoints().addAll(originalDataPoints.get(frameNum)));
    }

    // update feature info
    for (ModularFeature processedFeature : processedFeatures) {
      double[] mzs = processedFeature.getDataPoints().stream().mapToDouble(DataPoint::getMZ)
          .toArray();
      double[] intensities = processedFeature.getDataPoints().stream()
          .mapToDouble(DataPoint::getIntensity).toArray();
      //double[] mobilities = processedFeature.getDataPoints().stream().mapToDouble(RetentionTimeMobilityDataPoint::getMobility).toArray();
      double mz = centerFunction.calcCenter(mzs, intensities);
      //double mobility = centerFunction.calcCenter(mobilities, intensities);
      processedFeature.setMZ(mz);
      //processedFeature.set(MobilityType.class, mobility);
      // TODO: calc area
      processedFeature.set(IntensityRangeType.class, Range
          .closed((float) Arrays.stream(intensities).min().getAsDouble(),
              (float) Arrays.stream(intensities).max().getAsDouble()));
      processedFeature.set(MZRangeType.class, Range.closed(Arrays.stream(mzs).min().getAsDouble(),
          Arrays.stream(mzs).max().getAsDouble()));
    }
    return processedFeatures;
  }

  @Nonnull
  public static ModularFeature mapResolvedCollapsedFeaturesToImsFeature(
      ModularFeature processedFeature, final ModularFeature originalFeature,
      final CenterFunction centerFunction) {
    return mapResolvedCollapsedFeaturesToImsFeature(Set.of(processedFeature), originalFeature,
        centerFunction).stream().findAny().get();

  }

  /**
   * @param originalFeature The original feature
   * @return Mapping of frame id -> set of {@link RetentionTimeMobilityDataPoint}.
   */
  public static Map<Integer, Set<RetentionTimeMobilityDataPoint>> groupDataPointsByFrameId(
      @Nonnull final ModularFeature originalFeature) {
    if (!(originalFeature.getRawDataFile() instanceof IMSRawDataFile)) {
      throw new IllegalArgumentException(
          "Cannot collapse mobility dimension for features that were not created from IMSRawDataFiles.");
    }

    // I'm not using computeIfAbsent here because it might lead to a concurrent modification
    Map<Integer, Set<RetentionTimeMobilityDataPoint>> val = cache.get(originalFeature);
    if (val != null) {
      return val;
    }
    return groupByFrameIdAndCacheDataPoints(originalFeature);
  }

  /**
   * NOT TO BE CALLED DIRECTLY!!! USE THE CACHED {@link FeatureConvertorIonMobility#groupDataPointsByFrameId(ModularFeature)}
   * INSTEAD!!!
   *
   * @param originalFeature
   * @return
   */
  @Nonnull
  private static Map<Integer, Set<RetentionTimeMobilityDataPoint>> groupByFrameIdAndCacheDataPoints(
      @Nonnull final ModularFeature originalFeature) {

    List<? extends DataPoint> originalDataPoints = originalFeature.getDataPoints();
    List<RetentionTimeMobilityDataPoint> mobilityDataPoints =
        new ArrayList<>(originalDataPoints.size());
    for (DataPoint dp : originalDataPoints) {
      if (dp instanceof RetentionTimeMobilityDataPoint) {
        mobilityDataPoints.add((RetentionTimeMobilityDataPoint) dp);
      } else {
        throw new IllegalArgumentException("IMS feature contains invalid data points.");
      }
    }

    // group by frame & sort ascending
    Map<Integer, Set<RetentionTimeMobilityDataPoint>> sortedDataPoints = new TreeMap<>(
        Integer::compareTo);
    for (RetentionTimeMobilityDataPoint dp : mobilityDataPoints) {
      Set<RetentionTimeMobilityDataPoint> entry = sortedDataPoints
          .computeIfAbsent(dp.getFrameNumber(), HashSet::new);
      entry.add(dp);
    }

    cache.put(originalFeature, sortedDataPoints);
    return sortedDataPoints;
  }

  // maybe use ConcurrentSkipListMap instead if concurrency problems come up
  private static class FixedSizeHashMap<K, V> extends LinkedHashMap<K, V> {

    private static final Logger logger = Logger.getLogger(FixedSizeHashMap.class.getName());

    @Override
    public V get(Object key) {
      V val = super.get(key);
      if (val != null) {
        logger.finest(() -> "Retrieved entry " + key.toString() + " from cache.");
      } else {
        logger.finest(() -> "Entry " + key.toString() + " not present.");
      }
      return val;
    }

    private int maxSize = 50;

    public int getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
      if (this.size() >= maxSize) {
        return true;
      }
      return false;
    }
  }
}
