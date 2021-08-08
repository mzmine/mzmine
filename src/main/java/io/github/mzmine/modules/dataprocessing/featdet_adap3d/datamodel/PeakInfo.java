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
package io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 * Structure PeakInfo contains all information about a peak
 *
 * @author aleksandrsmirnov Modified by Dharak Shah to include in MSDK
 */
public class PeakInfo implements Comparator<PeakInfo>, Comparable<PeakInfo>, Serializable {
  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  private static final DecimalFormat DECIMAL = new DecimalFormat("#.00");

  public double retTime;
  public double mzValue;
  public double intensity; // Intensity
  public double retTimeStart;
  public double retTimeEnd;

  public int peakID;
  public int peakIndex; // pkInd
  public int leftApexIndex; // LBound
  public int rightApexIndex; // RBound
  public int leftPeakIndex; // lboundInd
  public int rightPeakIndex; // rboundInd
  // public int offset;
  // public boolean isShared;
  public double signalToNoiseRatio;
  public double coeffOverArea;

  // ------------------------------------------------------------------------
  // ----- Construtors ------------------------------------------------------
  // ------------------------------------------------------------------------

  /**
   * <p>
   * Constructor for PeakInfo.
   * </p>
   */
  public PeakInfo() {}

  /**
   * <p>
   * Constructor for PeakInfo.
   * </p>
   *
   * @param peakIndex a int.
   */
  public PeakInfo(final int peakIndex) {
    this.peakIndex = peakIndex;
  }

  /**
   * <p>
   * Constructor for PeakInfo.
   * </p>
   *
   * @param info a {@link PeakInfo} object.
   */
  public PeakInfo(final PeakInfo info) {
    retTime = info.retTime;
    mzValue = info.mzValue;
    intensity = info.intensity;
    retTimeStart = info.retTimeStart;
    retTimeEnd = info.retTimeEnd;

    peakID = info.peakID;
    peakIndex = info.peakIndex;
    leftApexIndex = info.leftApexIndex;
    rightApexIndex = info.rightApexIndex;
    leftPeakIndex = info.leftPeakIndex;
    rightPeakIndex = info.rightPeakIndex;
    // offset = info.offset;
    // isShared = info.isShared;
    signalToNoiseRatio = info.signalToNoiseRatio;
    // sharpness = info.sharpness;
    // coeffOverArea = info.coeffOverArea;
  }

  // ------------------------------------------------------------------------
  // ----- Properties -------------------------------------------------------
  // ------------------------------------------------------------------------

  /**
   * <p>
   * mzValue.
   * </p>
   *
   * @param mz a double.
   * @return a {@link PeakInfo} object.
   */
  public PeakInfo mzValue(final double mz) {
    this.mzValue = mz;
    return this;
  }

  /**
   * <p>
   * peakID.
   * </p>
   *
   * @param id a int.
   * @return a {@link PeakInfo} object.
   */
  public PeakInfo peakID(final int id) {
    this.peakID = id;
    return this;
  }

  // ------------------------------------------------------------------------
  // ----- Methods ----------------------------------------------------------
  // ------------------------------------------------------------------------

  /** {@inheritDoc} */
  @Override
  public int compare(final PeakInfo info1, final PeakInfo info2) {
    if (info1.peakIndex < info2.peakIndex)
      return -1;
    else if (info1.peakIndex == info2.peakIndex)
      return 0;
    return 1;
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(final PeakInfo info) {
    if (this.peakIndex < info.peakIndex)
      return -1;
    else if (this.peakIndex == info.peakIndex)
      return 0;
    return 1;
  }

  /**
   * <p>
   * merge.
   * </p>
   *
   * @param info1 a {@link PeakInfo} object.
   * @param info2 a {@link PeakInfo} object.
   * @return a {@link PeakInfo} object.
   */
  public static PeakInfo merge(final PeakInfo info1, final PeakInfo info2) {
    if (info1.mzValue != info2.mzValue)
      throw new IllegalArgumentException("Cannot merge PeakInfo with different m/z-values");

    PeakInfo result = new PeakInfo();

    result.mzValue = info1.mzValue;

    if (info1.intensity > info2.intensity) {
      result.intensity = info1.intensity;
      result.peakIndex = info1.peakIndex;
    } else {
      result.intensity = info2.intensity;
      result.peakIndex = info2.peakIndex;
    }

    result.leftApexIndex = Integer.min(info1.leftApexIndex, info2.leftApexIndex);
    result.leftPeakIndex = Integer.min(info1.leftPeakIndex, info2.leftPeakIndex);

    result.rightApexIndex = Integer.max(info1.rightApexIndex, info2.rightApexIndex);
    result.rightPeakIndex = Integer.max(info1.rightPeakIndex, info2.rightPeakIndex);

    return result;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "m/z " + DECIMAL.format(mzValue) + " @ " + DECIMAL.format(retTime) + " min.";
  }
}
