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

package io.github.mzmine.modules.dataprocessing.masscalibration;

import io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling.errortypes.ErrorType;

import java.util.Comparator;

public class MassPeakMatch {
  public static final Comparator<MassPeakMatch> mzErrorComparator =
          Comparator.comparing(MassPeakMatch::getMzError);

  protected final double measuredMzRatio;
  protected final double measuredRetentionTime;
  protected final double matchedMzRatio;
  protected final double matchedRetentionTime;

  protected ErrorType mzErrorType;
  protected double mzError;

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime,
                       double matchedMzRatio, double matchedRetentionTime) {
    this.measuredMzRatio = measuredMzRatio;
    this.measuredRetentionTime = measuredRetentionTime;
    this.matchedMzRatio = matchedMzRatio;
    this.matchedRetentionTime = matchedRetentionTime;
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime,
                       double matchedMzRatio, double matchedRetentionTime, ErrorType mzErrorType) {
    this(measuredMzRatio, measuredRetentionTime, matchedMzRatio, matchedRetentionTime);
    this.mzErrorType = mzErrorType;
    this.mzError = mzErrorType.calculateError(measuredMzRatio, matchedMzRatio);
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime,
                       double matchedMzRatio, double matchedRetentionTime, ErrorType mzErrorType, double mzError) {
    this(measuredMzRatio, measuredRetentionTime, matchedMzRatio, matchedRetentionTime);
    this.mzErrorType = mzErrorType;
    this.mzError = mzError;
  }

  public double getMeasuredMzRatio() {
    return measuredMzRatio;
  }

  public double getMeasuredRetentionTime() {
    return measuredRetentionTime;
  }

  public double getMatchedMzRatio() {
    return matchedMzRatio;
  }

  public double getMatchedRetentionTime() {
    return matchedRetentionTime;
  }

  public ErrorType getMzErrorType() {
    return mzErrorType;
  }

  public double getMzError() {
    return mzError;
  }
}
