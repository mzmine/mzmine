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
import javax.annotation.Nonnull;

public class RndResampleFilter implements ScanFilter {

  @Override
  public Scan filterScan(RawDataFile newFile, Scan scan, ParameterSet parameters) {

    boolean sum_duplicates =
        parameters.getParameter(RndResampleFilterParameters.SUM_DUPLICATES).getValue();
    boolean remove_zero_intensity =
        parameters.getParameter(RndResampleFilterParameters.REMOVE_ZERO_INTENSITY).getValue();

    // If CENTROIDED scan, use it as-is
    DataPoint dps[];
    if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED)
      dps = ScanUtils.extractDataPoints(scan);
    // Otherwise, detect local maxima
    else
      dps = LocMaxCentroidingAlgorithm.centroidScan(ScanUtils.extractDataPoints(scan));

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
    for(int i = 0; i < newNumOfDataPoints; i++) {
      newDp[0][i] = dps[i].getMZ();
      newDp[1][i] = dps[i].getIntensity();
    }
    // Create updated scan
    SimpleScan newScan = new SimpleScan(newFile, scan, newDp[0], newDp[1]);
    newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

    return newScan;

  }

  @Override
  public @Nonnull String getName() {
    return "Round resampling filter";
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return RndResampleFilterParameters.class;
  }
}
