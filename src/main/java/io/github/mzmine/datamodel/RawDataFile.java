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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Range;
import io.github.mzmine.util.MemoryMapStorage;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public interface RawDataFile {

  /**
   * Returns the name of this data file (can be a descriptive name, not necessarily the original
   * file name)
   */
  @NotNull
  String getName();

  /**
   * Change the name of this data file
   */
  void setName(@NotNull String name);

  int getNumOfScans();

  int getNumOfScans(int msLevel);


  /**
   * The maximum number of centroid data points in all scans (after mass detection and optional
   * processing)
   *
   * @return
   */
  int getMaxCentroidDataPoints();

  /**
   * The maximum number of raw data points in all scans
   *
   * @return
   */
  int getMaxRawDataPoints();

  /**
   * Returns sorted array of all MS levels in this file
   */
  @NotNull
  int[] getMSLevels();

  /**
   * Returns sorted array of all scan numbers in given MS level
   *
   * @param msLevel MS level (0 for all scans)
   * @return Sorted array of scan numbers, never returns null
   */
  @NotNull
  List<Scan> getScanNumbers(int msLevel);


  /**
   * Returns sorted array of all scan numbers in given MS level and retention time range
   *
   * @param msLevel MS level
   * @param rtRange Retention time range
   * @return Sorted array of scan numbers, never returns null
   */
  @NotNull
  Scan[] getScanNumbers(int msLevel, @NotNull Range<Float> rtRange);

  /**
   * @param rt The rt
   * @param mslevel The ms level
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   *         the rt range of this file.
   */
  Scan getScanNumberAtRT(float rt, int mslevel);

  /**
   * @param rt The rt
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   *         the rt range of this file.
   */
  Scan getScanNumberAtRT(float rt);

  @NotNull
  Range<Double> getDataMZRange();

  @NotNull
  Range<Float> getDataRTRange();

  @NotNull
  Range<Double> getDataMZRange(int msLevel);

  @NotNull
  Range<Float> getDataRTRange(Integer msLevel);

  double getDataMaxBasePeakIntensity(int msLevel);

  double getDataMaxTotalIonCurrent(int msLevel);

  /**
   * Returns a list of the different scan polarity types found in the raw data file.
   *
   * @return Scan polarity types.
   */
  @NotNull
  List<PolarityType> getDataPolarity();

  java.awt.Color getColorAWT();

  javafx.scene.paint.Color getColor();

  void setColor(Color color);

  ObjectProperty<Color> colorProperty();

  /**
   * Close the file in case it is removed from the project
   */
  void close();

  @NotNull
  MemoryMapStorage getMemoryMapStorage();

  void addScan(Scan newScan) throws IOException;

  void setRTRange(int msLevel, Range<Float> rtRange);

  void setMZRange(int msLevel, Range<Double> mzRange);


  ObservableList<Scan> getScans();

  ObservableList<Chromatogram> getChromatograms();

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
   * @param scanNumber
   * @return
   */
  default Scan getScanAtNumber(int scanNumber) {
    return getScans().stream().filter(s -> s.getScanNumber() == scanNumber).findFirst()
        .orElse(null);
  }

  /**
   * Scan at index i in list getScans()
   *
   * @param i
   * @return
   */
  default Scan getScan(int i) {
    return getScans().get(i);
  }

  @NotNull
  ObservableList<FeatureListAppliedMethod> getAppliedMethods();
}
