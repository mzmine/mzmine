/**
 * 
 */
package net.sf.mzmine.visualizers;

import net.sf.mzmine.io.RawDataFile;




/**
 *
 */
public interface MultipleRawDataVisualizer extends RawDataVisualizer {

    public RawDataFile[] getRawDataFiles();
    
    public void addRawDataFile(RawDataFile newFile);
 
    public void removeRawDataFile(RawDataFile file);
    
}
