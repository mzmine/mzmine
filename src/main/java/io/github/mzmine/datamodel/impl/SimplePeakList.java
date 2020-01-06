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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Simple implementation of the PeakList interface.
 */
public class SimplePeakList implements PeakList {

  private String name;
  private ObservableList<RawDataFile> dataFiles = FXCollections.observableArrayList();
  private final ObservableList<PeakListRow> peakListRows = FXCollections.observableArrayList();
  private double maxDataPointIntensity = 0;
  private Vector<PeakListAppliedMethod> descriptionOfAppliedTasks;
  private String dateCreated;
  private Range<Double> mzRange, rtRange;

  public static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  public SimplePeakList(String name, RawDataFile dataFile) {
    this(name, new RawDataFile[] {dataFile});
  }

  public SimplePeakList(String name, RawDataFile[] dataFiles) {
    this(name, Arrays.asList(dataFiles));
  }

  public SimplePeakList(String name, Collection<RawDataFile> dataFiles) {
    if ((dataFiles == null) || (dataFiles.size() == 0)) {
      throw (new IllegalArgumentException("Cannot create a feature list with no data files"));
    }
    this.name = name;
    this.dataFiles.addAll(dataFiles);

    descriptionOfAppliedTasks = new Vector<PeakListAppliedMethod>();

    dateCreated = dateFormat.format(new Date());

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
  public ObservableList<RawDataFile> getRawDataFiles() {
    return dataFiles;
  }

  @Override
  public RawDataFile getRawDataFile(int position) {
    return dataFiles.get(position);
  }

  /**
   * Returns number of rows in the alignment result
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
  public Feature getPeak(int row, RawDataFile rawDataFile) {
    return peakListRows.get(row).getPeak(rawDataFile);
  }

  /**
   * Returns all peaks for a raw data file
   */
  @Override
  public List<Feature> getPeaks(final RawDataFile rawDataFile) {
    var result = peakListRows.stream() //
        .map(row -> row.getPeak(rawDataFile)) //
        .filter(Objects::nonNull) //
        .collect(Collectors.toList());
    return result;
  }

  /**
   * Returns all peaks on one row
   */
  @Override
  public PeakListRow getRow(int row) {
    return peakListRows.get(row);
  }

  @Override
  public ObservableList<PeakListRow> getRows() {
    return peakListRows;
  }

  @Override
  public PeakListRow[] getRowsInsideMZRange(Range<Double> mzRange) {
    Range<Double> all = Range.all();
    return getRowsInsideScanAndMZRange(all, mzRange);
  }

  @Override
  public PeakListRow[] getRowsInsideScanRange(Range<Double> rtRange) {
    Range<Double> all = Range.all();
    return getRowsInsideScanAndMZRange(rtRange, all);
  }

  @Override
  public PeakListRow[] getRowsInsideScanAndMZRange(Range<Double> rtRange, Range<Double> mzRange) {
    Vector<PeakListRow> rowsInside = new Vector<PeakListRow>();

    for (PeakListRow row : peakListRows) {
      if (rtRange.contains(row.getAverageRT()) && mzRange.contains(row.getAverageMZ()))
        rowsInside.add(row);
    }

    return rowsInside.toArray(new PeakListRow[0]);
  }

  @Override
  public void addRow(PeakListRow row) {
    for (RawDataFile testFile : row.getRawDataFiles()) {
      if (!dataFiles.contains(testFile))
        throw (new IllegalArgumentException(
            "Data file " + testFile + " is not in this feature list"));
    }

    peakListRows.add(row);
    if (row.getDataPointMaxIntensity() > maxDataPointIntensity) {
      maxDataPointIntensity = row.getDataPointMaxIntensity();
    }

    if (mzRange == null) {
      mzRange = Range.singleton(row.getAverageMZ());
      rtRange = Range.singleton(row.getAverageRT());
    } else {
      mzRange = mzRange.span(Range.singleton(row.getAverageMZ()));
      rtRange = rtRange.span(Range.singleton(row.getAverageRT()));
    }

    row.setPeakList(this);
  }

  /**
   * Returns all peaks overlapping with a retention time range
   *
   * @param startRT Start of the retention time range
   * @param endRT End of the retention time range
   * @return
   */
  @Override
  public Feature[] getPeaksInsideScanRange(RawDataFile file, Range<Double> rtRange) {
    Range<Double> all = Range.all();
    return getPeaksInsideScanAndMZRange(file, rtRange, all);
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#getPeaksInsideMZRange(double, double)
   */
  @Override
  public Feature[] getPeaksInsideMZRange(RawDataFile file, Range<Double> mzRange) {
    Range<Double> all = Range.all();
    return getPeaksInsideScanAndMZRange(file, all, mzRange);
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#getPeaksInsideScanAndMZRange(double, double, double,
   *      double)
   */
  @Override
  public Feature[] getPeaksInsideScanAndMZRange(RawDataFile file, Range<Double> rtRange,
      Range<Double> mzRange) {
    Vector<Feature> peaksInside = new Vector<Feature>();

    Feature[] peaks = getPeaks(file).toArray(Feature[]::new);
    for (Feature p : peaks) {
      if (rtRange.contains(p.getRT()) && mzRange.contains(p.getMZ()))
        peaksInside.add(p);
    }

    return peaksInside.toArray(new Feature[0]);
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#removeRow(io.github.mzmine.datamodel.PeakListRow)
   */
  @Override
  public void removeRow(PeakListRow row) {
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
    for (PeakListRow peakListRow : peakListRows) {
      if (peakListRow.getDataPointMaxIntensity() > maxDataPointIntensity)
        maxDataPointIntensity = peakListRow.getDataPointMaxIntensity();

      if (mzRange == null) {
        mzRange = Range.singleton(peakListRow.getAverageMZ());
        rtRange = Range.singleton(peakListRow.getAverageRT());
      } else {
        mzRange = mzRange.span(Range.singleton(peakListRow.getAverageMZ()));
        rtRange = rtRange.span(Range.singleton(peakListRow.getAverageRT()));
      }
    }
  }

  @Override
  public Stream<PeakListRow> stream() {
    return peakListRows.stream();
  }

  @Override
  public Stream<PeakListRow> parallelStream() {
    return peakListRows.parallelStream();
  }

  /**
   * @see io.github.mzmine.datamodel.PeakList#getPeakRowNum(io.github.mzmine.datamodel.Feature)
   */
  @Override
  public int getPeakRowNum(Feature peak) {

    PeakListRow row = getPeakRow(peak);

    if (row == null)
      return -1;
    else
      return row.getID();

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
    return Arrays.asList(dataFiles).contains(hasFile);
  }

  @Override
  public PeakListRow getPeakRow(Feature peak) {

    for (PeakListRow row : peakListRows) {
      if (row.hasPeak(peak))
        return row;
    }

    return null;
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
  public PeakListAppliedMethod[] getAppliedMethods() {
    return descriptionOfAppliedTasks.toArray(new PeakListAppliedMethod[0]);
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
  public Range<Double> getRowsRTRange() {
    updateMaxIntensity(); // Update range before returning value
    return rtRange;
  }

  @Override
  public PeakListRow findRowByID(int id) {
    return stream().filter(r -> r.getID() == id).findFirst().orElse(null);
  }
}
