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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
 * by retention time). Uses only data points currently assigned to features. This differs for
 * chromatograms and resolved features
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FeatureDetectedDataAccess extends FeatureDataAccess {

  // current data
  protected final double[] mzs;
  protected final double[] intensities;


  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Uses only data points currently assigned to features. This differs for
   * chromatograms and resolved features
   *
   * @param flist target feature list. Loops through all features in all RawDataFiles
   */
  protected FeatureDetectedDataAccess(FeatureList flist) {
    this(flist, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Uses only data points currently assigned to features. This differs for
   * chromatograms and resolved features
   *
   * @param flist    target feature list. Loops through all features in dataFile
   * @param dataFile define the data file in an aligned feature list
   */
  protected FeatureDetectedDataAccess(FeatureList flist, @Nullable RawDataFile dataFile) {
    super(flist, dataFile);

    // detected data points currently on feature/chromatogram
    int detected = getMaxNumOfDetectedDataPoints();
    mzs = new double[detected];
    intensities = new double[detected];
  }

  @Override
  public List<Scan> getSpectra() {
    assert featureData != null;
    return featureData.getSpectra();
  }


  @Override
  public float getRetentionTime(int index) {
    assert index < getNumberOfValues() && index >= 0;
    assert featureData != null;

    return featureData.getRetentionTime(index);
  }

  /**
   * Get mass to charge value at index
   *
   * @param index data point index
   * @return mass to charge value at index
   */
  @Override
  public double getMZ(int index) {
    assert index < getNumberOfValues() && index >= 0;
    return mzs[index];
  }

  /**
   * Get intensity at index
   *
   * @param index data point index
   * @return intensity at index
   */
  @Override
  public double getIntensity(int index) {
    assert index < getNumberOfValues() && index >= 0;
    return intensities[index];
  }

  /**
   * Set the data to the next feature, if available. Returns the feature for additional data access.
   * retention time and intensity values should be accessed from this data class via {@link
   * #getRetentionTime(int)} and {@link #getIntensity(int)}
   *
   * @return the feature or null
   */
  @Nullable
  public Feature nextFeature() {
    super.nextFeature();
    if (feature != null) {
        featureData.getMzValues(mzs);
        featureData.getIntensityValues(intensities);
        currentNumberOfDataPoints = featureData.getNumberOfValues();
    }
    return feature;
  }

}
