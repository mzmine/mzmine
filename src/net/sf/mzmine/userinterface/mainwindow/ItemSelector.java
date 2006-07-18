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
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListModel;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.MZmineOpenedFile;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.visualizers.peaklist.table.PeakListTableView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;

/**
 * This class implements a selector of raw data files and alignment results
 */
public class ItemSelector extends JPanel implements ListSelectionListener,
        MouseListener, ActionListener, InternalFrameListener {

    private DefaultListModel rawDataObjects;
    private JList rawDataList;
    private JScrollPane rawDataScroll;

    private DefaultListModel resultObjects;
    private JList resultList;
    private JScrollPane resultScroll;

    private Desktop desktop;

    /**
     * Constructor
     */
    public ItemSelector(Desktop desktop) {

        this.desktop = desktop;
        // Create panel for raw data objects
        JPanel rawDataPanel = new JPanel();
        JLabel rawDataTitle = new JLabel(new String("Raw data files"));

        rawDataObjects = new DefaultListModel();
        rawDataList = new JList(rawDataObjects);
        rawDataScroll = new JScrollPane(rawDataList);

        rawDataPanel.setLayout(new BorderLayout());
        rawDataPanel.add(rawDataTitle, BorderLayout.NORTH);
        rawDataPanel.add(rawDataScroll, BorderLayout.CENTER);
        rawDataPanel.setMinimumSize(new Dimension(150, 10));

        // Create panel for alignment results
        JPanel resultsPanel = new JPanel();
        JLabel resultsTitle = new JLabel(new String("Alignment results"));

        resultObjects = new DefaultListModel();
        resultList = new JList(resultObjects);
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

        rawDataList.addListSelectionListener(this);
        resultList.addListSelectionListener(this);

        // Create a pop-up menu

        // Add listeners to both lists
        rawDataList.addMouseListener(this);
        resultList.addMouseListener(this);

    }

    public void fireDataChanged() {
        // hack
        rawDataList.setSelectedIndices(rawDataList.getSelectedIndices());
        resultList.setSelectedIndices(resultList.getSelectedIndices());
    }

    void addSelectionListener(ListSelectionListener listener) {
        rawDataList.addListSelectionListener(listener);
        resultList.addListSelectionListener(listener);
    }

    // Implementation of mouse listener interface

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger())
            showPopupMenu(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            showPopupMenu(e);
    }

    private void showPopupMenu(MouseEvent e) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (e.getComponent() == rawDataList) {
            if (rawDataList.locationToIndex(e.getPoint()) == -1)
                return;
            MZmineOpenedFile selectedFile = (MZmineOpenedFile) rawDataObjects.get(rawDataList.locationToIndex(e.getPoint()));
            /*
             * int[] msLevels = selectedFile.getMSLevels(); for (int msLevel :
             * msLevels) { JMenuItem showTIC = new JMenuItem("Show TIC of MS
             * level " + msLevel); showTIC.addActionListener(this);
             * showTIC.setActionCommand("TIC" + msLevel);
             * popupMenu.add(showTIC); }
             * 
             * for (int msLevel : msLevels) { JMenuItem showBP = new
             * JMenuItem("Show base peak intensity of MS level " + msLevel);
             * showBP.addActionListener(this); showBP.setActionCommand("BP" +
             * msLevel); popupMenu.add(showBP); }
             * 
             * for (int msLevel : msLevels) { JMenuItem showBP = new
             * JMenuItem("Show 2D visualizer of MS level " + msLevel);
             * showBP.addActionListener(this); showBP.setActionCommand("2D" +
             * msLevel); popupMenu.add(showBP); }
             * 
             * for (int msLevel : msLevels) { JMenuItem showBP = new
             * JMenuItem("Show 3D visualizer of MS level " + msLevel);
             * showBP.addActionListener(this); showBP.setActionCommand("3D" +
             * msLevel); popupMenu.add(showBP); }
             */

            // TODO: show scan list?
            JMenuItem peakListMenuItem = GUIUtils.addMenuItem(popupMenu,
                    "Show peak list", this, "PEAKLIST");
            boolean hasPeakList = MZmineProject.getCurrentProject().hasPeakList(
                    selectedFile);
            peakListMenuItem.setEnabled(hasPeakList);
            popupMenu.addSeparator();

            GUIUtils.addMenuItem(popupMenu, "Close", this, "CLOSE");

        }
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    // Implementation of action listener interface

    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if (command.equals("PEAKLIST")) {
            MZmineOpenedFile[] selectedFiles = getSelectedRawData();
            for (MZmineOpenedFile file : selectedFiles) {
                if (MZmineProject.getCurrentProject().hasPeakList(file)) {
                    PeakListTableView peakListTable = new PeakListTableView(
                            file);
                    desktop.addInternalFrame(peakListTable);
                }
            }
        }

        if (command.equals("CLOSE")) {
            MZmineOpenedFile[] selectedFiles = getSelectedRawData();
            for (MZmineOpenedFile file : selectedFiles)
                MZmineProject.getCurrentProject().removeFile(file);

        }

    }

    /**
     * Adds a raw data object to storage
     */
    public void addRawData(MZmineOpenedFile r) {
        rawDataObjects.addElement(r);
    }

    /**
     * Removes a raw data object from storage
     */
    public boolean removeRawData(MZmineOpenedFile r) {
        boolean ans = rawDataObjects.removeElement(r);

        // MainWindow.getInstance().getMainMenu().updateMenuAvailability();

        return ans;
    }

    /**
     * Replaces a raw data object in the list with a new file
     */
    public void replaceRawData(MZmineOpenedFile oldFile, MZmineOpenedFile newFile) {
        rawDataObjects.setElementAt(newFile, rawDataObjects.indexOf(oldFile));
    }

    /**
     * Returns selected raw data objects in an array
     */
    public MZmineOpenedFile[] getMZmineOpenedFiles() {

        Object o[] = rawDataObjects.toArray();

        MZmineOpenedFile res[] = new MZmineOpenedFile[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (MZmineOpenedFile) (o[i]);
        }

        return res;

    }

    /**
     * Returns selected raw data objects in an array
     */
    public MZmineOpenedFile[] getSelectedRawData() {

        Object o[] = rawDataList.getSelectedValues();

        MZmineOpenedFile res[] = new MZmineOpenedFile[o.length];

        for (int i = 0; i < o.length; i++) {
            res[i] = (MZmineOpenedFile) (o[i]);
        }

        return res;

    }

    /**
     * Returns first selected raw data file
     */
    public MZmineOpenedFile getFirstSelectedRawData() {

        return (MZmineOpenedFile) rawDataList.getSelectedValue();

    }

    /**
     * Sets the active raw data item in the list
     */
    public void setActiveAlignmentResult(AlignmentResult ar) {
        resultList.setSelectedValue(ar, true);

        // MainWindow.getInstance().getMainMenu().updateMenuAvailability();
        // repaint();
    }

    /**
     * Sets the active raw data item in the list
     */
    public void setActiveRawData(MZmineOpenedFile rawData) {
        rawDataList.setSelectedValue(rawData, true);

        // MainWindow.getInstance().getMainMenu().updateMenuAvailability();
        // repaint();
    }

    /**
     * Returns the run that is selected in run list
     * 
     * public RawDataAtClient getActiveRawData() { return (RawDataAtClient)
     * rawDataList.getSelectedValue(); }
     */

    // METHODS FOR MAINTAINING LIST OF RESULTS
    // ---------------------------------------
    /**
     * Returns a vector containing all currently selected result objects in the
     * list
     */
    public Vector<AlignmentResult> getSelectedAlignmentResults() {

        Vector<AlignmentResult> v = new Vector<AlignmentResult>();
        Object o[] = resultList.getSelectedValues();

        for (int i = 0; i < o.length; i++) {
            v.add((AlignmentResult) o[i]);
        }

        return v;
    }

    public int[] getSelectedAlignmentResultIDs() {

        Object o[] = resultList.getSelectedValues();
        int[] alignmentResultIDs = new int[o.length];

        for (int i = 0; i < o.length; i++) {
            alignmentResultIDs[i] = ((AlignmentResult) o[i]).getAlignmentResultID();
        }

        return alignmentResultIDs;

    }

    /**
     * Adds alignment result to item storage
     * 
     * @return ID for the alignment result
     */
    public void addAlignmentResult(AlignmentResult ar) {

        resultObjects.addElement(ar);

        // Add dependency to all involved raw data files
        for (int rawDataID : ar.getRawDataIDs()) {
            // getRawDataByID(rawDataID).addAlignmentResultID(
            // ar.getAlignmentResultID());
        }

    }

    public boolean removeAlignmentResult(AlignmentResult ar) {
        boolean ans = resultObjects.removeElement(ar);

        // MainWindow.getInstance().getMainMenu().updateMenuAvailability();

        return ans;
    }

    /**
     * Returns all alignment result ids
     */
    public int[] getAlignmentResultIDs() {
        ListModel listModel = resultList.getModel();

        int[] alignmentResultIDs = new int[listModel.getSize()];

        for (int i = 0; i < listModel.getSize(); i++) {
            alignmentResultIDs[i] = ((AlignmentResult) (listModel.getElementAt(i))).getAlignmentResultID();
        }

        return alignmentResultIDs;
    }

    public AlignmentResult getAlignmentResultByID(int alignmentResultID) {
        ListModel listModel = resultList.getModel();

        for (int i = 0; i < listModel.getSize(); i++) {
            AlignmentResult alignmentResult = (AlignmentResult) listModel.getElementAt(i);
            if (alignmentResult.getAlignmentResultID() == alignmentResultID) {
                return alignmentResult;
            }
        }

        return null;
    }

    /**
     * Returns the currently selected result object.
     */
    public AlignmentResult getActiveResult() {
        return (AlignmentResult) resultList.getSelectedValue();
    }

    // MISC. STUFF
    // -----------

    /**
     * Implementation of ListSelectionListener interface
     */
    public void valueChanged(ListSelectionEvent e) {

        MZmineOpenedFile activeRawData;
        AlignmentResult activeResult;

        // Avoid reacting to unnecessary events
        if (e.getValueIsAdjusting()) {
            return;
        }

        // Run list selection changed?
        if (e.getSource() == rawDataList) {

            int i = rawDataList.getSelectedIndex();

            // Something selected in run list?
            if (i != -1) {

                // Clear all selections in results list
                resultList.clearSelection();

                // Get run that was just selected in run list
                Object tmpObj = rawDataList.getSelectedValue();
                if (tmpObj != null) {

                    activeRawData = (MZmineOpenedFile) tmpObj;

                    // Update cursor position in status bar
                    // statBar.setCursorPosition(activeRawData);

                    // Bring visualizers for this run to top
                    // if ( !(mainWin.isBusy()) ) {
                    // mainWin.moveVisualizersToFront(activeRawData); }

                } else {
                }

            }

        }

        // Result list selection changed?
        if (e.getSource() == resultList) {

            int i = resultList.getSelectedIndex();

            if (i != -1) {
                // Clear all selections in run list
                rawDataList.clearSelection();

                // Get result object that was just selected in the list
                activeResult = (AlignmentResult) resultList.getSelectedValue();

                // Bring visualizers for this run to top
                // if ( !(mainWin.isBusy()) ) {
                // mainWin.moveVisualizersToFront(activeResult); }

            }

        }

        // MainWindow.getInstance().getMainMenu().updateMenuAvailability();

        // mainWin.repaint();

    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameOpened(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameOpened(InternalFrameEvent arg0) {
    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameClosing(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameClosing(InternalFrameEvent arg0) {
    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameClosed(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameClosed(InternalFrameEvent arg0) {
    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameIconified(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameIconified(InternalFrameEvent arg0) {
    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameDeiconified(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameDeiconified(InternalFrameEvent arg0) {
    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameActivated(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameActivated(InternalFrameEvent e) {
        if (e.getInternalFrame() instanceof RawDataVisualizer) {
            RawDataVisualizer visualizer = (RawDataVisualizer) e.getInternalFrame();
            // TODO: setActiveRawData(visualizer.getMZmineOpenedFile());
        }
    }

    /**
     * @see javax.swing.event.InternalFrameListener#internalFrameDeactivated(javax.swing.event.InternalFrameEvent)
     */
    public void internalFrameDeactivated(InternalFrameEvent arg0) {
    }

}