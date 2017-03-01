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

package net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart;

import org.jfree.data.xy.AbstractXYDataset;

public class DiagonalLineDataset extends AbstractXYDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private double min, max;
    private int fold;

    @Override
    public int getSeriesCount() {
	return 3;
    }

    @Override
    public Comparable<Integer> getSeriesKey(int series) {
	return series;
    }

    public int getItemCount(int series) {
	return 2;
    }

    public Number getX(int series, int item) {
	if (item == 0)
	    return min;
	else
	    return max;
    }

    public Number getY(int series, int item) {

	if (item == 0)
	    switch (series) {
	    case 0:
		return (min * fold);
	    case 1:
		return min;
	    case 2:
		return (min / fold);
	    }
	else {

	    switch (series) {
	    case 0:
		return (max * fold);
	    case 1:
		return max;
	    case 2:
		return (max / fold);
	    }
	}

	// We should never get here
	throw (new IllegalStateException());

    }

    public void updateDiagonalData(ScatterPlotDataSet mainDataSet, int fold) {

	this.fold = fold;

	int numOfPoints = mainDataSet.getItemCount(0);

	for (int i = 0; i < numOfPoints; i++) {

	    double x = mainDataSet.getXValue(0, i);
	    double y = mainDataSet.getYValue(0, i);

	    if ((i == 0) || (x < min))
		min = x;
	    if ((i == 0) || (x > max))
		max = x;
	    if (y < min)
		min = y;
	    if (y > max)
		max = y;
	}

	// Add a little space
	min -= min / 2;
	max += max / 2;

	fireDatasetChanged();
    }

    int getFold() {
	return fold;
    }

}
