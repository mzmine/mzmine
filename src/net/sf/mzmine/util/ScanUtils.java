/**
 * 
 */
package net.sf.mzmine.util;

import net.sf.mzmine.interfaces.Scan;


/**
 *
 */
public class ScanUtils {

    /**
     * Find a base peak of a given scan in a given m/z range 
     * @param scan Scan to search
     * @param mzMin m/z range minimum
     * @param mzMax m/z range maximum 
     * @return double[2] containing base peak m/z and intensity
     */
    public static double[] findBasePeak(Scan scan, double mzMin, double mzMax) {
        
        double mzValues[] = scan.getMZValues();
        double intensityValues[] = scan.getIntensityValues();
        double basePeak[] = new double[2];
        
        for (int i = 1; i < mzValues.length; i++) {
            
            if ((mzValues[i] >= mzMin) && (mzValues[i] <= mzMax) && (intensityValues[i] > basePeak[1])) {
                basePeak[0] = mzValues[i];
                basePeak[1] = intensityValues[i];
            }
            
        }
        
        return basePeak;
    }
    
}
