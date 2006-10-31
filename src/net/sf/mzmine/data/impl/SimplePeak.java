/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.data.impl;

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.ArrayList;


import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.impl.AbstractDataUnit;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;

/**
 * This class is a simple implementation of the peak interface.
 */
public class SimplePeak extends AbstractDataUnit implements Peak {

	private PeakStatus peakStatus;

	// This table maps a scanNumber to an array of m/z and intensity pairs
	private Hashtable<Integer, double[][]> datapointsMap; 

	// Raw M/Z, RT, Height and Area
	private double mz;
	private double rt;
	private double height;
	private double area;

	// Normalized versions of peak's basic properties
	private double normalizedMZ;
	private double normalizedRT;
	private double normalizedHeight;
	private double normalizedArea;
	
	// Boundaries of the peak 
	private double minRT;
	private double maxRT;
	private double minMZ;
	private double maxMZ;
	

	/**
	 * Initializes a new peak using given values
	 * @param	datapointsPerScan	first index equals index in scanNumbers, second index is the number of datapoint in scan, length of last dimension is 2: index value 0=M/Z, 1=Intensity of data point  
	 */
	public SimplePeak(	double rawMZ, 
						double rawRT, 
						double rawHeight,
						double rawArea,
						double normalizedMZ,
						double normalizedRT,
						double normalizedHeight,
						double normalizedArea,
						double minMZ,
						double maxMZ,
						double minRT,
						double maxRT,
						int[] scanNumbers,
						double[][][] datapointsPerScan,
						PeakStatus peakStatus) {
		this.mz = rawMZ;
		this.rt = rawRT;
		this.height = rawHeight;
		this.area = rawArea;
		
		this.normalizedMZ = normalizedMZ;
		this.normalizedRT = normalizedRT;
		this.normalizedHeight = normalizedHeight;
		this.normalizedArea = normalizedArea;
		
		this.minMZ = minMZ;
		this.maxMZ = maxMZ;
		this.minRT = minRT;
		this.maxRT = maxRT;
		
		datapointsMap = new Hashtable<Integer, double[][]>();
		for (int ind=0; ind<scanNumbers.length; ind++) {
			datapointsMap.put(scanNumbers[ind], datapointsPerScan[ind]);
		}
		
		this.peakStatus = peakStatus;
		
	}
	
	public SimplePeak(Peak p) {

		this.mz = p.getRawMZ();
		this.rt = p.getRawRT();
		this.height = p.getRawHeight();
		this.area = p.getRawArea();
		
		this.normalizedMZ = p.getNormalizedMZ();
		this.normalizedRT = p.getNormalizedRT();
		this.normalizedHeight = p.getNormalizedHeight();
		this.normalizedArea = p.getNormalizedArea();
		
		this.minMZ = p.getMinMZ();
		this.maxMZ = p.getMaxMZ();
		this.minRT = p.getMinRT();
		this.maxRT = p.getMaxRT();
		
		datapointsMap = new Hashtable<Integer, double[][]>();
		for (int scanNumber : p.getScanNumbers())
			datapointsMap.put(scanNumber, p.getRawDatapoints(scanNumber));
		
		this.peakStatus = p.getPeakStatus();
		
	}
	

	/**
	 * This method returns the status of the peak
	 */
	public PeakStatus getPeakStatus() {
		return peakStatus;
	}


	/* Get methods for basic properties of the peak as defined by the peak picking method */

	/**
	 * This method returns M/Z value of the peak
	 */
	public double getRawMZ() {
		return mz;
	}

	/**
	 * This method returns retention time of the peak
	 */
	public double getRawRT() {
		return rt;
	}

	/**
	 * This method returns the raw height of the peak
	 */
	public double getRawHeight() {
		return height;
	}

	/**
	 * This method returns the raw area of the peak
	 */
	public double getRawArea() {
		return area;
	}


	/**
	 * This method returns numbers of scans that contain this peak
	 */
	public int[] getScanNumbers() {	
		return CollectionUtils.toIntArray(datapointsMap.keySet());
	}

	/**
	 * This method returns an array of double[2] (mz and intensity) points for a given scan number
	 */
	public double[][] getRawDatapoints(int scanNumber) {

		double[][] datapoints = datapointsMap.get(scanNumber);
		
		if (datapoints==null) datapoints = new double[0][2];
			
		return datapoints;
	}

	/**
	 * Returns the first scan number of all datapoints
	 */
	public double getMinRT() {
		return minRT;
	}

	/**
	 * Returns the last scan number of all datapoints
	 */
	public double getMaxRT() {
		return maxRT;
	}

	/**
	 * Returns minimum M/Z value of all datapoints
	 */
	public double getMinMZ() {
		return minMZ;
	}

	/**
	 * Returns maximum M/Z value of all datapoints
	 */
	public double getMaxMZ() {
		return maxMZ;
	}

	/**
	 * Returns the normalized M/Z of the peak
	 */
	public double getNormalizedMZ() {
		return normalizedMZ;
	}

	/**
	 * Returns the normalized RT of the peak
	 */
	public double getNormalizedRT() {
		return normalizedRT;
	}

	/**
	 * Returns the normalized height of the peak
	 */
	public double getNormalizedHeight() {
		return normalizedHeight;
	}

	/**
	 * Returns the normalized area of the peak
	 */
	public double getNormalizedArea() {
		return normalizedArea;
	}

	public void setNormalizedMZ(double normalizedMZ) {
		this.normalizedMZ = normalizedMZ;
	}
	
	public void setNormalizedRT(double normalizedRT) {
		this.normalizedRT = normalizedRT;
	}
	
	public void setNormalizedHeight(double normalizedHeight) {
		this.normalizedHeight = normalizedHeight;
	}
	
	public void setNormalizedArea(double normalizedArea) {
		this.normalizedArea = normalizedArea;
	}


}
