/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.methods.alignment.join;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Comparator;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

import net.sf.mzmine.util.IsotopePatternUtils;
import net.sf.mzmine.util.MathUtils;

import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimpleAlignmentResult;
import net.sf.mzmine.data.impl.SimpleAlignmentResultRow;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.data.impl.StandardCompoundFlag;


/**
 *
 */
class JoinAlignerTask implements Task {

    private OpenedRawDataFile[] dataFiles;
    
    private TaskStatus status;
    private String errorMessage;

	private float processedPercentage;

    private SimpleAlignmentResult alignmentResult;
    
    private JoinAlignerParameters parameters;
    private double MZTolerance;
    private double MZvsRTBalance;
    private boolean RTToleranceUseAbs;
    private double RTToleranceValueAbs;
    private double RTToleranceValuePercent;
    

    /**
     * @param rawDataFile
     * @param parameters
     */
    JoinAlignerTask(OpenedRawDataFile[] dataFiles,
            JoinAlignerParameters parameters) {

        status = TaskStatus.WAITING;
        this.dataFiles = dataFiles;
        this.parameters = parameters;
        
		// Get parameter values for easier use
        MZTolerance = (Double) parameters.getParameterValue(JoinAlignerParameters.MZTolerance);
        MZvsRTBalance = (Double) parameters.getParameterValue(JoinAlignerParameters.MZvsRTBalance);
        
        if (parameters.getParameterValue(JoinAlignerParameters.RTToleranceType) == JoinAlignerParameters.RTToleranceTypeAbsolute) RTToleranceUseAbs = true; else RTToleranceUseAbs = false;  
        RTToleranceValueAbs = (Double) parameters.getParameterValue(JoinAlignerParameters.RTToleranceValueAbs);
        RTToleranceValuePercent = (Double) parameters.getParameterValue(JoinAlignerParameters.RTToleranceValuePercent);
        
       
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Join aligner, " + dataFiles.length + " peak lists.";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
		return processedPercentage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        Object[] results = new Object[2];
        results[0] = alignmentResult;
        results[1] = parameters;
        return results;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

		/*
		 * Initialize master isotope list and isotope pattern utility vector
		 */
		Vector<MasterIsotopeListRow> masterIsotopeListRows = new Vector<MasterIsotopeListRow>();
		Hashtable<OpenedRawDataFile, IsotopePatternUtils> isotopePatternUtils = new Hashtable<OpenedRawDataFile, IsotopePatternUtils>();


		/*
		 * Loop through all data files
		 */
		for (OpenedRawDataFile dataFile : dataFiles) {

			if (status == TaskStatus.CANCELED) return;

			/*
			 * Pickup peak list for this file and generate list of isotope patterns
			 */
			PeakList peakList = (PeakList)dataFile.getCurrentFile().getLastData(PeakList.class);
			IsotopePatternUtils isoUtil = new IsotopePatternUtils(peakList);
			isotopePatternUtils.put(dataFile, isoUtil);
			IsotopePattern[] isotopePatternList = isoUtil.getAllIsotopePatterns();
			IsotopePatternWrapper[] wrappedIsotopePatternList = new IsotopePatternWrapper[isotopePatternList.length];
			for (int i=0; i<isotopePatternList.length; i++)
				wrappedIsotopePatternList[i] = new IsotopePatternWrapper(isotopePatternList[i]);
	
				
			
			/*
			 * Calculate scores between all pairs of isotope pattern and master isotope list row
			 */

			// Reset score tree
			TreeSet<PatternVsRowScore> scoreTree = new TreeSet<PatternVsRowScore>(new ScoreOrderer());

			for (IsotopePatternWrapper wrappedIsotopePattern : wrappedIsotopePatternList) {
				for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {
					PatternVsRowScore score = new PatternVsRowScore(masterIsotopeListRow, wrappedIsotopePattern, isoUtil);
					if (score.isGoodEnough()) scoreTree.add(score);
				}
			}

			/*
			 * Browse scores in order of descending goodness-of-fit
			 */

			Iterator<PatternVsRowScore> scoreIter = scoreTree.iterator();
			while (scoreIter.hasNext()) {
				PatternVsRowScore score = scoreIter.next();

				MasterIsotopeListRow masterIsotopeListRow = score.getMasterIsotopeListRow();
				IsotopePatternWrapper wrappedIsotopePattern = score.getWrappedIsotopePattern();

				// Check if master list row is already assigned with an isotope pattern (from this rawDataID)
				if (masterIsotopeListRow.isAlreadyJoined()) continue;

				// Check if isotope pattern is already assigned to some master isotope list row
				if (wrappedIsotopePattern.isAlreadyJoined()) continue;
				
				// Assign isotope pattern to master peak list row
				masterIsotopeListRow.addIsotopePattern(dataFile, wrappedIsotopePattern.getIsotopePattern(), isoUtil);

				// Mark pattern and isotope pattern row as joined
				masterIsotopeListRow.setJoined(true);
				wrappedIsotopePattern.setAlreadyJoined(true);

				processedPercentage += 1.0f / (float)dataFiles.length / (float)scoreTree.size();

			}

			/*
			 * Remove 'joined' from all master isotope list rows
			 */
			 for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {
				 masterIsotopeListRow.setJoined(false);
			 }


			/*
			 * Add remaining isotope patterns as new rows to master isotope list
			 */
			for (IsotopePatternWrapper wrappedIsotopePattern : wrappedIsotopePatternList) {
				if (wrappedIsotopePattern.isAlreadyJoined()) continue;

				MasterIsotopeListRow masterIsotopeListRow = new MasterIsotopeListRow();
				masterIsotopeListRow.addIsotopePattern(dataFile, wrappedIsotopePattern.getIsotopePattern(), isoUtil);
				masterIsotopeListRows.add(masterIsotopeListRow);
			}

		}

		/*
		 * Convert master isotope list to alignment result (master peak list)
		 */

		// Get number of peak rows
		int numberOfRows = 0;
		for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) { numberOfRows += masterIsotopeListRow.getNumberOfPeaksOnRow(); }

		alignmentResult = new SimpleAlignmentResult("Result from Join Aligner");

		// Add openedrawdatafiles to alignment result
		for (OpenedRawDataFile dataFile : dataFiles) alignmentResult.addOpenedRawDataFile(dataFile);

		// Loop through master isotope list rows
		for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {

			SimpleIsotopePattern masterIsotopePattern = new SimpleIsotopePattern(masterIsotopeListRow.getChargeState());

			// Loop through peaks on this master isotope list row
			for (int peakRow=0; peakRow<masterIsotopeListRow.getNumberOfPeaksOnRow(); peakRow++) {

				// Create alignment result row
				SimpleAlignmentResultRow alignmentRow = new SimpleAlignmentResultRow();

				// Tag row with isotope pattern
				//alignmentRow.setIsotopePattern(masterIsotopePattern);
				alignmentRow.addData(IsotopePattern.class, masterIsotopePattern);

				// Loop through raw data files
				for (OpenedRawDataFile dataFile : dataFiles) {

					IsotopePattern isotopePattern = masterIsotopeListRow.getIsotopePattern(dataFile);
					if (isotopePattern==null) continue;
					IsotopePatternUtils isoUtil = isotopePatternUtils.get(dataFile);

					// Add peak to alignment row
					Peak[] isotopePeaks = isoUtil.getPeaksInPattern(isotopePattern);
					if (peakRow<isotopePeaks.length) {
						alignmentRow.addPeak(dataFile, isotopePeaks[peakRow]);
						if (isotopePeaks[peakRow].hasData(StandardCompoundFlag.class)) {
							if (!alignmentRow.hasData(StandardCompoundFlag.class)) {
								alignmentRow.addData(StandardCompoundFlag.class, new StandardCompoundFlag());
							}
						}
					}
				}

				alignmentResult.addRow(alignmentRow);

			}

		}
        status = TaskStatus.FINISHED;

    }





	/**
	 * This class represent one row of the master isotope list
	 */
	private class MasterIsotopeListRow {

		private Hashtable<OpenedRawDataFile, IsotopePattern> isotopePatterns;

		private double monoMZ;
		private double monoRT;

		private int chargeState;

		private Vector<Double> mzVals;
		private Vector<Double> rtVals;

		private boolean alreadyJoined = false;

		private int numberOfPeaksOnRow;

		public MasterIsotopeListRow() {
			isotopePatterns = new Hashtable<OpenedRawDataFile, IsotopePattern>();
			mzVals = new Vector<Double>();
			rtVals = new Vector<Double>();
		}

		public void addIsotopePattern(OpenedRawDataFile dataFile, IsotopePattern isotopePattern, IsotopePatternUtils util) {

			isotopePatterns.put(dataFile, isotopePattern);

			// Get monoisotopic peak
			Peak[] peaks = util.getPeaksInPattern(isotopePattern);
			Peak monoPeak = peaks[0];

			if (numberOfPeaksOnRow<peaks.length) numberOfPeaksOnRow = peaks.length;

			// Add M/Z and RT
			mzVals.add(monoPeak.getNormalizedMZ());
			rtVals.add(monoPeak.getNormalizedRT());

			// Update medians
			Double[] mzValsArray = mzVals.toArray(new Double[0]);
			double[] mzValsArrayN = new double[mzValsArray.length];
			for (int i=0; i<mzValsArray.length; i++)
				mzValsArrayN[i] = mzValsArray[i];
			monoMZ = MathUtils.calcQuantile(mzValsArrayN, 0.5);

			Double[] rtValsArray = rtVals.toArray(new Double[0]);
			double[] rtValsArrayN = new double[rtValsArray.length];
			for (int i=0; i<rtValsArray.length; i++)
				rtValsArrayN[i] = rtValsArray[i];
			monoRT = MathUtils.calcQuantile(rtValsArrayN, 0.5);


			// Set charge state
			chargeState = isotopePattern.getChargeState();

		}


		public double getMonoisotopicMZ() { return monoMZ; }

		public double getMonoisotopicRT() { return monoRT; }

		public int getChargeState() { return chargeState; }

		public int getNumberOfPeaksOnRow() { return numberOfPeaksOnRow; }

		public IsotopePattern getIsotopePattern(OpenedRawDataFile dataFile) {
			return isotopePatterns.get(dataFile);
		}

		public void setJoined(boolean b) { alreadyJoined = b; }

		public boolean isAlreadyJoined() { return alreadyJoined; }

	}



	/**
	 * This class represents a score between master peak list row and isotope pattern
	 */
	private class PatternVsRowScore {

		MasterIsotopeListRow masterIsotopeListRow;
		IsotopePatternWrapper wrappedIsotopePattern;
		double score = Double.MAX_VALUE;
		boolean goodEnough = false;
		
		public PatternVsRowScore(MasterIsotopeListRow masterIsotopeListRow, IsotopePatternWrapper wrappedIsotopePattern, IsotopePatternUtils isotopePatternUtil) {

			this.masterIsotopeListRow = masterIsotopeListRow;
			this.wrappedIsotopePattern = wrappedIsotopePattern;

			// Check that charge is same
			if (masterIsotopeListRow.getChargeState()!=wrappedIsotopePattern.getIsotopePattern().getChargeState()) {
				return;
			}

			// Get monoisotopic peak
			Peak monoPeak = isotopePatternUtil.getMonoisotopicPeak(wrappedIsotopePattern.getIsotopePattern());

			// Calculate differences between M/Z and RT values of isotope pattern and median of the row
			double diffMZ = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicMZ()-monoPeak.getNormalizedMZ());
			score = Double.MAX_VALUE;
			goodEnough = false;			
			if ( diffMZ < MZTolerance) {
				
				double diffRT = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicRT()-monoPeak.getNormalizedRT());

				// What type of RT tolerance is used?
				double rtTolerance=0;
				if (RTToleranceUseAbs) {
					rtTolerance = RTToleranceValueAbs;
				} else {
					rtTolerance = RTToleranceValuePercent * 0.5 * (masterIsotopeListRow.getMonoisotopicRT()+monoPeak.getNormalizedRT());
				}
				
				if (diffRT < rtTolerance)  {
					score = MZvsRTBalance * diffMZ + diffRT;
					goodEnough = true;
				}
			}
			
		}

		/**
		 * This method return the master peak list that is compared in this score
		 */
		public MasterIsotopeListRow getMasterIsotopeListRow() { return masterIsotopeListRow; }

		/**
		 * This method return the isotope pattern that is compared in this score
		 */
		public IsotopePatternWrapper getWrappedIsotopePattern() { return wrappedIsotopePattern; }

		/**
		 * This method returns score between the isotope pattern and the row
		 * (the lower score, the better match)
		 */
		public double getScore() { return score; }

		/**
		 * This method returns true only if difference between isotope pattern and row is within tolerance
		 */
		public boolean isGoodEnough() {	return goodEnough; }

	}


	/**
	 * This is a helper class required for TreeSet to sorting scores in order of descending goodness of fit.
	 */
	private class ScoreOrderer implements Comparator<PatternVsRowScore> {
		public int compare(PatternVsRowScore score1, PatternVsRowScore score2) {

			// Smaller score value means smaller M/Z and RT difference 
			// and therefore smaller score is better and should come first
			if (score1.getScore()<score2.getScore()) return -1;
			return 1;

		}

		public boolean equals(Object obj) { return false; }
	}
	
	
	private class IsotopePatternWrapper {
		private IsotopePattern isotopePattern;
		private boolean alreadyJoined;
		
		public IsotopePatternWrapper(IsotopePattern isotopePattern) {
			this.isotopePattern = isotopePattern;
		}
		
		public IsotopePattern getIsotopePattern() {
			return isotopePattern;
		}
		
		public boolean isAlreadyJoined() {
			return alreadyJoined;
		}
		
		public void setAlreadyJoined(boolean alreadyJoined) {
			this.alreadyJoined = alreadyJoined;
		}
	}


}
