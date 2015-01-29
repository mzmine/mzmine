/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.mztabimport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.desktop.impl.projecttree.ProjectTree;
import net.sf.mzmine.desktop.impl.projecttree.RawDataTreeModel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportModule;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.impl.TaskQueue;
import uk.ac.ebi.pride.jmztab.model.Assay;
import uk.ac.ebi.pride.jmztab.model.MZTabFile;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.SmallMolecule;
import uk.ac.ebi.pride.jmztab.model.SplitList;
import uk.ac.ebi.pride.jmztab.model.StudyVariable;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;

import com.google.common.collect.Range;

class MzTabImportTask extends AbstractTask {

    // parameter values
    private File file;
    private boolean importrawfiles;
    private double currentStage = 0;

    MzTabImportTask(ParameterSet parameters) {
	this.file = parameters.getParameter(MzTabImportParameters.file).getValue();
	this.importrawfiles = parameters.getParameter(MzTabImportParameters.importrawfiles).getValue();
    }

    public double getFinishedPercentage() {
	/**
	 * TODO: WRITE PERCENTRAGE HANDLER!
	 **/
	//return 0.5d;
	return (currentStage * 1/10);
    }

    public String getTaskDescription() {
	return "Loading data from " + file;
    }

    public void cancel() {
	System.out.println("Cancel!");
	super.cancel();
    }

    public void run() {

	setStatus(TaskStatus.PROCESSING);

	try {
	    // Block MZTabFileParser from writing to console 
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(baos);

	    // Load mzTab file
	    MZTabFileParser mzTabFileParser = new MZTabFileParser(file, ps);
	    MZTabFile mzTabFile = mzTabFileParser.getMZTabFile();
	    currentStage=1;

	    // Import raw data files
	    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

	    SortedMap<Integer, MsRun> msrun = mzTabFile.getMetadata().getMsRunMap();
	    RawDataFileWriter newMZmineFile;
	    RawDataFile[] rawDataFiles = new RawDataFile[msrun.size()];

	    final RawDataImportModule RDI = new RawDataImportModule();
	    ParameterSet parameters = RDI.getParameterSetClass().newInstance();
	    List<Task> tasksList = new ArrayList<Task>();
	    File newFiles[] = new File[msrun.size()];

	    int rawFileCounter = 0;
    	    for(Entry<Integer, MsRun> entry : msrun.entrySet()) {
    		File testFile = new File(entry.getValue().getLocation().getPath());
    	        File f = new File(entry.getValue().getLocation().getPath());
    	        newMZmineFile = MZmineCore.createNewFile(f.getName());
    	        RawDataFile newDataFile2 = newMZmineFile.finishWriting();

    	        if (importrawfiles) {
    	    	    if(testFile.exists() && !testFile.isDirectory()) {
    	    		newFiles[rawFileCounter] = new File(entry.getValue().getLocation().getPath());
    	    	    }

    	    	    else {
    	    		// Add dummy raw file
            		project.addFile(newDataFile2);
    	    	    }
    	    	}
    	    	else {
    	    	    // Add dummy raw file
    	    	    project.addFile(newDataFile2);
    	    	}

    	        currentStage=currentStage+8/msrun.size();
    	        rawFileCounter++;
    	    }

    	    parameters.getParameter(RawDataImportParameters.fileNames).setValue(newFiles);
    	    RDI.runModule(project, parameters, tasksList);

    	    // Process tasks
    	    Task newTasks[] = new Task[1];
    	    for (Task stepTask : tasksList) {
		newTasks[0] = stepTask;
		MZmineCore.getTaskController().addTasks(newTasks);
    	    }

	    // Wait until all raw data file imports have completed
	    TaskQueue taskQueue = MZmineCore.getTaskController().getTaskQueue();
	    while (taskQueue.getNumOfWaitingTasks() > 1) { 
		if (isCanceled()) { return; }
		Thread.sleep(1000);
	    }
	    currentStage=9;

	    // Sort raw data files based on order in mzTab file
	    // Get all rows in raw data file tree
	    MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
	    ProjectTree rawDataTree = mainWindow.getMainPanel().getRawDataTree();
	    MZmineProjectImpl project2 = (MZmineProjectImpl) MZmineCore.getProjectManager().getCurrentProject();
	    final RawDataTreeModel treeModel = project2.getRawDataTreeModel();
	    final DefaultMutableTreeNode rootNode = treeModel.getRoot();
	    int[] selectedRows = new int[rootNode.getChildCount()];
	    for (int i=1; i<rootNode.getChildCount()+1; i++) {
		selectedRows[i-1] = i;
	    }
	    final ArrayList<DefaultMutableTreeNode> selectedNodes = new ArrayList<DefaultMutableTreeNode>();
	    for (int row : selectedRows) {
		TreePath path = rawDataTree.getPathForRow(row);
		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		selectedNodes.add(selectedNode);
	    }

	    // Reorder the nodes in the tree model based on order in mzTab file
	    int fileCounter = 0;
	    for(Entry<Integer, MsRun> entry : msrun.entrySet()) {
		fileCounter++;
	    	File f = new File(entry.getValue().getLocation().getPath());
	    	for (DefaultMutableTreeNode node : selectedNodes) {
		    if (node.toString().equals(f.getName())) {
		        treeModel.removeNodeFromParent(node);
		        treeModel.insertNodeInto(node, rootNode, fileCounter-1);
		    }
	    	}
	    }

	    // Add sample parameters if available in mzTab file
	    fileCounter = 0;
	    SortedMap<Integer, StudyVariable> variableMap = mzTabFile.getMetadata().getStudyVariableMap();
	    if (variableMap.size() > 0) {
		UserParameter<?, ?> Parameter = new StringParameter("Parameter", null);
		 MZmineCore.getProjectManager().getCurrentProject().addParameter(Parameter);
	    
		 for (int i=1; i<variableMap.size()+1; i++) {
		     fileCounter = 0;
		     for (RawDataFile rawData : MZmineCore.getProjectManager().getCurrentProject().getDataFiles()) {
			 SortedMap<Integer, Assay> assayMap = variableMap.get(i).getAssayMap();
			 fileCounter++;

			 for (int x=1; x<rawFileCounter+1; x++) {
			     if (assayMap.get(x) != null) {
				 if (assayMap.get(x).toString().contains("ms_run["+fileCounter+"]")) {
				 	MZmineCore.getProjectManager().getCurrentProject().
				 	setParameterValue(Parameter, rawData, variableMap.get(i).getDescription());
			     	}
			     }
			 }
		     }
    
		 }

	    }

	    // Create Peak list
	    fileCounter = 0;
	    for (RawDataFile rawData : project.getDataFiles()) {
		for(Entry<Integer, MsRun> entry : msrun.entrySet()) {
		    File f = new File(entry.getValue().getLocation().getPath());
		    if(rawData.toString().equals(f.getName())) {
		        rawDataFiles[fileCounter] = rawData;
		        fileCounter++;
		    }
		}
	    }
	    PeakList newPeakList = new SimplePeakList(file.getName().replace(".mzTab", ""), rawDataFiles);

	    // Loop through SML data
	    String formula, description, database, url = "";
	    double mzExp=0, abundance=0, peak_mz=0, peak_rt=0, peak_height=0, rtValue=0;
	    int charge=0;
	    int rowCounter = 0;

	    SortedMap<Integer, Assay> assayMap = mzTabFile.getMetadata().getAssayMap();
	    Collection<SmallMolecule> smallMolecules = mzTabFile.getSmallMolecules();
	    for (SmallMolecule smallMolecule : smallMolecules) {
		rowCounter++;
		formula 	= smallMolecule.getChemicalFormula();
		//smile 	= smallMolecule.getSmiles();
		//inchiKey 	= smallMolecule.getInchiKey();
		description	= smallMolecule.getDescription();
		//species 	= smallMolecule.getSpecies();
		database 	= smallMolecule.getDatabase();
		//dbVersion	= smallMolecule.getDatabaseVersion();
		//reliability	= smallMolecule.getReliability();
		
		if (smallMolecule.getURI() != null)  { url = smallMolecule.getURI().toString(); }

		String identifier 			= smallMolecule.getIdentifier().toString();
		SplitList<Double> rt 			= smallMolecule.getRetentionTime();
		//SplitList<Modification> modifications = smallMolecule.getModifications();

		if (smallMolecule.getExpMassToCharge() != null) { mzExp = smallMolecule.getExpMassToCharge(); }
		if (smallMolecule.getCharge() != null) { charge = smallMolecule.getCharge(); }

		// Calculate average RT if multiple values are available
		if (rt.size() > 1) {
		    Object[] rtArray = rt.toArray();
		    double rtTotal = 0; 
		    for(int i=0; i<rt.size(); i++){
			rtTotal = rtTotal + Double.parseDouble(rtArray[i].toString());
		    }
		    rtValue = rtTotal/rt.size();
		}
		else {
		    if (rt != null && !rt.toString().equals("")) { rtValue = Double.parseDouble(rt.toString()); }
		}

		if (url.equals("null")) { url = null; }
		if (identifier.equals("null")) { identifier = null; }
		if (description == null && identifier != null) { description = identifier;}

		// Add shared information to row
		SimplePeakListRow newRow = new SimplePeakListRow(rowCounter);
		newRow.setAverageMZ(mzExp);
		newRow.setAverageRT(rtValue);
		if (description != null) {
		    SimplePeakIdentity newIdentity = new SimplePeakIdentity(description,
			formula, database, identifier, url);
		    newRow.addPeakIdentity(newIdentity, false);
		}

		// Add raw data file entries to row
		fileCounter = 0;
		for (RawDataFile rawData : project.getDataFiles()) {
		    if (isCanceled()) { return; }
		    if (fileCounter+1 <= rawFileCounter) {
		        abundance=0; peak_mz=0; peak_rt=0; peak_height=0;
		        fileCounter ++;
    	    	        if (smallMolecule.getAbundanceColumnValue(assayMap.get(fileCounter)) != null) {
    	    		    abundance = smallMolecule.getAbundanceColumnValue(assayMap.get(fileCounter));
    	    	        }

    	    	        if (smallMolecule.getOptionColumnValue(assayMap.get(fileCounter), "peak_mz") != null) {
    	    	  	    peak_mz = Double.parseDouble(smallMolecule.getOptionColumnValue(assayMap.get(fileCounter), "peak_mz"));
    	    	        }
    	    	        else { peak_mz = mzExp; }

    	    	        if (smallMolecule.getOptionColumnValue(assayMap.get(fileCounter), "peak_rt") != null) {
    	    		    peak_rt = Double.parseDouble(smallMolecule.getOptionColumnValue(assayMap.get(fileCounter), "peak_rt"));
    	    	        }
    	    	        else { peak_rt = rtValue; }

    	    	        if (smallMolecule.getOptionColumnValue(assayMap.get(fileCounter), "peak_height") != null) {
    	    		    peak_height = Double.parseDouble(smallMolecule.getOptionColumnValue(assayMap.get(fileCounter), "peak_height"));
    	    	        }
    	    	        else { peak_height = 0.0; }

    	    	        int scanNumbers[] = {};
    	    	        DataPoint finalDataPoint[] = new DataPoint[1];
    	    	        finalDataPoint[0] = new SimpleDataPoint(peak_mz, peak_height);
    	    	        int representativeScan = 0;
    	    	        int fragmentScan = 0;
    	    	        Range<Double> finalRTRange = Range.singleton(peak_rt);
    	    	        Range<Double> finalMZRange = Range.singleton(peak_mz);
    	    	        Range<Double> finalIntensityRange = Range.singleton(peak_height);
    	    	        FeatureStatus status = FeatureStatus.MANUAL;

    		        Feature peak = new SimpleFeature(rawData, peak_mz, peak_rt, peak_height, abundance,
    			    scanNumbers, finalDataPoint, status, representativeScan,
    			    fragmentScan, finalRTRange, finalMZRange, finalIntensityRange);

    		        if (abundance > 0) {
    		            newRow.addPeak(rawData, peak);
    		        }
    		        
    		    }

		}

		// Add row to peak list
		newPeakList.addRow(newRow);

	    }

	    MZmineCore.getProjectManager().getCurrentProject().addPeakList(newPeakList);
	    currentStage=10;

	} catch (Exception e) {
	    e.printStackTrace();
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage("Could not import data from " + file + ": " + e.getMessage());
	    return;
	}

	if (getStatus() == TaskStatus.PROCESSING)
	    setStatus(TaskStatus.FINISHED);

    }

}
