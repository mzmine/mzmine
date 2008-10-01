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

package net.sf.mzmine.modules.visualization.spectra;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.MzDataTable;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.isotopes.isotopeprediction.PredictedIsotopePattern;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Spectra visualizer data set for scan data points
 */
public class SpectraDataSet extends AbstractXYDataset implements IntervalXYDataset {

	private boolean predicted = false;
	private float increase = (float) Math.pow(10, 4);
	private float biggestIntensity = Float.MIN_VALUE;
	
	/*
     * Save a local copy of m/z and intensity values, because accessing the scan
     * every time may cause reloading the data from HDD
     */
    private DataPoint dataPoints[];
    private String label;

    public SpectraDataSet(MzDataTable mzDataTable) {

    	float intensity;
    	dataPoints = mzDataTable.getDataPoints();
        
    	for (DataPoint dp: dataPoints){
    		intensity = dp.getIntensity();
    		if ( intensity > biggestIntensity) {
    			biggestIntensity = intensity;
    		}
    	}

    	boolean isotopeFlag = mzDataTable instanceof IsotopePattern;
    	if (isotopeFlag){
    		predicted = ((IsotopePattern)mzDataTable).getIsotopePatternStatus() == IsotopePatternStatus.PREDICTED;
    		if (predicted){
        		float probablyIncrease = ((PredictedIsotopePattern)mzDataTable).getIsotopeHeight();
        		if (probablyIncrease > 0)
        			increase = probablyIncrease;
    			label = "Predicted isotope pattern";
    		}
    		else {
        		label = "Raw data";
    		}
    	}
    	else{
    		label = "Scan #" + ((Scan) mzDataTable).getScanNumber();
    	}
    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int series) {
   		return label;
    }

    public int getItemCount(int series) {
        return dataPoints.length;
    }

    public Number getX(int series, int item) {
        return dataPoints[item].getMZ();
    }

    public Number getY(int series, int item) {
    	if (predicted)
    		return dataPoints[item].getIntensity() * increase;
    	else
            return dataPoints[item].getIntensity();

    }

    public Number getEndX(int series, int item) {
        return getX(series, item);
    }

    public double getEndXValue(int series, int item) {
        return getXValue(series, item);
    }

    public Number getEndY(int series, int item) {
        return getY(series, item);
    }

    public double getEndYValue(int series, int item) {
        return getYValue(series, item);
    }

    public Number getStartX(int series, int item) {
        return getX(series, item);
    }

    public double getStartXValue(int series, int item) {
        return getXValue(series, item);
    }

    public Number getStartY(int series, int item) {
        return getY(series, item);
    }

    public double getStartYValue(int series, int item) {
        return getYValue(series, item);
    }
    
    public float getBiggestIntensity(){
    	if (predicted)
    		return biggestIntensity * increase;
    	else
            return biggestIntensity;
    }
    
    public float getIncrease(){
    	return increase;
    }

}
