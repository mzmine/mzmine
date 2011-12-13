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

import net.sf.mzmine.data.*;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.OnlineDBSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionModule;
import net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchModule;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.manual.ManualPeakPickerModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import net.sf.mzmine.modules.visualization.peaklist.table.*;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerModule;
import net.sf.mzmine.modules.visualization.tic.PlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerModule;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Peak-list table pop-up menu.
 */
public class PeakListTablePopupMenu extends JPopupMenu implements ActionListener {

    private final PeakListTable table;
    private final PeakList peakList;
    private final PeakListTableColumnModel columnModel;

    private final JMenu showMenu;
    private final JMenu searchMenu;
    private final JMenuItem deleteRowsItem;
    private final JMenuItem addNewRowItem;
    private final JMenuItem plotRowsItem;
    private final JMenuItem showSpectrumItem;
    private final JMenuItem showXICItem;
    private final JMenuItem showXICSetupItem;
    private final JMenuItem showMSMSItem;
    private final JMenuItem showIsotopePatternItem;
    private final JMenuItem show2DItem;
    private final JMenuItem show3DItem;
    private final JMenuItem manuallyDefineItem;
    private final JMenuItem showPeakRowSummaryItem;
    private final JMenuItem clearIdsItem;
    private final JMenuItem dbSearchItem;
    private final JMenuItem formulaItem;
    private final JMenuItem nistSearchItem;

    private RawDataFile clickedDataFile;
    private PeakListRow clickedPeakListRow;
    private PeakListRow[] allClickedPeakListRows;

    public PeakListTablePopupMenu(final PeakListTable listTable,
                                  final PeakListTableColumnModel model,
                                  final PeakList list) {

        table = listTable;
        peakList = list;
        columnModel = model;

        clickedDataFile = null;
        clickedPeakListRow = null;
        allClickedPeakListRows = null;

        showMenu = new JMenu("Show...");
        add(showMenu);

        showXICItem = GUIUtils.addMenuItem(showMenu, "Chromatogram (quick)", this);
        showXICSetupItem = GUIUtils.addMenuItem(showMenu, "Chromatogram (dialog)", this);
        showSpectrumItem = GUIUtils.addMenuItem(showMenu, "Mass spectrum", this);
        show2DItem = GUIUtils.addMenuItem(showMenu, "Peak in 2D", this);
        show3DItem = GUIUtils.addMenuItem(showMenu, "Peak in 3D", this);
        showMSMSItem = GUIUtils.addMenuItem(showMenu, "MS/MS", this);
        showIsotopePatternItem = GUIUtils.addMenuItem(showMenu, "Isotope pattern", this);
        showPeakRowSummaryItem = GUIUtils.addMenuItem(showMenu, "Peak row summary", this);

        searchMenu = new JMenu("Search...");
        dbSearchItem = OnlineDBSearchModule.getInstance() == null ?
                       null :
                       GUIUtils.addMenuItem(searchMenu, "Search online database", this);
        nistSearchItem = NistMsSearchModule.getInstance() == null ?
                         null :
                         GUIUtils.addMenuItem(searchMenu, "NIST MS Search", this);
        formulaItem = FormulaPredictionModule.getInstance() == null ?
                      null :
                      GUIUtils.addMenuItem(searchMenu, "Predict molecular formula", this);
        if (searchMenu.getItemCount() > 0) {
            add(searchMenu);
        }

        plotRowsItem = GUIUtils.addMenuItem(this, "Plot using Intensity Plot module", this);
        manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak", this);
        clearIdsItem = GUIUtils.addMenuItem(this, "Clear selected rows' identities", this);
        deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected rows", this);
        addNewRowItem = GUIUtils.addMenuItem(this, "Add new row", this);
    }

    @Override
    public void show(final Component invoker, final int x, final int y) {

        // First, disable all the Show... items
        show2DItem.setEnabled(false);
        show3DItem.setEnabled(false);
        manuallyDefineItem.setEnabled(false);
        showMSMSItem.setEnabled(false);
        showIsotopePatternItem.setEnabled(false);
        showPeakRowSummaryItem.setEnabled(false);

        // Enable row items if applicable
        final int[] selectedRows = table.getSelectedRows();
        final boolean enabled = selectedRows.length > 0;
        deleteRowsItem.setEnabled(enabled);
        clearIdsItem.setEnabled(enabled);
        plotRowsItem.setEnabled(enabled);
        showMenu.setEnabled(enabled);

        final boolean oneRowSelected = selectedRows.length == 1;
        searchMenu.setEnabled(oneRowSelected);

        // Find the row and column where the user clicked
        clickedDataFile = null;
        final Point clickedPoint = new Point(x, y);
        final int clickedRow = table.rowAtPoint(clickedPoint);
        final int clickedColumn = columnModel.getColumn(table.columnAtPoint(clickedPoint)).getModelIndex();
        if (clickedRow >= 0 && clickedColumn >= 0) {

            final int rowIndex = table.convertRowIndexToModel(clickedRow);
            clickedPeakListRow = peakList.getRow(rowIndex);
            allClickedPeakListRows = new PeakListRow[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {

                allClickedPeakListRows[i] = peakList.getRow(table.convertRowIndexToModel(selectedRows[i]));
            }

            // Enable items.
            show2DItem.setEnabled(oneRowSelected);
            show3DItem.setEnabled(oneRowSelected);
            showPeakRowSummaryItem.setEnabled(oneRowSelected);

            // If we clicked on data file columns, check the peak
            if (clickedColumn >= CommonColumnType.values().length) {

                // Enable manual peak picking
                manuallyDefineItem.setEnabled(oneRowSelected);

                // Find the actual peak, if we have it.
                clickedDataFile = peakList.getRawDataFile(
                        (clickedColumn - CommonColumnType.values().length) / DataFileColumnType.values().length);

                final ChromatographicPeak clickedPeak =
                        peakList.getRow(table.convertRowIndexToModel(clickedRow)).getPeak(clickedDataFile);

                // If we have the peak, enable Show... items
                if (clickedPeak != null && oneRowSelected) {
                    showIsotopePatternItem.setEnabled(clickedPeak.getIsotopePattern() != null);
                    showMSMSItem.setEnabled(clickedPeak.getMostIntenseFragmentScanNumber() > 0);
                }

            } else {

                showIsotopePatternItem.setEnabled(
                        clickedPeakListRow.getBestIsotopePattern() != null && oneRowSelected);
                showMSMSItem.setEnabled(
                        clickedPeakListRow.getBestPeak().getMostIntenseFragmentScanNumber() > 0 && oneRowSelected);
            }
        }

        super.show(invoker, x, y);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        final Object src = e.getSource();

        if (deleteRowsItem.equals(src)) {

            final int[] rowsToDelete = table.getSelectedRows();

            final int[] unsortedIndexes = new int[rowsToDelete.length];
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                unsortedIndexes[i] = table.convertRowIndexToModel(rowsToDelete[i]);
            }

            // sort row indexes and start removing from the last
            Arrays.sort(unsortedIndexes);

            // delete the rows starting from last
            for (int i = unsortedIndexes.length - 1; i >= 0; i--) {
                peakList.removeRow(unsortedIndexes[i]);
            }

            // Notify the GUI that peaklist contents have changed
            ((AbstractTableModel) table.getModel()).fireTableDataChanged();
            MZmineCore.getCurrentProject().notifyObjectChanged(peakList, true);
        }

        if (clearIdsItem.equals(src)) {

            // Delete identities of selected rows.
            for (final int rowIndex : table.getSelectedRows()) {

                // Selected row index.
                final PeakListRow row = peakList.getRow(table.convertRowIndexToModel(rowIndex));
                for (final PeakIdentity id : row.getPeakIdentities()) {

                    // Remove id.
                    row.removePeakIdentity(id);
                }
            }

            // Update table GUI.
            ((AbstractTableModel) table.getModel()).fireTableDataChanged();
            MZmineCore.getCurrentProject().notifyObjectChanged(peakList, true);
        }

        if (plotRowsItem.equals(src)) {

            final int[] selectedTableRows = table.getSelectedRows();

            final PeakListRow[] selectedRows = new PeakListRow[selectedTableRows.length];
            for (int i = 0; i < selectedTableRows.length; i++) {

                selectedRows[i] = peakList.getRow(table.convertRowIndexToModel(selectedTableRows[i]));
            }
            IntensityPlotModule.showIntensityPlot(peakList, selectedRows);
        }

        if (showXICItem.equals(src) && allClickedPeakListRows.length != 0) {

            // Map peaks to their identity labels.
            Map<ChromatographicPeak, String> labelsMap =
                    new HashMap<ChromatographicPeak, String>(allClickedPeakListRows.length);

            final RawDataFile selectedDataFile =
                    clickedDataFile == null ? allClickedPeakListRows[0].getBestPeak().getDataFile() : clickedDataFile;

            Range mzRange = null;
            final List<ChromatographicPeak> selectedPeaks =
                    new ArrayList<ChromatographicPeak>(allClickedPeakListRows.length);
            for (final PeakListRow row : allClickedPeakListRows) {

                final ChromatographicPeak peak = row.getPeak(selectedDataFile);
                if (peak != null) {

                    selectedPeaks.add(peak);

                    if (mzRange == null) {
                        mzRange = peak.getRawDataPointsMZRange();
                    } else {
                        mzRange.extendRange(peak.getRawDataPointsMZRange());
                    }

                    // Label the peak with the row's preferred identity.
                    PeakIdentity identity = row.getPreferredPeakIdentity();
                    if (identity != null) {
                        labelsMap.put(peak, identity.getName());
                    }
                }
            }

            TICVisualizerModule.showNewTICVisualizerWindow(
                    new RawDataFile[]{selectedDataFile},
                    selectedPeaks.toArray(new ChromatographicPeak[selectedPeaks.size()]),
                    labelsMap,
                    1,
                    PlotType.BASEPEAK,
                    selectedDataFile.getDataRTRange(1),
                    mzRange);
        }

        if (showXICSetupItem.equals(src) && allClickedPeakListRows.length != 0) {

            // Map peaks to their identity labels.
            Map<ChromatographicPeak, String> labelsMap =
                    new HashMap<ChromatographicPeak, String>(allClickedPeakListRows.length);

            final RawDataFile[] selectedDataFiles =
                    clickedDataFile == null ? peakList.getRawDataFiles() : new RawDataFile[]{clickedDataFile};

            Range mzRange = null;
            final ArrayList<ChromatographicPeak> allClickedPeaks =
                    new ArrayList<ChromatographicPeak>(allClickedPeakListRows.length);
            final ArrayList<ChromatographicPeak> selectedClickedPeaks =
                    new ArrayList<ChromatographicPeak>(allClickedPeakListRows.length);
            for (final PeakListRow row : allClickedPeakListRows) {

                // Label the peak with the row's preferred identity.
                PeakIdentity identity = row.getPreferredPeakIdentity();

                for (final ChromatographicPeak peak : row.getPeaks()) {

                    allClickedPeaks.add(peak);
                    if (peak.getDataFile() == clickedDataFile) {
                        selectedClickedPeaks.add(peak);
                    }

                    if (mzRange == null) {
                        mzRange = peak.getRawDataPointsMZRange();
                    } else {
                        mzRange.extendRange(peak.getRawDataPointsMZRange());
                    }

                    if (identity != null) {
                        labelsMap.put(peak, identity.getName());
                    }
                }
            }

            TICVisualizerModule.setupNewTICVisualizer(
                    MZmineCore.getCurrentProject().getDataFiles(),
                    selectedDataFiles,
                    allClickedPeaks.toArray(new ChromatographicPeak[allClickedPeaks.size()]),
                    selectedClickedPeaks.toArray(new ChromatographicPeak[selectedClickedPeaks.size()]),
                    labelsMap,
                    selectedDataFiles[0].getDataRTRange(1),
                    mzRange);
        }

        if (show2DItem.equals(src)) {

            final ChromatographicPeak showPeak = getSelectedPeak();
            if (showPeak != null) {

                TwoDVisualizerModule.show2DVisualizerSetupDialog(
                        showPeak.getDataFile(), getPeakMZRange(showPeak), getPeakRTRange(showPeak));
            }
        }

        if (show3DItem.equals(src)) {

            final ChromatographicPeak showPeak = getSelectedPeak();
            if (showPeak != null) {

                ThreeDVisualizerModule.setupNew3DVisualizer(
                        showPeak.getDataFile(), getPeakMZRange(showPeak), getPeakRTRange(showPeak));
            }
        }

        if (manuallyDefineItem.equals(src)) {

            ManualPeakPickerModule.runManualDetection(clickedDataFile, clickedPeakListRow);
        }

        if (showSpectrumItem.equals(src)) {

            final ChromatographicPeak showPeak = getSelectedPeak();
            if (showPeak != null) {

                SpectraVisualizerModule.showNewSpectrumWindow(
                        showPeak.getDataFile(),
                        showPeak.getRepresentativeScanNumber(),
                        showPeak);
            }
        }

        if (showMSMSItem.equals(src)) {

            final ChromatographicPeak showPeak = getSelectedPeak();
            if (showPeak != null) {

                final int scanNumber = showPeak.getMostIntenseFragmentScanNumber();
                if (scanNumber > 0) {
                    SpectraVisualizerModule.showNewSpectrumWindow(showPeak.getDataFile(), scanNumber);
                } else {
                    MZmineCore.getDesktop().displayMessage(
                            "There is no fragment for " + MZmineCore.getMZFormat().format(showPeak.getMZ())
                            + " m/z in the current raw data.");
                }
            }
        }

        if (showIsotopePatternItem.equals(src)) {

            final ChromatographicPeak showPeak = getSelectedPeak();
            if (showPeak != null && showPeak.getIsotopePattern() != null) {

                SpectraVisualizerModule.showNewSpectrumWindow(
                        showPeak.getDataFile(),
                        showPeak.getRepresentativeScanNumber(),
                        showPeak.getIsotopePattern());
            }
        }

        if (formulaItem != null && formulaItem.equals(src)) {

            FormulaPredictionModule.showSingleRowIdentificationDialog(clickedPeakListRow);
        }

        if (dbSearchItem != null && dbSearchItem.equals(src)) {

            OnlineDBSearchModule.showSingleRowIdentificationDialog(clickedPeakListRow);
        }

        if (nistSearchItem != null && nistSearchItem.equals(src)) {

            NistMsSearchModule.singleRowSearch(peakList, clickedPeakListRow);
        }

        if (addNewRowItem.equals(src)) {

            // find maximum ID and add 1
            int newID = 1;
            for (final PeakListRow row : peakList.getRows()) {
                if (row.getID() >= newID) {
                    newID = row.getID() + 1;
                }
            }

            // create a new row
            final PeakListRow newRow = new SimplePeakListRow(newID);
            peakList.addRow(newRow);
            final PeakListTableModel tableModel = (PeakListTableModel) table.getModel();
            tableModel.fireTableDataChanged();
            ManualPeakPickerModule.runManualDetection(peakList.getRawDataFiles(), newRow);

            // Notify the GUI that peaklist contents have changed
            MZmineCore.getCurrentProject().notifyObjectChanged(peakList, true);
        }

        if (showPeakRowSummaryItem.equals(src)) {

            PeakSummaryVisualizerModule.showNewPeakSummaryWindow(clickedPeakListRow);
        }
    }

    /**
     * Get a peak's m/z range.
     *
     * @param peak the peak.
     * @return The peak's m/z range.
     */
    private static Range getPeakMZRange(final ChromatographicPeak peak) {

        final Range range = peak.getRawDataPointsMZRange();
        return new Range(Math.max(0.0, range.getMin() - range.getSize()), range.getMax() + range.getSize());
    }

    /**
     * Get a peak's RT range.
     *
     * @param peak the peak.
     * @return The peak's RT range.
     */
    private static Range getPeakRTRange(final ChromatographicPeak peak) {

        final Range range = peak.getRawDataPointsRTRange();
        return new Range(Math.max(0.0, range.getMin() - range.getSize()), range.getMax() + range.getSize());
    }

    /**
     * Get the selected peak.
     *
     * @return the peak.
     */
    private ChromatographicPeak getSelectedPeak() {

        return clickedDataFile != null ? clickedPeakListRow.getPeak(clickedDataFile) : clickedPeakListRow.getBestPeak();
    }
}
