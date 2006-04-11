/**
 * 
 */
package net.sf.mzmine.io.mzxml;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;

/**
 *
 */
class MZXMLScan implements Scan {

    private int scanNumber;
    private MZXMLFile rawDataFile;
    
    private int msLevel;
    
    private double precursorRT, precursorMZ;
    private int precursorScanNumber;
    
    private double scanAcquisitionTime, scanDuration;
    
    private double MZRangeMin;
    private double MZRangeMax;
    
    private double basePeakMZ;
    private double basePeakIntensity;
    
    private double totalIonCurrent;
    
    private double MZValues[], intensityValues[];
    
    /**
     * 
     */
    public MZXMLScan(MZXMLFile rawDataFile, int scanNumber) {
        this.rawDataFile = rawDataFile;
        this.scanNumber = scanNumber;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getRawData()
     */
    public RawDataFile getRawData() {
        return rawDataFile;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }




    /**
     * @return Returns the basePeakIntensity.
     */
    public double getBasePeakIntensity() {
        return basePeakIntensity;
    }

    /**
     * @return Returns the basePeakMZ.
     */
    public double getBasePeakMZ() {
        return basePeakMZ;
    }

    /**
     * @return Returns the intensityValues.
     */
    public double[] getIntensityValues() {
        return intensityValues;
    }

    /**
     * @return Returns the mZRangeMax.
     */
    public double getMZRangeMax() {
        return MZRangeMax;
    }

    /**
     * @return Returns the mZRangeMin.
     */
    public double getMZRangeMin() {
        return MZRangeMin;
    }

    /**
     * @return Returns the mZValues.
     */
    public double[] getMZValues() {
        return MZValues;
    }

    /**
     * @return Returns the precursorMZ.
     */
    public double getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @return Returns the precursorRT.
     */
    public double getPrecursorRT() {
        return precursorRT;
    }

    /**
     * @return Returns the precursorScanNumber.
     */
    public int getPrecursorScanNumber() {
        return precursorScanNumber;
    }

    
    /**
     * @return Returns the scanAcquisitionTime.
     */
    public double getScanAcquisitionTime() {
        return scanAcquisitionTime;
    }

    /**
     * @return Returns the scanDuration.
     */
    public double getScanDuration() {
        return scanDuration;
    }

    /**
     * @return Returns the totalIonCurrent.
     */
    public double getTotalIonCurrent() {
        return totalIonCurrent;
    }

    /**
     * @see net.sf.mzmine.io.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
        return MZValues.length;
    }



}
