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


package net.sf.mzmine.methods.alignment;

import javax.swing.JOptionPane;

import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.obsoletedistributionframework.Task;
import net.sf.mzmine.userinterface.ClientDialog;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.userinterface.Statusbar;
import net.sf.mzmine.util.GeneralParameters;


/**
 *
 */
public class LinearNormalizer implements Normalizer {

  ///////////////////////////////////////
  // operations

	private MainWindow mainWin;




	private String NORMALIZATION_AVERAGEINT_STR = "Average intensity";
	private String NORMALIZATION_AVERAGESQUAREINT_STR = "Average squared intensity";
	private String NORMALIZATION_MAXPEAK_STR = "Maximum peak intensity";
	private String NORMALIZATION_TOTRAWSIGNAL_STR = "Total raw signal";


	public LinearNormalizerParameters askParameters(MainWindow _mainWin, LinearNormalizerParameters currentValues) {

		mainWin = _mainWin;

		LinearNormalizerParameters myParameters;
		if (currentValues==null) {
			myParameters = new LinearNormalizerParameters();
		} else {
			myParameters = currentValues;
		}

		Statusbar statBar = _mainWin.getStatusBar();

		// Set labels depending on peak heights / areas setting
		if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
			NORMALIZATION_AVERAGEINT_STR = "Average peak height";
			NORMALIZATION_AVERAGESQUAREINT_STR = "Average squared peak height";
			NORMALIZATION_MAXPEAK_STR = "Maximum peak height";
			NORMALIZATION_TOTRAWSIGNAL_STR = "Total raw signal";
		}
		if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
			NORMALIZATION_AVERAGEINT_STR = "Average peak area";
			NORMALIZATION_AVERAGESQUAREINT_STR = "Average squared peak area";
			NORMALIZATION_MAXPEAK_STR = "Maximum peak area";
			NORMALIZATION_TOTRAWSIGNAL_STR = "Total raw signal";
		}

		// Array of possible dialog values
		Object[] possibleValues = {		NORMALIZATION_AVERAGEINT_STR,
										NORMALIZATION_AVERAGESQUAREINT_STR,
										NORMALIZATION_MAXPEAK_STR,
										NORMALIZATION_TOTRAWSIGNAL_STR };

		// Set preselected value for the dialog
		Object preSelectedValue = possibleValues[0];
		if ( myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGEINT ) { preSelectedValue = possibleValues[0]; }
		if ( myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGESQUAREINT ) { preSelectedValue = possibleValues[1]; }
		if ( myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_MAXPEAK ) { preSelectedValue = possibleValues[2]; }
		if ( myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL ) { preSelectedValue = possibleValues[3]; }

		// Show dialog
		Object selectedValue = JOptionPane.showInputDialog(null,
															"Do linear normalization by...",
															"Linear normalization",
															JOptionPane.INFORMATION_MESSAGE,
															null,
															possibleValues, preSelectedValue);

		if (selectedValue == possibleValues[0]) { myParameters.paramNormalizationType = LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGEINT;	}
		if (selectedValue == possibleValues[1]) { myParameters.paramNormalizationType = LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGESQUAREINT;	}
		if (selectedValue == possibleValues[2]) { myParameters.paramNormalizationType = LinearNormalizerParameters.NORMALIZATIONTYPE_MAXPEAK;	}
		if (selectedValue == possibleValues[3]) { myParameters.paramNormalizationType = LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL;	}
		if (selectedValue == null) {
			statBar.setStatusText("Normalization cancelled.");
			return null;
		}

		return myParameters;
	}



    public AlignmentResult calcNormalization(MainWindow _mainWin, AlignmentResult ar, NormalizerParameters _myParameters) {

		mainWin = _mainWin;

		ClientDialog waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Normalizing, please wait...");
		Integer jobID = new Integer(1);
		waitDialog.addJob(jobID, ar.getNiceName(), "client-side", Task.JOBSTATUS_UNDERPROCESSING_STR, new Double(0));
		waitDialog.showMe();
		waitDialog.paintNow();

		LinearNormalizerParameters myParameters = (LinearNormalizerParameters)_myParameters;

		int numOfRawDatas = ar.getNumOfRawDatas();

		//RawDataAtClient r;
		int rawDataID;

		int[] peakStatuses;

		double[] rawPeakHeights;
		double[] rawPeakAreas;
		double normFactor;
		double relativeFactor = 0;

		AlignmentResult nar = null;
		String desc = new String("Unknown normalization type!");

		if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGEINT) { desc = new String("Results from " + ar.getNiceName() + " normalized by " + NORMALIZATION_AVERAGEINT_STR); }
		if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGESQUAREINT) { desc = new String("Results from " + ar.getNiceName() + " normalized by " + NORMALIZATION_AVERAGESQUAREINT_STR); }
		if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_MAXPEAK) { desc = new String("Results from " + ar.getNiceName() + " normalized by " + NORMALIZATION_MAXPEAK_STR); }
		if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL) { desc = new String("Results from " + ar.getNiceName() + " normalized by " + NORMALIZATION_TOTRAWSIGNAL_STR); }

		// nar = new AlignmentResult(numOfRawDatas, desc);
		nar = new AlignmentResult(ar, desc);


		// Loop through all control runs
		for (int i=0; i<numOfRawDatas; i++) {

			if (waitDialog!=null) {
				waitDialog.updateJobStatus(jobID, Task.JOBSTATUS_UNDERPROCESSING_STR, new Double((double)(i+1)/(double)(numOfRawDatas)));
				//waitDialog.paintNow();
			}

			// Get this control run and raw peak heights and areas
			//r = ar.getRun(i);
			rawDataID = ar.getRawDataID(i);
			peakStatuses = ar.getPeakStatuses(rawDataID);
			rawPeakHeights = ar.getPeakHeights(rawDataID);
			rawPeakAreas = ar.getPeakAreas(rawDataID);

 			// Calculate normalization factor
 			normFactor = 1;


			//
 			if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGEINT) {

				// Calc average over all peak heights and areas for this run
				double sumOfPeakMeasures = 0;
				int numOfPeakMeasures = 0;

				for (int pi=0; pi<rawPeakHeights.length; pi++) {

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						if ((peakStatuses[pi]==AlignmentResult.PEAKSTATUS_DETECTED) ||
							(peakStatuses[pi]==AlignmentResult.PEAKSTATUS_ESTIMATED)) {
							sumOfPeakMeasures += rawPeakHeights[pi];
							numOfPeakMeasures++;
						}
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						if (peakStatuses[pi]==AlignmentResult.PEAKSTATUS_DETECTED) {
							sumOfPeakMeasures += rawPeakAreas[pi];
							numOfPeakMeasures++;
						}
					}
				}
				normFactor = sumOfPeakMeasures / ((double)numOfPeakMeasures);
			}



			//
 			if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_AVERAGESQUAREINT) {

				// Calc average over all peak intensities for this run
				double sumOfPeakMeasures = 0;
				int numOfPeakMeasures = 0;

				for (int pi=0; pi<rawPeakHeights.length; pi++) {

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						if ((peakStatuses[pi]==AlignmentResult.PEAKSTATUS_DETECTED) ||
							(peakStatuses[pi]==AlignmentResult.PEAKSTATUS_ESTIMATED)) {
							sumOfPeakMeasures += (rawPeakHeights[pi]*rawPeakHeights[pi]);
							numOfPeakMeasures++;
						}
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						if (peakStatuses[pi]==AlignmentResult.PEAKSTATUS_DETECTED) {
							sumOfPeakMeasures += (rawPeakAreas[pi]*rawPeakAreas[pi]);
							numOfPeakMeasures++;
						}
					}
				}

				normFactor = (double)java.lang.Math.sqrt(sumOfPeakMeasures / (double)numOfPeakMeasures);
			}



			//
			if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_MAXPEAK) {
				double maxVal = 1;

				for (int pi=0; pi<rawPeakHeights.length; pi++) {
					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						if (maxVal<rawPeakHeights[pi]) { maxVal = rawPeakHeights[pi]; }
					}
					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						if (maxVal<rawPeakAreas[pi]) { maxVal = rawPeakAreas[pi]; }
					}
				}
				normFactor = maxVal;
			}


			//
			if (myParameters.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL) {
				rawDataID = ar.getRawDataID(i);
				RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);
				normFactor = rawData.getTotalRawSignal();
			}



			// Calculate normalized intensities
			double[] normPeakHeights = new double[rawPeakHeights.length];
			double[] normPeakAreas = new double[rawPeakAreas.length];

			for (int pi=0; pi<rawPeakHeights.length; pi++) {
				normPeakHeights[pi] = rawPeakHeights[pi] / normFactor;
				normPeakAreas[pi] = rawPeakAreas[pi] / normFactor;
			}

			// Assign relativeFactor a value if this is the first control run
			// Idea of this "relativeFactor" is to normalize first control runs maximum peak height to 100000 and rest of the runs correspondingly.
			double maxVal = 0;
			if (i==0) {
				for (int pi=0; pi<normPeakHeights.length; pi++) {
					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						if (maxVal<normPeakHeights[pi]) { maxVal = normPeakHeights[pi]; }
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						if (maxVal<normPeakAreas[pi]) { maxVal = normPeakAreas[pi]; }
					}
				}
				relativeFactor = 100000 / maxVal;
			}

			// Do additional normalization with relativeFactor (this factor is same for all runs)
			for (int pi=0; pi<normPeakHeights.length; pi++) {
				normPeakHeights[pi] = relativeFactor * normPeakHeights[pi];
				normPeakAreas[pi] = relativeFactor * normPeakAreas[pi];
			}

			// Set normalized heights and areas to the new alignment result object
			nar.setPeakHeights(rawDataID, normPeakHeights);
			nar.setPeakAreas(rawDataID, normPeakAreas);

		}

		waitDialog.hideMe();

		return nar;
	}

}
