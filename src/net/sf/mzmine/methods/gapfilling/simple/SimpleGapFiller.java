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

package net.sf.mzmine.methods.gapfilling.simple;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.gapfilling.GapFiller;
import net.sf.mzmine.methods.gapfilling.GapFillerParameters;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


public class SimpleGapFiller implements GapFiller {

private static int idCount = 0; // DEBUG

	public SimpleGapFillerParameters askParameters(MainWindow mainWin, SimpleGapFillerParameters currentValues) {

		SimpleGapFillerParameters myParameters;
		if (currentValues==null) {
			myParameters = new SimpleGapFillerParameters();
		} else {
			myParameters = currentValues;
		}

		SimpleGapFillerParameterSetupDialog sgPSD = new SimpleGapFillerParameterSetupDialog(mainWin, new String("Please give parameter values"), myParameters);
		sgPSD.setLocationRelativeTo(mainWin);
		sgPSD.setVisible(true);

		// Check if user pressed cancel
		if (sgPSD.getExitCode()==-1) {
			return null;
		}

		myParameters = sgPSD.getParameters();

		return myParameters;

	}

	public Hashtable<Integer, double[]> fillGaps(Hashtable<Integer, double[]> gapsToFill, RawDataFile rawData, GapFillerParameters _parameters) {

		SimpleGapFillerParameters parameters = (SimpleGapFillerParameters)_parameters;


		// Transfer contents of gapsToFill to a bit different data structure...
		Vector<EmptySlot> emptySlots = new Vector<EmptySlot>();
		EmptySlot es;
		Enumeration<Integer> gapsRows = gapsToFill.keys();
		Enumeration<double[]> gapsCoordinates = gapsToFill.elements();

		while (gapsRows.hasMoreElements()) {
			Integer rowInd = gapsRows.nextElement();
			double[] coords = gapsCoordinates.nextElement();
			es = new EmptySlot( coords[0], coords[1], rowInd.intValue(), parameters );
			emptySlots.add(es);
		}

		gapsRows = null;
		gapsCoordinates = null;
		gapsToFill = null;

		// Loop through required scan range
/*		rawData.initializeScanBrowser(0, rawData.getNumberOfScans()-1);

		for (int scanInd=0; scanInd<=(rawData.getNumberOfScans()-1); scanInd++) {

			// Get this scan and its mass & intensity measurements
			Scan s = rawData.getNextScan();
			double rt = rawData.getScanTime(scanInd);

			boolean someSlotNeededScan = false;

			// Loop through all empty slots
			for (int emptySlotInd=0; emptySlotInd<emptySlots.size(); emptySlotInd++) {

				es = (EmptySlot)emptySlots.get(emptySlotInd);

				boolean wasNeeded = es.offerNextScan(s, rt);
				someSlotNeededScan = someSlotNeededScan || wasNeeded;

			}

			if (!someSlotNeededScan) { break; }
		}

		// Finalize scan loop
		rawData.finalizeScanBrowser();
*/

		// Contruct results
		Hashtable<Integer, double[]> results = new Hashtable<Integer, double[]>();
		Enumeration<EmptySlot> emptySlotEnum = emptySlots.elements();
		while (emptySlotEnum.hasMoreElements()) {
			es = emptySlotEnum.nextElement();

			es.noMoreOffers();

			double[] oneResult = new double[4];
			oneResult[0] = es.getEstimatedMZ();
			oneResult[1] = es.getEstimatedRT();
			oneResult[2] = es.getEstimatedHeight();
			oneResult[3] = es.getEstimatedArea();

			results.put(new Integer(es.getAlignmentRowNum()), oneResult);
		}

		return results;

	}




	/**
	 * This class is used for representing one empty slot in alignment result
	 */
	private class EmptySlot {
		private double centroidMZ;
		private double centroidRT;

		private int alignmentRowNum;

		private double rangeMinMZ;
		private double rangeMaxMZ;
		private double rangeMinRT;
		private double rangeMaxRT;

		private double averageIntensitySum;
		private int averageIntensityN;

		// These store information about peak that is currently under construction
		Vector<Double> peakInts;
		Vector<Double> peakMZs;
		Vector<Double> peakRTs;


		// These store information about the peak inside MZ range and so-far closest to the RT search (not necessary inside it)
		private boolean bestPeakFound;
		private double bestPeakArea;
		private double bestPeakHeight;
		private double bestPeakMZ;
		private double bestPeakRT;

		private boolean allDone = false;

		private SimpleGapFillerParameters params;


		private int myID = 0; // DEBUG




		/**
		 * Constructor:	Initializes an empty slot
		 * @param	_centroidMZ		M/Z coordinate of this empty slot
		 * @param	_centroidRT		RT coordinate of this empty slot
		 * @param	_alignmentRowNum	Number of alignment row
		 */
		public EmptySlot(double _centroidMZ, double _centroidRT, int _alignmentRowNum, SimpleGapFillerParameters _params) {
			myID = idCount++;
			centroidMZ = _centroidMZ;
			centroidRT = _centroidRT;

			params = _params;

			rangeMinMZ = centroidMZ - params.paramMZTolerance;
			rangeMaxMZ = centroidMZ + params.paramMZTolerance;

			if (params.paramRTToleranceUseAbs) {
				rangeMinRT = centroidRT - params.paramRTToleranceAbs;
				rangeMaxRT = centroidRT + params.paramRTToleranceAbs;
			} else {
				rangeMinRT = (1-params.paramRTTolerancePercent) * centroidRT;
				rangeMaxRT = (1+params.paramRTTolerancePercent) * centroidRT;
			}

			alignmentRowNum = _alignmentRowNum;

			bestPeakFound = false;
			bestPeakArea = -1;
			bestPeakHeight = -1;
			bestPeakMZ = -1;
			bestPeakRT = -1;

			averageIntensitySum = 0;
			averageIntensityN = 0;

		}


		public boolean offerNextScan(Scan s, double scanRT) {

			// If this empty gap s already filled, there is no need to analyse anymore scans
			if (allDone) { return false; }

			double[] massValues = s.getMZValues();
			double[] intensityValues = s.getIntensityValues();


			// Find local intensity maximum inside the M/Z range
			double currentIntensity = -1;
			double currentMZ = -1;
			for (int i=0; i<massValues.length; i++) {

				// Not yet in the mz range
				if (massValues[i]<rangeMinMZ) {	continue; }

				// Already passed mz range
				if (massValues[i]>rangeMaxMZ) {	break; }

				// Inside MZ range
				if (currentIntensity<=intensityValues[i]) {
					currentIntensity = intensityValues[i];
					currentMZ = massValues[i];
				}

			}

			// If there are no datapoints inside the range, then assume intensity is zero.
			// (Interpolation is not fair because data maybe centroided)
			if (currentIntensity<0) {
				currentIntensity = 0;
				currentMZ = centroidMZ;
			}


			// If this scan is inside search range, then include currentIntensity in calcualating average intensity over the area
			// (average intensity is used for estimating when there isn't any local maximum inside the range)
			if ( (rangeMinRT<=scanRT) && (scanRT<=rangeMaxRT) ) {
				averageIntensitySum += currentIntensity;
				averageIntensityN++;
			}


			// If this is the very first scan offering, then just initialize
			if (peakInts==null) {
				// New peak starts
				peakInts = new Vector<Double>();
				peakMZs = new Vector<Double>();
				peakRTs = new Vector<Double>();
				peakInts.add(new Double(currentIntensity));
				peakMZs.add(new Double(currentMZ));
				peakRTs.add(new Double(scanRT));
				return true;
			}


			// Check if this continues previous peak?
			if (checkRTShape(scanRT, currentIntensity, rangeMinRT, rangeMaxRT)) {
				// Yes, it is. Just continue this peak.
				peakInts.add(new Double(currentIntensity));
				peakMZs.add(new Double(currentMZ));
				peakRTs.add(new Double(scanRT));
			} else {

				// No it is not

				// Check previous peak as a candidate for estimator

				checkPeak();

				// New peak starts
				if (scanRT>rangeMaxRT) {
					allDone = true;
					return false;
				}

				peakInts.clear();
				peakMZs.clear();
				peakRTs.clear();
				peakInts.add(new Double(currentIntensity));
				peakMZs.add(new Double(currentMZ));
				peakRTs.add(new Double(scanRT));

			}

			return true;

		}




		public void noMoreOffers() {

			// Check peak that was last constructed
			checkPeak();
		}

		/**
		 * This function returns the M/Z coordinate of current local maximum
		 */
		public double getEstimatedMZ() {
			//return (double)alignmentRowNum/2.0;
			if (bestPeakFound) { return bestPeakMZ; }
			else {
				return centroidMZ;
			}
		}

		/**
		 * This function returns the RT coordinate of current local maximum
		 */
		public double getEstimatedRT() {
			//return (double)alignmentRowNum*2.0;
			if (bestPeakFound) { return bestPeakRT; }
			else {
				return centroidRT;
			}
		}

		/**
		 * This function returns the intensity of current local maximum
		 */
		public double getEstimatedHeight() {
			//return (double)alignmentRowNum*10.0;
			if (bestPeakFound) { return bestPeakHeight; }
			else {
				if (averageIntensityN>0) {
					return averageIntensitySum/(double)averageIntensityN;
				} else { return 0; }
			}
		}

		/**
		 * This function returns the area of estimated peak
		 */
		public double getEstimatedArea() {
			//return (double)alignmentRowNum*20.0;
			if (bestPeakFound) { return bestPeakArea; }
			else {
				if (averageIntensityN>0) {
					return averageIntensitySum/(double)averageIntensityN;
				} else { return 0; }
			}
		}

		/**
		 * This function returns the row number of this empty slot
		 */
		public int getAlignmentRowNum() {
			return alignmentRowNum;
		}


		/**
		 * This function check for the shape of the peak in RT direction, and
		 * determines if it is possible to add given m/z peak at the end of the peak.
		 */
		private boolean checkRTShape(double nextRT, double nextInt, double rangeMinRT, double rangeMaxRT) {

			if (nextRT<rangeMinRT) {
				double prevInt = peakInts.get(peakInts.size()-1);
				if (nextInt>(prevInt*(1-params.paramIntensityTolerancePercent))) { return true;	}
			}

			if ( (rangeMinRT<=nextRT) && (nextRT<=rangeMaxRT) ) { return true; }

			if (nextRT>rangeMaxRT) {
				double prevInt = peakInts.get(peakInts.size()-1);
				if (nextInt<(prevInt*(1+params.paramIntensityTolerancePercent))) { return true; }
			}

			return false;

		}


		private void checkPeak()  {

			// 1) Check if previous peak has a local maximum inside the search range
			int highestMaximumInd = -1;
			double highestMaximumHeight = 0;
			for (int ind=0; ind<peakRTs.size(); ind++) {

				if ( peakRTs.get(ind) > rangeMaxRT ) { break; }

				if ( (rangeMinRT<=peakRTs.get(ind)) && (peakRTs.get(ind)<=rangeMaxRT) ) {

					int prevind = ind-1; if (prevind<0) { prevind=0; }
					int nextind = ind+1; if (nextind>=peakRTs.size()) { nextind = peakRTs.size()-1; }

					if ((peakInts.get(ind) >= peakInts.get(nextind)) &&
						(peakInts.get(ind) >= peakInts.get(prevind))) {

							if (peakInts.get(ind)>=highestMaximumHeight) {

								highestMaximumHeight = peakInts.get(ind);
								highestMaximumInd = ind;
							}
					}

				}

			}


			if (highestMaximumInd>-1) {

				// 2) Calculate estimate for area if peak has maximum inside the range
				int ind=highestMaximumInd;
				double currentInt = peakInts.get(ind);
				double peakHeight = currentInt;
				double peakMZ = peakMZs.get(ind);
				double peakRT = peakRTs.get(ind);
				double peakArea = currentInt;

				while (true) {
					if (ind==0) { break; }
					ind--;
					double nextInt = peakInts.get(ind);
					if (currentInt>=(nextInt*(1-params.paramIntensityTolerancePercent))) {
						peakArea += nextInt;
					} else { break; }
					currentInt = nextInt;
				}

				ind = highestMaximumInd;
				currentInt = peakInts.get(ind);
				while (true) {
					if (ind==(peakInts.size()-1)) { break; }
					ind++;
					double nextInt = peakInts.get(ind);
					if (nextInt<=(currentInt*(1+params.paramIntensityTolerancePercent))) {
						peakArea += nextInt;
					} else { break; }
					currentInt = nextInt;
				}


				// 3) Check if this is the best candidate for estimator
				if (bestPeakFound) {
					if (bestPeakHeight<=peakHeight) {
						bestPeakHeight = peakHeight;
						bestPeakArea = peakArea;
						bestPeakMZ = peakMZ;
						bestPeakRT = peakRT;
					}
				} else {
					bestPeakHeight = peakHeight;
					bestPeakArea = peakArea;
					bestPeakMZ = peakMZ;
					bestPeakRT = peakRT;
					bestPeakFound = true;
				}

			}

		}



	}


	private class SimpleGapFillerParameterSetupDialog extends JDialog implements ActionListener {

		// Array for Text fields
		private JFormattedTextField txtIntensityTolerancePercent;
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
		private JLabel lblIntensityTolerancePercent;
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
		SimpleGapFillerParameters params;

		// Exit code for controlling ok/cancel response
		private int exitCode = -1;


		/**
		 * Constructor
		 */
		public SimpleGapFillerParameterSetupDialog(MainWindow _mainWin, String title, SimpleGapFillerParameters _params) {
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
			txtIntensityTolerancePercent = new JFormattedTextField(percentFormat);
			txtIntensityTolerancePercent.setColumns(8);
			txtIntensityTolerancePercent.setValue(params.paramIntensityTolerancePercent);
			pnlFields.add(txtIntensityTolerancePercent);

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
			lblIntensityTolerancePercent = new JLabel("Intensity tolerance (percent)");
			lblIntensityTolerancePercent.setLabelFor(txtIntensityTolerancePercent);
			pnlLabels.add(lblIntensityTolerancePercent);

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
				params.paramIntensityTolerancePercent = ((Number)(txtIntensityTolerancePercent.getValue())).doubleValue();
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
		public SimpleGapFillerParameters getParameters() {
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