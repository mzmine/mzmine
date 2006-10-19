/*
    Copyright 2005-2006 VTT Biotechnology

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

package net.sf.mzmine.methods.alignment.filterbygaps;

import javax.swing.JOptionPane;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


/**
 * This class implements a filter for alignment results
 * Filter removes rows which have less than defined number of peaks detected
 *
 * @version 30 March 2006
 */
public class AlignmentResultFilterByGaps implements Method {

	private MainWindow mainWin;
	
	private AlignmentResultFilterByGapsParameters parameters;


	public boolean askParameters() {

        parameters = new AlignmentResultFilterByGapsParameters();
        parameters.initParameters();

        ParameterSetupDialog dialog = new ParameterSetupDialog(		
        				MainWindow.getInstance(),
        				"Please check parameter values for Join Aligner",
        				parameters
        		);
        dialog.setVisible(true);
        
		if (dialog.getExitCode()==-1) return false;

		return true;
        
	}

	public AlignmentResult processAlignment(MainWindow _mainWin, AlignmentResult ar, AlignmentResultFilterByGapsParameters _params) {

		mainWin = _mainWin;

		/*ClientDialog waitDialog = new ClientDialog(_mainWin);
		waitDialog.setTitle("Filtering alignment result, please wait...");
		waitDialog.addJob(new Integer(1), ar.getNiceName(), "client-side", Task.JOBSTATUS_UNDERPROCESSING_STR, new Double(0));
		waitDialog.showMe();
		waitDialog.paintNow();*/

		AlignmentResultFilterByGapsParameters params = (AlignmentResultFilterByGapsParameters)_params;

		// Count number of rows to drop
		/*int rowsFine = 0;
		Vector<Integer> fineRowInds = new Vector<Integer>();

		for (int rowInd=0; rowInd<ar.getNumOfRows(); rowInd++) {

			int numDetectedOnRow = 0;
			for (int colGroupInd=0; colGroupInd<ar.getNumOfRawDatas(); colGroupInd++) {
				int rawDataID = ar.getRawDataID(colGroupInd);
				if (ar.getPeakStatus(rawDataID, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) { numDetectedOnRow++; }
			}

			if (numDetectedOnRow>=params.paramRequiredNumOfPresent) {
				rowsFine++;
				fineRowInds.add(new Integer(rowInd));
			}

		}


		// Create datastructures for constructing new alignment result
		Vector<Integer> newRawDataIDs = new Vector<Integer>();
		boolean[] newStandardCompounds = null;
		int[] newIsotopePatternIDs = null;
		int[] newIsotopePeakNumbers = null;
		int[] newChargeStates = null;


		Hashtable<Integer, int[]> newPeakIDs = new Hashtable<Integer, int[]>();
		Hashtable<Integer, double[]> newPeakMZs = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> newPeakRTs = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> newPeakHeights = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> newPeakAreas = new Hashtable<Integer, double[]>();
		Hashtable<Integer, int[]> newPeakStatuses = new Hashtable<Integer, int[]>();

		//AlignmentResult nar = new AlignmentResult();

		// Copy peak statuses, ids, mzs, rts, heights and areas

		int[] originalRawDataIDs = ar.getRawDataIDs();

		for (int originalRawDataID : originalRawDataIDs) {
			Integer orgRawDataID = new Integer(originalRawDataID);

			// Fetch old arrays
			boolean[] orgStdFlags = ar.getStandardCompoundFlags();
			int[] orgIsotopePatternIDs = ar.getIsotopePatternIDs();
			int[] orgIsotopePeakNumbers = ar.getIsotopePeakNumbers();
			int[] orgChargeStates = ar.getChargeStates();

			int[] orgIDs = ar.getPeakIDs(orgRawDataID);
			double[] orgMZs = ar.getPeakMZs(orgRawDataID);
			double[] orgRTs = ar.getPeakRTs(orgRawDataID);
			double[] orgHeights = ar.getPeakHeights(orgRawDataID);
			double[] orgAreas = ar.getPeakAreas(orgRawDataID);
			int[] orgStatuses = ar.getPeakStatuses(orgRawDataID);


			// Create new arrays
			boolean[] newStdFlags = new boolean[rowsFine];
			int[] newIsoPatternIDs = new int[rowsFine];
			int[] newIsoPeakNumbers = new int[rowsFine];
			int[] newCharges = new int[rowsFine];

			int[] newIDs = new int[rowsFine];
			double[] newMZs = new double[rowsFine];
			double[] newRTs = new double[rowsFine];
			double[] newHeights = new double[rowsFine];
			double[] newAreas = new double[rowsFine];
			int[] newStatuses = new int[rowsFine];


			// Copy array contents from old to new
			int targetRowInd=0;
			for (int sourceRowInd=0; sourceRowInd<ar.getNumOfRows(); sourceRowInd++) {
				if (fineRowInds.indexOf(new Integer(sourceRowInd))>=0) {

					newStdFlags[targetRowInd] = orgStdFlags[sourceRowInd];
					newIsoPatternIDs[targetRowInd] = orgIsotopePatternIDs[sourceRowInd];
					newIsoPeakNumbers[targetRowInd] = orgIsotopePeakNumbers[sourceRowInd];
					newCharges[targetRowInd] = orgChargeStates[sourceRowInd];

					newIDs[targetRowInd] = orgIDs[sourceRowInd];
					newMZs[targetRowInd] = orgMZs[sourceRowInd];
					newRTs[targetRowInd] = orgRTs[sourceRowInd];
					newHeights[targetRowInd] = orgHeights[sourceRowInd];
					newAreas[targetRowInd] = orgAreas[sourceRowInd];
					newStatuses[targetRowInd] = orgStatuses[sourceRowInd];

					targetRowInd++;
				}
			}

			// Add values to new alignment result
			Integer tmpID = new Integer(orgRawDataID);
			newRawDataIDs.add(tmpID);

			newStandardCompounds = newStdFlags;
			newIsotopePatternIDs = newIsoPatternIDs;
			newIsotopePeakNumbers = newIsoPeakNumbers;
			newChargeStates = newCharges;

			newPeakIDs.put(tmpID, newIDs);
			newPeakMZs.put(tmpID, newMZs);
			newPeakRTs.put(tmpID, newRTs);
			newPeakHeights.put(tmpID, newHeights);
			newPeakAreas.put(tmpID, newAreas);
			newPeakStatuses.put(tmpID, newStatuses);

		}


		AlignmentResult nar = new AlignmentResult(	newRawDataIDs,
													newStandardCompounds,
													newIsotopePatternIDs,
													newIsotopePeakNumbers,
													newChargeStates,
													newPeakStatuses,
													newPeakIDs,
													newPeakMZs,
													newPeakRTs,
													newPeakHeights,
													newPeakAreas,
													new String("Results from " + ar.getNiceName() + " filtered by number of detections.")
												);

		
		return nar;
        */
        return null;

	}


    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters, OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }

}