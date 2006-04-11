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
import java.util.Hashtable;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.methods.rawdata.FilterParameters;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;


/**
 *
 */
public class ControllerForClientsImpl extends java.rmi.server.UnicastRemoteObject implements ControllerForClients {

	private ControllerServer controllerMain;


	/**
	 * Contructor
	 * @param cm	Main class of Controller
	 */
	public ControllerForClientsImpl(ControllerServer cm) throws java.rmi.RemoteException {
		super();
		controllerMain = cm;
	}

	/**
	 * Client calls this method when it starts conenction with the controller.
	 * Controller gives a ClientID that must be included in all further communication
	 * @param	Client's IP address
	 * @return	ClientID given by the controller
	 */
	public int connectToController(InetAddress clientIP, String clientPort) throws java.rmi.RemoteException {
		return controllerMain.addClient(clientIP, clientPort);
	}

	public int connectToController(ClientForCluster client) {
		return controllerMain.addClient(client);
	}

	public void disconnectFromController(int clientID) throws java.rmi.RemoteException {
		controllerMain.removeClient(clientID);
	}


	/**
	 * Add task method used for opening a set of raw data files
	 * @param clientID	client's ID
	 * @param taskType	Task type (not really necessary in the current design)
	 * @param taskItems	Array of File objects to be opened
	 * @return taskID of this task
	 */
	public int addTask(int clientID, int taskType, File[] taskItems) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_OPENRAWDATAFILES) {
			Logger.put("CONTROLLER: Received a task to open " + taskItems.length + " raw data files.");
			return controllerMain.initiateTask(clientID, taskType, taskItems);
		}
		return -1;
	}


	/**
	 * Add task method for closing a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @param	saveChanges	True/false flag telling whether modified working copies should overwrite to original raw data files
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int[] taskItems, boolean saveChanges) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_CLOSERAWDATAFILES) {
			Logger.put("CONTROLLER: Received a task to close " + taskItems.length + " raw data files.");
			return controllerMain.initiateTask(clientID, taskType, taskItems, saveChanges);
		}
		return -1;
	}


	/**
	 * Add task method for refreshing visualizers for a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, RawDataVisualizerRefreshRequest[] taskItems) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_REFRESHVISUALIZERS) {
			Logger.put("CONTROLLER: Received a task to refresh visualizers of " + taskItems.length + " raw data files.");
			return controllerMain.initiateTask(clientID, taskType, taskItems);
		}
		return -1;
	}


	/**
	 * Add task method for filtering a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @param	parameters	Parameters for raw data filtering method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int[] taskItems, FilterParameters parameters) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_FILTERRAWDATAFILES) {
			Logger.put("CONTROLLER: Received a task to filter " + taskItems.length + " raw data files.");
			return controllerMain.initiateTask(clientID, taskType, taskItems, parameters);
		}
		return -1;
	}


	/**
	 * Add task method for finding peaks in a set of raw data files
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Array of raw data IDs
	 * @param	parameters	Parameters for peak picking method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int[] taskItems, PeakPickerParameters parameters) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_FINDPEAKS) {
			Logger.put("CONTROLLER: Received a task to find peaks in " + taskItems.length + " raw data files.");
			return controllerMain.initiateTask(clientID, taskType, taskItems, parameters);
		}
		return -1;
	}

	/**
	 * Add task method for aligning peak lists
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Hashtable where key is raw data ID and value is a vector of peaks for the raw data
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, Hashtable<Integer,PeakList> taskItems, PeakListAlignerParameters parameters) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_ALIGNMENT) {
			Logger.put("CONTROLLER: Received a task to align " + taskItems.size() + " peak lists.");
			return controllerMain.initiateTask(clientID, taskType, taskItems, parameters);
		}
		return -1;

	}

	/**
	 * Add task method for filling gaps in alignment result
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Hashtable where key is raw data ID and value is a vector of peaks for the raw data
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, AlignmentResult alignmentResult, GapFillerParameters parameters) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_GAPFILL) {
			Logger.put("CONTROLLER: Received a task to fill gaps in alignment ID " + alignmentResult.getAlignmentResultID() + ".");
			return controllerMain.initiateTask(clientID, taskType, alignmentResult, parameters);
		}
		return -1;

	}


	/**
	 * Add task method for calculating total raw signal for a set of raw data files (required in normalization by total raw signal)
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	alignmentResult	Total raw signal should be calculated for every raw data file involved in this alignment
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, int rawDataIDs[]) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_CALCTOTALRAWSIGNAL) {
			Logger.put("CONTROLLER: Received a task to calc total raw signal in " + rawDataIDs.length + " raw data files.");
			return controllerMain.initiateTask(clientID, taskType, rawDataIDs);
		}
		return -1;

	}


	/**
	 * Add task method for aligning peak lists
	 * @param	clientID	Client's ID
	 * @param	taskType	Task type
	 * @param	taskItems	Hashtable where key is raw data ID and value is a vector of peaks for the raw data
	 * @param	parameters	Parameters for aligner method
	 * @return	taskID of this task
	 */
	public int addTask(int clientID, int taskType, Hashtable<Integer,PeakList> taskItems, PeakListProcessorParameters parameters) throws java.rmi.RemoteException {

		if (taskType == Task.TASKTYPE_PROCESSPEAKLISTS) {
			Logger.put("CONTROLLER: Received a task to process " + taskItems.size() + " peak lists.");
			return controllerMain.initiateTask(clientID, taskType, taskItems, parameters);
		}
		return -1;

	}




	/**
	 * This methods return a list of itemIDs used in one task.
	 */
	public int[] getItemIDs(int taskID) throws java.rmi.RemoteException {
		return null;
	}




}