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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Range;

/**
 * Represents a single chromatogram.
 *
 */
public interface Chromatogram {

  /**
   * Returns the raw data file that contains this chromatogram. This might return null when the
   * chromatogram is created, but once the chromatogram is added to the raw data file by calling
   * RawDataFile.addChromatogram(), the RawDataFile automatically calls the
   * Chromatogram.setRawDataFile() method to update this reference.
   *
   * @return RawDataFile containing this chromatogram, or null.
   */
  @Nullable
  RawDataFile getRawDataFile();

  /**
   * Returns the number of this chromatogram, represented by an integer, typically positive.
   * Typically, the chromatogram number will be unique within the file. However, the data model does
   * not guarantee that, and in some cases multiple chromatogram with the same number may be present
   * in the file.
   *
   * @return Chromatogram number
   */
  @NotNull
  Integer getChromatogramNumber();

  /**
   * Returns the type of the chromatogram. If unknown, ChromatogramType.UNKNOWN is returned.
   *
   * @return Chromatogram type
   */
  @NotNull
  ChromatogramType getChromatogramType();

  /**
   * <p>
   * getNumberOfDataPoints.
   * </p>
   *
   * @return a {@link Integer} object.
   */
  @NotNull
  Integer getNumberOfDataPoints();

  /**
   * Returns the info of this chromatogram. Generally, this method should pass null to the method
   * that takes an array as a parameter.
   *
   * Note: this method may need to read data from disk, therefore it may be quite slow.
   *
   * @return an array of
   */
  @NotNull
  default float[] getRetentionTimes() {
    return getRetentionTimes(null);
  }

  @NotNull
  float[] getRetentionTimes(@Nullable float array[]);

  /**
   * <p>
   * getIntensityValues.
   * </p>
   *
   * @return an array of float.
   */
  @NotNull
  default float[] getIntensityValues() {
    return getIntensityValues(null);
  }

  /**
   * <p>
   * getIntensityValues.
   * </p>
   *
   * @param array an array of float.
   * @return an array of float.
   */
  @NotNull
  float[] getIntensityValues(@Nullable float array[]);

  /**
   * <p>
   * getMzValues.
   * </p>
   *
   * @return an array of double.
   */
  @Nullable
  default double[] getMzValues() {
    return getMzValues(null);
  }

  /**
   * 
   * @param array
   * @return
   */
  @Nullable
  double[] getMzValues(@Nullable double array[]);


  /**
   * Returns the m/z value of this chromatogram, or null if no m/z value is set for the
   * chromatogram.
   *
   * @return a {@link Double} object.
   */
  @Nullable
  Double getMz();

  /**
   * Returns a list of isolations performed for this chromatogram. These isolations may also include
   * fragmentations (tandem MS).
   *
   * @return A mutable list of isolations. New isolation items can be added to this list.
   */
  @NotNull
  List<IsolationInfo> getIsolations();


  /**
   * Returns the range of retention times. This can return null if the chromatogram has no data
   * points.
   *
   * @return RT range
   */
  @Nullable
  Range<Float> getRtRange();

}
