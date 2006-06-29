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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.io.PeakListWriter;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.filtering.chromatographicmedian.ChromatographicMedianFilter;
import net.sf.mzmine.methods.filtering.chromatographicmedian.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.filtering.crop.CropFilter;
import net.sf.mzmine.methods.filtering.crop.CropFilterParameters;
import net.sf.mzmine.methods.filtering.mean.MeanFilter;
import net.sf.mzmine.methods.filtering.mean.MeanFilterParameters;
import net.sf.mzmine.methods.filtering.savitzkygolay.SavitzkyGolayFilter;
import net.sf.mzmine.methods.filtering.savitzkygolay.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.filtering.zoomscan.ZoomScanFilter;
import net.sf.mzmine.methods.filtering.zoomscan.ZoomScanFilterParameters;
import net.sf.mzmine.methods.peakpicking.centroid.CentroidPicker;
import net.sf.mzmine.methods.peakpicking.centroid.CentroidPickerParameters;
import net.sf.mzmine.methods.peakpicking.recursivethreshold.RecursiveThresholdPicker;
import net.sf.mzmine.methods.peakpicking.recursivethreshold.RecursiveThresholdPickerParameters;
import net.sf.mzmine.methods.peakpicking.local.LocalPicker;
import net.sf.mzmine.methods.peakpicking.local.LocalPickerParameters;
import net.sf.mzmine.userinterface.dialogs.FileOpenDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCoVarPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerLogratioPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotView;
import net.sf.mzmine.visualizers.rawdata.basepeak.BasePeakSetup;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectraSetup;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;
import net.sf.mzmine.visualizers.rawdata.threed.ThreeDSetup;
import net.sf.mzmine.visualizers.rawdata.tic.TICSetup;
import net.sf.mzmine.visualizers.rawdata.tic.TICVisualizer;
import net.sf.mzmine.visualizers.rawdata.twod.TwoDSetup;
import net.sf.mzmine.visualizers.rawdata.twod.TwoDVisualizer;
import sunutils.ExampleFileFilter;

/**
 *
 */
public class MainMenu extends JMenuBar implements ActionListener {

    private JMenu fileMenu;
    private JMenuItem fileOpen, fileClose, fileExportPeakList,
            fileExportAlignmentResult, fileSaveParameters, fileLoadParameters,
            filePrint, fileExit;
    private JMenu editMenu;
    private JMenuItem editCopy;
    private JMenu filterMenu;
    private JMenuItem ssMeanFilter, ssSGFilter, ssChromatographicMedianFilter,
            ssCropFilter, ssZoomScanFilter;
    private JMenu peakMenu;
    private JMenuItem ssRecursiveThresholdPicker, ssLocalPicker,
            ssCentroidPicker, ssSimpleDeisotoping, ssCombinatorialDeisotoping,
            ssIncompleteIsotopePatternFilter;
    private JMenu alignmentMenu;
    private JMenuItem tsJoinAligner, tsFastAligner, tsAlignmentFilter,
            tsEmptySlotFiller;
    private JMenu normalizationMenu;
    private JMenuItem normLinear, normStdComp;
    private JMenu batchMenu;
    private JMenuItem batDefine;
    private JMenu visualizationMenu;
    private JMenuItem visOpenTIC, visOpenBasePeak, visOpenSpectra, visOpenTwoD,
            visOpenThreeD;
    private JMenuItem visOpenSRView, visOpenSCVView, visOpenCDAView,
            visOpenSammonsView;
    private JMenu toolsMenu;
    private JMenuItem toolsOptions;
    private JMenu windowMenu;
    private JMenuItem windowTileWindows;
    private JMenu helpMenu;
    private JMenuItem hlpAbout;

    private Statusbar statBar;
    private MainWindow mainWin;
    private ItemSelector itemSelector;

    MainMenu() {

        mainWin = MainWindow.getInstance();
        statBar = mainWin.getStatusBar();
        itemSelector = mainWin.getItemSelector();

        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        this.add(fileMenu);

        fileOpen = GUIUtils.addMenuItem(fileMenu, "Open...", this,
                KeyEvent.VK_O, true);
        fileClose = GUIUtils.addMenuItem(fileMenu, "Close", this, KeyEvent.VK_C);
        fileMenu.addSeparator();
        fileExportPeakList = GUIUtils.addMenuItem(fileMenu,
                "Export peak list(s)...", this, KeyEvent.VK_E);
        fileExportAlignmentResult = GUIUtils.addMenuItem(fileMenu,
                "Export alignment result(s)...", this, KeyEvent.VK_A);
        fileMenu.addSeparator();
        fileSaveParameters = GUIUtils.addMenuItem(fileMenu,
                "Save parameters...", this, KeyEvent.VK_S);
        fileLoadParameters = GUIUtils.addMenuItem(fileMenu,
                "Load parameters...", this, KeyEvent.VK_S);
        fileMenu.addSeparator();
        filePrint = GUIUtils.addMenuItem(fileMenu, "Print figure...", this,
                KeyEvent.VK_P, true);
        fileMenu.addSeparator();
        fileExit = GUIUtils.addMenuItem(fileMenu, "Exit", this, KeyEvent.VK_X,
                true);

        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        this.add(editMenu);

        editCopy = GUIUtils.addMenuItem(editMenu, "Copy", this, KeyEvent.VK_C,
                true);

        filterMenu = new JMenu("Raw data filtering");
        filterMenu.setMnemonic(KeyEvent.VK_R);
        this.add(filterMenu);

        ssMeanFilter = GUIUtils.addMenuItem(filterMenu, "Mean filter spectra",
                this, KeyEvent.VK_M);
        ssSGFilter = GUIUtils.addMenuItem(filterMenu,
                "Savitzky-Golay filter spectra", this, KeyEvent.VK_S);
        ssChromatographicMedianFilter = GUIUtils.addMenuItem(filterMenu,
                "Chromatographic median filter", this, KeyEvent.VK_S);
        ssCropFilter = GUIUtils.addMenuItem(filterMenu, "Cropping filter",
                this, KeyEvent.VK_C);
        ssZoomScanFilter = GUIUtils.addMenuItem(filterMenu, "Zoom scan filter",
                this, KeyEvent.VK_Z);

        peakMenu = new JMenu("Peak detection");
        peakMenu.setMnemonic(KeyEvent.VK_P);
        this.add(peakMenu);

        ssRecursiveThresholdPicker = GUIUtils.addMenuItem(peakMenu,
                "Recursive threshold peak detector", this);
        ssLocalPicker = GUIUtils.addMenuItem(peakMenu,
                "Local maxima peak detector", this);
        ssCentroidPicker = GUIUtils.addMenuItem(peakMenu,
                "Centroid peak detector", this);
        peakMenu.addSeparator();
        ssSimpleDeisotoping = GUIUtils.addMenuItem(peakMenu,
                "Simple deisotoper", this);
        ssCombinatorialDeisotoping = GUIUtils.addMenuItem(peakMenu,
                "Combinatorial deisotoping", this);
        ssIncompleteIsotopePatternFilter = GUIUtils.addMenuItem(peakMenu,
                "Filter incomplete isotope patterns", this);

        alignmentMenu = new JMenu("Alignment");
        alignmentMenu.setMnemonic(KeyEvent.VK_A);
        this.add(alignmentMenu);

        tsJoinAligner = GUIUtils.addMenuItem(alignmentMenu, "Slow aligner",
                this, KeyEvent.VK_S);
        tsFastAligner = GUIUtils.addMenuItem(alignmentMenu, "Fast aligner",
                this, KeyEvent.VK_A);
        alignmentMenu.addSeparator();
        tsAlignmentFilter = GUIUtils.addMenuItem(alignmentMenu,
                "Filter out rare peaks", this, KeyEvent.VK_R);
        tsEmptySlotFiller = GUIUtils.addMenuItem(alignmentMenu,
                "Fill-in empty gaps", this, KeyEvent.VK_F);

        normalizationMenu = new JMenu("Normalization");
        normalizationMenu.setMnemonic(KeyEvent.VK_N);
        this.add(normalizationMenu);

        normLinear = GUIUtils.addMenuItem(normalizationMenu,
                "Linear normalization", this, KeyEvent.VK_L);
        normStdComp = GUIUtils.addMenuItem(normalizationMenu,
                "Normalization using standards", this, KeyEvent.VK_N);

        batchMenu = new JMenu("Batch mode");
        batchMenu.setMnemonic(KeyEvent.VK_B);
        this.add(batchMenu);

        batDefine = GUIUtils.addMenuItem(batchMenu, "Define batch operations",
                this, KeyEvent.VK_R);

        visualizationMenu = new JMenu("Visualization");
        visualizationMenu.setMnemonic(KeyEvent.VK_V);
        this.add(visualizationMenu);

        visOpenTIC = GUIUtils.addMenuItem(visualizationMenu, "TIC plot", this,
                KeyEvent.VK_T);
        visOpenBasePeak = GUIUtils.addMenuItem(visualizationMenu,
                "Base peak plot", this, KeyEvent.VK_B);
        visOpenSpectra = GUIUtils.addMenuItem(visualizationMenu,
                "Spectra plot", this, KeyEvent.VK_S);
        visOpenTwoD = GUIUtils.addMenuItem(visualizationMenu, "2D plot", this,
                KeyEvent.VK_2);
        visOpenThreeD = GUIUtils.addMenuItem(visualizationMenu, "3D plot",
                this, KeyEvent.VK_3);
        visualizationMenu.addSeparator();
        visOpenSRView = GUIUtils.addMenuItem(visualizationMenu,
                "Logratio plot", this, KeyEvent.VK_L);
        visOpenSCVView = GUIUtils.addMenuItem(visualizationMenu,
                "Coefficient of variation plot", this, KeyEvent.VK_V);
        visOpenCDAView = GUIUtils.addMenuItem(visualizationMenu,
                "CDA plot of samples", this, KeyEvent.VK_C);
        visOpenSammonsView = GUIUtils.addMenuItem(visualizationMenu,
                "Sammons plot of samples", this, KeyEvent.VK_S);

        toolsMenu = new JMenu("Configure");
        toolsMenu.setMnemonic(KeyEvent.VK_C);
        this.add(toolsMenu);

        toolsOptions = GUIUtils.addMenuItem(toolsMenu, "Preferences...", this,
                KeyEvent.VK_P);

        windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        this.add(windowMenu);

        windowTileWindows = GUIUtils.addMenuItem(windowMenu, "Tile Windows",
                this, KeyEvent.VK_T, true);

        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        this.add(helpMenu);

        hlpAbout = GUIUtils.addMenuItem(helpMenu, "About MZmine...", this,
                KeyEvent.VK_A);

        updateMenuAvailability();

    }

    /**
     * ActionListener interface implementation
     */
    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();



        // File -> Open
        if (src == fileOpen) {

            FileOpenDialog fileOpenDialog = new FileOpenDialog();
            fileOpenDialog.setVisible(true);

        }

        // File->Close
        if (src == fileClose) {

            // Grab selected raw data files
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                MZmineProject.getCurrentProject().removeFile(file);

            // int[] alignmentResultIDs = itemSelector
            // .getSelectedAlignmentResultIDs();

            // mainWin.closeAlignmentResults(alignmentResultIDs);

        }

        if (src == fileLoadParameters) {

            statBar.setStatusText("Please select a parameter file");

            // Build open dialog
            JFileChooser fileOpenChooser = new JFileChooser();
            fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            fileOpenChooser.setMultiSelectionEnabled(false);
            fileOpenChooser.setDialogTitle("Please select parameter file");

            // Setup file extension filter
            ExampleFileFilter filter = new ExampleFileFilter();
            filter.addExtension("mzmine-parameters");
            filter.setDescription("MZmine parameters file");
            fileOpenChooser.setFileFilter(filter);

            // Show dialog and test return value from user
            int retval = fileOpenChooser.showOpenDialog(mainWin);
            if (retval == JFileChooser.APPROVE_OPTION) {

                File selectedFile = fileOpenChooser.getSelectedFile();
                if (!(selectedFile.exists())) {
                    mainWin.displayErrorMessage("Selected parameter file "
                            + selectedFile + " does not exist!");
                    return;
                }

                // Read parameters from file
                try {
                    mainWin.getParameterStorage().readParameters(selectedFile);
                } catch (IOException ioexce) {
                    mainWin.displayErrorMessage("Failed to load parameter settings from file "
                            + selectedFile + ": " + ioexce.toString());
                }

            }

        }

        // File -> Save parameters
        if (src == fileSaveParameters) {

            // Build save dialog
            JFileChooser fileSaveChooser = new JFileChooser();
            fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileSaveChooser.setMultiSelectionEnabled(false);
            fileSaveChooser.setDialogTitle("Please give file name for parameters file.");

            // Setup file extension filter
            ExampleFileFilter filter = new ExampleFileFilter();
            filter.addExtension("mzmine-parameters");
            filter.setDescription("MZmine parameters file");
            fileSaveChooser.setFileFilter(filter);

            // Show dialog and test return value from user
            int retval = fileSaveChooser.showSaveDialog(mainWin);
            if (retval == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileSaveChooser.getSelectedFile();

                // Add extension .mzmine-parameters to file name unless it is
                // there already
                String extension = selectedFile.getName().substring(
                        selectedFile.getName().lastIndexOf(".") + 1).toLowerCase();
                if (!extension.equals("mzmine-parameters")) {
                    selectedFile = new File(selectedFile.getPath()
                            + ".mzmine-parameters");
                }

                // Write parameters to file
                try {
                    mainWin.getParameterStorage().writeParameters(selectedFile);
                } catch (IOException ioexce) {
                    mainWin.displayErrorMessage("Failed to save parameter settings from file "
                            + selectedFile + ": " + ioexce.toString());
                }

            }

        }

        if (src == fileExportPeakList) {

            MZmineProject proj = MZmineProject.getCurrentProject();
            // Grab selected raw data files
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles) {
                if (proj.hasPeakList(file)) {

                    // Build save dialog
                    JFileChooser fileSaveChooser = new JFileChooser();
                    fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    fileSaveChooser.setMultiSelectionEnabled(false);
                    fileSaveChooser.setDialogTitle("Please give file name for peak list of "
                            + file.toString());

                    // Setup file extension filter
                    ExampleFileFilter filter = new ExampleFileFilter();
                    filter.addExtension("txt");
                    filter.setDescription("Tab-delimitted text file");
                    fileSaveChooser.setFileFilter(filter);

                    // Show dialog and test return value from user
                    int retval = fileSaveChooser.showSaveDialog(mainWin);
                    if (retval == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileSaveChooser.getSelectedFile();

                        // Add extension .txt to file name unless it is there
                        // already
                        String extension = selectedFile.getName().substring(
                                selectedFile.getName().lastIndexOf(".") + 1).toLowerCase();
                        if (!extension.equals("txt")) {
                            selectedFile = new File(selectedFile.getPath()
                                    + ".txt");
                        }

                        // Export peak list to file
                        try {
                            PeakListWriter.exportPeakListToFile(file,
                                    selectedFile);
                        } catch (IOException ioexce) {
                            mainWin.displayErrorMessage("Failed to export peak list for file "
                                    + file.toString()
                                    + ": "
                                    + ioexce.toString());
                        }

                    }

                }

            }

        }

        // File -> Exit
        if (src == fileExit) {
            statBar.setStatusText("Exiting.");
            mainWin.exitMZmine();
        }

        // Filter -> Mean
        if (src == ssMeanFilter) {

            // Ask parameters from user
            MeanFilter filter = new MeanFilter();
            MeanFilterParameters filterParam = mainWin.getParameterStorage().getMeanFilterParameters();

            startFilter(filter, filterParam);

        }

        // Filter -> Chromatographic median
        if (src == ssChromatographicMedianFilter) {

            // Ask parameters from user
            ChromatographicMedianFilter filter = new ChromatographicMedianFilter();
            ChromatographicMedianFilterParameters filterParam = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();

            startFilter(filter, filterParam);

        }

        // Filter -> Crop
        if (src == ssCropFilter) {

            // Ask parameters from user
            CropFilter filter = new CropFilter();
            CropFilterParameters filterParam = mainWin.getParameterStorage().getCropFilterParameters();

            startFilter(filter, filterParam);

        }

        // Filter -> Crop
        if (src == ssSGFilter) {

            // Ask parameters from user
            SavitzkyGolayFilter filter = new SavitzkyGolayFilter();
            SavitzkyGolayFilterParameters filterParam = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();

            startFilter(filter, filterParam);
        }

        // Filter -> Zoom scan
        if (src == ssZoomScanFilter) {

            ZoomScanFilter filter = new ZoomScanFilter();
            ZoomScanFilterParameters filterParam = mainWin.getParameterStorage().getZoomScanFilterParameters();

            startFilter(filter, filterParam);

        }

        if (src == ssRecursiveThresholdPicker) {

            RecursiveThresholdPicker picker = new RecursiveThresholdPicker();
            RecursiveThresholdPickerParameters pickerParam = mainWin.getParameterStorage().getRecursiveThresholdPickerParameters();

            startPeakPicker(picker, pickerParam);

        }

        if (src == ssCentroidPicker) {

            CentroidPicker picker = new CentroidPicker();
            CentroidPickerParameters pickerParam = mainWin.getParameterStorage().getCentroidPickerParameters();
            startPeakPicker(picker, pickerParam);

        }

        if (src == ssLocalPicker) {

            LocalPicker picker = new LocalPicker();
            LocalPickerParameters pickerParam = mainWin.getParameterStorage().getLocalPickerParameters();
            startPeakPicker(picker, pickerParam);

		}

        // Visualization -> TIC plot
        if (src == visOpenTIC) {
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                TICSetup.showSetupDialog(file);
        }

        // Visualization -> Base peak plot
        if (src == visOpenBasePeak) {
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                BasePeakSetup.showSetupDialog(file);
        }

        // Visualization -> Spectrum plot
        if (src == visOpenSpectra) {
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                SpectraSetup.showSetupDialog(file);
        }

        // Visualization -> 2D plot
        if (src == visOpenTwoD) {
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                TwoDSetup.showSetupDialog(file);
        }

        // Visualization -> 3D plot
        if (src == visOpenThreeD) {
            RawDataFile[] selectedFiles = itemSelector.getSelectedRawData();
            for (RawDataFile file : selectedFiles)
                ThreeDSetup.showSetupDialog(file);
        }

    }

    private void startFilter(Method filter, MethodParameters filterParam) {

        // Ask parameters from user
        if (!(filter.askParameters((MethodParameters) filterParam))) {
            statBar.setStatusText("Filtering cancelled.");
            return;
        }

        // It seems user didn't cancel
        statBar.setStatusText("Filtering spectra.");

        RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();

        filter.runMethod(filterParam, rawDataFiles, null);

    }

    private void startPeakPicker(Method picker, MethodParameters pickerParam) {

        if (!(picker.askParameters((MethodParameters) pickerParam))) {
            statBar.setStatusText("Peak picking cancelled.");
            return;
        }

        // It seems user didn't cancel
        statBar.setStatusText("Finding peaks.");

        // Check if selected data files have previous peak detection results
        RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();
        boolean previousExists = false;
        MZmineProject proj = MZmineProject.getCurrentProject();
        for (RawDataFile r : rawDataFiles)
            if (proj.hasPeakList(r)) {
                previousExists = true;
                break;
            }

        // Show warning if going to remove previous lists
        if (previousExists) {
            // Ask if is it ok to replace existing peak picking results
            int selectedValue = JOptionPane.showInternalConfirmDialog(
                    mainWin.getDesktop(),
                    "Previous peak picking results will be overwritten. Do you want to continue?",
                    "Overwrite?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (selectedValue != JOptionPane.YES_OPTION) {
                statBar.setStatusText("Peak picking cancelled.");
                return;
            }
        }

        // Remove previous peak lists
        for (RawDataFile r : rawDataFiles)
            if (proj.hasPeakList(r))
                proj.removePeakList(r);

        picker.runMethod(pickerParam, rawDataFiles, null);

    }

    /**
     * Update menu elements availability according to what is currently selected
     * in run selector and on desktop
     */
    public void updateMenuAvailability() {

        fileClose.setEnabled(false);
        filePrint.setEnabled(false);
        fileExportPeakList.setEnabled(false);
        editCopy.setEnabled(false);
        ssMeanFilter.setEnabled(false);
        ssSGFilter.setEnabled(false);
        ssChromatographicMedianFilter.setEnabled(false);
        ssCropFilter.setEnabled(false);
        ssZoomScanFilter.setEnabled(false);
        ssRecursiveThresholdPicker.setEnabled(false);
        ssLocalPicker.setEnabled(false);
        ssCentroidPicker.setEnabled(false);
        ssSimpleDeisotoping.setEnabled(false);
        ssCombinatorialDeisotoping.setEnabled(false);
        ssIncompleteIsotopePatternFilter.setEnabled(false);
        tsJoinAligner.setEnabled(false);
        tsFastAligner.setEnabled(false);
        normLinear.setEnabled(false);
        normStdComp.setEnabled(false);
        batDefine.setEnabled(false);
        windowTileWindows.setEnabled(false);
        tsEmptySlotFiller.setEnabled(false);
        tsAlignmentFilter.setEnabled(false);

        visOpenTIC.setEnabled(false);
        visOpenBasePeak.setEnabled(false);
        visOpenSpectra.setEnabled(false);
        visOpenTwoD.setEnabled(false);
        visOpenThreeD.setEnabled(false);

        visOpenSRView.setEnabled(false);
        visOpenSCVView.setEnabled(false);
        visOpenCDAView.setEnabled(false);
        visOpenSammonsView.setEnabled(false);

        /*
         * if ( (numOfRawDataWithVisibleVisualizer(false)>0) ||
         * (numOfResultsWithVisibleVisualizer(false)>0) ) {
         * windowTileWindows.setEnabled(true); }
         */
        RawDataFile[] actRawData = itemSelector.getSelectedRawData();
        if ((actRawData != null) && (actRawData.length > 0)) {

            fileClose.setEnabled(true);

            MZmineProject proj = MZmineProject.getCurrentProject();
            fileExportPeakList.setEnabled(false);
            for (RawDataFile file : actRawData)
                if (proj.hasPeakList(file)) {
                    fileExportPeakList.setEnabled(true);
                    // ssSimpleDeisotoping.setEnabled(true);
                    // ssCombinatorialDeisotoping.setEnabled(true);
                    // tsJoinAligner.setEnabled(true);
                    // tsFastAligner.setEnabled(true);
                    break;
                }

            ssMeanFilter.setEnabled(true);
            ssSGFilter.setEnabled(true);
            ssChromatographicMedianFilter.setEnabled(true);
            ssCropFilter.setEnabled(true);
            ssZoomScanFilter.setEnabled(true);

            ssRecursiveThresholdPicker.setEnabled(true);
            ssCentroidPicker.setEnabled(true);
            ssLocalPicker.setEnabled(true);

            visOpenTIC.setEnabled(true);
            visOpenBasePeak.setEnabled(true);
            visOpenSpectra.setEnabled(true);
            visOpenTwoD.setEnabled(true);
            visOpenThreeD.setEnabled(true);

            // batDefine.setEnabled(true);

            JInternalFrame activeWindow = mainWin.getDesktop().getSelectedFrame();

            if (activeWindow != null) {
                if ((activeWindow.getClass() == TICVisualizer.class)
                        || (activeWindow.getClass() == TwoDVisualizer.class)
                        || (activeWindow.getClass() == SpectrumVisualizer.class)) {
                    filePrint.setEnabled(true);
                    editCopy.setEnabled(true);
                }
            }
        }

        AlignmentResult actResult = itemSelector.getActiveResult();

        if (actResult != null) {
            fileClose.setEnabled(true);

            normLinear.setEnabled(true);
            normStdComp.setEnabled(true);
            tsAlignmentFilter.setEnabled(true);
            tsEmptySlotFiller.setEnabled(true);
            visOpenSRView.setEnabled(true);
            visOpenSCVView.setEnabled(true);
            visOpenCDAView.setEnabled(true);
            visOpenSammonsView.setEnabled(true);

            fileExportPeakList.setEnabled(true);

            JInternalFrame activeWindow = mainWin.getDesktop().getSelectedFrame();

            if (activeWindow != null) {
                if ((activeWindow.getClass() == AlignmentResultVisualizerLogratioPlotView.class)
                        || (activeWindow.getClass() == AlignmentResultVisualizerCoVarPlotView.class)
                        || (activeWindow.getClass() == AlignmentResultVisualizerCDAPlotView.class)
                        || (activeWindow.getClass() == AlignmentResultVisualizerSammonsPlotView.class)) {
                    filePrint.setEnabled(true);
                    editCopy.setEnabled(true);
                }
            }
        }

        // If at least one run or result is visible, then tile windows is active
        /*
         * if ( (numOfRawDataWithVisibleVisualizer(false)>0) ||
         * (numOfResultsWithVisibleVisualizer(false)>0) ) {
         * windowTileWindows.setEnabled(true); }
         */
    }

}
