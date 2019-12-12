package io.github.mzmine.datamodel.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.RawFileType;
import io.github.mzmine.datamodel.data.types.numbers.IDType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class ModularFeatureList implements PeakList {

  // columns: summary of all
  private final ObservableMap<Class<? extends DataType>, DataType> rowTypes =
      FXCollections.observableHashMap();
  private final ObservableMap<Class<? extends DataType>, DataType> featureTypes =
      FXCollections.observableHashMap();

  public static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  // unmodifiable list
  private final List<RawDataFile> dataFiles;
  private final ArrayList<ModularFeatureListRow> peakListRows;
  private String name;
  private List<PeakListAppliedMethod> descriptionOfAppliedTasks;
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
    this.dataFiles = Collections.unmodifiableList(dataFiles);
    peakListRows = new ArrayList<>();
    descriptionOfAppliedTasks = new ArrayList<>();
    dateCreated = DATA_FORMAT.format(new Date());

    addRowType(new IDType());
    // has raw files - add column to row and feature
    if (!dataFiles.isEmpty()) {
      addRowType(new FeaturesType());
      addFeatureType(new RawFileType());
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
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
      if (!getFeatureTypes().containsKey(type.getClass())) {
        getFeatureTypes().put(type.getClass(), type);
        // add to maps
        streamFeatures().forEach(f -> {
          f.setProperty(type, type.createProperty());
        });
      }
    }
  }

  public void addFeatureType(@Nonnull DataType<?>... types) {
    addFeatureType(Arrays.asList(types));
  }

  public void addRowType(@Nonnull List<DataType<?>> types) {
    for (DataType<?> type : types) {
      if (!getRowTypes().containsKey(type.getClass())) {
        getRowTypes().put(type.getClass(), type);
        // add type columns to maps
        stream().forEach(row -> {
          row.setProperty(type, type.createProperty());
        });
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
   */
  @Override
  public List<RawDataFile> getRawDataFiles() {
    return dataFiles;
  }

  @Override
  public RawDataFile getRawDataFile(int i) {
    if (i >= 0 && i < dataFiles.size())
      return dataFiles.get(i);
    else
      return null;
  }

  /**
   * Returns number of rows
   */
  @Override
  public int getNumberOfRows() {
    return peakListRows.size();
  }

  /**
   * Returns the peak of a given raw data file on a give row of the alignment result
   * 
   * @param row Row of the alignment result
   * @param rawDataFile Raw data file where the peak is detected/estimated
   */
  @Override
  public ModularFeature getPeak(int row, RawDataFile raw) {
    return peakListRows.get(row).getFeatures().get(raw);
  }

  /**
   * Returns all peaks for a raw data file
   */
  @Override
  public List<ModularFeature> getPeaks(RawDataFile raw) {
    List<ModularFeature> peakSet = new ArrayList<>();
    for (int row = 0; row < getNumberOfRows(); row++) {
      ModularFeature f = getPeak(row, raw);
      if (f != null)
        peakSet.add(f);
    }
    return peakSet;
  }

  /**
   * Returns all peaks on one row
   */
  @Override
  public ModularFeatureListRow getRow(int row) {
    return peakListRows.get(row);
  }

  @Override
  public List<ModularFeatureListRow> getRows() {
    return peakListRows;
  }

  @Override
  public List<ModularFeatureListRow> getRowsInsideMZRange(Range<Double> mzRange) {
    Range<Float> all = Range.all();
    return getRowsInsideScanAndMZRange(all, mzRange);
  }

  @Override
  public List<ModularFeatureListRow> getRowsInsideScanRange(Range<Float> rtRange) {
    Range<Double> all = Range.all();
    return getRowsInsideScanAndMZRange(rtRange, all);
  }

  @Override
  public List<ModularFeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange,
      Range<Double> mzRange) {
    // TODO handle if mz or rt is not present
    return stream()
        .filter(row -> rtRange.contains(row.getRT().get()) && mzRange.contains(row.getMZ().get()))
        .collect(Collectors.toList());
  }

  @Override
  public void addRow(ModularFeatureListRow row) {
    List<RawDataFile> myFiles = this.getRawDataFiles();
    for (RawDataFile testFile : row.getRawDataFiles()) {
      if (!myFiles.contains(testFile))
        throw (new IllegalArgumentException(
            "Data file " + testFile + " is not in this feature list"));
    }

    peakListRows.add(row);

    // TODO solve with bindings
    // max intensity
    // ranges
  }

  /**
   * Returns all peaks overlapping with a retention time range
   * 
   * @param startRT Start of the retention time range
   * @param endRT End of the retention time range
   * @return
   */
  @Override
  public List<ModularFeature> getPeaksInsideScanRange(RawDataFile raw, Range<Float> rtRange) {
    Range<Double> all = Range.all();
    return getPeaksInsideScanAndMZRange(raw, rtRange, all);
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#getPeaksInsideMZRange(double, double)
   */
  @Override
  public List<ModularFeature> getPeaksInsideMZRange(RawDataFile raw, Range<Double> mzRange) {
    Range<Float> all = Range.all();
    return getPeaksInsideScanAndMZRange(raw, all, mzRange);
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#getPeaksInsideScanAndMZRange(double, double, double,
   *      double)
   */
  @Override
  public List<ModularFeature> getPeaksInsideScanAndMZRange(RawDataFile raw, Range<Float> rtRange,
      Range<Double> mzRange) {
    // TODO solve with bindings and check for rt or mz presence in row
    return stream().map(ModularFeatureListRow::getFeatures).map(map -> map.get(raw))
        .filter(Objects::nonNull)
        .filter(f -> rtRange.contains(f.getRT().get()) && mzRange.contains(f.getMZ().get()))
        .collect(Collectors.toList());
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#removeRow(io.github.mzmine.datamodel.PeakListRow)
   */
  @Override
  public void removeRow(ModularFeatureListRow row) {
    peakListRows.remove(row);
    updateMaxIntensity();
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#removeRow(io.github.mzmine.datamodel.PeakListRow)
   */
  @Override
  public void removeRow(int rowNum) {
    removeRow(peakListRows.get(rowNum));
  }

  private void updateMaxIntensity() {
    // TODO
    // binding
  }

  @Override
  public Stream<ModularFeatureListRow> stream() {
    return peakListRows.stream();
  }

  @Override
  public Stream<ModularFeatureListRow> parallelStream() {
    return peakListRows.parallelStream();
  }

  @Override
  public Stream<ModularFeature> streamFeatures() {
    return stream().flatMap(row -> row.getFeatures().values().stream()).filter(Objects::nonNull);
  }

  @Override
  public Stream<ModularFeature> parallelStreamFeatures() {
    return parallelStream().flatMap(row -> row.getFeatures().values().stream())
        .filter(Objects::nonNull);
  }



  /**
   * @see io.github.mzmine.datamodel.PeakList#getPeakRowNum(io.github.mzmine.datamodel.Feature)
   */
  @Override
  public int getPeakRowNum(ModularFeature feature) {
    for (int i = 0; i < peakListRows.size(); i++) {
      if (peakListRows.get(i).hasFeature(feature))
        return i;
    }
    return -1;
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#getDataPointMaxIntensity()
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
  public ModularFeatureListRow getPeakRow(ModularFeature feature) {
    return stream().filter(row -> row.hasFeature(feature)).findFirst().orElse(null);
  }

  @Override
  public ModularFeatureListRow findRowByID(int id) {
    return stream().filter(r -> r.getID() == id).findFirst().orElse(null);
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void addDescriptionOfAppliedTask(PeakListAppliedMethod appliedMethod) {
    descriptionOfAppliedTasks.add(appliedMethod);
  }

  @Override
  public List<PeakListAppliedMethod> getAppliedMethods() {
    return descriptionOfAppliedTasks;
  }

  public String getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(String date) {
    this.dateCreated = date;
  }

  @Override
  public Range<Double> getRowsMZRange() {
    updateMaxIntensity(); // Update range before returning value
    return mzRange;
  }

  @Override
  public Range<Float> getRowsRTRange() {
    updateMaxIntensity(); // Update range before returning value
    return rtRange;
  }

}
