/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JInternalFrame;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.MzDataTable;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizerWindow extends JInternalFrame implements
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private SpectraVisualizerType visualizerType;

	private SpectraToolBar toolBar;
	private SpectraPlot spectrumPlot;
	private SpectraBottomPanel bottomPanel;

	private RawDataFile dataFile;

	// Currently loaded scan
	private Scan currentScan;

	// Current scan data set
	private SpectraDataSet spectrumDataSet;

	private NumberFormat rtFormat = MZmineCore.getRTFormat();
	private NumberFormat mzFormat = MZmineCore.getMZFormat();
	private NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

	public SpectraVisualizerWindow(RawDataFile dataFile, String title,
			SpectraVisualizerType type) {

		super(title, true, true, true, true);
		this.dataFile = dataFile;

		visualizerType = type;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		spectrumPlot = new SpectraPlot(this, visualizerType);
		add(spectrumPlot, BorderLayout.CENTER);

		toolBar = new SpectraToolBar(spectrumPlot, visualizerType);
		add(toolBar, BorderLayout.EAST);

		// Create relationship between the current Plot and Tool bar
		spectrumPlot.setRelatedToolBar(toolBar);

		if (visualizerType == SpectraVisualizerType.SPECTRUM) {
			bottomPanel = new SpectraBottomPanel(this, dataFile, visualizerType);
			add(bottomPanel, BorderLayout.SOUTH);
		}

		pack();

	}

	SpectraVisualizerWindow(RawDataFile dataFile, String title,
			final MzDataTable mzDataTable) {

		super(title, true, true, true, true);

		if (mzDataTable instanceof Scan) {
			visualizerType = SpectraVisualizerType.SPECTRUM;
		} else if (mzDataTable instanceof IsotopePattern) {
			visualizerType = SpectraVisualizerType.ISOTOPE;
		}

		this.dataFile = dataFile;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setBackground(Color.white);

		spectrumPlot = new SpectraPlot(this, visualizerType);
		add(spectrumPlot, BorderLayout.CENTER);

		toolBar = new SpectraToolBar(spectrumPlot, visualizerType);
		add(toolBar, BorderLayout.EAST);

		// Create relationship between the current Plot and Tool bar
		spectrumPlot.setRelatedToolBar(toolBar);

		if (visualizerType == SpectraVisualizerType.SPECTRUM) {
			bottomPanel = new SpectraBottomPanel(this, dataFile, visualizerType);
			add(bottomPanel, BorderLayout.SOUTH);
		}

		if (visualizerType == SpectraVisualizerType.SPECTRUM) {

			Runnable newThreadRunnable = new Runnable() {

				public void run() {
					loadRawData(mzDataTable);
					loadPeaks();
				}

			};

			Thread newThread = new Thread(newThreadRunnable);
			newThread.start();

		} else {

			Runnable newThreadRunnable = new Runnable() {

				public void run() {
					loadRawData(mzDataTable);
					//loadPeaks();
					//loadIsotopePattern((IsotopePattern) mzDataTable);
				}

			};

			Thread newThread = new Thread(newThreadRunnable);
			newThread.start();

		}

		pack();

		// After we have constructed everything, load the peak lists into the
		// bottom panel
		if (visualizerType == SpectraVisualizerType.SPECTRUM)
			bottomPanel.rebuildPeakListSelector(MZmineCore.getCurrentProject());

	}

	public void loadRawData(MzDataTable rawData) {

		logger.finest("Loading rawData#  from " + dataFile
				+ " for spectra visualizer");

		spectrumDataSet = new SpectraDataSet(rawData);

		if (visualizerType == SpectraVisualizerType.SPECTRUM) {

			currentScan = (Scan) rawData;

			// Set plot mode only if it hasn't been set before
			if (spectrumPlot.getPlotMode() == PlotMode.UNDEFINED)
				// if the scan is centroided, switch to centroid mode
				if (currentScan.isCentroided()) {
					spectrumPlot.setPlotMode(PlotMode.CENTROID);
					toolBar.setCentroidButton(false);
				} else {
					spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
					toolBar.setCentroidButton(true);
				}

			// Clean up the MS/MS selector combo

			JComboBox msmsSelector = bottomPanel.getMSMSSelector();
			msmsSelector.removeAllItems();
			boolean msmsVisible = false;

			// Add parent scan to MS/MS selector combo

			int parentNumber = currentScan.getParentScanNumber();
			if ((currentScan.getMSLevel() > 1) && (parentNumber > 0)) {

				Scan parentScan = dataFile.getScan(parentNumber);
				if (parentScan != null) {
					String itemText = "Parent scan #" + parentNumber + ", RT: "
							+ rtFormat.format(parentScan.getRetentionTime())
							+ ", precursor m/z: "
							+ mzFormat.format(currentScan.getPrecursorMZ());

					if (currentScan.getPrecursorCharge() > 0)
						itemText += " (chrg "
								+ currentScan.getPrecursorCharge() + ")";

					msmsSelector.addItem(itemText);
					msmsVisible = true;
				}
			}

			// Add all fragment scans to MS/MS selector combo
			int fragmentScans[] = currentScan.getFragmentScanNumbers();
			if (fragmentScans != null) {

				for (int fragment : fragmentScans) {
					Scan fragmentScan = dataFile.getScan(fragment);
					if (fragmentScan == null)
						continue;
					String itemText = "Fragment scan #" + fragment + ", RT: "
							+ rtFormat.format(fragmentScan.getRetentionTime())
							+ ", precursor m/z: "
							+ mzFormat.format(fragmentScan.getPrecursorMZ());
					msmsSelector.addItem(itemText);
					msmsVisible = true;
				}
			}

			// Update the visibility of MS/MS selection combo
			bottomPanel.setMSMSSelectorVisible(msmsVisible);

			// Set window and plot titles

			String title = "[" + dataFile.toString() + "] scan #"
					+ currentScan.getScanNumber();

			String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
					+ rtFormat.format(currentScan.getRetentionTime());

			DataPoint basePeak = currentScan.getBasePeak();
			if (basePeak != null) {
				subTitle += ", base peak: " + mzFormat.format(basePeak.getMZ())
						+ " m/z ("
						+ intensityFormat.format(basePeak.getIntensity()) + ")";
			}

			setTitle(title);
			spectrumPlot.setTitle(title, subTitle);

		} else {

			IsotopePattern isotopePattern = (IsotopePattern) rawData;

			Range mzRange = isotopePattern.getIsotopeMzRange();
			String subTitle = "";

			if (isotopePattern.getIsotopePatternStatus() == IsotopePatternStatus.PREDICTED) {
				spectrumPlot.setPlotMode(PlotMode.CENTROID);
				toolBar.setCentroidButton(false);
			} else {
				spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
				toolBar.setCentroidButton(true);
				subTitle = isotopePattern.getDataFile().getFileName()
						+ " Scan "
						+ isotopePattern.getRepresentativeScanNumber();
			}

			toolBar.setPeaksButtonEnabled(false);

			// Set window and plot titles
			String title = "Isotope pattern of " + isotopePattern.toString();
			setTitle(title);
			spectrumPlot.setTitle(title, subTitle);
			spectrumPlot.getXYPlot().getDomainAxis().setRange(mzRange.getMin(),
					mzRange.getMax());

		}

		// Set plot data sets
		spectrumPlot.setSpectrumDataSet(spectrumDataSet);

	}

	public void loadPeaks() {

		PeakListDataSet peaksDataSet = null;
		
		if (bottomPanel == null)
			return;

		PeakList selectedPeakList = bottomPanel.getSelectedPeakList();

		if (selectedPeakList != null) {
			toolBar.setPeaksButtonEnabled(true);
			peaksDataSet = new PeakListDataSet(dataFile, currentScan
					.getScanNumber(), selectedPeakList);
			// Set plot data sets
			spectrumPlot.addPeaksDataSet(peaksDataSet);
		} else {
			toolBar.setPeaksButtonEnabled(false);
		}

	}

	public void loadIsotopePattern(IsotopePattern isotopePattern) {

		PeakListDataSet peaksDataSet = null;

		peaksDataSet = new PeakListDataSet(isotopePattern);

		if (peaksDataSet != null) {
			toolBar.setPeaksButtonEnabled(true);
			spectrumPlot.addPeaksDataSet(peaksDataSet);
		} else {
			// toolBar.setPeaksButtonEnabled(false);
		}

	}


	public void setAxesRange(double xMin, double xMax, double xTickSize,
			double yMin, double yMax, double yTickSize) {
		NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot()
				.getDomainAxis();
		NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
		xAxis.setRange(xMin, xMax);
		xAxis.setTickUnit(new NumberTickUnit(xTickSize));
		yAxis.setRange(yMin, yMax);
		yAxis.setTickUnit(new NumberTickUnit(yTickSize));
	}

	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		if (command.equals("PEAKLIST_CHANGE")) {

			// If no scan is loaded yet, ignore
			if (currentScan == null)
				return;

			PeakList selectedPeakList = bottomPanel.getSelectedPeakList();
			PeakListDataSet peaksDataSet = null;

			if (selectedPeakList != null) {
				logger.finest("Loading a peak list " + selectedPeakList
						+ " to a spectrum window " + getTitle());
				toolBar.setPeaksButtonEnabled(true);
				peaksDataSet = new PeakListDataSet(dataFile, currentScan
						.getScanNumber(), selectedPeakList);
				spectrumPlot.setSpectrumDataSet(spectrumDataSet);
				spectrumPlot.addPeaksDataSet(peaksDataSet);
			} else {
				toolBar.setPeaksButtonEnabled(false);
			}

		}

		if (command.equals("PREVIOUS_SCAN")) {

			if (dataFile == null)
				return;

			int msLevel = currentScan.getMSLevel();
			int scanNumbers[] = dataFile.getScanNumbers(msLevel);
			int scanIndex = Arrays.binarySearch(scanNumbers, currentScan
					.getScanNumber());
			if (scanIndex > 0) {
				final int prevScanIndex = scanNumbers[scanIndex - 1];

				Runnable newThreadRunnable = new Runnable() {

					public void run() {
						loadRawData(dataFile.getScan(prevScanIndex));
						loadPeaks();
					}

				};

				Thread newThread = new Thread(newThreadRunnable);
				newThread.start();

			}
		}

		if (command.equals("NEXT_SCAN")) {

			if (dataFile == null)
				return;

			int msLevel = currentScan.getMSLevel();
			int scanNumbers[] = dataFile.getScanNumbers(msLevel);
			int scanIndex = Arrays.binarySearch(scanNumbers, currentScan
					.getScanNumber());
			if (scanIndex < (scanNumbers.length - 1)) {
				final int nextScanIndex = scanNumbers[scanIndex + 1];

				Runnable newThreadRunnable = new Runnable() {

					public void run() {
						loadRawData(dataFile.getScan(nextScanIndex));
						loadPeaks();
					}

				};

				Thread newThread = new Thread(newThreadRunnable);
				newThread.start();

			}
		}

		if (command.equals("SHOW_MSMS")) {

			String selectedScanString = (String) bottomPanel.getMSMSSelector()
					.getSelectedItem();
			if (selectedScanString == null)
				return;

			int sharpIndex = selectedScanString.indexOf('#');
			int commaIndex = selectedScanString.indexOf(',');
			selectedScanString = selectedScanString.substring(sharpIndex + 1,
					commaIndex);
			int selectedScan = Integer.valueOf(selectedScanString);

			SpectraVisualizer specVis = SpectraVisualizer.getInstance();
			specVis.showNewSpectrumWindow(dataFile, selectedScan);
		}

	}

	public SpectraPlot getSpectrumPlot() {
		return spectrumPlot;
	}

}