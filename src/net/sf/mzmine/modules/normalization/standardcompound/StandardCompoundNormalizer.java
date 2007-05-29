/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.normalization.standardcompound;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;



/**
 *
 */
public class StandardCompoundNormalizer implements MZmineModule {

  ///////////////////////////////////////
  // operations




    /*

	/**
	 * This is the distance function used in measuring distance between two peaks
	private double calcDistance(double mz1, double rt1, double mz2, double rt2, double k) {
		return (java.lang.Math.abs(mz1-mz2)*k+java.lang.Math.abs(rt1-rt2));
	}


	/**
	 * This method does normalization on given alignment result object and returns a new version with normalized peak heights and areas
    public AlignmentResult calcNormalization(MainWindow _mainWin, AlignmentResult ar, NormalizerParameters _myParameters) {

		ClientDialog waitDialog = new ClientDialog(mainWin);
		waitDialog.setTitle("Normalizing, please wait...");
		waitDialog.addJob(new Integer(1), ar.getNiceName(), "client-side", Task.JOBSTATUS_UNDERPROCESSING_STR, new Double(0));
		waitDialog.showMe();
		waitDialog.paintNow();

		StandardCompoundNormalizerParameters myParameters = (StandardCompoundNormalizerParameters)_myParameters;

		int numOfRawDatas = ar.getNumOfRawDatas();

		RawDataAtClient r;
		int rawDataID;

		double[] rawPeakHeights;
		double[] normPeakHeights;

		double[] rawPeakAreas;
		double[] normPeakAreas;

		double peakMZ;
		double peakRT;

		double[] stdCompNormFactors;
		double[] stdCompMZ;
		double[] stdCompRT;
		double[] stdCompDistances;
		boolean[] stdFlags;
		int stdCompInd;

		double currentRunStdMeasurement;
		double firstRunStdMeasurement;
		double currentVsFirstRatio;

		double normFactor;
		double relativeFactor = 0;

		// Check that there are at least some standard compounds defined in this alignment result
		if (ar.getNumOfStandardCompounds()==0) {
			waitDialog.hideMe();
			return null;
		}


		// Construct title for the new normalized alignment result
		String desc = new String("");
		desc = desc + "Results from " + ar.getNiceName() + " ";

		if (myParameters.paramNormalizationType == StandardCompoundNormalizerParameters.NORMALIZATIONTYPE_STDCOMPOUND_NEAREST) {
			//desc = new String("Results from " + ar.getNiceName() + " normalized by nearest standard compound method.");
			desc = desc + "normalized by nearest standard compound method ";
		}
		if (myParameters.paramNormalizationType == StandardCompoundNormalizerParameters.NORMALIZATIONTYPE_STDCOMPOUND_WEIGHTED) {
			//desc = new String("Results from " + ar.getNiceName() + " normalized by weighted stadard compounds method.");
			desc = desc + "normalized by weighted standard compound method ";
		}

		if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
			desc = desc + "using peak heights.";
		}
		if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
			desc = desc + "using peak areas.";
		}

		// Initialize a new alignment result for storing the normalized version of ar
		AlignmentResult nar = new AlignmentResult(ar, desc);


		// Initialize variables for storing information about standard compounds
		int numOfStds = ar.getNumOfStandardCompounds();
		stdCompNormFactors = new double[numOfStds];
		stdCompMZ = new double[numOfStds];
		stdCompRT = new double[numOfStds];
		stdCompDistances = new double[numOfStds];
		stdFlags = ar.getStandardCompoundFlags();
		stdCompInd = 0;


		// Loop through all alignment columns, and normalize each column against first column
		int firstColRawDataID = ar.getRawDataID(0);

		for (int rawDataCount=0; rawDataCount<numOfRawDatas; rawDataCount++) {

			if (waitDialog!=null) {
				waitDialog.updateJobStatus(new Integer(1), Task.JOBSTATUS_UNDERPROCESSING_STR, new Double((double)(rawDataCount+1)/(double)(numOfRawDatas)));
				waitDialog.paintNow();
			}


			int currentColRawDataID = ar.getRawDataID(rawDataCount);

			// Calculate normalization factors between this run and the first run for all checked standard compounds
			stdCompInd = 0;
			for (int rowInd=0; rowInd<stdFlags.length; rowInd++) {

				// If this is a standard compound peak, then check that it has necessary data for calculating normalization factor
				if (stdFlags[rowInd]==true) {

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						// Standard compounds must be "found" or "estimated" in both runs, so that height ratio can be determined
						if ( (ar.getPeakStatus(firstColRawDataID, rowInd)==AlignmentResult.PEAKSTATUS_NOTFOUND) ||
							 (ar.getPeakStatus(currentColRawDataID, rowInd)==AlignmentResult.PEAKSTATUS_NOTFOUND) ) {

							waitDialog.hideMe();
							return null;
						}
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						// Standard compounds must be "found" in both runs, so that area ratio can be determined
						if ( (ar.getPeakStatus(firstColRawDataID, rowInd)!=AlignmentResult.PEAKSTATUS_DETECTED) ||
							 (ar.getPeakStatus(currentColRawDataID, rowInd)!=AlignmentResult.PEAKSTATUS_DETECTED) ) {
							waitDialog.hideMe();
							return null;
						}
					}

					// Store mz and rt for this standard compound
					stdCompMZ[stdCompInd] = ar.getAverageMZ(rowInd);
					stdCompRT[stdCompInd] = ar.getAverageRT(rowInd);
					currentVsFirstRatio = 1;

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						// Calculate ratio of heights for this standard compound
						currentRunStdMeasurement = (double)ar.getPeakHeight(currentColRawDataID, rowInd);
						firstRunStdMeasurement = (double)ar.getPeakHeight(firstColRawDataID, rowInd);
						currentVsFirstRatio = currentRunStdMeasurement / firstRunStdMeasurement;
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						// Calculate ratio of areas for this standard compound
						currentRunStdMeasurement = (double)ar.getPeakArea(currentColRawDataID, rowInd);
						firstRunStdMeasurement = (double)ar.getPeakArea(firstColRawDataID, rowInd);
						currentVsFirstRatio = currentRunStdMeasurement / firstRunStdMeasurement;
					}

					// Store normalization factor for this standard compound
					stdCompNormFactors[stdCompInd] = currentVsFirstRatio;
					stdCompInd++;
				}

			}


			// Get raw peak heights and areas in current column

			rawPeakHeights = ar.getPeakHeights(currentColRawDataID);
			normPeakHeights = new double[rawPeakHeights.length];

			rawPeakAreas = ar.getPeakAreas(currentColRawDataID);
			normPeakAreas = new double[rawPeakAreas.length];

			normFactor = 0;

 			if (myParameters.paramNormalizationType == StandardCompoundNormalizerParameters.NORMALIZATIONTYPE_STDCOMPOUND_NEAREST) {

				// Loop through all peak intensities for this run
				for (int rowInd=0; rowInd<rawPeakHeights.length; rowInd++) {
					if (ar.getPeakStatus(currentColRawDataID, rowInd)!=AlignmentResult.PEAKSTATUS_NOTFOUND) {
						peakMZ = ar.getPeakMZ(currentColRawDataID, rowInd);
						peakRT = ar.getPeakRT(currentColRawDataID, rowInd);

						// Search for nearest standard compound and use its normalization factor
						int minInd = 0;
						double minDistance = Double.MAX_VALUE;
						double thisDistance;
						for (stdCompInd=0; stdCompInd<stdCompMZ.length; stdCompInd++) {
							thisDistance = calcDistance(peakMZ, peakRT, stdCompMZ[stdCompInd], stdCompRT[stdCompInd], myParameters.paramMZvsRTBalance);
							if (thisDistance<=minDistance) {
								minInd = stdCompInd;
								minDistance = thisDistance;
							}
						}
						normFactor = stdCompNormFactors[minInd];

						// Calculate normalized height and area
						normPeakHeights[rowInd] = rawPeakHeights[rowInd] / normFactor;
						normPeakAreas[rowInd] = rawPeakAreas[rowInd] / normFactor;
					} else {
						// Do not try to normalize an empty peak
						normPeakHeights[rowInd] = rawPeakHeights[rowInd];
						normPeakAreas[rowInd] = rawPeakAreas[rowInd];
					}
				}
			}

			if (myParameters.paramNormalizationType == StandardCompoundNormalizerParameters.NORMALIZATIONTYPE_STDCOMPOUND_WEIGHTED) {

				// Loop through all peak intensities for this run
				for (int rowInd=0; rowInd<rawPeakHeights.length; rowInd++) {
					if (ar.getPeakStatus(currentColRawDataID, rowInd)!=AlignmentResult.PEAKSTATUS_NOTFOUND) {
						peakMZ = ar.getPeakMZ(currentColRawDataID, rowInd);
						peakRT = ar.getPeakRT(currentColRawDataID, rowInd);

						// Measure distances from each standard compound to this peak
						double[] stdDistances = new double[stdCompMZ.length];
						double[] stdWeights = new double[stdCompMZ.length];
						for (stdCompInd=0; stdCompInd<stdCompMZ.length; stdCompInd++) {
							stdDistances[stdCompInd] = calcDistance(peakMZ, peakRT, stdCompMZ[stdCompInd], stdCompRT[stdCompInd], myParameters.paramMZvsRTBalance);
							if (stdDistances[stdCompInd]>0) {
								stdWeights[stdCompInd] = 1 / (stdDistances[stdCompInd]);
							} else {
								for (int stdCompIndtmp=0; stdCompIndtmp<stdCompMZ.length; stdCompIndtmp++) {
									stdWeights[stdCompIndtmp] = 0;
								}
								stdWeights[stdCompInd] = 1;
								break;
							}
						}

						// Calc sum of weights
						double sumOfWeights=0;
						for (stdCompInd=0; stdCompInd<stdCompMZ.length; stdCompInd++) {
							sumOfWeights += stdWeights[stdCompInd];
						}

						// Calculate custom normalization factor as weighted average of all standard compound normalization factors
						normFactor = 0;

						for (stdCompInd=0; stdCompInd<stdCompMZ.length; stdCompInd++) {
							double tmpAdd = stdWeights[stdCompInd] * stdCompNormFactors[stdCompInd] / sumOfWeights;

							normFactor += tmpAdd;
						}

						// Calculate normalized intensity
						normPeakHeights[rowInd] = rawPeakHeights[rowInd] / normFactor;
						normPeakAreas[rowInd] = rawPeakAreas[rowInd] / normFactor;
					} else {
						// Do not try to normalize an empty peak
						normPeakHeights[rowInd] = rawPeakHeights[rowInd];
						normPeakAreas[rowInd] = rawPeakAreas[rowInd];
					}
				}
			}


			// Assign relativeFactor a value if this is the first control run
			// Idea of this "relativeFactor" is to normalize first control runs maximum peak height to 100000 and rest of the runs correspondingly.
			double maxVal = 0;
			if (rawDataCount==0) {
				for (int rowInd=0; rowInd<normPeakHeights.length; rowInd++) {
					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						if (maxVal<normPeakHeights[rowInd]) { maxVal = normPeakHeights[rowInd]; }
					}
					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						if (maxVal<normPeakAreas[rowInd]) { maxVal = normPeakAreas[rowInd]; }
					}
				}
				relativeFactor = 100000 / maxVal;
			}

			// Do additional normalization with relativeFactor (this factor is same for all runs)
			for (int rowInd=0; rowInd<normPeakHeights.length; rowInd++) {
				normPeakHeights[rowInd] = relativeFactor * normPeakHeights[rowInd];
				normPeakAreas[rowInd] = relativeFactor * normPeakAreas[rowInd];
			}

			nar.setPeakHeights(currentColRawDataID, normPeakHeights);
			nar.setPeakAreas(currentColRawDataID, normPeakAreas);

		}

		waitDialog.hideMe();

		return nar;

	}

   */


	


    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub
        
    }


}
