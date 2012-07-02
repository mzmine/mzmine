/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.data.impl;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.util.ScanUtils;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

    private DataPoint dataPoints[], highestIsotope;
    private IsotopePatternStatus status;
    private String description;

    public SimpleIsotopePattern(DataPoint dataPoints[],
	    IsotopePatternStatus status, String description) {

	assert dataPoints.length > 0;

	highestIsotope = ScanUtils.findTopDataPoint(dataPoints);
	this.dataPoints = dataPoints;
	this.status = status;
	this.description = description;
    }

    @Override
    public @Nonnull DataPoint[] getDataPoints() {
	return dataPoints;
    }

    @Override
    public int getNumberOfIsotopes() {
	return dataPoints.length;
    }

    @Override
    public @Nonnull IsotopePatternStatus getStatus() {
	return status;
    }

    @Override
    public @Nonnull DataPoint getHighestIsotope() {
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

}