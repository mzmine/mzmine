/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.OnlineDBSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionModule;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.manual.ManualPeakPickerModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerModule;
import net.sf.mzmine.modules.visualization.tic.PlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerModule;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;

/**
 * 
 */
public class PeakListTablePopupMenu extends JPopupMenu implements
		ActionListener {

	private PeakListTable table;
	private PeakList peakList;
	private PeakListTableColumnModel columnModel;

	private JMenu showMenu, searchMenu;
	private JMenuItem deleteRowsItem, addNewRowItem, plotRowsItem,
			showSpectrumItem, showXICItem, showXICSetupItem, showMSMSItem,
			showIsotopePatternItem, show2DItem, show3DItem, dbSearchItem,
			formulaItem, manuallyDefineItem, showPeakRowSummaryItem;

	private RawDataFile clickedDataFile;
	private PeakListRow clickedPeakListRow;
	private PeakListRow[] allClickedPeakListRows;

	public PeakListTablePopupMenu(PeakListTableWindow window,
			PeakListTable table, PeakListTableColumnModel columnModel,
			PeakList peakList) {

		this.table = table;
		this.peakList = peakList;
		this.columnModel = columnModel;

		showMenu = new JMenu("Show...");
		this.add(showMenu);

		showXICItem = GUIUtils.addMenuItem(showMenu, "Chromatogram (quick)",
				this);
		showXICSetupItem = GUIUtils.addMenuItem(showMenu,
				"Chromatogram (dialog)", this);
		showSpectrumItem = GUIUtils
				.addMenuItem(showMenu, "Mass spectrum", this);
		show2DItem = GUIUtils.addMenuItem(showMenu, "Peak in 2D", this);
		show3DItem = GUIUtils.addMenuItem(showMenu, "Peak in 3D", this);
		showMSMSItem = GUIUtils.addMenuItem(showMenu, "MS/MS", this);
		showIsotopePatternItem = GUIUtils.addMenuItem(showMenu,
				"Isotope pattern", this);
		showPeakRowSummaryItem = GUIUtils.addMenuItem(showMenu,
				"Peak row summary", this);

		searchMenu = new JMenu("Search...");
		this.add(searchMenu);

		if (OnlineDBSearchModule.getInstance() != null) {
			dbSearchItem = GUIUtils.addMenuItem(searchMenu,
					"Search online database", this);
		}

		if (FormulaPredictionModule.getInstance() != null) {
			formulaItem = GUIUtils.addMenuItem(searchMenu,
					"Predict molecular formula", this);
		}

		plotRowsItem = GUIUtils.addMenuItem(this,
				"Plot using Intensity Plot module", this);

		manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak",
				this);

		deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected rows",
				this);

		addNewRowItem = GUIUtils.addMenuItem(this, "Add new row", this);

	}

	public void show(Component invoker, int x, int y) {

		// First, disable all the Show... items
		show2DItem.setEnabled(false);
		show3DItem.setEnabled(false);
		manuallyDefineItem.setEnabled(false);
		showMSMSItem.setEnabled(false);
		showIsotopePatternItem.setEnabled(false);
		showPeakRowSummaryItem.setEnabled(false);

		// Enable row items if applicable
		int selectedRows[] = table.getSelectedRows();
		deleteRowsItem.setEnabled(selectedRows.length > 0);
		plotRowsItem.setEnabled(selectedRows.length > 0);

		// Find the row and column where the user clicked
		clickedDataFile = null;
		Point clickedPoint = new Point(x, y);
		int clickedRow = table.rowAtPoint(clickedPoint);
		int clickedColumn = columnModel.getColumn(
				table.columnAtPoint(clickedPoint)).getModelIndex();
		if ((clickedRow >= 0) && (clickedColumn >= 0)) {

			int rowIndex = table.convertRowIndexToModel(clickedRow);
			clickedPeakListRow = peakList.getRow(rowIndex);
			allClickedPeakListRows = new PeakListRow[selectedRows.length];
			for (int i = 0; i < selectedRows.length; i++) {
				allClickedPeakListRows[i] = peakList.getRow(table
						.convertRowIndexToModel(selectedRows[i]));
			}
			showXICItem.setEnabled(selectedRows.length > 0);

			// Enable Show 2D peak plot
			show2DItem.setEnabled(selectedRows.length == 1);

			// Enable Show 3D peak plot
			show3DItem.setEnabled(selectedRows.length == 1);

			// Enable peak list row summary
			showPeakRowSummaryItem.setEnabled(selectedRows.length == 1);

			// If we clicked on data file columns, check the peak
			if (clickedColumn >= CommonColumnType.values().length) {

				// Enable manual peak picking
				manuallyDefineItem.setEnabled(selectedRows.length == 1);

				// Find the actual peak, if we have it
				int dataFileIndex = (clickedColumn - CommonColumnType.values().length)
						/ DataFileColumnType.values().length;
				clickedDataFile = peakList.getRawDataFile(dataFileIndex);

				PeakListRow clickedPeakListRow = peakList.getRow(table
						.convertRowIndexToModel(clickedRow));
				ChromatographicPeak clickedPeak = clickedPeakListRow
						.getPeak(clickedDataFile);

				// If we have the peak, enable Show... items
				if ((clickedPeak != null) && (selectedRows.length == 1)) {
					showIsotopePatternItem.setEnabled(clickedPeak
							.getIsotopePattern() != null);
					showMSMSItem.setEnabled(clickedPeak
							.getMostIntenseFragmentScanNumber() > 0);
				}

			} else {

				ChromatographicPeak rowBestPeak = clickedPeakListRow
						.getBestPeak();
				IsotopePattern rowBestIsotopePattern = clickedPeakListRow
						.getBestIsotopePattern();
				showIsotopePatternItem.setEnabled(rowBestIsotopePattern != null
						&& selectedRows.length == 1);
				showMSMSItem.setEnabled(rowBestPeak
						.getMostIntenseFragmentScanNumber() > 0
						&& selectedRows.length == 1);

			}

		}

		super.show(invoker, x, y);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();

		if (src == deleteRowsItem) {

			int rowsToDelete[] = table.getSelectedRows();

			PeakListTableModel originalModel = (PeakListTableModel) table
					.getModel();

			int unsordedIndexes[] = new int[rowsToDelete.length];
			for (int i = rowsToDelete.length - 1; i >= 0; i--) {
				unsordedIndexes[i] = table
						.convertRowIndexToModel(rowsToDelete[i]);
			}

			// sort row indexes and start removing from the last
			Arrays.sort(unsordedIndexes);

			// delete the rows starting from last
			for (int i = unsordedIndexes.length - 1; i >= 0; i--) {
				peakList.removeRow(unsordedIndexes[i]);
			}
			originalModel.fireTableDataChanged();

			// Notify the GUI that peaklist contents have changed
			MZmineCore.getCurrentProject().notifyObjectChanged(peakList, true);

		}

		if (src == plotRowsItem) {

			int selectedTableRows[] = table.getSelectedRows();

			PeakListRow selectedRows[] = new PeakListRow[selectedTableRows.length];
			for (int i = 0; i < selectedTableRows.length; i++) {
				int unsortedIndex = table
						.convertRowIndexToModel(selectedTableRows[i]);
				selectedRows[i] = peakList.getRow(unsortedIndex);
			}
			IntensityPlotModule.showIntensityPlot(peakList, selectedRows);
		}

		if (src == showXICItem) {

			Range rtRange = null, mzRange = null;
			Vector<ChromatographicPeak> selectedPeaks = new Vector<ChromatographicPeak>();

			if (allClickedPeakListRows.length == 0)
				return;
			
			RawDataFile selectedDataFiles[] = new RawDataFile[1];
			selectedDataFiles[0] = clickedDataFile;
			if (selectedDataFiles[0] == null)
				selectedDataFiles[0] = allClickedPeakListRows[0].getBestPeak()
						.getDataFile();

			rtRange = selectedDataFiles[0].getDataRTRange(1);

			for (PeakListRow row : allClickedPeakListRows) {

				ChromatographicPeak peak = row.getPeak(selectedDataFiles[0]);
				if (peak == null)
					continue;

				selectedPeaks.add(peak);

				if (mzRange == null)
					mzRange = peak.getRawDataPointsMZRange();
				else
					mzRange.extendRange(peak.getRawDataPointsMZRange());

			}

			ChromatographicPeak peaks[] = selectedPeaks
					.toArray(new ChromatographicPeak[0]);

			TICVisualizerModule.showNewTICVisualizerWindow(selectedDataFiles,
					peaks, 1, PlotType.BASEPEAK, rtRange, mzRange);

			return;

		}

		if (src == showXICSetupItem) {

			Range rtRange = null, mzRange = null;

			if (allClickedPeakListRows.length == 0)
				return;
			
			RawDataFile selectedDataFiles[] = new RawDataFile[1];
			selectedDataFiles[0] = clickedDataFile;
			if (selectedDataFiles[0] == null)
				selectedDataFiles[0] = allClickedPeakListRows[0].getBestPeak()
						.getDataFile();

			rtRange = selectedDataFiles[0].getDataRTRange(1);

			for (PeakListRow row : allClickedPeakListRows) {

				for (ChromatographicPeak peak : row.getPeaks()) {

					if (mzRange == null)
						mzRange = peak.getRawDataPointsMZRange();
					else
						mzRange.extendRange(peak.getRawDataPointsMZRange());
				}
			}

			TICVisualizerModule.setupNewTICVisualizer(selectedDataFiles, 
					1, PlotType.BASEPEAK, rtRange, mzRange);

			return;

		}

		if (src == show2DItem) {

			ChromatographicPeak showPeak;

			if (clickedDataFile != null)
				showPeak = clickedPeakListRow.getPeak(clickedDataFile);
			else
				showPeak = clickedPeakListRow.getBestPeak();

			if (showPeak == null)
				return;

			Range peakRTRange = showPeak.getRawDataPointsRTRange();
			Range peakMZRange = showPeak.getRawDataPointsMZRange();
			Range rtRange = new Range(Math.max(0, peakRTRange.getMin()
					- peakRTRange.getSize()), peakRTRange.getMax()
					+ peakRTRange.getSize());

			Range mzRange = new Range(Math.max(0, peakMZRange.getMin()
					- peakMZRange.getSize()), peakMZRange.getMax()
					+ peakMZRange.getSize());
			TwoDVisualizerModule.show2DVisualizerSetupDialog(
					showPeak.getDataFile(), mzRange, rtRange);

		}

		if (src == show3DItem) {

			ChromatographicPeak showPeak;

			if (clickedDataFile != null)
				showPeak = clickedPeakListRow.getPeak(clickedDataFile);
			else
				showPeak = clickedPeakListRow.getBestPeak();

			if (showPeak == null)
				return;

			Range peakRTRange = showPeak.getRawDataPointsRTRange();
			Range peakMZRange = showPeak.getRawDataPointsMZRange();
			Range rtRange = new Range(Math.max(0, peakRTRange.getMin()
					- peakRTRange.getSize()), peakRTRange.getMax()
					+ peakRTRange.getSize());

			Range mzRange = new Range(Math.max(0, peakMZRange.getMin()
					- peakMZRange.getSize()), peakMZRange.getMax()
					+ peakMZRange.getSize());
			ThreeDVisualizerModule.setupNew3DVisualizer(
					showPeak.getDataFile(), mzRange, rtRange);

		}

		if (src == manuallyDefineItem) {
			ManualPeakPickerModule.runManualDetection(clickedDataFile,
					clickedPeakListRow);
		}

		if (src == showSpectrumItem) {

			ChromatographicPeak showPeak;

			if (clickedDataFile != null)
				showPeak = clickedPeakListRow.getPeak(clickedDataFile);
			else
				showPeak = clickedPeakListRow.getBestPeak();

			if (showPeak == null)
				return;

			SpectraVisualizerModule.showNewSpectrumWindow(
					showPeak.getDataFile(),
					showPeak.getRepresentativeScanNumber(), showPeak);
		}

		if (src == showMSMSItem) {

			ChromatographicPeak showPeak;

			if (clickedDataFile != null)
				showPeak = clickedPeakListRow.getPeak(clickedDataFile);
			else
				showPeak = clickedPeakListRow.getBestPeak();

			if (showPeak == null)
				return;

			int scanNumber = showPeak.getMostIntenseFragmentScanNumber();
			if (scanNumber > 0) {
				SpectraVisualizerModule.showNewSpectrumWindow(
						showPeak.getDataFile(), scanNumber);
			} else {
				MZmineCore.getDesktop().displayMessage(
						"There is no fragment for "
								+ MZmineCore.getMZFormat().format(showPeak.getMZ())
								+ " m/z in the current raw data.");
				return;
			}
		}

		if (src == showIsotopePatternItem) {

			ChromatographicPeak showPeak = null;

			if (clickedDataFile != null) {
				showPeak = clickedPeakListRow.getPeak(clickedDataFile);
			} else {
				showPeak = clickedPeakListRow.getBestPeak();
			}

			if ((showPeak == null) || (showPeak.getIsotopePattern() == null))
				return;

			SpectraVisualizerModule.showNewSpectrumWindow(
					showPeak.getDataFile(),
					showPeak.getRepresentativeScanNumber(),
					showPeak.getIsotopePattern());

		}

		if (src == formulaItem) {
			FormulaPredictionModule
					.showSingleRowIdentificationDialog(clickedPeakListRow);
		}

		if (src == dbSearchItem) {
			OnlineDBSearchModule
					.showSingleRowIdentificationDialog(clickedPeakListRow);
		}

		if (src == addNewRowItem) {

			// find maximum ID and add 1
			int newID = 1;
			for (PeakListRow row : peakList.getRows()) {
				if (row.getID() >= newID)
					newID = row.getID() + 1;
			}

			// create a new row
			SimplePeakListRow newRow = new SimplePeakListRow(newID);
			peakList.addRow(newRow);
			PeakListTableModel tableModel = (PeakListTableModel) table
					.getModel();
			tableModel.fireTableDataChanged();
			ManualPeakPickerModule.runManualDetection(
					peakList.getRawDataFiles(), newRow);

			// Notify the GUI that peaklist contents have changed
			MZmineCore.getCurrentProject().notifyObjectChanged(peakList, true);

		}

		if (src == showPeakRowSummaryItem) {
			PeakSummaryVisualizerModule
					.showNewPeakSummaryWindow(clickedPeakListRow);
		}

	}

}
