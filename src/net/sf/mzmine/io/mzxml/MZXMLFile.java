/**
 * 
 */
package net.sf.mzmine.io.mzxml;

import java.io.File;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;

/**
 *
 */
class MZXMLFile implements RawDataFile {

    /**
     * 
     */
    public MZXMLFile(File file) {
        super();
        // TODO Auto-generated constructor stub
    }

    
    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#reloadFile()
     */
    public void reloadFile() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#getScan(int)
     */
    public Scan getScan(int scan) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#getDataDescription()
     */
    public String getDataDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#getDataMinMZ()
     */
    public double getDataMinMZ() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public double getDataMaxMZ() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see net.sf.mzmine.io.RawDataFile#getTotalRawSignal()
     */
    public double getTotalRawSignal() {
        // TODO Auto-generated method stub
        return 0;
    }


    /**
     * @see net.sf.mzmine.io.RawDataFile#saveFile()
     */
    public void saveFile() {
        // TODO Auto-generated method stub
        
    }


    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * @see net.sf.mzmine.io.RawDataFile#getMSLevels()
     */
    public int[] getMSLevels() {
        // TODO Auto-generated method stub
        return null;
    }

}
