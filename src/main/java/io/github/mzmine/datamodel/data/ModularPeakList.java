package io.github.mzmine.datamodel.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;

public class ModularPeakList implements PeakList {

  public static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private final ArrayList<ModularFeatureListRow> peakListRows;
  private final List<RawDataFile> dataFiles;
  private String name;
  private List<PeakListAppliedMethod> descriptionOfAppliedTasks;
  private String dateCreated;
  private Range<Double> mzRange;
  private Range<Float> rtRange;
  private double maxDataPointIntensity = 0;



  public ModularPeakList(String name, RawDataFile... dataFiles) {
    if ((dataFiles == null) || (dataFiles.length == 0)) {
      throw (new IllegalArgumentException("Cannot create a feature list with no data files"));
    }
    this.name = name;
    this.dataFiles = new ArrayList<>(dataFiles.length);
    for (RawDataFile raw : dataFiles) {
      this.dataFiles.add(raw);
    }
    peakListRows = new ArrayList<>();
    descriptionOfAppliedTasks = new ArrayList<>();
    dateCreated = DATA_FORMAT.format(new Date());
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
    return stream().filter(row -> rtRange.contains(row.getRT()) && mzRange.contains(row.getMZ()))
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
    if (row.getHeight() > maxDataPointIntensity) {
      maxDataPointIntensity = row.getHeight();
    }

    if (mzRange == null) {
      mzRange = Range.singleton(row.getMZ());
      rtRange = Range.singleton(row.getRT());
    } else {
      mzRange = mzRange.span(Range.singleton(row.getMZ()));
      rtRange = rtRange.span(Range.singleton(row.getRT()));
    }
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
    return stream().map(ModularFeatureListRow::getFeatures).map(map -> map.get(raw))
        .filter(Objects::nonNull)
        .filter(f -> rtRange.contains(f.getRT()) && mzRange.contains(f.getMZ()))
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
    maxDataPointIntensity = 0;
    mzRange = null;
    rtRange = null;
    for (ModularFeatureListRow peakListRow : peakListRows) {
      if (peakListRow.getHeight() > maxDataPointIntensity)
        maxDataPointIntensity = peakListRow.getHeight();

      if (mzRange == null) {
        mzRange = Range.singleton(peakListRow.getMZ());
        rtRange = Range.singleton(peakListRow.getRT());
      } else {
        mzRange = mzRange.span(Range.singleton(peakListRow.getMZ()));
        rtRange = rtRange.span(Range.singleton(peakListRow.getRT()));
      }
    }
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
    return maxDataPointIntensity;
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
