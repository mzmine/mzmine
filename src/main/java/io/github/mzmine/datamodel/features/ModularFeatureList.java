package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.CorrelationGroupingUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ModularFeatureList implements FeatureList {

  public static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  /**
   * The storage of this feature list. May be null if data points of features shall be stored in
   * ram.
   */
  @Nullable
  private final MemoryMapStorage memoryMapStorage;
  // bindings for values
  private final List<RowBinding> rowBindings = new ArrayList<>();
  // unmodifiable list
  private final ObservableList<RawDataFile> dataFiles;
  private final ObservableMap<RawDataFile, List<? extends Scan>> selectedScans;
  // columns: summary of all
  // using LinkedHashMaps to save columns order according to the constructor
  // TODO do we need two maps? We could have ObservableMap of LinkedHashMap
  private ObservableMap<Class<? extends DataType>, DataType> rowTypes =
      FXCollections.observableMap(new LinkedHashMap<>());
  // TODO do we need two maps? We could have ObservableMap of LinkedHashMap
  private ObservableMap<Class<? extends DataType>, DataType> featureTypes =
      FXCollections.observableMap(new LinkedHashMap<>());
  private ObservableList<FeatureListRow> featureListRows;
  private ObservableList<FeatureListAppliedMethod> descriptionOfAppliedTasks;
  private String dateCreated;
  private Range<Double> mzRange;
  private Range<Float> rtRange;
  @NotNull
  private final StringProperty nameProperty;

  // grouping
  private List<RowGroup> groups;

  // a map that stores row-2-row relationship maps for MS1, MS2, and other relationships
  private Map<RowsRelationship.Type, R2RMap<RowsRelationship>> r2rMaps = new ConcurrentHashMap<>();


  public ModularFeatureList(String name, @Nullable MemoryMapStorage storage,
      @NotNull RawDataFile... dataFiles) {
    this(name, storage, List.of(dataFiles));
  }

  public ModularFeatureList(String name, @Nullable MemoryMapStorage storage,
      @NotNull List<RawDataFile> dataFiles) {
    this.nameProperty = new SimpleStringProperty(name);
    this.dataFiles = FXCollections.observableList(dataFiles);
    featureListRows = FXCollections.observableArrayList();
    descriptionOfAppliedTasks = FXCollections.observableArrayList();
    dateCreated = DATA_FORMAT.format(new Date());
    selectedScans = FXCollections.observableMap(new HashMap<>());
    this.memoryMapStorage = storage;

    // only a few standard types
    addRowType(new IDType());
    addRowType(new ManualAnnotationType());
  }

  @Override
  @NotNull
  public String getNameProperty() {
    return nameProperty.get();
  }

  @Override
  public String getName() {
    return nameProperty.get();
  }

  @Override
  public void setName(String name) {
    MZmineCore.runLater(() -> this.nameProperty.set(name));
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
  public void addRowBinding(@NotNull List<RowBinding> bindings) {
    for (RowBinding b : bindings) {
      rowBindings.add(b);
      // add missing row types, that are based on RowBindings
      addRowType(b.getRowType());
      // apply to all rows
      modularStream().forEach(b::apply);
    }
  }

  public void addRowBinding(@NotNull RowBinding... bindings) {
    addRowBinding(Arrays.asList(bindings));
  }

  /**
   * Apply all bindings to all this row
   *
   * @param row
   */
  private void applyRowBindings(ModularFeatureListRow row) {
    rowBindings.forEach(bind -> bind.apply(row));
  }

  /**
   * Summary of all feature type columns
   *
   * @return
   */
  public ObservableMap<Class<? extends DataType>, DataType> getFeatureTypes() {
    return featureTypes;
  }

  public void addFeatureType(@NotNull List<DataType<?>> types) {
    for (DataType<?> type : types) {
      if (!featureTypes.containsKey(type.getClass())) {
        // all {@link ModularFeature} will automatically add a default property to their data map
        featureTypes.put(type.getClass(), type);
        // add row bindings
        addRowBinding(type.createDefaultRowBindings());
      }
    }
  }

  public void addFeatureType(@NotNull DataType<?>... types) {
    addFeatureType(Arrays.asList(types));
  }

  public void addRowType(@NotNull List<DataType<?>> types) {
    for (DataType<?> type : types) {
      if (!rowTypes.containsKey(type.getClass())) {
        // add row type - all rows will automatically generate a default property for this type in
        // their data map
        rowTypes.put(type.getClass(), type);
      }
    }
  }

  public void addRowType(@NotNull DataType<?>... types) {
    addRowType(Arrays.asList(types));
  }

  /**
   * Row type columns
   *
   * @return
   */
  public ObservableMap<Class<? extends DataType>, DataType> getRowTypes() {
    return rowTypes;
  }

  /**
   * Checks if typeClass was added as a FeatureType - does not check nested types in a {@link
   * ModularType}
   *
   * @param typeClass class of a DataType
   * @return true if feature type is available
   */
  public boolean hasFeatureType(Class typeClass) {
    return getFeatureTypes().containsKey(typeClass);
  }

  /**
   * Checks if typeClass was added as a row type - does not check nested types in a {@link
   * ModularType}
   *
   * @param typeClass class of a DataType
   * @return true if row type is available
   */
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
   * @return
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
  public ObservableList<Feature> getFeatures(RawDataFile raw) {
    ObservableList<Feature> features = FXCollections.observableArrayList();
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
    // TODO handle if mz or rt is not present
    return modularStream().filter(
        row -> rtRange.contains(row.getAverageRT()) && mzRange.contains(row.getAverageMZ()))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
  }

  @Override
  public void addRow(FeatureListRow row) {
    if (!(row instanceof ModularFeatureListRow)) {
      throw new IllegalArgumentException(
          "Can not add non-modular feature list row to modular feature list");
    }
    ModularFeatureListRow modularRow = (ModularFeatureListRow) row;

    ObservableList<RawDataFile> myFiles = this.getRawDataFiles();
    for (RawDataFile testFile : modularRow.getRawDataFiles()) {
      if (!myFiles.contains(testFile)) {
        throw (new IllegalArgumentException(
            "Data file " + testFile + " is not in this feature list"));
      }
    }

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
  public List<Feature> getFeaturesInsideScanAndMZRange(RawDataFile raw,
      Range<Float> rtRange,
      Range<Double> mzRange) {
    // TODO solve with bindings and check for rt or mz presence in row
    return modularStream().map(ModularFeatureListRow::getFilesFeatures).map(map -> map.get(raw))
        .filter(Objects::nonNull)
        .filter(
            f -> rtRange.contains(f.getRT()) && mzRange.contains(f.getMZ()))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
  }

  /**
   * @see FeatureList#removeRow(FeatureListRow)
   */
  @Override
  public void removeRow(FeatureListRow row) {
    featureListRows.remove(row);
    updateMaxIntensity();
  }

  /**
   * @see FeatureList#removeRow(FeatureListRow)
   */
  @Override
  public void removeRow(int rowNum) {
    removeRow(featureListRows.get(rowNum));
  }

  private void updateMaxIntensity() {
    // TODO
    // binding
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
  public Stream<Feature> streamFeatures() {
    return stream().flatMap(row -> row.getFeatures().stream()).filter(Objects::nonNull);
  }

  public Stream<ModularFeature> modularStreamFeatures() {
    return streamFeatures().map(feature -> (ModularFeature) feature);
  }

  @Override
  public Stream<Feature> parallelStreamFeatures() {
    return parallelStream().flatMap(row -> row.getFeatures().stream())
        .filter(Objects::nonNull);
  }

  public Stream<ModularFeature> modularParallelStreamFeatures() {
    return parallelStreamFeatures().map(feature -> (ModularFeature) feature);
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
    return stream().filter(r -> r.getID() == id).findFirst().orElse(null);
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

    updateMaxIntensity(); // Update range before returning value

    DoubleSummaryStatistics mzStatistics = getRows().stream()
        .map(FeatureListRow::getAverageMZ)
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

    updateMaxIntensity(); // Update range before returning value

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
  public void addRowsRelationships(R2RMap<? extends RowsRelationship> map,
      Type relationship) {
    R2RMap<RowsRelationship> rowMap = r2rMaps.computeIfAbsent(relationship, key -> new R2RMap<>());
    rowMap.putAll(map);
  }

  @Override
  public void addRowsRelationship(FeatureListRow a, FeatureListRow b,
      RowsRelationship relationship) {
    R2RMap<RowsRelationship> rowMap = r2rMaps
        .computeIfAbsent(relationship.getType(), key -> new R2RMap<>());
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
          (ModularFeatureListRow) row,
          true);
      flist.addRow(copyRow);
      mapCopied.put(row, copyRow);
    }

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : this.getAppliedMethods()) {
      flist.addDescriptionOfAppliedTask(proc);
    }

    selectedScans.forEach(flist::setSelectedScans);
    return flist;
  }

  public List<RowBinding> getRowBindings() {
    return rowBindings;
  }

  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return memoryMapStorage;
  }

}
