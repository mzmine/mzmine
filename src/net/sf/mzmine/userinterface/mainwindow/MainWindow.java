/*
 * Copyright 2005 VTT Biotechnology
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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.userinterface.dialogs.TaskProgressWindow;
import net.sf.mzmine.util.GeneralParameters;
import net.sf.mzmine.util.ParameterStorage;
import net.sf.mzmine.visualizers.RawDataVisualizer;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizer;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotViewParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCoVarPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerList;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerLogratioPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotViewParameters;
import net.sf.mzmine.visualizers.peaklist.RawDataVisualizerPeakListView;
import net.sf.mzmine.visualizers.rawdata.spectra.RawDataVisualizerSpectrumView;
import net.sf.mzmine.visualizers.rawdata.tic.TICVisualizer;
import net.sf.mzmine.visualizers.rawdata.twod.RawDataVisualizerTwoDView;

/**
 * This class is the main window of application
 * 
 */
public class MainWindow extends JFrame implements WindowListener {

    private static MainWindow myInstance;

    // This is the .ini file for GUI program (client for cluster has different
    // .ini file)
    private static final String settingsFilename = "mzmine.ini";

    // These constants are used with setSameZoomToOtherRawDatas method
    public static final int SET_SAME_ZOOM_MZ = 1;

    public static final int SET_SAME_ZOOM_SCAN = 2;

    public static final int SET_SAME_ZOOM_BOTH = 3;

    public static MainWindow getInstance() {
        return myInstance;
    }

    // GUI components
    private JDesktopPane desktop;


    private MainMenu menuBar;

    private Statusbar statBar;

    private JSplitPane split;

    private ParameterStorage parameterStorage;

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

    /**
     * Constructor for MainWindow
     */
    public MainWindow(String title) {

        assert myInstance == null;
        myInstance = this;

        // Setup data structures
        // ---------------------

        // Load default parameter settings
        parameterStorage = new ParameterStorage();
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
        itemSelector = new ItemSelector();

        // Initialize method parameter storage (and default parameter values
        // too)
        parameterStorage = new ParameterStorage();

        // Construct menu

        menuBar = new MainMenu();

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

        menuBar.updateMenuAvailability();

        pack();

        setBounds(0, 0, 800, 600);
        setLocationRelativeTo(null);

        // Application wants to control closing by itself
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle(title);

        statBar.setStatusText("Welcome to MZmine!");

        System.out.println(desktop.getWidth());
        taskList = new TaskProgressWindow(this);
        desktop.add(taskList, javax.swing.JLayeredPane.DEFAULT_LAYER);

    }

    public MainMenu getMainMenu() {
        return menuBar;
    }

    /**
     * This method returns the desktop
     */
    public JDesktopPane getDesktop() {
        return desktop;
    }

    public ItemSelector getItemSelector() {
        return itemSelector;
    }


    public ParameterStorage getParameterStorage() {
        return parameterStorage;
    }

    /**
     * This method moves all visualizers of given raw data to the top on desktop
     */
    public void moveVisualizersToFront(RawDataAtClient rawData) {

        Integer rawDataID = new Integer(rawData.getRawDataID());

        // Get all visualizers for this raw data file
        Vector<RawDataVisualizer> visualizers = rawDataVisualizers
                .get(rawDataID);

        if (visualizers == null) {
            return;
        }

        // Move each visualizer window to front
        boolean somethingIsSelected = false;
        JInternalFrame jif = null;
        for (RawDataVisualizer vis : visualizers) {
            jif = (JInternalFrame) vis;
            if (jif.isSelected()) {
                somethingIsSelected = true;
            }
            jif.moveToFront();
        }

        if ((!somethingIsSelected) && (visualizers != null)) {
            try {
                jif = (JInternalFrame) (visualizers.get(0));
                jif.setSelected(true);
            } catch (Exception e) {
            }
        }

    }

    /**
     * This method moves all visualizers of given alignment result to the top on
     * desktop
     */
    public void moveVisualizersToFront(AlignmentResult result) {

        Integer alignmentResultID = new Integer(result.getAlignmentResultID());

        // Get all visualizer for this alignment result
        Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers
                .get(alignmentResultID);

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
        Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers
                .get(new Integer(rawDataID));

        // If raw data file has some visualizers, check if any of them is
        // visible
        if (visualizerVector != null) {
            Enumeration<RawDataVisualizer> visualizers = visualizerVector
                    .elements();
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

    private int numOfResultsWithVisibleVisualizer(boolean includeIcons) {

        int num = 0;
        int[] alignmentResultIDs = itemSelector.getAlignmentResultIDs();

        for (int id : alignmentResultIDs) {
            if (alignmentHasVisibleVisualizers(id, includeIcons)) {
                num++;
            }
        }

        return num;
    }

    private boolean alignmentHasVisibleVisualizers(int alignmentResultID,
            boolean includeIcons) {

        // Get visualizer vector for this raw data ID
        Vector<AlignmentResultVisualizer> visualizerVector = alignmentResultVisualizers
                .get(new Integer(alignmentResultID));

        // If raw data file has some visualizers, check if any of them is
        // visible
        if (visualizerVector != null) {
            Enumeration<AlignmentResultVisualizer> visualizers = visualizerVector
                    .elements();
            while (visualizers.hasMoreElements()) {
                AlignmentResultVisualizer vis = visualizers.nextElement();
                JInternalFrame jif = (JInternalFrame) vis;
                if (includeIcons) {
                    if (jif.isVisible()) {
                        return true;
                    }
                } else {
                    if (jif.isVisible() && !(jif.isIcon())) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    /**
     * This method tiles all visible visualizer windows
     */
    public void tileWindows() {

        final double goldenCut = 0.618033989;

        double widthForRawDataFiles = 0;
        double heightForRawDataFile = 0;
        double widthForAlignmentResults = 0;
        double heightForAlignmentResult = 0;

        double currentX;
        double currentY;
        JInternalFrame jif;

        // Calculate number of raw data files and alignment results with one or
        // more open visualizer windows
        int numViewableRawDataFiles = 0; // numOfRawDataWithVisibleVisualizer(false);
        int numViewableResults = 0; // numOfResultsWithVisibleVisualizer(false);

        // Arrange visualizers for raw data files
        if (numViewableRawDataFiles > 0) {

            // Determine space that is available for each run's visualizers on
            // the main window
            if (numViewableResults == 0) {
                widthForRawDataFiles = desktop.getWidth();
            } else {
                widthForRawDataFiles = goldenCut * desktop.getWidth();
            }

            heightForRawDataFile = desktop.getHeight()
                    / numViewableRawDataFiles;

            currentX = 0;
            currentY = 0;

            // Arrange visualizers of each raw data file
            // Enumeration<Vector<RawDataVisualizer>> allVisualizerVectors =
            // rawDataVisualizers.elements();
            // while (allVisualizerVectors.hasMoreElements()) {
            int rawDataIDs[] = null; // getItemSelector().getRawDataIDs();
            for (int rawDataID : rawDataIDs) {

                RawDataAtClient rawData = getItemSelector().getRawDataByID(
                        rawDataID);
                Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers
                        .get(new Integer(rawDataID));

                // Get all visualizers for this raw data file
                // Vector<RawDataVisualizer> visualizerVector =
                // allVisualizerVectors.nextElement();
                if (visualizerVector == null) {
                    continue;
                }

                // Calculate how many of these visualizers should be shown
                int numToBeShown = 0;
                Enumeration<RawDataVisualizer> visualizers = visualizerVector
                        .elements();
                while (visualizers.hasMoreElements()) {
                    jif = (JInternalFrame) visualizers.nextElement();
                    if (jif.isVisible() && !(jif.isIcon())) {
                        numToBeShown++;
                    }
                    // if (jif.isIcon() || !jif.isVisible()) {} else {
                    // numToBeShown++; }
                }

                // If there are visible visualizers, then arrange all of the
                // visualizers for this raw data file
                if (numToBeShown > 0) {

                    if (!rawData.hasPeakData()) {
                        // TIC
                        jif = (JInternalFrame) visualizerVector.get(0);
                        jif.setLocation((int) currentX, (int) currentY);
                        jif.setSize((int) (goldenCut * widthForRawDataFiles),
                                (int) (heightForRawDataFile / 2.0));
                        // Scan
                        jif = (JInternalFrame) visualizerVector.get(1);
                        jif
                                .setLocation(
                                        (int) currentX,
                                        (int) (currentY + 1 + heightForRawDataFile / 2.0));
                        jif.setSize((int) (1.0 * widthForRawDataFiles),
                                (int) (heightForRawDataFile / 2.0));
                        // 2D
                        jif = (JInternalFrame) visualizerVector.get(2);
                        jif.setLocation((int) (currentX + goldenCut
                                * widthForRawDataFiles), (int) currentY);
                        jif.setSize(
                                (int) ((1 - goldenCut) * widthForRawDataFiles),
                                (int) (heightForRawDataFile / 2.0));
                    } else {

                        // TIC
                        jif = (JInternalFrame) visualizerVector.get(0);
                        jif.setLocation((int) currentX, (int) currentY);
                        jif.setSize((int) (goldenCut * widthForRawDataFiles),
                                (int) (heightForRawDataFile / 3.0));
                        // Scan
                        jif = (JInternalFrame) visualizerVector.get(1);
                        jif
                                .setLocation(
                                        (int) currentX,
                                        (int) (currentY + 1 + heightForRawDataFile / 3.0));
                        jif.setSize((int) (goldenCut * widthForRawDataFiles),
                                (int) (heightForRawDataFile / 3.0));
                        // 2D
                        jif = (JInternalFrame) visualizerVector.get(2);
                        jif.setLocation((int) (currentX + goldenCut
                                * widthForRawDataFiles), (int) currentY);
                        jif.setSize(
                                (int) ((1 - goldenCut) * widthForRawDataFiles),
                                (int) (heightForRawDataFile));
                        // Peaklist
                        jif = (JInternalFrame) visualizerVector.get(3);
                        jif
                                .setLocation(
                                        (int) currentX,
                                        (int) (currentY + 1 + heightForRawDataFile * 2.0 / 3.0));
                        jif.setSize((int) (goldenCut * widthForRawDataFiles),
                                (int) (heightForRawDataFile / 3.0));
                    }

                    currentY += heightForRawDataFile;

                } // shouldBeShown

            } // raw data files loop

        } // numViewableRawDataFiles>0

        if (numViewableResults > 0) {

            currentY = 0;
            currentX = widthForRawDataFiles + 1;

            // Determine space available for each results' visualizers on main
            // window
            widthForAlignmentResults = desktop.getWidth()
                    - widthForRawDataFiles;
            heightForAlignmentResult = desktop.getHeight() / numViewableResults;

            Enumeration<Vector<AlignmentResultVisualizer>> allVisualizerVectors = alignmentResultVisualizers
                    .elements();
            while (allVisualizerVectors.hasMoreElements()) {

                Vector<AlignmentResultVisualizer> visualizerVector = allVisualizerVectors
                        .nextElement();
                if (visualizerVector == null) {
                    continue;
                }

                // Count how many of these visualizers should be shown
                int numToBeShown = 0;
                Enumeration<AlignmentResultVisualizer> visualizers = visualizerVector
                        .elements();
                while (visualizers.hasMoreElements()) {
                    jif = (JInternalFrame) visualizers.nextElement();
                    if (jif.isIcon() || !jif.isVisible()) {
                    } else {
                        numToBeShown++;
                    }
                }

                // If there are visible visualizers, then arrange all of the
                // visualizers for this raw data file
                if (numToBeShown > 0) {
                    visualizers = visualizerVector.elements();
                    while (visualizers.hasMoreElements()) {
                        jif = (JInternalFrame) visualizers.nextElement();
                        if (!jif.isIcon() && jif.isVisible()) {
                            jif.setLocation((int) currentX, (int) currentY);
                            jif
                                    .setSize(
                                            (int) (widthForAlignmentResults),
                                            (int) (heightForAlignmentResult / (double) numToBeShown));
                            currentY += heightForAlignmentResult
                                    / (double) numToBeShown;
                        }
                    }
                }
            }
        }

    } // Method







    
 
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
    public void paintNow() {
        update(getGraphics());
    }

    public void closeRawDataFiles(int[] rawDataIDs) {

        if (rawDataIDs.length > 0) {
            // Check that any of these files is not participating any alignment
            String errorMessage = null;
            for (int rawDataID : rawDataIDs) {
                RawDataAtClient rawData = itemSelector
                        .getRawDataByID(rawDataID);
                if (rawData.getAlignmentResultIDs().size() > 0) {
                    Vector<Integer> alignmentResultIDs = rawData
                            .getAlignmentResultIDs();
                    errorMessage = rawData.getNiceName()
                            + " is participating in alignment(s). Before closing the raw data files, please close alignment result(s): ";
                    for (Integer alignmentResultID : alignmentResultIDs) {
                        errorMessage += itemSelector.getAlignmentResultByID(
                                alignmentResultID.intValue()).getNiceName()
                                + ", ";
                    }
                }
                if (errorMessage != null) {
                    displayErrorMessage(errorMessage);
                    return;
                }
            }

            // Remove all visualizers for these raw data files
            for (int rawDataID : rawDataIDs) {
                removeRawDataVisualizers(rawDataID);
            }

            // clientForCluster.closeRawDataFiles(rawDataIDs);

            getStatusBar().setStatusText(
                    "Closing " + rawDataIDs.length + " raw data file(s).");

        }

    }


    public void closeAlignmentResults(int[] alignmentResultIDs) {

        if (alignmentResultIDs.length > 0) {

            for (int alignmentResultID : alignmentResultIDs) {

                if (itemSelector.getAlignmentResultByID(alignmentResultID)
                        .isImported()) {
                    continue;
                }

                // Remove dependency from each involved raw data file
                int[] rawDataIDs = itemSelector.getAlignmentResultByID(
                        alignmentResultID).getRawDataIDs();
                for (int rawDataID : rawDataIDs) {
                    RawDataAtClient rawData = itemSelector
                            .getRawDataByID(rawDataID);
                    rawData.removeAlignmentResultID(alignmentResultID);
                }
            }

            for (int alignmentResultID : alignmentResultIDs) {
                // Close all visualizers for these alignment results
                removeAlignmentResultVisualizers(alignmentResultID);

                // Remove these alignment results from the item selector
                itemSelector.removeAlignmentResult(itemSelector
                        .getAlignmentResultByID(alignmentResultID));
            }

            getStatusBar().setStatusText(
                    "Closed " + alignmentResultIDs.length
                            + " alignment result(s).");

        }

    }

    public void doLinearNormalizationClientSide(LinearNormalizerParameters lnp,
            Vector<AlignmentResult> originalAlignmentResults) {
        statBar.setStatusText("Normalizing selected alignment results.");
        paintNow();

        setBusy(true);

        LinearNormalizer ln = new LinearNormalizer();
        Enumeration<AlignmentResult> originalAlignmentResultEnum = originalAlignmentResults
                .elements();
        while (originalAlignmentResultEnum.hasMoreElements()) {
            AlignmentResult ar = originalAlignmentResultEnum.nextElement();
            AlignmentResult nar = ln
                    .calcNormalization(MainWindow.this, ar, lnp);
            itemSelector.addAlignmentResult(nar);
            addAlignmentResultVisualizerList(nar);
        }

        setBusy(false);
        statBar.setStatusText("Normalization done.");
    }

    public void runAlignmentResultFilteringByGapsClientSide(
            AlignmentResultFilterByGapsParameters arpp,
            Vector<AlignmentResult> originalAlignmentResults) {
        statBar.setStatusText("Filtering selected alignment results.");
        paintNow();

        setBusy(true);

        AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();

        Enumeration<AlignmentResult> originalAlignmentResultEnum = originalAlignmentResults
                .elements();
        while (originalAlignmentResultEnum.hasMoreElements()) {
            AlignmentResult ar = originalAlignmentResultEnum.nextElement();
            AlignmentResult nar = arfbg.processAlignment(MainWindow.this, ar,
                    (AlignmentResultProcessorParameters) arpp);
            itemSelector.addAlignmentResult(nar);
            addAlignmentResultVisualizerList(nar);
        }

        setBusy(false);
        statBar.setStatusText("Alignment result filtering done.");

    }

    public AlignmentResult runAlignmentResultFilteringByGapsClientSide(
            AlignmentResultFilterByGapsParameters arpp,
            AlignmentResult originalAlignmentResult) {
        statBar.setStatusText("Filtering alignment result.");
        paintNow();

        setBusy(true);

        AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();

        AlignmentResult nar = arfbg.processAlignment(MainWindow.this,
                originalAlignmentResult,
                (AlignmentResultProcessorParameters) arpp);
        itemSelector.addAlignmentResult(nar);
        addAlignmentResultVisualizerList(nar);

        setBusy(false);
        statBar.setStatusText("Alignment result filtering done.");
        return nar;

    }

    /**
     * Copies the zoom settings from given run to all other runs. whatDims
     * parameter defines wheter to copy zoom settings only in mz, rt or both
     * dimensions
  
    public void setSameZoomToOtherRawDatas(RawDataAtClient originalRawData,
            int whatDims) {

        double startMZ = 0;
        double endMZ = 0;
        int startScan = 0;
        int endScan = 0;
        double cursorMZ = 0;
        int cursorScan = 0;

        double tmpStartMZ;
        double tmpEndMZ;
        int tmpStartScan;
        int tmpEndScan;

        double tmpCursorMZ;
        int tmpCursorScan;

        startMZ = originalRawData.getSelectionMZStart();
        endMZ = originalRawData.getSelectionMZEnd();
        startScan = originalRawData.getSelectionScanStart();
        endScan = originalRawData.getSelectionScanEnd();
        cursorMZ = originalRawData.getCursorPositionMZ();
        cursorScan = originalRawData.getCursorPositionScan();

        RawDataAtClient[] rawDatas = itemSelector.getRawDatas();
        int[] modifiedRawDataIDs = new int[rawDatas.length - 1];
        int modifiedRawDataIDsInd = 0;

        for (RawDataAtClient r : rawDatas) {

            if (originalRawData != r) {

                modifiedRawDataIDs[modifiedRawDataIDsInd] = r.getRawDataID();
                modifiedRawDataIDsInd++;

                // Check that selection and cursor position of source run are
                // valid also for this target run
                if (r.getDataMinMZ() > cursorMZ) {
                    tmpCursorMZ = r.getDataMinMZ();
                } else {
                    tmpCursorMZ = cursorMZ;
                }
                if (r.getDataMaxMZ() < cursorMZ) {
                    tmpCursorMZ = r.getDataMaxMZ();
                } else {
                    tmpCursorMZ = cursorMZ;
                }

                if (r.getDataMinMZ() > startMZ) {
                    tmpStartMZ = r.getDataMinMZ();
                } else {
                    tmpStartMZ = startMZ;
                }
                if (r.getDataMaxMZ() < startMZ) {
                    tmpStartMZ = r.getDataMaxMZ();
                } else {
                    tmpStartMZ = startMZ;
                }

                if (r.getDataMinMZ() > endMZ) {
                    tmpEndMZ = r.getDataMinMZ();
                } else {
                    tmpEndMZ = endMZ;
                }
                if (r.getDataMaxMZ() < endMZ) {
                    tmpEndMZ = r.getDataMaxMZ();
                } else {
                    tmpEndMZ = endMZ;
                }

                if ((r.getNumOfScans() - 1) < cursorScan) {
                    tmpCursorScan = r.getNumOfScans() - 1;
                } else {
                    tmpCursorScan = cursorScan;
                }

                if ((r.getNumOfScans() - 1) < endScan) {
                    tmpEndScan = r.getNumOfScans() - 1;
                } else {
                    tmpEndScan = endScan;
                }
                tmpStartScan = startScan; // Start scan is 0 for all runs

                if (whatDims == SET_SAME_ZOOM_MZ) {
                    r.setSelectionMZ(tmpStartMZ, tmpEndMZ);
                    r.setCursorPositionMZ(tmpCursorMZ);
                    // r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_MZ,
                    // statBar);
                }

                if (whatDims == SET_SAME_ZOOM_SCAN) {
                    r.setSelectionScan(tmpStartScan, tmpEndScan);
                    r.setCursorPositionScan(tmpCursorScan);
                    // r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_SCAN,
                    // statBar);
                }

                if (whatDims == SET_SAME_ZOOM_BOTH) {
                    r.setSelection(tmpStartScan, tmpEndScan, tmpStartMZ,
                            tmpEndMZ);
                    r.setCursorPosition(tmpCursorScan, tmpCursorMZ);
                    // r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_BOTH,
                    // statBar);
                }
            }
        }

        if (whatDims == SET_SAME_ZOOM_MZ) {
            startRefreshRawDataVisualizers(
                    RawDataVisualizer.CHANGETYPE_SELECTION_MZ,
                    modifiedRawDataIDs);
        }
        if (whatDims == SET_SAME_ZOOM_SCAN) {
            startRefreshRawDataVisualizers(
                    RawDataVisualizer.CHANGETYPE_SELECTION_SCAN,
                    modifiedRawDataIDs);
        }
        if (whatDims == SET_SAME_ZOOM_BOTH) {
            startRefreshRawDataVisualizers(
                    RawDataVisualizer.CHANGETYPE_SELECTION_BOTH,
                    modifiedRawDataIDs);
        }

    }


    /*
     * public FormatCoordinates getFormatCoordinates() { return
     * paramSettings.getFormatCoordinates(); }



    
*/
    /**
     * Prepares everything for quit and then shutdowns the application
     */
    void exitMZmine() {

        // Ask if use really wants to quit
        int selectedValue = JOptionPane.showInternalConfirmDialog(desktop,
                "Are you sure you want to exit MZmine?", "Exiting...",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (selectedValue != JOptionPane.YES_OPTION) {
            statBar.setStatusText("Exit cancelled.");
            return;
        }

        // Close all alignment results
       // int[] alignmentResultIDs = itemSelector.getAlignmentResultIDs();
        // closeAlignmentResults(alignmentResultIDs);

        // Close all raw data files
      //  int[] rawDataIDs = itemSelector.getRawDataIDs();
        // closeRawDataFiles(rawDataIDs);

        // Disconnect client from cluster
        // clientForCluster.disconnectFromController();

        // Save settings
        // (not automatic)

        // Shutdown
        dispose();
        System.exit(0);


    }
    public void displayErrorMessage(String msg) {
            statBar.setStatusText(msg);
            JOptionPane.showInternalMessageDialog(myInstance.getDesktop(), msg, "Sorry",
                    JOptionPane.ERROR_MESSAGE);
    }

}
