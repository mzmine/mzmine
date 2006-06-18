/*
 * Copyright 2006 The MZmine Development Team
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
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * This class represents a MZmine project.
 * That includes raw data files, processed raw data files,
 * peak lists, alignment results....
 */
public class MZmineProject {

    private static MZmineProject currentProject;
    private Vector<RawDataFile> projectFiles;
    private Hashtable<RawDataFile, PeakList> peakLists;


    public MZmineProject() {
        projectFiles = new Vector<RawDataFile>();
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
        MainWindow.getInstance().getItemSelector().removeRawData(file);
    }

    /**
     */
    public void updateFile(RawDataFile oldFile, RawDataFile newFile) {

        projectFiles.setElementAt(newFile, projectFiles.indexOf(oldFile));
        MainWindow.getInstance().getItemSelector().replaceRawData(oldFile, newFile);
        // TODO: notify visualizers?
    }

    public RawDataFile[] getRawDataFiles() {
        return projectFiles.toArray(new RawDataFile[0]);
    }

	public PeakList getPeakList(RawDataFile rawData) {
		return peakLists.get(rawData);
	}

	public boolean hasPeakList(RawDataFile rawData) {
		return peakLists.containsKey(rawData);
	}

	public void setPeakList(RawDataFile rawData, PeakList peakList) {
		peakLists.put(rawData, peakList);
	}

	public void removePeakList(RawDataFile rawData) {
		peakLists.remove(rawData);
	}



}
