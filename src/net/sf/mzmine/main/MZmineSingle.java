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

package net.sf.mzmine.main;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;


public class MZmineSingle {

	private MainWindow guiMain;




	/**
	 * Main method
	 */
	public static void main(String argz[]) {

		Logger.disableOutput();

		int numberOfNodes = 1;

		for (String arg : argz) {
			if ( (arg.compareToIgnoreCase(new String("verbose")))==0 ) {
				Logger.setOutputOnScreen();
			} else if ( arg.toLowerCase().startsWith("numberofnodes=") ) {
				numberOfNodes = Integer.parseInt(arg.substring(arg.toLowerCase().indexOf("numberofnodes=") + 14));
			} else {
				Logger.putFatal("Could not interpret command-line argument: " + arg);
				System.exit(-1);
			}

		}

		new MZmineSingle(numberOfNodes);

	}

	/**
	 * Starts the application
	 */
	public MZmineSingle(int numberOfNodes) {

		Logger.putFatal("MZmine is starting up: Single computer mode with " + numberOfNodes + " computing nodes...");



		// Create RMI registry
		/*
		Logger.put("STARTUP THREAD: Starting RMI registry.");
		RMI removed from Single mode
		try {
			java.rmi.registry.LocateRegistry.createRegistry(java.rmi.registry.Registry.REGISTRY_PORT);
		} catch (Exception e) {
			Logger.putFatal("STARTUP THREAD: FATAL ERROR - Couldn't create RMI registry.");
			Logger.putFatal(e.toString());
			return;
		}


		try { Thread.sleep(2000); }
			catch (Exception e) { Logger.put("STARTUP THREAD: ERROR - Can't get no sleep"); }
		*/

		Logger.put("STARTUP THREAD: Starting node(s)");

		// Start node(s)
		NodeServer[] allNodes = new NodeServer[numberOfNodes];
		for (int nodeNum=0; nodeNum<numberOfNodes; nodeNum++) {

			NodeThread nodeCreatorThread = new NodeThread();
			nodeCreatorThread.start();

			NodeServer onlyNodeServer = null;
			while (onlyNodeServer == null) {
				try { Thread.sleep(100); }
					catch (Exception e) { Logger.put("STARTUP THREAD: ERROR - Can't get no sleep"); }

				onlyNodeServer = nodeCreatorThread.getCreatedNode();
			}

			allNodes[nodeNum] = onlyNodeServer;

		}



		/*
		RMI removed from Single mode
		try { Thread.sleep(2000); }
			catch (Exception e) { Logger.put("STARTUP THREAD: ERROR - Can't get no sleep"); }
		*/

		Logger.put("STARTUP THREAD: Starting controller.");

		// Start controller
		ControllerThread controllerCreatorThread = new ControllerThread(allNodes);
		controllerCreatorThread.start();

		ControllerServer onlyControllerServer = null;
		while (onlyControllerServer == null) {
			try { Thread.sleep(100); }
				catch (Exception e) { Logger.put("STARTUP THREAD: ERROR - Can't get no sleep"); }

			onlyControllerServer = controllerCreatorThread.getCreatedController();
		}

		/*
		RMI removed from Single mode
		try { Thread.sleep(2000); }
			catch (Exception e) { Logger.put("STARTUP THREAD: ERROR - Can't get no sleep"); }
		*/

		Logger.put("STARTUP THREAD: Starting GUI.");

		// Start client
		guiMain = new MainWindow("MZmine", onlyControllerServer);
		guiMain.setVisible(true);
	}


	private class NodeThread extends Thread {

		private NodeServer createdNode = null;

		public void run() {
			createdNode = new NodeServer(true);
		}

		public NodeServer getCreatedNode() { return createdNode; }
	}

	private class ControllerThread extends Thread {
		private NodeServer[] allNodes;
		private ControllerServer createdController = null;

		public ControllerThread(NodeServer[] _allNodes) {
			allNodes = _allNodes;
		}
		public void run() {
			createdController = new ControllerServer(allNodes);
		}

		public ControllerServer getCreatedController() {
			return createdController;
		}
	}

}