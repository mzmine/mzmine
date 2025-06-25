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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.resample;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.scans.ScanUtils;
import org.jetbrains.annotations.NotNull;

public class ResampleFilter implements ScanFilter {

  private final double binSize;

  // requires default constructor for config
  public ResampleFilter() {
    binSize = 0.01;
  }

  public ResampleFilter(final ParameterSet parameters) {
    binSize = parameters.getParameter(ResampleFilterParameters.binSize).getValue();
  }


  @Override
  public Scan filterScan(RawDataFile newFile, Scan scan) {

    if (scan.isEmptyScan()) {
//      return scan;
      return scan;
    }

    Range<Double> mzRange = scan.getDataPointMZRange();
    int numberOfBins = (int) Math.round(
        (mzRange.upperEndpoint() - mzRange.lowerEndpoint()) / binSize);
    if (numberOfBins == 0) {
      numberOfBins++;
    }

    // ScanUtils.binValues needs arrays
    DataPoint dps[] = ScanUtils.extractDataPoints(scan);
    double[] x = new double[dps.length];
    double[] y = new double[dps.length];
    for (int i = 0; i < dps.length; i++) {
      x[i] = dps[i].getMZ();
      y[i] = dps[i].getIntensity();
    }
    // the new intensity values
    double[] newY = ScanUtils.binValues(x, y, mzRange, numberOfBins,
        scan.getSpectrumType() == MassSpectrumType.PROFILE, ScanUtils.BinningType.AVG);
    SimpleDataPoint[] newPoints = new SimpleDataPoint[newY.length];

    // set the new m/z value in the middle of the bin
    double newX = mzRange.lowerEndpoint() + binSize / 2.0;
    // creates new DataPoints
    for (int i = 0; i < newY.length; i++) {
      newPoints[i] = new SimpleDataPoint(newX, newY[i]);
      newX += binSize;
    }

    double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(newPoints);
    // Create updated scan
    SimpleScan newScan = new SimpleScan(newFile, scan, dp[0], dp[1]);
    newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

    return newScan;
  }

  @Override
  public @NotNull String getName() {
    return "Resampling filter";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ResampleFilterParameters.class;
  }
}
