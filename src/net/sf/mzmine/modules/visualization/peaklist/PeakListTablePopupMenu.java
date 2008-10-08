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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.identification.pubchem.PubChemSearch;
import net.sf.mzmine.modules.peakpicking.manual.ManualPeakPicker;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlot;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotDialog;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotFrame;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerParameters;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

import com.sun.java.TableSorter;

/**
 * 
 */
public class PeakListTablePopupMenu extends JPopupMenu implements
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakListTable table;
    private PeakList peakList;
    private PeakListTableColumnModel columnModel;

    private JMenuItem deleteRowsItem, addNewRowItem, plotRowsItem, showXICItem,
            manuallyDefineItem, showFragmentation, showIsotopePattern, pubChemSearch;

    private RawDataFile clickedDataFile;
    private PeakListRow clickedPeakListRow;

    public PeakListTablePopupMenu(PeakListTableWindow window,
            PeakListTable table, PeakListTableColumnModel columnModel,
            PeakList peakList) {

        this.table = table;
        this.peakList = peakList;
        this.columnModel = columnModel;

        GUIUtils.addMenuItem(this, "Set properties", window, "PROPERTIES");

        deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected rows",
                this);

        addNewRowItem = GUIUtils.addMenuItem(this, "Add new row", this);

        plotRowsItem = GUIUtils.addMenuItem(this,
                "Plot selected rows using Intensity Plot module", this);

        showXICItem = GUIUtils.addMenuItem(this, "Show XIC of this peak", this);

        manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak",
                this);
        
        showFragmentation = GUIUtils.addMenuItem(this, "Show spectra fragmentation",
                this);
        
        showIsotopePattern = GUIUtils.addMenuItem(this, "Show Isotope pattern",
                this);
        
        pubChemSearch = GUIUtils.addMenuItem(this, "Search identity in PubChem",
                this);

    }

    public void show(Component invoker, int x, int y) {

        int selectedRows[] = table.getSelectedRows();
        boolean display;

        deleteRowsItem.setEnabled(selectedRows.length > 0);
        plotRowsItem.setEnabled(selectedRows.length > 0);

        Point clickedPoint = new Point(x, y);
        int clickedRow = table.rowAtPoint(clickedPoint);
        int clickedColumn = columnModel.getColumn(
                table.columnAtPoint(clickedPoint)).getModelIndex();
        if ((clickedRow >= 0) && (clickedColumn >= 0)) {
        	
        	display = clickedColumn >= CommonColumnType.values().length;
        	
            showXICItem.setEnabled((clickedColumn == CommonColumnType.PEAKSHAPE.ordinal())
                    || (display));
            manuallyDefineItem.setEnabled(display);
            pubChemSearch.setEnabled(display);
            showFragmentation.setEnabled(display);
            
            if (display) {
                int dataFileIndex = (clickedColumn - CommonColumnType.values().length)
                        / DataFileColumnType.values().length;
                clickedDataFile = peakList.getRawDataFile(dataFileIndex);
            } else
                clickedDataFile = null;
            TableSorter sorter = (TableSorter) table.getModel();
            clickedPeakListRow = peakList.getRow(sorter.modelIndex(clickedRow));
            
            if (clickedDataFile != null)
            	showIsotopePattern.setEnabled(clickedPeakListRow.getPeak(clickedDataFile) instanceof IsotopePattern);
            else
            	showIsotopePattern.setEnabled(false);
            
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
            // sort row indices
            Arrays.sort(rowsToDelete);
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();

            // delete the rows starting from last
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                int unsordedIndex = sorterModel.modelIndex(rowsToDelete[i]);
                peakList.removeRow(unsordedIndex);
                originalModel.fireTableRowsDeleted(unsordedIndex, unsordedIndex);
            }

            table.clearSelection();

        }

        if (src == plotRowsItem) {

            int selectedTableRows[] = table.getSelectedRows();
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListRow selectedPeakListRows[] = new PeakListRow[selectedTableRows.length];
            for (int i = 0; i < selectedTableRows.length; i++) {
                int unsortedIndex = sorterModel.modelIndex(selectedTableRows[i]);
                selectedPeakListRows[i] = peakList.getRow(unsortedIndex);
            }
            IntensityPlot intensityPlotModule = IntensityPlot.getInstance();
            IntensityPlotParameters currentParameters = intensityPlotModule.getParameterSet();
            IntensityPlotParameters newParameters = new IntensityPlotParameters(
                    peakList, currentParameters.getXAxisValueSource(),
                    currentParameters.getYAxisValueSource(),
                    peakList.getRawDataFiles(), selectedPeakListRows);
            IntensityPlotDialog setupDialog = new IntensityPlotDialog(peakList,
                    newParameters);
            setupDialog.setVisible(true);
            if (setupDialog.getExitCode() == ExitCode.OK) {
                intensityPlotModule.setParameters(newParameters);
                Desktop desktop = MZmineCore.getDesktop();
                logger.info("Opening new intensity plot");
                IntensityPlotFrame newFrame = new IntensityPlotFrame(
                        newParameters);
                desktop.addInternalFrame(newFrame);
            }

        }

        if (src == showXICItem) {

            TICVisualizer tic = TICVisualizer.getInstance();

            // Check if we clicked on a raw data file XIC, or combined XIC
            if (clickedDataFile == null) {
                Range rtRange = null, mzRange = null;
                for (RawDataFile dataFile : clickedPeakListRow.getRawDataFiles()) {
                    if (rtRange == null)
                        rtRange = dataFile.getDataRTRange(1);
                    else
                        rtRange.extendRange(dataFile.getDataRTRange(1));
                }
                for (ChromatographicPeak peak : clickedPeakListRow.getPeaks()) {
                    if (mzRange == null)
                        mzRange = peak.getRawDataPointsMZRange();
                    else
                        mzRange.extendRange(peak.getRawDataPointsMZRange());
                }
                tic.showNewTICVisualizerWindow(
                        clickedPeakListRow.getRawDataFiles(),
                        clickedPeakListRow.getPeaks(), 1,
                        TICVisualizerParameters.plotTypeBP, rtRange, mzRange);
                return;
            }

            ChromatographicPeak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);

            if (clickedPeak != null) {
                tic.showNewTICVisualizerWindow(
                        new RawDataFile[] { clickedDataFile },
                        new ChromatographicPeak[] { clickedPeak }, 1,
                        TICVisualizerParameters.plotTypeBP,
                        clickedDataFile.getDataRTRange(1),
                        clickedPeak.getRawDataPointsMZRange());

            } else {
                Range mzRange = new Range(clickedPeakListRow.getAverageMZ());

                for (ChromatographicPeak peak : clickedPeakListRow.getPeaks()) {
                    if (peak == null)
                        continue;
                    mzRange.extendRange(peak.getRawDataPointsMZRange());

                }
                tic.showNewTICVisualizerWindow(
                        new RawDataFile[] { clickedDataFile }, null, 1,
                        TICVisualizerParameters.plotTypeBP,
                        clickedDataFile.getDataRTRange(1), mzRange);
            }

        }

        if (src == manuallyDefineItem) {
            ManualPeakPicker.runManualDetection(clickedDataFile,
                    clickedPeakListRow);
        }
        
        if (src == showFragmentation) {

            SpectraVisualizer specVis = SpectraVisualizer.getInstance();

            ChromatographicPeak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);

            if (clickedPeak != null) {
            	int scanNumber = clickedPeak.getMostIntenseFragmentScanNumber();
            	if (scanNumber > 0)
                specVis.showNewSpectrumWindow(clickedDataFile, scanNumber);
            } 
        }

        if (src == showIsotopePattern) {

            SpectraVisualizer specVis = SpectraVisualizer.getInstance();

            ChromatographicPeak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);

            if (clickedPeak != null) {
            	if (clickedPeak instanceof IsotopePattern)
                specVis.showNewSpectrumWindow(clickedDataFile, (IsotopePattern) clickedPeak);
            } 
        }

        if (src == pubChemSearch) {

        	PubChemSearch pubChem = PubChemSearch.getInstance();
        	
        	ChromatographicPeak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);

            if (clickedPeak != null) {
            	
            	pubChem.showPubChemSearchDialog(peakList, clickedPeakListRow, clickedPeak);
            } 
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
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();
            originalModel.fireTableDataChanged();
            ManualPeakPicker.runManualDetection(peakList.getRawDataFiles(),
                    newRow);
        }

    }

}
