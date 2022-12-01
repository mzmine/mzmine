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
