/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.obsoletedistributionframework;
import java.io.File;
import java.net.InetAddress;
import java.rmi.Naming;
import java.util.Hashtable;

import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.filtering.FilterParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;


/**
 *
 */
public class NodeImpl extends java.rmi.server.UnicastRemoteObject implements Node {

	private NodeServer nodeMain;				// Main node class


	/**
	 * Constructor
	 */
	public NodeImpl(NodeServer _nodeMain) throws java.rmi.RemoteException {
		super();
		nodeMain = _nodeMain;
	}


	/**
	 * Controller calls this node when it is starting up.
	 * This methods creates connection back to controller, which is used for passing results.
	 */
	public void connectToNode(InetAddress controllerIP, String controllerPort) throws java.rmi.RemoteException {

		ControllerForNodes myController = null;

		Logger.put("NODE: Controller serving at " + controllerIP.getHostAddress() + ":" + controllerPort + " has connected to me.");

		// Connect back to controller
		Logger.put("NODE: Connecting back to controller...");
		try {
			myController = (ControllerForNodes)Naming.lookup("rmi://" + controllerIP.getHostAddress() + ":" + controllerPort + "/ControllerForNodesService");
			Logger.put("NODE: Succesfully connected back to controller.");
		} catch (Exception e) {
			Logger.putFatal("NODE: FATAL ERROR - Failed to connect back to controller.");
			Logger.putFatal(e.toString());
			Logger.putFatal("NODE: Unable to continue.");
			System.exit(0);
		}

		nodeMain.setController(myController);

	}

	/**
	 * Controller calls this node when it is starting up in single computer mode.
	 * This methods creates connection back to controller, which is used for passing results.
	 */
	public void connectToNode(ControllerServer controllerServer) {
		ControllerForNodes myController = controllerServer.getServicesForNodes();
		nodeMain.setController(myController);
	}

	/* Methods for taking job orders from controller
	   ---------------------------------------------  */

	/**
	 * Opens a raw data file and preloads some data out of it.
	 * @param	taskID 	TaskID of the task where this job is part of.
	 * @param	jobID	jobID (rawDataID) assigned to this raw data file by controller
	 * @param	originalRawDataFile	File object pointing to the original raw data file
	 */
	public void addJob(int taskID, int jobID, File originalRawDataFile) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, originalRawDataFile);

	}


	/**
	 * Closes a raw data file and and writes over the original version with working copy if requested
	 * @param	taskID 	TaskID of the task where this job is part of.
	 * @param	jobID	jobID (rawDataID) of the raw data file to be closed
	 * @param	saveChanges	If working copy has been altered and this is true, overwrite original version with working copy
	 */
	public void addJob(int taskID, int jobID, boolean saveChanges) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, saveChanges);

	}


	/**
	 * Calculates data for visualizers
	 * @param	taskID 	TaskID of the task where this job is part of.
	 * @param	jobID	jobID (rawDataID) of the raw data file to be closed
	 * @param	refreshRequest	Describes what information is needed by each visualizer of this raw data file
	 */
	public void addJob(int taskID, int jobID, RawDataVisualizerRefreshRequest refreshRequest) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, refreshRequest);
	}


	/**
	 * Filters raw data file
	 * @param	taskID 	TaskID of the task where this job is part of.
	 * @param	jobID	jobID (rawDataID) of the raw data file to be filtered
	 * @param	filterParameters	Parameters for the filter
	 */
	public void addJob(int taskID, int jobID, FilterParameters filterParameters) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, filterParameters);
	}

	/**
	 * Finds peaks in raw data file
	 * @param	taskID 	TaskID of the task where this job is part of.
	 * @param	jobID	jobID (rawDataID) of the raw data file to be peak picked
	 * @param	parameters	Parameters for the peak picker
	 */
	public void addJob(int taskID, int jobID, PeakPickerParameters parameters) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, parameters);

	}

	/**
	 * Aligns multiple peak lists and creates an alignment result
	 * @param	taskID 	TaskID of the task where this job is part of.
	 * @param	jobID	jobID (counting number of alignment tasks so far) of the alignment job
	 * @param	peakLists	Hashtable of with raw data IDs as keys and peaklists as values
	 * @param	parameters	Parameter values for alignment method
	 */
	public void addJob(int taskID, int jobID, Hashtable<Integer, PeakList> peakLists, PeakListAlignerParameters parameters) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, peakLists, parameters);

	}


	/**
	 * Fills in gaps in an alignment result
	 * @param	taskID			TaskID of the task where this job is part of.
	 * @param	jobID			jobID is rawData ID for the raw data file whose column should be filled
	 * @param	alignmentResult	Original alignment result
	 * @param	parameters		Parameter values for gap filler method
	 */
	public void addJob(int taskID, int jobID, Hashtable<Integer, double[]> gapsToFill, GapFillerParameters parameters) throws java.rmi.RemoteException {

		nodeMain.addJob(taskID, jobID, gapsToFill, parameters);

	}

	/**
	 * Calc total raw signal job
	 * @param	taskID			TaskID of the task where this job is part of.
	 * @param	jobID			ID of the Raw data file whose total signal should be summed
	 */
	public void addJob(int taskID, int jobID) throws java.rmi.RemoteException {
		nodeMain.addJob(taskID, jobID);
	}

	/**
	 * Processes a peak list
	 * @param	taskID		TaskID of the task where this job is part of.
	 * @param	jobID		ID of the Raw data file whose peak list is going to be processed
	 * @param	peakList	Peak list to be processed
	 * @param	parameters	Parameters for peak list processor method
	 */
	public void addJob(int taskID, int jobID, PeakList peakList, PeakListProcessorParameters parameters) throws java.rmi.RemoteException {
		nodeMain.addJob(taskID, jobID, peakList, parameters);
	}



}
