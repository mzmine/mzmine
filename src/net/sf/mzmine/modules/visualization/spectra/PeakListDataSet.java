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

import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.modules.isotopes.isotopeprediction.PredictedIsotopePattern;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Picked peaks data set;
 */
public class PeakListDataSet extends AbstractXYDataset implements IntervalXYDataset {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList peakList;

    private ChromatographicPeak displayedPeaks[];
    private double mzValues[], intensityValues[], increase = -1.0f;
    private boolean isotopeFlag=false,  predicted = false;
    private String label;
    private double thickness = 0.001f;
    private IsotopePatternStatus isotopeStatus;
    private String formula;

    public PeakListDataSet(RawDataFile dataFile, int scanNumber, PeakList peakList) {

        this.peakList = peakList;

        ChromatographicPeak peaks[] = peakList.getPeaks(dataFile);

        Vector<ChromatographicPeak> candidates = new Vector<ChromatographicPeak>();
        for (ChromatographicPeak peak : peaks) {
            DataPoint peakDataPoint = peak.getMzPeak(scanNumber);
            if (peakDataPoint != null)
                candidates.add(peak);
        }
        displayedPeaks = candidates.toArray(new ChromatographicPeak[0]);

        mzValues = new double[displayedPeaks.length];
        intensityValues = new double[displayedPeaks.length];

        for (int i = 0; i < displayedPeaks.length; i++) {
            mzValues[i] = displayedPeaks[i].getMzPeak(scanNumber).getMZ();
            intensityValues[i] = displayedPeaks[i].getMzPeak(scanNumber).getIntensity();
        }
        
        isotopeFlag = false;
        label = "Identified peaks";

    }

    public PeakListDataSet(IsotopePattern isotopePattern) {
    	
    	if (isotopePattern.getIsotopePatternStatus() == IsotopePatternStatus.PREDICTED){
        	DataPoint[] dataPoints = isotopePattern.getDataPoints();

            mzValues = new double[dataPoints.length];
            intensityValues = new double[dataPoints.length];

            for (int i = 0; i < dataPoints.length; i++) {
                mzValues[i] = dataPoints[i].getMZ();
                intensityValues[i] = dataPoints[i].getIntensity();
            }

            label = "Isotopes (" + dataPoints.length 
    		+ ") "+ isotopePattern.getIsotopeInfo();
            
            predicted = true;
            isotopeStatus = IsotopePatternStatus.PREDICTED;
            increase = ((PredictedIsotopePattern)isotopePattern).getIsotopeHeight();
            formula = ((PredictedIsotopePattern)isotopePattern).getFormula();
    		
    	}
    	else{
        	displayedPeaks = isotopePattern.getOriginalPeaks();

            mzValues = new double[displayedPeaks.length];
            intensityValues = new double[displayedPeaks.length];

            for (int i = 0; i < displayedPeaks.length; i++) {
                mzValues[i] = displayedPeaks[i].getMZ();
                intensityValues[i] = displayedPeaks[i].getHeight();
            }
        	label = "Isotopes ("+ ((SimpleIsotopePattern) isotopePattern).getNumberOfIsotopes()
    		+ ") "+ isotopePattern.getIsotopeInfo();
    		
            isotopeStatus = IsotopePatternStatus.DETECTED;
            predicted = false;
    	}

        isotopeFlag = true;
        
    }
    
    public boolean isIsotopeDataSet(){
    	return isotopeFlag;
    }

    public IsotopePatternStatus getIsotopePatternStatus(){
    	if (isIsotopeDataSet())
    		return isotopeStatus;
    	else
    		return null;
    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int series) {
        return label;
    }

    public PeakList getPeakList() {
        return peakList;
    }

    public ChromatographicPeak getPeak(int series, int item) {
        return displayedPeaks[item];
    }

    public int getItemCount(int series) {
        return mzValues.length;
    }

    public Number getX(int series, int item) {
        return mzValues[item];
    }

    public Number getY(int series, int item) {
    	if (predicted)
    		return intensityValues[item] * increase;
    	else
            return intensityValues[item];
    }

    public Number getEndX(int series, int item) {
        return getX(series, item).doubleValue() + thickness;
    }

    public double getEndXValue(int series, int item) {
        return getX(series, item).doubleValue() + thickness;
    }

    public Number getEndY(int series, int item) {
        return getY(series, item);
    }

    public double getEndYValue(int series, int item) {
        return getYValue(series, item);
    }

    public Number getStartX(int series, int item) {
        return getX(series, item).doubleValue() - thickness;
    }

    public double getStartXValue(int series, int item) {
        return getX(series, item).doubleValue() - thickness;
    }

    public Number getStartY(int series, int item) {
        return getY(series, item);
    }

    public double getStartYValue(int series, int item) {
        return getYValue(series, item);
    }
    
    public void setThickness(double thickness){
    	this.thickness = thickness;
    }
    
    public void setIncreaseIntensity (double increase){
    	this.increase = increase;
    }
    
    public double getIncrease(){
    	return increase;
    }
    
    public boolean isPredicted(){
    	return predicted;
    }
    
    public String getFormula(){
    	return formula;
    }
    
    public double getBiggestIntensity(double mass){
    	
    	TreeMap<Double,Integer> scores = new TreeMap<Double,Integer>();
    	double value;
    	int itemCount = mzValues.length;
    	
    	for (int i=0; i<itemCount; i++){
    		value = Math.abs(mass - mzValues[i]);
    		scores.put(value,i);
    	}
    	return intensityValues[scores.get(scores.firstKey())];
    }


}
