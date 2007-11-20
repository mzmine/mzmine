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

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlot;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotDialog;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotFrame;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.modules.visualization.tic.TICSetupDialog;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.NumberFormatter;

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

    private JMenuItem deleteRowsItem;
    private JMenuItem plotRowsItem;
    private JMenuItem showXICItem;
    private JMenuItem manuallyDefineItem;

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

        plotRowsItem = GUIUtils.addMenuItem(this,
                "Plot selected rows using Intensity Plot module", this);

        showXICItem = GUIUtils.addMenuItem(this, "Show XIC of this peak", this);

        manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak",
                this);

    }

    public void show(Component invoker, int x, int y) {

        int selectedRows[] = table.getSelectedRows();

        deleteRowsItem.setEnabled(selectedRows.length > 0);
        plotRowsItem.setEnabled(selectedRows.length > 0);

        Point clickedPoint = new Point(x, y);
        int clickedRow = table.rowAtPoint(clickedPoint);
        int clickedColumn = columnModel.getColumn(
                table.columnAtPoint(clickedPoint)).getModelIndex();
        if ((clickedRow >= 0) && (clickedColumn >= 0)) {
            showXICItem.setEnabled(clickedColumn >= CommonColumnType.values().length);
            manuallyDefineItem.setEnabled(clickedColumn >= CommonColumnType.values().length);
            int dataFileIndex = (clickedColumn - CommonColumnType.values().length)
                    / DataFileColumnType.values().length;
            clickedDataFile = peakList.getRawDataFile(dataFileIndex);
            TableSorter sorter = (TableSorter) table.getModel();
            clickedPeakListRow = peakList.getRow(sorter.modelIndex(clickedRow));
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

            Peak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);
            JDialog setupDialog;
            if (clickedPeak != null) {
                setupDialog = new TICSetupDialog(clickedDataFile,
                        clickedPeak.getDataPointMinMZ(),
                        clickedPeak.getDataPointMaxMZ(),
                        new Peak[] { clickedPeak });

            } else {
                float minMZ = clickedPeakListRow.getAverageMZ();
                float maxMZ = clickedPeakListRow.getAverageMZ();
                for (Peak peak : clickedPeakListRow.getPeaks()) {
                    if (peak == null)
                        continue;
                    if (peak.getDataPointMinMZ() < minMZ)
                        minMZ = peak.getDataPointMinMZ();
                    if (peak.getDataPointMaxMZ() > maxMZ)
                        maxMZ = peak.getDataPointMaxMZ();
                }
                setupDialog = new TICSetupDialog(clickedDataFile, minMZ, maxMZ);
            }
            setupDialog.setVisible(true);

        }

        if (src == manuallyDefineItem) {

            Peak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);
            float minRT, maxRT, minMZ, maxMZ;
            if (clickedPeak != null) {
                minRT = clickedPeak.getDataPointMinRT();
                maxRT = clickedPeak.getDataPointMaxRT();
                minMZ = clickedPeak.getDataPointMinMZ();
                maxMZ = clickedPeak.getDataPointMaxMZ();
            } else {
                minRT = clickedPeakListRow.getAverageRT();
                maxRT = clickedPeakListRow.getAverageRT();
                minMZ = clickedPeakListRow.getAverageMZ();
                maxMZ = clickedPeakListRow.getAverageMZ();

                for (Peak peak : clickedPeakListRow.getPeaks()) {
                    if (peak == null)
                        continue;
                    if (peak.getDataPointMinRT() < minRT)
                        minRT = peak.getDataPointMinRT();
                    if (peak.getDataPointMaxRT() > maxRT)
                        maxRT = peak.getDataPointMaxRT();
                    if (peak.getDataPointMinMZ() < minMZ)
                        minMZ = peak.getDataPointMinMZ();
                    if (peak.getDataPointMaxMZ() > maxMZ)
                        maxMZ = peak.getDataPointMaxMZ();
                }
            }

            NumberFormatter mzFormat = MZmineCore.getDesktop().getMZFormat();
            NumberFormatter rtFormat = MZmineCore.getDesktop().getRTFormat();

            Parameter minRTparam = new SimpleParameter(ParameterType.FLOAT,
                    "Retention time min", "Retention time min", "s", minRT,
                    clickedDataFile.getDataMinRT(1),
                    clickedDataFile.getDataMaxRT(1), rtFormat);
            Parameter maxRTparam = new SimpleParameter(ParameterType.FLOAT,
                    "Retention time max", "Retention time max", "s", maxRT,
                    clickedDataFile.getDataMinRT(1),
                    clickedDataFile.getDataMaxRT(1), rtFormat);
            Parameter minMZparam = new SimpleParameter(ParameterType.FLOAT,
                    "m/z min", "m/z min", "Da", minMZ,
                    clickedDataFile.getDataMinMZ(1),
                    clickedDataFile.getDataMaxMZ(1), mzFormat);
            Parameter maxMZparam = new SimpleParameter(ParameterType.FLOAT,
                    "m/z max", "m/z max", "Da", maxMZ,
                    clickedDataFile.getDataMinMZ(1),
                    clickedDataFile.getDataMaxMZ(1), mzFormat);
            Parameter[] params = { minRTparam, maxRTparam, minMZparam,
                    maxMZparam };

            SimpleParameterSet parameterSet = new SimpleParameterSet(params);

            ParameterSetupDialog parameterSetupDialog = new ParameterSetupDialog(
                    MZmineCore.getDesktop().getMainFrame(),
                    "Please set peak boundaries", parameterSet);

            parameterSetupDialog.setVisible(true);

            if (parameterSetupDialog.getExitCode() != ExitCode.OK)
                return;

            minRT = (Float) parameterSet.getParameterValue(minRTparam);
            maxRT = (Float) parameterSet.getParameterValue(maxRTparam);
            minMZ = (Float) parameterSet.getParameterValue(minMZparam);
            maxMZ = (Float) parameterSet.getParameterValue(maxMZparam);

            ManuallyDefinePeakTask task = new ManuallyDefinePeakTask(
                    clickedPeakListRow, clickedDataFile, minRT, maxRT, minMZ,
                    maxMZ);

            MZmineCore.getTaskController().addTask(task);
        }

    }

}
