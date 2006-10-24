package net.sf.mzmine.methods.alignment.filterbygaps;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

class AlignmentResultFilterByGapsTask implements Task {

	private AlignmentResult originalAlignmentResult;
	private AlignmentResult processedAlignmentResult;
	private AlignmentResultFilterByGapsParameters parameters;
	private TaskStatus status;
	private String errorMessage;
	
	private float processedAlignmentRows;
	private float totalAlignmentRows;
	
	
	public AlignmentResultFilterByGapsTask(AlignmentResult alignmentResult, AlignmentResultFilterByGapsParameters parameters) {
		status = TaskStatus.WAITING;
		this.originalAlignmentResult = alignmentResult;
		this.parameters = parameters;
	}
	
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public float getFinishedPercentage() {
		return processedAlignmentRows / totalAlignmentRows;
	}

	public Object getResult() {
		// TODO Auto-generated method stub
		Object[] result = new Object[3];
		result[0] = originalAlignmentResult;
		result[1] = processedAlignmentResult;
		result[2] = parameters;
		return result;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Filter alignment result by gaps.";
	}

	public void run() {

			status = TaskStatus.PROCESSING;
			
			
			status = TaskStatus.FINISHED;
			
			
			// mainWin = _mainWin;

			/*ClientDialog waitDialog = new ClientDialog(_mainWin);
			waitDialog.setTitle("Filtering alignment result, please wait...");
			waitDialog.addJob(new Integer(1), ar.getNiceName(), "client-side", Task.JOBSTATUS_UNDERPROCESSING_STR, new Double(0));
			waitDialog.showMe();
			waitDialog.paintNow();*/

			// AlignmentResultFilterByGapsParameters params = (AlignmentResultFilterByGapsParameters)_params;

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

	}

}
