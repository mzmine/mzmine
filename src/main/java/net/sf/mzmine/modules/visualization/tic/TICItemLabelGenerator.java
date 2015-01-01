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

package net.sf.mzmine.modules.visualization.tic;

import java.util.ArrayList;

import net.sf.mzmine.main.MZmineCore;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Item label generator for TIC visualizer
 * 
 * Basic method for annotation is
 * 
 * 1) Check if this data point is local maximum
 * 
 * 2) Search neighbourhood defined by pixel range for other local maxima with
 * higher intensity
 * 
 * 3) If there is no other maximum, create a label for this one
 * 
 */
class TICItemLabelGenerator implements XYItemLabelGenerator {

    /*
     * Number of screen pixels to reserve for each label, so that the labels do
     * not overlap
     */
    public static final int POINTS_RESERVE_X = 100;
    public static final int POINTS_RESERVE_Y = 100;

    /*
     * Only data points which have intensity >= (dataset minimum value *
     * THRESHOLD_FOR_ANNOTATION) will be annotated
     */
    public static final double THRESHOLD_FOR_ANNOTATION = 2;

    private TICPlot plot;

    /**
     * Constructor
     */
    TICItemLabelGenerator(TICPlot plot) {
	this.plot = plot;
    }

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataSet, int series, int item) {

	// dataSet should be actually TICDataSet
	if (!(dataSet instanceof TICDataSet))
	    return null;
	TICDataSet ticDataSet = (TICDataSet) dataSet;

	// X and Y values of current data point
	double originalX = ticDataSet.getX(0, item).doubleValue();
	double originalY = ticDataSet.getY(0, item).doubleValue();

	// Check if the intensity of this data point is above threshold
	if (originalY < ticDataSet.getMinIntensity() * THRESHOLD_FOR_ANNOTATION)
	    return null;

	// Check if this data point is local maximum
	if (!ticDataSet.isLocalMaximum(item))
	    return null;

	// Calculate data size of 1 screen pixel
	double xLength = (double) plot.getXYPlot().getDomainAxis().getRange()
		.getLength();
	double pixelX = xLength / plot.getWidth();
	double yLength = (double) plot.getXYPlot().getRangeAxis().getRange()
		.getLength();
	double pixelY = yLength / plot.getHeight();

	ArrayList<TICDataSet> allDataSets = new ArrayList<TICDataSet>();

	// Get all data sets of current plot
	for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
	    XYDataset dataset = plot.getXYPlot().getDataset(i);
	    if (dataset instanceof TICDataSet)
		allDataSets.add((TICDataSet) dataset);
	}

	// Check each data set for conflicting data points
	for (TICDataSet checkedDataSet : allDataSets) {

	    // Search for local maxima
	    double searchMinX = originalX - (POINTS_RESERVE_X / 2) * pixelX;
	    double searchMaxX = originalX + (POINTS_RESERVE_X / 2) * pixelX;
	    double searchMinY = originalY;
	    double searchMaxY = originalY + POINTS_RESERVE_Y * pixelY;

	    // We don't want to search below the threshold level of the data set
	    if (searchMinY < (checkedDataSet.getMinIntensity() * THRESHOLD_FOR_ANNOTATION))
		searchMinY = checkedDataSet.getMinIntensity()
			* THRESHOLD_FOR_ANNOTATION;

	    // Do search
	    int foundLocalMaxima[] = checkedDataSet.findLocalMaxima(searchMinX,
		    searchMaxX, searchMinY, searchMaxY);

	    // If we found other maximum then this data point, bail out
	    if (foundLocalMaxima.length > (dataSet == checkedDataSet ? 1 : 0))
		return null;

	}

	// Prepare the label
	double mz = ticDataSet.getZ(0, item).doubleValue();
	String label = MZmineCore.getConfiguration().getMZFormat().format(mz);

	return label;

    }
}
