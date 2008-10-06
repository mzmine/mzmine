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

import java.util.TreeSet;

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
	private double increase = (double) Math.pow(10, 4);
	private double biggestIntensity = Double.MIN_VALUE;

	// Half of one Hydrogen mass
	private static double TOLERANCE = 0.5039125d;

	
	/*
     * Save a local copy of m/z and intensity values, because accessing the scan
     * every time may cause reloading the data from HDD
     */
    private DataPoint dataPoints[];
    private String label;

    public SpectraDataSet(MzDataTable mzDataTable) {

    	double intensity;
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
        		double probablyIncrease = ((IsotopePattern)mzDataTable).getIsotopeHeight();
        		if (probablyIncrease > 0)
        			increase = probablyIncrease;
                label = "Isotopes (" + dataPoints.length 
        		+ ") "+ ((IsotopePattern)mzDataTable).getIsotopeInfo();
                
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
    
    public double getBiggestIntensity(){
    	if (predicted)
    		return biggestIntensity * increase;
    	else
            return biggestIntensity;
    }
    
    public double getIncrease(){
    	return increase;
    }
    
    public boolean isPredicted(){
    	return predicted;
    }
    
    public double getBiggestIntensity(double mass){
    	
    	TreeSet<Double> intensities = new TreeSet<Double>();
    	double value;
    	int itemCount = getItemCount(0);
    	
    	for (int i=0; i<itemCount; i++){
    		value = Math.abs(mass - getX(0,i).doubleValue());
    		if (value <= TOLERANCE){
    			intensities.add(getY(0,i).doubleValue());
    		}
    	}
    	return intensities.last();
    }

}
