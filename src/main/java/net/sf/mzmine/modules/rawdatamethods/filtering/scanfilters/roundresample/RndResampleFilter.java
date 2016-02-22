/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.roundresample;

import javax.annotation.Nonnull;

import java.util.Arrays;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilter;
import net.sf.mzmine.parameters.ParameterSet;

public class RndResampleFilter implements ScanFilter {

    public Scan filterScan(Scan scan, ParameterSet parameters) {

        boolean sum_duplicates = parameters.getParameter(
                RndResampleFilterParameters.SUM_DUPLICATES).getValue();
        boolean remove_zero_intensity = parameters.getParameter(
                RndResampleFilterParameters.REMOVE_ZERO_INTENSITY).getValue();

        // If CENTROIDED scan, use it as-is
        Scan inputScan;
        if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED)
            inputScan = scan;
        // Otherwise, detect local maxima
        else
            inputScan = new LocMaxCentroidingAlgorithm(scan).centroidScan();

        DataPoint dps[] = inputScan.getDataPoints();

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
                        dps[newNumOfDataPoints - 1] = new SimpleDataPoint(
                                prevMz, newIntensity);
                    } else {
                        // Use average
                        newIntensity += newDps[i].getIntensity();
                        dps[newNumOfDataPoints - 1] = new SimpleDataPoint(
                                prevMz, newIntensity);
                        divider += 1.0;
                    }
                } else {
                    dps[newNumOfDataPoints - 1] = new SimpleDataPoint(prevMz,
                            newIntensity / divider);

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

        // Create updated scan
        SimpleScan newScan = new SimpleScan(inputScan);
        newScan.setDataPoints(Arrays.copyOfRange(dps, 0, newNumOfDataPoints));
        newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

        return newScan;

    }

    @Override
    public @Nonnull
    String getName() {
        return "Round resampling filter";
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return RndResampleFilterParameters.class;
    }
}
