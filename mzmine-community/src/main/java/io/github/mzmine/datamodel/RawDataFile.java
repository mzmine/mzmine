/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RawDataFile {

  /**
   * Returns the name of this data file (can be a descriptive name, not necessarily the original
   * file name)
   */
  @NotNull String getName();

  /**
   * @return The absolute path this file was loaded from. Null if the file does not exist on the
   * file space or was created as a dummy file by mzTab-m import.
   */
  @Nullable String getAbsolutePath();

  /**
   * @return The absolute path this file was loaded from. or a file of getName() if no path was
   * provided
   */
  default @NotNull File getAbsoluteFilePath() {
    return new File(requireNonNullElse(getAbsolutePath(), getName()));
  }

  /**
   * Uses the absolute file path first, if null then use the name which might have been changed by
   * the user
   *
   * @return usually the file.extension as in File.getName()
   */
  default String getFileName() {
    var path = getAbsolutePath();
    return path != null ? new File(path).getName() : getName();
  }

  int getNumOfScans();

  int getNumOfScans(int msLevel);


  /**
   * The maximum number of centroid data points in all scans (after mass detection and optional
   * processing)
   *
   * @return max number of data points in all masslists (centroid) of all scans
   */
  default int getMaxCentroidDataPoints() {
    return stream().map(Scan::getMassList).filter(Objects::nonNull)
        .mapToInt(MassList::getNumberOfDataPoints).max().orElse(0);
  }


  /**
   * The maximum number of raw data points in all scans
   *
   * @return max number of data points in all raw scans
   */
  int getMaxRawDataPoints();

  /**
   * Returns sorted array of all MS levels in this file
   */
  default @NotNull int[] getMSLevels() {
    return stream().mapToInt(Scan::getMSLevel).distinct().sorted().toArray();
  }

  /**
   * Returns sorted array of all scan numbers in given MS level
   *
   * @param msLevel MS level
   * @return Sorted array of scan numbers, never returns null
   */
  default @NotNull List<Scan> getScanNumbers(int msLevel) {
    return stream().filter(s -> s.getMSLevel() == msLevel).collect(Collectors.toList());
  }

  /**
   * Returns sorted array of all scan numbers in given MS level and retention time range
   *
   * @param msLevel MS level
   * @param rtRange Retention time range
   * @return Sorted array of scan numbers, never returns null
   */
  default @NotNull Scan[] getScanNumbers(int msLevel, @NotNull Range<Float> rtRange) {
    return stream()
        .filter(s -> s.getMSLevel() == msLevel && rtRange.contains(s.getRetentionTime()))
        .toArray(Scan[]::new);
  }

  /**
   * Uses binary search
   *
   * @param rt The rt
   * @return the closest scan or null if no scans avaialble
   */
  default @Nullable Scan binarySearchClosestScan(float rt, int mslevel) {
    // scans are sorted by rt ascending
    // closest index will be negative direct hit is positive
    int indexClosestScan = binarySearchClosestScanIndex(rt, mslevel);
    if (indexClosestScan < getNumOfScans() && indexClosestScan >= 0) {
      return getScan(indexClosestScan);
    }
    return null;
  }

  /**
   * Uses binary search
   *
   * @param rt The rt
   * @return the closest scan or null if no scans avaialble
   */
  default @Nullable Scan binarySearchClosestScan(float rt) {
    // scans are sorted by rt ascending
    // closest index will be negative direct hit is positive
    int indexClosestScan = binarySearchClosestScanIndex(rt);
    if (indexClosestScan < getNumOfScans() && indexClosestScan >= 0) {
      return getScan(indexClosestScan);
    }
    return null;
  }

  /**
   * binary search the closest scan
   *
   * @param rt search retention time
   * @return closest index or -1 if no scan was found
   */
  default int binarySearchClosestScanIndex(float rt) {
    // scans are sorted by rt ascending
    // closest index will be negative direct hit is positive
    int closestIndex = Math.abs(
        BinarySearch.binarySearch(rt, DefaultTo.CLOSEST_VALUE, getNumOfScans(),
        index -> getScan(index).getRetentionTime()));
    return closestIndex >= getNumOfScans() ? -1 : closestIndex;
  }

  /**
   * binary search the closest scan
   *
   * @param rt search retention time
   * @return closest index or -1 if no scan was found
   */
  default int binarySearchClosestScanIndex(float rt, int mslevel) {
    // scans are sorted by rt ascending
    // closest index will be negative direct hit is positive
    int indexClosestScan = binarySearchClosestScanIndex(rt);
    if (indexClosestScan == -1) {
      return -1;
    }
    //matches ms level
    if (getScan(indexClosestScan).getMSLevel() == mslevel) {
      return indexClosestScan;
    }

    // find the closest scan with msLevel around the found scan (might be other level)
    int before = -1;
    int after = -1;
    for (int i = indexClosestScan; i < getNumOfScans(); i++) {
      if (getScan(i).getMSLevel() == mslevel) {
        after = i;
        break;
      }
    }
    for (int i = indexClosestScan - 1; i > 0; i--) {
      if (getScan(i).getMSLevel() == mslevel) {
        before = i;
        break;
      }
    }
    if (after != -1 && before != -1) {
      if (Math.abs(getScan(after).getRetentionTime() - rt) < Math.abs(
          getScan(before).getRetentionTime() - rt)) {
        return after;
      } else {
        return before;
      }
    } else if (after != -1) {
      return after;
    } else {
      return before;
    }
  }

  @NotNull Range<Double> getDataMZRange();


  /**
   * Contains at least one zero intensity (or negative). This might be a sign that the conversion
   * with msconvert had incorrect settings. Peak picking needs to be the first step NOT title maker
   *
   * @return true if <=0 in any scan
   */
  boolean isContainsZeroIntensity();

  /**
   * Contains at least one empty scan.
   *
   * @return true if m/z range is absent in any scan
   */
  boolean isContainsEmptyScans();


  /**
   * The spectrum type of all spectra or {@link MassSpectrumType#MIXED}
   *
   * @return the type of all spectra
   */
  MassSpectrumType getSpectraType();

  /**
   * @return The rt range of this raw data file. This range might be empty e.g., (0, 0). If a
   * positive range is required,
   * {@link io.github.mzmine.util.RangeUtils#getPositiveRange(Range, Number)}
   */
  @NotNull Range<Float> getDataRTRange();

  @NotNull Range<Double> getDataMZRange(int msLevel);

  /**
   * @return The rt range of this raw data file. This range might be empty e.g., (0, 0). If a
   * positive range is required,
   * {@link io.github.mzmine.util.RangeUtils#getPositiveRange(Range, Number)}
   */
  @NotNull Range<Float> getDataRTRange(Integer msLevel);

  double getDataMaxBasePeakIntensity(int msLevel);

  double getDataMaxTotalIonCurrent(int msLevel);

  /**
   * Returns a list of the different scan polarity types found in the raw data file.
   *
   * @return Scan polarity types.
   */
  @NotNull List<PolarityType> getDataPolarity();

  java.awt.Color getColorAWT();

  javafx.scene.paint.Color getColor();

  void setColor(Color color);

  ObjectProperty<Color> colorProperty();

  /**
   * Close the file in case it is removed from the project
   */
  void close();

  @Nullable MemoryMapStorage getMemoryMapStorage();

  void addScan(Scan newScan) throws IOException;

  @NotNull ObservableList<Scan> getScans();

  default @NotNull Stream<Scan> stream() {
    return getScans().stream();
  }

  /**
   * Mass list has changed. reset all precomputed values
   *
   * @param scan   the scan that was changed
   * @param old    old mass list
   * @param masses new mass list
   */
  void applyMassListChanged(Scan scan, MassList old, MassList masses);

  /**
   * The scan at the specified scan number or null
   *
   * @param scanNumber the number defined in the scan
   * @return scan or null
   */
  default @Nullable Scan getScanAtNumber(int scanNumber) {
    return getScans().stream().filter(s -> s.getScanNumber() == scanNumber).findFirst()
        .orElse(null);
  }

  /**
   * Scan at index i in list getScans()
   *
   * @param i index
   * @return scan or null
   */
  default @Nullable Scan getScan(int i) {
    return getScans().get(i);
  }

  @NotNull ObservableList<FeatureListAppliedMethod> getAppliedMethods();

  /**
   * Get the start time stamp of the sample.
   *
   * @return a datetime stamp (or null in case if it wasn't mentioned in the RawDataFile)
   */
  @Nullable
  default LocalDateTime getStartTimeStamp() {
    return null;
  }

  /**
   * Set the start time stamp of the sample.
   */
  default void setStartTimeStamp(@Nullable LocalDateTime localDateTime) {
  }

  List<OtherDataFile> getOtherDataFiles();
}
