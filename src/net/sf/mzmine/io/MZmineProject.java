/*
 * Copyright 2006 Okinawa Institute of Science and Technology
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

package net.sf.mzmine.io;

import java.io.File;
import java.util.Vector;

import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.peakpicking.PeakList;

/**
 * This class represents a data file open in MZmine.
 * That includes original raw data files, temporary (processed) raw data files,
 * peak lists, alignment results.... 
 */
public class MZmineProject {

    private RawDataFile currentFile;
    private Vector<ProcessedFile> fileHistory;
    
    class ProcessedFile {
        RawDataFile data;
        Method processsingMethod;
        MethodParameters parameters;
    }
    
    public RawDataFile getCurrentFile() {
        return currentFile;
    }
 
    MZmineProject(RawDataFile parsedFile) {
        currentFile = parsedFile;
        // originalFileName = parsedFile.getFileName();
        fileHistory = new Vector<ProcessedFile>();
    }
    
    /**
     * TODO: how to notify visualizers? 
     * @param newFile
     * @param methodParameters
     */
    public void updateCurrentFile(RawDataFile newFile, Method processingMethod, MethodParameters methodParameters) {
        currentFile = newFile;
        ProcessedFile historyRecord = new ProcessedFile();
        historyRecord.data = newFile;
        historyRecord.processsingMethod = processingMethod;
        historyRecord.parameters = methodParameters;
    }
    

    
    public File[] getTemporaryFiles() {
        return null;
    }
    
    public PeakList getPeakList() {
        return null;
    }
    
    public AlignmentResult getAlignmentResult() {
        return null;
    }
    
    
}
