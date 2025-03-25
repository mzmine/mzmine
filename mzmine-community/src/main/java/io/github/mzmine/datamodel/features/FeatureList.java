/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataTypeUtils;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Interface for feature list
 */
public interface FeatureList {

  /**
   * Creates a dummy feature list. e.g. for creating a
   * {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver} for
   * an intermediate processing step. Not intended to be added to a project.
   */
  public static ModularFeatureList createDummy() {
    return new ModularFeatureList("dummy flist", null, RawDataFile.createDummyFile());
  }

  /**
   * @return Short descriptive name for the feature list
   */
  @NotNull String getName();

  /**
   * Change the name of this feature list
   *
   * @return the actually set name after checking for resticted symbols and duplicate names
   */
  String setName(@NotNull String name);

  void addRowBinding(@NotNull List<RowBinding> bindings);

  void addFeatureTypeListener(DataType featureType, DataTypeValueChangeListener listener);

  void addRowTypeListener(DataType rowType, DataTypeValueChangeListener listener);

  void removeRowTypeListener(DataType rowType, DataTypeValueChangeListener listener);

  void removeFeatureTypeListener(DataType featureType, DataTypeValueChangeListener listener);

  /**
   * Apply all row bindings to all rows (e.g., calculating the average m/z etc)
   */
  void applyRowBindings();

  /**
   * Apply all row bindings to row (e.g., calculating the average m/z etc)
   *
   * @param row
   */
  void applyRowBindings(FeatureListRow row);

  ObservableSet<DataType> getFeatureTypes();

  void addFeatureType(Collection<DataType> types);

  void addFeatureType(@NotNull DataType<?>... types);

  void addRowType(Collection<DataType> types);

  void addRowType(@NotNull DataType<?>... types);

  ObservableSet<DataType> getRowTypes();


  /**
   * Checks if typeClass was added as a FeatureType
   *
   * @param typeClass class of a DataType
   * @return true if feature type is available
   */
  default boolean hasFeatureType(Class<? extends DataType> typeClass) {
    return hasFeatureType(DataTypes.get(typeClass));
  }

  /**
   * Checks if typeClass was added as a FeatureType
   *
   * @param type DataType
   * @return true if feature type is available
   */
  default boolean hasFeatureType(DataType type) {
    return getFeatureTypes().contains(type);
  }

  /**
   * Checks if typeClass was added as a row type
   *
   * @param typeClass class of a DataType
   * @return true if row type is available
   */
  default boolean hasRowType(Class<? extends DataType> typeClass) {
    return hasRowType(DataTypes.get(typeClass));
  }

  /**
   * Checks if typeClass was added as a row type
   *
   * @param type DataType
   * @return true if row type is available
   */
  default boolean hasRowType(DataType type) {
    return getRowTypes().contains(type);
  }

  /**
   * Returns number of raw data files participating in the feature list
   */
  public int getNumberOfRawDataFiles();

  /**
   * Returns all raw data files participating in the feature list
   */
  public List<RawDataFile> getRawDataFiles();

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
  public ModularFeature getFeature(int row, RawDataFile rawDataFile);

  /**
   * Returns all features for a raw data file
   */
  public List<ModularFeature> getFeatures(RawDataFile rawDataFile);

  /**
   * Returns all features on one row
   */
  public FeatureListRow getRow(int row);

  /**
   * Returns all feature list rows
   */
  public ObservableList<FeatureListRow> getRows();

  /**
   * Clear all rows and set new rows
   *
   * @param rows new rows to set
   */
  void setRows(FeatureListRow... rows);

  /**
   * Clear all rows and set new rows
   *
   * @param rows new rows to set
   */
  default void setRows(List<FeatureListRow> rows) {
    setRows(rows.toArray(FeatureListRow[]::new));
  }

  /**
   * Creates a stream of FeatureListRows
   *
   * @return
   */
  default Stream<FeatureListRow> stream(boolean parallel) {
    return parallel ? parallelStream() : stream();
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
  default Stream<ModularFeature> streamFeatures(boolean parallel) {
    return parallel ? parallelStreamFeatures() : streamFeatures();
  }

  /**
   * Stream of all features across all samples
   *
   * @return
   */
  public Stream<ModularFeature> streamFeatures();

  /**
   * Parallel stream of all rows.features across all samples
   *
   * @return
   */
  public Stream<ModularFeature> parallelStreamFeatures();

  /**
   * prefer method {@link #setName} over this method to have path safe encoding and unique names in
   * the project. Notifies listeners
   *
   * @param name force set this name
   * @return the set name (equals the arguments)
   */
  String setNameNoChecks(@NotNull String name);

  /**
   * The selected scans to build this feature/chromatogram
   *
   * @param file  the data file of the scans
   * @param scans all filtered scans that were used to build the chromatogram in the first place.
   *              For ion mobility data, the Frames are returned
   */
  void setSelectedScans(@NotNull RawDataFile file, @Nullable List<? extends Scan> scans);

  /**
   * @param file the data file
   * @return The scans used to build this feature list. For ion mobility data, the frames are
   * returned.
   */
  @Nullable List<? extends Scan> getSeletedScans(@NotNull RawDataFile file);

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
  public List<Feature> getFeaturesInsideScanAndMZRange(RawDataFile file, Range<Float> rtRange,
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
   *
   * @return
   */
  List<RowGroup> getGroups();

  /**
   * List of RowGroups group features based on different methods
   */
  void setGroups(List<RowGroup> groups);


  /**
   * Short cut to get the MS1 correlation map of grouped features
   *
   * @return the map for {@link Type#MS1_FEATURE_CORR}
   */
  default Optional<R2RMap<RowsRelationship>> getMs1CorrelationMap() {
    return getRowMap(Type.MS1_FEATURE_CORR);
  }

  /**
   * Short cut to get the MS2 spectral similarity map of grouped features
   *
   * @return the map for {@link Type#MS2_COSINE_SIM}
   */
  default Optional<R2RMap<RowsRelationship>> getMs2SimilarityMap() {
    return getRowMap(Type.MS2_COSINE_SIM);
  }

  /**
   * A mutable map of row-to-row relationships.
   *
   * @param relationship the relationship between two rows
   * @return
   */
  default Optional<R2RMap<RowsRelationship>> getRowMap(Type relationship) {
    return getRowMap(relationship.toString());
  }

  /**
   * A mutable map of row-to-row relationships.
   *
   * @param type the relationship between two rows
   * @return
   */
  default Optional<R2RMap<RowsRelationship>> getRowMap(String type) {
    return getRowMaps().getRowsMap(type);
  }

  /**
   * An immutable map to store different relationships
   *
   * @return a map that stores different relationship maps
   */
  @NotNull R2RNetworkingMaps getRowMaps();

  /**
   * Maps {@link Feature} DataType listeners, e.g., for calculating the mean values for a DataType
   * over all features into a row DataType
   *
   * @return map of feature DataType listeners
   */
  @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getFeatureTypeChangeListeners();

  /**
   * Maps {@link FeatureListRow} DataType listeners, e.g., for graphical representations
   *
   * @return map of feature DataType listeners
   */
  @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getRowTypeChangeListeners();

  /**
   * @param row
   * @param newFeature
   * @param raw
   * @param updateByRowBindings if true, update values by row bindings. This option may be set to
   *                            false with caution, when multiple features are added. Remember to
   *                            update call {@link #applyRowBindings(FeatureListRow)} manually.
   */
  default void fireFeatureChangedEvent(FeatureListRow row, Feature newFeature, RawDataFile raw,
      boolean updateByRowBindings) {
    if (updateByRowBindings) {
      applyRowBindings(row);
    }

    if (newFeature != null) {
      boolean isImagingFile = raw instanceof ImagingRawDataFile;
      if (newFeature.getFeatureData() instanceof IonMobilogramTimeSeries) {
        DataTypeUtils.DEFAULT_ION_MOBILITY_COLUMNS_ROW.stream()
            .filter(type -> type instanceof LinkedGraphicalType)
            .forEach(type -> row.set(type, true));
        DataTypeUtils.DEFAULT_ION_MOBILITY_COLUMNS_FEATURE.stream()
            .filter(type -> type instanceof LinkedGraphicalType)
            .forEach(type -> row.set(type, true));
      }
      if (hasRowType(RTType.class) && !isImagingFile) {
        // activate shape for this row
        DataTypeUtils.DEFAULT_CHROMATOGRAPHIC_ROW.stream()
            .filter(type -> type instanceof LinkedGraphicalType)
            .forEach(type -> row.set(type, true));
      }
      if (isImagingFile && newFeature instanceof ModularFeature f) {
        // activate image for this feature
        DataTypeUtils.DEFAULT_IMAGING_COLUMNS_FEATURE.stream()
            .filter(type -> type instanceof LinkedGraphicalType).forEach(type -> f.set(type, true));
      }
    }
  }

  default void addRowMaps(R2RNetworkingMaps maps) {
    var master = getRowMaps();
    master.addAll(maps);
  }

  void removeRows(Set<FeatureListRow> rowsToRemove);

  /**
   * TODO: extract interface and rename to AppliedMethod. Not doing it now to avoid merge
   * conflicts.
   */
  public interface FeatureListAppliedMethod {

    @NotNull
    public String getDescription();

    /**
     * This {@link FeatureListAppliedMethod} stores a clone of the original parameter set.
     *
     * @return A clone of the parameter set stored in this object, so the stored values cannot be
     * edited.
     */
    @NotNull
    public ParameterSet getParameters();

    public MZmineModule getModule();

    public Instant getModuleCallDate();

    public void saveValueToXML(Element element);
  }
}
