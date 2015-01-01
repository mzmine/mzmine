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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart;

import java.util.ArrayList;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotAxisSelection;
import net.sf.mzmine.util.SearchDefinition;

import org.jfree.data.xy.AbstractXYDataset;

/**
 * 
 * This data set contains 2 series: first series (index 0) contains all peak
 * list rows. Second series (index 1) contains those peak list rows which
 * conform to current search definition (currentSearch).
 * 
 */
public class ScatterPlotDataSet extends AbstractXYDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private PeakListRow displayedRows[], selectedRows[];

    private ScatterPlotAxisSelection axisX, axisY;
    private SearchDefinition currentSearch;

    // We use this value for zero data points, because zero cannot be plotted in
    // the log-scale scatter plot
    private double defaultValue;

    public ScatterPlotDataSet(PeakList peakList) {
	this.displayedRows = peakList.getRows();
    }

    void setDisplayedAxes(ScatterPlotAxisSelection axisX,
	    ScatterPlotAxisSelection axisY) {

	this.axisX = axisX;
	this.axisY = axisY;

	// Update the default value to minimum value divided by 2
	double minValue = Double.MAX_VALUE;
	for (PeakListRow row : displayedRows) {
	    double valX = axisX.getValue(row);
	    double valY = axisX.getValue(row);
	    if ((valX > 0) && (valX < minValue))
		minValue = valX;
	    if ((valY > 0) && (valY < minValue))
		minValue = valY;
	}
	this.defaultValue = minValue / 2;

	updateSearchDefinition(currentSearch);

    }

    public PeakListRow getRow(int series, int item) {
	if (series == 0)
	    return displayedRows[item];
	else
	    return selectedRows[item];
    }

    @Override
    public int getSeriesCount() {
	if ((displayedRows == null) || (axisX == null) || (axisY == null))
	    return 0;
	if ((selectedRows == null) || (selectedRows.length == 0))
	    return 1;
	return 2;
    }

    @Override
    public Comparable<Integer> getSeriesKey(int series) {
	return series;
    }

    /**
     * 
     */
    public int getItemCount(int series) {
	if (series == 0)
	    return displayedRows.length;
	else
	    return selectedRows.length;
    }

    /**
     * 
     */
    public Number getX(int series, int item) {
	double value;
	if (series == 0)
	    value = axisX.getValue(displayedRows[item]);
	else
	    value = axisX.getValue(selectedRows[item]);
	// We must not return zero, because it cannot be plotted
	if (value > 0)
	    return value;
	else
	    return defaultValue;
    }

    /**
     * 
     */
    public Number getY(int series, int item) {
	double value;
	if (series == 0)
	    value = axisY.getValue(displayedRows[item]);
	else
	    value = axisY.getValue(selectedRows[item]);
	// We must not return zero, because it cannot be plotted
	if (value > 0)
	    return value;
	else
	    return defaultValue;
    }

    /**
     * Returns the peak list row which exactly matches given X and Y values
     */
    public PeakListRow getRow(double valueX, double valueY) {

	for (int i = 0; i < displayedRows.length; i++) {
	    if ((Math.abs(valueX - getXValue(0, i)) < 0.0000001)
		    && (Math.abs(valueY - getYValue(0, i)) < 0.0000001))
		return displayedRows[i];
	}
	return null;
    }

    void updateSearchDefinition(SearchDefinition newSearch) {

	this.currentSearch = newSearch;

	if (newSearch == null) {
	    this.selectedRows = new PeakListRow[0];
	} else {
	    ArrayList<PeakListRow> selected = new ArrayList<PeakListRow>();
	    for (PeakListRow row : displayedRows) {
		if (newSearch.conforms(row)) {
		    selected.add(row);
		}
	    }
	    this.selectedRows = selected.toArray(new PeakListRow[0]);
	}

	fireDatasetChanged();
    }

}
