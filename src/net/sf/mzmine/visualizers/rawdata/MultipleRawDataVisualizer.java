/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata;

import net.sf.mzmine.io.OpenedRawDataFile;




/**
 *
 */
public interface MultipleRawDataVisualizer extends RawDataVisualizer {

    public OpenedRawDataFile[] getRawDataFiles();
    
    public void addRawDataFile(OpenedRawDataFile newFile);
 
    public void removeRawDataFile(OpenedRawDataFile file);
    
}
