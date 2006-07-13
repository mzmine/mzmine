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
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.TaskProgressWindow;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizer;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements Desktop, MZmineModule,
        WindowListener {

    private TaskController taskController;
    private IOController ioController;
    private Logger logger;

    private JDesktopPane desktop;

    private Statusbar statBar;

    private JSplitPane split;

    // private ItemStorage itemStorage;
    private ItemSelector itemSelector;

    // This table maps rawDataIDs to visualizers showing the data
    private Hashtable<Integer, Vector<RawDataVisualizer>> rawDataVisualizers;

    private Hashtable<Integer, Vector<AlignmentResultVisualizer>> alignmentResultVisualizers;

    private boolean busyFlag; // This flag is true when system is busy doing
    // some computational stuff

    private String dataDirectory; // Stores the last used directory for saving
    // peak lists & alignment results

    private TaskProgressWindow taskList;

    public TaskProgressWindow getTaskList() {
        return taskList;
    }

    private MainMenu menuBar;

    public MainMenu getMainMenu() {
        return menuBar;
    }

    public void addInternalFrame(JInternalFrame frame) {
        desktop.add(frame, JLayeredPane.DEFAULT_LAYER);
        // TODO: adjust frame position
        frame.addInternalFrameListener(itemSelector);
        frame.setVisible(true);
    }

    /**
     * This method returns the desktop
     */
    public JDesktopPane getDesktopPane() {
        return desktop;
    }

    public ItemSelector getItemSelector() {
        return itemSelector;
    }

    /**
     * This method moves all visualizers of given raw data to the top on desktop
     * 
     * public void moveVisualizersToFront(RawDataAtClient rawData) {
     * 
     * Integer rawDataID = new Integer(rawData.getRawDataID());
     *  // Get all visualizers for this raw data file Vector<RawDataVisualizer>
     * visualizers = rawDataVisualizers .get(rawDataID);
     * 
     * if (visualizers == null) { return; }
     *  // Move each visualizer window to front boolean somethingIsSelected =
     * false; JInternalFrame jif = null; for (RawDataVisualizer vis :
     * visualizers) { jif = (JInternalFrame) vis; if (jif.isSelected()) {
     * somethingIsSelected = true; } jif.moveToFront(); }
     * 
     * if ((!somethingIsSelected) && (visualizers != null)) { try { jif =
     * (JInternalFrame) (visualizers.get(0)); jif.setSelected(true); } catch
     * (Exception e) { } }
     *  }
     * 
     * /** This method moves all visualizers of given alignment result to the
     * top on desktop
     */
    public void moveVisualizersToFront(AlignmentResult result) {

        Integer alignmentResultID = new Integer(result.getAlignmentResultID());

        // Get all visualizer for this alignment result
        Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers.get(alignmentResultID);

        if (visualizers == null) {
            return;
        }

        // Move each visualizer window to front
        for (AlignmentResultVisualizer vis : visualizers) {
            ((JInternalFrame) vis).moveToFront();
        }

        // Select first visualizer window as the active window
        try {
            ((JInternalFrame) (visualizers.get(0))).setSelected(true);
        } catch (Exception e) {
        }

    }

    /**
     * Checks if a raw data file has one or more visible visualizers
     * 
     * @param rawDataID
     *            raw data file ID
     * @return true if raw data file has one or more visible visualizers
     */
    public boolean rawDataHasVisibleVisualizers(int rawDataID,
            boolean includeIcons) {
        boolean shouldBeShown = false;

        // Get visualizer vector for this raw data ID
        Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers.get(new Integer(
                rawDataID));

        // If raw data file has some visualizers, check if any of them is
        // visible
        if (visualizerVector != null) {
            Enumeration<RawDataVisualizer> visualizers = visualizerVector.elements();
            while (visualizers.hasMoreElements()) {

                RawDataVisualizer vis = visualizers.nextElement();
                JInternalFrame jif = (JInternalFrame) vis;

                if (includeIcons) {
                    if (jif.isVisible()) {
                        shouldBeShown = true;
                    }
                } else {
                    if (jif.isVisible() && !(jif.isIcon())) {
                        shouldBeShown = true;
                    }
                }
            }
        }

        return shouldBeShown;

    }

    void tileInternalFrames() {
        JInternalFrame[] frames = getVisibleFrames();
        if (frames.length == 0)
            return;
        Rectangle dBounds = desktop.getBounds();

        int cols = (int) Math.sqrt(frames.length);
        int rows = (int) (Math.ceil(((double) frames.length) / cols));
        int lastRow = frames.length - cols * (rows - 1);
        int width, height;

        if (lastRow == 0) {
            rows--;
            height = dBounds.height / rows;
        } else {
            height = dBounds.height / rows;
            if (lastRow < cols) {
                rows--;
                width = dBounds.width / lastRow;
                for (int i = 0; i < lastRow; i++) {
                    frames[cols * rows + i].setBounds(i * width, rows * height,
                            width, height);
                }
            }
        }

        width = dBounds.width / cols;
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                frames[i + j * cols].setBounds(i * width, j * height, width,
                        height);
            }
        }
    }

    void cascadeInternalFrames() {
        JInternalFrame[] frames = getVisibleFrames();
        if (frames.length == 0)
            return;
        Rectangle dBounds = desktop.getBounds();
        int separation = 24;
        int margin = (frames.length - 1) * separation;
        int width = dBounds.width - margin;
        int height = dBounds.height - margin;
        for (int i = 0; i < frames.length; i++) {
            frames[i].setBounds(i * separation, i * separation, width, height);
        }
    }

    /**
     * WindowListener interface implementation
     */
    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        exitMZmine();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public Statusbar getStatusBar() {
        return statBar;
    }

    /*
     * public RunSelector getRunSelector() { //return runPick; return null; }
     */
    /*
     * public void paintNow() { update(getGraphics()); }
     * 
     * public void closeRawDataFiles(int[] rawDataIDs) {
     * 
     * if (rawDataIDs.length > 0) { // Check that any of these files is not
     * participating any alignment String errorMessage = null; for (int
     * rawDataID : rawDataIDs) { RawDataAtClient rawData = itemSelector
     * .getRawDataByID(rawDataID); if (rawData.getAlignmentResultIDs().size() >
     * 0) { Vector<Integer> alignmentResultIDs = rawData
     * .getAlignmentResultIDs(); errorMessage = rawData.getNiceName() + " is
     * participating in alignment(s). Before closing the raw data files, please
     * close alignment result(s): "; for (Integer alignmentResultID :
     * alignmentResultIDs) { errorMessage +=
     * itemSelector.getAlignmentResultByID(
     * alignmentResultID.intValue()).getNiceName() + ", "; } } if (errorMessage !=
     * null) { displayErrorMessage(errorMessage); return; } }
     *  // Remove all visualizers for these raw data files for (int rawDataID :
     * rawDataIDs) { removeRawDataVisualizers(rawDataID); }
     *  // clientForCluster.closeRawDataFiles(rawDataIDs);
     * 
     * getStatusBar().setStatusText( "Closing " + rawDataIDs.length + " raw data
     * file(s).");
     *  }
     *  }
     * 
     * 
     * public void closeAlignmentResults(int[] alignmentResultIDs) {
     * 
     * if (alignmentResultIDs.length > 0) {
     * 
     * for (int alignmentResultID : alignmentResultIDs) {
     * 
     * if (itemSelector.getAlignmentResultByID(alignmentResultID) .isImported()) {
     * continue; }
     *  // Remove dependency from each involved raw data file int[] rawDataIDs =
     * itemSelector.getAlignmentResultByID( alignmentResultID).getRawDataIDs();
     * for (int rawDataID : rawDataIDs) { RawDataAtClient rawData = itemSelector
     * .getRawDataByID(rawDataID);
     * rawData.removeAlignmentResultID(alignmentResultID); } }
     * 
     * for (int alignmentResultID : alignmentResultIDs) { // Close all
     * visualizers for these alignment results
     * removeAlignmentResultVisualizers(alignmentResultID);
     *  // Remove these alignment results from the item selector
     * itemSelector.removeAlignmentResult(itemSelector
     * .getAlignmentResultByID(alignmentResultID)); }
     * 
     * getStatusBar().setStatusText( "Closed " + alignmentResultIDs.length + "
     * alignment result(s).");
     *  }
     *  }
     * 
     * public void doLinearNormalizationClientSide(LinearNormalizerParameters
     * lnp, Vector<AlignmentResult> originalAlignmentResults) {
     * statBar.setStatusText("Normalizing selected alignment results.");
     * paintNow();
     * 
     * setBusy(true);
     * 
     * LinearNormalizer ln = new LinearNormalizer(); Enumeration<AlignmentResult>
     * originalAlignmentResultEnum = originalAlignmentResults .elements(); while
     * (originalAlignmentResultEnum.hasMoreElements()) { AlignmentResult ar =
     * originalAlignmentResultEnum.nextElement(); AlignmentResult nar = ln
     * .calcNormalization(MainWindow.this, ar, lnp);
     * itemSelector.addAlignmentResult(nar);
     * addAlignmentResultVisualizerList(nar); }
     * 
     * setBusy(false); statBar.setStatusText("Normalization done."); }
     * 
     * public void runAlignmentResultFilteringByGapsClientSide(
     * AlignmentResultFilterByGapsParameters arpp, Vector<AlignmentResult>
     * originalAlignmentResults) { statBar.setStatusText("Filtering selected
     * alignment results."); paintNow();
     * 
     * setBusy(true);
     * 
     * AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();
     * 
     * Enumeration<AlignmentResult> originalAlignmentResultEnum =
     * originalAlignmentResults .elements(); while
     * (originalAlignmentResultEnum.hasMoreElements()) { AlignmentResult ar =
     * originalAlignmentResultEnum.nextElement(); AlignmentResult nar =
     * arfbg.processAlignment(MainWindow.this, ar,
     * (AlignmentResultProcessorParameters) arpp);
     * itemSelector.addAlignmentResult(nar);
     * addAlignmentResultVisualizerList(nar); }
     * 
     * setBusy(false); statBar.setStatusText("Alignment result filtering
     * done.");
     *  }
     * 
     * public AlignmentResult runAlignmentResultFilteringByGapsClientSide(
     * AlignmentResultFilterByGapsParameters arpp, AlignmentResult
     * originalAlignmentResult) { statBar.setStatusText("Filtering alignment
     * result."); paintNow();
     * 
     * setBusy(true);
     * 
     * AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();
     * 
     * AlignmentResult nar = arfbg.processAlignment(MainWindow.this,
     * originalAlignmentResult, (AlignmentResultProcessorParameters) arpp);
     * itemSelector.addAlignmentResult(nar);
     * addAlignmentResultVisualizerList(nar);
     * 
     * setBusy(false); statBar.setStatusText("Alignment result filtering
     * done."); return nar;
     *  }
     * 
     * /** Copies the zoom settings from given run to all other runs. whatDims
     * parameter defines wheter to copy zoom settings only in mz, rt or both
     * dimensions
     * 
     * public void setSameZoomToOtherRawDatas(RawDataAtClient originalRawData,
     * int whatDims) {
     * 
     * double startMZ = 0; double endMZ = 0; int startScan = 0; int endScan = 0;
     * double cursorMZ = 0; int cursorScan = 0;
     * 
     * double tmpStartMZ; double tmpEndMZ; int tmpStartScan; int tmpEndScan;
     * 
     * double tmpCursorMZ; int tmpCursorScan;
     * 
     * startMZ = originalRawData.getSelectionMZStart(); endMZ =
     * originalRawData.getSelectionMZEnd(); startScan =
     * originalRawData.getSelectionScanStart(); endScan =
     * originalRawData.getSelectionScanEnd(); cursorMZ =
     * originalRawData.getCursorPositionMZ(); cursorScan =
     * originalRawData.getCursorPositionScan();
     * 
     * RawDataAtClient[] rawDatas = itemSelector.getRawDatas(); int[]
     * modifiedRawDataIDs = new int[rawDatas.length - 1]; int
     * modifiedRawDataIDsInd = 0;
     * 
     * for (RawDataAtClient r : rawDatas) {
     * 
     * if (originalRawData != r) {
     * 
     * modifiedRawDataIDs[modifiedRawDataIDsInd] = r.getRawDataID();
     * modifiedRawDataIDsInd++;
     *  // Check that selection and cursor position of source run are // valid
     * also for this target run if (r.getDataMinMZ() > cursorMZ) { tmpCursorMZ =
     * r.getDataMinMZ(); } else { tmpCursorMZ = cursorMZ; } if (r.getDataMaxMZ() <
     * cursorMZ) { tmpCursorMZ = r.getDataMaxMZ(); } else { tmpCursorMZ =
     * cursorMZ; }
     * 
     * if (r.getDataMinMZ() > startMZ) { tmpStartMZ = r.getDataMinMZ(); } else {
     * tmpStartMZ = startMZ; } if (r.getDataMaxMZ() < startMZ) { tmpStartMZ =
     * r.getDataMaxMZ(); } else { tmpStartMZ = startMZ; }
     * 
     * if (r.getDataMinMZ() > endMZ) { tmpEndMZ = r.getDataMinMZ(); } else {
     * tmpEndMZ = endMZ; } if (r.getDataMaxMZ() < endMZ) { tmpEndMZ =
     * r.getDataMaxMZ(); } else { tmpEndMZ = endMZ; }
     * 
     * if ((r.getNumOfScans() - 1) < cursorScan) { tmpCursorScan =
     * r.getNumOfScans() - 1; } else { tmpCursorScan = cursorScan; }
     * 
     * if ((r.getNumOfScans() - 1) < endScan) { tmpEndScan = r.getNumOfScans() -
     * 1; } else { tmpEndScan = endScan; } tmpStartScan = startScan; // Start
     * scan is 0 for all runs
     * 
     * if (whatDims == SET_SAME_ZOOM_MZ) { r.setSelectionMZ(tmpStartMZ,
     * tmpEndMZ); r.setCursorPositionMZ(tmpCursorMZ); //
     * r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_MZ, // statBar); }
     * 
     * if (whatDims == SET_SAME_ZOOM_SCAN) { r.setSelectionScan(tmpStartScan,
     * tmpEndScan); r.setCursorPositionScan(tmpCursorScan); //
     * r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_SCAN, // statBar); }
     * 
     * if (whatDims == SET_SAME_ZOOM_BOTH) { r.setSelection(tmpStartScan,
     * tmpEndScan, tmpStartMZ, tmpEndMZ); r.setCursorPosition(tmpCursorScan,
     * tmpCursorMZ); //
     * r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_BOTH, // statBar); } } }
     * 
     * if (whatDims == SET_SAME_ZOOM_MZ) { startRefreshRawDataVisualizers(
     * RawDataVisualizer.CHANGETYPE_SELECTION_MZ, modifiedRawDataIDs); } if
     * (whatDims == SET_SAME_ZOOM_SCAN) { startRefreshRawDataVisualizers(
     * RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, modifiedRawDataIDs); } if
     * (whatDims == SET_SAME_ZOOM_BOTH) { startRefreshRawDataVisualizers(
     * RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, modifiedRawDataIDs); }
     *  }
     * 
     *  /* public FormatCoordinates getFormatCoordinates() { return
     * paramSettings.getFormatCoordinates(); }
     * 
     * 
     * 
     * 
     */
    /**
     * Prepares everything for quit and then shutdowns the application
     */
    public void exitMZmine() {

        // Ask if use really wants to quit
        int selectedValue = JOptionPane.showInternalConfirmDialog(desktop,
                "Are you sure you want to exit MZmine?", "Exiting...",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (selectedValue != JOptionPane.YES_OPTION)
            return;

        dispose();
        System.exit(0);

    }

    public void displayErrorMessage(String msg) {
        // statBar.setStatusText(msg);
        JOptionPane.showMessageDialog(this, msg, "Sorry",
                JOptionPane.ERROR_MESSAGE);
    }

    public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {

        menuBar.addMenuItem(parentMenu, newItem);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#setStatusBarString(java.lang.String)
     */
    public void setStatusBarText(String msg) {
        statBar.setStatusText(msg);

    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getSelectedRawData()
     */
    public RawDataFile[] getSelectedRawData() {
        return itemSelector.getSelectedRawData();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getFirstSelectedRawData()
     */
    public RawDataFile getFirstSelectedRawData() {
        return itemSelector.getFirstSelectedRawData();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addSelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void addSelectionListener(ListSelectionListener listener) {
        itemSelector.addSelectionListener(listener);
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.io.IOController,
     *      net.sf.mzmine.taskcontrol.TaskController,
     *      net.sf.mzmine.userinterface.Desktop, java.util.logging.Logger)
     */
    public void initModule(IOController ioController,
            TaskController taskController, Desktop d, Logger logger) {
        this.ioController = ioController;
        this.taskController = taskController;
        this.logger = logger;

        // Setup data structures
        // ---------------------

        // Load default parameter settings
        // parameterStorage.readParametesFromFile(settingsFilename); (not
        // automatic)

        // Initialize structures for storing visualizers
        rawDataVisualizers = new Hashtable<Integer, Vector<RawDataVisualizer>>();
        alignmentResultVisualizers = new Hashtable<Integer, Vector<AlignmentResultVisualizer>>();

        // Setup GUI
        // ---------

        // Initialize options window (controller for parameter settings)

        // Initialize status bar
        statBar = new Statusbar(this);

        // Initialize item selector
        itemSelector = new ItemSelector(this);

        // Initialize method parameter storage (and default parameter values
        // too)

        // Construct menu

        menuBar = new MainMenu(ioController, this);

        setJMenuBar(menuBar);

        // Place objects on main window
        desktop = new JDesktopPane();
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, itemSelector,
                desktop);

        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(split, BorderLayout.CENTER);
        c.add(statBar, BorderLayout.SOUTH);

        // Initialize window listener for responding to user events
        addWindowListener(this);

        // menuBar.updateMenuAvailability();

        pack();

        // TODO: check screen size?
        setBounds(0, 0, 1000, 700);
        setLocationRelativeTo(null);

        // Application wants to control closing by itself
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("MZmine");

        statBar.setStatusText("Welcome to MZmine!");

        taskList = new TaskProgressWindow((TaskControllerImpl) taskController);
        desktop.add(taskList, javax.swing.JLayeredPane.DEFAULT_LAYER);

    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getMainWindow()
     */
    public JFrame getMainWindow() {
        return this;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#isRawDataSelected()
     */
    public boolean isRawDataSelected() {
        return itemSelector.getSelectedRawData().length > 0;
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addMenuItem(net.sf.mzmine.userinterface.Desktop.MZmineMenu,
     *      java.lang.String, java.awt.event.ActionListener, java.lang.String,
     *      int, boolean, boolean)
     */
    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            ActionListener listener, String actionCommand, int mnemonic,
            boolean setAccelerator, boolean enabled) {
        return menuBar.addMenuItem(parentMenu, text, listener, actionCommand,
                mnemonic, setAccelerator, enabled);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#addMenuSeparator(net.sf.mzmine.userinterface.Desktop.MZmineMenu)
     */
    public void addMenuSeparator(MZmineMenu parentMenu) {
        menuBar.addMenuSeparator(parentMenu);

    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getSelectedFrame()
     */
    public JInternalFrame getSelectedFrame() {
        return desktop.getSelectedFrame();
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getVisibleFrames()
     */
    public JInternalFrame[] getVisibleFrames() {

        JInternalFrame[] allFrames = desktop.getAllFrames();

        ArrayList<JInternalFrame> visibleFrames = new ArrayList<JInternalFrame>();
        for (JInternalFrame frame : allFrames)
            if (frame.isVisible())
                visibleFrames.add(frame);

        return visibleFrames.toArray(new JInternalFrame[0]);
    }

    /**
     * @see net.sf.mzmine.userinterface.Desktop#getVisibleFrames()
     */
    public JInternalFrame[] getVisibleFrames(Class frameClass) {

        JInternalFrame[] allFrames = desktop.getAllFrames();

        ArrayList<JInternalFrame> visibleFrames = new ArrayList<JInternalFrame>();
        for (JInternalFrame frame : allFrames)
            if (frame.isVisible() && (frameClass.isInstance(frame)))
                visibleFrames.add(frame);

        return visibleFrames.toArray(new JInternalFrame[0]);
    }

}
