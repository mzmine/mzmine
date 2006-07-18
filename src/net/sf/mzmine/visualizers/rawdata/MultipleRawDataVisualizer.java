/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata;

import net.sf.mzmine.io.MZmineOpenedFile;




/**
 *
 */
public interface MultipleRawDataVisualizer extends RawDataVisualizer {

    public MZmineOpenedFile[] getRawDataFiles();
    
    public void addRawDataFile(MZmineOpenedFile newFile);
 
    public void removeRawDataFile(MZmineOpenedFile file);
    
}
