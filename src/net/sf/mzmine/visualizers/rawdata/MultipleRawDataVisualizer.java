/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata;

import net.sf.mzmine.io.RawDataFile;



/**
 *
 */
public interface MultipleRawDataVisualizer extends RawDataVisualizer {

    public void addRawDataFile(RawDataFile newFile);
 
    public void removeRawDataFile(RawDataFile file);
    
}
