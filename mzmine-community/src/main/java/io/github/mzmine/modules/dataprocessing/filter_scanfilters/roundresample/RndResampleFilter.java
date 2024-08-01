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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.roundresample;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.scans.ScanUtils;
import org.jetbrains.annotations.NotNull;

public class RndResampleFilter implements ScanFilter {

  private final boolean sum_duplicates;
  private final boolean remove_zero_intensity;

  // requires default constructor for config
  public RndResampleFilter() {
    sum_duplicates = false;
    remove_zero_intensity = false;
  }

  public RndResampleFilter(final ParameterSet parameters) {
    sum_duplicates = parameters.getParameter(RndResampleFilterParameters.SUM_DUPLICATES).getValue();
    remove_zero_intensity = parameters.getParameter(
        RndResampleFilterParameters.REMOVE_ZERO_INTENSITY).getValue();
  }

  @Override
  public Scan filterScan(RawDataFile newFile, Scan scan) {

    // If CENTROIDED scan, use it as-is
    DataPoint dps[];
    if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      dps = ScanUtils.extractDataPoints(scan);
    }
    // Otherwise, detect local maxima
    else {
      dps = LocMaxCentroidingAlgorithm.centroidScan(ScanUtils.extractDataPoints(scan));
    }

    // Cleanup first: Remove zero intensity data points (if requested)
    // Reuse dps array
    int newNumOfDataPoints = 0;
    for (int i = 0; i < dps.length; ++i) {
      if (!remove_zero_intensity || dps[i].getIntensity() > 0.0) {
        dps[newNumOfDataPoints] = dps[i];
        ++newNumOfDataPoints;
      }
    }

    // Getting started
    SimpleDataPoint[] newDps = new SimpleDataPoint[newNumOfDataPoints];
    for (int i = 0; i < newNumOfDataPoints; ++i) {
      // Set the new m/z value to nearest integer / unit value
      int newMz = (int) Math.round(dps[i].getMZ());
      // Create new DataPoint accordingly (intensity untouched)
      newDps[i] = new SimpleDataPoint(newMz, dps[i].getIntensity());
    }

    // Post-treatments
    // Cleanup: Merge duplicates/overlap
    // ArrayList<SimpleDataPoint> dpsList = new
    // ArrayList<SimpleDataPoint>();
    double prevMz = -1.0, curMz = -1.0;
    double newIntensity = 0.0;
    double divider = 1.0;

    // Reuse dps array
    newNumOfDataPoints = 0;
    for (int i = 0; i < newDps.length; ++i) {

      curMz = newDps[i].getMZ();
      if (i > 0) {
        // Handle duplicates
        if (curMz == prevMz) {
          if (sum_duplicates) {
            // Use sum
            newIntensity += newDps[i].getIntensity();
            dps[newNumOfDataPoints - 1] = new SimpleDataPoint(prevMz, newIntensity);
          } else {
            // Use average
            newIntensity += newDps[i].getIntensity();
            dps[newNumOfDataPoints - 1] = new SimpleDataPoint(prevMz, newIntensity);
            divider += 1.0;
          }
        } else {
          dps[newNumOfDataPoints - 1] = new SimpleDataPoint(prevMz, newIntensity / divider);

          dps[newNumOfDataPoints] = newDps[i];
          ++newNumOfDataPoints;
          newIntensity = dps[newNumOfDataPoints - 1].getIntensity();
          divider = 1.0;
        }
      } else {
        dps[newNumOfDataPoints] = newDps[i];
        ++newNumOfDataPoints;
      }
      prevMz = newDps[i].getMZ();
    }

    double[][] newDp = new double[2][];
    newDp[0] = new double[newNumOfDataPoints];
    newDp[1] = new double[newNumOfDataPoints];
    for (int i = 0; i < newNumOfDataPoints; i++) {
      newDp[0][i] = dps[i].getMZ();
      newDp[1][i] = dps[i].getIntensity();
    }
    // Create updated scan
    SimpleScan newScan = new SimpleScan(newFile, scan, newDp[0], newDp[1]);
    newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

    return newScan;

  }

  @Override
  public @NotNull String getName() {
    return "Round resampling filter";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return RndResampleFilterParameters.class;
  }
}
