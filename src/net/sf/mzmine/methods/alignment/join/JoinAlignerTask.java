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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.ArrayList;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.IsotopePattern;

import net.sf.mzmine.methods.deisotoping.util.IsotopePatternUtility;

/**
 *
 */
class JoinAlignerTask implements Task {

    private OpenedRawDataFile[] dataFiles;
    private JoinAlignerParameters parameters;

    private TaskStatus status;
    private String errorMessage;

    private int processedScans;
    private int totalScans;

    /**
     * @param rawDataFile
     * @param parameters
     */
    JoinAlignerTask(OpenedRawDataFile[] dataFiles,
            JoinAlignerParameters parameters) {

        status = TaskStatus.WAITING;
        this.dataFiles = dataFiles;
        this.parameters = parameters;

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
		return 0.0f;
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
        results[0] = null; // alignmentResult; TODO
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

		// Initialize master isotope list
		// ------------------------------
		Vector<MasterIsotopeListRow> masterIsotopeListRows = new Vector<MasterIsotopeListRow>();


		// Loop through all data files
		// ---------------------------
		for (OpenedRawDataFile dataFile : dataFiles) {

			// Pickup peak list for this file and generate list of isotope patterns
			PeakList peakList = (PeakList)dataFile.getCurrentFile().getLastData(PeakList.class);
			IsotopePatternUtility isoUtil = new IsotopePatternUtility(peakList);
			IsotopePattern[] isotopePatternList = isoUtil.getAllIsotopePatterns();

			// Calculate scores between all pairs of isotope pattern and master isotope list row
			// Loop through isotope patterns
			for (IsotopePattern isotopePattern : isotopePatternList) {

				// Loop through master isotope list row

			}




		}



        status = TaskStatus.FINISHED;

    }





	/**
	 * This class represent one row of the master isotope list
	 */
	private class MasterIsotopeListRow extends Hashtable<OpenedRawDataFile, IsotopePattern> {

		private double monoMZ;
		private double monoRT;

		private int chargeState;

		private Vector<Double> mzVals;
		private Vector<Double> rtVals;

		private boolean alreadyJoined = false;

		public MasterIsotopeListRow() {
			mzVals = new Vector<Double>();
			rtVals = new Vector<Double>();
		}

		public void addIsotopePattern(IsotopePattern isotopePattern, IsotopePatternUtility util) {
			// TODO:

			// Get monoisotopic peak

			// Add M/Z and RT

			// Update medians

			// Set charge state

		}

		public double getMonoisotopicMZ() { return monoMZ; }

		public double getMonoisotopicRT() { return monoRT; }

		public int getChargeState() { return chargeState; }

		public void setJoined(boolean b) { alreadyJoined = b; }

		public boolean isAlreadyJoined() { return alreadyJoined; }

	}



	/**
	 * This class represents a score between master peak list row and isotope pattern
	 */
	private class PatternVsRowScore {

		MasterIsotopeListRow masterIsotopeListRow;
		IsotopePattern isotopePattern;
		double score;
		boolean goodEnough;


		public PatternVsRowScore(MasterIsotopeListRow masterIsotopeListRow, IsotopePattern isotopePattern, IsotopePatternUtility isotopePatternUtil, JoinAlignerParameters parameters) {

			this.masterIsotopeListRow = masterIsotopeListRow;
			this.isotopePattern = isotopePattern;

			// Check that charge is same
			if (masterIsotopeListRow.getChargeState()!=isotopePattern.getChargeState()) {
				score = Double.MAX_VALUE;
				goodEnough = false;
			}

			// Get monoisotopic peak
			Peak monoPeak = isotopePatternUtil.getMonoisotopicPeak(isotopePattern);

			// Calculate differences between M/Z and RT values of isotope pattern and median of the row
			double diffMZ = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicMZ()-monoPeak.getNormalizedMZ());
			double diffRT = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicRT()-monoPeak.getNormalizedRT());

			// What type of RT tolerance is used?
			double rtTolerance=0;
			if (parameters.paramRTToleranceUseAbs) {
				rtTolerance = parameters.paramRTToleranceAbs;
			} else {
				rtTolerance = parameters.paramRTTolerancePercent * 0.5 * (masterIsotopeListRow.getMonoisotopicRT()+monoPeak.getNormalizedRT());
			}

			// Calculate score if differences within tolerances
			if ( (diffMZ < parameters.paramMZTolerance) &&
				 (diffRT < rtTolerance) ) {
				score = parameters.paramMZvsRTBalance * diffMZ + diffRT;
				goodEnough = true;
			} else {
				score = Double.MAX_VALUE;
				goodEnough = false;
			}

		}

		/**
		 * This method return the master peak list that is compared in this score
		 */
		public MasterIsotopeListRow getMasterIsotopeListRow() { return masterIsotopeListRow; }

		/**
		 * This method return the isotope pattern that is compared in this score
		 */
		public IsotopePattern getIsotopePattern() { return isotopePattern; }

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



}
