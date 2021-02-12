package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.impl.AbstractStorableSpectrum;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class ModularFeatureList implements FeatureList {

  public static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  // bindings for values
  private final List<RowBinding> rowBindings = new ArrayList<>();
  // unmodifiable list
  private final ObservableList<RawDataFile> dataFiles;
  private final ObservableMap<RawDataFile, List<? extends Scan>> selectedScans;
  // storage of data points in case features are edited
  private MemoryMapStorage memoryMapStorage = null;
  private boolean forceFeatureDataIntoRam = false;
  // columns: summary of all
  // using LinkedHashMaps to save columns order according to the constructor
  // TODO do we need two maps? We could have ObservableMap of LinkedHashMap
  private ObservableMap<Class<? extends DataType>, DataType> rowTypes =
      FXCollections.observableMap(new LinkedHashMap<>());
  // TODO do we need two maps? We could have ObservableMap of LinkedHashMap
  private ObservableMap<Class<? extends DataType>, DataType> featureTypes =
      FXCollections.observableMap(new LinkedHashMap<>());
  private ObservableList<FeatureListRow> featureListRows;
  private String name;
  private ObservableList<FeatureListAppliedMethod> descriptionOfAppliedTasks;
  private String dateCreated;
  private Range<Double> mzRange;
  private Range<Float> rtRange;

  public ModularFeatureList(String name) {
    this(name, List.of());
  }

  public ModularFeatureList(String name, @Nonnull RawDataFile... dataFiles) {
    this(name, List.of(dataFiles));
  }

  public ModularFeatureList(String name, @Nonnull List<RawDataFile> dataFiles) {
    this.name = name;
    this.dataFiles = FXCollections.observableList(dataFiles);
    featureListRows = FXCollections.observableArrayList();
    descriptionOfAppliedTasks = FXCollections.observableArrayList();
    dateCreated = DATA_FORMAT.format(new Date());
    selectedScans = FXCollections.observableMap(new HashMap<>());

    // only a few standard types
    addRowType(new IDType());
    addRowType(new ManualAnnotationType());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public void setSelectedScans(@Nonnull RawDataFile file, @Nullable List<? extends Scan> scans) {
    selectedScans.put(file, scans);
  }

  /**
   * @param file
   * @return The scans used to build this feature list. For ion mobility data, the frames are
   * returned.
   */
  @Nullable
  public List<? extends Scan> getSeletedScans(@Nonnull RawDataFile file) {
    return selectedScans.get(file);
  }

  /**
   * Bind row types to feature types to calculate averages, sums, min, max, counts.
   *
   * @param bindings list of bindings
   */
  public void addRowBinding(@Nonnull List<RowBinding> bindings) {
    for (RowBinding b : bindings) {
      rowBindings.add(b);
      // add missing row types, that are based on RowBindings
      addRowType(b.getRowType());
      // apply to all rows
      modularStream().forEach(b::apply);
    }
  }

  public void addRowBinding(@Nonnull RowBinding... bindings) {
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

  public void addFeatureType(@Nonnull List<DataType<?>> types) {
    for (DataType<?> type : types) {
      if (!featureTypes.containsKey(type.getClass())) {
        // all {@link ModularFeature} will automatically add a default property to their data map
        featureTypes.put(type.getClass(), type);
        // add row bindings
        addRowBinding(type.createDefaultRowBindings());
      }
    }
  }

  public void addFeatureType(@Nonnull DataType<?>... types) {
    addFeatureType(Arrays.asList(types));
  }

  public void addRowType(@Nonnull List<DataType<?>> types) {
    for (DataType<?> type : types) {
      if (!rowTypes.containsKey(type.getClass())) {
        // add row type - all rows will automatically generate a default property for this type in
        // their data map
        rowTypes.put(type.getClass(), type);
      }
    }
  }

  public void addRowType(@Nonnull DataType<?>... types) {
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
  public ObservableList<FeatureListRow> getRowsInsideMZRange(Range<Double> mzRange) {
    Range<Float> all = Range.all();
    return getRowsInsideScanAndMZRange(all, mzRange);
  }

  @Override
  public ObservableList<FeatureListRow> getRowsInsideScanRange(Range<Float> rtRange) {
    Range<Double> all = Range.all();
    return getRowsInsideScanAndMZRange(rtRange, all);
  }

  @Override
  public ObservableList<FeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange,
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
  public ObservableList<Feature> getFeaturesInsideScanRange(RawDataFile raw, Range<Float> rtRange) {
    Range<Double> all = Range.all();
    return getFeaturesInsideScanAndMZRange(raw, rtRange, all);
  }

  /**
   * @see io.github.mzmine.datamodel.features.FeatureList#getFeaturesInsideMZRange
   */
  @Override
  public ObservableList<Feature> getFeaturesInsideMZRange(RawDataFile raw, Range<Double> mzRange) {
    Range<Float> all = Range.all();
    return getFeaturesInsideScanAndMZRange(raw, all, mzRange);
  }

  /**
   * @see io.github.mzmine.datamodel.features.FeatureList#getFeaturesInsideScanAndMZRange
   */
  @Override
  public ObservableList<Feature> getFeaturesInsideScanAndMZRange(RawDataFile raw,
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
   * @see io.github.mzmine.datamodel.features.FeatureList#removeRow(FeatureListRow)
   */
  @Override
  public void removeRow(FeatureListRow row) {
    featureListRows.remove(row);
    updateMaxIntensity();
  }

  /**
   * @see io.github.mzmine.datamodel.features.FeatureList#removeRow(FeatureListRow)
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
   * @see io.github.mzmine.datamodel.features.FeatureList#getFeatureListRowNum(Feature)
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
   * @see io.github.mzmine.datamodel.features.FeatureList#getDataPointMaxIntensity()
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

  /**
   * create copy of all feature list rows and features
   *
   * @param title
   * @return
   */
  public ModularFeatureList createCopy(String title) {
    ModularFeatureList flist = new ModularFeatureList(title, this.getRawDataFiles());
    // copy all rows and features
    this.stream().map(row -> new ModularFeatureListRow(flist, (ModularFeatureListRow) row, true))
        .forEach(newRow -> flist.addRow(newRow));

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

  public boolean getForceFeatureDataIntoRam() {
    return forceFeatureDataIntoRam;
  }

  /**
   * <b>Note</b> Currently unsafe fore feature lists that are added to the project. Causes merging
   * of mobility scans to crash until either {@link AbstractStorableSpectrum} can handle a null
   * storage or we create a central storage for that case.
   * <p></p>
   *
   * @param forceFeatureDataIntoRam Forces that feature data of new features will be stored into ram
   *                                for usage on servers.
   */
  public void setForceFeatureDataIntoRam(boolean forceFeatureDataIntoRam) {
    this.forceFeatureDataIntoRam = forceFeatureDataIntoRam;
  }

  /**
   * @return The {@link MemoryMapStorage}. May be null if {@link  ModularFeatureList#setForceFeatureDataIntoRam(boolean)}
   * is set to true. Downstream modules have to handle this case.
   */
  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    if (memoryMapStorage == null && !forceFeatureDataIntoRam) {
      memoryMapStorage = new MemoryMapStorage();
    }
    return memoryMapStorage;
  }

}
