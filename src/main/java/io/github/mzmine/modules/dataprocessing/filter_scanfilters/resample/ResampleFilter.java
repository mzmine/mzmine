/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.resample;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.scans.ScanUtils;

public class ResampleFilter implements ScanFilter {

    public Scan filterScan(Scan scan, ParameterSet parameters) {

        double binSize = parameters
                .getParameter(ResampleFilterParameters.binSize).getValue();

        Range<Double> mzRange = scan.getDataPointMZRange();
        int numberOfBins = (int) Math.round(
                (mzRange.upperEndpoint() - mzRange.lowerEndpoint()) / binSize);
        if (numberOfBins == 0) {
            numberOfBins++;
        }

        // ScanUtils.binValues needs arrays
        DataPoint dps[] = scan.getDataPoints();
        double[] x = new double[dps.length];
        double[] y = new double[dps.length];
        for (int i = 0; i < dps.length; i++) {
            x[i] = dps[i].getMZ();
            y[i] = dps[i].getIntensity();
        }
        // the new intensity values
        double[] newY = ScanUtils.binValues(x, y, mzRange, numberOfBins,
                scan.getSpectrumType() == MassSpectrumType.PROFILE,
                ScanUtils.BinningType.AVG);
        SimpleDataPoint[] newPoints = new SimpleDataPoint[newY.length];

        // set the new m/z value in the middle of the bin
        double newX = mzRange.lowerEndpoint() + binSize / 2.0;
        // creates new DataPoints
        for (int i = 0; i < newY.length; i++) {
            newPoints[i] = new SimpleDataPoint(newX, newY[i]);
            newX += binSize;
        }

        // Create updated scan
        SimpleScan newScan = new SimpleScan(scan);
        newScan.setDataPoints(newPoints);
        newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

        return newScan;
    }

    @Override
    public @Nonnull String getName() {
        return "Resampling filter";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return ResampleFilterParameters.class;
    }
}
