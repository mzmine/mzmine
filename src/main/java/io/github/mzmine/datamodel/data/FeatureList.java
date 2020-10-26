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
 * the im plied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data;

import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;

/**
 * Interface for feature list
 */
public interface FeatureList {

  public interface FeatureListAppliedMethod {

    @Nonnull
    public String getDescription();

    @Nonnull
    public String getParameters();

  }

  /**
   * @return Short descriptive name for the feature list
   */
  public String getName();

  /**
   * Change the name of this feature list
   */
  public void setName(String name);

  /**
   * Returns number of raw data files participating in the feature list
   */
  public int getNumberOfRawDataFiles();

  /**
   * Returns all raw data files participating in the feature list
   */
  public ObservableList<RawDataFile> getRawDataFiles();

  /**
   * Returns true if this feature list contains given file
   */
  public boolean hasRawDataFile(RawDataFile file);

  /**
   * Returns a raw data file
   * 
   * @param position Position of the raw data file in the matrix (running numbering from left
   *        0,1,2,...)
   */
  public RawDataFile getRawDataFile(int position);

  /**
   * Returns number of rows in the alignment result
   */
  public int getNumberOfRows();

  /**
   * Returns the peak of a given raw data file on a give row of the feature list
   * 
   * @param row Row of the feature list
   * @param rawDataFile Raw data file where the peak is detected/estimated
   */
  public Feature getPeak(int row, RawDataFile rawDataFile);

  /**
   * Returns all peaks for a raw data file
   */
  public ObservableList<Feature> getPeaks(RawDataFile rawDataFile);

  /**
   * Returns all peaks on one row
   */
  public FeatureListRow getRow(int row);

  /**
   * Returns all feature list rows
   */
  public ObservableList<FeatureListRow> getRows();

  /**
   * Creates a stream of PeakListRows
   * 
   * @return
   */
  public Stream<FeatureListRow> stream();

  /**
   * Creates a parallel stream of PeakListRows
   * 
   * @return
   */
  public Stream<FeatureListRow> parallelStream();

  /**
   * Stream of all features across all samples
   * 
   * @return
   */
  public Stream<Feature> streamFeatures();

  /**
   * Parallel stream of all rows.features across all samples
   * 
   * @return
   */
  public Stream<Feature> parallelStreamFeatures();

  /**
   * Returns all rows with average retention time within given range
   *
   * @param rtRange Retention time range
   */
  public ObservableList<FeatureListRow> getRowsInsideScanRange(Range<Float> rtRange);

  /**
   * Returns all rows with average m/z within given range
   * 
   * @param mzRange m/z range
   */
  public ObservableList<FeatureListRow> getRowsInsideMZRange(Range<Double> mzRange);

  /**
   * Returns all rows with average m/z and retention time within given range
   *
   * @param rtRange Retention time range
   * @param mzRange m/z range
   */
  public ObservableList<FeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange,
      Range<Double> mzRange);

  /**
   * Returns all peaks overlapping with a retention time range
   *
   * @param file Raw data file
   * @param rtRange Retention time range
   */
  public ObservableList<Feature> getPeaksInsideScanRange(RawDataFile file, Range<Float> rtRange);

  /**
   * Returns all peaks in a given m/z range
   * 
   * @param file Raw data file
   * @param mzRange m/z range
   */
  public ObservableList<Feature> getPeaksInsideMZRange(RawDataFile file, Range<Double> mzRange);

  /**
   * Returns all peaks in a given m/z & retention time ranges
   *
   * @param file Raw data file
   * @param rtRange Retention time range
   * @param mzRange m/z range
   */
  public ObservableList<Feature> getPeaksInsideScanAndMZRange(RawDataFile file, Range<Float> rtRange,
      Range<Double> mzRange);

  /**
   * Returns maximum raw data point intensity among all peaks in this feature list
   * 
   * @return Maximum intensity
   */
  public double getDataPointMaxIntensity();

  /**
   * Add a new row to the feature list
   */
  public void addRow(FeatureListRow row);

  /**
   * Removes a row from this feature list
   * 
   */
  public void removeRow(int row);

  /**
   * Removes a row from this feature list
   * 
   */
  public void removeRow(FeatureListRow row);

  /**
   * Returns a row number of given peak
   */
  public int getPeakRowNum(Feature peak);

  /**
   * Returns a row containing given peak
   */
  public FeatureListRow getPeakRow(Feature peak);

  public void addDescriptionOfAppliedTask(FeatureListAppliedMethod appliedMethod);

  /**
   * Returns all tasks (descriptions) applied to this feature list
   */
  public ObservableList<FeatureListAppliedMethod> getAppliedMethods();

  /**
   * Returns the whole m/z range of the feature list
   */
  public Range<Double> getRowsMZRange();

  /**
   * Returns the whole retention time range of the feature list
   */
  public Range<Float> getRowsRTRange();

  /**
   * Find row by ID
   * 
   * @param id id
   * @return the peaklist row or null
   */
  public FeatureListRow findRowByID(int id);

  default boolean isEmpty() {
    return getRows().isEmpty();
  }

  public String getDateCreated();

  public void setDateCreated(String date);

}
