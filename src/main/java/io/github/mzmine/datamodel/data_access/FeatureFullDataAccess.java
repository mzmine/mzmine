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
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
 * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros for
 * missing data points. This is important for a few chromatogram deconvolution algorithms,
 * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean no
 * signal.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FeatureFullDataAccess extends FeatureDataAccess {

  // current data
  protected final double[] detectedMzs;
  protected final double[] detectedIntensities;

  protected final double[] mzs;
  protected final double[] intensities;

  // all scans of the whole chromatogram (for the current raw data file)
  private List<Scan> allScans;

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros
   * for missing data points. This is important for a few chromatogram deconvolution algorithms,
   * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean
   * no signal.
   *
   * @param flist target feature list. Loops through all features in all RawDataFiles
   */
  protected FeatureFullDataAccess(FeatureList flist) {
    this(flist, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros
   * for missing data points. This is important for a few chromatogram deconvolution algorithms,
   * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean
   * no signal.
   *
   * @param flist    target feature list. Loops through all features in dataFile
   * @param dataFile define the data file in an aligned feature list
   */
  protected FeatureFullDataAccess(FeatureList flist, @Nullable RawDataFile dataFile) {
    super(flist, dataFile);

    // return all scans that were used to create the chromatograms in the first place
    int max = 0;
    if (dataFile == null && flist.getNumberOfRawDataFiles() > 1) {
      for (RawDataFile raw : flist.getRawDataFiles()) {
        int scans = flist.getSeletedScans(raw).size();
        if (max < scans) {
          max = scans;
        }
      }
    } else {
      // one raw data file
      max = flist.getSeletedScans(dataFile != null ? dataFile : flist.getRawDataFile(0)).size();
    }

    mzs = new double[max];
    intensities = new double[max];
    // detected data points currently on feature/chromatogram
    int detected = getMaxNumOfDetectedDataPoints();

    detectedMzs = new double[detected];
    detectedIntensities = new double[detected];
  }

  @Override
  public List<Scan> getSpectra() {
    assert allScans != null;
    return allScans;
  }

  @Override
  public float getRetentionTime(int index) {
    assert index < getNumberOfValues() && index >= 0;

    return getSpectrum(index).getRetentionTime();
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
      // add detected data points and zero for missing values
      allScans = (List<Scan>) flist.getSeletedScans(feature.getRawDataFile());
      // read detected data in batch
      List<Scan> detectedScans = feature.getScanNumbers();
      featureData.getMzValues(detectedMzs);
      featureData.getIntensityValues(detectedIntensities);

      int detectedIndex = 0;
      for (int i = 0; i < intensities.length; i++) {
        if (allScans.get(i) == detectedScans.get(detectedIndex)) {
          intensities[i] = detectedIntensities[detectedIndex];
          mzs[i] = detectedMzs[detectedIndex];
          detectedIndex++;
        } else {
          intensities[i] = 0d;
        }
        if (detectedIndex == detectedScans.size() && i < intensities.length - 1) {
          Arrays.fill(intensities, i + 1, intensities.length, 0d);
          break;
        }
      }
      currentNumberOfDataPoints = allScans.size();
    } else {
      // clear
      allScans = null;
    }
    return feature;
  }

  /**
   * Usage of this method is strongly discouraged because it returns the internal buffer of this
   * data access. However, in exceptional use-cases such as resolving or smoothing XICs, a direct
   * access might be necessary to avoid copying arrays. Since the chromatograms might originate from
   * different raw data files, the number of data points in that raw file might be different from
   * the length of this buffer, which is set to the longest XIC. The current number of data points
   * can be accessed via {@link FeatureDataAccess#getNumberOfValues()}.
   * <p></p>
   * <b>NOTE:</b> In most cases, the use of  {@link FeatureDataAccess#getIntensity(int)} (int)} is more appropriate.
   *
   * @return The intensity buffer of this data access.
   */
  public double[] getIntensityValues() {
    return intensities;
  }

  /**
   * Usage of this method is strongly discouraged because it returns the internal buffer of this
   * data access. However, in exceptional use-cases such as resolving or smoothing XICs, a direct
   * access might be necessary to avoid copying arrays. Since the chromatograms might originate from
   * different raw data files, the number of data points in that raw file might be different from
   * the length of this buffer, which is set to the longest XIC. The current number of data points
   * can be accessed via {@link FeatureDataAccess#getNumberOfValues()}.
   * <p></p>
   * <b>NOTE:</b> In most cases, the use of  {@link FeatureDataAccess#getMZ(int)} is more appropriate.
   *
   * @return The m/z buffer of this data access.
   */
  public double[] getMzValues() {
    return mzs;
  }
}
