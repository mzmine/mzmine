/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.impl.projecttree;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.io.FilenameUtils;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.exportscans.ExportScansModule;
import io.github.mzmine.modules.io.rawdataexport.RawDataExportModule;
import io.github.mzmine.modules.tools.sortdatafiles.SortDataFilesModule;
import io.github.mzmine.modules.tools.sortdatafiles.SortDataFilesParameters;
import io.github.mzmine.modules.tools.sortpeaklists.SortPeakListsModule;
import io.github.mzmine.modules.tools.sortpeaklists.SortPeakListsParameters;
import io.github.mzmine.modules.visualization.featurelisttable.PeakListTableModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.infovisualizer.InfoVisualizerModule;
import io.github.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizerModule;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.msms.MsMsVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.tic.TICVisualizerModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.GUIUtils;

/**
 * This class handles pop-up menus and double click events in the project tree
 */
public class ProjectTreeMouseHandler extends MouseAdapter
        implements ActionListener {

    private ProjectTree tree;
    private JPopupMenu dataFilePopupMenu, peakListPopupMenu, scanPopupMenu,
            massListPopupMenu, peakListRowPopupMenu;

    /**
     * Constructor
     */
    public ProjectTreeMouseHandler(ProjectTree tree) {

        this.tree = tree;

        dataFilePopupMenu = new JPopupMenu();

        GUIUtils.addMenuItem(dataFilePopupMenu, "Show TIC", this, "SHOW_TIC");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Show mass spectrum", this,
                "SHOW_SPECTRUM");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Show 2D visualizer", this,
                "SHOW_2D");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Show 3D visualizer", this,
                "SHOW_3D");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Show MS/MS visualizer", this,
                "SHOW_IDA");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Sort alphabetically", this,
                "SORT_FILES");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Remove file extension", this,
                "REMOVE_EXTENSION");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Export file", this,
                "EXPORT_FILE");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Rename file", this,
                "RENAME_FILE");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Remove file", this,
                "REMOVE_FILE");

        scanPopupMenu = new JPopupMenu();

        GUIUtils.addMenuItem(scanPopupMenu, "Show scan", this, "SHOW_SCAN");

        GUIUtils.addMenuItem(scanPopupMenu, "Export scan", this, "EXPORT_SCAN");

        massListPopupMenu = new JPopupMenu();

        GUIUtils.addMenuItem(massListPopupMenu, "Show mass list", this,
                "SHOW_MASSLIST");

        GUIUtils.addMenuItem(massListPopupMenu, "Remove mass list", this,
                "REMOVE_MASSLIST");
        GUIUtils.addMenuItem(massListPopupMenu,
                "Remove all mass lists with this name", this,
                "REMOVE_ALL_MASSLISTS");

        peakListPopupMenu = new JPopupMenu();

        GUIUtils.addMenuItem(peakListPopupMenu, "Show feature list table", this,
                "SHOW_PEAKLIST_TABLES");
        GUIUtils.addMenuItem(peakListPopupMenu, "Show feature list info", this,
                "SHOW_PEAKLIST_INFO");
        GUIUtils.addMenuItem(peakListPopupMenu, "Show scatter plot", this,
                "SHOW_SCATTER_PLOT");
        GUIUtils.addMenuItem(peakListPopupMenu, "Sort alphabetically", this,
                "SORT_PEAKLISTS");
        GUIUtils.addMenuItem(peakListPopupMenu, "Rename", this,
                "RENAME_FEATURELIST");
        GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
                "REMOVE_PEAKLIST");

        peakListRowPopupMenu = new JPopupMenu();

        GUIUtils.addMenuItem(peakListRowPopupMenu, "Show peak summary", this,
                "SHOW_PEAK_SUMMARY");

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        // Actions for raw data files

        if (command.equals("SHOW_TIC")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            TICVisualizerModule.setupNewTICVisualizer(selectedFiles);
        }

        if (command.equals("SHOW_SPECTRUM")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            SpectraVisualizerModule module = MZmineCore
                    .getModuleInstance(SpectraVisualizerModule.class);
            ParameterSet parameters = MZmineCore.getConfiguration()
                    .getModuleParameters(SpectraVisualizerModule.class);
            parameters.getParameter(SpectraVisualizerParameters.dataFiles)
                    .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
                            selectedFiles);
            ExitCode exitCode = parameters.showSetupDialog(
                    MZmineCore.getDesktop().getMainWindow(), true);
            MZmineProject project = MZmineCore.getProjectManager()
                    .getCurrentProject();
            if (exitCode == ExitCode.OK)
                module.runModule(project, parameters, new ArrayList<Task>());
        }

        if (command.equals("SHOW_IDA")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            if (selectedFiles.length == 0)
                return;
            MsMsVisualizerModule.showIDAVisualizerSetupDialog(selectedFiles[0]);

        }

        if (command.equals("SHOW_2D")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            if (selectedFiles.length == 0)
                return;
            TwoDVisualizerModule.show2DVisualizerSetupDialog(selectedFiles[0]);
        }

        if (command.equals("SHOW_3D")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            if (selectedFiles.length == 0)
                return;
            Fx3DVisualizerModule.setupNew3DVisualizer(selectedFiles[0]);
        }

        if (command.equals("SORT_FILES")) {
            // save current selection
            TreePath savedSelection[] = tree.getSelectionPaths();
            RawDataFile selectedFiles[] = tree
                    .getSelectedObjects(RawDataFile.class);
            SortDataFilesModule module = MZmineCore
                    .getModuleInstance(SortDataFilesModule.class);
            ParameterSet params = MZmineCore.getConfiguration()
                    .getModuleParameters(SortDataFilesModule.class);
            params.getParameter(SortDataFilesParameters.dataFiles).setValue(
                    RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles);
            module.runModule(MZmineCore.getProjectManager().getCurrentProject(),
                    params, new ArrayList<Task>());
            // restore selection
            tree.setSelectionPaths(savedSelection);
        }

        if (command.equals("REMOVE_EXTENSION")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            for (RawDataFile file : selectedFiles) {
                file.setName(FilenameUtils.removeExtension(file.toString()));
            }
            tree.updateUI();
        }

        if (command.equals("EXPORT_FILE")) {
            RawDataExportModule exportModule = MZmineCore
                    .getModuleInstance(RawDataExportModule.class);
            ParameterSet params = MZmineCore.getConfiguration()
                    .getModuleParameters(RawDataExportModule.class);
            ExitCode ec = params.showSetupDialog(null, true);
            if (ec == ExitCode.OK) {
                ParameterSet parametersCopy = params.cloneParameterSet();
                ArrayList<Task> tasks = new ArrayList<>();
                MZmineProject project = MZmineCore.getProjectManager()
                        .getCurrentProject();
                exportModule.runModule(project, parametersCopy, tasks);
                MZmineCore.getTaskController()
                        .addTasks(tasks.toArray(new Task[0]));
            }
        }

        if (command.equals("RENAME_FILE")) {
            TreePath path = tree.getSelectionPath();
            if (path == null)
                return;
            else
                tree.startEditingAtPath(path);
        }

        if (command.equals("REMOVE_FILE")) {
            RawDataFile[] selectedFiles = tree
                    .getSelectedObjects(RawDataFile.class);
            PeakList allPeakLists[] = MZmineCore.getProjectManager()
                    .getCurrentProject().getPeakLists();
            for (RawDataFile file : selectedFiles) {
                for (PeakList peakList : allPeakLists) {
                    if (peakList.hasRawDataFile(file)) {
                        String msg = "Cannot remove file " + file.getName()
                                + ", because it is present in the feature list "
                                + peakList.getName();
                        MZmineCore.getDesktop().displayErrorMessage(
                                MZmineCore.getDesktop().getMainWindow(), msg);
                        return;
                    }
                }
                MZmineCore.getProjectManager().getCurrentProject()
                        .removeFile(file);
            }
        }

        // Actions for scans

        if (command.equals("SHOW_SCAN")) {
            Scan selectedScans[] = tree.getSelectedObjects(Scan.class);
            for (Scan scan : selectedScans) {
                SpectraVisualizerModule.showNewSpectrumWindow(
                        scan.getDataFile(), scan.getScanNumber());
            }
        }

        if (command.equals("EXPORT_SCAN")) {
            Scan selectedScans[] = tree.getSelectedObjects(Scan.class);
            ExportScansModule.showSetupDialog(selectedScans);
        }

        if (command.equals("SHOW_MASSLIST")) {
            MassList selectedMassLists[] = tree
                    .getSelectedObjects(MassList.class);
            for (MassList massList : selectedMassLists) {
                Scan scan = massList.getScan();
                SpectraVisualizerWindow window = SpectraVisualizerModule
                        .showNewSpectrumWindow(scan.getDataFile(),
                                scan.getScanNumber());
                MassListDataSet dataset = new MassListDataSet(massList);
                window.addDataSet(dataset, Color.green);
            }
        }

        if (command.equals("REMOVE_MASSLIST")) {
            MassList selectedMassLists[] = tree
                    .getSelectedObjects(MassList.class);
            for (MassList massList : selectedMassLists) {
                Scan scan = massList.getScan();
                scan.removeMassList(massList);
            }
        }

        if (command.equals("REMOVE_ALL_MASSLISTS")) {
            MassList selectedMassLists[] = tree
                    .getSelectedObjects(MassList.class);
            for (MassList massList : selectedMassLists) {
                String massListName = massList.getName();
                RawDataFile dataFiles[] = MZmineCore.getProjectManager()
                        .getCurrentProject().getDataFiles();
                for (RawDataFile dataFile : dataFiles) {
                    int scanNumbers[] = dataFile.getScanNumbers();
                    for (int scanNum : scanNumbers) {
                        Scan scan = dataFile.getScan(scanNum);
                        MassList ml = scan.getMassList(massListName);
                        if (ml != null)
                            scan.removeMassList(ml);
                    }
                }
            }
        }

        // Actions for feature lists

        if (command.equals("SHOW_PEAKLIST_TABLES")) {
            PeakList[] selectedPeakLists = tree
                    .getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists) {
                PeakListTableModule.showNewPeakListVisualizerWindow(peakList);
            }
        }

        if (command.equals("SHOW_PEAKLIST_INFO")) {
            PeakList[] selectedPeakLists = tree
                    .getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists) {
                InfoVisualizerModule.showNewPeakListInfo(peakList);
            }
        }

        if (command.equals("SHOW_SCATTER_PLOT")) {
            PeakList[] selectedPeakLists = tree
                    .getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists) {
                ScatterPlotVisualizerModule.showNewScatterPlotWindow(peakList);
            }
        }

        if (command.equals("SORT_PEAKLISTS")) {
            // save current selection
            TreePath savedSelection[] = tree.getSelectionPaths();
            PeakList selectedPeakLists[] = tree
                    .getSelectedObjects(PeakList.class);
            SortPeakListsModule module = MZmineCore
                    .getModuleInstance(SortPeakListsModule.class);
            ParameterSet params = MZmineCore.getConfiguration()
                    .getModuleParameters(SortPeakListsModule.class);
            params.getParameter(SortPeakListsParameters.peakLists).setValue(
                    PeakListsSelectionType.SPECIFIC_PEAKLISTS,
                    selectedPeakLists);
            module.runModule(MZmineCore.getProjectManager().getCurrentProject(),
                    params, new ArrayList<Task>());
            // restore selection
            tree.setSelectionPaths(savedSelection);
        }

        if (command.equals("RENAME_FEATURELIST")) {
            TreePath path = tree.getSelectionPath();
            if (path == null)
                return;
            else
                tree.startEditingAtPath(path);
        }

        if (command.equals("REMOVE_PEAKLIST")) {
            PeakList[] selectedPeakLists = tree
                    .getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists)
                MZmineCore.getProjectManager().getCurrentProject()
                        .removePeakList(peakList);
        }

        // Actions for feature list rows

        if (command.equals("SHOW_PEAK_SUMMARY")) {
            PeakListRow[] selectedRows = tree
                    .getSelectedObjects(PeakListRow.class);
            for (PeakListRow row : selectedRows) {
                PeakSummaryVisualizerModule.showNewPeakSummaryWindow(row);
            }
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (e.isPopupTrigger())
            handlePopupTriggerEvent(e);

        if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1))
            handleDoubleClickEvent(e);

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger())
            handlePopupTriggerEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            handlePopupTriggerEvent(e);
    }

    private void handlePopupTriggerEvent(MouseEvent e) {
        TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
        if (clickedPath == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) clickedPath
                .getLastPathComponent();
        Object clickedObject = node.getUserObject();

        if (clickedObject instanceof RawDataFile)
            dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
        if (clickedObject instanceof Scan)
            scanPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        if (clickedObject instanceof MassList)
            massListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        if (clickedObject instanceof PeakList)
            peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        if (clickedObject instanceof PeakListRow)
            peakListRowPopupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void handleDoubleClickEvent(MouseEvent e) {
        TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
        if (clickedPath == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) clickedPath
                .getLastPathComponent();
        Object clickedObject = node.getUserObject();

        if (clickedObject instanceof RawDataFile) {
            RawDataFile clickedFile = (RawDataFile) clickedObject;
            TICVisualizerModule.setupNewTICVisualizer(clickedFile);
        }

        if (clickedObject instanceof PeakList) {
            PeakList clickedPeakList = (PeakList) clickedObject;
            PeakListTableModule
                    .showNewPeakListVisualizerWindow(clickedPeakList);
        }

        if (clickedObject instanceof Scan) {
            Scan clickedScan = (Scan) clickedObject;
            SpectraVisualizerModule.showNewSpectrumWindow(
                    clickedScan.getDataFile(), clickedScan.getScanNumber());
        }

        if (clickedObject instanceof MassList) {
            MassList clickedMassList = (MassList) clickedObject;
            Scan clickedScan = clickedMassList.getScan();
            SpectraVisualizerWindow window = SpectraVisualizerModule
                    .showNewSpectrumWindow(clickedScan.getDataFile(),
                            clickedScan.getScanNumber());
            MassListDataSet dataset = new MassListDataSet(clickedMassList);
            window.addDataSet(dataset, Color.green);
        }

        if (clickedObject instanceof PeakListRow) {
            PeakListRow clickedPeak = (PeakListRow) clickedObject;
            PeakSummaryVisualizerModule.showNewPeakSummaryWindow(clickedPeak);
        }

    }

}
