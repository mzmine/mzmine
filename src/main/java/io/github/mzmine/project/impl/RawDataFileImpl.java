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

package io.github.mzmine.project.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxColorUtil;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  protected final String absolutePath;
  protected final ObservableList<Scan> scans;
  protected final ObservableList<FeatureListAppliedMethod> appliedMethods = FXCollections.observableArrayList();
  // for ease of use we have a javafx safe copy of name
  private final StringProperty nameProperty = new SimpleStringProperty("");
  private final Map<Integer, Range<Double>> dataMZRange = new HashMap<>();
  private final Map<Integer, Range<Float>> dataRTRange = new HashMap<>();
  private final Int2DoubleOpenHashMap dataMaxBasePeakIntensity = new Int2DoubleOpenHashMap(2);
  private final Int2DoubleOpenHashMap dataMaxTIC = new Int2DoubleOpenHashMap(2);
  // Temporary file for scan data storage
  private final MemoryMapStorage storageMemoryMap;
  private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
  // maximum number of data points and centroid data points in all scans
  protected int maxRawDataPoints = -1;
  // Name of this raw data file - may be changed by the user
  private String name = "";
  // track if file contains zero intensity as this might originate from wrong conversion
  // msconvert needs to have the peak picker as first step / not even title maker before that
  private boolean containsZeroIntensity;

  private boolean containsEmptyScans;
  private MassSpectrumType spectraType;

  @Nullable
  private LocalDateTime startTimeStamp = null;

  public RawDataFileImpl(@NotNull final String dataFileName, @Nullable final String absolutePath,
      @Nullable final MemoryMapStorage storage) {
    this(dataFileName, absolutePath, storage,
        MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor());
  }

  public RawDataFileImpl(@NotNull final String dataFileName, @Nullable final String absolutePath,
      @Nullable final MemoryMapStorage storage, @NotNull Color color) {
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
    return dataMaxBasePeakIntensity.computeIfAbsent(msLevel, key -> {
      double max = Double.NEGATIVE_INFINITY;
      // find the value
      for (Scan scan : scans) {
        // ignore scans of other ms levels
        if (scan.getMSLevel() != msLevel) {
          continue;
        }

        Double basePeakIntensity = scan.getBasePeakIntensity();
        if (basePeakIntensity != null && basePeakIntensity > max) {
          max = scan.getTIC();
        }
      }
      return Double.compare(Double.NEGATIVE_INFINITY, max) == 0 ? -1d : max;
    });
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getDataMaxTotalIonCurrent(int)
   */
  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {
    // check if we have this value already cached
    return dataMaxTIC.computeIfAbsent(msLevel, key -> {
      double max = Double.NEGATIVE_INFINITY;
      // find the value
      for (Scan scan : scans) {
        // ignore scans of other ms levels
        if (scan.getMSLevel() != msLevel) {
          continue;
        }

        if (scan.getTIC() > max) {
          max = scan.getTIC();
        }
      }
      return Double.compare(Double.NEGATIVE_INFINITY, max) == 0 ? -1d : max;
    });
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
    // check spec type
    MassSpectrumType newType = newScan.getSpectrumType();
    if (newType != spectraType) {
      if (spectraType == null) {
        spectraType = newType;
      } else {
        spectraType = MassSpectrumType.MIXED;
      }
    }

    //check for empty scans (absent m/z range or absent intensity)
    if (!containsEmptyScans && newScan.isEmptyScan()) {
      containsEmptyScans = true;
      logger.warning("Some scans were recognized as empty (no detected peaks).");
    }

    // check for zero intensity because this might indicate incorrect conversion by msconvert
    // when not using peak picking as the first step
    if (!containsZeroIntensity) {
      double[] intensities = newScan.getIntensityValues(new double[0]);
      for (double v : intensities) {
        if (v <= 0) {
          containsZeroIntensity = true;
          if (spectraType.isCentroided()) {
            logger.warning("""
                Scans were detected as centroid but contain zero intensity values. This might indicate incorrect conversion by msconvert. 
                Make sure to run "peak picking" with vendor algorithm as the first step (even before title maker), otherwise msconvert uses 
                a different algorithm that picks the highest data point of a profile spectral peak and adds zero intensities next to each signal.
                This leads to degraded mass accuracies.""");
          }
          break;
        }
      }
    }
    // Remove cached values
    dataMZRange.clear();
    dataRTRange.clear();
    dataMaxBasePeakIntensity.clear();
    dataMaxTIC.clear();
  }

  @Override
  public boolean isContainsZeroIntensity() {
    return containsZeroIntensity;
  }


  public boolean isContainsEmptyScans() {
    return containsEmptyScans;
  }


  @Override
  public MassSpectrumType getSpectraType() {
    return spectraType;
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
    return name;
  }

  @Override
  public String setName(@NotNull String name) {
    if (name.isBlank() || name.equals(getName())) {
      // keep old name
      return getName();
    }

    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

    if (project != null) {
      project.setUniqueDataFileName(this, name);
    } else {
      // path safe encode
      setNameNoChecks(FileAndPathUtil.safePathEncode(name));
    }

    return this.name;
  }


  @Override
  public String setNameNoChecks(@NotNull String name) {
    this.name = name;

    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    if (project != null) {
      project.fireDataFilesChangeEvent(List.of(this), ProjectChangeEvent.Type.RENAMED);
    }
    MZmineCore.runLater(() -> nameProperty.set(this.name));
    return name;
  }

  @Override
  public String toString() {
    return name;
  }


  @Override
  public @NotNull ObservableList<Scan> getScans() {
    return scans;
  }

  @NotNull
  @Override
  public ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return appliedMethods;
  }

  @Override
  public StringProperty nameProperty() {
    return nameProperty;
  }

  @Override
  public @Nullable LocalDateTime getStartTimeStamp() {
    return startTimeStamp;
  }

  public void setStartTimeStamp(@Nullable LocalDateTime startTimeStamp) {
    this.startTimeStamp = startTimeStamp;
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
