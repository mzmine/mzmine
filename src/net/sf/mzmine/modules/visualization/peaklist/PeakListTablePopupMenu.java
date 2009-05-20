/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
import java.text.NumberFormat;
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
import net.sf.mzmine.modules.identification.pubchem.PubChemSearch;
import net.sf.mzmine.modules.peakpicking.manual.ManualPeakPicker;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlot;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerParameters;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizer;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;
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
			showSpectrumItem, showXICItem, showMSMSItem,
			showIsotopePatternItem, show2DItem, show3DItem, pubChemSearchItem,
			manuallyDefineItem, showPeakRowSummaryItem;

	private RawDataFile clickedDataFile;
	private PeakListRow clickedPeakListRow;
	private PeakListRow[] allClickedPeakListRows;

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	public PeakListTablePopupMenu(PeakListTableWindow window,
			PeakListTable table, PeakListTableColumnModel columnModel,
			PeakList peakList) {

		this.table = table;
		this.peakList = peakList;
		this.columnModel = columnModel;

		showMenu = new JMenu("Show...");
		this.add(showMenu);

		showXICItem = GUIUtils.addMenuItem(showMenu, "Chromatogram", this);
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

		pubChemSearchItem = GUIUtils.addMenuItem(searchMenu,
				"Search in PubChem", this);

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
		showXICItem.setEnabled(false);
		manuallyDefineItem.setEnabled(false);
		showSpectrumItem.setEnabled(false);
		show2DItem.setEnabled(false);
		show3DItem.setEnabled(false);
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
					showSpectrumItem.setEnabled(true);
					show2DItem.setEnabled(true);
					show3DItem.setEnabled(true);
					showIsotopePatternItem
							.setEnabled(clickedPeak instanceof IsotopePattern);
					showMSMSItem.setEnabled(clickedPeak
							.getMostIntenseFragmentScanNumber() > 0);
				}

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

			// Notify the project manager that peaklist contents have changed
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.PEAKLIST_CONTENTS_CHANGED, peakList);
			MZmineCore.getProjectManager().fireProjectListeners(newEvent);

		}

		if (src == plotRowsItem) {

			int selectedTableRows[] = table.getSelectedRows();

			PeakListRow selectedRows[] = new PeakListRow[selectedTableRows.length];
			for (int i = 0; i < selectedTableRows.length; i++) {
				int unsortedIndex = table
						.convertRowIndexToModel(selectedTableRows[i]);
				selectedRows[i] = peakList.getRow(unsortedIndex);
			}
			IntensityPlot.showIntensityPlot(peakList, selectedRows);
		}

		if (src == showXICItem) {

			Range rtRange = null, mzRange = null;
			Vector<ChromatographicPeak> selectedPeaks = new Vector<ChromatographicPeak>();
			Vector<ChromatographicPeak> dataFilePeaks = new Vector<ChromatographicPeak>();
			ChromatographicPeak[] peaks, preSelectedPeaks;
			RawDataFile selectedDataFiles[] = peakList.getRawDataFiles();
			if (clickedDataFile != null)
				selectedDataFiles = new RawDataFile[] { clickedDataFile };

			// Check if we clicked on a raw data file XIC, or combined XIC
			for (PeakListRow row : allClickedPeakListRows) {

				for (RawDataFile dataFile : row.getRawDataFiles()) {
					if (rtRange == null)
						rtRange = dataFile.getDataRTRange(1);
					else
						rtRange.extendRange(dataFile.getDataRTRange(1));
				}
				for (ChromatographicPeak peak : row.getPeaks()) {
					selectedPeaks.add(peak);
					if (clickedDataFile != null)
						if (clickedDataFile == peak.getDataFile())
							dataFilePeaks.add(peak);
					if (mzRange == null)
						mzRange = peak.getRawDataPointsMZRange();
					else
						mzRange.extendRange(peak.getRawDataPointsMZRange());
				}
			}

			peaks = selectedPeaks.toArray(new ChromatographicPeak[0]);

			if (clickedDataFile != null)
				preSelectedPeaks = dataFilePeaks
						.toArray(new ChromatographicPeak[0]);
			else
				preSelectedPeaks = peaks;

			TICVisualizer.showNewTICVisualizerWindow(selectedDataFiles, peaks,
					preSelectedPeaks, 1, TICVisualizerParameters.plotTypeBP,
					rtRange, mzRange);

			return;

		}

		if (src == show2DItem) {

			ChromatographicPeak clickedPeak = clickedPeakListRow
					.getPeak(clickedDataFile);

			if (clickedPeak != null) {
				Range peakRTRange = clickedPeak.getRawDataPointsRTRange();
				Range peakMZRange = clickedPeak.getRawDataPointsMZRange();
				Range rtRange = new Range(Math.max(0, peakRTRange.getMin()
						- peakRTRange.getSize()), peakRTRange.getMax()
						+ peakRTRange.getSize());

				Range mzRange = new Range(Math.max(0, peakMZRange.getMin()
						- peakMZRange.getSize()), peakMZRange.getMax()
						+ peakMZRange.getSize());
				TwoDVisualizer.show2DVisualizerSetupDialog(clickedDataFile,
						mzRange, rtRange);

			}
		}

		if (src == show3DItem) {

			ChromatographicPeak clickedPeak = clickedPeakListRow
					.getPeak(clickedDataFile);

			if (clickedPeak != null) {
				Range peakRTRange = clickedPeak.getRawDataPointsRTRange();
				Range peakMZRange = clickedPeak.getRawDataPointsMZRange();
				Range rtRange = new Range(Math.max(0, peakRTRange.getMin()
						- peakRTRange.getSize()), peakRTRange.getMax()
						+ peakRTRange.getSize());

				Range mzRange = new Range(Math.max(0, peakMZRange.getMin()
						- peakMZRange.getSize()), peakMZRange.getMax()
						+ peakMZRange.getSize());
				ThreeDVisualizer.show3DVisualizerSetupDialog(clickedDataFile,
						mzRange, rtRange);

			}
		}

		if (src == manuallyDefineItem) {
			ManualPeakPicker.runManualDetection(clickedDataFile,
					clickedPeakListRow);
		}

		if (src == showSpectrumItem) {
			ChromatographicPeak clickedPeak = clickedPeakListRow
					.getPeak(clickedDataFile);
			SpectraVisualizer.showNewSpectrumWindow(clickedDataFile,
					clickedPeak.getRepresentativeScanNumber());
		}

		if (src == showMSMSItem) {

			ChromatographicPeak clickedPeak = clickedPeakListRow
					.getPeak(clickedDataFile);

			if (clickedPeak != null) {
				int scanNumber = clickedPeak.getMostIntenseFragmentScanNumber();
				if (scanNumber > 0) {
					SpectraVisualizer.showNewSpectrumWindow(clickedDataFile,
							scanNumber);
				} else {
					MZmineCore.getDesktop().displayMessage(
							"There is no fragment for the mass "
									+ massFormater.format(clickedPeak.getMZ())
									+ "m/z in the current raw data.");
					return;
				}
			}
		}

		if (src == showIsotopePatternItem) {

			ChromatographicPeak clickedPeak = clickedPeakListRow
					.getPeak(clickedDataFile);

			if (clickedPeak != null) {
				if (clickedPeak instanceof IsotopePattern)
					SpectraVisualizer.showIsotopePattern(clickedDataFile,
							(IsotopePattern) clickedPeak);
			}
		}

		if (src == pubChemSearchItem) {
			PubChemSearch.showPubChemSingleRowIdentificationDialog(clickedPeakListRow);
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
			ManualPeakPicker.runManualDetection(peakList.getRawDataFiles(),
					newRow);

			// Notify the project manager that peaklist contents have changed
			ProjectEvent newEvent = new ProjectEvent(
					ProjectEventType.PEAKLIST_CONTENTS_CHANGED, peakList);
			MZmineCore.getProjectManager().fireProjectListeners(newEvent);

		}

		if (src == showPeakRowSummaryItem) {
			PeakSummaryVisualizer.showNewPeakSummaryWindow(clickedPeakListRow);
		}

	}

}
