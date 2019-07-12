package net.sf.mzmine.parameters.parametertypes.filenames;

import java.io.File;
import java.util.List;

/**
 * Listen to changes in a file list
 * 
 * @author Robin Schmid
 *
 */
public interface FileNameListChangedListener {

  public void fileListChanged(List<File> files);
}
