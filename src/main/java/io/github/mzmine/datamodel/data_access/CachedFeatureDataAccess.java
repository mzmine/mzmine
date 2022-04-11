/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingTask;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class caches feature data and either preloads all features of all provided feature list rows
 * object loads data lazily and stores already loaded intensity/mz values in memory for later data
 * accession. The intended use is for cases where the feature data is accessed multiple times, e.g.,
 * in the {@link CorrelateGroupingTask} where all feature shapes are correlated against each other.
 * In this case, all intensity data are preloaded to avoid synchronization in a ConcurrentHashMap.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CachedFeatureDataAccess {

  protected final Map<Feature, double[]> intensityMap;
  protected final Map<Feature, double[]> mzMap;

  /**
   * Initialize data access as a cache with {@link ConcurrentHashMap} for thread safety. This is
   * useful if the features of interest are not predefined and if data are accessed multiple times
   * for the same feature. Otherwise, access the feature data directly.
   */
  public CachedFeatureDataAccess() {
    intensityMap = new ConcurrentHashMap<>();
    mzMap = new ConcurrentHashMap<>();
  }

  /**
   * Preloads specific data for all feature in all rows of a FeatureList. This is useful if all (or
   * most) of the features in the feature list are accessed multiple times. Otherwise, access the
   * feature data directly.
   *
   * @param featureList      defines the rows and features to be loaded
   * @param preloadMz        preload all m/z arrays for all features
   * @param preloadIntensity preload all intensity arrays for all features
   */
  public CachedFeatureDataAccess(ModularFeatureList featureList, boolean preloadMz,
      boolean preloadIntensity) {
    this(featureList.getRows().toArray(new FeatureListRow[0]), preloadMz, preloadIntensity);
  }

  /**
   * Preloads specific data for all feature in an array of rows. This is useful if all (or most) of
   * the features in the selected rows are accessed multiple times. Otherwise, access the feature
   * data directly.
   *
   * @param rows             a list of rows (provides the option to prefilter)
   * @param preloadMz        preload all m/z arrays for all features
   * @param preloadIntensity preload all intensity arrays for all features
   */
  public CachedFeatureDataAccess(FeatureListRow[] rows, boolean preloadMz,
      boolean preloadIntensity) {
    intensityMap = new HashMap<>();
    mzMap = new HashMap<>();
    if (preloadMz) {
      loadMzValues(rows);
    }
    if (preloadIntensity) {
      loadIntensityValues(rows);
    }
  }

  /**
   * This method returns the intensity array and keeps it in memory for later accessions
   *
   * @param f feature
   * @return the intensity array of this feature
   */
  public double[] getIntensityValues(Feature f) {
    assert f != null;
    return intensityMap.computeIfAbsent(f, feature -> feature.getFeatureData()
        .getIntensityValues(new double[feature.getNumberOfDataPoints()]));
  }

  /**
   * This method returns the m/z array and keeps it in memory for later accessions
   *
   * @param f feature
   * @return the m/z array of this feature
   */
  public double[] getMzValues(Feature f) {
    assert f != null;
    return mzMap.computeIfAbsent(f, feature -> feature.getFeatureData()
        .getMzValues(new double[feature.getNumberOfDataPoints()]));
  }

  /**
   * Load all intensity values
   *
   * @param rows load data for these rows
   */
  private void loadIntensityValues(FeatureListRow[] rows) {
    for (FeatureListRow row : rows) {
      for (Feature feature : row.getFeatures()) {
        if (feature != null && !feature.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
          intensityMap.put(feature, feature.getFeatureData()
              .getIntensityValues(new double[feature.getNumberOfDataPoints()]));
        }
      }
    }
  }

  /**
   * Load all mz values
   *
   * @param rows load data for these rows
   */
  private void loadMzValues(FeatureListRow[] rows) {
    for (FeatureListRow row : rows) {
      for (Feature feature : row.getFeatures()) {
        if (feature != null && !feature.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
          mzMap.put(feature, feature.getFeatureData()
              .getMzValues(new double[feature.getNumberOfDataPoints()]));
        }
      }
    }
  }

}
