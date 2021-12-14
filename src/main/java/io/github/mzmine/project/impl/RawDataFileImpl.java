/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.project.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * RawDataFile implementation. It provides storage of data points for scans and mass lists using the
 * storeDataPoints() and readDataPoints() methods. The data points are stored in a temporary file
 * (dataPointsFile) and the structure of the file is stored in two TreeMaps. The dataPointsOffsets
 * maps storage ID to the offset in the dataPointsFile. The dataPointsLength maps the storage ID to
 * the number of data points stored under this ID. When stored data points are deleted using
 * removeStoredDataPoints(), the dataPointsFile is not modified, the storage ID is just deleted from
 * the two TreeMaps. When the project is saved, the contents of the dataPointsFile are consolidated
 * - only data points referenced by the TreeMaps are saved (see the RawDataFileSaveHandler class).
 */
public class RawDataFileImpl implements RawDataFile {

  public static final String SAVE_IDENTIFIER = "Raw data file";

  private static final Logger logger = Logger.getLogger(RawDataFileImpl.class.getName());

  // Name of this raw data file - may be changed by the user
  private final StringProperty nameProperty = new SimpleStringProperty("");

  protected final String absolutePath;

  private final Hashtable<Integer, Range<Double>> dataMZRange = new Hashtable<>();
  private final Hashtable<Integer, Range<Float>> dataRTRange = new Hashtable<>();

  private final Hashtable<Integer, Double> dataMaxBasePeakIntensity = new Hashtable<>();
  private final Hashtable<Integer, Double> dataMaxTIC = new Hashtable<>();

  // Temporary file for scan data storage
  private final MemoryMapStorage storageMemoryMap;

  private final ObjectProperty<Color> color = new SimpleObjectProperty<>();

  protected final ObservableList<Scan> scans;
  // maximum number of data points and centroid data points in all scans
  protected int maxRawDataPoints = -1;

  protected final ObservableList<FeatureListAppliedMethod> appliedMethods = FXCollections
      .observableArrayList();

  public RawDataFileImpl(@NotNull final String dataFileName, @Nullable final String absolutePath,
      @Nullable final MemoryMapStorage storage) throws IOException {
    this(dataFileName, absolutePath, storage,
        MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor());
  }

  public RawDataFileImpl(@NotNull final String dataFileName, @Nullable final String absolutePath,
      @Nullable final MemoryMapStorage storage, @NotNull Color color) throws IOException {
    setName(dataFileName);
    this.storageMemoryMap = storage;
    this.absolutePath = absolutePath;

    scans = FXCollections.observableArrayList();

    this.color.setValue(color);
  }

  @Override
  public @Nullable MemoryMapStorage getMemoryMapStorage() {
    return storageMemoryMap;
  }

  @Override
  public RawDataFile clone() throws CloneNotSupportedException {
    return (RawDataFile) super.clone();
  }

  /**
   * The maximum number of centroid data points in all scans (after mass detection and optional
   * processing)
   *
   * @return data point with maximum intensity (centroided)
   */
  @Override
  public int getMaxCentroidDataPoints() {
    return scans.stream().map(Scan::getMassList).filter(Objects::nonNull)
        .mapToInt(MassList::getNumberOfDataPoints).max().orElse(0);
  }

  /**
   * The maximum number of raw data points in all scans
   *
   * @return data point with maximum intensity in unprocessed data points
   */
  @Override
  public int getMaxRawDataPoints() {
    return maxRawDataPoints;
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getNumOfScans()
   */
  @Override
  public int getNumOfScans() {
    return scans.size();
  }

  /**
   * @param rt      The rt
   * @param mslevel The ms level
   * @return The scan number at a given retention time within a range of 2 (min/sec?) or -1 if no
   * scan can be found.
   */
  @Override
  public Scan getScanNumberAtRT(float rt, int mslevel) {
    if (rt > getDataRTRange(mslevel).upperEndpoint()) {
      return null;
    }
    Range<Float> range = Range.closed(rt - 2, rt + 2);
    Scan[] scanNumbers = getScanNumbers(mslevel, range);
    double minDiff = 10E6;

    for (int i = 0; i < scanNumbers.length; i++) {
      Scan scanNum = scanNumbers[i];
      double diff = Math.abs(rt - scanNum.getRetentionTime());
      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) { // not triggered in first run
        return scanNumbers[i - 1]; // the previous one was better
      }
    }
    return null;
  }

  /**
   * @param rt The rt
   * @return The scan at a given retention time within a range of 2 (min/sec?) or null if no scan
   * can be found.
   */
  @Override
  public Scan getScanNumberAtRT(float rt) {
    if (rt > getDataRTRange().upperEndpoint()) {
      return null;
    }
    double minDiff = 10E10;
    for (Scan scan : scans) {
      double diff = Math.abs(rt - scan.getRetentionTime());
      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) { // not triggered in first run
        return scan;
      }
    }
    return null;
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getScanNumbers(int)
   */
  @Override
  @NotNull
  public List<Scan> getScanNumbers(int msLevel) {
    return scans.stream().filter(s -> s.getMSLevel() == msLevel).collect(Collectors.toList());
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getScanNumbers(int, Range)
   */
  @Override
  public @NotNull Scan[] getScanNumbers(int msLevel, @NotNull Range<Float> rtRange) {
    return scans.stream()
        .filter(s -> s.getMSLevel() == msLevel && rtRange.contains(s.getRetentionTime()))
        .toArray(Scan[]::new);
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getMSLevels()
   */
  @Override
  @NotNull
  public int[] getMSLevels() {
    return scans.stream().mapToInt(Scan::getMSLevel).distinct().sorted().toArray();
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getDataMaxBasePeakIntensity(int)
   */
  @Override
  public double getDataMaxBasePeakIntensity(int msLevel) {
    // check if we have this value already cached
    Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
    if (maxBasePeak != null) {
      return maxBasePeak;
    }

    // find the value
    for (Scan scan : scans) {
      // ignore scans of other ms levels
      if (scan.getMSLevel() != msLevel) {
        continue;
      }

      Double scanBasePeak = scan.getBasePeakIntensity();
      if (scanBasePeak == null) {
        continue;
      }

      if ((maxBasePeak == null) || (scanBasePeak > maxBasePeak)) {
        maxBasePeak = scanBasePeak;
      }
    }

    // return -1 if no scan at this MS level
    if (maxBasePeak == null) {
      maxBasePeak = -1d;
    }

    // cache the value
    dataMaxBasePeakIntensity.put(msLevel, maxBasePeak);

    return maxBasePeak;
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getDataMaxTotalIonCurrent(int)
   */
  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {

    // check if we have this value already cached
    Double maxTIC = dataMaxTIC.get(msLevel);
    if (maxTIC != null) {
      return maxTIC;
    }

    // find the value
    for (Scan scan : scans) {
      // ignore scans of other ms levels
      if (scan.getMSLevel() != msLevel) {
        continue;
      }

      if ((maxTIC == null) || (scan.getTIC() > maxTIC)) {
        maxTIC = scan.getTIC();
      }
    }

    // return -1 if no scan at this MS level
    if (maxTIC == null) {
      maxTIC = -1d;
    }

    // cache the value
    dataMaxTIC.put(msLevel, maxTIC);

    return maxTIC;
  }


  @Override
  public synchronized void addScan(Scan newScan) throws IOException {
    scans.add(newScan);
    if (newScan.getNumberOfDataPoints() > maxRawDataPoints) {
      // TODO how to make sure changes to Frames are reflected
      // Scan will be unmodifiable - Frame is the average spectrum calculated from all MobilityScans
      // so data changes
      maxRawDataPoints = newScan.getNumberOfDataPoints();
    }

    // Remove cached values
    dataMZRange.clear();
    dataRTRange.clear();
    dataMaxBasePeakIntensity.clear();
    dataMaxTIC.clear();
  }


  @Override
  @NotNull
  public Range<Double> getDataMZRange() {
    return getDataMZRange(0);
  }

  @Override
  @NotNull
  public Range<Double> getDataMZRange(int msLevel) {

    // check if we have this value already cached
    Range<Double> mzRange = dataMZRange.get(msLevel);
    if (mzRange != null) {
      return mzRange;
    }

    // find the value
    for (Scan scan : scans) {

      // ignore scans of other ms levels
      if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) {
        continue;
      }

      final Range<Double> scanMzRange = scan.getDataPointMZRange();
      if (mzRange == null) {
        mzRange = scanMzRange;
      } else {
        if (scanMzRange != null) {
          mzRange = mzRange.span(scanMzRange);
        }
      }

    }

    // cache the value, if we found any
    if (mzRange != null) {
      dataMZRange.put(msLevel, mzRange);
    } else {
      mzRange = Range.singleton(0.0);
    }

    return mzRange;

  }

  @Override
  @NotNull
  public Range<Float> getDataRTRange() {
    return getDataRTRange(0);
  }

  @NotNull
  @Override
  public Range<Float> getDataRTRange(Integer msLevel) {
    if (msLevel == null) {
      return getDataRTRange();
    }
    // check if we have this value already cached
    Range<Float> rtRange = dataRTRange.get(msLevel);
    if (rtRange != null) {
      return rtRange;
    }

    // find the value
    for (Scan scan : scans) {

      // ignore scans of other ms levels
      if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) {
        continue;
      }

      if (rtRange == null) {
        rtRange = Range.singleton(scan.getRetentionTime());
      } else {
        rtRange = rtRange.span(Range.singleton(scan.getRetentionTime()));
      }

    }

    // cache the value
    if (rtRange != null) {
      dataRTRange.put(msLevel, rtRange);
    } else {
      rtRange = Range.singleton(0.0f);
    }

    return rtRange;
  }

  @Override
  public void setRTRange(int msLevel, Range<Float> rtRange) {
    dataRTRange.put(msLevel, rtRange);
  }

  @Override
  public void setMZRange(int msLevel, Range<Double> mzRange) {
    dataMZRange.put(msLevel, mzRange);
  }

  @Override
  public int getNumOfScans(int msLevel) {
    return getScanNumbers(msLevel).size();
  }

  @NotNull
  @Override
  public List<PolarityType> getDataPolarity() {
    Set<PolarityType> polarities = scans.stream().map(Scan::getPolarity)
        .collect(Collectors.toSet());
    return ImmutableList.copyOf(polarities);
  }

  @Override
  public java.awt.Color getColorAWT() {
    return FxColorUtil.fxColorToAWT(color.getValue());
  }

  @Override
  public javafx.scene.paint.Color getColor() {
    return color.getValue();
  }

  @Override
  public void setColor(javafx.scene.paint.Color color) {
    this.color.setValue(color);
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
    return color;
  }

  @Override
  public synchronized void close() {

  }

  @Override
  @NotNull
  public String getName() {
    return nameProperty.get();
  }

  @Override
  public @NotNull StringProperty nameProperty() {
    return nameProperty;
  }

  @Override
  public String setName(@NotNull String name) {
    if (name.isBlank()) {
      // keep old name
      return getName();
    }

    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

    if (project != null) {
      synchronized (project.getCurrentRawDataFiles()) {
        final List<String> names = new ArrayList<>(
            project.getCurrentRawDataFiles().stream().map(RawDataFile::getName).toList());
        final String oldName = getName();
        // name is empty if set for the first time
        if (!oldName.isBlank()) {
          names.remove(oldName);
        }

        // make path safe
        name = FileAndPathUtil.safePathEncode(name);
        // handle duplicates
        name =
            names.contains(name) ? MZmineProjectImpl.getUniqueName(name, names) : name;
      }
    }

    final String finalName = name;
    MZmineCore.runLater(() -> this.nameProperty.set(finalName));
    return finalName;
  }

  @Override
  public String toString() {
    return nameProperty.get();
  }


  @Override
  public ObservableList<Scan> getScans() {
    return scans;
  }

  @NotNull
  @Override
  public ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return appliedMethods;
  }

  /**
   * Mass list has changed. reset all precomputed values
   *
   * @param scan   the scan that was changed
   * @param old    old mass list
   * @param masses new mass list
   */
  public void applyMassListChanged(Scan scan, MassList old, MassList masses) {
  }

  @Nullable
  @Override
  public String getAbsolutePath() {
    return absolutePath;
  }
}
