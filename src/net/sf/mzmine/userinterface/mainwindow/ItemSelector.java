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

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableWindow;
import net.sf.mzmine.modules.visualization.tic.TICSetupDialog;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.DragOrderedJList;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class implements a selector of raw data files and alignment results
 */
public class ItemSelector extends JPanel implements ActionListener,
        MouseListener, ListSelectionListener {

    public static final String DATA_FILES_LABEL = "Raw data files";
    public static final String PEAK_LISTS_LABEL = "Peak lists";

    private DefaultListModel rawDataFiles;
    private DragOrderedJList rawDataList;

    private DefaultListModel peakLists;
    private JList alignedPeakListList;
    private JPopupMenu dataFilePopupMenu, peakListPopupMenu;

    /**
     * Constructor
     */
    public ItemSelector(Desktop desktop) {

        // Create panel for raw data objects
        JPanel rawDataPanel = new JPanel();
        JLabel rawDataTitle = new JLabel(DATA_FILES_LABEL);

        rawDataFiles = new DefaultListModel();
        rawDataList = new DragOrderedJList(rawDataFiles);
        rawDataList.setCellRenderer(new ItemSelectorListRenderer());
        rawDataList.addMouseListener(this);
        rawDataList.addListSelectionListener(this);
        JScrollPane rawDataScroll = new JScrollPane(rawDataList);

        rawDataPanel.setLayout(new BorderLayout());
        rawDataPanel.add(rawDataTitle, BorderLayout.NORTH);
        rawDataPanel.add(rawDataScroll, BorderLayout.CENTER);
        rawDataPanel.setMinimumSize(new Dimension(150, 10));

        // Create panel for alignment results
        JPanel resultsPanel = new JPanel();
        JLabel resultsTitle = new JLabel(PEAK_LISTS_LABEL);

        peakLists = new DefaultListModel();
        alignedPeakListList = new DragOrderedJList(peakLists);
        alignedPeakListList.setCellRenderer(new ItemSelectorListRenderer());
        alignedPeakListList.addMouseListener(this);
        alignedPeakListList.addListSelectionListener(this);
        JScrollPane resultScroll = new JScrollPane(alignedPeakListList);

        resultsPanel.setLayout(new BorderLayout());
        resultsPanel.add(resultsTitle, BorderLayout.NORTH);
        resultsPanel.add(resultScroll, BorderLayout.CENTER);
        resultsPanel.setMinimumSize(new Dimension(150, 10));

        // Add panels to a split and put split on the main panel
        setPreferredSize(new Dimension(150, 10));
        setLayout(new BorderLayout());

        JSplitPane rawAndResultsSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, rawDataPanel, resultsPanel);
        add(rawAndResultsSplit, BorderLayout.CENTER);

        rawAndResultsSplit.setDividerLocation(230);

        dataFilePopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(dataFilePopupMenu, "Show TIC", this, "SHOW_TIC");
        GUIUtils.addMenuItem(dataFilePopupMenu, "Remove", this, "REMOVE_FILE");

        peakListPopupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(peakListPopupMenu, "Show peak list", this,
                "SHOW_ALIGNED_PEAKLIST");
        GUIUtils.addMenuItem(peakListPopupMenu, "Remove", this,
                "REMOVE_PEAKLIST");

    }

    void addSelectionListener(ListSelectionListener listener) {
        rawDataList.addListSelectionListener(listener);
        alignedPeakListList.addListSelectionListener(listener);
    }

    // Implementation of action listener interface

    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if (command.equals("REMOVE_FILE")) {
            RawDataFile[] selectedFiles = getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                MZmineCore.getCurrentProject().removeFile(file);
        }

        if (command.equals("SHOW_TIC")) {
            RawDataFile[] selectedFiles = getSelectedRawData();
            for (RawDataFile file : selectedFiles) {
                TICSetupDialog dialog = new TICSetupDialog(file);
                dialog.setVisible(true);
            }
        }

        if (command.equals("REMOVE_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedAlignedPeakLists();
            for (PeakList peakList : selectedPeakLists)
                MZmineCore.getCurrentProject().removePeakList(peakList);
        }

        if (command.equals("SHOW_ALIGNED_PEAKLIST")) {
            PeakList[] selectedPeakLists = getSelectedAlignedPeakLists();
            Desktop desktop = MZmineCore.getDesktop();
            for (PeakList peakList : selectedPeakLists) {
                PeakListTableWindow window = new PeakListTableWindow(peakList);
                desktop.addInternalFrame(window);
            }
        }

    }

    /**
     * Adds a raw data object to storage
     */
    public void addRawData(RawDataFile r) {
        rawDataFiles.addElement(r);
    }

    /**
     * Removes a raw data object from storage
     */
    public boolean removeRawData(RawDataFile r) {
        return rawDataFiles.removeElement(r);
    }

    /**
     * Replaces a raw data object in the list with a new file
     */
    public void replaceRawData(RawDataFile oldFile, RawDataFile newFile) {
        rawDataFiles.setElementAt(newFile, rawDataFiles.indexOf(oldFile));
    }

    /**
     * Returns selected raw data objects in an array
     */
    public RawDataFile[] getSelectedRawData() {

        Object o[] = rawDataList.getSelectedValues();

        RawDataFile res[] = new RawDataFile[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (RawDataFile) (o[i]);
        }

        return res;

    }

    /**
     * Sets the active raw data item in the list
     */
    public void setActiveRawData(RawDataFile rawData) {
        rawDataList.setSelectedValue(rawData, true);
    }

    // METHODS FOR MAINTAINING LIST OF RESULTS
    // ---------------------------------------

    public void addAlignmentResult(PeakList a) {
        peakLists.addElement(a);
    }

    public boolean removeAlignedPeakList(PeakList a) {
        return peakLists.removeElement(a);
    }

    public PeakList[] getSelectedAlignedPeakLists() {

        Object o[] = alignedPeakListList.getSelectedValues();

        PeakList res[] = new PeakList[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (PeakList) (o[i]);
        }

        return res;

    }

    public void mouseClicked(MouseEvent e) {

        if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {

            if (e.getSource() == rawDataList) {
                int clickedIndex = rawDataList.locationToIndex(e.getPoint());
                if (clickedIndex < 0)
                    return;
                RawDataFile clickedFile = (RawDataFile) rawDataFiles.get(clickedIndex);
                TICSetupDialog dialog = new TICSetupDialog(clickedFile);
                dialog.setVisible(true);
            }

            if (e.getSource() == alignedPeakListList) {
                int clickedIndex = alignedPeakListList.locationToIndex(e.getPoint());
                if (clickedIndex < 0)
                    return;
                PeakList clickedPeakList = (PeakList) peakLists.get(clickedIndex);
                PeakListTableWindow window = new PeakListTableWindow(
                        clickedPeakList);
                Desktop desktop = MZmineCore.getDesktop();
                desktop.addInternalFrame(window);
            }

        }

    }

    public void mouseEntered(MouseEvent e) {
        // ignore

    }

    public void mouseExited(MouseEvent e) {
        // ignore
    }

    public void mousePressed(MouseEvent e) {

        if (e.isPopupTrigger()) {
            if (e.getSource() == rawDataList)
                dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (e.getSource() == alignedPeakListList)
                peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }

    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            if (e.getSource() == rawDataList)
                dataFilePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            if (e.getSource() == alignedPeakListList)
                peakListPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void valueChanged(ListSelectionEvent event) {

        Object src = event.getSource();

        // Update the highlighting of peak list list in case raw data list
        // selection has changed and vice versa.
        if (src == rawDataList) {
            alignedPeakListList.repaint();
        }

        if (src == alignedPeakListList) {
            rawDataList.repaint();
        }

    }

}