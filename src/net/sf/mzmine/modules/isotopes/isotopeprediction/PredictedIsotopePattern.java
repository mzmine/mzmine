/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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


package net.sf.mzmine.modules.isotopes.isotopeprediction;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

public class PredictedIsotopePattern implements IsotopePattern {
	
	private MzDataPoint[] dataPoints;
	private String formula;
	private int charge;
	private Range range;
	private IsotopePatternStatus patternStatus = IsotopePatternStatus.PREDICTED;
	private double height = 0.0f;

	
	public PredictedIsotopePattern (MzDataPoint[] dataPoints, String formula, int charge){
		this.dataPoints = dataPoints;
		this.formula = formula;
		this.charge = charge;
		
		range = calculateMzRange(dataPoints);
	}

	public int getCharge() {
		return charge;
	}

	public String getIsotopeInfo() {
		return " Pattern of " + formula + " charge=" + charge;
	}

	public double getIsotopeMass() {
		return dataPoints[0].getMZ();
	}

	public Range getIsotopeMzRange() {
		return range;
	}

	public ChromatographicPeak[] getOriginalPeaks() {
		return null;
	}

	public ChromatographicPeak getRepresentativePeak() {
		return null;
	}

	public double getArea() {
		return 0;
	}

	public RawDataFile getDataFile() {
		return null;
	}

	public double getHeight() {
		return getMostIntenseDataPoint().getIntensity();
	}

	public double getMZ() {
		return getMostIntenseDataPoint().getMZ();
	}
	
	private MzDataPoint getMostIntenseDataPoint(){
		int index = 0;
		double intensity = 0;
		
		for (int i=0; i<dataPoints.length; i++){
			if (dataPoints[i].getIntensity() > intensity){
				index = i;
				intensity = dataPoints[i].getIntensity();
			}
		}
		
		return dataPoints[index];
	}

	public MzDataPoint getDataPoint(int scanNumber) {
		return null;
	}

	public PeakStatus getPeakStatus() {
		return null;
	}

	public double getRT() {
		return 0;
	}

	public Range getRawDataPointsIntensityRange() {
		return null;
	}

	public Range getRawDataPointsMZRange() {
		return null;
	}

	public Range getRawDataPointsRTRange() {
		return null;
	}

	public int getRepresentativeScanNumber() {
		return 0;
	}

	public int[] getScanNumbers() {
		return null;
	}

	public MzDataPoint[] getDataPoints() {
		return dataPoints;
	}

	public int getNumberOfDataPoints() {
		return dataPoints.length;
	}
	
	private Range calculateMzRange(MzDataPoint[] dataPoints){
		double H = 1.0078f;
		Range range = new Range(dataPoints[0].getMZ());
		for (int i=1; i<dataPoints.length; i++){
			range.extendRange(dataPoints[i].getMZ());
		}

		//Extend range by +/- one hydrogen
		range.extendRange(range.getMin() - H);
		range.extendRange(range.getMax() + H);
		
		return range;
	}
	
	public void setIsotopeHeight(double height){
		this.height = height;
	}
	
	public double getIsotopeHeight(){
		return height;
	}

	public IsotopePatternStatus getIsotopePatternStatus() {
		return patternStatus;
	}
	
	public String getFormula(){
		return formula;
	}
	
	public String toString(){
		return formula + " Charge " + charge;
	}

	public int getNumberOfIsotopes() {
		return dataPoints.length;
	}

	public MzDataPoint[] getIsotopes() {
		return dataPoints;
	}

	public int getMostIntenseFragmentScanNumber() {
		return 0;
	}

    public MzDataPoint[] getDataPointsByMass(Range mzRange) {
        return ScanUtils.selectDataPointsByMass(getDataPoints(), mzRange);
    }

    public MzDataPoint[] getDataPointsOverIntensity(double intensity) {
        return ScanUtils.selectDataPointsOverIntensity(getDataPoints(), intensity);
    }

}
