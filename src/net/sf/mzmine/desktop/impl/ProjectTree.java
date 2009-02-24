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

package net.sf.mzmine.desktop.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Array;
import java.util.Vector;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.visualization.infovisualizer.InfoVisualizer;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableVisualizer;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.NameChangeDialog;
import net.sf.mzmine.util.dialogs.NameChangeable;

/**
 * This class implements a selector of raw data files and alignment results
 */
class ProjectTree extends JTree implements MouseListener, ActionListener {

    private ProjectTreeModel treeModel;

    private JPopupMenu dataFilePopupMenu, peakListPopupMenu, scanPopupMenu,
            peakListRowPopupMenu;

    /**
     * Constructor
     */
    ProjectTree() {

        treeModel = new ProjectTreeModel();
        setModel(treeModel);

        ProjectTreeRenderer renderer = new ProjectTreeRenderer();
        setCellRenderer(renderer);

        ProjectTreeEditor editor = new ProjectTreeEditor(this, renderer);
        setCellEditor(editor);

        setRootVisible(true);
        setShowsRootHandles(false);
        setToggleClickCount(-1);

        addMouseListener(this);

        expandPath(new TreePath(new Object[] { ProjectTreeModel.rootItem,
                ProjectTreeModel.dataFilesItem }));
        expandPath(new TreePath(new Object[] { ProjectTreeModel.rootItem,
                ProjectTreeModel.peakListsItem }));

        dataFilePopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(dataFilePopupMenu, "Show selected files TIC",
                this, "SHOW_TIC");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Rename", this, "RENAME_FILE");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Remove", this, "REMOVE_FILE");

        scanPopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(scanPopupMenu, "Show selected spectra", this,
                "SHOW_SPECTRA");

        peakListPopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(peakListPopupMenu, "Show selected peak lists",
                this, "SHOW_PEAKLIST_TABLES");
        GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list info", this,
                "SHOW_PEAKLIST_INFO");
        GUIUtils.addMenuItem(peakListPopupMenu, "Rename", this,
                "RENAME_PEAKLIST");
        GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
                "REMOVE_PEAKLIST");

        peakListRowPopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(peakListRowPopupMenu,
                "Show peak list row summaries", this, "SHOW_PEAK_SUMMARIES");

    }

    public ProjectTreeModel getModel() {
        return treeModel;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getSelectedObjects(Class<T> objectClass) {
        Vector<T> selectedObjects = new Vector<T>();
        TreePath selectedItems[] = getSelectionPaths();
        if (selectedItems != null) {
            for (TreePath path : selectedItems) {
                Object selectedObject = path.getLastPathComponent();
                if (objectClass.isInstance(selectedObject))
                    selectedObjects.add((T) selectedObject);
            }
        }
        return (T[]) selectedObjects.toArray((Object[]) Array.newInstance(
                objectClass, 0));
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals("RENAME_FILE")) {
            RawDataFile[] selectedFiles = getSelectedObjects(RawDataFile.class);
            for (RawDataFile file : selectedFiles) {
                if (file instanceof NameChangeable) {
                    NameChangeDialog dialog = new NameChangeDialog(
                            (NameChangeable) file);
                    dialog.setVisible(true);
                }
            }
        }

        if (command.equals("REMOVE_FILE")) {
            RawDataFile[] selectedFiles = getSelectedObjects(RawDataFile.class);
            for (RawDataFile file : selectedFiles)
                MZmineCore.getCurrentProject().removeFile(file);
        }

        if (command.equals("SHOW_TIC")) {
            RawDataFile[] selectedFiles = getSelectedObjects(RawDataFile.class);
            TICVisualizer.showNewTICVisualizerWindow(selectedFiles, null, null);
        }

        if (command.equals("SHOW_SPECTRA")) {
            TreePath selectedItems[] = getSelectionPaths();
            if (selectedItems != null) {
                for (TreePath path : selectedItems) {
                    Object selectedObject = path.getLastPathComponent();
                    if (selectedObject instanceof Scan) {
                        Scan scan = (Scan) selectedObject;
                        RawDataFile dataFile = (RawDataFile) path.getParentPath().getLastPathComponent();
                        SpectraVisualizer.showNewSpectrumWindow(dataFile,
                                scan.getScanNumber());
                    }
                }
            }
        }

        if (command.equals("RENAME_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists) {
                if (peakList instanceof NameChangeable) {
                    NameChangeDialog dialog = new NameChangeDialog(
                            (NameChangeable) peakList);
                    dialog.setVisible(true);
                }
            }
        }

        if (command.equals("REMOVE_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists)
                MZmineCore.getCurrentProject().removePeakList(peakList);
        }

        if (command.equals("SHOW_PEAKLIST_TABLES")) {
            PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists) {
                PeakListTableVisualizer.showNewPeakListVisualizerWindow(peakList);
            }
        }

        if (command.equals("SHOW_PEAKLIST_INFO")) {
            PeakList[] selectedPeakLists = getSelectedObjects(PeakList.class);
            for (PeakList peakList : selectedPeakLists) {
                InfoVisualizer.showNewPeakListInfo(peakList);
            }
        }

        if (command.equals("SHOW_PEAK_SUMMARIES")) {
            PeakListRow[] selectedRows = getSelectedObjects(PeakListRow.class);
            for (PeakListRow row : selectedRows) {
                PeakSummaryVisualizer.showNewPeakSummaryWindow(row);
            }
        }

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        TreePath clickedPath = getPathForLocation(e.getX(), e.getY());
        Object clickedObject = null;
        if (clickedPath != null)
            clickedObject = clickedPath.getLastPathComponent();

        if (e.isPopupTrigger()) {
            if (clickedObject instanceof RawDataFile)
                dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (clickedObject instanceof Scan)
                scanPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (clickedObject instanceof PeakList)
                peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (clickedObject instanceof PeakListRow)
                peakListRowPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }

        if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {

            if (clickedObject instanceof RawDataFile) {
                RawDataFile clickedFile = (RawDataFile) clickedObject;
                TICVisualizer.showNewTICVisualizerWindow(
                        new RawDataFile[] { clickedFile }, null, null);
            }

            if (clickedObject instanceof PeakList) {
                PeakList clickedPeakList = (PeakList) clickedObject;
                PeakListTableVisualizer.showNewPeakListVisualizerWindow(clickedPeakList);
            }

            if (clickedObject instanceof Scan) {
                Scan clickedScan = (Scan) clickedObject;
                RawDataFile dataFile = (RawDataFile) clickedPath.getParentPath().getLastPathComponent();
                SpectraVisualizer.showNewSpectrumWindow(dataFile,
                        clickedScan.getScanNumber());
            }

            if (clickedObject instanceof PeakListRow) {
                PeakListRow clickedPeak = (PeakListRow) clickedObject;
                PeakSummaryVisualizer.showNewPeakSummaryWindow(clickedPeak);
            }

        }
    }

    public void mouseReleased(MouseEvent e) {
        TreePath clickedPath = getPathForLocation(e.getX(), e.getY());
        Object clickedObject = null;
        if (clickedPath != null)
            clickedObject = clickedPath.getLastPathComponent();

        if (e.isPopupTrigger()) {
            if (clickedObject instanceof RawDataFile)
                dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (clickedObject instanceof Scan)
                scanPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (clickedObject instanceof PeakList)
                peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (clickedObject instanceof PeakListRow)
                peakListRowPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}