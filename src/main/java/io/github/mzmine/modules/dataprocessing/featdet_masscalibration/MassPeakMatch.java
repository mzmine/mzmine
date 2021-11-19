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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.errortypes.ErrorType;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist.StandardsListItem;
import java.util.Comparator;

public class MassPeakMatch {
  public static final Comparator<MassPeakMatch> mzErrorComparator =
      Comparator.comparing(MassPeakMatch::getMzError);

  public static final Comparator<MassPeakMatch> measuredMzComparator =
      Comparator.comparing(MassPeakMatch::getMeasuredMzRatio);

  protected final double measuredMzRatio;
  protected final double measuredRetentionTime;
  protected final double matchedMzRatio;
  protected final double matchedRetentionTime;

  protected ErrorType mzErrorType;
  protected double mzError;

  protected DataPoint measuredDataPoint;
  protected StandardsListItem matchedCalibrant;
  protected Scan scan;

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime, double matchedMzRatio,
      double matchedRetentionTime) {
    this.measuredMzRatio = measuredMzRatio;
    this.measuredRetentionTime = measuredRetentionTime;
    this.matchedMzRatio = matchedMzRatio;
    this.matchedRetentionTime = matchedRetentionTime;
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime, double matchedMzRatio,
      double matchedRetentionTime, ErrorType mzErrorType) {
    this(measuredMzRatio, measuredRetentionTime, matchedMzRatio, matchedRetentionTime);
    this.mzErrorType = mzErrorType;
    this.mzError = mzErrorType.calculateError(measuredMzRatio, matchedMzRatio);
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime, double matchedMzRatio,
      double matchedRetentionTime, ErrorType mzErrorType, DataPoint measuredDataPoint) {
    this(measuredMzRatio, measuredRetentionTime, matchedMzRatio, matchedRetentionTime, mzErrorType);
    this.measuredDataPoint = measuredDataPoint;
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime, double matchedMzRatio,
      double matchedRetentionTime, ErrorType mzErrorType, DataPoint measuredDataPoint,
      Scan scan) {
    this(measuredMzRatio, measuredRetentionTime, matchedMzRatio, matchedRetentionTime, mzErrorType,
        measuredDataPoint);
    this.scan = scan;
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime, double matchedMzRatio,
      double matchedRetentionTime, ErrorType mzErrorType, DataPoint measuredDataPoint,
      Scan scan, StandardsListItem matchedCalibrant) {
    this(measuredMzRatio, measuredRetentionTime, matchedMzRatio, matchedRetentionTime, mzErrorType,
        measuredDataPoint, scan);
    this.matchedCalibrant = matchedCalibrant;
  }

  public MassPeakMatch(double measuredMzRatio, double measuredRetentionTime, double matchedMzRatio,
      double matchedRetentionTime, ErrorType mzErrorType, double mzError) {
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

  public DataPoint getMeasuredDataPoint() {
    return measuredDataPoint;
  }

  public StandardsListItem getMatchedCalibrant() {
    return matchedCalibrant;
  }

  public Scan getScan() {
    return scan;
  }
}
