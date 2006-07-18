/**
 * 
 */
package net.sf.mzmine.util;

import net.sf.mzmine.io.MZmineOpenedFile;
import net.sf.mzmine.io.RawDataFile;


/**
 *
 */
public class CursorPosition {

    private double retentionTime, mzValue, intensityValue;
    private MZmineOpenedFile dataFile;
    private int scanNumber;

    /**
     * @param retentionTime
     * @param mzValue
     * @param intensityValue
     * @param rawDataFile
     * @param scanNumber
     */
    public CursorPosition(double retentionTime, double mzValue, double intensityValue, MZmineOpenedFile dataFile, int scanNumber) {
        this.retentionTime = retentionTime;
        this.mzValue = mzValue;
        this.intensityValue = intensityValue;
        this.dataFile = dataFile;
        this.scanNumber = scanNumber;
    }


    /**
     * @return Returns the intensityValue.
     */
    public double getIntensityValue() {
        return intensityValue;
    }

    
    /**
     * @param intensityValue The intensityValue to set.
     */
    public void setIntensityValue(double intensityValue) {
        this.intensityValue = intensityValue;
    }

    
    /**
     * @return Returns the mzValue.
     */
    public double getMzValue() {
        return mzValue;
    }

    
    /**
     * @param mzValue The mzValue to set.
     */
    public void setMzValue(double mzValue) {
        this.mzValue = mzValue;
    }

    
    /**
     * @return Returns the retentionTime.
     */
    public double getRetentionTime() {
        return retentionTime;
    }

    
    /**
     * @param retentionTime The retentionTime to set.
     */
    public void setRetentionTime(double retentionTime) {
        this.retentionTime = retentionTime;
    }


    
    /**
     * @return Returns the dataFile.
     */
    public MZmineOpenedFile getDataFile() {
        return dataFile;
    }


    
    /**
     * @param dataFile The dataFile to set.
     */
    public void setDataFile(MZmineOpenedFile dataFile) {
        this.dataFile = dataFile;
    }


    
    /**
     * @return Returns the scanNumber.
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
    
}
