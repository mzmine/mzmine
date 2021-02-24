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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
 * by retention time)
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FeatureDataAccess implements IonTimeSeries<Scan> {

  /**
   * Different types to handle feature data: {@link #ONLY_DETECTED}: Use only detected data points;
   * {@link #INCLUDE_ZEROS}: Fill all missing data points in the chromatogram with zeros;
   */
  public enum FeatureDataType {
    ONLY_DETECTED, INCLUDE_ZEROS
  }

  protected final FeatureList flist;
  protected final FeatureDataType type;
  private final List<FeatureListRow> rows;
  protected final int totalFeatures;
  // data file is only set for aligned lists to access all features of this data file
  @Nullable
  private final RawDataFile dataFile;

  // current data
  private Feature feature;
  private IonTimeSeries<? extends Scan> featureData;
  // all scans of the whole chromatogram; only used if FeatureDataType != ONLY_DETECTED
  private List<? extends Scan> allScans;

  protected final double[] mzs;
  protected final double[] intensities;
  protected int currentFeatureIndex = -1;
  protected int currentRowIndex = -1;
  protected int currentRawFileIndex = -1;
  protected int currentNumberOfDataPoints = -1;

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist    target feature list. Loops through all features in all RawDataFiles
   * @param type     defines the data accession type
   */
  protected FeatureDataAccess(FeatureList flist,
      FeatureDataType type) {
    this(flist, type, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist    target feature list. Loops through all features in dataFile
   * @param type     defines the data accession type
   * @param dataFile define the data file in an aligned feature list
   */
  protected FeatureDataAccess(FeatureList flist,
      FeatureDataType type, @Nullable RawDataFile dataFile) {
    this.flist = flist;
    this.type = type;
    this.dataFile = dataFile;

    // set rows and number of features
    int totalFeatures = 0;
    List<FeatureListRow> allRows = flist.getRows();
    // handle aligned flist
    if (flist.getNumberOfRawDataFiles() > 1) {
      if (dataFile != null) {
        List<FeatureListRow> result = new ArrayList<>();
        for (FeatureListRow row : allRows) {
          if (row.hasFeature(dataFile)) {
            result.add(row);
            totalFeatures++;
          }
        }
        this.rows = Collections.unmodifiableList(result);
      } else {
        this.rows = Collections.unmodifiableList(allRows);
        // all features
        for (FeatureListRow row : allRows) {
          totalFeatures += row.getNumberOfFeatures();
        }
      }
    } else {
      // single raw data file in feature list - use all rows
      this.rows = Collections.unmodifiableList(allRows);
      totalFeatures = rows.size();
    }
    this.totalFeatures = totalFeatures;

    // find maximum length depending on FeatureDataType
    int length = getMaxNumOfDataPoints();
    mzs = new double[length];
    intensities = new double[length];
  }

  /**
   * The maximum number of data points depending on the FeatureDataType
   */
  private int getMaxNumOfDataPoints() {
    switch (type) {
      case ONLY_DETECTED -> {
        // Find maximum number of detected data points
        int max = 0;
        if (dataFile != null) {
          for (FeatureListRow row : rows) {
            int scans = row.getFeature(dataFile).getNumberOfDataPoints();
            if (max < scans) {
              max = scans;
            }
          }
        } else {
          // aligned feature list or dataFile not set
          for (FeatureListRow row : rows) {
            for (Feature f : row.getFeatures()) {
              int scans = f.getNumberOfDataPoints();
              if (max < scans) {
                max = scans;
              }
            }
          }
        }
        return max;
      }
      case INCLUDE_ZEROS -> {
        // return all scans that were used to create the chromatograms in the first place
        if (dataFile == null && flist.getNumberOfRawDataFiles() > 1) {
          int max = 0;
          for (RawDataFile raw : flist.getRawDataFiles()) {
            int scans = flist.getSeletedScans(raw).size();
            if (max < scans) {
              max = scans;
            }
          }
          return max;
        } else {
          // one raw data file
          return flist.getSeletedScans(dataFile != null ? dataFile : flist.getRawDataFile(0))
              .size();
        }
      }
      default -> throw new IllegalStateException("Unexpected value: " + type);
    }
  }


  @Override
  public List<Scan> getSpectra() {
    return null;
  }

  @Override
  public float getRetentionTime(int index) {
    assert index < getNumOfDataPoints() && index >= 0;
    return allScans != null ? allScans.get(index).getRetentionTime()
        : featureData.getRetentionTime(index);
  }

  /**
   * Get mass to charge value at index
   *
   * @param index data point index
   * @return mass to charge value at index
   */
  public double getMzValue(int index) {
    assert index < getNumOfDataPoints() && index >= 0;
    return mzs[index];
  }

  /**
   * Get intensity at index
   *
   * @param index data point index
   * @return intensity at index
   */
  public double getIntensity(int index) {
    assert index < getNumOfDataPoints() && index >= 0;
    return intensities[index];
  }

  /**
   * @return current number of data points depending on FeatureDataType
   */
  private int getNumOfDataPoints() {
    return currentNumberOfDataPoints;
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
    if (hasNextFeature()) {
      currentFeatureIndex++;
      // set next feature
      if (dataFile == null && flist.getNumberOfRawDataFiles() > 1) {
        // aligned feature list: find next feature
        do {
          currentRowIndex++;
          if (currentRowIndex >= rows.size()) {
            // start over at next data file
            currentRowIndex = 0;
            currentRawFileIndex++;
          }
          feature = getRow()
              .getFeature(getRow().getRawDataFiles().get(currentRawFileIndex));
        } while (feature == null);
      } else {
        currentRowIndex++;
        // single data file
        feature = getRow()
            .getFeature(dataFile != null ? dataFile : getRow().getRawDataFiles().get(0));
      }

      featureData = feature.getFeatureData();
      switch (type) {
        case ONLY_DETECTED -> {
          featureData.getMzValues(mzs);
          featureData.getIntensityValues(intensities);
          currentNumberOfDataPoints = featureData.getNumberOfValues();
        }
        case INCLUDE_ZEROS -> {
          // add detected data points and zero for missing values
          allScans = flist.getSeletedScans(feature.getRawDataFile());
          List<Scan> detectedScans = feature.getScanNumbers();
          int detectedIndex = 0;
          for (int i = 0; i < intensities.length; i++) {
            if (allScans.get(i) == detectedScans.get(detectedIndex)) {
              intensities[i] = featureData.getIntensity(detectedIndex);
              mzs[i] = featureData.getMZ(detectedIndex);
              detectedIndex++;
            }
          }
          currentNumberOfDataPoints = allScans.size();
        }
      }
      return feature;
    }
    return null;
  }

  /**
   * @return The current feature list row
   */
  private FeatureListRow getRow() {
    return rows.get(currentRowIndex);
  }

  /**
   * The current list of scans has another element
   *
   * @return
   */
  public boolean hasNextFeature() {
    return currentFeatureIndex < getNumOfFeatures();
  }

  /**
   * The number of feature list rows with at least one feature (matching the filters)
   *
   * @return Number of feature list rows
   */
  public int getNumOfFeatureListRows() {
    return rows.size();
  }

  /**
   * For feature lists of one RawDataFile, num of features equals to {@link
   * #getNumOfFeatureListRows()}. For aligned feature lists two options are available: Either all
   * features or all features of a selected RawDataFile
   *
   * @return number of features
   */
  public int getNumOfFeatures() {
    return totalFeatures;
  }


  //#######################################
  // Unsupported methods due to different intended use
  @Override
  public IonTimeSeries<Scan> subSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<Scan> subset) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public IonSpectrumSeries<Scan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @Nonnull double[] newMzValues, @Nonnull double[] newIntensityValues) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public IonSeries copy(MemoryMapStorage storage) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public DoubleBuffer getMZValues() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public double[] getMzValues(double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }
}
