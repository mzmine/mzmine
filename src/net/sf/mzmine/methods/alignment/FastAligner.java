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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.methods.peakpicking.Peak;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.util.MyMath;




/**
 *
 */
public class FastAligner implements PeakListAligner {

	private FastAlignerParameters parameters;

	/**
	 * This method asks user to define which raw data files should be aligned and also check parameter values
	 */
	public FastAlignerParameters askParameters(MainWindow mainWin, FastAlignerParameters currentValues) {

		FastAlignerParameters myParameters;
		if (currentValues==null) {
			myParameters = new FastAlignerParameters();
		} else {
			myParameters = currentValues;
		}

		FastAlignerParameterSetupDialog jaPSD = new FastAlignerParameterSetupDialog(mainWin, new String("Please give parameter values"), myParameters);
		jaPSD.setLocationRelativeTo(mainWin);
		jaPSD.setVisible(true);

		// Check if user pressed cancel
		if (jaPSD.getExitCode()==-1) {
			return null;
		}

		myParameters = jaPSD.getParameters();

		return myParameters;
	}


	/**
	 * This function aligns peak lists of selected group of raw data files
	 */
	// public AlignmentResult doAlignment(MainWindow _mainWin) {
	 public AlignmentResult doAlignment( Hashtable<Integer, PeakList> peakLists, PeakListAlignerParameters _parameters) {


		parameters = (FastAlignerParameters)_parameters;


		// Translate peak lists to isotope lists
		// -------------------------------------

		// Data structure for storing isotope lists
		Hashtable<Integer, Hashtable<Integer, IsotopePattern>> isotopeLists = new Hashtable<Integer, Hashtable<Integer, IsotopePattern>>();

		// Loop through the peak lists, and collect peaks to isotope patterns
		Enumeration<Integer> rawDataIDEnum = peakLists.keys();
		Enumeration<PeakList> peakListEnum = peakLists.elements();
		while (rawDataIDEnum.hasMoreElements()) {

			// Pickup next rawDataID and all peaks for that raw data
			Integer rawDataID = rawDataIDEnum.nextElement();
			Vector<Peak> peakList = peakListEnum.nextElement().getPeaks();

			// Initialize isotope list
			Hashtable<Integer, IsotopePattern> isotopeList = new Hashtable<Integer, IsotopePattern>();

			// Find maximum used isotope pattern number (information is needed for filling unassigned isotope numbers)
			int nextUnassignedIsotopePatternID=-1;
			for (Peak p : peakList) {
				if ( p.getIsotopePatternID()>nextUnassignedIsotopePatternID ) { nextUnassignedIsotopePatternID = p.getIsotopePatternID(); }
			}
			nextUnassignedIsotopePatternID++;

			// Loop through all peaks in this raw data
			for (Peak p : peakList) {
				// Get isotope pattern ID for this peak
				Integer isotopePatternID = new Integer(p.getIsotopePatternID());

				// If isotope Pattern ID is unassigned (-1), then this peak must be assigned to a new isotope pattern
				if (isotopePatternID==-1) {
					isotopePatternID = new Integer(nextUnassignedIsotopePatternID);
					nextUnassignedIsotopePatternID++;
				}


				// Check if there is an exisiting isotope pattern object for this ID and create a new pattern if there isn't one already
				IsotopePattern isotopePattern = isotopeList.get(isotopePatternID);
				if (isotopePattern==null) {
					isotopePattern = new IsotopePattern(rawDataID.intValue());
					isotopeList.put(isotopePatternID, isotopePattern);
				}

				// Add this peak to the isotope pattern
				isotopePattern.addPeak(p);
			}

			// Store the isotope list
			isotopeLists.put(rawDataID, isotopeList);

		}

		System.gc();

		// Initialize master isotope list
		// ------------------------------
		Vector<MasterIsotopeListRow> masterIsotopeListRows = new Vector<MasterIsotopeListRow>();
		//Vector<Hashtable<Integer, IsotopePattern>> alignmentRows = new Vector<Hashtable<Integer, IsotopePattern>>();


		// Match eack isotope list against master isotope list
		// ---------------------------------------------------

		// Loop through all isotope lists
		rawDataIDEnum = isotopeLists.keys();
		int numberOfLists = isotopeLists.size();
		int currentList = 0;
		while (rawDataIDEnum.hasMoreElements()) {
			Integer rawDataID = rawDataIDEnum.nextElement();

		//	nodeServer.updateJobCompletionRate((double)(currentList)/(double)(numberOfLists));
			currentList++;



			// Calculate scores between isotopes in this list and rows currently in the master peak list
			// -----------------------------------------------------------------------------------------

			// Pickup isotope list for this rawDataID
			Hashtable<Integer, IsotopePattern> isotopeList = isotopeLists.get(rawDataID);

			// Calculate scores between all isotope patterns on the current list and all rows of the master isotope list
			TreeSet<PatternVsRowScore> scoreTree = new TreeSet<PatternVsRowScore>(new ScoreOrderer());

			// Loop isotope patterns
			Enumeration<IsotopePattern> isotopePatternEnum = isotopeList.elements();
			while (isotopePatternEnum.hasMoreElements()) {
				IsotopePattern isotopePattern = isotopePatternEnum.nextElement();

				// Loop master isotope list rows
				for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {

					// Calc & store score
					PatternVsRowScore score = new PatternVsRowScore(masterIsotopeListRow, isotopePattern, parameters);
					if (score.isGoodEnough()) scoreTree.add(score);

				}

			}

			// Browse scores in order of descending goodness-of-fit
			// ----------------------------------------------------

			Iterator<PatternVsRowScore> scoreIter = scoreTree.iterator();
			while (scoreIter.hasNext()) {
				PatternVsRowScore score = scoreIter.next();

				MasterIsotopeListRow masterIsotopeListRow = score.getMasterIsotopeListRow();
				IsotopePattern isotopePattern = score.getIsotopePattern();

				// Check if master list row is already assigned with an isotope pattern (from this rawDataID)
				if (masterIsotopeListRow.isAlreadyJoined()) { continue; }

				// Check if isotope pattern is already assigned to some master isotope list row
				if (isotopePattern.isAlreadyJoined()) { continue; }

				// Check if score good enough
				//if (score.isGoodEnough()) {
					// Assign isotope pattern to master peak list row
					masterIsotopeListRow.addPattern(rawDataID, isotopePattern);

					// Mark pattern and isotope pattern row as joined
					masterIsotopeListRow.setJoined(true);
					isotopePattern.setJoined(true);
				//}

			}


			// Append all non-assigned isotope patterns to new rows of the master isotope list
			// -------------------------------------------------------------------------------

			isotopePatternEnum = isotopeList.elements();
			while (isotopePatternEnum.hasMoreElements()) {
				IsotopePattern isotopePattern = isotopePatternEnum.nextElement();
				if (!isotopePattern.isAlreadyJoined()) {
					MasterIsotopeListRow masterIsotopeListRow = new MasterIsotopeListRow();
					masterIsotopeListRow.addPattern(new Integer(isotopePattern.getRawDataID()), isotopePattern);
					masterIsotopeListRows.add(masterIsotopeListRow);
				}
			}

			// Clear "Joined" information from all master isotope list rows
			// ------------------------------------------------------------
			for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {
				masterIsotopeListRow.setJoined(false);
			}


		}

		// Convert master isotope list to master peak list
		// -----------------------------------------------

		// Get number of peak rows
		int numberOfRows = 0;
		for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) { numberOfRows += masterIsotopeListRow.getCombinedPatternSize(); }


		Vector<Integer> rawDataIDs = new Vector<Integer>();

		// Allocate arrays for storing common information (shared between all raw data)
		boolean[] commonStandardCompounds = new boolean[numberOfRows];
		int[] commonIsotopePatternIDs = new int[numberOfRows];
		int[] commonIsotopePeakNumbers = new int[numberOfRows];
		int[] commonChargeStates = new int[numberOfRows];

		// Allocate Hashtables for storing column arrays for each raw data
		Hashtable<Integer, int[]> peakStatuses = new Hashtable<Integer, int[]>();
		Hashtable<Integer, int[]> peakIDs = new Hashtable<Integer, int[]>();
		Hashtable<Integer, double[]> peakMZs = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> peakRTs = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> peakHeights = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> peakAreas = new Hashtable<Integer, double[]>();

		// Initialize Hashtables
		rawDataIDEnum = isotopeLists.keys();
		while (rawDataIDEnum.hasMoreElements()) {
			Integer rawDataID = rawDataIDEnum.nextElement();
			rawDataIDs.add(rawDataID);

			peakStatuses.put(rawDataID, new int[numberOfRows]);
			peakIDs.put(rawDataID, new int[numberOfRows]);
			peakMZs.put(rawDataID, new double[numberOfRows]);
			peakRTs.put(rawDataID, new double[numberOfRows]);
			peakHeights.put(rawDataID, new double[numberOfRows]);
			peakAreas.put(rawDataID, new double[numberOfRows]);
		}


		// Loop through master isotope list, and fill rows of master peak list
		int currentPeakListRow = 0;
		int currentIsotopePattern = 0;
		for (MasterIsotopeListRow masterIsotopeListRow : masterIsotopeListRows) {

			currentIsotopePattern++;

			// Loop through all different isotopic peaks available on this row
			int[] isotopePeakNumbers = masterIsotopeListRow.getCombinedPeakNumbers();
			for (int isotopePeakNumber : isotopePeakNumbers) {




				// Finally, loop through all raw data IDs participating in this alignment
				rawDataIDEnum = isotopeLists.keys();
				while (rawDataIDEnum.hasMoreElements()) {
					Integer rawDataID = rawDataIDEnum.nextElement();


					// Check if this raw data ID has isotope pattern on this row, and if this pattern contains corresponding isotope peak
					IsotopePattern isotopePattern = null;
					Peak p = null;
					isotopePattern = masterIsotopeListRow.get(rawDataID);
					if (isotopePattern!=null) { p = isotopePattern.getPeak(isotopePeakNumber); }
					if (p!=null) {

						// Isotope Pattern ID is given as a running numbering
						commonIsotopePatternIDs[currentPeakListRow] = currentIsotopePattern;

						// Isotope peak number and charge state must match between all peaks
						commonIsotopePeakNumbers[currentPeakListRow] = p.getIsotopePeakNumber();
						commonChargeStates[currentPeakListRow] = p.getChargeState();


						// Yes => Assign peak's information to current row and raw data's column
						(peakStatuses.get(rawDataID))[currentPeakListRow] = AlignmentResult.PEAKSTATUS_DETECTED;

						(peakIDs.get(rawDataID))[currentPeakListRow] = p.getPeakID();
						(peakMZs.get(rawDataID))[currentPeakListRow] = p.getMZ();
						(peakRTs.get(rawDataID))[currentPeakListRow] = p.getRT();
						(peakHeights.get(rawDataID))[currentPeakListRow] = p.getHeight();
						(peakAreas.get(rawDataID))[currentPeakListRow] = p.getArea();

					} else {

						// No. either the whole isotope pattern is missing, or this particular peak in the pattern was not detected in raw data file
						// Put missing information to current row and raw data's column
						(peakStatuses.get(rawDataID))[currentPeakListRow] = AlignmentResult.PEAKSTATUS_NOTFOUND;

						(peakIDs.get(rawDataID))[currentPeakListRow] = -1;
						(peakMZs.get(rawDataID))[currentPeakListRow] = -1;
						(peakRTs.get(rawDataID))[currentPeakListRow] = -1;
						(peakHeights.get(rawDataID))[currentPeakListRow] = -1;
						(peakAreas.get(rawDataID))[currentPeakListRow] = -1;

					}

				}

				// Move to next row
				currentPeakListRow++;

			}

		}


		AlignmentResult ar = new AlignmentResult(	rawDataIDs,
													commonStandardCompounds,
													commonIsotopePatternIDs,
													commonIsotopePeakNumbers,
													commonChargeStates,
													peakStatuses,
													peakIDs,
													peakMZs,
													peakRTs,
													peakHeights,
													peakAreas,
													new String("Raw peak data after alignment"));


		return ar;

    }








	/**
	 * This class represents a score between master peak list row and isotope pattern
	 */
	private class PatternVsRowScore {

		MasterIsotopeListRow masterIsotopeListRow;
		IsotopePattern isotopePattern;
		double score;
		boolean goodEnough;


		public PatternVsRowScore(MasterIsotopeListRow _masterIsotopeListRow, IsotopePattern _isotopePattern, FastAlignerParameters parameters) {

			masterIsotopeListRow = _masterIsotopeListRow;
			isotopePattern = _isotopePattern;

			// Check that charge is same
			if (masterIsotopeListRow.getChargeState()!=isotopePattern.getChargeState()) {
				score = Double.MAX_VALUE;
				goodEnough = false;
			}

			// Calculate differences between M/Z and RT values of isotope pattern and median of the row
			double diffMZ = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicMZ()-isotopePattern.getMonoisotopicMZ());
			double diffRT = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicRT()-isotopePattern.getMonoisotopicRT());

			// What type of RT tolerance is used?
			double rtTolerance=0;
			if (parameters.paramRTToleranceUseAbs) {
				rtTolerance = parameters.paramRTToleranceAbs;
			} else {
				rtTolerance = parameters.paramRTTolerancePercent * 0.5 * (masterIsotopeListRow.getMonoisotopicRT()+isotopePattern.getMonoisotopicRT());
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

	/**
	 * This is a helper class required for TreeSet to sorting scores in order of descending goodness of fit.
	 */
	private class ScoreOrderer implements Comparator<PatternVsRowScore> {
		public int compare(PatternVsRowScore score1, PatternVsRowScore score2) {

			// Pattern with smaller scores is better, and should come first
			if (score1.getScore()<score2.getScore()) { return -1; }
			return 1;
		}

		public boolean equals(Object obj) { return false; }
	}

	/**
	 * This class represent one row of the master isotope list
	 */
	private class MasterIsotopeListRow extends Hashtable<Integer, IsotopePattern> {
		private boolean alreadyJoined = false;
		private double monoisotopicMZ;
		private double monoisotopicRT;
		private int chargeState;

		public void setJoined(boolean b) { alreadyJoined = b; }
		public boolean isAlreadyJoined() { return alreadyJoined; }

		/**
		 * This method adds one pattern to this row and updates the average monoisotopic M/Z and RT of the row
		 * It is assumed that chargeState of the added pattern is same as previously added pattern's charge.
		 */
		public void addPattern(Integer rawDataID, IsotopePattern pattern) {
			put(rawDataID, pattern);
			chargeState = pattern.getChargeState();
			updateMonoisotopicMZRT();
		}

		/**
		 * This method calculates the median M/Z and RT of all monoisotopic peaks on the row.
		 */
		private void updateMonoisotopicMZRT() {
			double[] mzVals = new double[size()];
			double[] rtVals = new double[size()];

			Enumeration<IsotopePattern> isotopeEnum = elements();
			int index=0;
			while (isotopeEnum.hasMoreElements()) {
				IsotopePattern pattern = isotopeEnum.nextElement();
				mzVals[index] = pattern.getMonoisotopicMZ();
				rtVals[index] = pattern.getMonoisotopicRT();
				index++;
			}

			monoisotopicMZ = MyMath.calcQuantile(mzVals, 0.5);
			monoisotopicRT = MyMath.calcQuantile(rtVals, 0.5);
		}


		/**
		 * This method returns the median M/Z of all monoisotopic peaks on this row
		 */
		public double getMonoisotopicMZ() { return monoisotopicMZ; }

		/**
		 * This method return the median RT of all monoisotopic peaks on this row
		 */
		public double getMonoisotopicRT() { return monoisotopicRT; }

		/**
		 * This method returns the charge state of peaks on this row
		 */
		public int getChargeState() { return chargeState; }

		/**
		 * This method calculates the total number of different isotopic peaks in all patterns
		 */
		public int getCombinedPatternSize() {
			return getCombinedPeakNumbers().length;
		}

		/**
		 * This method collects all unique isotopic peak numbers participating in patterns on this row
		 */
		public int[] getCombinedPeakNumbers() {
			// Collect all unique isotope pattern peak numbers to this set
			HashSet<Integer> allPeakNumbers = new HashSet<Integer>();

			// Loop through all isotope patterns
			Enumeration<IsotopePattern> isotopePatternEnum = elements();
			while (isotopePatternEnum.hasMoreElements()) {
				IsotopePattern isotopePattern = isotopePatternEnum.nextElement();

				// Get isotope peak numbers of this pattern & and store them (if not already stored)
				Set<Integer> isotopePatternPeakNumbers = isotopePattern.getPeaks().keySet();
				allPeakNumbers.addAll(isotopePatternPeakNumbers);
			}

			int[] allPeakNumbersIntArray = new int[allPeakNumbers.size()];
			Iterator<Integer> allPeakNumbersIter = allPeakNumbers.iterator();
			int index=0;
			while (allPeakNumbersIter.hasNext()) {
				Integer peakNumber = allPeakNumbersIter.next();
				allPeakNumbersIntArray[index] = peakNumber.intValue();
				index++;
			}

			return allPeakNumbersIntArray;

		}


	}


	/**
	 * This class is used for keeping all peaks of a pattern together during alignment
	 */
	private class IsotopePattern {

		private int isotopePatternID;
		private int rawDataID;						// Raw data ID whose peaks are in this pattern

		private Hashtable<Integer, Peak> peaks;		// All peaks in this isotope pattern

		// These three values are picked up from the monoisotopic peak, so accessing them is fast
		private double monoisotopicMZ;
		private double monoisotopicRT;
		private int chargeState;

		private boolean alreadyJoined = false;



		public IsotopePattern(int _rawDataID) {
			rawDataID = _rawDataID;
			peaks = new Hashtable<Integer, Peak>();
		}

		public int getRawDataID() { return rawDataID; }

		public void addPeak(Peak p) {
			if (p.getIsotopePeakNumber()==0) {
				monoisotopicMZ = p.getMZ();
				monoisotopicRT = p.getRT();
				chargeState = p.getChargeState();
			}
			peaks.put(new Integer(p.getIsotopePeakNumber()), p);
		}

		public boolean containsPeakNumber(int peakNumber) {
			if (peaks.get(new Integer(peakNumber))!=null) { return true; } else { return false; }
		}

		public Peak getPeak(int peakNumber) {
			return peaks.get(new Integer(peakNumber));
		}

		public Hashtable<Integer, Peak> getPeaks() { return peaks; }
		public double getMonoisotopicMZ() {	return monoisotopicMZ; }
		public double getMonoisotopicRT() { return monoisotopicRT; }
		private int getChargeState() { return chargeState; }

		public boolean isAlreadyJoined() { return alreadyJoined; }
		public void setJoined(boolean b) { alreadyJoined = b; }


	}



	/**
	 * Customized parameter setup dialog for join aligner
	 */
	private class FastAlignerParameterSetupDialog extends JDialog implements ActionListener {

		// Array for Text fields
		private JFormattedTextField txtMZvsRTBalance;
		private JFormattedTextField txtMZTolerance;
		private JComboBox cmbRTToleranceType;
		private JFormattedTextField txtRTToleranceAbsValue;
		private JFormattedTextField txtRTTolerancePercent;

		// Number formatting used in text fields
		private NumberFormat decimalNumberFormatOther;
		private NumberFormat decimalNumberFormatMZ;
		private NumberFormat percentFormat;

		// Options available in cmbRTToleranceType
		private Vector<String> optionsIncmbRTToleranceType;

		// Labels
		private JLabel lblMZvsRTBalance;
		private JLabel lblMZTolerance;
		private JLabel lblRTToleranceType;
		private JLabel lblRTToleranceAbsValue;
		private JLabel lblRTTolerancePercent;

		// Buttons
		private JButton btnOK;
		private JButton btnCancel;

		// Panels for all above
		private JPanel pnlAll;
		private JPanel pnlLabels;
		private JPanel pnlFields;
		private JPanel pnlButtons;

		// Parameter values
		FastAlignerParameters params;

		// Exit code for controlling ok/cancel response
		private int exitCode = -1;


		/**
		 * Constructor
		 */
		public FastAlignerParameterSetupDialog(MainWindow _mainWin, String title, FastAlignerParameters _params) {
			super(_mainWin, title, true);

			params = _params;
			exitCode = -1;

			// Panel where everything is collected
			pnlAll = new JPanel(new BorderLayout());
			pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			getContentPane().add(pnlAll);

			// Two more panels: one for labels and another for text fields
			pnlLabels = new JPanel(new GridLayout(0,1));
			pnlFields = new JPanel(new GridLayout(0,1));

			// Setup number formats for text fields
			decimalNumberFormatMZ = NumberFormat.getNumberInstance();
			decimalNumberFormatMZ.setMinimumFractionDigits(3);
			decimalNumberFormatOther = NumberFormat.getNumberInstance();
			decimalNumberFormatOther.setMinimumFractionDigits(1);
			percentFormat = NumberFormat.getPercentInstance();

			// Create fields
			txtMZvsRTBalance = new JFormattedTextField(decimalNumberFormatOther);
			txtMZvsRTBalance.setColumns(8);
			txtMZvsRTBalance.setValue(params.paramMZvsRTBalance);
			pnlFields.add(txtMZvsRTBalance);

			txtMZTolerance = new JFormattedTextField(decimalNumberFormatMZ);
			txtMZTolerance.setColumns(8);
			txtMZTolerance.setValue(params.paramMZTolerance);
			pnlFields.add(txtMZTolerance);

			optionsIncmbRTToleranceType = new Vector<String>();
			optionsIncmbRTToleranceType.add(new String("Absolute (seconds)"));
			optionsIncmbRTToleranceType.add(new String("Percent of RT"));
			cmbRTToleranceType = new JComboBox(optionsIncmbRTToleranceType);
			cmbRTToleranceType.addActionListener(this);
			pnlFields.add(cmbRTToleranceType);

			txtRTToleranceAbsValue = new JFormattedTextField(decimalNumberFormatOther);
			txtRTToleranceAbsValue.setColumns(8);
			txtRTToleranceAbsValue.setValue(params.paramRTToleranceAbs);
			pnlFields.add(txtRTToleranceAbsValue);

			txtRTTolerancePercent = new JFormattedTextField(percentFormat);
			txtRTTolerancePercent.setColumns(8);
			txtRTTolerancePercent.setValue(params.paramRTTolerancePercent);
			pnlFields.add(txtRTTolerancePercent);



			// Create labels
			lblMZvsRTBalance = new JLabel("Balance between M/Z and RT");
			lblMZvsRTBalance.setLabelFor(txtMZvsRTBalance);
			pnlLabels.add(lblMZvsRTBalance);

			lblMZTolerance = new JLabel("M/Z tolerance size");
			lblMZTolerance.setLabelFor(txtMZTolerance);
			pnlLabels.add(lblMZTolerance);

			lblRTToleranceType = new JLabel("RT tolerance type");
			lblRTToleranceType.setLabelFor(cmbRTToleranceType);
			pnlLabels.add(lblRTToleranceType);

			lblRTToleranceAbsValue = new JLabel("RT tolerance size (absolute)");
			lblRTToleranceAbsValue.setLabelFor(txtRTToleranceAbsValue);
			pnlLabels.add(lblRTToleranceAbsValue);

			lblRTTolerancePercent = new JLabel("RT tolerance size (percent)");
			lblRTTolerancePercent.setLabelFor(txtRTTolerancePercent);
			pnlLabels.add(lblRTTolerancePercent);


			if (params.paramRTToleranceUseAbs) {
				cmbRTToleranceType.setSelectedIndex(0);
			} else {
				cmbRTToleranceType.setSelectedIndex(1);
			}

			// Buttons
			pnlButtons = new JPanel();
			btnOK = new JButton("OK");
			btnOK.addActionListener(this);
			btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(this);
			pnlButtons.add(btnOK);
			pnlButtons.add(btnCancel);

			pnlAll.add(pnlLabels,BorderLayout.CENTER);
			pnlAll.add(pnlFields,BorderLayout.LINE_END);
			pnlAll.add(pnlButtons,BorderLayout.SOUTH);

			getContentPane().add(pnlAll);

			setLocationRelativeTo(_mainWin);

			pack();


		}

		/**
		 * Implementation for ActionListener interface
		 */
		public void actionPerformed(java.awt.event.ActionEvent ae) {
			Object src = ae.getSource();
			if (src==btnOK) {

				// Copy values back to parameters object
				params.paramMZvsRTBalance = ((Number)(txtMZvsRTBalance.getValue())).doubleValue();
				params.paramMZTolerance = ((Number)(txtMZTolerance.getValue())).doubleValue();
				params.paramRTToleranceAbs = ((Number)(txtRTToleranceAbsValue.getValue())).doubleValue();
				params.paramRTTolerancePercent = ((Number)(txtRTTolerancePercent.getValue())).doubleValue();
				int ind = cmbRTToleranceType.getSelectedIndex();
				if (ind==0) {
					params.paramRTToleranceUseAbs = true;
				} else {
					params.paramRTToleranceUseAbs = false;
				}

				// Set exit code and fade away
				exitCode = 1;
				setVisible(false);
			}

			if (src==btnCancel) {
				exitCode = -1;
				setVisible(false);
			}

			if (src==cmbRTToleranceType) {
				int ind = cmbRTToleranceType.getSelectedIndex();
				if (ind==0) {
					// "Absolute" selected
					txtRTToleranceAbsValue.setEnabled(true);
					lblRTToleranceAbsValue.setEnabled(true);

					txtRTTolerancePercent.setEnabled(false);
					lblRTTolerancePercent.setEnabled(false);
				}

				if (ind==1) {
					// "Percent" selected
					txtRTToleranceAbsValue.setEnabled(false);
					lblRTToleranceAbsValue.setEnabled(false);

					txtRTTolerancePercent.setEnabled(true);
					lblRTTolerancePercent.setEnabled(true);
				}

			}

		}

		/**
		 * Method for reading contents of a field
		 * @param	fieldNum	Number of field
		 * @return	Value of the field
		 */
		public FastAlignerParameters getParameters() {
			return params;
		}

		/**
		 * Method for reading exit code
		 * @return	1=OK clicked, -1=cancel clicked
		 */
		public int getExitCode() {
			return exitCode;
		}

	}

}
