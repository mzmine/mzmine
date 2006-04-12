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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.rmi.Naming;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.filtering.FilterParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;
import edu.csusb.danby.stat.RegressionCalculator;


/**
 *
 */
public class ControllerServer {

	// ----

	private String settingsFilename 				// This is the name of .ini-file storing settings for client
						= "mzminecontroller.ini";

	// ----

	private String myPort;							// These will be loaded from an .ini file in the future
	private Vector<String> nodeHostnames;
	private Vector<String> nodePorts;
	private Vector<Integer> nodePerformanceFactors;

	// ----

	private InetAddress myIP; 						// IP-address of the computer running client
	private String myHostname;						// Hostname of the computer running client

	private Hashtable<Integer, NodeInfo> nodes;		// Table for finding node by ID

	private int clientIDCount = 0;					// Counter used for giving IDs to new clients
	private Hashtable<Integer, Client> clients;		// Table for finding client by ID

	private int taskIDCount = 0;					// Counter used for giving taskIDs for new tasks
	private Hashtable<Integer, Task> tasks;			// Table for accessing task data by taskID


	private int rawDataIDCount = 0;					// Counter used for giving rawDataIDs for new raw data files
	private Hashtable<Integer, NodeInfo> nodeForRawData;// Table for finding which Node is processor for a raw data file. (rawDataID is the key)
	private Hashtable<Integer, String> rawDataNames;	// Table for finding names for raw data files. (rawDataID is the key) (names are needed in feeding wait dialog)

	private int jobIDCounter = 0;					// Counter used for giving Job IDs when task items do not provide suitable, unique IDs themselves.

	private ControllerForNodes servicesForNodes;		// Two servers: one for nodes and another for clients
	private ControllerForClients servicesForClients;

	private WorkerThread workerThread;				// Worker thread for passing jobs to nodes and results to clients

	// ----


	/**
	 * Constructor when running in a cluster
	 */
	public ControllerServer() {

		Logger.put("CONTROLLER: Starting up... ");

		// Determine IP-address of the local host
		try {
			myIP = InetAddress.getLocalHost();
			myHostname = myIP.getHostName();
		} catch (Exception e) {
			Logger.putFatal("CONTROLLER: FATAL ERROR - Failed to determine IP address and hostname of the computer.");
			Logger.putFatal(e.toString());
			Logger.putFatal("Unable continue.");
			System.exit(0);
		}

		// Initialize vectors for storing node IPs and ports (these are loaded from .ini file in the next step)
		nodeHostnames = new Vector<String>();
		nodePorts = new Vector<String>();
		nodePerformanceFactors = new Vector<Integer>();


		// Load settings from .INI file
		if (loadSettings(settingsFilename) == false ) {
			Logger.putFatal("CONTROLLER: FATAL ERROR - Failed to read necessary settings from " + settingsFilename);
			Logger.putFatal("Unable continue.");
			System.exit(0);
		}

		Logger.put("CONTROLLER: Service running at IP-address:port " + myIP.getHostAddress() + ":" + myPort);

		// Initialize hashtable for client connections
		clients = new Hashtable<Integer, Client>();

		// Initialize hashtable for tasks
		tasks = new Hashtable<Integer, Task>();

		// Initialize queue for tasks that need dispatching to nodes or clients
		/*
		if (taskQueue == null) {
			Logger.put("CONTROLLER: Create a new task queue.");
			taskQueue = new Vector<Task>();
		}
		*/

		// Initialize table for storing information about which raw data file is being processed by what node
		nodeForRawData = new Hashtable<Integer, NodeInfo>();

		// Initialize table for storing information about raw data file names
		rawDataNames = new Hashtable<Integer, String>();

		// Initialize worker thread for passing jobs to nodes and results to clients
		workerThread = new WorkerThread();
		workerThread.start();

		// Start server for nodes
		Logger.put("CONTROLLER: Starting server for nodes...");
		try {
			servicesForNodes = new ControllerForNodesImpl(this);
			Naming.rebind("rmi://" + myIP.getHostAddress() + ":" + myPort + "/ControllerForNodesService", servicesForNodes);
			Logger.put("CONTROLLER: Started server for nodes.");
		} catch (Exception e) {
			Logger.putFatal("CONTROLLER: FAILED to start server for nodes.");
			Logger.putFatal(e.toString());
			Logger.putFatal("Unable to continue.");
			System.exit(0);
		}

		// Start server for clients
		Logger.put("CONTROLLER: Starting server for clients...");
		try {
			servicesForClients = new ControllerForClientsImpl(this);
			Naming.rebind("rmi://" + myIP.getHostAddress() + ":" + myPort + "/ControllerForClientsService", servicesForClients);
			Logger.put("CONTROLLER: Started server for clients.");
		} catch (Exception e) {
			Logger.putFatal("CONTROLLER: FAILED to start server for clients.");
			Logger.putFatal(e.toString());
			Logger.putFatal("CONTROLLER: Unable to continue.");
			System.exit(0);
		}

		// Connect to nodes of the cluster
		nodes = new Hashtable<Integer, NodeInfo>();
		for (int nodeID=0; nodeID<nodeHostnames.size(); nodeID++) {
			Logger.put("CONTROLLER: Connecting to node #" + (nodeID+1) + " at " + nodeHostnames.get(nodeID) + " - ");
			NodeInfo ni = new NodeInfo();
			try {
				// Lookup for the node and connect to it
				Node tmpNode = (Node)Naming.lookup("rmi://" + nodeHostnames.get(nodeID) + ":" + nodePorts.get(nodeID) + "/NodeService");
				tmpNode.connectToNode(myIP, myPort);

				// Store all information about node in NodeInfo object and put it into hashtable
				ni.theNode = tmpNode;
				ni.name = nodeHostnames.get(nodeID);
				ni.performanceFactor = nodePerformanceFactors.get(nodeID);
				ni.assignedRuns = 0;
				nodes.put(new Integer(nodeID), ni);

				Logger.put("CONTROLLER: Connect to node done.");
			} catch (Exception e) {
				Logger.putFatal("CONTROLLER: ERROR - Connect to node failed.");
				Logger.putFatal(e.toString());
			}
		}
		if (nodes.size()==0) {
			Logger.putFatal("CONTROLLER: Could not connect to any nodes.");
			Logger.putFatal("CONTROLLER: Unable to continue.");
			System.exit(0);
		}
		// Original .ini-file information about nodes is no longer needed
		nodeHostnames = null;
		nodePorts = null;
		nodePerformanceFactors = null;

		// Start-up done.
		Logger.put("CONTROLLER: started ok.");

	}




	/**
	 * Constructor when running in a single computer
	 */
	public ControllerServer(NodeServer[] myNodes) {

		Logger.put("CONTROLLER: Starting up... ");


		// Initialize dummy vectors for storing information coming from .ini file
		nodeHostnames = new Vector<String>();
		nodePorts = new Vector<String>();
		nodePerformanceFactors = new Vector<Integer>();

		// Load settings from .INI file
		if (loadSettings(settingsFilename) == false ) {
			Logger.putFatal("CONTROLLER: FATAL ERROR - Failed to read necessary settings from " + settingsFilename);
			Logger.putFatal("Unable continue.");
			System.exit(0);
		}

		// Initialize hashtable for client connections
		clients = new Hashtable<Integer, Client>();

		// Initialize hashtable for tasks
		tasks = new Hashtable<Integer, Task>();

		// Initialize table for storing information about which raw data file is being processed by what node
		nodeForRawData = new Hashtable<Integer, NodeInfo>();

		// Initialize table for storing information about raw data file names
		rawDataNames = new Hashtable<Integer, String>();

		// Initialize worker thread for passing jobs to nodes and results to clients
		workerThread = new WorkerThread();
		workerThread.start();

		// Start servers for nodes and client
		try {
			servicesForNodes = new ControllerForNodesImpl(this);
			servicesForClients = new ControllerForClientsImpl(this);
		} catch (Exception e) {}

		// Connect to nodes
		nodes = new Hashtable<Integer, NodeInfo>();

		Integer nodeID = 1;
		for (NodeServer nodeServer : myNodes) {
			NodeInfo ni = new NodeInfo();
			Node tmpNode = nodeServer.getServicesForController();

			try {
				tmpNode.connectToNode(this);
			} catch (Exception e) {}

			// Store all information about node in NodeInfo object and put it into hashtable
			ni.theNode = tmpNode;
			ni.name = "localhost (node #" + nodeID + ")";
			ni.performanceFactor = 100;
			ni.assignedRuns = 0;
			nodes.put(new Integer(nodeID), ni);

			nodeID++;
		}

		if (nodes.size()==0) {
			Logger.putFatal("CONTROLLER: Could not connect to any nodes.");
			Logger.putFatal("CONTROLLER: Unable to continue.");
			System.exit(0);
		}

		// Original .ini-file information about nodes is no longer needed
		nodeHostnames = null;
		nodePorts = null;
		nodePerformanceFactors = null;


		// Start-up done.
		Logger.put("CONTROLLER: started ok.");

	}


	/**
	 * When running without Java RMI, this method is used to get servicesForNodes-object
	 */
	public ControllerForNodes getServicesForNodes() { return servicesForNodes; }

	/**
	 * When running without Java RMI, this method is used to get servicesForClients-object
	 */
	public ControllerForClients getServicesForClients() { return servicesForClients; }


	/**
	 *
	 */
	public static void main(String argz[]) {

		if (argz.length>0) {
			if ( (argz[0].compareToIgnoreCase(new String("quiet")))==0 ) {
				Logger.disableOutput();
			}
		} else {
			Logger.setOutputOnScreen();
		}

		new ControllerServer();
	}


	/**
	 * This method adds new client that has connected the controller and gives it a clientID that
	 * must be used in all further communication between client and controller
	 */
	public synchronized int addClient(InetAddress clientIP, String clientPort) {

		Client tmpClient;

		// Try connecting to the client
		Logger.put("CONTROLLER: Trying to connect back to client.");
		try {
			tmpClient = (Client)Naming.lookup("rmi://" + clientIP.getHostAddress() + ":" + clientPort + "/ClientService");
			Logger.put("CONTROLLER: Connect back to client done.");
		} catch (Exception e) {
			Logger.putFatal("CONTROLLER: ERROR - Failed to connect back to client.");
			Logger.putFatal(e.toString());
			return -1;
		}

		// Connection successful, store remote client service
		clientIDCount++;
		clients.put(new Integer(clientIDCount), tmpClient);
		return clientIDCount;

	}

	/**
	 * This method adds new client that has connected the controller and gives it a clientID that
	 * must be used in all further communication between client and controller
	 */
	public synchronized int addClient(ClientForCluster client) {

		Client tmpClient = client.getServicesForController();

		// Connection successful, store remote client service
		clientIDCount++;
		clients.put(new Integer(clientIDCount), tmpClient);
		return clientIDCount;

	}

	/**
	 * This method removes a disconnecting client from controller
	 */
	public synchronized void removeClient(int clientID) {
		// Tell nodes that all data regarding this client can be trashed
		// (NOT IMPLEMENTED)

		// Remove client from client table
		Logger.put("CONTROLLER: Client " + clientID + " is disconnecting from the controller...");
		clients.remove(new Integer(clientID));
		Logger.put("CONTROLLER: Client " + clientID + " disconnected from the controller.");

	}




	/**
	 * Initiate task method for opening a set of raw data files.
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 * @param	rawDataFiles 	File objects pointing to original raw data files to be opened
	 * @return	TaskID of the new task
	 */
	public synchronized int initiateTask(int clientID, int taskType, File[] rawDataFiles) {

		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to open raw data files (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();

		t.client = clients.get(new Integer(clientID));

		t.taskID = taskIDCount;
		t.taskType = taskType;
		t.items = rawDataFiles;
		t.numOfJobs = t.items.length;

		// Assign new rawDataIDs for each raw data file and save their filenames
		t.itemIDs = new int[t.items.length];
		for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
			rawDataIDCount++;
			t.itemIDs[itemInd] = rawDataIDCount;
			rawDataNames.put(new Integer(rawDataIDCount), rawDataFiles[itemInd].getName());
		}

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (open raw data files task)");
		workerThread.addTask(t);

		return taskIDCount;

	}


	/**
	 * Initiate task method for closing a set of raw data files.
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 * @param	rawDataIDs	IDs of raw data files to be closed
	 * @param	taskType	Type of the task (must be TASKTYPE_CLOSERAWDATAFILES)
	 * @param	rawDataIDs	Array of raw data IDs to be closed
	 * @param	saveChanges	If true then overwrite original raw data files with working copies if they have been modified
	 * @return	TaskID of the new task
	 */
	public synchronized int initiateTask(int clientID, int taskType, int[] rawDataIDs, boolean saveChanges) {

		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to close raw data files (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();

		t.client = clients.get(new Integer(clientID));

		t.taskID = taskIDCount;
		t.taskType = taskType;

		t.items = null;
		t.itemIDs = new int[rawDataIDs.length];
		t.numOfJobs = t.itemIDs.length;
		for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
			t.itemIDs[itemInd] = rawDataIDs[itemInd];
		}

		t.taskParameters.put(Task.TASKPARAMETER_SAVECHANGES, new Boolean(saveChanges));

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (close raw data files task)");
		workerThread.addTask(t);


		return taskIDCount;

	}


	/**
	 * Initiate task method for refreshing visualizers for a set of raw data files
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 * @param	rawDataIDs	IDs of raw data files to be closed
	 * @param	taskType	Type of the task (must be TASKTYPE_REFRESHVISUALIZERS)
	 * @param	refreshRequests	Array of refresh requests (one for each raw data file)
	 * @param	saveChanges	If true then overwrite original raw data files with working copies if they have been modified
	 * @return	TaskID of the new task
	 */
	public synchronized int initiateTask(int clientID, int taskType, RawDataVisualizerRefreshRequest[] refreshRequests) {

		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to refresh visualizers (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Put raw data IDs to item IDs
		t.itemIDs = new int[refreshRequests.length];
		t.numOfJobs = t.itemIDs.length;
		for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
			t.itemIDs[itemInd] = refreshRequests[itemInd].rawDataID;
		}

		// Put refresh requests to items
		t.items = refreshRequests;

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (refresh visualizers)");
		workerThread.addTask(t);

		return taskIDCount;

	}


	/**
	 * Initiate task method for filtering a set of raw data files.
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 * @param	rawDataFiles 	File objects pointing to original raw data files to be opened
	 * @return	TaskID of the new task
	 */
	public synchronized int initiateTask(int clientID, int taskType, int[] rawDataIDs, FilterParameters filterParameters) {
		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to filter raw data files (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Put raw data IDs to item IDs
		t.items = null;
		t.itemIDs = new int[rawDataIDs.length];
		t.numOfJobs = t.itemIDs.length;
		for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
			t.itemIDs[itemInd] = rawDataIDs[itemInd];
		}

		// Store filter parameters as a task parameter
		t.taskParameters.put(Task.TASKPARAMETER_FILTERPARAMETERS, filterParameters);

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (filter)");
		workerThread.addTask(t);


		return taskIDCount;
	}


	/**
	 * Initiate task method for finding peaks in a set of raw data files.
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 */
	public synchronized int initiateTask(int clientID, int taskType, int[] rawDataIDs, PeakPickerParameters parameters) {
		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to search peaks (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Put raw data IDs to item IDs
		t.items = null;
		t.itemIDs = new int[rawDataIDs.length];
		t.numOfJobs = t.itemIDs.length;
		for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
			t.itemIDs[itemInd] = rawDataIDs[itemInd];
		}

		// Store filter parameters as a task parameter
		t.taskParameters.put(Task.TASKPARAMETER_PEAKPICKERPARAMETERS, parameters);

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (find peaks)");
		workerThread.addTask(t);

		return taskIDCount;
	}


	/**
	 * Initiate task method for aligning a set of peak lists
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 */
	public synchronized int initiateTask(int clientID, int taskType, Hashtable<Integer, PeakList> peakLists, PeakListAlignerParameters parameters) {
		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to align peak lists (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Put all peak lists to a single item and pickup a new index number for item ID
		t.items = new Object[1];
		t.itemIDs = new int[1];
		jobIDCounter++;
		t.itemIDs[0] = jobIDCounter;
		t.items[0] = peakLists;
		t.numOfJobs = t.itemIDs.length;

		// Store filter parameters as a task parameter
		t.taskParameters.put(Task.TASKPARAMETER_ALIGNMENTPARAMETERS, parameters);

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (alignment)");
		workerThread.addTask(t);

		return taskIDCount;
	}


	/**
	 * Initiate task method for filling gaps in an alignment result
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 */
	public synchronized int initiateTask(int clientID, int taskType, AlignmentResult alignmentResult, GapFillerParameters parameters) {
		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to do gap filling (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Put all peak lists to a single item and pickup a new index number for item ID
		t.items = new Object[1];
		t.itemIDs = new int[1];
		t.items[0] = alignmentResult;
		t.itemIDs[0] = alignmentResult.getAlignmentResultID();
		t.numOfJobs = alignmentResult.getNumOfRawDatas();

		// Store filter parameters as a task parameter
		t.taskParameters.put(Task.TASKPARAMETER_GAPFILLERPARAMETERS, parameters);

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (fill-in gaps)");
		workerThread.addTask(t);

		return taskIDCount;
	}


	/**
	 * Initiate task method for calculating total raw signal for a set of raw data files
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 */
	public synchronized int initiateTask(int clientID, int taskType, int rawDataIDs[]) {
		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to calc total raw signal (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Make each raw Data id a separate item
		t.items = null;
		t.itemIDs = new int[rawDataIDs.length];
		for (int ri=0; ri<rawDataIDs.length; ri++) {
			t.itemIDs[ri] = rawDataIDs[ri];
		}
		t.numOfJobs = t.itemIDs.length;

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (calc total raw signal)");
		workerThread.addTask(t);

		return taskIDCount;
	}


	/**
	 * Initiate task method for processing a set of peak lists
	 * Creates a task object with given data and puts it in queue for
	 * distributing jobs to nodes.
	 */
	public synchronized int initiateTask(int clientID, int taskType, Hashtable<Integer, PeakList> peakLists, PeakListProcessorParameters parameters) {
		taskIDCount++;

		Logger.put("CONTROLLER: Controller initiates a task to process peak lists (taskID=" + taskIDCount +").");

		// Create new task object
		Task t = new Task();
		t.taskID = taskIDCount;
		t.taskType = taskType;

		// Store client for the task
		t.client = clients.get(new Integer(clientID));

		// Put raw data IDs to item IDs and peak lists to items
		t.items = new PeakList[peakLists.size()];
		t.itemIDs = new int[peakLists.size()];
		Enumeration<Integer> rawDataIDEnum = peakLists.keys();
		Enumeration<PeakList> peakListEnum = peakLists.elements();
		int tmpInd = 0;
		while (rawDataIDEnum.hasMoreElements()) {
			t.itemIDs[tmpInd] = rawDataIDEnum.nextElement();
			t.items[tmpInd] = peakListEnum.nextElement();
			tmpInd++;
		}
		t.numOfJobs = t.itemIDs.length;

		// Store filter parameters as a task parameter
		t.taskParameters.put(Task.TASKPARAMETER_PEAKLISTPROCESSORPARAMETERS, parameters);

		// Add task object to task queue for dividing it to jobs and dispatching them to nodes
		t.taskStatus = Task.TASKSTATUS_READYFORSENDINGTONODES;
		tasks.put(new Integer(taskIDCount), t);
		Logger.put("CONTROLLER: Add to task queue (alignment)");
		workerThread.addTask(t);

		return taskIDCount;
	}



/*
	private void wakeupWorker() {
		if ( (workerThread == null) || (!workerThread.isAlive()) ) {
			// Start worker thread if it is not already active
			Logger.put("CONTROLLER: Worker thread was non-existent or dead, starting it up.");
			workerThread = new WorkerThread();
			workerThread.start();
		} else {
			// If worker is in wait, then notify

			Logger.put("CONTROLLER: Notifying worker thread.");
			workerThread.kickMe();
		}

	}
*/


	/**
	 * This method sets the current processing rate of one job
	 */
	public synchronized void updateJobCompletionRate(int taskID, int jobID, int status, double completionRate) {

		Logger.put("CONTROLLER: Updating job completion rate. Task: " + taskID + " Job: " + jobID + " Status: " + status + " Rate: " + completionRate);

		Integer taskIDInteger = new Integer(taskID);
		Integer jobIDInteger = new Integer(jobID);
		Integer statusIDInteger = new Integer(status);
		Double completionRateDouble = new Double(completionRate);

		Task t = tasks.get(taskIDInteger);
		t.jobStatus.put( jobIDInteger, new Integer(status) );
		t.jobCompletionRate.put( jobIDInteger, new Double(completionRate) );

		try {
			t.client.updateJobStatus(taskIDInteger, jobIDInteger, statusIDInteger, completionRateDouble);
		} catch (Exception e) {
			Logger.put("FAILED to update job status on client");
			Logger.put(e.toString());
		}

		jobIDInteger = null;
		taskIDInteger = null;

	}


	/**
	 * This method stores the result or error message for a job.
	 * If all results for a task become available it also queues
	 * the task for returning results to the client.
	 */
	public synchronized void setJobResult(int taskID, int jobID, Object result) {

		Logger.put("CONTROLLER: Setting job result. Task:" + taskID + " Job:" + jobID);


		// Add job result
		Integer taskIDInteger = new Integer(taskID);
		Integer jobIDInteger = new Integer(jobID);

		Task t = tasks.get(taskIDInteger);

		t.jobResult.put( jobIDInteger, result );

		jobIDInteger = null;
		taskIDInteger = null;


		// Check if all jobs have results
		if ( t.jobResult.size() == t.numOfJobs ) {

			// Results are ready, change task status and add it to queue for dispatching results to the client.
			t.taskStatus = Task.TASKSTATUS_PROCESSINGDONE;
			Logger.put("CONTROLLER: Add to task queue (processing done, taskid=" + t.taskID + ")");
			workerThread.addTask(t);

		}


	}




	/**
	 * Loads settings from .ini file
	 */
	private boolean loadSettings(String filename) {
		int varsRead = -4; // - number of necessary variables

		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));

			String nextLine = br.readLine();
			String varStr = null;
			String valStr = null;

			while (nextLine != null) {
				StringTokenizer st = new StringTokenizer(nextLine, " = ");

				if (st.hasMoreTokens()) {

					varStr = st.nextToken();
					if ( st.hasMoreTokens() ) { valStr = st.nextToken(); } else { valStr = new String(""); }

					if (varStr.equals("ControllerPort")) { myPort = valStr; varsRead++; }
					if (varStr.equals("NodeHostname")) { nodeHostnames.add(valStr); varsRead++;	}
					if (varStr.equals("NodePort")) { nodePorts.add(valStr); varsRead++;	}
					if (varStr.equals("NodePerformanceFactor")) { nodePerformanceFactors.add(new Integer(100)); varsRead++; }

				}

				nextLine = br.readLine();

			}

			br.close();

		} catch (Exception e) {
			Logger.put("CONTROLLER: ERROR - Could not open/read settings from file " + filename);
			Logger.put(e.toString());
			return false;
		}

		if (varsRead<0) {
			Logger.put("CONTROLLER: ERROR - Could not find all necessary variables in file " + filename);
			return false;
		} else {
			return true;
		}

	}

	/**
	 * This method searches for the best available node for assigning a new run
	 */
	private NodeInfo getBestAvailableNode() {
		double bestNodePerformanceRatio = Double.MAX_VALUE;
		int bestNodeAssignedRuns = Integer.MAX_VALUE;
		NodeInfo bestNode = nodes.get(0);

		Enumeration<NodeInfo> nodeInfoEnum = nodes.elements();

		while (nodeInfoEnum.hasMoreElements()) {
			NodeInfo ni = nodeInfoEnum.nextElement();

			if (ni.getPerformanceRatio()<bestNodePerformanceRatio) {
				bestNodePerformanceRatio = ni.getPerformanceRatio();
				bestNodeAssignedRuns = ni.assignedRuns;
				bestNode = ni;
			} else {
				if (ni.getPerformanceRatio()==bestNodePerformanceRatio) {
					if (ni.assignedRuns<bestNodeAssignedRuns) {
						bestNodePerformanceRatio = ni.getPerformanceRatio();
						bestNodeAssignedRuns = ni.assignedRuns;
						bestNode = ni;
					}
				}
			}
		}


		return bestNode;
	}


	/**
	 * Class for collecting node and information about it to same packet (for storing this in a single hashtable)
	 */
	private class NodeInfo {
		public Node theNode;

		public String name;
		public int performanceFactor;
		public int assignedRuns;

		public double getPerformanceRatio() {
			return (double)assignedRuns/(double)performanceFactor;
		}
	}



	/**
	 * Worker thread is used for dispatching jobs to nodes and results to clients
	 */
	private class WorkerThread extends Thread {

		private Vector<Task> taskQueue;	// Task queue for those tasks that require dispatching to nodes or the client.

		public WorkerThread() {
			taskQueue = new Vector<Task>();
		}


		/**
		 * Adds a new task to worker
		 */
		public void addTask(Task t) {
			synchronized(this) {
				taskQueue.add(t);
				try {
					notifyAll();
				} catch (Exception e) {
					Logger.putFatal("CONTROLLER WORKER THREAD: Worker failed to notifyAll()");
					Logger.putFatal(e.toString());
				}
			}
		}


		/**
		 * Run method of the thread
		 */
		//public synchronized void run() {
		public void run() {

			Logger.put("CONTROLLER WORKER THREAD: Thread started!");

			Logger.put("CONTROLLER WORKER THREAD: Size of the queue is " + taskQueue.size());
			//while (taskQueue.size()>0) {

			while (1==1) {

				synchronized(this) {
					if (taskQueue.size()==0) {
						Logger.put("CONTROLLER WORKER THREAD: Nothing to do, going to wait!");
						try {
							wait();
						} catch (Exception e) {
							Logger.put("CONTROLLER WORKER THREAD: Exception during wait: " + e.toString());
						}
					}
				}

				Task t = taskQueue.remove(0);

				Logger.put("CONTROLLER WORKER THREAD: Picked a task from queue.");
				Logger.put("CONTROLLER WORKER THREAD: Size of the queue is now " + taskQueue.size());

				// Divide task into jobs and distribute them to nodes.
				// ---------------------------------------------------
				if (t.taskStatus == Task.TASKSTATUS_READYFORSENDINGTONODES) {

					Logger.put("CONTROLLER WORKER THREAD: Dividing task " + t.taskID + " into jobs and passing them to nodes.");

					// Open raw data files task
					// ------------------------
					if (t.taskType == Task.TASKTYPE_OPENRAWDATAFILES) {

						// Update task status
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.items.length; fileInd++) {
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.put("CONTROLLER WORKER THREAD: FATAL ERROR: Failed to define jobs to client");
							Logger.put(e.toString());
						}

						// Start assining jobs from the first node
						NodeInfo currentNodeInfo;
						Node currentNode;

						// Loop through items and create the jobs
						for (int fileInd=0; fileInd<t.items.length; fileInd++) {

							// Pick up next raw data file
							String fileName = ((File)(t.items[fileInd])).getName();

							// Use rawDataID as jobID
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);

							// Change job status
							t.jobStatus.put( jobIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Select the best node for processing this run
							currentNodeInfo = getBestAvailableNode();
							currentNode = currentNodeInfo.theNode;

							// Send job to node
							try {
								currentNode.addJob(taskIDCount, jobIDInteger.intValue(), ((File)(t.items[fileInd])));
							} catch (Exception e) {
								currentNode = null;
								Logger.put("CONTROLLER WORKER THREAD: ERROR - Failed to addJob to node.");
								Logger.put(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( jobIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( jobIDInteger, new String("Couldn't connect to node."));
							}


							if (currentNode!=null) {
								// Increase counter for runs assigned to this node
								currentNodeInfo.assignedRuns++;

								// Store this node to rawDataID->Node table
								nodeForRawData.put(jobIDInteger, currentNodeInfo);

								// Update node name for this job to client
								try {
									t.client.updateJobNode(new Integer(t.taskID), jobIDInteger, currentNodeInfo.name);
								} catch (Exception e) {
									Logger.put("CONTROLLER WORKER THREAD: ERROR - Failed to update name of processing node to client");
									Logger.put(e.toString());
								}
							}

						}

						// Do not process any other option during the same loop
						continue;

					}



					// Close raw data files task
					// ------------------------
					if (t.taskType == Task.TASKTYPE_CLOSERAWDATAFILES) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;


						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.itemIDs.length; fileInd++) {

							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.put("CONTROLLER WORKER THREAD: ERROR - Failed to define jobs to client");
							Logger.put(e.toString());
						}

						boolean saveChanges = ((Boolean)(t.taskParameters.get(Task.TASKPARAMETER_SAVECHANGES))).booleanValue();


						// Loop through items and create the jobs
						for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
							// Pick up next raw data ID and use it as jobID
							int rawDataID = t.itemIDs[itemInd];
							Integer rawDataIDInteger = new Integer(rawDataID);

							// Determine node that is processing this raw data file
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.put("CONTROLLER WORKER THREAD: ERROR - Failed to update name of processing node to client");
								Logger.put(e.toString());
							}


							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node
							try {
								currentNodeInfo.theNode.addJob(taskIDCount, rawDataID, saveChanges);
							} catch (Exception e) {
								Logger.put("CONTROLLER WORKER THREAD: ERROR - Failed to addJob to node.");
								Logger.put(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}

						}

						// Do not process any other option during the same loop
						continue;

					}


					// Refresh visualizers task
					// ------------------------
					if (t.taskType == Task.TASKTYPE_REFRESHVISUALIZERS) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.itemIDs.length; fileInd++) {
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						// Loop through items and create the jobs
						for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {
							// Pick up next raw data ID and use it as jobID
							int rawDataID = t.itemIDs[itemInd];
							Integer rawDataIDInteger = new Integer(rawDataID);

							// Pick up actual refresh request
							RawDataVisualizerRefreshRequest refreshRequest = (RawDataVisualizerRefreshRequest)(t.items[itemInd]);

							// Determine node that is processing this raw data file
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: ERROR - FAILED to update name of processing node to client");
								Logger.putFatal(e.toString());
							}


							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node
							try {
								currentNodeInfo.theNode.addJob(taskIDCount, rawDataID, refreshRequest);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
								Logger.putFatal(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}


						}

						// Do not process any other option during the same loop
						continue;

					}

					// Filter raw data files task
					// --------------------------
					if (t.taskType == Task.TASKTYPE_FILTERRAWDATAFILES) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.itemIDs.length; fileInd++) {
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						// Get the only parameter for this task (filter parameters object)
						FilterParameters filterParameters = (FilterParameters)(t.taskParameters.get(Task.TASKPARAMETER_FILTERPARAMETERS));

						// Loop through items and create the jobs
						for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {

							// Pick up next raw data ID and use it as jobID
							int rawDataID = t.itemIDs[itemInd];
							Integer rawDataIDInteger = new Integer(rawDataID);

							// Determine node that is processing this raw data file
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to update name of processing node to client");
								Logger.putFatal(e.toString());
							}


							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node

							try {
								currentNodeInfo.theNode.addJob(taskIDCount, rawDataID, filterParameters);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
								Logger.putFatal(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}

						}

						// Do not process any other option during the same loop
						continue;

					}

					// Find peaks task
					// ---------------
					if (t.taskType == Task.TASKTYPE_FINDPEAKS) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.itemIDs.length; fileInd++) {
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						// Get the only parameter for this task (filter parameters object)
						PeakPickerParameters parameters = (PeakPickerParameters)(t.taskParameters.get(Task.TASKPARAMETER_PEAKPICKERPARAMETERS));

						// Loop through items and create the jobs
						for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {

							// Pick up next raw data ID and use it as jobID
							int rawDataID = t.itemIDs[itemInd];
							Integer rawDataIDInteger = new Integer(rawDataID);

							// Determine node that is processing this raw data file
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to update name of processing node to client");
								Logger.putFatal(e.toString());
							}


							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node

							try {
								currentNodeInfo.theNode.addJob(taskIDCount, rawDataID, parameters);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
								Logger.putFatal(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}


						}

						// Do not process any other option during the same loop
						continue;

					}

					// Align peak lists task
					// ---------------------
					if (t.taskType == Task.TASKTYPE_ALIGNMENT) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						Integer jobIDInteger = new Integer(t.itemIDs[0]);
						jobDescriptions.put(jobIDInteger, new String("Align"));

						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						Hashtable<Integer, PeakList> peakLists = (Hashtable<Integer, PeakList>)(t.items[0]);

						// Get the only parameter for this task (filter parameters object)
						PeakListAlignerParameters parameters = (PeakListAlignerParameters)(t.taskParameters.get(Task.TASKPARAMETER_ALIGNMENTPARAMETERS));

						// Select the best node for processing this run
						NodeInfo currentNodeInfo = getBestAvailableNode();
						Node currentNode = currentNodeInfo.theNode;

						// Update job's node information on the client
						try {
							t.client.updateJobNode(new Integer(t.taskID), jobIDInteger, currentNodeInfo.name);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to update name of processing node to client");
							Logger.putFatal(e.toString());
						}

						// Change job status
						t.jobStatus.put( jobIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

						// Send job to node
						try {
							currentNodeInfo.theNode.addJob(taskIDCount, jobIDInteger.intValue(), peakLists, parameters);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
							Logger.putFatal(e.toString());
							// If sending failed, change job status to failed and set error message as result of the job.
							t.jobStatus.put( jobIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
							t.jobResult.put( jobIDInteger, new String("Couldn't connect to node."));
						}

						// Do not process any other option during the same loop
						continue;

					}

					// Fill-in gaps task
					// ---------------------
					if (t.taskType == Task.TASKTYPE_GAPFILL) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						AlignmentResult alignmentResult = (AlignmentResult)t.items[0];
						int[] rawDataIDs = alignmentResult.getRawDataIDs();
						for (int i=0; i<t.numOfJobs; i++) {
							Integer jobIDInteger = new Integer(rawDataIDs[i]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						// Get the only parameter for this task (gap filler parameters object)
						GapFillerParameters parameters = (GapFillerParameters)(t.taskParameters.get(Task.TASKPARAMETER_GAPFILLERPARAMETERS));

						// Loop through all raw data ids of the alignment result
						for (int rawDataID : rawDataIDs) {

							// Collect all empty slots for this run to a vector
							Hashtable<Integer, double[]> gapsToFill = new Hashtable<Integer, double[]>();

							int[] peakStatuses = alignmentResult.getPeakStatuses(rawDataID);
							for (int alignmentRowNum=0; alignmentRowNum<peakStatuses.length; alignmentRowNum++) {
								if (peakStatuses[alignmentRowNum]==AlignmentResult.PEAKSTATUS_NOTFOUND) {

									Integer rowInd = new Integer(alignmentRowNum);
									double[] coords = new double[2];
									coords[0] = alignmentResult.getAverageMZ(alignmentRowNum);
									coords[1] = alignmentResult.getAverageRT(alignmentRowNum);
									gapsToFill.put(rowInd, coords);
								}
							}

							// Get the node that is processing this raw data file

							Integer rawDataIDInteger = new Integer(rawDataID);
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to update name of processing node to client");
								Logger.putFatal(e.toString());
							}

							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node
							try {
								currentNodeInfo.theNode.addJob(t.taskID, rawDataID, gapsToFill, parameters);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
								Logger.putFatal(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}


						}

						// Do not process any other option during the same loop
						continue;

					}

					// Calc total raw signal
					// ---------------------
					if (t.taskType == Task.TASKTYPE_CALCTOTALRAWSIGNAL) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.itemIDs.length; fileInd++) {
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						// Loop through items and create the jobs
						for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {

							// Pick up next raw data ID and use it as jobID
							int rawDataID = t.itemIDs[itemInd];
							Integer rawDataIDInteger = new Integer(rawDataID);

							// Determine node that is processing this raw data file
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to update name of processing node to client");
								Logger.putFatal(e.toString());
							}


							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node

							try {
								currentNodeInfo.theNode.addJob(taskIDCount, rawDataID);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
								Logger.putFatal(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}

						}

						// Do not process any other option during the same loop
						continue;

					}

					// Process peak lists
					// ------------------
					if (t.taskType == Task.TASKTYPE_PROCESSPEAKLISTS) {

						// Update task status and number of jobs (equal to number of raw data files)
						t.taskStatus = Task.TASKSTATUS_UNDERPROCESSING;

						// Create information about jobs and pass it to client
						Hashtable<Integer, String> jobDescriptions = new Hashtable<Integer, String>();
						for (int fileInd=0; fileInd<t.itemIDs.length; fileInd++) {
							Integer jobIDInteger = new Integer(t.itemIDs[fileInd]);
							String fileName = rawDataNames.get(jobIDInteger);
							if (fileName==null) { fileName = "Unknown"; }
							jobDescriptions.put(jobIDInteger, fileName);
						}
						try {
							t.client.defineJobs(t.taskID, jobDescriptions);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to define jobs to client");
							Logger.putFatal(e.toString());
						}

						// Get parameter for this task (peak list processor's parameters object)
						PeakListProcessorParameters parameters = (PeakListProcessorParameters)(t.taskParameters.get(Task.TASKPARAMETER_PEAKLISTPROCESSORPARAMETERS));

						// Loop through items and create the jobs
						for (int itemInd=0; itemInd<t.itemIDs.length; itemInd++) {

							// Pick up next raw data ID and peak list
							int rawDataID = t.itemIDs[itemInd];
							Integer rawDataIDInteger = new Integer(rawDataID);
							PeakList peakList = (PeakList)(t.items[itemInd]);

							// Determine node that is processing this raw data file
							NodeInfo currentNodeInfo = nodeForRawData.get(rawDataIDInteger);

							// Update job's node information on the client
							try {
								t.client.updateJobNode(new Integer(t.taskID), rawDataIDInteger, currentNodeInfo.name);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to update name of processing node to client");
								Logger.putFatal(e.toString());
							}


							// Change job status
							t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_READYFORSENDINGTONODE );

							// Send job to node

							try {
								currentNodeInfo.theNode.addJob(taskIDCount, rawDataID, peakList, parameters);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to addJob to node.");
								Logger.putFatal(e.toString());
								// If sending failed, change job status to failed and set error message as result of the job.
								t.jobStatus.put( rawDataIDInteger, Task.JOBSTATUS_PROCESSINGFAILED );
								t.jobResult.put( rawDataIDInteger, new String("Couldn't connect to node."));
							}

						}

						// Do not process any other option during the same loop
						continue;



					}


				}



				// Collect job results and pass them to client
				// -------------------------------------------
				if (t.taskStatus == Task.TASKSTATUS_PROCESSINGDONE) {

					Logger.put("CONTROLLER WORKER THREAD: Collecting job results for task " + t.taskID + " and passing item results to the client.");

					// Open raw data files task
					if (t.taskType == Task.TASKTYPE_OPENRAWDATAFILES) {

						// In this task, combining job results is very simple.
						// They are already equal to item results, and no combining is needed

						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;


					}

					// Close raw data files task
					if (t.taskType == Task.TASKTYPE_CLOSERAWDATAFILES) {

						// In this task job result is equal to item result
						// Item result is raw data ID if close was a success and file name if it failed
						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;
					}

					// Refresh visualizers task
					if (t.taskType == Task.TASKTYPE_REFRESHVISUALIZERS) {

						// In this task job result is equal to item result
						// Item result is raw data ID if close was a success and file name if it failed
						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;
					}

					// Filter raw data files task
					if (t.taskType == Task.TASKTYPE_FILTERRAWDATAFILES) {

						// In this task job result is equal to item result
						// Item result is raw data ID if close was a success and file name if it failed
						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;
					}

					if (t.taskType == Task.TASKTYPE_FINDPEAKS) {

						// In this task job result is equal to item result
						// Item result is a peak list if peak picking was successful,
						// or a error message if it failed
						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: ERROR - Failed to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;

					}

					if (t.taskType == Task.TASKTYPE_ALIGNMENT) {

						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: ERROR - Failed to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;

					}

					// Fill-in gaps task
					// ---------------------
					if (t.taskType == Task.TASKTYPE_GAPFILL) {


						// Create a copy of the original alignment result
						AlignmentResult orgAlignmentResult = (AlignmentResult)(t.items[0]);
						String desc = "Results from " + orgAlignmentResult.getNiceName() + " with heights for missing peaks estimated from raw data.";
						AlignmentResult newAlignmentResult = new AlignmentResult(orgAlignmentResult, desc);


						// Loop through each job result
						Enumeration<Integer> statusEnum = t.jobStatus.elements();
						Enumeration<Integer> rawDataIDEnum = t.jobStatus.keys();
						while (statusEnum.hasMoreElements()) {
							Integer status = statusEnum.nextElement();
							Integer rawDataID = rawDataIDEnum.nextElement();

							// Was fill-in successful for this raw data file?
							if (status == Task.ITEMSTATUS_PROCESSINGDONE) {

								// Get all fill-ins for this raw data file
								Hashtable<Integer, double[]> fillIns = (Hashtable<Integer, double[]>)(t.jobResult.get(rawDataID));

								// Loop through all fill-ins for this raw data file
								Enumeration<Integer> alignmentRowEnum = fillIns.keys();
								Enumeration<double[]> valuesEnum = fillIns.elements();

								while (alignmentRowEnum.hasMoreElements()) {
									Integer alignmentRow = alignmentRowEnum.nextElement();
									double[] values = valuesEnum.nextElement();

									// Put values to new alignment result
									newAlignmentResult.setPeakStatus(rawDataID.intValue(), alignmentRow.intValue(), AlignmentResult.PEAKSTATUS_ESTIMATED);
									newAlignmentResult.setPeakMZ(rawDataID.intValue(), alignmentRow.intValue(), values[0]);
									newAlignmentResult.setPeakRT(rawDataID.intValue(), alignmentRow.intValue(), values[1]);
									newAlignmentResult.setPeakHeight(rawDataID.intValue(), alignmentRow.intValue(), values[2]);
									newAlignmentResult.setPeakArea(rawDataID.intValue(), alignmentRow.intValue(), values[3]);
								}

							}

							// Did fill-in task fail on this raw data file
							if (status == Task.ITEMSTATUS_PROCESSINGFAILED) {

								// Could not fill-in gaps in each raw data:
								newAlignmentResult = null;
								break;
							}

						}


						// KLUDGE BEGIN: This should be (and will be in future) done inside the simple gap filler (or any gap filler), but
						// code must be changed so that entire alignment result (or at least all columns of a sample) is passed to the gap-filler
						// (which would probably increase memory usage and cause problems with huge sample sets)

						// If fill-in was succesful for every raw data file
						if (newAlignmentResult!=null) {

							// Normalize all estimated peak areas
							// Loop through all area columns in this alignment results
							int[] rawDataIDs = newAlignmentResult.getRawDataIDs();
							int numOfRows = newAlignmentResult.getNumOfRows();
							for (int rawDataID : rawDataIDs) {

								// Calculate number of detected and estimated areas
								int numEstimated = 0;
								int numDetected = 0;
								for (int rowInd=0; rowInd<numOfRows; rowInd++) {
									if (newAlignmentResult.getPeakStatus(rawDataID, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) { numDetected++; }
									if ( (newAlignmentResult.getPeakStatus(rawDataID, rowInd)==AlignmentResult.PEAKSTATUS_ESTIMATED) && (newAlignmentResult.getPeakArea(rawDataID, rowInd)>0) ) { numEstimated++; }
								}

								Logger.put("DEBUG: numEstimated=" + numEstimated + " numDetected=" + numDetected);

								if (numEstimated==0) { Logger.put("DEBUG: numEstimated==0 => jump to next col."); continue; }

								// Collect peak heights and areas for both detected and estimated peaks
								double[] heightsEstimated = new double[numEstimated];
								double[] areasEstimated = new double[numEstimated];
								double[] heightsDetected = new double[numDetected];
								double[] areasDetected = new double[numDetected];

								int estInd = 0;
								int detInd = 0;
								for (int rowInd=0; rowInd<numOfRows; rowInd++) {
									if (newAlignmentResult.getPeakStatus(rawDataID, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) {
										heightsDetected[detInd] = java.lang.Math.log10(newAlignmentResult.getPeakHeight(rawDataID, rowInd));
										areasDetected[detInd] = java.lang.Math.log10(newAlignmentResult.getPeakArea(rawDataID, rowInd));
										detInd++;
									}
									if (
										(newAlignmentResult.getPeakStatus(rawDataID, rowInd)==AlignmentResult.PEAKSTATUS_ESTIMATED) &&
										(newAlignmentResult.getPeakArea(rawDataID, rowInd)>0)
									   ) {
										heightsEstimated[estInd] = java.lang.Math.log10(newAlignmentResult.getPeakHeight(rawDataID, rowInd));
										areasEstimated[estInd] = java.lang.Math.log10(newAlignmentResult.getPeakArea(rawDataID, rowInd));
										estInd++;
									}
								}

								// Do linear fitting for both estimated and detected height/area pairs
								RegressionCalculator regCalc = new RegressionCalculator(heightsDetected, areasDetected);
								double detectedSlope = regCalc.getSlope();
								double detectedIntercept = regCalc.getIntercept();

								regCalc = new RegressionCalculator(heightsEstimated, areasEstimated);
								double estimatedSlope = regCalc.getSlope();
								double estimatedIntercept = regCalc.getIntercept();


								// Replace every estimated area with normalized value
								for (int rowInd=0; rowInd<numOfRows; rowInd++) {
									if (
										(newAlignmentResult.getPeakStatus(rawDataID, rowInd)==AlignmentResult.PEAKSTATUS_ESTIMATED) &&
										(newAlignmentResult.getPeakArea(rawDataID, rowInd)>0)
									   ) {
										double height = java.lang.Math.log10(newAlignmentResult.getPeakHeight(rawDataID, rowInd));
										double area = java.lang.Math.log10(newAlignmentResult.getPeakArea(rawDataID, rowInd));

										double normalizationFactor = detectedIntercept-estimatedIntercept + (detectedSlope-estimatedSlope)*height;

										double normArea = java.lang.Math.pow(10.0, area + normalizationFactor);

										newAlignmentResult.setPeakArea(rawDataID, rowInd, normArea);
									}
								}

								Logger.put("DEBUG: detectedSlope=" + detectedSlope + " detectedIntercept=" + detectedIntercept);
								Logger.put("DEBUG: estimatedSlope=" + estimatedSlope + " estimatedIntercept=" + estimatedIntercept);

							}

						}

						// KLUDGE END.

						// If fill-in was succesful for every raw data file
						if (newAlignmentResult!=null) {

							jobIDCounter++;
							//newAlignmentResult.setAlignmentResultID(jobIDCounter);

							Hashtable<Integer, Integer> tmpStatus = new Hashtable<Integer, Integer>();
							tmpStatus.put(new Integer(jobIDCounter), new Integer(Task.ITEMSTATUS_PROCESSINGDONE));

							Hashtable<Integer, Object> tmpResult = new Hashtable<Integer, Object>();
							tmpResult.put(new Integer(jobIDCounter), newAlignmentResult);

							// Send result to client
							try {
								t.client.setTaskResults(t.taskID, tmpStatus, tmpResult);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
								Logger.putFatal(e.toString());
							}

						} else {

							// Send error message to client
							Hashtable<Integer, Integer> tmpStatus = new Hashtable<Integer, Integer>();
							tmpStatus.put(new Integer(0), new Integer(Task.ITEMSTATUS_PROCESSINGFAILED));

							Hashtable<Integer, Object> tmpResult = new Hashtable<Integer, Object>();
							tmpResult.put(new Integer(jobIDCounter), new String("Gap filling failed"));

							try {
								t.client.setTaskResults(t.taskID, tmpStatus, tmpResult);
							} catch (Exception e) {
								Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
								Logger.putFatal(e.toString());
							}

						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;

					}

					// Calc total raw signal
					// ---------------------
					if (t.taskType == Task.TASKTYPE_CALCTOTALRAWSIGNAL) {
						// In this task job results are equal to item results
						// Item results are the total raw signal for each raw data file
						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: FAILED to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;
					}


					// Process peak lists
					// ------------------
					if (t.taskType == Task.TASKTYPE_PROCESSPEAKLISTS) {

						// In this task job result is equal to item result
						// Item result is a peak list if peak picking was successful,
						// or a error message if it failed
						try {
							t.client.setTaskResults(t.taskID, t.jobStatus, t.jobResult);
						} catch (Exception e) {
							Logger.putFatal("CONTROLLER WORKER THREAD: ERROR - Failed to send results to the client.");
							Logger.putFatal(e.toString());
						}

						// Task done, delete it
						tasks.remove(t.taskID);
						t = null;

						// Do not process any other option during the same loop
						continue;


					}

				}

			}

			// Logger.put("CONTROLLER WORKER THREAD: Thread is finishing normally.");

		}

	}

}