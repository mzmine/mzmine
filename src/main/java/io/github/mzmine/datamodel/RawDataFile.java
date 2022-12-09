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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
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
   * Change the name of this data file.
   * <p></p>
   * Setting the name of a file via this function is not reproducible in MZmine projects if the name
   * is not predetermined in by a parameter. In that case,
   * {@link
   * io.github.mzmine.modules.tools.rawfilerename.RawDataFileRenameModule#renameFile(RawDataFile,
   * String)} should be used.
   *
   * @return the actually set name after checking restricted symbols and for duplicate names
   */
  String setName(@NotNull String name);

  /**
   * @return The absolute path this file was loaded from. Null if the file does not exist on the
   * file space or was created as a dummy file by mzTab-m import.
   */
  @Nullable String getAbsolutePath();

  int getNumOfScans();

  int getNumOfScans(int msLevel);


  /**
   * The maximum number of centroid data points in all scans (after mass detection and optional
   * processing)
   *
   * @return max number of data points in masslist
   */
  int getMaxCentroidDataPoints();

  /**
   * The maximum number of raw data points in all scans
   *
   * @return max raw data points in scans
   */
  int getMaxRawDataPoints();

  /**
   * Returns sorted array of all MS levels in this file
   */
  @NotNull int[] getMSLevels();

  /**
   * Returns sorted array of all scan numbers in given MS level
   *
   * @param msLevel MS level
   * @return Sorted array of scan numbers, never returns null
   */
  @NotNull List<Scan> getScanNumbers(int msLevel);

  /**
   * Returns sorted array of all scan numbers in given MS level and retention time range
   *
   * @param msLevel MS level
   * @param rtRange Retention time range
   * @return Sorted array of scan numbers, never returns null
   */
  @NotNull Scan[] getScanNumbers(int msLevel, @NotNull Range<Float> rtRange);

  /**
   * @param rt      The rt
   * @param mslevel The ms level
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   * the rt range of this file.
   */
  Scan getScanNumberAtRT(float rt, int mslevel);

  /**
   * @param rt The rt
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   * the rt range of this file.
   */
  Scan getScanNumberAtRT(float rt);

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


  String setNameNoChecks(@NotNull String name);

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
   * JavaFX safe copy of the name
   */
  StringProperty nameProperty();

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
}
