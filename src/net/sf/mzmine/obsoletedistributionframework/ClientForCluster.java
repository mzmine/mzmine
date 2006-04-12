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
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.AlignmentResultProcessorParameters;
import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.LinearNormalizerParameters;
import net.sf.mzmine.methods.alignment.NormalizerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.filtering.FilterParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.obsoletedatastructures.RawDataOnTransit;
import net.sf.mzmine.userinterface.ClientDialog;
import net.sf.mzmine.userinterface.ItemSelector;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshResult;


/**
 *
 */
public class ClientForCluster {

	// This is the name of .ini-file storing settings for client
	private String settingsFilename = "mzmineclient.ini";

	// These must be loaded from .ini-file.
	private String controllerHostname;
	private String controllerPort;
	private String myPort;
	private String myDataRoot;

	// ----

	private MainWindow mainWin;					// MZmine main window (needed for displaying a modal dialog)
	private ClientDialog waitDialog;			// Wait dialog shown while cluster is operating
	private int waitTaskID;
	private int waitTaskType;

	private InetAddress myIP; 					// IP-address of the computer running client
	private String myHostname;					// Hostname of the computer running client

	private File myDataRootFile;

	private Client servicesForController;					// Client service listening to communication from controller
	private int myClientID;						// ClientID given by the controller

	private ControllerForClients myController;	// Remote controller service

	private Vector<AlignmentResult> originalAlignmentResults;	// These are used to do linear normalization on client-side immediately after calc total raw signal task finishes on cluster
	private LinearNormalizerParameters linearNormalizerParameters;

	// ----

	private boolean runningBatch = false;

	protected int[] batchRawDataIDs;
	protected FilterParameters batchFilterParameters1;
	protected FilterParameters batchFilterParameters2;
	protected FilterParameters batchFilterParameters3;
	protected PeakPickerParameters batchPeakPickerParameters;
	protected PeakListProcessorParameters batchPeakListProcessorParameters1;
	protected PeakListProcessorParameters batchPeakListProcessorParameters2;
	protected PeakListAlignerParameters batchAlignerParameters;
	protected AlignmentResultProcessorParameters batchAlignmentResultProcessorParameters1;
	protected GapFillerParameters batchGapFillerParameters;
	protected NormalizerParameters batchNormalizerParameters;

	protected Hashtable<Integer, PeakList> batchPeakLists;
	protected Hashtable<Integer, PeakList> batchPeakLists2;
	protected Hashtable<Integer, PeakList> batchPeakLists3;
	protected AlignmentResult batchAlignmentResult;
	protected AlignmentResult batchAlignmentResult2;
	protected AlignmentResult batchAlignmentResult3;
	protected AlignmentResult batchAlignmentResult4;
	protected int batchCurrentStage = 0;
	protected final int batchRestStage = 12;

	// ----

	// ----


	/**
	 * Constructor when running in cluster mode
	 * Initializes the client and connects to cluster.
	 */
	public ClientForCluster(MainWindow _mainWin) {
		mainWin = _mainWin;

		Logger.put("CLIENT: starting up... ");

		waitTaskID = -1;
		waitTaskType = -1;

		// Determine IP-address of the local host
		try {
			myIP = InetAddress.getLocalHost();
			myHostname = myIP.getHostName();
		} catch (Exception e) {
			Logger.put("CLIENT: ERROR - Failed to determine IP address and hostname of the computer.");
			Logger.put(e.toString());
			System.exit(0);
		}

		// Read settings from .INI file
		if (loadSettings(settingsFilename) == false ) {
			Logger.put("CLIENT: ERROR - Failed to read necessary settings from " + settingsFilename);
			System.exit(0);
		}

		myDataRootFile = new File(myDataRoot);

		Logger.put("CLIENT: Service running at IP-address:port " + myIP.getHostAddress() + ":" + myPort);

		// Start server for receiving communication from controller
		Logger.put("CLIENT: Starting client service...");
		try {
			servicesForController = new ClientImpl(this);
			Naming.rebind("rmi://" + myIP.getHostAddress() + ":" + myPort + "/ClientService", servicesForController);
			Logger.put("CLIENT: Starting client service, done.");
		} catch (Exception e) {
			Logger.putFatal("CLIENT: Starting client service, FAILED.");
			Logger.putFatal(e.toString());
			Logger.putFatal("Sorry, unable to continue.");
			System.exit(0);
		}

		// Connect to controller
		Logger.put("CLIENT: Connecting to cluster controller...");
		try {
			myController = (ControllerForClients)Naming.lookup("rmi://" + controllerHostname + ":" + controllerPort + "/ControllerForClientsService");
			myClientID = myController.connectToController(myIP, myPort);
			Logger.put("CLIENT: Connecting to cluster controller, done.");
		} catch (Exception e) {
			Logger.putFatal("CLIENT: Connecting to cluster controller FAILED.");
			Logger.putFatal(e.toString());
			Logger.putFatal("Sorry, unable to continue.");
			System.exit(0);
		}
		if (myClientID == -1) {
			Logger.putFatal("CLIENT: Controller FAILED to register client.");
			Logger.putFatal("Sorry, unable to continue.");
			System.exit(0);
		}

		Logger.put("CLIENT: Started ok!");

	}



	/**
	 * Constructor when running in cluster mode
	 * Initializes the client and connects to cluster.
	 */
	public ClientForCluster(MainWindow _mainWin, ControllerServer controllerServer) {
		mainWin = _mainWin;

		Logger.put("CLIENT: starting up... ");

		waitTaskID = -1;
		waitTaskType = -1;

		// Read settings from .INI file
		if (loadSettings(settingsFilename) == false ) {
			Logger.put("CLIENT: ERROR - Failed to read necessary settings from " + settingsFilename);
			System.exit(0);
		}

		myDataRootFile = new File(myDataRoot);

		try {
			servicesForController = new ClientImpl(this);
			myController = controllerServer.getServicesForClients();
			myClientID = myController.connectToController(this);
		} catch (Exception e) {}



		Logger.put("CLIENT: Started ok!");

	}

	public Client getServicesForController() {
		return servicesForController;
	}


	/**
	 * This method returns the path that points to the common data storage.
	 */
	public String getDataRootPath() {

		try {
			return myDataRootFile.getCanonicalPath();
		} catch (java.io.IOException e) {
			return null;
		}

	}


	/**
	 * This method initiates a batch processing of several tasks
	 */
	public void startBatch(				int[] rawDataIDs,
										FilterParameters filterParameters1,
										FilterParameters filterParameters2,
										FilterParameters filterParameters3,
										PeakPickerParameters peakPickerParameters,
										PeakListProcessorParameters peakListProcessorParameters1,
										PeakListProcessorParameters peakListProcessorParameters2,
										PeakListAlignerParameters alignerParameters,
										AlignmentResultProcessorParameters alignmentResultProcessorParameters1,
										GapFillerParameters gapFillerParameters,
										NormalizerParameters normalizerParameters) {

		batchRawDataIDs = rawDataIDs;
		batchFilterParameters1 = filterParameters1;
		batchFilterParameters2 = filterParameters2;
		batchFilterParameters3 = filterParameters3;
		batchPeakPickerParameters = peakPickerParameters;
		batchPeakListProcessorParameters1 = peakListProcessorParameters1;
		batchPeakListProcessorParameters2 = peakListProcessorParameters2;
		batchAlignerParameters = alignerParameters;
		batchAlignmentResultProcessorParameters1 = alignmentResultProcessorParameters1;
		batchGapFillerParameters = gapFillerParameters;
		batchNormalizerParameters = normalizerParameters;

		runningBatch = true;

		batchCurrentStage = 0;
		mainWin.getStatusBar().setStatusText("Running a batch.");

		nextBatchStage();

	}

	/**
	 * This method starts next batch processing step
	 */
	public void nextBatchStage() {


		batchCurrentStage++;
		boolean busyOrEnd = false;

		while (!busyOrEnd) {

			switch (batchCurrentStage) {
				// Filter 1
				case 1:
					if (batchFilterParameters1!=null) {
						filterRawDataFiles(batchRawDataIDs, batchFilterParameters1);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Filter 2
				case 2:
					if (batchFilterParameters2!=null) {
						filterRawDataFiles(batchRawDataIDs, batchFilterParameters2);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Filter 3
				case 3:
					if (batchFilterParameters3!=null) {
						filterRawDataFiles(batchRawDataIDs, batchFilterParameters3);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Picker (generates batchPeakLists)
				case 4:
					if (batchPeakPickerParameters!=null) {
						findPeaks(batchRawDataIDs, batchPeakPickerParameters);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Peak list processor 1 (transforms batchPeakLists => batchPeakLists)
				case 5:
					if (batchPeakListProcessorParameters1!=null) {
						processPeakLists(batchPeakLists, batchPeakListProcessorParameters1);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Peak list processor 1 (transforms batchPeakLists => batchPeakLists)
				case 6:
					if (batchPeakListProcessorParameters2!=null) {
						processPeakLists(batchPeakLists, batchPeakListProcessorParameters2);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Alignment (batchPeakLists3 => batchAlignmentResult)
				case 7:
					if (batchAlignerParameters!=null) {
						doAlignment(batchPeakLists, batchAlignerParameters);
						busyOrEnd = true;
					} else { batchCurrentStage++; }
					break;

				// Alignment processor (batchAlignmentResult => batchAlignmentResult2)
				case 8:
					if (batchAlignmentResultProcessorParameters1!=null) {
						if (batchAlignmentResultProcessorParameters1.getClass() == AlignmentResultFilterByGapsParameters.class) {
							// Filter by number of detections
							AlignmentResultFilterByGapsParameters arfbgParams = (AlignmentResultFilterByGapsParameters)batchAlignmentResultProcessorParameters1;
							batchAlignmentResult2 = mainWin.runAlignmentResultFilteringByGapsClientSide(arfbgParams, batchAlignmentResult);
							batchCurrentStage++;
						}
					} else {
						batchAlignmentResult2 = batchAlignmentResult;
						batchAlignmentResult = null;
						batchCurrentStage++;
					}
					break;


				// Gap-filling (batchAlignmentResult2 => batchAlignmentResult3)
				case 9:
					if (batchGapFillerParameters!=null) {
						fillGaps(batchAlignmentResult2, batchGapFillerParameters);
						busyOrEnd = true;
					} else {
						batchAlignmentResult3 = batchAlignmentResult2;
						batchAlignmentResult2 = null;
						batchCurrentStage++;
					}
					break;

				// Normalization (batchAlignmentResult3 => batchAlignmentResult4)
				case 10:
					batchAlignmentResult = null;
					if (batchNormalizerParameters!=null) {
						if (batchNormalizerParameters.getClass() == LinearNormalizerParameters.class) {
							LinearNormalizerParameters lnp = (LinearNormalizerParameters)batchNormalizerParameters;

							Vector<AlignmentResult> originalAlignmentResults = new Vector<AlignmentResult>();
							originalAlignmentResults.add(batchAlignmentResult3);

							// If linear normalization is by total raw signal, then we must first fetch total raw signal
							if (lnp.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL) {

								calcTotalRawSignal(batchAlignmentResult3.getRawDataIDs(), lnp, originalAlignmentResults);
								busyOrEnd = true;
							} else {
								mainWin.doLinearNormalizationClientSide(lnp, originalAlignmentResults);
								batchCurrentStage++;
							}
						}
					} else {
						batchAlignmentResult4 = null;
						batchAlignmentResult3 = null;
						batchCurrentStage++;
					}
					break;

				// Finally refresh visualizers
				case 11:
					batchCurrentStage++;
					InitiateLateRefreshThread helperThread = new InitiateLateRefreshThread(RawDataVisualizer.CHANGETYPE_PEAKSANDDATA, batchRawDataIDs);
					helperThread.start();

					runningBatch = false;
					busyOrEnd = true;
					break;

				// End of batch
				case 12:
				default:

			}

		}

	}




	/**
	 * This methods initiates a open raw data files task
	 * @param fullFilePaths		Array of file path strings that point to raw data files
	 */
	public synchronized void openRawDataFiles(String fullFilePaths[]) {

		// Not necessary?
		if (waitDialog != null) { return; }

		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Opening raw data files, please wait...");
		waitTaskType = Task.TASKTYPE_OPENRAWDATAFILES;
		waitDialog.showMe();
		mainWin.setBusy(true);


		Logger.put("CLIENT: Giving open raw data files task to controller.");
		try {

			File[] rawFiles = new File[fullFilePaths.length];
			for (int i=0; i<fullFilePaths.length; i++) { rawFiles[i] = new File(fullFilePaths[i]); }
			File[] rawFilesStrippedPath = new File[fullFilePaths.length];

			for (int i=0; i<fullFilePaths.length; i++) {

				rawFilesStrippedPath[i] = stripFullFilePath(rawFiles[i], myDataRootFile);

			}

			waitTaskID = myController.addTask(myClientID, waitTaskType, rawFilesStrippedPath);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());
			mainWin.displayErrorMessage("Passing task to cluster failed.");


		}

	}

	/**
	 * This method initiates a close raw data files task
	 * @param	rawDataIDs	Array of rawDataIDs for files to be closed
	 */
	public synchronized void closeRawDataFiles(int[] rawDataIDs) {

		// Not necessary?
		if (waitDialog != null) { return; }


		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Closing raw data files, please wait...");
		waitTaskType = Task.TASKTYPE_CLOSERAWDATAFILES;
		waitDialog.showMe();
		mainWin.setBusy(true);

		// Contact control and pass the task
		Logger.put("CLIENT: Giving close raw data files task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, rawDataIDs, false);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);
			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: ERROR - Failed to give task to controller.");
			Logger.put(e.toString());

		}

	}

	/**
	 * This method does initiates filtering on given raw data files
	 * @param	rawDataIDs	Array of rawDataIDs for files to be closed
	 */
	public synchronized void filterRawDataFiles(int[] rawDataIDs, FilterParameters filterParameters) {

		// Not necessary?
		if (waitDialog != null) { return; }

		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Filtering raw data files, please wait...");
		waitTaskType = Task.TASKTYPE_FILTERRAWDATAFILES;
		waitDialog.showMe();
		mainWin.setBusy(true);

		// Contact control and pass the task
		Logger.put("CLIENT: Giving filter raw data files task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, rawDataIDs, filterParameters);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			runningBatch = false; batchCurrentStage = batchRestStage;
			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());

		}

	}

	/**
	 * This method initiates a visualizer refresh task
	 * @param	refreshRequests	Array of refresh requests, one for each raw data file whose visualizers should be redrawn
	 */
	public synchronized void refreshVisualizers(RawDataVisualizerRefreshRequest[] refreshRequests) {

		//Logger.put("refreshVisualizers: starting");

		// Not necessary?
		if (waitDialog != null) {
			//Logger.put("refreshVisualizers: waitDialog not null, returning");
			return;
		}

		//Logger.put("refreshVisualizers: Initialize wait dialog");

		// Initialize wait dialog

		waitDialog = new ClientDialog(mainWin);

				//Logger.put("refreshVisualizers: new waitDialog created");
		waitDialog.setTitle("Redrawing raw data visualizers, please wait...");

		waitTaskType = Task.TASKTYPE_REFRESHVISUALIZERS;
		waitDialog.showMe();
		mainWin.setBusy(true);
				//Logger.put("refreshVisualizers: showme called");


		// Contact control and pass the task
		Logger.put("CLIENT: Giving refresh visualizers task to controller.");
		try {
			waitTaskID = myController.addTask(myClientID, waitTaskType, refreshRequests);
		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());

		}

	}

	/**
	 * This method initiates a peak picking task
	 */
	public synchronized void findPeaks(int[] rawDataIDs, PeakPickerParameters parameters) {

		// Not necessary?
		if (waitDialog != null) { return; }

		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Searching for peaks in raw data files, please wait...");
		waitTaskType = Task.TASKTYPE_FINDPEAKS;
		waitDialog.showMe();
		mainWin.setBusy(true);

		// Contact control and pass the task
		Logger.put("CLIENT: Giving peak picking task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, rawDataIDs, parameters);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			runningBatch = false; batchCurrentStage = batchRestStage;
			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());
		}

	}

	/**
	 * This method initiates a peak list processing task
	 */
	public synchronized void processPeakLists(Hashtable<Integer, PeakList> peakLists, PeakListProcessorParameters parameters) {

		// Not necessary?
		if (waitDialog != null) { return; }

		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Processing peak lists, please wait...");
		waitTaskType = Task.TASKTYPE_PROCESSPEAKLISTS;
		waitDialog.showMe();
		mainWin.setBusy(true);

		// Contact control and pass the task
		Logger.put("CLIENT: Giving peak list processing task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, peakLists, parameters);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			runningBatch = false; batchCurrentStage = batchRestStage;
			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());
		}

	}

	/**
	 * This method initiates a peak alignment task
	 */
	public synchronized void doAlignment(Hashtable<Integer, PeakList> peakLists, PeakListAlignerParameters parameters) {

		// Not necessary?
		if (waitDialog != null) { return; }

		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Aligning peak lists, please wait...");
		waitTaskType = Task.TASKTYPE_ALIGNMENT;
		waitDialog.showMe();
		mainWin.setBusy(true);

		// Contact control and pass the task
		Logger.put("CLIENT: Giving alignment task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, peakLists, parameters);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			runningBatch = false; batchCurrentStage = batchRestStage;
			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());
		}

	}

	/**
	 * This method intiates a gap-filling task
	 */
	public synchronized void fillGaps(AlignmentResult alignmentResult, GapFillerParameters parameters) {

		// Not necessary?
		if (waitDialog != null) { return; }

		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Filling gaps in alignment result, please wait...");
		waitTaskType = Task.TASKTYPE_GAPFILL;
		waitDialog.showMe();
		mainWin.setBusy(true);

		// Contact control and pass the task
		Logger.put("CLIENT: Giving gap filling task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, alignmentResult, parameters);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			runningBatch = false; batchCurrentStage = batchRestStage;
			mainWin.displayErrorMessage("Passing task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());
		}

	}


	/**
	 * This method initiates normalization by total raw signal.
	 * This has to be done using the controller (unlike other normalization method), because
	 * this method requires access to the raw data files (for calculating to total raw signal)
	 */
	public synchronized void calcTotalRawSignal(int rawDataIDs[], LinearNormalizerParameters lnp, Vector<AlignmentResult> _originalAlignmentResults) {

		// Not necessary?
		if (waitDialog != null) { return; }

		// Initialize wait dialog
		waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Calculating total raw signal, please wait...");
		waitTaskType = Task.TASKTYPE_CALCTOTALRAWSIGNAL;
		waitDialog.showMe();
		mainWin.setBusy(true);

		originalAlignmentResults = _originalAlignmentResults;
		linearNormalizerParameters = lnp;

		// Contact control and pass the task
		Logger.put("CLIENT: Giving normalization task to controller.");
		try {

			waitTaskID = myController.addTask(myClientID, waitTaskType, rawDataIDs);

		} catch (Exception e) {
			waitDialog.hideMe();
			waitDialog = null;
			waitTaskID = -1;
			waitTaskType = -1;
			mainWin.setBusy(false);

			runningBatch = false; batchCurrentStage = batchRestStage;
			mainWin.displayErrorMessage("Passing normalization task to cluster failed.");
			Logger.put("CLIENT: FAILED to give task to controller.");
			Logger.put(e.toString());
		}



	}


	/**
	 * This method is called when results are ready for a task
	 * itemStates
	 */
	public synchronized void setTaskResults(int taskID, Hashtable<Integer, Integer> itemStates, Hashtable<Integer, Object> itemResults) {

		Logger.put("CLIENT: Received task results for taskID " + taskID);

		if (waitTaskID != taskID) {
			Logger.putFatal("CLIENT: FATAL ERROR - taskID mismatch in setTaskResults(): offered taskID=" + taskID + " waitTaskID=" + waitTaskID + " and waitTaskType=" + waitTaskType);
		}

		if (waitTaskType == Task.TASKTYPE_OPENRAWDATAFILES) {

			// Loop through all items and put raw data object into item selector and also collect possible error messages
			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Object> itemResultEnum = itemResults.elements();
			Integer itemState;
			ItemSelector itemSelector = mainWin.getItemSelector();
			boolean somethingFailed = false;
			String failedFiles = "";

			while (itemStateEnum.hasMoreElements()) {
				itemState = itemStateEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {
					// Add rawdata objects to item storage
					RawDataOnTransit rdot = (RawDataOnTransit)itemResultEnum.nextElement();
					RawDataAtClient rdac = new RawDataAtClient(rdot);
					mainWin.addRawDataVisualizers(rdac);
					itemSelector.addRawData(rdac);

				}
				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {
					somethingFailed = true;
					// Collect name of the file for error message
					String failedFile = (String)itemResultEnum.nextElement();
					failedFiles += failedFile + " ";
				}
			}

			// Inform user about possible errors and completion of the task
			if (somethingFailed) {
				mainWin.displayErrorMessage("Could not open raw data files: " + failedFiles);
			}
			mainWin.getStatusBar().setStatusText("File open done.");
			waitTaskID = -1; waitTaskType = -1;

			waitDialog.hideMe();
			mainWin.repaint();
			waitDialog = null;
			mainWin.setBusy(false);

			return;
		}



		if (waitTaskType == Task.TASKTYPE_CLOSERAWDATAFILES) {

			// Loop through all items and remove raw data files from item selector if they were closed successfully. If close failed then collect file names for an error message
			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Object> itemResultEnum = itemResults.elements();
			Integer itemState;
			ItemSelector itemSelector = mainWin.getItemSelector();
			boolean somethingFailed = false;
			String failedFiles = "";

			while (itemStateEnum.hasMoreElements()) {

				itemState = itemStateEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {

					Integer rawDataID = (Integer)(itemResultEnum.nextElement());
					mainWin.getItemSelector().removeRawData(rawDataID.intValue());
				}

				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {

					somethingFailed = true;
					String failedFile = (String)(itemResultEnum.nextElement());
					failedFiles += failedFile + " ";
				}

			}

			// Inform user about possible errors and completion of the task
			if (somethingFailed) {
				mainWin.displayErrorMessage("Could not close raw data files: " + failedFiles);
				runningBatch = false; batchCurrentStage = batchRestStage;
			}
			mainWin.getStatusBar().setStatusText("File close done.");
			waitTaskID = -1; waitTaskType = -1;

			waitDialog.hideMe();
			mainWin.repaint();
			waitDialog = null;
			mainWin.setBusy(false);

			return;
		}

		if (waitTaskType == Task.TASKTYPE_REFRESHVISUALIZERS) {

			//Logger.put("waitTaskType == Task.TASKTYPE_REFRESHVISUALIZERS");

			// Loop through all items and pass the data to corresponding visualizers
			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Object> itemResultEnum = itemResults.elements();
			Integer itemState;
			ItemSelector itemSelector = mainWin.getItemSelector();
			boolean somethingFailed = false;
			String failedFiles = "";
			int failCount = 0;

			while (itemStateEnum.hasMoreElements()) {
				//Logger.put("Next element");

				itemState = itemStateEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {
					//Logger.put("element done");
					RawDataVisualizerRefreshResult refreshResult = (RawDataVisualizerRefreshResult)(itemResultEnum.nextElement());
					//Logger.put("calling mainWin.doRefreshRawDataVisualizers");
					mainWin.doRefreshRawDataVisualizers(refreshResult);

				}

				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {
					//Logger.put("element failed");
					somethingFailed = true;
					failCount++;
				}
			}

			// Inform user about possible errors and completion of the task
			if (somethingFailed) {
				mainWin.displayErrorMessage("Could not refresh visualizers for " + failCount + " raw data file(s).");
			}
			mainWin.getStatusBar().setStatusText("Visualizer refresh done.");
			waitTaskID = -1; waitTaskType = -1;

			waitDialog.hideMe();
			mainWin.repaint();
			waitDialog = null;
			mainWin.setBusy(false);


			//Logger.put("returning from setTaskResults");
			return;

		}

		if (waitTaskType == Task.TASKTYPE_FILTERRAWDATAFILES) {

			// Loop through all items and collect IDs of raw data files that were successfully filtered
			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Integer> rawDataIDEnum = itemResults.keys();
			Enumeration<Object> itemResultEnum = itemResults.elements();

			Integer itemState;
			Integer rawDataID;
			ItemSelector itemSelector = mainWin.getItemSelector();
			boolean somethingFailed = false;
			String failedFiles = "";
			int failCount = 0;

			Vector<Integer> filteredRawDataIDs = new Vector<Integer>();

			while (itemStateEnum.hasMoreElements()) {

				itemState = itemStateEnum.nextElement();
				rawDataID = rawDataIDEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {
					// Update raw data at client-side with raw data coming from node-side.
					RawDataOnTransit rdot = (RawDataOnTransit)itemResultEnum.nextElement();
					RawDataAtClient rdac = mainWin.getItemSelector().getRawDataByID(rawDataID);
					rdac.updateFromRawDataOnTransit(rdot);

					// Collect the rawDataID for refreshing visualizers
					filteredRawDataIDs.add(rawDataID);
				}

				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {
					somethingFailed = true;
					failCount++;
				}
			}

			// Inform user about possible errors and completion of the task
			if (somethingFailed) {
				mainWin.displayErrorMessage("Could not filter " + failCount + " raw data file(s).");
				runningBatch = false; batchCurrentStage = batchRestStage;
			}
			if (!runningBatch) { mainWin.getStatusBar().setStatusText("Filtering done."); }


			// Check which filtered raw data files have visible visualizers
			int numOfRefreshNeeded = 0;
			for (int i=0; i<filteredRawDataIDs.size(); i++) {
				rawDataID = filteredRawDataIDs.get(i);
				if (mainWin.rawDataHasVisibleVisualizers(rawDataID.intValue(), true)) {
					numOfRefreshNeeded++;
				}
			}


			// Does some raw data files require visualizer refreshing after filtering?
			if ( (numOfRefreshNeeded>0) && (!runningBatch) ) {

				// Move IDs of raw data files that need refreshing to a new array
				int[] filteredRawDataIDsArray = new int[numOfRefreshNeeded];
				int j=0;
				for (int i=0; i<filteredRawDataIDs.size(); i++) {
					rawDataID = filteredRawDataIDs.get(i);
					if (mainWin.rawDataHasVisibleVisualizers(rawDataID.intValue(), true)) {
						filteredRawDataIDsArray[j] = rawDataID;
						j++;
					}
				}

				waitTaskID = -1; waitTaskType = -1;
				waitDialog.hideMe();
				waitDialog = null;
				mainWin.setBusy(false);

				// Start visualizer refresh
				InitiateLateRefreshThread helperThread = new InitiateLateRefreshThread(RawDataVisualizer.CHANGETYPE_DATA, filteredRawDataIDsArray);
				helperThread.start();
				//mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_DATA, filteredRawDataIDsArray);

			} else {

				waitTaskID = -1; waitTaskType = -1;
				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

				if (runningBatch) {
					ContinueBatchThread batchThread = new ContinueBatchThread(this);
					batchThread.start();
				}
			}
		}

		if (waitTaskType == Task.TASKTYPE_FINDPEAKS) {

			// Loop through all items and collect IDs of raw data files that were successfully filtered
			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Integer> rawDataIDEnum = itemResults.keys();
			Enumeration<Object> itemResultEnum = itemResults.elements();

			Integer itemState;
			Integer rawDataID;
			ItemSelector itemSelector = mainWin.getItemSelector();
			boolean somethingFailed = false;
			String failedFiles = "";
			int failCount = 0;

			Vector<Integer> foundPeaksRawDataIDs = new Vector<Integer>();

			batchPeakLists = new Hashtable<Integer, PeakList>();

			while (itemStateEnum.hasMoreElements()) {

				itemState = itemStateEnum.nextElement();
				rawDataID = rawDataIDEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {

					// Get raw data by raw data id (item key)
					RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);

					// Get peak list (item result)
					PeakList peakList = (PeakList)(itemResultEnum.nextElement());

					// When in batch mode, collect all peak lists for next step
					if (runningBatch) {	batchPeakLists.put(rawDataID, peakList); }

					// Assign peak list to raw data
					rawData.setPeakList(peakList);

					// Show all visualizers (including peak list!) & tile windows
					//mainWin.toggleVisualizers(rawDataID.intValue(), true);
					mainWin.showRawDataPeakListVisualizer(rawDataID.intValue());

					// Store raw data ID for visualizer refreshing
					foundPeaksRawDataIDs.add(rawDataID);

				}

				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {
					Logger.put("CLIENT: Peak picking FAILED on rawDataID " + rawDataID);
					somethingFailed = true;
					failCount++;
				}

			}

			// Update menu (when raw data has peaks, there are more options available)
			mainWin.updateMenuAvailability();

			// Re-tile windows
			mainWin.tileWindows();


			// Inform user about possible errors and completion of the task
			if (somethingFailed) {
				if (runningBatch) { batchPeakLists = null; }
				mainWin.displayErrorMessage("Could not do peak picking on " + failCount + " raw data file(s).");
			}
			if (!runningBatch) { mainWin.getStatusBar().setStatusText("Peak picking done."); }

			if ((foundPeaksRawDataIDs.size()>0) && (!runningBatch)) {

				int[] foundPeaksRawDataIDArray = new int[foundPeaksRawDataIDs.size()];
				for (int i=0; i<foundPeaksRawDataIDArray.length; i++) { foundPeaksRawDataIDArray[i] = foundPeaksRawDataIDs.get(i).intValue(); }

				waitTaskID = -1; waitTaskType = -1;
				waitDialog.hideMe();
				waitDialog = null;
				mainWin.setBusy(false);

				// Start visualizer refresh
				InitiateLateRefreshThread helperThread = new InitiateLateRefreshThread(RawDataVisualizer.CHANGETYPE_PEAKS, foundPeaksRawDataIDArray);
				helperThread.start();

			} else {

				waitTaskID = -1; waitTaskType = -1;

				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

				if (runningBatch) {
					if (somethingFailed) {
						runningBatch = false; batchCurrentStage = batchRestStage;
					} else {
						ContinueBatchThread batchThread = new ContinueBatchThread(this);
						batchThread.start();
					}
				}
			}
		}

		if (waitTaskType == Task.TASKTYPE_ALIGNMENT) {
			// Only a single item is expected, so pick-up its status and result


			Integer itemState = itemStates.elements().nextElement();
			if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {
				AlignmentResult alignmentResult = (AlignmentResult)(itemResults.elements().nextElement());

				if (runningBatch) { batchAlignmentResult = alignmentResult; }

				mainWin.getItemSelector().addAlignmentResult(alignmentResult);
				mainWin.addAlignmentResultVisualizerList(alignmentResult);
				if (!runningBatch) { mainWin.getStatusBar().setStatusText("Alignment done."); }

				waitTaskID = -1; waitTaskType = -1;

				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

				if (runningBatch) {
					ContinueBatchThread batchThread = new ContinueBatchThread(this);
					batchThread.start();
				}
			}
			if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {

				if (runningBatch) { batchAlignmentResult = null; }

				// Inform user about possible errors and completion of the task
				mainWin.displayErrorMessage("Could not do alignment.");
				mainWin.getStatusBar().setStatusText("Alignment failed.");
				waitTaskID = -1; waitTaskType = -1;
				runningBatch = false; batchCurrentStage = batchRestStage;

				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

			}
		}



		if (waitTaskType == Task.TASKTYPE_GAPFILL) {


			Integer itemState = itemStates.elements().nextElement();

			if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {

				Enumeration tmpEnum = itemResults.elements();
				AlignmentResult alignmentResult = (AlignmentResult)(tmpEnum.nextElement());
				mainWin.getItemSelector().addAlignmentResult(alignmentResult);
				mainWin.addAlignmentResultVisualizerList(alignmentResult);
				if (!runningBatch) { mainWin.getStatusBar().setStatusText("Alignment done."); }
				if (runningBatch) { batchAlignmentResult3 = alignmentResult; }
				waitTaskID = -1; waitTaskType = -1;

				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

				if (runningBatch) {
					ContinueBatchThread batchThread = new ContinueBatchThread(this);
					batchThread.start();
				}
			}

			if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {

				// Inform user about possible errors and completion of the task
				mainWin.displayErrorMessage("Could not do gap filling.");
				mainWin.getStatusBar().setStatusText("Gap filling failed.");
				waitTaskID = -1; waitTaskType = -1;
				runningBatch = false; batchCurrentStage = batchRestStage;

				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

			}

		}

		if (waitTaskType == Task.TASKTYPE_CALCTOTALRAWSIGNAL) {

			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Integer> rawDataIDEnum = itemResults.keys();
			Enumeration<Object> itemResultEnum = itemResults.elements();

			boolean somethingFailed = false;
			while (itemStateEnum.hasMoreElements()) {

				Integer itemState = itemStateEnum.nextElement();
				Integer rawDataID = rawDataIDEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {
					Double totalRawSignal = (Double)(itemResultEnum.nextElement());
					RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID.intValue());
					rawData.setTotalRawSignal(totalRawSignal);
				}

				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {
					somethingFailed = true;
				}

			}

			if (somethingFailed) {

				waitTaskID = -1; waitTaskType = -1;

				mainWin.displayErrorMessage("Could not calculate total raw signal.");
				mainWin.getStatusBar().setStatusText("Normalization failed.");
				runningBatch = false; batchCurrentStage = batchRestStage;


			} else {
				waitTaskID = -1; waitTaskType = -1;
				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

				if ( linearNormalizerParameters != null) {	mainWin.doLinearNormalizationClientSide(linearNormalizerParameters, originalAlignmentResults); }

				if (runningBatch) {
					ContinueBatchThread batchThread = new ContinueBatchThread(this);
					batchThread.start();
				}

			}

		}


		if (waitTaskType == Task.TASKTYPE_PROCESSPEAKLISTS) {

			// Loop through all items and collect IDs of raw data files that were successfully filtered
			Enumeration<Integer> itemStateEnum = itemStates.elements();
			Enumeration<Integer> rawDataIDEnum = itemResults.keys();
			Enumeration<Object> itemResultEnum = itemResults.elements();

			Integer itemState;
			Integer rawDataID;
			ItemSelector itemSelector = mainWin.getItemSelector();
			boolean somethingFailed = false;
			String failedFiles = "";
			int failCount = 0;

			Vector<Integer> foundPeaksRawDataIDs = new Vector<Integer>();

			batchPeakLists = new Hashtable<Integer, PeakList>();

			while (itemStateEnum.hasMoreElements()) {

				itemState = itemStateEnum.nextElement();
				rawDataID = rawDataIDEnum.nextElement();

				if (itemState == Task.ITEMSTATUS_PROCESSINGDONE) {

					// Get raw data by raw data id (item key)
					RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);

					// Get peak list (item result)
					PeakList peakList = (PeakList)(itemResultEnum.nextElement());

					// When in batch mode, collect all peak lists for next step
					if (runningBatch) {	batchPeakLists.put(rawDataID, peakList); }

					// Assign peak list to raw data
					rawData.setPeakList(peakList);

					// Store raw data ID for visualizer refreshing
					foundPeaksRawDataIDs.add(rawDataID);

				}

				if (itemState == Task.ITEMSTATUS_PROCESSINGFAILED) {
					Logger.put("CLIENT: Peak picking FAILED on rawDataID " + rawDataID);
					somethingFailed = true;
					failCount++;
				}

			}

			// Inform user about possible errors and completion of the task
			if (somethingFailed) {
				if (runningBatch) { batchPeakLists = null; }
				mainWin.displayErrorMessage("Could not process peak list on " + failCount + " raw data file(s).");
			}
			if (!runningBatch) { mainWin.getStatusBar().setStatusText("Peak list processing done."); }

			if ((foundPeaksRawDataIDs.size()>0) && (!runningBatch)) {

				int[] foundPeaksRawDataIDArray = new int[foundPeaksRawDataIDs.size()];
				for (int i=0; i<foundPeaksRawDataIDArray.length; i++) { foundPeaksRawDataIDArray[i] = foundPeaksRawDataIDs.get(i).intValue(); }

				waitTaskID = -1; waitTaskType = -1;
				waitDialog.hideMe();
				waitDialog = null;
				mainWin.setBusy(false);

				// Start visualizer refresh
				InitiateLateRefreshThread helperThread = new InitiateLateRefreshThread(RawDataVisualizer.CHANGETYPE_PEAKS, foundPeaksRawDataIDArray);
				helperThread.start();

			} else {

				waitTaskID = -1; waitTaskType = -1;

				waitDialog.hideMe();
				mainWin.repaint();
				waitDialog = null;
				mainWin.setBusy(false);

				if (runningBatch) {
					if (somethingFailed) {
						runningBatch = false; batchCurrentStage = batchRestStage;
					} else {
						ContinueBatchThread batchThread = new ContinueBatchThread(this);
						batchThread.start();
					}
				}
			}


		}

	}


	/**
	 * This method sets the title of the wait dialog
	 */
// TEST synchronized
	public synchronized void setTaskDescription(String desc) {


		waitDialog.setTitle(desc);

	}


	/**
	 * This method defines jobs in the wait dialog
	 */
// TEST synchronized
	public synchronized void defineJobs(int taskID, Hashtable<Integer, String> jobDescription) {


		Enumeration<String> descEnum = jobDescription.elements();
		Enumeration<Integer> idEnum = jobDescription.keys();

		String strUnknown = "Unknown";
		Double fltZero = new Double(0);

		while (idEnum.hasMoreElements()) {
			String desc = descEnum.nextElement();
			Integer id = idEnum.nextElement();

			waitDialog.addJob(id, desc, strUnknown, Task.JOBSTATUS_UNKNOWN_STR, fltZero);
		}


	}


	/**
	 * This method updates the node information for a job in the wait dialog
	 */
// TEST synchronized
	public synchronized void updateJobNode(Integer taskID, Integer jobID, String nodeName) {

		waitDialog.updateJobNode(jobID, nodeName);

	}

	/**
	 * This method updates jobs' processing status in the wait dialog
	 */
// TEST synchronized
	public synchronized void updateJobStatus(Integer taskID, Integer jobID, Integer jobStateID, Double jobCompletionRate) {



		String jobState = Task.JOBSTATUS_UNKNOWN_STR;
		switch (jobStateID.intValue()) {
			case Task.JOBSTATUS_READYFORSENDINGTONODE:
				jobState = Task.JOBSTATUS_READYFORSENDINGTONODE_STR;
				break;
			case Task.JOBSTATUS_INQUEUEATNODE:
				jobState = Task.JOBSTATUS_INQUEUEATNODE_STR;
				break;
			case Task.JOBSTATUS_UNDERPROCESSING:
				jobState = Task.JOBSTATUS_UNDERPROCESSING_STR;
				break;
			case Task.JOBSTATUS_PROCESSINGDONE:
				jobState = Task.JOBSTATUS_PROCESSINGDONE_STR;
				break;
			case Task.JOBSTATUS_PROCESSINGFAILED:
				jobState = Task.JOBSTATUS_PROCESSINGFAILED_STR;
				break;
		}

		waitDialog.updateJobStatus(jobID, jobState, jobCompletionRate);



	}


	/**
	 * This method disconnects client from cluster.
	 */
	public void disconnectFromController() {
		Logger.put("CLIENT: Disconnecting from controller...");
		try {
			myController.disconnectFromController(myClientID);
		} catch(Exception e) {
			Logger.put("CLIENT: FAILED to disconnect from controller.");
			Logger.put(e.toString());
		}
		Logger.put("CLIENT: Disconnected succesfully from controller.");
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

					if (varStr.equals("ControllerHostname")) { controllerHostname = valStr; varsRead++;}
					if (varStr.equals("ControllerPort")) { controllerPort = valStr; varsRead++; }
					if (varStr.equals("ClientPort")) { myPort = valStr; varsRead++; }
					if (varStr.equals("ClientDataRoot")) { myDataRoot = valStr; varsRead++; }

				}

				nextLine = br.readLine();

			}

			br.close();

		} catch (Exception e) {
			Logger.put("CLIENT: Could NOT open/read settings from file " + filename);
			Logger.put(e.toString());
			return false;
		}

		if (varsRead<0) {
			Logger.put("CLIENT: Could NOT find all necessary variables in file " + filename);
			return false;
		} else {
			return true;
		}

	}


	/**
	 * This method returns a file object that points to same file as fullPath
	 * but only relative to dataRootPath
	 */
	private File stripFullFilePath(File filePath, File dataRootPath) {

		if (dataRootPath.equals(new File(new String("")))) { return filePath; }

		// Create canonical presentation of fullPath and myDataRoot
		String filePathCano = null;
		String dataRootPathCano = null;
		try {
			filePathCano = filePath.getCanonicalPath();
			dataRootPathCano = dataRootPath.getCanonicalPath();
		} catch (Exception e) {
			Logger.put("CLIENT: ERROR - Failed to generate canonical path names!");
			Logger.put(e.toString());
		}

		// Make sure filePathCano starts with dataRootPathCano

		if ( !filePathCano.startsWith(dataRootPathCano) ) {
			Logger.put("CLIENT: ERROR - Path doesn't start with root!");
			return null;
		}

		// Remove dataRootPathCano from the beginning of filePathCano
		String strippedFilePath = filePathCano.substring(dataRootPathCano.length(), filePathCano.length());

		// Create File object for strippedFilePath
		File f = new File(strippedFilePath);
		return f;

	}


	/**
	 * This class is used for initiating next part of a batch operation after previous has ended
	 */
	class ContinueBatchThread extends Thread {

		private ClientForCluster clientMain;

		public ContinueBatchThread( ClientForCluster _clientMain) {
			clientMain = _clientMain;
		}

		public void run() {
			clientMain.nextBatchStage();
		}

	}


	/**
	 * This class is used for initiating refresh task after some processing task has ended
	 */
	private class InitiateLateRefreshThread extends Thread {
		private int changeType;
		private int[] rawDataIDs;
		public InitiateLateRefreshThread(int _changeType, int[] _rawDataIDs) {
			changeType = _changeType;
			rawDataIDs = _rawDataIDs;
		}

		public void run() {
			mainWin.startRefreshRawDataVisualizers(changeType, rawDataIDs);
		}
	}




}