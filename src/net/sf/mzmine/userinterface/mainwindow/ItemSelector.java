/*
 * Copyright 2006 The MZmine Development Team
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
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.DragOrderedJList;
import net.sf.mzmine.userinterface.components.PopupListener;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class implements a selector of raw data files and alignment results
 */
public class ItemSelector extends JPanel implements 
        ActionListener {

    private DefaultListModel rawDataObjects;
    private DragOrderedJList rawDataList;
    private JScrollPane rawDataScroll;

    private DefaultListModel resultObjects;
    private DragOrderedJList resultList;
    private JScrollPane resultScroll;

    /**
     * Constructor
     */
    public ItemSelector(Desktop desktop) {

        // Create panel for raw data objects
        JPanel rawDataPanel = new JPanel();
        JLabel rawDataTitle = new JLabel(new String("Raw data files"));

        rawDataObjects = new DefaultListModel();
        rawDataList = new DragOrderedJList(rawDataObjects);
        rawDataScroll = new JScrollPane(rawDataList);

        rawDataPanel.setLayout(new BorderLayout());
        rawDataPanel.add(rawDataTitle, BorderLayout.NORTH);
        rawDataPanel.add(rawDataScroll, BorderLayout.CENTER);
        rawDataPanel.setMinimumSize(new Dimension(150, 10));

        // Create panel for alignment results
        JPanel resultsPanel = new JPanel();
        JLabel resultsTitle = new JLabel("Aligned peak lists");

        resultObjects = new DefaultListModel();
        resultList = new DragOrderedJList(resultObjects);
        resultScroll = new JScrollPane(resultList);

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

        JPopupMenu popupMenu = new JPopupMenu();
        GUIUtils.addMenuItem(popupMenu, "Close", this, "CLOSE");
        rawDataList.addMouseListener(new PopupListener(popupMenu));
        


    }

    void addSelectionListener(ListSelectionListener listener) {
        rawDataList.addListSelectionListener(listener);
        resultList.addListSelectionListener(listener);
    }


    // Implementation of action listener interface

    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if (command.equals("CLOSE")) {
            OpenedRawDataFile[] selectedFiles = getSelectedRawData();
            for (OpenedRawDataFile file : selectedFiles)
                MZmineProject.getCurrentProject().removeFile(file);
        }

    }

    /**
     * Adds a raw data object to storage
     */
    public void addRawData(OpenedRawDataFile r) {
        rawDataObjects.addElement(r);
    }

    /**
     * Removes a raw data object from storage
     */
    public boolean removeRawData(OpenedRawDataFile r) {
        boolean ans = rawDataObjects.removeElement(r);
        return ans;
    }

    /**
     * Replaces a raw data object in the list with a new file
     */
    public void replaceRawData(OpenedRawDataFile oldFile,
            OpenedRawDataFile newFile) {
        rawDataObjects.setElementAt(newFile, rawDataObjects.indexOf(oldFile));
    }

    /**
     * Returns selected raw data objects in an array
     */
    public OpenedRawDataFile[] getSelectedRawData() {

        Object o[] = rawDataList.getSelectedValues();

        OpenedRawDataFile res[] = new OpenedRawDataFile[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (OpenedRawDataFile) (o[i]);
        }

        return res;

    }

    /**
     * Returns first selected raw data file
     */
    public OpenedRawDataFile getFirstSelectedRawData() {
        return (OpenedRawDataFile) rawDataList.getSelectedValue();
    }

    /**
     * Sets the active raw data item in the list
     */
    public void setActiveRawData(OpenedRawDataFile rawData) {
        rawDataList.setSelectedValue(rawData, true);
    }

    // METHODS FOR MAINTAINING LIST OF RESULTS
    // ---------------------------------------

    public void addAlignmentResult(PeakList a) {
        resultObjects.addElement(a);
    }

    public boolean removeAlignmentResult(PeakList a) {
        boolean ans = resultObjects.removeElement(a);
        return ans;
    }

    public void replaceAlignmentResult(PeakList oldResult,
            PeakList newResult) {
        resultObjects.setElementAt(newResult, resultObjects.indexOf(oldResult));
    }

    public PeakList[] getSelectedAlignmentResults() {

        Object o[] = resultList.getSelectedValues();

        PeakList res[] = new PeakList[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (PeakList) (o[i]);
        }

        return res;

    }

    /**
     * Returns first selected raw data file
     */
    public PeakList getFirstSelectedAlignmentResult() {
        return (PeakList) resultList.getSelectedValue();
    }

    public void setActiveAlignmentResult(PeakList ar) {
        resultList.setSelectedValue(ar, true);
    }

}