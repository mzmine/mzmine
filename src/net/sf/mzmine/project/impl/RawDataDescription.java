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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */


package net.sf.mzmine.project.impl;

import java.io.Serializable;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;

public class RawDataDescription implements Serializable{
	private int numOfScans;	
	private int[] numOfDatapoints;
	private int[] msLevel;
	private double[]retentionTime;
	private int[] parentScan;
	private double[] precursorMZ;
	private int[][] fragmentScans;
	private boolean[] centroided;
	private int[] scanNumbers;
	private String Name;

	public RawDataDescription(RawDataFile rawDataFile){
		this.numOfScans = rawDataFile.getNumOfScans();
		scanNumbers = rawDataFile.getScanNumbers();
		int cont =0;
		this.numOfDatapoints = new int[scanNumbers.length];
		this.msLevel = new int[scanNumbers.length];
		this.centroided = new boolean[scanNumbers.length];
		this.retentionTime = new double[scanNumbers.length];
		this.precursorMZ = new double[scanNumbers.length];
		this.parentScan = new int [scanNumbers.length];
		this.fragmentScans= new int[scanNumbers.length][];
		for(int scanNumber : scanNumbers){
			Scan scan = rawDataFile.getScan(scanNumber);
			numOfDatapoints[cont] = scan.getDataPoints().length;
			this.msLevel[cont] = scan.getMSLevel();
			this.centroided[cont] = scan.isCentroided();
			this.retentionTime[cont] = scan.getRetentionTime();
			this.parentScan[cont] = scan.getParentScanNumber();
			this.precursorMZ[cont] = scan.getPrecursorMZ();
			this.fragmentScans[cont++]=scan.getFragmentScanNumbers();
		}
		this.Name = rawDataFile.getName();
	}

	public String getName(){
		return Name;
	}

	public int getNumOfDataPoints(int scan){
		return numOfDatapoints[scan];
	}

	public int getNumOfScans(){
		return this.numOfScans;
	}

	public int getMsLevel(int i){
		return this.msLevel[i];
	}

	public boolean isCentroided(int i){
		return this.centroided[i];
	}

	public double getRetentionTime(int i){
		return this.retentionTime[i];
	}
	public double getPrecursorMZ(int i){
		return this.precursorMZ[i];
	}
	public int getParentScan(int i){
		return this.parentScan[i];
	}
	public int[] getFragmentScans(int i){
		return this.fragmentScans[i];
	}

	public int getScanNumber(int i){
		return this.scanNumbers[i];
	}

}
