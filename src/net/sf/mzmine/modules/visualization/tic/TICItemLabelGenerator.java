/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.tic;

import java.awt.event.ActionListener;
import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilderSetupDialog;

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
    public static final float THRESHOLD_FOR_ANNOTATION = 2f;

    /*
     * Some saved values
     */
    private TICPlot plot;
    //private TICVisualizerWindow ticWindow;
    private ActionListener ticWindow;
    private Object plotType;
    private NumberFormat mzFormat = MZmineCore.getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

    /**
     * Constructor
     */
    TICItemLabelGenerator(TICPlot plot, ActionListener ticWindow) {
    	if (ticWindow instanceof TICVisualizerWindow)
    		plotType = ((TICVisualizerWindow) ticWindow).getPlotType();
    	else
    		plotType = TICVisualizerParameters.plotTypeBP;
        this.plot = plot;
        this.ticWindow = ticWindow;
    }

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataSet, int series, int item) {

        // dataSet is actually TICDataSet
        TICDataSet ticDataSet = (TICDataSet) dataSet;

        // X and Y values of current data point
        float originalX = ticDataSet.getX(0, item).floatValue();
        float originalY = ticDataSet.getY(0, item).floatValue();

        // Check if the intensity of this data point is above threshold
        if (originalY < ticDataSet.getMinIntensity() * THRESHOLD_FOR_ANNOTATION)
            return null;

        // Check if this data point is local maximum
        if (!ticDataSet.isLocalMaximum(item))
            return null;

        // Calculate data size of 1 screen pixel
        float xLength = (float) plot.getXYPlot().getDomainAxis().getRange().getLength();
        float pixelX = xLength / plot.getWidth();
        float yLength = (float) plot.getXYPlot().getRangeAxis().getRange().getLength();
        float pixelY = yLength / plot.getHeight();

        TICDataSet[] allDataSets={};
        
        // Get all data sets of current plot
    	if (ticWindow instanceof TICVisualizerWindow){
    		allDataSets = ((TICVisualizerWindow) ticWindow).getAllDataSets();
    	}
    	if (ticWindow instanceof PeakBuilderSetupDialog){
    		allDataSets = ((PeakBuilderSetupDialog) ticWindow).getDataSet();
    	}

    	
        // Check each data set for conflicting data points
        for (TICDataSet checkedDataSet : allDataSets) {

            // Search for local maxima
            float searchMinX = originalX - (POINTS_RESERVE_X / 2) * pixelX;
            float searchMaxX = originalX + (POINTS_RESERVE_X / 2) * pixelX;
            float searchMinY = originalY;
            float searchMaxY = originalY + POINTS_RESERVE_Y * pixelY;

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
        String label = null;

        // Base peak plot shows m/z, TIC shows total intensity
        if (plotType == TICVisualizerParameters.plotTypeBP) {
            float mz = ticDataSet.getZ(0, item).floatValue();
            label = mzFormat.format(mz);
        } else {
            label = intensityFormat.format(originalY);
        }

        return label;

    }
}
