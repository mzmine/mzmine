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

package net.sf.mzmine.io;

import java.io.File;
import java.io.RandomAccessFile;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.Range;

public interface RawDataFile {
 
	/**
     * Returns file name of raw data file object
     * 
     * @return File file path
     */
	public String getFileName();

    /**
     * Returns File ScanDataFileName
     * 
     * @return File ScanDataFileName
     */	
	public String getScanDataFileName() ;
    /**
     * Returns Opened Random access file ScanDataFile
     * 
     * @return File ScanDataFileName
     */	
    public RandomAccessFile getScanDataFile() ; 
    /**
     * Returns Opened Random access file writingScanDataFile
     * 
     * @return File writingScanDataFileName
     */	
    public RandomAccessFile getWritingScanDataFile() ; 
      /**
     * resetScanDataFile when the file location is changed 
     * 
     * @return 
     */	 
    public void updateScanDataFile(File file);
    
    public PreloadLevel getPreloadLevel();

    public int getNumOfScans();

    public int[] getMSLevels();

    /**
     * Returns sorted array of all scan numbers in this file
     * 
     * @return Sorted array of scan numbers, never returns null
     */
    public int[] getScanNumbers();
    
    /**
     * Returns sorted array of all scan numbers in given MS level
     * 
     * @param msLevel MS level
     * @return Sorted array of scan numbers, never returns null
     */
    public int[] getScanNumbers(int msLevel);

    /**
     * Returns sorted array of all scan numbers in given MS level and retention
     * time range
     * 
     * @param msLevel MS level
     * @param rtMin Minimum retention time
     * @param rtMax Maximum retention time
     * @return Sorted array of scan numbers, never returns null
     */
    public int[] getScanNumbers(int msLevel, float rtMin, float rtMax);


    /**
     * 
     * @param scan Desired scan number
     * @return Desired scan
     */
    public Scan getScan(int scan);

    public Range getDataMZRange();

    public Range getDataRTRange();

    public Range getDataMZRange(int msLevel);

    public Range getDataRTRange(int msLevel);

    public float getDataMaxBasePeakIntensity(int msLevel);

    public float getDataMaxTotalIonCurrent(int msLevel);

    public String toString();

}
