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

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.util.AlignmentResultExporter;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.AboutDialog;
import net.sf.mzmine.userinterface.dialogs.FileOpenDialog;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelectionDialog;
import net.sf.mzmine.util.GUIUtils;

/**
 * 
 */
public class MainMenu extends JMenuBar implements ActionListener,
        ListSelectionListener {

    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu filterMenu;
    private JMenu peakMenu;
    private JMenu alignmentMenu;
    private JMenu normalizationMenu;
    private JMenu batchMenu;
    private JMenu visualizationMenu;
    private JMenu toolsMenu;
    private JMenu windowMenu;
    private JMenu helpMenu;

    private JMenuItem editCopy;

    private JMenuItem fileOpen, fileClose, fileExportPeakList,
            fileExportAlignmentResult, fileSaveParameters, fileLoadParameters,
            filePrint, fileExit;
    /*
     * private JMenu filterMenu; private JMenuItem ssMeanFilter, ssSGFilter,
     * ssChromatographicMedianFilter, ssCropFilter, ssZoomScanFilter; private
     * JMenu peakMenu; private JMenuItem ssRecursiveThresholdPicker,
     * ssLocalPicker, ssCentroidPicker, ssSimpleIsotopicPeaksGrouper,
     * ssCombinatorialDeisotoping, ssIncompleteIsotopePatternFilter; private
     * JMenu alignmentMenu; private JMenuItem tsJoinAligner, tsFastAligner,
     * tsAlignmentFilter, tsEmptySlotFiller; private JMenu normalizationMenu;
     * private JMenuItem normLinear, normStdComp; private JMenu batchMenu;
     * private JMenu visualizationMenu; private JMenuItem visOpenTIC,
     * visOpenSpectra, visOpenTwoD, visOpenThreeD; private JMenuItem
     * visOpenSRView, visOpenSCVView, visOpenCDAView, visOpenSammonsView;
     * private JMenu toolsMenu;
     */

    private JMenuItem batDefine;
    private JMenuItem toolsOptions;

    private JMenuItem windowTileWindows, windowCascadeWindows;
    private JMenuItem hlpAbout;

    private IOController ioController;
    private Desktop desktop;

    MainMenu(IOController ioController, Desktop desktop) {

        this.ioController = ioController;
        this.desktop = desktop;

        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(fileMenu);

        fileOpen = GUIUtils.addMenuItem(fileMenu, "Open...", this,
                KeyEvent.VK_O, true);
        fileClose = GUIUtils.addMenuItem(fileMenu, "Close", this, KeyEvent.VK_C);
        fileMenu.addSeparator();
        fileExportPeakList = GUIUtils.addMenuItem(fileMenu,
                "Export peak  list(s)...", this, KeyEvent.VK_E);
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

        peakMenu = new JMenu("Peak detection");
        peakMenu.setMnemonic(KeyEvent.VK_P);
        this.add(peakMenu);

        /*
         * ssRecursiveThresholdPicker = GUIUtils.addMenuItem(peakMenu,
         * "Recursive threshold peak detector", this); ssLocalPicker =
         * GUIUtils.addMenuItem(peakMenu, "Local maxima peak detector", this);
         * ssCentroidPicker = GUIUtils.addMenuItem(peakMenu, "Centroid peak
         * detector", this); peakMenu.addSeparator();
         * ssSimpleIsotopicPeaksGrouper = GUIUtils.addMenuItem(peakMenu, "Simple
         * isotopic peaks grouper", this); ssCombinatorialDeisotoping =
         * GUIUtils.addMenuItem(peakMenu, "Combinatorial deisotoping", this);
         * ssIncompleteIsotopePatternFilter = GUIUtils.addMenuItem(peakMenu,
         * "Filter incomplete isotope patterns", this);
         */

        alignmentMenu = new JMenu("Alignment");
        alignmentMenu.setMnemonic(KeyEvent.VK_A);
        this.add(alignmentMenu);

        /*
         * tsJoinAligner = GUIUtils.addMenuItem(alignmentMenu, "Slow aligner",
         * this, KeyEvent.VK_S); tsFastAligner =
         * GUIUtils.addMenuItem(alignmentMenu, "Fast aligner", this,
         * KeyEvent.VK_A); alignmentMenu.addSeparator(); tsAlignmentFilter =
         * GUIUtils.addMenuItem(alignmentMenu, "Filter out rare peaks", this,
         * KeyEvent.VK_R); tsEmptySlotFiller =
         * GUIUtils.addMenuItem(alignmentMenu, "Fill-in empty gaps", this,
         * KeyEvent.VK_F);
         */

        normalizationMenu = new JMenu("Normalization");
        normalizationMenu.setMnemonic(KeyEvent.VK_N);
        this.add(normalizationMenu);

        /*
         * normLinear = GUIUtils.addMenuItem(normalizationMenu, "Linear
         * normalization", this, KeyEvent.VK_L); normStdComp =
         * GUIUtils.addMenuItem(normalizationMenu, "Normalization using
         * standards", this, KeyEvent.VK_N);
         */

        batchMenu = new JMenu("Batch mode");
        batchMenu.setMnemonic(KeyEvent.VK_B);
        this.add(batchMenu);

        

        visualizationMenu = new JMenu("Visualization");
        visualizationMenu.setMnemonic(KeyEvent.VK_V);
        this.add(visualizationMenu);

        /*
         * visOpenTIC = GUIUtils.addMenuItem(visualizationMenu, "TIC plot",
         * this, KeyEvent.VK_T); visOpenSpectra =
         * GUIUtils.addMenuItem(visualizationMenu, "Spectra plot", this,
         * KeyEvent.VK_S); visOpenTwoD = GUIUtils.addMenuItem(visualizationMenu,
         * "2D plot", this, KeyEvent.VK_2); visOpenThreeD =
         * GUIUtils.addMenuItem(visualizationMenu, "3D plot", this,
         * KeyEvent.VK_3); visualizationMenu.addSeparator(); visOpenSRView =
         * GUIUtils.addMenuItem(visualizationMenu, "Logratio plot", this,
         * KeyEvent.VK_L); visOpenSCVView =
         * GUIUtils.addMenuItem(visualizationMenu, "Coefficient of variation
         * plot", this, KeyEvent.VK_V); visOpenCDAView =
         * GUIUtils.addMenuItem(visualizationMenu, "CDA plot of samples", this,
         * KeyEvent.VK_C); visOpenSammonsView =
         * GUIUtils.addMenuItem(visualizationMenu, "Sammons plot of samples",
         * this, KeyEvent.VK_S);
         */

        toolsMenu = new JMenu("Configure");
        toolsMenu.setMnemonic(KeyEvent.VK_C);
        this.add(toolsMenu);

        toolsOptions = GUIUtils.addMenuItem(toolsMenu, "Preferences...", this,
                KeyEvent.VK_P);

        windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        this.add(windowMenu);

        windowTileWindows = GUIUtils.addMenuItem(windowMenu, "Tile windows",
                this, KeyEvent.VK_T, true);
        windowCascadeWindows = GUIUtils.addMenuItem(windowMenu,
                "Cascade windows", this, KeyEvent.VK_A, true);

        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        this.add(helpMenu);

        hlpAbout = GUIUtils.addMenuItem(helpMenu, "About MZmine...", this,
                KeyEvent.VK_A);

        desktop.addSelectionListener(this);

    }

    public void addMenuItem(MZmineMenu parentMenu, JMenuItem newItem) {
        switch (parentMenu) {
        case FILTERING:
            filterMenu.add(newItem);
            break;
        case PEAKPICKING:
            peakMenu.add(newItem);
            break;
        case ALIGNMENT:
            alignmentMenu.add(newItem);
            break;
        case NORMALIZATION:
            normalizationMenu.add(newItem);
            break;
        case BATCH:
            batchMenu.add(newItem);
            break;
        case VISUALIZATION:
            visualizationMenu.add(newItem);
            break;

        }
    }

    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            ActionListener listener, String actionCommand, int mnemonic,
            boolean setAccelerator, boolean enabled) {

        JMenuItem newItem = new JMenuItem(text);
        if (listener != null)
            newItem.addActionListener(listener);
        if (actionCommand != null)
            newItem.setActionCommand(actionCommand);
        if (mnemonic > 0)
            newItem.setMnemonic(mnemonic);
        if (setAccelerator)
            newItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
                    ActionEvent.CTRL_MASK));
        newItem.setEnabled(enabled);
        addMenuItem(parentMenu, newItem);
        return newItem;

    }

    public void addMenuSeparator(MZmineMenu parentMenu) {
        switch (parentMenu) {
        case FILTERING:
            filterMenu.addSeparator();
            break;
        case PEAKPICKING:
            peakMenu.addSeparator();
            break;
        case ALIGNMENT:
            alignmentMenu.addSeparator();
            break;
        case NORMALIZATION:
            normalizationMenu.addSeparator();
            break;
        case BATCH:
            batchMenu.addSeparator();
            break;
        case VISUALIZATION:
            visualizationMenu.addSeparator();
            break;
            

        }
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();

        if (src == fileExit) {
            desktop.exitMZmine();
        }

        // File -> Open
        if (src == fileOpen) {
            FileOpenDialog fileOpenDialog = new FileOpenDialog(ioController,
                    desktop);
            fileOpenDialog.setVisible(true);

        }

        // File->Close
        if (src == fileClose) {

            // Grab selected raw data files
            OpenedRawDataFile[] selectedFiles = desktop.getSelectedDataFiles();
            for (OpenedRawDataFile file : selectedFiles)
                MZmineProject.getCurrentProject().removeFile(file);

        }
        
        if (src == fileExportAlignmentResult) {
        	
        }

        // Window->Tile
        if (src == windowTileWindows) {
            MainWindow mainWindow = (MainWindow) desktop;
            mainWindow.tileInternalFrames();
        }

        // Window->Cascade
        if (src == windowCascadeWindows) {
            MainWindow mainWindow = (MainWindow) desktop;
            mainWindow.cascadeInternalFrames();
        }

        // Help->About
        if (src == hlpAbout) {
            AboutDialog dialog = new AboutDialog(desktop);
            dialog.setVisible(true);
        }

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        fileClose.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * ActionListener interface implementation public void
     * actionPerformed(ActionEvent e) {
     * 
     * Object src = e.getSource(); // File -> Open if (src == fileOpen) { //
     * TODO FileOpenDialog fileOpenDialog = new FileOpenDialog(ioController,
     * desktop); // fileOpenDialog.setVisible(true); } // File->Close if (src ==
     * fileClose) { // Grab selected raw data files RawDataFile[] selectedFiles =
     * itemSelector.getSelectedRawData(); for (RawDataFile file : selectedFiles)
     * MZmineProject.getCurrentProject().removeFile(file); // int[]
     * alignmentResultIDs = itemSelector // .getSelectedAlignmentResultIDs(); //
     * desktop.closeAlignmentResults(alignmentResultIDs); }
     * 
     * if (src == fileLoadParameters) {
     * 
     * statBar.setStatusText("Please select a parameter file"); // Build open
     * dialog JFileChooser fileOpenChooser = new JFileChooser();
     * fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
     * fileOpenChooser.setMultiSelectionEnabled(false);
     * fileOpenChooser.setDialogTitle("Please select parameter file"); // Setup
     * file extension filter ExampleFileFilter filter = new ExampleFileFilter();
     * filter.addExtension("mzmine-parameters"); filter.setDescription("MZmine
     * parameters file"); fileOpenChooser.setFileFilter(filter); // Show dialog
     * and test return value from user int retval =
     * fileOpenChooser.showOpenDialog(desktop); if (retval ==
     * JFileChooser.APPROVE_OPTION) {
     * 
     * File selectedFile = fileOpenChooser.getSelectedFile(); if
     * (!(selectedFile.exists())) { desktop.displayErrorMessage("Selected
     * parameter file " + selectedFile + " does not exist!"); return; } // Read
     * parameters from file try {
     * desktop.getParameterStorage().readParameters(selectedFile); } catch
     * (IOException ioexce) { desktop.displayErrorMessage("Failed to load
     * parameter settings from file " + selectedFile + ": " +
     * ioexce.toString()); } } } // File -> Save parameters if (src ==
     * fileSaveParameters) { // Build save dialog JFileChooser fileSaveChooser =
     * new JFileChooser();
     * fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
     * fileSaveChooser.setMultiSelectionEnabled(false);
     * fileSaveChooser.setDialogTitle("Please give file name for parameters
     * file."); // Setup file extension filter ExampleFileFilter filter = new
     * ExampleFileFilter(); filter.addExtension("mzmine-parameters");
     * filter.setDescription("MZmine parameters file");
     * fileSaveChooser.setFileFilter(filter); // Show dialog and test return
     * value from user int retval = fileSaveChooser.showSaveDialog(desktop); if
     * (retval == JFileChooser.APPROVE_OPTION) { File selectedFile =
     * fileSaveChooser.getSelectedFile(); // Add extension .mzmine-parameters to
     * file name unless it is // there already String extension =
     * selectedFile.getName().substring( selectedFile.getName().lastIndexOf(".") +
     * 1).toLowerCase(); if (!extension.equals("mzmine-parameters")) {
     * selectedFile = new File(selectedFile.getPath() + ".mzmine-parameters"); } //
     * Write parameters to file try {
     * desktop.getParameterStorage().writeParameters(selectedFile); } catch
     * (IOException ioexce) { desktop.displayErrorMessage("Failed to save
     * parameter settings from file " + selectedFile + ": " +
     * ioexce.toString()); } } }
     * 
     * if (src == fileExportPeakList) {
     * 
     * MZmineProject proj = MZmineProject.getCurrentProject(); // Grab selected
     * raw data files RawDataFile[] selectedFiles =
     * itemSelector.getSelectedRawData(); for (RawDataFile file : selectedFiles) {
     * if (proj.hasPeakList(file)) { // Build save dialog JFileChooser
     * fileSaveChooser = new JFileChooser();
     * fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
     * fileSaveChooser.setMultiSelectionEnabled(false);
     * fileSaveChooser.setDialogTitle("Please give file name for peak list of " +
     * file.toString()); // Setup file extension filter ExampleFileFilter filter =
     * new ExampleFileFilter(); filter.addExtension("txt");
     * filter.setDescription("Tab-delimitted text file");
     * fileSaveChooser.setFileFilter(filter); // Show dialog and test return
     * value from user int retval = fileSaveChooser.showSaveDialog(desktop); if
     * (retval == JFileChooser.APPROVE_OPTION) { File selectedFile =
     * fileSaveChooser.getSelectedFile(); // Add extension .txt to file name
     * unless it is there // already String extension =
     * selectedFile.getName().substring( selectedFile.getName().lastIndexOf(".") +
     * 1).toLowerCase(); if (!extension.equals("txt")) { selectedFile = new
     * File(selectedFile.getPath() + ".txt"); } // Export peak list to file try {
     * PeakListWriter.exportPeakListToFile(file, selectedFile); } catch
     * (IOException ioexce) { desktop.displayErrorMessage("Failed to export peak
     * list for file " + file.toString() + ": " + ioexce.toString()); } } } } } //
     * File -> Exit if (src == fileExit) { statBar.setStatusText("Exiting.");
     * desktop.exitMZmine(); } // Filter -> Mean if (src == ssMeanFilter) { //
     * Ask parameters from user MeanFilter filter = new MeanFilter();
     * MeanFilterParameters filterParam =
     * desktop.getParameterStorage().getMeanFilterParameters();
     * 
     * startRawDataMethod(filter, filterParam); } // Filter -> Chromatographic
     * median if (src == ssChromatographicMedianFilter) { // Ask parameters from
     * user ChromatographicMedianFilter filter = new
     * ChromatographicMedianFilter(); ChromatographicMedianFilterParameters
     * filterParam =
     * desktop.getParameterStorage().getChromatographicMedianFilterParameters();
     * 
     * startRawDataMethod(filter, filterParam); } // Filter -> Crop if (src ==
     * ssCropFilter) { // Ask parameters from user CropFilter filter = new
     * CropFilter(); CropFilterParameters filterParam =
     * desktop.getParameterStorage().getCropFilterParameters();
     * 
     * startRawDataMethod(filter, filterParam); } // Filter -> Crop if (src ==
     * ssSGFilter) { // Ask parameters from user SavitzkyGolayFilter filter =
     * new SavitzkyGolayFilter(); SavitzkyGolayFilterParameters filterParam =
     * desktop.getParameterStorage().getSavitzkyGolayFilterParameters();
     * 
     * startRawDataMethod(filter, filterParam); } // Filter -> Zoom scan if (src ==
     * ssZoomScanFilter) {
     * 
     * ZoomScanFilter filter = new ZoomScanFilter(); ZoomScanFilterParameters
     * filterParam =
     * desktop.getParameterStorage().getZoomScanFilterParameters();
     * 
     * startRawDataMethod(filter, filterParam); }
     * 
     * if (src == ssRecursiveThresholdPicker) {
     * 
     * RecursiveThresholdPicker picker = new RecursiveThresholdPicker();
     * RecursiveThresholdPickerParameters pickerParam =
     * desktop.getParameterStorage().getRecursiveThresholdPickerParameters();
     * 
     * startPeakListMethod(picker, pickerParam); }
     * 
     * if (src == ssCentroidPicker) {
     * 
     * CentroidPicker picker = new CentroidPicker(); CentroidPickerParameters
     * pickerParam =
     * desktop.getParameterStorage().getCentroidPickerParameters();
     * startPeakListMethod(picker, pickerParam); }
     * 
     * if (src == ssLocalPicker) {
     * 
     * LocalPicker picker = new LocalPicker(); LocalPickerParameters pickerParam =
     * desktop.getParameterStorage().getLocalPickerParameters();
     * startPeakListMethod(picker, pickerParam); }
     * 
     * 
     * if (src == ssSimpleIsotopicPeaksGrouper) {
     * 
     * SimpleIsotopicPeaksGrouper grouper = new SimpleIsotopicPeaksGrouper();
     * SimpleIsotopicPeaksGrouperParameters grouperParam =
     * desktop.getParameterStorage().getSimpleIsotopicPeaksGrouperParameters();
     * startPeakListMethod(grouper, grouperParam); } // Visualization -> TIC
     * plot if (src == visOpenTIC) { RawDataFile firstSelectedFile =
     * itemSelector.getFirstSelectedRawData(); JDialog setupDialog = new
     * TICSetupDialog(firstSelectedFile); setupDialog.setVisible(true); } //
     * Visualization -> Spectrum plot if (src == visOpenSpectra) { RawDataFile
     * firstSelectedFile = itemSelector.getFirstSelectedRawData(); // JDialog
     * setupDialog = new SpectraSetupDialog(firstSelectedFile); //
     * setupDialog.setVisible(true); } // Visualization -> 2D plot if (src ==
     * visOpenTwoD) { RawDataFile firstSelectedFile =
     * itemSelector.getFirstSelectedRawData(); JDialog setupDialog = new
     * TwoDSetupDialog(firstSelectedFile); setupDialog.setVisible(true); } //
     * Visualization -> 3D plot if (src == visOpenThreeD) { RawDataFile
     * firstSelectedFile = itemSelector.getFirstSelectedRawData(); JDialog
     * setupDialog = new ThreeDSetupDialog(firstSelectedFile);
     * setupDialog.setVisible(true); } }
     * 
     * private void startRawDataMethod(Method method, MethodParameters
     * methodParam) { // Ask parameters from user if
     * (!(method.askParameters((MethodParameters) methodParam))) {
     * statBar.setStatusText("Processing cancelled."); return; } // It seems
     * user didn't cancel statBar.setStatusText("Processing...");
     * 
     * RawDataFile[] rawDataFiles =
     * desktop.getItemSelector().getSelectedRawData();
     * 
     * method.runMethod(methodParam, rawDataFiles, null); }
     * 
     * private void startPeakListMethod(Method method, MethodParameters
     * methodParam) { // Check if selected data files have previous peak
     * detection results RawDataFile[] rawDataFiles =
     * desktop.getItemSelector().getSelectedRawData(); boolean previousExists =
     * false; MZmineProject proj = MZmineProject.getCurrentProject(); for
     * (RawDataFile r : rawDataFiles) if (proj.hasPeakList(r)) { previousExists =
     * true; break; } // Show warning if going to remove previous lists if
     * (previousExists) { // Ask if is it ok to replace existing peak picking
     * results int selectedValue = JOptionPane.showInternalConfirmDialog(
     * desktop.getDesktop(), "Previous peak list(s) will be replaced with new
     * ones and alignment results will be closed. Do you want to continue?",
     * "Continue?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE); if
     * (selectedValue != JOptionPane.YES_OPTION) {
     * statBar.setStatusText("Processing cancelled."); return; } }
     * 
     * 
     * if (!(method.askParameters((MethodParameters) methodParam))) {
     * statBar.setStatusText("Processing cancelled."); return; } // It seems
     * user didn't cancel statBar.setStatusText("Processing...");
     * 
     * method.runMethod(methodParam, rawDataFiles, null); }
     * 
     * 
     * 
     * /** Update menu elements availability according to what is currently
     * selected in run selector and on desktop
     * 
     * public void updateMenuAvailability() {
     * 
     * fileClose.setEnabled(false); filePrint.setEnabled(false);
     * fileExportPeakList.setEnabled(false); editCopy.setEnabled(false);
     * ssMeanFilter.setEnabled(false); ssSGFilter.setEnabled(false);
     * ssChromatographicMedianFilter.setEnabled(false);
     * ssCropFilter.setEnabled(false); ssZoomScanFilter.setEnabled(false);
     * ssRecursiveThresholdPicker.setEnabled(false);
     * ssLocalPicker.setEnabled(false); ssCentroidPicker.setEnabled(false);
     * ssSimpleIsotopicPeaksGrouper.setEnabled(false);
     * ssCombinatorialDeisotoping.setEnabled(false);
     * ssIncompleteIsotopePatternFilter.setEnabled(false);
     * tsJoinAligner.setEnabled(false); tsFastAligner.setEnabled(false);
     * normLinear.setEnabled(false); normStdComp.setEnabled(false);
     * batDefine.setEnabled(false); windowTileWindows.setEnabled(false);
     * tsEmptySlotFiller.setEnabled(false); tsAlignmentFilter.setEnabled(false);
     * 
     * visOpenTIC.setEnabled(false); visOpenSpectra.setEnabled(false);
     * visOpenTwoD.setEnabled(false); visOpenThreeD.setEnabled(false);
     * 
     * visOpenSRView.setEnabled(false); visOpenSCVView.setEnabled(false);
     * visOpenCDAView.setEnabled(false); visOpenSammonsView.setEnabled(false);
     * 
     * 
     * RawDataFile[] actRawData = itemSelector.getSelectedRawData(); if
     * ((actRawData != null) && (actRawData.length > 0)) {
     * 
     * fileClose.setEnabled(true);
     * 
     * MZmineProject proj = MZmineProject.getCurrentProject();
     * fileExportPeakList.setEnabled(false); for (RawDataFile file : actRawData)
     * if (proj.hasPeakList(file)) { fileExportPeakList.setEnabled(true);
     * ssSimpleIsotopicPeaksGrouper.setEnabled(true); //
     * ssCombinatorialDeisotoping.setEnabled(true); //
     * tsJoinAligner.setEnabled(true); // tsFastAligner.setEnabled(true); break; }
     * 
     * ssMeanFilter.setEnabled(true); ssSGFilter.setEnabled(true);
     * ssChromatographicMedianFilter.setEnabled(true);
     * ssCropFilter.setEnabled(true); ssZoomScanFilter.setEnabled(true);
     * 
     * ssRecursiveThresholdPicker.setEnabled(true);
     * ssCentroidPicker.setEnabled(true); ssLocalPicker.setEnabled(true);
     * 
     * visOpenTIC.setEnabled(true); visOpenSpectra.setEnabled(true);
     * visOpenTwoD.setEnabled(true); visOpenThreeD.setEnabled(true); //
     * batDefine.setEnabled(true);
     * 
     * JInternalFrame activeWindow = desktop.getDesktop().getSelectedFrame();
     * 
     * if (activeWindow != null) { if (activeWindow instanceof
     * RawDataVisualizer) { filePrint.setEnabled(true);
     * editCopy.setEnabled(true); } } }
     * 
     * AlignmentResult actResult = itemSelector.getActiveResult();
     * 
     * if (actResult != null) { fileClose.setEnabled(true);
     * 
     * normLinear.setEnabled(true); normStdComp.setEnabled(true);
     * tsAlignmentFilter.setEnabled(true); tsEmptySlotFiller.setEnabled(true);
     * visOpenSRView.setEnabled(true); visOpenSCVView.setEnabled(true);
     * visOpenCDAView.setEnabled(true); visOpenSammonsView.setEnabled(true);
     * 
     * fileExportPeakList.setEnabled(true);
     * 
     * JInternalFrame activeWindow = desktop.getDesktop().getSelectedFrame();
     * 
     * if (activeWindow != null) { if ((activeWindow.getClass() ==
     * AlignmentResultVisualizerLogratioPlotView.class) ||
     * (activeWindow.getClass() == AlignmentResultVisualizerCoVarPlotView.class) ||
     * (activeWindow.getClass() == AlignmentResultVisualizerCDAPlotView.class) ||
     * (activeWindow.getClass() ==
     * AlignmentResultVisualizerSammonsPlotView.class)) {
     * filePrint.setEnabled(true); editCopy.setEnabled(true); } } } // If at
     * least one run or result is visible, then tile windows is active }
     * 
     * 
     */

}
