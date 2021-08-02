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

  /**
   * Create the data set.
   *
   * @param p  the feature.
   * @param id peak identity to use as a label.
   */
  public FeatureDataSet(final Feature p, final String id) {

    feature = p;
    name = id;

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
    return FeatureUtils.featureToString(feature);
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
