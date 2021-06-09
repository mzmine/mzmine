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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.HashMap;
import java.util.Map;

/**
 * This data access object loads data lazily and stores already loaded intensity/mz values in memory
 * for later data accession.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class PreloadedFeatureDataAccess {

  private final FeatureListRow[] rows;

  protected Map<Feature, double[]> intensityMap = new HashMap();
  protected Map<Feature, double[]> mzMap = new HashMap();

  public PreloadedFeatureDataAccess(ModularFeatureList featureList) {
    this(featureList.getRows().toArray(new FeatureListRow[0]));
  }

  public PreloadedFeatureDataAccess(FeatureListRow[] rows) {
    this.rows = rows;
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
   */
  public void loadIntensityValues() {
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
   */
  public void loadMzValues() {
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
