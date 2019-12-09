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

package io.github.mzmine.datamodel.impl;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

    private DataPoint dataPoints[], highestIsotope;
    private IsotopePatternStatus status;
    private String description;
    private Range<Double> mzRange;

    public SimpleIsotopePattern(DataPoint dataPoints[],
            IsotopePatternStatus status, String description) {

        assert dataPoints.length > 0;

        highestIsotope = ScanUtils.findTopDataPoint(dataPoints);
        this.dataPoints = dataPoints;
        this.status = status;
        this.description = description;
        this.mzRange = ScanUtils.findMzRange(dataPoints);
    }

    @Override
    public @Nonnull DataPoint[] getDataPoints() {
        return dataPoints;
    }

    @Override
    public int getNumberOfDataPoints() {
        return dataPoints.length;
    }

    @Override
    public @Nonnull IsotopePatternStatus getStatus() {
        return status;
    }

    @Override
    public @Nonnull DataPoint getHighestDataPoint() {
        return highestIsotope;
    }

    @Override
    public @Nonnull String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Isotope pattern: " + description;
    }

    @Override
    @Nonnull
    public Range<Double> getDataPointMZRange() {
        return mzRange;
    }

    @Override
    public double getTIC() {
        return 0;
    }

    @Override
    public MassSpectrumType getSpectrumType() {
        return MassSpectrumType.CENTROIDED;
    }

    @Override
    @Nonnull
    public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public DataPoint[] getDataPointsOverIntensity(double intensity) {
        throw new UnsupportedOperationException();
    }

}
