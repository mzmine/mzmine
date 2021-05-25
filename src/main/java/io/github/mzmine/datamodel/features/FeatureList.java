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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import java.util.stream.Stream;

import io.github.mzmine.datamodel.features.correlation.R2RMS2Similarity;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import javax.annotation.Nullable;

/**
 * Interface for feature list
 */
public interface FeatureList {

  /**
   * TODO: extract interface and rename to AppliedMethod. Not doing it now to avoid merge conflicts.
   */
  public interface FeatureListAppliedMethod {

    @Nonnull
    public String getDescription();

    /**
     * This {@link FeatureListAppliedMethod} stores a clone of the original parameter set.
     *
     * @return A clone of the parameter set stored in this object, so the stored values cannot be
     * edited.
     */
    @Nonnull
    public ParameterSet getParameters();

    public MZmineModule getModule();
  }

  /**
   * @return Short descriptive name for the feature list
   */
  @Nonnull
  public String getName();

  /**
   * Change the name of this feature list
   */
  public void setName(@Nonnull String name);

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
   *                 0,1,2,...)
   */
  public RawDataFile getRawDataFile(int position);

  /**
   * Returns number of rows in the alignment result
   */
  public int getNumberOfRows();

  /**
   * Returns the feature of a given raw data file on a give row of the feature list
   *
   * @param row         Row of the feature list
   * @param rawDataFile Raw data file where the feature is detected/estimated
   */
  public Feature getFeature(int row, RawDataFile rawDataFile);

  /**
   * Returns all features for a raw data file
   */
  public ObservableList<Feature> getFeatures(RawDataFile rawDataFile);

  /**
   * Returns all features on one row
   */
  public FeatureListRow getRow(int row);

  /**
   * Returns all feature list rows
   */
  public ObservableList<FeatureListRow> getRows();

  /**
   * Creates a stream of FeatureListRows
   *
   * @return
   */
  default Stream<FeatureListRow> stream(boolean parallel) {
    return parallel? parallelStream() : stream();
  }

  /**
   * Creates a stream of FeatureListRows
   *
   * @return
   */
  public Stream<FeatureListRow> stream();

  /**
   * Creates a parallel stream of FeatureListRows
   *
   * @return
   */
  public Stream<FeatureListRow> parallelStream();

  /**
   * Stream of all features across all samples
   *
   * @return
   */
  default Stream<Feature> streamFeatures(boolean parallel) {
    return parallel? parallelStreamFeatures() : streamFeatures();
  }
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
   * The selected scans to build this feature/chromatogram
   *
   * @param file  the data file of the scans
   * @param scans all filtered scans that were used to build the chromatogram in the first place.
   *              For ion mobility data, the Frames are returned
   */
  void setSelectedScans(@Nonnull RawDataFile file, @Nullable List<? extends Scan> scans);

  /**
   * @param file the data file
   * @return The scans used to build this feature list. For ion mobility data, the frames are
   * returned.
   */
  @Nullable
  List<? extends Scan> getSeletedScans(@Nonnull RawDataFile file);

  /**
   * Returns all rows with average retention time within given range
   *
   * @param rtRange Retention time range
   */
  public List<FeatureListRow> getRowsInsideScanRange(Range<Float> rtRange);

  /**
   * Returns all rows with average m/z within given range
   *
   * @param mzRange m/z range
   */
  public List<FeatureListRow> getRowsInsideMZRange(Range<Double> mzRange);

  /**
   * Returns all rows with average m/z and retention time within given range
   *
   * @param rtRange Retention time range
   * @param mzRange m/z range
   */
  public List<FeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange,
      Range<Double> mzRange);

  /**
   * Returns all features overlapping with a retention time range
   *
   * @param file    Raw data file
   * @param rtRange Retention time range
   */
  public List<Feature> getFeaturesInsideScanRange(RawDataFile file, Range<Float> rtRange);

  /**
   * Returns all features in a given m/z range
   *
   * @param file    Raw data file
   * @param mzRange m/z range
   */
  public List<Feature> getFeaturesInsideMZRange(RawDataFile file, Range<Double> mzRange);

  /**
   * Returns all features in a given m/z & retention time ranges
   *
   * @param file    Raw data file
   * @param rtRange Retention time range
   * @param mzRange m/z range
   */
  public List<Feature> getFeaturesInsideScanAndMZRange(RawDataFile file,
      Range<Float> rtRange,
      Range<Double> mzRange);

  /**
   * Returns maximum raw data point intensity among all features in this feature list
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
   */
  public void removeRow(int row);

  /**
   * Removes a row from this feature list
   */
  public void removeRow(FeatureListRow row);

  /**
   * Returns a row number of given feature
   */
  public int getFeatureListRowNum(Feature feature);

  /**
   * Returns a row containing given feature
   */
  public FeatureListRow getFeatureRow(Feature feature);

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
   * @return the feature list row or null
   */
  public FeatureListRow findRowByID(int id);

  default boolean isEmpty() {
    return getRows().isEmpty();
  }

  public String getDateCreated();

  public void setDateCreated(String date);

  default boolean isAligned() {
    return getNumberOfRawDataFiles() > 1;
  }

  /**
   * List of RowGroups group features based on different methods
   */
  void setGroups(RowGroupList groups);

  /**
   * List of RowGroups group features based on different methods
   * @return
   */
  RowGroupList getGroups();


  /**
   * Add row-to-row relationships
   *
   * @param a
   * @param b
   * @param similarity
   */
  void addR2RSimilarity(FeatureListRow a, FeatureListRow b, R2RMS2Similarity similarity);

  /**
   * Add row-to-row relationships
   *
   * @param map
   */
  void addR2RSimilarity(R2RMap<R2RMS2Similarity> map);

  /**
   * A map with all row-to-row relationships
   * @return
   */
  R2RMap<R2RMS2Similarity> getR2RSimilarityMap();
}
