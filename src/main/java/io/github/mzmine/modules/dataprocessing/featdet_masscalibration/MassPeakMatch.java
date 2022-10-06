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
