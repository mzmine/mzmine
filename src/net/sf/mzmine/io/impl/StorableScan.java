/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.io.impl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;

/**
 * Simple implementation of the Scan interface.
 */
public class StorableScan implements Scan {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private int scanNumber, msLevel, parentScan, fragmentScans[];
    private float precursorMZ;
    private int precursorCharge;
    private float retentionTime;
    private float mzRangeMin, mzRangeMax;
    private float basePeakMZ, basePeakIntensity;
    private boolean centroided;
    
    private RandomAccessFile storageFile;
    private long storageFileOffset;
    private int storageArrayByteLength;
    private int numberOfDataPoints;

    /**
     * Clone constructor
     */
    public StorableScan(Scan sc, RandomAccessFile storageFile) {
        this(sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
                sc.getParentScanNumber(), sc.getPrecursorMZ(),
                sc.getFragmentScanNumbers(), sc.getMZValues(),
                sc.getIntensityValues(), sc.isCentroided(), storageFile);
    }

    /**
     * Constructor for creating scan with given data
     */
    public StorableScan(int scanNumber, int msLevel, float retentionTime,
            int parentScan, float precursorMZ, int fragmentScans[],
            float[] mzValues, float[] intensityValues, boolean centroided, RandomAccessFile storageFile) {

        // check assumptions about proper scan data
        assert (msLevel == 1) || (parentScan > 0);
        
        // save storage file reference
        this.storageFile = storageFile;

        // save scan data
        this.scanNumber = scanNumber;
        this.msLevel = msLevel;
        this.retentionTime = retentionTime;
        this.parentScan = parentScan;
        this.precursorMZ = precursorMZ;
        this.fragmentScans = fragmentScans;
        this.centroided = centroided;

        setData(mzValues, intensityValues);

    }

    /**
     * @return Returns the intensityValues.
     */
    public float[] getIntensityValues() {
        ByteBuffer buffer = ByteBuffer.allocate(storageArrayByteLength);
        synchronized(storageFile) {
            try {
            storageFile.seek(storageFileOffset + storageArrayByteLength);
            storageFile.read(buffer.array(), 0, storageArrayByteLength);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not read data from temporary file", e);
                return new float[0];
            }
        }
        float intensityValues[] = new float[numberOfDataPoints];
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.get(intensityValues);
        return intensityValues;
    }

    /**
     * @return Returns the mZValues.
     */
    public float[] getMZValues() {
        ByteBuffer buffer = ByteBuffer.allocate(storageArrayByteLength);
        synchronized(storageFile) {
            try {
            storageFile.seek(storageFileOffset);
            storageFile.read(buffer.array(), 0, storageArrayByteLength);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not read data from temporary file", e);
                return new float[0];
            }
        }
        float mzValues[] = new float[numberOfDataPoints];
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.get(mzValues);
        return mzValues;
    }

    /**
     * @param mzValues m/z values to set
     * @param intensityValues Intensity values to set
     */
    public void setData(float[] mzValues, float[] intensityValues) {

        // check assumptions
        assert mzValues.length == intensityValues.length;

        numberOfDataPoints = mzValues.length;
        
        
        // every float needs 4 bytes
        storageArrayByteLength = numberOfDataPoints * 4;
        
        ByteBuffer buffer = ByteBuffer.allocate(storageArrayByteLength);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        
        synchronized(storageFile) {
            try {
                
                storageFileOffset = storageFile.length();
                
                storageFile.seek(storageFileOffset);
                floatBuffer.put(mzValues);
                storageFile.write(buffer.array());
                floatBuffer.clear();
                floatBuffer.put(intensityValues);
                storageFile.write(buffer.array());
            
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not write data to temporary file", e);
            }
        }

        // find m/z range and base peak
        if (mzValues.length>0) {
	        mzRangeMin = mzValues[0];
	        mzRangeMax = mzValues[0];
	        basePeakMZ = mzValues[0];
	        basePeakIntensity = intensityValues[0];
	        for (int i = 1; i < mzValues.length; i++) {
	            if (mzRangeMin > mzValues[i])
	                mzRangeMin = mzValues[i];
	            if (mzRangeMax < mzValues[i])
	                mzRangeMax = mzValues[i];
	            if (basePeakIntensity < intensityValues[i]) {
	                basePeakIntensity = intensityValues[i];
	                basePeakMZ = mzValues[i];
	            }
	        }
        } else {
        	// Empty scan, so no m/z range or base peak
        	mzRangeMin = 0;
        	mzRangeMax = mzRangeMin;
	        basePeakMZ = 0;
	        basePeakIntensity = 0;        	
        }
        

    }

    /**
     * @see net.sf.mzmine.data.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
        return numberOfDataPoints;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @param scanNumber The scanNumber to set.
     */
    public void setScanNumber(int scanNumber) {
        this.scanNumber = scanNumber;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }

    /**
     * @param msLevel The msLevel to set.
     */
    public void setMSLevel(int msLevel) {
        this.msLevel = msLevel;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getPrecursorMZ()
     */
    public float getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @param precursorMZ The precursorMZ to set.
     */
    public void setPrecursorMZ(float precursorMZ) {
        this.precursorMZ = precursorMZ;
    }

    
    /**
     * @return Returns the precursorCharge.
     */
    public int getPrecursorCharge() {
        return precursorCharge;
    }

    
    /**
     * @param precursorCharge The precursorCharge to set.
     */
    public void setPrecursorCharge(int precursorCharge) {
        this.precursorCharge = precursorCharge;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanAcquisitionTime()
     */
    public float getRetentionTime() {
        return retentionTime;
    }

    /**
     * @param retentionTime The retentionTime to set.
     */
    public void setRetentionTime(float retentionTime) {
        this.retentionTime = retentionTime;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMZRangeMin()
     */
    public float getMZRangeMin() {
        return mzRangeMin;
    }

    /**
     * @param mzRangeMin The mzRangeMin to set.
     */
    public void setMZRangeMin(float mzRangeMin) {
        this.mzRangeMin = mzRangeMin;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMZRangeMax()
     */
    public float getMZRangeMax() {
        return mzRangeMax;
    }

    /**
     * @param mzRangeMax The mzRangeMax to set.
     */
    public void setMZRangeMax(float mzRangeMax) {
        this.mzRangeMax = mzRangeMax;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getBasePeakMZ()
     */
    public float getBasePeakMZ() {
        return basePeakMZ;
    }

    /**
     * @param basePeakMZ The basePeakMZ to set.
     */
    public void setBasePeakMZ(float basePeakMZ) {
        this.basePeakMZ = basePeakMZ;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getBasePeakIntensity()
     */
    public float getBasePeakIntensity() {
        return basePeakIntensity;
    }

    /**
     * @param basePeakIntensity The basePeakIntensity to set.
     */
    public void setBasePeakIntensity(float basePeakIntensity) {
        this.basePeakIntensity = basePeakIntensity;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getParentScanNumber()
     */
    public int getParentScanNumber() {
        return parentScan;
    }

    /**
     * @param parentScan The parentScan to set.
     */
    public void setParentScanNumber(int parentScan) {
        this.parentScan = parentScan;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getFragmentScanNumbers()
     */
    public int[] getFragmentScanNumbers() {
        return fragmentScans;
    }

    /**
     * @param fragmentScans The fragmentScans to set.
     */
    public void setFragmentScanNumbers(int[] fragmentScans) {
        this.fragmentScans = fragmentScans;
    }

    /**
     * @see net.sf.mzmine.data.Scan#isCentroided()
     */
    public boolean isCentroided() {
        return centroided;
    }

    /**
     * @param centroided Is scan centroided?
     */
    public void setCentroided(boolean centroided) {
        this.centroided = centroided;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMassTolerance()
     */
    public float getMassTolerance() {
        return 0.5f;
    }

}
