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
import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * This class represents a MZmine project.
 * That includes raw data files, processed raw data files,
 * peak lists, alignment results.... 
 */
public class MZmineProject {

    private static MZmineProject currentProject;
    private Vector<RawDataFile> projectFiles;
    
    class Operation {
        File previousFileName;
        Method processsingMethod;
        MethodParameters parameters;
    }
    
    /* we have to index by File, not by RawDataFile - that would 
     * cause keeping all previous RawDataFile objects in memory */
    private Hashtable<File, Operation> fileHistory;
    
    public MZmineProject() {
        projectFiles = new Vector<RawDataFile>();
        fileHistory = new Hashtable<File, Operation>();
        currentProject = this;
    }
    
    public static MZmineProject getCurrentProject() {
        assert currentProject != null;
        return currentProject;
    }
    
    void addFile(RawDataFile newFile) {
        projectFiles.add(newFile);
        MainWindow.getInstance().getItemSelector().addRawData(newFile);
    }
    
    public void removeFile(RawDataFile file) {
        projectFiles.remove(file);
        fileHistory.remove(file);
        MainWindow.getInstance().getItemSelector().removeRawData(file);
    }
    
    /**
     */
    public void updateFile(RawDataFile oldFile, RawDataFile newFile, Method processingMethod, MethodParameters methodParameters) {
        Operation op = new Operation();
        op.previousFileName = oldFile.getFileName();
        op.processsingMethod = processingMethod;
        op.parameters = methodParameters;
        projectFiles.setElementAt(newFile, projectFiles.indexOf(oldFile));
        fileHistory.put(newFile.getFileName(), op);
        // TODO: notify visualizers? 
    }
    
    
    
}
