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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.util.FeatureUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Integrated peak area data set. Separate data set is created for every peak shown in this
 * visualizer window.
 */
public class FeatureDataSet extends AbstractXYDataset {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final Feature feature;
  private final float[] retentionTimes;
  private final double[] intensities;
  private final double[] mzValues;
  private final String name;
  private final int featureItem;
  private final String seriesKey;

  /**
   * Create the data set.
   *
   * @param p  the feature.
   * @param id peak identity to use as a label.
   */
  public FeatureDataSet(final Feature p, final String id) {
    feature = p;
    name = id;
    seriesKey = FeatureUtils.featureToString(feature);

    final List<Scan> scanNumbers = feature.getScanNumbers();
    final RawDataFile dataFile = feature.getRawDataFile();
    final Scan peakScanNumber = feature.getRepresentativeScan();

    // Copy peak data.
    final int scanCount = scanNumbers.size();
    retentionTimes = new float[scanCount];
    intensities = new double[scanCount];
    mzValues = new double[scanCount];
    int peakIndex = -1;
    for (int i = 0; i < scanCount; i++) {

      // Representative scan number?
      final Scan scanNumber = scanNumbers.get(i);
      if (peakIndex < 0 && Objects.equals(scanNumber, peakScanNumber)) {

        peakIndex = i;
      }

      // Copy RT and m/z.
      retentionTimes[i] = scanNumber.getRetentionTime();

      if (feature instanceof ModularFeature) {
        mzValues[i] = ((ModularFeature) feature).getFeatureData().getMzForSpectrum(scanNumber);
        intensities[i] = ((ModularFeature) feature).getFeatureData()
            .getIntensityForSpectrum(scanNumber);
      } else { // todo remove this when everything is a ModularFeature
        final DataPoint dataPoint = feature.getDataPoint(scanNumber);
        if (dataPoint == null) {
          mzValues[i] = 0.0;
          intensities[i] = 0.0;
        } else {
          mzValues[i] = dataPoint.getMZ();
          intensities[i] = dataPoint.getIntensity();
        }
      }
    }

    featureItem = peakIndex;
  }

  /**
   * Create the data set - no label.
   *
   * @param p the peak.
   */
  public FeatureDataSet(final Feature p) {
    this(p, null);
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(final int series) {
    return seriesKey;
  }

  @Override
  public int getItemCount(final int series) {
    return retentionTimes.length;
  }

  @Override
  public Number getX(final int series, final int item) {
    return retentionTimes[item];
  }

  @Override
  public Number getY(final int series, final int item) {
    return intensities[item];
  }

  public double getMZ(final int item) {
    return mzValues[item];
  }

  public Feature getFeature() {
    return feature;
  }

  public String getName() {
    return name;
  }

  public boolean isFeature(final int item) {
    return item == featureItem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureDataSet)) {
      return false;
    }
    FeatureDataSet that = (FeatureDataSet) o;
    return featureItem == that.featureItem &&
        Objects.equals(feature, that.feature) &&
        Arrays.equals(retentionTimes, that.retentionTimes) &&
        Arrays.equals(intensities, that.intensities) &&
        Arrays.equals(mzValues, that.mzValues) &&
        Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(feature);
    result = 31 * result + Arrays.hashCode(retentionTimes);
    result = 31 * result + Arrays.hashCode(mzValues);
    return result;
  }
}
