/**
 * 
 */
package net.sf.mzmine.io.netcdf;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;

/**
 *
 */
public class NetCDFFile implements RawDataFile {

    /**
     * 
     */
    public NetCDFFile() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#reloadFile()
     */
    public void reloadFile() {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#saveFile()
     */
    public void saveFile() {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScan(int)
     */
    public Scan getScan(int scan) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataDescription()
     */
    public String getDataDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinMZ()
     */
    public double getDataMinMZ() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public double getDataMaxMZ() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getTotalRawSignal()
     */
    public double getTotalRawSignal() {
        // TODO Auto-generated method stub
        return 0;
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
