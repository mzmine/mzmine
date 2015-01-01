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

package net.sf.mzmine.util;

import java.util.Comparator;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;

/**
 * Compare peak list rows either by ID, average m/z or median area of peaks
 * 
 */
public class PeakListRowSorter implements Comparator<PeakListRow> {

    private SortingProperty property;
    private SortingDirection direction;

    public PeakListRowSorter(SortingProperty property,
	    SortingDirection direction) {
	this.property = property;
	this.direction = direction;
    }

    public int compare(PeakListRow row1, PeakListRow row2) {

	Double row1Value = getValue(row1);
	Double row2Value = getValue(row2);

	if (direction == SortingDirection.Ascending)
	    return row1Value.compareTo(row2Value);
	else
	    return row2Value.compareTo(row1Value);

    }

    private double getValue(PeakListRow row) {
	switch (property) {
	case Area:
	    Feature[] areaPeaks = row.getPeaks();
	    double[] peakAreas = new double[areaPeaks.length];
	    for (int i = 0; i < peakAreas.length; i++)
		peakAreas[i] = areaPeaks[i].getArea();
	    double medianArea = MathUtils.calcQuantile(peakAreas, 0.5);
	    return medianArea;
	case Intensity:
	    Feature[] intensityPeaks = row.getPeaks();
	    double[] peakIntensities = new double[intensityPeaks.length];
	    for (int i = 0; i < intensityPeaks.length; i++)
		peakIntensities[i] = intensityPeaks[i].getArea();
	    double medianIntensity = MathUtils.calcQuantile(peakIntensities,
		    0.5);
	    return medianIntensity;
	case Height:
	    Feature[] heightPeaks = row.getPeaks();
	    double[] peakHeights = new double[heightPeaks.length];
	    for (int i = 0; i < peakHeights.length; i++)
		peakHeights[i] = heightPeaks[i].getHeight();
	    double medianHeight = MathUtils.calcQuantile(peakHeights, 0.5);
	    return medianHeight;
	case MZ:
	    return row.getAverageMZ();
	case RT:
	    return row.getAverageRT();
	case ID:
	    return row.getID();
	}

	// We should never get here, so throw exception
	throw (new IllegalStateException());
    }

}