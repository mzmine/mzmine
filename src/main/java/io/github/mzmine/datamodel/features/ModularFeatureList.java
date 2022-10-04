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

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.CachedIMSFrame;
import io.github.mzmine.modules.io.projectload.CachedIMSRawDataFile;
import io.github.mzmine.project.impl.ProjectChangeEvent;
import io.github.mzmine.util.CorrelationGroupingUtils;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@SuppressWarnings("rawtypes")
public class ModularFeatureList implements FeatureList {

  public static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  private static final Logger logger = Logger.getLogger(ModularFeatureList.class.getName());
  /**
   * The storage of this feature list. May be null if data points of features shall be stored in
   * ram.
   */
  @Nullable
  private final MemoryMapStorage memoryMapStorage;
  // bindings for values
  private final Map<DataType<?>, List<DataTypeValueChangeListener<?>>> featureTypeListeners = new HashMap<>();
  private final Map<DataType<?>, List<DataTypeValueChangeListener<?>>> rowTypeListeners = new HashMap<>();

  // unmodifiable list
  private final ObservableList<RawDataFile> dataFiles;
  private final ObservableMap<RawDataFile, List<? extends Scan>> selectedScans;
  // columns: summary of all
  // using LinkedHashMaps to save columns order according to the constructor
  // TODO do we need two maps? We could have ObservableMap of LinkedHashMap
  private final ObservableMap<Class<? extends DataType>, DataType> rowTypes = FXCollections.observableMap(
      new LinkedHashMap<>());
  // TODO do we need two maps? We could have ObservableMap of LinkedHashMap
  private final ObservableMap<Class<? extends DataType>, DataType> featureTypes = FXCollections.observableMap(
      new LinkedHashMap<>());
  private final ObservableList<FeatureListRow> featureListRows;
  private final ObservableList<FeatureListAppliedMethod> descriptionOfAppliedTasks;
  // a map that stores row-2-row relationship maps for MS1, MS2, and other relationships
  private final Map<RowsRelationship.Type, R2RMap<RowsRelationship>> r2rMaps = new ConcurrentHashMap<>();
  @NotNull
  private String nameProperty = "";
  private String dateCreated;
  // grouping
  private List<RowGroup> groups;


  public ModularFeatureList(String name, @Nullable MemoryMapStorage storage,
      @NotNull RawDataFile... dataFiles) {
    this(name, storage, List.of(dataFiles));
  }

  public ModularFeatureList(String name, @Nullable MemoryMapStorage storage,
      @NotNull List<RawDataFile> dataFiles) {
    setName(name);
    this.dataFiles = FXCollections.observableList(dataFiles);
    featureListRows = FXCollections.observableArrayList();
    descriptionOfAppliedTasks = FXCollections.observableArrayList();
    dateCreated = DATA_FORMAT.format(new Date());
    selectedScans = FXCollections.observableMap(new HashMap<>());
    this.memoryMapStorage = storage;

    // only a few standard types
    addRowType(new IDType());
    addRowType(new ManualAnnotationType());
    addDefaultListeners();
  }

  private void addDefaultListeners() {
    addFeatureTypeListener(new FeatureDataType(), (dataModel, type, oldValue, newValue) -> {
      // check feature data for graphical columns
      DataTypeUtils.applyFeatureSpecificGraphicalTypes((ModularFeature) dataModel);
    });
  }

  @Override
  public @NotNull String getName() {
    return nameProperty;
  }

  /**
   * Checks for safe path encoding and no duplicate names in project
   *
   * @param name the new name candidate
   * @return the actually set name
   */
  @Override
  public String setName(@NotNull String name) {
    if (name.isBlank() || name.equals(this.nameProperty)) {
      // keep old name
      return this.nameProperty;
    }

    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

    if (project != null) {
      // project finds the name and calls the setNameNoChecks method
      project.setUniqueFeatureListName(this, name);
    } else {
      setNameNoChecks(FileAndPathUtil.safePathEncode(name));
    }
    return this.nameProperty;
  }

  @Override
  public String setNameNoChecks(@NotNull String name) {
    this.nameProperty = name;

    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    if (project != null) {
      project.fireFeatureListsChangeEvent(List.of(this), ProjectChangeEvent.Type.RENAMED);
    }
    return nameProperty;
  }

  @Override
  public String toString() {
    return getName();
  }

  /**
   * The selected scans to build this feature/chromatogram
   *
   * @param file  the data file of the scans
   * @param scans all filtered scans that were used to build the chromatogram in the first place.
   *              For ion mobility data, the Frames are returned
   */
  public void setSelectedScans(@NotNull RawDataFile file, @Nullable List<? extends Scan> scans) {
    selectedScans.put(file, scans);
  }

  /**
   * @param file the data file
   * @return The scans used to build this feature list. For ion mobility data, the frames are
   * returned.
   */
  @Nullable
  public List<? extends Scan> getSeletedScans(@NotNull RawDataFile file) {
    return selectedScans.get(file);
  }

  /**
   * Bind row types to feature types to calculate averages, sums, min, max, counts.
   *
   * @param bindings list of bindings
   */
  @Override
  public void addRowBinding(@NotNull List<RowBinding> bindings) {
    for (RowBinding b : bindings) {
      addFeatureTypeListener(b.getFeatureType(), b);
      // add missing row types, that are based on RowBindings
      addRowType(b.getRowType());
      // apply to all rows
      for (FeatureListRow row : getRows()) {
        b.apply(row);
      }
    }
  }

  /**
   * Add a listener for a feature DataType
   *
   * @param featureType data type that is present in the feature types
   * @param listener    the listener for value changes
   */
  @Override
  public void addFeatureTypeListener(DataType featureType, DataTypeValueChangeListener listener) {
    featureTypeListeners.compute(featureType, (key, list) -> {
      if (list == null) {
        list = new ArrayList<>();
      }
      list.add(listener);
      return list;
    });
  }

  /**
   * Add a listener for a FeatureListRow DataType
   *
   * @param rowType  data type that is present in the FeatureListRow types
   * @param listener the listener for value changes
   */
  @Override
  public void addRowTypeListener(DataType rowType, DataTypeValueChangeListener listener) {
    rowTypeListeners.compute(rowType, (key, list) -> {
      if (list == null) {
        list = new ArrayList<>();
      }
      list.add(listener);
      return list;
    });
  }

  /**
   * Removes a listener for a FeatureListRow DataType
   *
   * @param rowType  data type that is present in the FeatureListRow types
   * @param listener the listener for value changes
   */
  @Override
  public void removeRowTypeListener(DataType rowType, DataTypeValueChangeListener listener) {
    rowTypeListeners.compute(rowType, (key, list) -> {
      if (list == null || list.isEmpty()) {
        return null;
      }
      list.remove(listener);
      return list.isEmpty() ? null : list;
    });
  }

  /**
   * Removes a listener for a Feature DataType
   *
   * @param featureType data type that is present in the Feature types
   * @param listener    the listener for value changes
   */
  @Override
  public void removeFeatureTypeListener(DataType featureType,
      DataTypeValueChangeListener listener) {
    featureTypeListeners.compute(featureType, (key, list) -> {
      if (list == null || list.isEmpty()) {
        return null;
      }
      list.remove(listener);
      return list.isEmpty() ? null : list;
    });
  }

  @Override
  public void applyRowBindings() {
    for (var row : featureListRows) {
      applyRowBindings(row);
    }
  }

  @Override
  public void applyRowBindings(FeatureListRow row) {
    for (var listeners : featureTypeListeners.values()) {
      for (var listener : listeners) {
        if (listener instanceof RowBinding bind) {
          bind.apply(row);
        }
      }
    }
  }

  /**
   * Summary of all feature type columns
   *
   * @return feature types (columns)
   */
  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getFeatureTypes() {
    return featureTypes;
  }

  @Override
  public void addFeatureType(Collection<DataType> types) {
    for (DataType<?> type : types) {
      if (!featureTypes.containsKey(type.getClass())) {
        // all {@link ModularFeature} will automatically add a default data map
        featureTypes.put(type.getClass(), type);
        // add row bindings
        addRowBinding(type.createDefaultRowBindings());
      }
    }
  }

  @Override
  public void addFeatureType(@NotNull DataType<?>... types) {
    addFeatureType(Arrays.asList(types));
  }

  @Override
  public void addRowType(Collection<DataType> types) {
    for (DataType<?> type : types) {
      if (!rowTypes.containsKey(type.getClass())) {
        // add row type - all rows will automatically generate a default property for this type in
        // their data map
        rowTypes.put(type.getClass(), type);
      }
    }
  }

  @Override
  public void addRowType(@NotNull DataType<?>... types) {
    addRowType(Arrays.asList(types));
  }

  /**
   * Row type columns
   *
   * @return row types (columns)
   */
  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getRowTypes() {
    return rowTypes;
  }

  /**
   * Checks if typeClass was added as a FeatureType
   *
   * @param typeClass class of a DataType
   * @return true if feature type is available
   */
  @Override
  public boolean hasFeatureType(Class typeClass) {
    return getFeatureTypes().containsKey(typeClass);
  }

  /**
   * Checks if typeClass was added as a row type
   *
   * @param typeClass class of a DataType
   * @return true if row type is available
   */
  @Override
  public boolean hasRowType(Class typeClass) {
    return getRowTypes().containsKey(typeClass);
  }

  /**
   * Returns number of raw data files participating in the alignment
   */
  @Override
  public int getNumberOfRawDataFiles() {
    return dataFiles.size();
  }

  /**
   * Returns all raw data files participating in the alignment
   *
   * @return the raw data files for this list
   */
  @Override
  public ObservableList<RawDataFile> getRawDataFiles() {
    return dataFiles;
  }

  @Override
  public RawDataFile getRawDataFile(int i) {
    if (i >= 0 && i < dataFiles.size()) {
      return dataFiles.get(i);
    } else {
      return null;
    }
  }

  /**
   * Returns number of rows
   */
  @Override
  public int getNumberOfRows() {
    return featureListRows.size();
  }

  /**
   * Returns the feature of a given raw data file on a give row of the alignment result
   *
   * @param row Row of the alignment result
   * @param raw Raw data file where the feature is detected/estimated
   */
  @Override
  public ModularFeature getFeature(int row, RawDataFile raw) {
    return ((ModularFeatureListRow) featureListRows.get(row)).getFilesFeatures().get(raw);
  }

  /**
   * Returns all features for a raw data file
   */
  @Override
  public List<ModularFeature> getFeatures(RawDataFile raw) {
    List<ModularFeature> features = new ArrayList<>();
    for (int row = 0; row < getNumberOfRows(); row++) {
      ModularFeature f = getFeature(row, raw);
      if (f != null) {
        features.add(f);
      }
    }
    return features;
  }

  /**
   * Returns all features on one row
   */
  @Override
  public FeatureListRow getRow(int row) {
    return featureListRows.get(row);
  }

  @Override
  public ObservableList<FeatureListRow> getRows() {
    return featureListRows;
  }

  @Override
  public void setRows(FeatureListRow... rows) {
    Set<RawDataFile> fileSet = new HashSet<>();
    for (FeatureListRow row : rows) {
      if (!(row instanceof ModularFeatureListRow)) {
        throw new IllegalArgumentException(
            "Can not add non-modular feature list row to modular feature list");
      }
      for (var raw : row.getRawDataFiles()) {
        fileSet.add(raw);
      }
    }

    // check that all files are represented
    final List<RawDataFile> rawFiles = getRawDataFiles();
    for (var raw : fileSet) {
      if (!rawFiles.contains(raw)) {
        throw (new IllegalArgumentException("Data file " + raw + " is not in this feature list"));
      }
    }
//    logger.log(Level.FINEST, "SET ALL ROWS");
    featureListRows.clear();
    featureListRows.addAll(rows);
    applyRowBindings();
  }

  @Override
  public List<FeatureListRow> getRowsInsideMZRange(Range<Double> mzRange) {
    Range<Float> all = Range.all();
    return getRowsInsideScanAndMZRange(all, mzRange);
  }

  @Override
  public List<FeatureListRow> getRowsInsideScanRange(Range<Float> rtRange) {
    Range<Double> all = Range.all();
    return getRowsInsideScanAndMZRange(rtRange, all);
  }

  @Override
  public List<FeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange,
      Range<Double> mzRange) {
    List<FeatureListRow> rows = new ArrayList<>();
    for(var row : getRows()) {
      Float rt = row.getAverageRT();
      if(rt==null || (rtRange.contains(rt) && mzRange.contains(row.getAverageMZ()))) {
        rows.add(row);
      } else if(rt>rtRange.upperEndpoint()) {
        break;
      }
    }
    return rows;
  }

  @Override
  public void addRow(FeatureListRow row) {
    if (!(row instanceof ModularFeatureListRow modularRow)) {
      throw new IllegalArgumentException(
          "Can not add non-modular feature list row to modular feature list");
    }

    ObservableList<RawDataFile> myFiles = this.getRawDataFiles();
    for (RawDataFile testFile : modularRow.getRawDataFiles()) {
      if (!myFiles.contains(testFile)) {
        throw (new IllegalArgumentException(
            "Data file " + testFile + " is not in this feature list"));
      }
    }
    //    logger.finest("ADD ROW");
    featureListRows.add(modularRow);
    applyRowBindings(modularRow);

    // TODO solve with bindings
    // max intensity
    // ranges
  }

  /**
   * Returns all features overlapping with a retention time range
   *
   * @param rtRange Retention time range
   * @return List of features
   */
  @Override
  public List<Feature> getFeaturesInsideScanRange(RawDataFile raw, Range<Float> rtRange) {
    Range<Double> all = Range.all();
    return getFeaturesInsideScanAndMZRange(raw, rtRange, all);
  }

  /**
   * @see FeatureList#getFeaturesInsideMZRange
   */
  @Override
  public List<Feature> getFeaturesInsideMZRange(RawDataFile raw, Range<Double> mzRange) {
    Range<Float> all = Range.all();
    return getFeaturesInsideScanAndMZRange(raw, all, mzRange);
  }

  /**
   * @see FeatureList#getFeaturesInsideScanAndMZRange
   */
  @Override
  public List<Feature> getFeaturesInsideScanAndMZRange(RawDataFile raw, Range<Float> rtRange,
      Range<Double> mzRange) {
    // TODO solve with bindings and check for rt or mz presence in row
    return modularStream().map(ModularFeatureListRow::getFilesFeatures).map(map -> map.get(raw))
        .filter(Objects::nonNull)
        .filter(f -> rtRange.contains(f.getRT()) && mzRange.contains(f.getMZ()))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
  }

  /**
   *
   */
  @Override
  public void removeRow(FeatureListRow row) {
    // remove buffered charts, otherwise the reference is kept alive. What references the row, though?
    ((ModularFeatureListRow) row).clearBufferedColCharts();
    //    logger.finest("REMOVE ROW");
    featureListRows.remove(row);
  }

  /**
   * if available pass index and row {@see #removeRow(int, FeatureListRow)} for optimized version.
   */
  @Override
  public void removeRow(int rowNum) {
    removeRow(featureListRows.get(rowNum));
  }

  /**
   *
   */
  @Override
  public void removeRow(int rowNum, FeatureListRow row) {
    removeRow(featureListRows.get(rowNum));
    // remove buffered charts, otherwise the reference is kept alive. What references the row, though?
    ((ModularFeatureListRow) row).clearBufferedColCharts();
    featureListRows.remove(rowNum);
  }

  @Override
  public Stream<FeatureListRow> stream() {
    return featureListRows.stream();
  }

  public Stream<ModularFeatureListRow> modularStream() {
    return featureListRows.stream().map(row -> (ModularFeatureListRow) row);
  }

  @Override
  public Stream<FeatureListRow> parallelStream() {
    return featureListRows.parallelStream();
  }

  public Stream<ModularFeatureListRow> modularParallelStream() {
    return featureListRows.parallelStream().map(row -> (ModularFeatureListRow) row);
  }

  @Override
  public Stream<ModularFeature> streamFeatures() {
    return stream().flatMap(row -> row.getFeatures().stream()).filter(Objects::nonNull);
  }

  @Override
  public Stream<ModularFeature> parallelStreamFeatures() {
    return parallelStream().flatMap(row -> row.getFeatures().stream()).filter(Objects::nonNull);
  }


  /**
   * @see FeatureList#getFeatureListRowNum(Feature)
   */
  @Override
  public int getFeatureListRowNum(Feature feature) {
    for (int i = 0; i < featureListRows.size(); i++) {
      if (featureListRows.get(i).hasFeature(feature)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * @see FeatureList#getDataPointMaxIntensity()
   */
  @Override
  public double getDataPointMaxIntensity() {
    // TODO max intensity by binding
    return 0;
  }

  @Override
  public boolean hasRawDataFile(RawDataFile hasFile) {
    return dataFiles.contains(hasFile);
  }

  @Override
  public FeatureListRow getFeatureRow(Feature feature) {
    return stream().filter(row -> row.hasFeature(feature)).findFirst().orElse(null);
  }

  @Override
  public FeatureListRow findRowByID(int id) {
    List<FeatureListRow> featureListRows = stream().filter(r -> r.getID() == id).toList();
    if (featureListRows.isEmpty()) {
      return null;
    }

    if (featureListRows.size() > 1) {
      logger.info("more than one row with id " + id);
    }

    return featureListRows.get(0);
  }

  @Override
  public void addDescriptionOfAppliedTask(FeatureListAppliedMethod appliedMethod) {
    descriptionOfAppliedTasks.add(appliedMethod);
  }

  @Override
  public ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return descriptionOfAppliedTasks;
  }

  @Override
  public String getDateCreated() {
    return dateCreated;
  }

  @Override
  public void setDateCreated(String date) {
    this.dateCreated = date;
  }


  // TODO: if this method would be called frequently, then store and update whole mz range in
  //  a private variable during rows initialization
  @Override
  public Range<Double> getRowsMZRange() {
    if (getRows().isEmpty()) {
      return Range.singleton(0d);
    }

    DoubleSummaryStatistics mzStatistics = getRows().stream().map(FeatureListRow::getAverageMZ)
        .collect(Collectors.summarizingDouble((Double::doubleValue)));

    return Range.closed(mzStatistics.getMin(), mzStatistics.getMax());
  }

  // TODO: if this method would be called frequently, then store and update whole rt range in
  //  a private variable during rows initialization
  @Override
  public Range<Float> getRowsRTRange() {
    if (getRows().isEmpty()) {
      return Range.singleton(0f);
    }

    DoubleSummaryStatistics rtStatistics = getRows().stream()
        .map(row -> (double) (row).getAverageRT())
        .collect(Collectors.summarizingDouble((Double::doubleValue)));

    return Range.closed((float) rtStatistics.getMin(), (float) rtStatistics.getMax());
  }

  @Override
  public List<RowGroup> getGroups() {
    return groups;
  }

  @Override
  public void setGroups(List<RowGroup> groups) {
    this.groups = groups;
    CorrelationGroupingUtils.setGroupsToAllRows(groups);
  }

  @NotNull
  public Map<Type, R2RMap<RowsRelationship>> getRowMaps() {
    return r2rMaps;
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getFeatureTypeChangeListeners() {
    return featureTypeListeners;
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getRowTypeChangeListeners() {
    return rowTypeListeners;
  }

  @Override
  public void addRowsRelationships(R2RMap<? extends RowsRelationship> map, Type relationship) {
    R2RMap<RowsRelationship> rowMap = r2rMaps.computeIfAbsent(relationship, key -> new R2RMap<>());
    rowMap.putAll(map);
  }

  @Override
  public void addRowsRelationship(FeatureListRow a, FeatureListRow b,
      RowsRelationship relationship) {
    R2RMap<RowsRelationship> rowMap = r2rMaps.computeIfAbsent(relationship.getType(),
        key -> new R2RMap<>());
    rowMap.add(a, b, relationship);
  }

  /**
   * create copy of all feature list rows and features
   *
   * @param title       the new title
   * @param renumberIDs true: renumber row IDs or false: use original IDs
   * @return a copy of the orginal feature list
   */
  public ModularFeatureList createCopy(String title, @Nullable MemoryMapStorage storage,
      boolean renumberIDs) {
    return createCopy(title, storage, getRawDataFiles(), renumberIDs);
  }


  /**
   * create copy of all feature list rows and features. Use a different list of raw data files. The
   * new list of raw data files might be used by alignment modules to create a copy of a base
   * feature list and then add all the other feature lists to it.
   *
   * @param title       the new title
   * @param dataFiles   the new list of raw data files
   * @param renumberIDs true: renumber row IDs or false: use original IDs
   * @return a copy of the orginal feature list
   */
  public ModularFeatureList createCopy(String title, @Nullable MemoryMapStorage storage,
      List<RawDataFile> dataFiles, boolean renumberIDs) {
    ModularFeatureList flist = new ModularFeatureList(title, storage, dataFiles);

    // key is original row and value is copied row
    Map<FeatureListRow, ModularFeatureListRow> mapCopied = new HashMap<>();
    // copy all rows and features
    int id = 0;
    for (FeatureListRow row : this.getRows()) {
      id = renumberIDs ? id + 1 : row.getID();
      ModularFeatureListRow copyRow = new ModularFeatureListRow(flist, id,
          (ModularFeatureListRow) row, true);
      flist.addRow(copyRow);
      mapCopied.put(row, copyRow);
    }

    // todo copy all row to row relationships and exchange row references in datatypes

    // change references in IIN

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : this.getAppliedMethods()) {
      flist.addDescriptionOfAppliedTask(proc);
    }

    selectedScans.forEach(flist::setSelectedScans);
    return flist;
  }

  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return memoryMapStorage;
  }

  /**
   * Replaces {@link CachedIMSRawDataFile}s and {@link CachedIMSFrame}s in the selected scans and
   * raw data files of this feature list. Cached files are used during feature list import to avoid
   * multiple copies of {@link io.github.mzmine.datamodel.MobilityScan}s, since the main
   * implementation ({@link io.github.mzmine.datamodel.impl.StoredMobilityScan}) is created on
   * demand and passed through data types.
   * <p></p>
   * After the project import, the files have to be replaced to lower ram consumption and allow
   * further processing.
   */
  public void replaceCachedFilesAndScans() {
    for (int i = 0; i < getNumberOfRawDataFiles(); i++) {
      RawDataFile file = getRawDataFile(i);
      if (file instanceof IMSRawDataFile imsfile) {
        if (imsfile instanceof CachedIMSRawDataFile cached) {
          dataFiles.set(i, cached.getOriginalFile());

          List<? extends Scan> scans = selectedScans.remove(cached);
          List<Frame> frames = scans.stream()
              .map(scan -> ((CachedIMSFrame) scan).getOriginalFrame()).toList();
          selectedScans.put(cached.getOriginalFile(), frames);
        }
      }
    }
  }
}
