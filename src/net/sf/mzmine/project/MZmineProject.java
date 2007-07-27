/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;

/**
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public interface MZmineProject {

    /**
     * Adds a new experimental parameter to the project
     * @param parameter
     */
    public void addParameter(Parameter parameter);

    /**
     * Removes an experimental parameter from the project
     * @param parameter
     */
    public void removeParameter(Parameter parameter);
    
    /**
     * Returns true if project contains the experimental parameter
     */
    public boolean hasParameter(Parameter parameter);
    
    /**
     * Returns all experimental parameter defined in the project
     */
    public Parameter[] getParameters();
    
    /**
     * Sets the value of the parameter in the given raw data file.
     * If experimental parameter does not exists in the project, it is added to the project
     * If parameter has previous value in the given file, the previous value is replaced
     */
    public void setParameterValue(Parameter parameter, RawDataFile rawDataFile, Object value);
    
    /**
	 * Returns the value of an experimental parameter in the given raw data file
     * @param newFile
     */
    public Object getParameterValue(Parameter parameter, RawDataFile rawDataFile);

    public void addFile(RawDataFile newFile);

    public void removeFile(RawDataFile file);

    public RawDataFile[] getDataFiles();

    public void addAlignedPeakList(PeakList newResult);

    public void removeAlignedPeakList(PeakList result);

    public PeakList[] getAlignedPeakLists();

    public PeakList getFilePeakList(RawDataFile file);

    public void setFilePeakList(RawDataFile file, PeakList peakList);

}
