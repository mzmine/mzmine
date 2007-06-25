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
     * @param parameter
     */
    public void addParameter(Parameter parameter);

    /**
     * @param parameter
     */
    public void removeParameter(Parameter parameter);

    public void addFile(RawDataFile newFile);

    public void removeFile(RawDataFile file);

    public void updateFile(RawDataFile oldFile, RawDataFile newFile);

    public RawDataFile[] getDataFiles();

    public void addAlignedPeakList(PeakList newResult);

    public void removeAlignedPeakList(PeakList result);

    public PeakList[] getAlignedPeakLists();

    public PeakList getFilePeakList(RawDataFile file);

    public void setFilePeakList(RawDataFile file, PeakList peakList);

}
