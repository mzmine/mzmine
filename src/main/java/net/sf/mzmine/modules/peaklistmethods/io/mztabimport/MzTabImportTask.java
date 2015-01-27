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
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataFileType;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataFileTypeDetector;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.AgilentCsvReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NativeFileReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.parameters.ParameterSet;
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
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;

import com.google.common.collect.Range;

class MzTabImportTask extends AbstractTask {

    // parameter values
    private File file;
    private boolean importrawfiles;

    MzTabImportTask(ParameterSet parameters) {
	this.file = parameters.getParameter(MzTabImportParameters.file).getValue();
	this.importrawfiles = parameters.getParameter(MzTabImportParameters.importrawfiles).getValue();
    }

    public double getFinishedPercentage() {
	/**
	 * TODO: WRITE PERCENTRAGE HANDLER!
	 **/
	return 0.5d;
    }

    public String getTaskDescription() {
	return "Loading data from " + file;
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

	    // Import raw data files
	    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

	    SortedMap<Integer, MsRun> msrun = mzTabFile.getMetadata().getMsRunMap();
	    Task newTasks[] = new Task[1];
	    RawDataFileWriter newMZmineFile;

    	    for(Entry<Integer, MsRun> entry : msrun.entrySet()) {
    	        File testFile = new File(entry.getValue().getLocation().getPath());
    	        File f = new File(entry.getValue().getLocation().getPath());
    	        newMZmineFile = MZmineCore.createNewFile(f.getName());
    	        if (importrawfiles) {
    	    	    if(testFile.exists() && !testFile.isDirectory()) {
        		RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(f);
        		switch (fileType) {
        		    case MZDATA:
        			newTasks[0] = new MzDataReadTask(project, f, newMZmineFile);
        			break;
        		    case MZML:
        			newTasks[0] = new MzMLReadTask(project, f, newMZmineFile);
        			break;
        		    case MZXML:
        			newTasks[0] = new MzXMLReadTask(project, f, newMZmineFile);
        			break;
        		    case NETCDF:
        			newTasks[0] = new NetCDFReadTask(project, f, newMZmineFile);
        			break;
        		    case AGILENT_CSV:
        			newTasks[0] = new AgilentCsvReadTask(project, f, newMZmineFile);
        			break;
        		    case THERMO_RAW:
        		    case WATERS_RAW:
        			newTasks[0] = new NativeFileReadTask(project, f, fileType, newMZmineFile);
        			break;
        		}
        		// Process task
        		MZmineCore.getTaskController().addTasks(newTasks);

    	    	    }

    	    	    else {
    	    		// Add dummy raw file
            		RawDataFile newDataFile2 = newMZmineFile.finishWriting();
            		project.addFile(newDataFile2);
    	    	    }
    	    	}
    	    	else {
    	    	    // Add dummy raw file
    	    	    RawDataFile newDataFile2 = newMZmineFile.finishWriting();
    	    	    project.addFile(newDataFile2);
    	    	}

    	    }

	    // Wait until all raw data file imports have completed
	    TaskQueue taskQueue = MZmineCore.getTaskController().getTaskQueue();
	    while (taskQueue.getNumOfWaitingTasks() > 1) { Thread.sleep(1000); }

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

	    // Add sample parameters
	    /**
	     * TODO: Add sample parameters if available in mzTab file
	     */
	    int rawFileCounter = 0;

	    // Create Peak list
	    RawDataFile[] RawDataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
	    PeakList newPeakList = new SimplePeakList(file.getName().replace(".mzTab", ""), RawDataFiles);

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
		rawFileCounter = 0;
		for (RawDataFile rawData : MZmineCore.getProjectManager().getCurrentProject().getDataFiles()) {
		    abundance=0; peak_mz=0; peak_rt=0; peak_height=0;
		    rawFileCounter ++;
    	    	    if (smallMolecule.getAbundanceColumnValue(assayMap.get(rawFileCounter)) != null) {
    	    		abundance = smallMolecule.getAbundanceColumnValue(assayMap.get(rawFileCounter));
    	    	    }

    	    	    if (smallMolecule.getOptionColumnValue(assayMap.get(rawFileCounter), "peak_mz") != null) {
    	    		peak_mz = Double.parseDouble(smallMolecule.getOptionColumnValue(assayMap.get(rawFileCounter), "peak_mz"));
    	    	    }
    	    	    else { peak_mz = mzExp; }

    	    	    if (smallMolecule.getOptionColumnValue(assayMap.get(rawFileCounter), "peak_rt") != null) {
    	    		peak_rt = Double.parseDouble(smallMolecule.getOptionColumnValue(assayMap.get(rawFileCounter), "peak_rt"));
    	    	    }
    	    	    else { peak_rt = rtValue; }

    	    	    if (smallMolecule.getOptionColumnValue(assayMap.get(rawFileCounter), "peak_height") != null) {
    	    		peak_height = Double.parseDouble(smallMolecule.getOptionColumnValue(assayMap.get(rawFileCounter), "peak_height"));
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

    	    	// Add row to peak list
		newPeakList.addRow(newRow);

	    }

	    MZmineCore.getProjectManager().getCurrentProject().addPeakList(newPeakList);

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
