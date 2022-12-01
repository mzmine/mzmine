/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
