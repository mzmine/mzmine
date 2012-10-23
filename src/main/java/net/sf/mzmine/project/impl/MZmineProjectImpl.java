/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.desktop.impl.projecttree.ProjectTree;
import net.sf.mzmine.desktop.impl.projecttree.ProjectTreeModel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.project.MZmineProject;

/**
 * This class represents a MZmine project. That includes raw data files, peak
 * lists and parameters.
 */
public class MZmineProjectImpl implements MZmineProject {

    private Hashtable<UserParameter, Hashtable<RawDataFile, Object>> projectParametersAndValues;

    private ProjectTreeModel treeModel;

    private File projectFile;

    public MZmineProjectImpl() {

	this.treeModel = new ProjectTreeModel(this);
	projectParametersAndValues = new Hashtable<UserParameter, Hashtable<RawDataFile, Object>>();

    }

    public void activateProject() {

	// If running without GUI, just return
	if (!(MZmineCore.getDesktop() instanceof MainWindow))
	    return;

	Runnable swingThreadCode = new Runnable() {
	    public void run() {
		MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();

		// Update the name of the project in the window title
		mainWindow.updateTitle();

		ProjectTree projectTree = mainWindow.getMainPanel()
			.getProjectTree();
		projectTree.setModel(treeModel);

		// Expand the rows Raw data files and Peak lists items by
		// default
		int childCount = treeModel.getChildCount(treeModel.getRoot());
		for (int i = 0; i < childCount; i++) {
		    TreeNode node = (TreeNode) treeModel.getChild(
			    treeModel.getRoot(), i);
		    TreeNode pathToRoot[] = treeModel.getPathToRoot(node);
		    TreePath path = new TreePath(pathToRoot);
		    projectTree.expandPath(path);
		}
	    }
	};
	try {
	    if (SwingUtilities.isEventDispatchThread())
		swingThreadCode.run();
	    else
		SwingUtilities.invokeAndWait(swingThreadCode);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void addParameter(UserParameter parameter) {
	if (projectParametersAndValues.containsKey(parameter))
	    return;

	Hashtable<RawDataFile, Object> parameterValues = new Hashtable<RawDataFile, Object>();
	projectParametersAndValues.put(parameter, parameterValues);

    }

    public void removeParameter(UserParameter parameter) {
	projectParametersAndValues.remove(parameter);
    }

    public boolean hasParameter(UserParameter parameter) {
	return projectParametersAndValues.containsKey(parameter);
    }

    public UserParameter[] getParameters() {
	return projectParametersAndValues.keySet()
		.toArray(new UserParameter[0]);
    }

    public void setParameterValue(UserParameter parameter,
	    RawDataFile rawDataFile, Object value) {
	if (!(hasParameter(parameter)))
	    addParameter(parameter);
	Hashtable<RawDataFile, Object> parameterValues = projectParametersAndValues
		.get(parameter);
	if (value == null)
	    parameterValues.remove(rawDataFile);
	else
	    parameterValues.put(rawDataFile, value);
    }

    public Object getParameterValue(UserParameter parameter,
	    RawDataFile rawDataFile) {
	if (!(hasParameter(parameter)))
	    return null;
	Object value = projectParametersAndValues.get(parameter).get(
		rawDataFile);

	return value;
    }

    public void addFile(final RawDataFile newFile) {

	assert newFile != null;
	
	Runnable swingCode = new Runnable() {
	    public void run() {
		treeModel.addObject(newFile);
	    }
	};
	try {
	    if (SwingUtilities.isEventDispatchThread())
		swingCode.run();
	    else
		SwingUtilities.invokeAndWait(swingCode);
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    public void removeFile(final RawDataFile file) {

	assert file != null;
	
	Runnable swingCode = new Runnable() {
	    public void run() {
		treeModel.removeObject(file);
	    }
	};
	try {
	    if (SwingUtilities.isEventDispatchThread())
		swingCode.run();
	    else
		SwingUtilities.invokeAndWait(swingCode);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Close the data file, which also removed the temporary data
	file.close();

    }

    public RawDataFile[] getDataFiles() {
	return treeModel.getDataFiles();
    }

    public PeakList[] getPeakLists() {
	return treeModel.getPeakLists();
    }

    public void addPeakList(final PeakList peakList) {

	assert peakList != null;
	
	Runnable swingCode = new Runnable() {
	    public void run() {
		treeModel.addObject(peakList);
	    }
	};
	try {
	    if (SwingUtilities.isEventDispatchThread())
		swingCode.run();
	    else
		SwingUtilities.invokeAndWait(swingCode);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void removePeakList(final PeakList peakList) {

	assert peakList != null;
	
	Runnable swingCode = new Runnable() {
	    public void run() {
		treeModel.removeObject(peakList);
	    }
	};
	try {
	    if (SwingUtilities.isEventDispatchThread())
		swingCode.run();
	    else
		SwingUtilities.invokeAndWait(swingCode);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public PeakList[] getPeakLists(RawDataFile file) {
	PeakList[] currentPeakLists = getPeakLists();
	Vector<PeakList> result = new Vector<PeakList>();
	for (PeakList peakList : currentPeakLists) {
	    if (peakList.hasRawDataFile(file))
		result.add(peakList);
	}
	return result.toArray(new PeakList[0]);

    }

    public File getProjectFile() {
	return projectFile;
    }

    public void setProjectFile(File file) {
	projectFile = file;
	// Notify the tree model to update the name of the project
	treeModel.notifyObjectChanged(this, false);
    }

    public void removeProjectFile() {
	projectFile.delete();
    }

    public String toString() {
	if (projectFile == null)
	    return "New project";
	String projectName = projectFile.getName();
	if (projectName.endsWith(".mzmine")) {
	    projectName = projectName.substring(0, projectName.length() - 7);
	}
	return projectName;
    }

    @Override
    public void notifyObjectChanged(Object object, boolean structureChanged) {
	treeModel.notifyObjectChanged(object, structureChanged);
    }

    public ProjectTreeModel getTreeModel() {
	return treeModel;
    }

}