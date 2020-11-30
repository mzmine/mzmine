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

package io.github.mzmine.datamodel;

import javafx.beans.property.ObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import java.util.List;

public interface RawDataFile {

  @Nonnull
  public RawDataFile clone() throws CloneNotSupportedException;

  /**
   * Returns the name of this data file (can be a descriptive name, not necessarily the original
   * file name)
   */
  @Nonnull
  public String getName();

  /**
   * Change the name of this data file
   */
  public void setName(@Nonnull String name);

  public int getNumOfScans();

  public int getNumOfScans(int msLevel);

  /**
   * Returns sorted array of all MS levels in this file
   */
  @Nonnull
  public int[] getMSLevels();

  /**
   * Returns sorted array of all scan numbers in this file
   *
   * @return Sorted array of scan numbers, never returns null
   */
  @Nonnull
  public int[] getScanNumbers();

  /**
   * Returns sorted array of all scan numbers in given MS level
   *
   * @param msLevel MS level
   * @return Sorted array of scan numbers, never returns null
   */
  @Nonnull
  public int[] getScanNumbers(int msLevel);

  /**
   * Returns sorted array of all scan numbers in given MS level and retention time range
   *
   * @param msLevel MS level
   * @param rtRange Retention time range
   * @return Sorted array of scan numbers, never returns null
   */
  @Nonnull
  public int[] getScanNumbers(int msLevel, @Nonnull Range<Float> rtRange);

  /**
   * Scan could be null if scanID is not contained in the raw data file
   *
   * @param scan Desired scan number
   * @return Desired scan
   */
  @Nullable
  public Scan getScan(int scan);

  /**
   * @param rt      The rt
   * @param mslevel The ms level
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   * the rt range of this file.
   */
  public int getScanNumberAtRT(float rt, int mslevel);

  /**
   * @param rt The rt
   * @return Returns the scan closest to the given rt in the given ms level. -1 if the rt exceeds
   * the rt range of this file.
   */
  public int getScanNumberAtRT(float rt);

  @Nonnull
  public Range<Double> getDataMZRange();

  @Nonnull
  public Range<Float> getDataRTRange();

  @Nonnull
  public Range<Double> getDataMZRange(int msLevel);

  @Nonnull
  public Range<Float> getDataRTRange(Integer msLevel);

  public double getDataMaxBasePeakIntensity(int msLevel);

  public double getDataMaxTotalIonCurrent(int msLevel);

  /**
   * Returns a list of the different scan polarity types found in the raw data file.
   *
   * @return Scan polarity types.
   */
  @Nonnull
  public List<PolarityType> getDataPolarity();

  public java.awt.Color getColorAWT();

  public javafx.scene.paint.Color getColor();

  public void setColor(javafx.scene.paint.Color color);

  public ObjectProperty<javafx.scene.paint.Color> colorProperty();

  /**
   * Close the file in case it is removed from the project
   */
  public void close();

}
