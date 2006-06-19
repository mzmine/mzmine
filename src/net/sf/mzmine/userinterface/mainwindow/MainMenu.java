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

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.IOException;

import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.PeakListWriter;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.userinterface.dialogs.FileOpenDialog;


import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.filtering.mean.MeanFilter;
import net.sf.mzmine.methods.filtering.mean.MeanFilterParameters;
import net.sf.mzmine.methods.filtering.chromatographicmedian.ChromatographicMedianFilter;
import net.sf.mzmine.methods.filtering.chromatographicmedian.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.filtering.crop.CropFilter;
import net.sf.mzmine.methods.filtering.crop.CropFilterParameters;
import net.sf.mzmine.methods.filtering.savitzkygolay.SavitzkyGolayFilter;
import net.sf.mzmine.methods.filtering.savitzkygolay.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.filtering.zoomscan.ZoomScanFilter;
import net.sf.mzmine.methods.filtering.zoomscan.ZoomScanFilterParameters;
import net.sf.mzmine.methods.peakpicking.recursivethreshold.RecursiveThresholdPicker;
import net.sf.mzmine.methods.peakpicking.recursivethreshold.RecursiveThresholdPickerParameters;

import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCoVarPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerLogratioPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotView;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;
import net.sf.mzmine.visualizers.rawdata.tic.TICVisualizer;
import net.sf.mzmine.visualizers.rawdata.twod.TwoDVisualizer;
import sunutils.ExampleFileFilter;

/**
 *
 */
public class MainMenu extends JMenuBar implements ActionListener {

    private JMenu fileMenu;
    private JMenuItem fileOpen;
    private JMenuItem fileClose;
    private JMenuItem fileExportPeakList;
    private JMenuItem fileExportAlignmentResult;
    private JMenuItem fileSaveParameters;
    private JMenuItem fileLoadParameters;
    private JMenuItem filePrint;
    private JMenuItem fileExit;
    private JMenu editMenu;
    private JMenuItem editCopy;
    private JMenu filterMenu;
    private JMenuItem ssMeanFilter;
    private JMenuItem ssSGFilter;
    private JMenuItem ssChromatographicMedianFilter;
    private JMenuItem ssCropFilter;
    private JMenuItem ssZoomScanFilter;
    private JMenu peakMenu;
    private JMenuItem ssRecursiveThresholdPicker;
    private JMenuItem ssLocalPicker;
    private JMenuItem ssCentroidPicker;
    private JMenuItem ssSimpleDeisotoping;
    private JMenuItem ssCombinatorialDeisotoping;
    private JMenuItem ssIncompleteIsotopePatternFilter;
    private JMenu alignmentMenu;
    private JMenuItem tsJoinAligner;
    private JMenuItem tsFastAligner;
    private JMenuItem tsAlignmentFilter;
    private JMenuItem tsEmptySlotFiller;
    private JMenu normalizationMenu;
    private JMenuItem normLinear;
    private JMenuItem normStdComp;
    private JMenu batchMenu;
    private JMenuItem batDefine;
    private JMenu analysisMenu;
    private JMenuItem anOpenSRView;
    private JMenuItem anOpenSCVView;
    private JMenuItem anOpenCDAView;
    private JMenuItem anOpenSammonsView;
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
        fileOpen = new JMenuItem("Open...", KeyEvent.VK_O);
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                ActionEvent.CTRL_MASK));
        fileOpen.addActionListener(this);
        fileClose = new JMenuItem("Close", KeyEvent.VK_C);
        fileClose.addActionListener(this);
        fileClose.setEnabled(false);
        fileExportPeakList = new JMenuItem("Export peak list(s)...", KeyEvent.VK_E);
        fileExportPeakList.addActionListener(this);
        fileExportPeakList.setEnabled(false);
        fileExportAlignmentResult = new JMenuItem("Export alignment result(s)...", KeyEvent.VK_A);
        fileExportAlignmentResult.addActionListener(this);
        fileExportAlignmentResult.setEnabled(false);
        fileSaveParameters = new JMenuItem("Save parameters...", KeyEvent.VK_S);
        fileSaveParameters.addActionListener(this);
        fileSaveParameters.setEnabled(true);
        fileLoadParameters = new JMenuItem("Load parameters...", KeyEvent.VK_S);
        fileLoadParameters.addActionListener(this);
        fileLoadParameters.setEnabled(true);
        filePrint = new JMenuItem("Print figure...", KeyEvent.VK_P);
        filePrint.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                ActionEvent.CTRL_MASK));
        filePrint.addActionListener(this);
        filePrint.setEnabled(false);
        fileExit = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExit.addActionListener(this);
        fileMenu.add(fileOpen);
        fileMenu.add(fileClose);
        fileMenu.addSeparator();
        fileMenu.add(fileExportPeakList);
        fileMenu.add(fileExportAlignmentResult);
        fileMenu.addSeparator();
        fileMenu.add(fileLoadParameters);
        fileMenu.add(fileSaveParameters);
        fileMenu.addSeparator();
        fileMenu.add(filePrint);
        fileMenu.addSeparator();
        fileMenu.add(fileExit);
        this.add(fileMenu);

        editMenu = new JMenu();
        editMenu.setText("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.addActionListener(this);
        editCopy = new JMenuItem("Copy", KeyEvent.VK_C);
        editCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                ActionEvent.CTRL_MASK));
        editCopy.addActionListener(this);
        editCopy.setEnabled(false);
        editMenu.add(editCopy);
        this.add(editMenu);

        filterMenu = new JMenu();
        filterMenu.setText("Raw data filtering");
        filterMenu.setMnemonic(KeyEvent.VK_R);
        filterMenu.addActionListener(this);
        ssMeanFilter = new JMenuItem("Mean filter spectra", KeyEvent.VK_M);
        ssMeanFilter.addActionListener(this);
        ssMeanFilter.setEnabled(false);
        ssSGFilter = new JMenuItem("Savitzky-Golay filter spectra",
                KeyEvent.VK_S);
        ssSGFilter.addActionListener(this);
        ssSGFilter.setEnabled(false);
        ssChromatographicMedianFilter = new JMenuItem(
                "Chromatographic median filter", KeyEvent.VK_S);
        ssChromatographicMedianFilter.addActionListener(this);
        ssChromatographicMedianFilter.setEnabled(false);
        ssCropFilter = new JMenuItem("Cropping filter", KeyEvent.VK_C);
        ssCropFilter.addActionListener(this);
        ssCropFilter.setEnabled(false);
        ssZoomScanFilter = new JMenuItem("Zoom scan filter", KeyEvent.VK_Z);
        ssZoomScanFilter.addActionListener(this);
        ssZoomScanFilter.setEnabled(false);
        filterMenu.add(ssMeanFilter);
        filterMenu.add(ssSGFilter);
        filterMenu.add(ssChromatographicMedianFilter);
        filterMenu.add(ssCropFilter);
        filterMenu.add(ssZoomScanFilter);
        this.add(filterMenu);

        peakMenu = new JMenu();
        peakMenu.setText("Peak detection");
        peakMenu.setMnemonic(KeyEvent.VK_P);
        peakMenu.addActionListener(this);
        ssRecursiveThresholdPicker = new JMenuItem();
        ssRecursiveThresholdPicker.setText("Recursive threshold peak detector");
        ssRecursiveThresholdPicker.addActionListener(this);
        ssRecursiveThresholdPicker.setEnabled(false);
        ssLocalPicker = new JMenuItem();
        ssLocalPicker.setText("Local maxima peak detector");
        ssLocalPicker.addActionListener(this);
        ssLocalPicker.setEnabled(false);
        ssCentroidPicker = new JMenuItem();
        ssCentroidPicker.setText("Centroid peak detector");
        ssCentroidPicker.addActionListener(this);
        ssCentroidPicker.setEnabled(false);
        ssSimpleDeisotoping = new JMenuItem();
        ssSimpleDeisotoping.setText("Simple deisotoper");
        ssSimpleDeisotoping.addActionListener(this);
        ssSimpleDeisotoping.setEnabled(false);
        ssCombinatorialDeisotoping = new JMenuItem();
        ssCombinatorialDeisotoping.setText("Combinatorial deisotoping");
        ssCombinatorialDeisotoping.addActionListener(this);
        ssCombinatorialDeisotoping.setEnabled(false);
        ssIncompleteIsotopePatternFilter = new JMenuItem();
        ssIncompleteIsotopePatternFilter
                .setText("Filter incomplete isotope patterns");
        ssIncompleteIsotopePatternFilter.addActionListener(this);
        ssIncompleteIsotopePatternFilter.setEnabled(false);
        peakMenu.add(ssRecursiveThresholdPicker);
        peakMenu.add(ssLocalPicker);
        peakMenu.add(ssCentroidPicker);
        peakMenu.addSeparator();
        peakMenu.add(ssSimpleDeisotoping);
        peakMenu.add(ssIncompleteIsotopePatternFilter);
        this.add(peakMenu);

        alignmentMenu = new JMenu();
        alignmentMenu.setText("Alignment");
        alignmentMenu.setMnemonic(KeyEvent.VK_A);
        alignmentMenu.addActionListener(this);
        tsJoinAligner = new JMenuItem("Slow aligner", KeyEvent.VK_S);
        tsJoinAligner.addActionListener(this);
        tsFastAligner = new JMenuItem("Fast aligner", KeyEvent.VK_A);
        tsFastAligner.addActionListener(this);
        tsAlignmentFilter = new JMenuItem("Filter out rare peaks",
                KeyEvent.VK_R);
        tsAlignmentFilter.addActionListener(this);
        tsEmptySlotFiller = new JMenuItem("Fill-in empty gaps", KeyEvent.VK_F);
        tsEmptySlotFiller.addActionListener(this);
        alignmentMenu.add(tsJoinAligner);
        alignmentMenu.add(tsFastAligner);
        alignmentMenu.addSeparator();
        alignmentMenu.add(tsAlignmentFilter);
        alignmentMenu.add(tsEmptySlotFiller);
        this.add(alignmentMenu);

        normalizationMenu = new JMenu();
        normalizationMenu.setText("Normalization");
        normalizationMenu.setMnemonic(KeyEvent.VK_N);
        normalizationMenu.addActionListener(this);
        normLinear = new JMenuItem("Linear normalization", KeyEvent.VK_L);
        normLinear.addActionListener(this);
        normStdComp = new JMenuItem("Normalization using standards",
                KeyEvent.VK_N);
        normStdComp.addActionListener(this);
        normalizationMenu.add(normLinear);
        normalizationMenu.add(normStdComp);
        this.add(normalizationMenu);

        batchMenu = new JMenu();
        batchMenu.setText("Batch mode");
        batchMenu.setMnemonic(KeyEvent.VK_B);
        batchMenu.addActionListener(this);
        batDefine = new JMenuItem("Define batch operations", KeyEvent.VK_R);
        batDefine.addActionListener(this);
        batchMenu.add(batDefine);
        this.add(batchMenu);

        analysisMenu = new JMenu();
        analysisMenu.setText("Visualization");
        analysisMenu.setMnemonic(KeyEvent.VK_V);
        analysisMenu.addActionListener(this);
        anOpenSRView = new JMenuItem("Logratio plot", KeyEvent.VK_L);
        anOpenSRView.addActionListener(this);
        anOpenSCVView = new JMenuItem("Coefficient of variation plot",
                KeyEvent.VK_V);
        anOpenSCVView.addActionListener(this);
        anOpenCDAView = new JMenuItem("CDA plot of samples", KeyEvent.VK_C);
        anOpenCDAView.addActionListener(this);
        anOpenCDAView.setEnabled(false);
        anOpenSammonsView = new JMenuItem("Sammons plot of samples",
                KeyEvent.VK_S);
        anOpenSammonsView.addActionListener(this);
        anOpenSammonsView.setEnabled(false);
        analysisMenu.add(anOpenSRView);
        analysisMenu.add(anOpenSCVView);
        analysisMenu.add(anOpenCDAView);
        analysisMenu.add(anOpenSammonsView);
        this.add(analysisMenu);

        toolsMenu = new JMenu();
        toolsMenu.setText("Configure");
        toolsMenu.setMnemonic(KeyEvent.VK_C);
        toolsMenu.addActionListener(this);
        toolsOptions = new JMenuItem("Preferences...", KeyEvent.VK_P);
        toolsOptions.addActionListener(this);
        toolsMenu.add(toolsOptions);
        this.add(toolsMenu);

        windowMenu = new JMenu();
        windowMenu.setText("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        windowMenu.addActionListener(this);
        windowTileWindows = new JMenuItem("Tile Windows", KeyEvent.VK_T);
        windowTileWindows.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,
                ActionEvent.CTRL_MASK));
        windowTileWindows.addActionListener(this);// windowTileWindows.setEnabled(false);
        windowMenu.add(windowTileWindows);
        this.add(windowMenu);

        helpMenu = new JMenu();
        helpMenu.setText("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.addActionListener(this);
        hlpAbout = new JMenuItem("About MZmine...", KeyEvent.VK_A);
        hlpAbout.addActionListener(this);
        hlpAbout.setEnabled(true);
        helpMenu.add(hlpAbout);
        this.add(helpMenu);

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
			if(retval == JFileChooser.APPROVE_OPTION) {

				File selectedFile = fileOpenChooser.getSelectedFile();
				if (!(selectedFile.exists())) {
					mainWin.displayErrorMessage("Selected parameter file " + selectedFile + " does not exist!");
					return;
				}

				// Read parameters from file
				try {
					mainWin.getParameterStorage().readParameters(selectedFile);
				} catch (IOException ioexce) {
					mainWin.displayErrorMessage("Failed to load parameter settings from file " + selectedFile + ": " + ioexce.toString());
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
			if(retval == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileSaveChooser.getSelectedFile();

				// Add extension .mzmine-parameters to file name unless it is there already
				String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".") + 1).toLowerCase();
				if (!extension.equals("mzmine-parameters")) { selectedFile = new File(selectedFile.getPath() + ".mzmine-parameters"); }

				// Write parameters to file
				try {
					mainWin.getParameterStorage().writeParameters(selectedFile);
				} catch (IOException ioexce) {
					mainWin.displayErrorMessage("Failed to save parameter settings from file " + selectedFile + ": " + ioexce.toString());
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
					fileSaveChooser.setDialogTitle("Please give file name for peak list of " + file.toString());

					// Setup file extension filter
					ExampleFileFilter filter = new ExampleFileFilter();
					filter.addExtension("txt");
					filter.setDescription("Tab-delimitted text file");
					fileSaveChooser.setFileFilter(filter);

					// Show dialog and test return value from user
					int retval = fileSaveChooser.showSaveDialog(mainWin);
					if(retval == JFileChooser.APPROVE_OPTION) {
						File selectedFile = fileSaveChooser.getSelectedFile();

						// Add extension .txt to file name unless it is there already
						String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".") + 1).toLowerCase();
						if (!extension.equals("txt")) { selectedFile = new File(selectedFile.getPath() + ".txt"); }

						// Export peak list to file
						try {
							PeakListWriter.exportPeakListToFile(file, selectedFile);
						} catch (IOException ioexce) {
							mainWin.displayErrorMessage("Failed to export peak list for file " + file.toString() + ": " + ioexce.toString());
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
			MeanFilter mf = new MeanFilter();
			MeanFilterParameters mfParam = mainWin.getParameterStorage().getMeanFilterParameters();

			if (!(mf.askParameters((MethodParameters)mfParam))) {
				statBar.setStatusText("Filtering cancelled."); return;
			}

         	// It seems user didn't cancel
         	statBar.setStatusText("Filtering spectra.");
         	//paintNow();

         	RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();

         	mf.runMethod(mfParam, rawDataFiles, null);

		}

		// Filter -> Chromatographic median
		if (src == ssChromatographicMedianFilter) {

			 // Ask parameters from user
			ChromatographicMedianFilter cmf = new ChromatographicMedianFilter();
			ChromatographicMedianFilterParameters cmfParam = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();

			if (!(cmf.askParameters((MethodParameters)cmfParam))) {
				statBar.setStatusText("Filtering cancelled."); return;
			}

         	// It seems user didn't cancel
         	statBar.setStatusText("Filtering spectra.");
         	//paintNow();

         	RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();

         	cmf.runMethod(cmfParam, rawDataFiles, null);

		}

		// Filter -> Crop
		if (src == ssCropFilter) {

			 // Ask parameters from user
			CropFilter cf = new CropFilter();
			CropFilterParameters cfParam = mainWin.getParameterStorage().getCropFilterParameters();

			if (!(cf.askParameters((MethodParameters)cfParam))) {
				statBar.setStatusText("Filtering cancelled."); return;
			}

         	// It seems user didn't cancel
         	statBar.setStatusText("Filtering spectra.");
         	//paintNow();

         	RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();

         	cf.runMethod(cfParam, rawDataFiles, null);

		}

		// Filter -> Crop
		if (src == ssSGFilter) {

			 // Ask parameters from user
			SavitzkyGolayFilter sgf = new SavitzkyGolayFilter();
			SavitzkyGolayFilterParameters sgfParam = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();

			if (!(sgf.askParameters((MethodParameters)sgfParam))) {
				statBar.setStatusText("Filtering cancelled."); return;
			}

         	// It seems user didn't cancel
         	statBar.setStatusText("Filtering spectra.");
         	//paintNow();

         	RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();

         	sgf.runMethod(sgfParam, rawDataFiles, null);

		}

		// Filter -> Zoom scan
		if (src == ssZoomScanFilter) {

			 // Ask parameters from user
			ZoomScanFilter zsf = new ZoomScanFilter();
			ZoomScanFilterParameters zsfParam = mainWin.getParameterStorage().getZoomScanFilterParameters();

			if (!(zsf.askParameters((MethodParameters)zsfParam))) {
				statBar.setStatusText("Filtering cancelled."); return;
			}

         	// It seems user didn't cancel
         	statBar.setStatusText("Filtering spectra.");
         	//paintNow();

         	RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();

         	zsf.runMethod(zsfParam, rawDataFiles, null);

		}


		if (src == ssRecursiveThresholdPicker) {

			RecursiveThresholdPicker rp = new RecursiveThresholdPicker();
			RecursiveThresholdPickerParameters rpParam = mainWin.getParameterStorage().getRecursiveThresholdPickerParameters();

			if (!(rp.askParameters((MethodParameters)rpParam))) {
				statBar.setStatusText("Peak picking cancelled."); return;
			}

         	// It seems user didn't cancel
         	statBar.setStatusText("Finding peaks.");

         	// Check if selected data files have previous peak detection results
         	RawDataFile[] rawDataFiles = mainWin.getItemSelector().getSelectedRawData();
         	boolean previousExists = false;
         	MZmineProject proj = MZmineProject.getCurrentProject();
         	for (RawDataFile r : rawDataFiles)
				if (proj.hasPeakList(r)) { previousExists = true; break; }

			// Show warning if going to remove previous lists
			if (previousExists) {
				// Ask if is it ok to replace existing peak picking results
				int selectedValue = JOptionPane.showInternalConfirmDialog(mainWin.getDesktop(),
						"Previous peak picking results will be overwritten. Do you want to continue?", "Overwrite?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (selectedValue != JOptionPane.YES_OPTION) {
					statBar.setStatusText("Peak picking cancelled.");
					return;
				}
			}

			// Remove previous peak lists
         	for (RawDataFile r : rawDataFiles)
				if (proj.hasPeakList(r))
					proj.removePeakList(r);

			rp.runMethod(rpParam, rawDataFiles, null);

		}

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

        anOpenSRView.setEnabled(false);
        anOpenSCVView.setEnabled(false);
        anOpenCDAView.setEnabled(false);
        anOpenSammonsView.setEnabled(false);


        /*
         * if ( (numOfRawDataWithVisibleVisualizer(false)>0) ||
         * (numOfResultsWithVisibleVisualizer(false)>0) ) {
         * windowTileWindows.setEnabled(true); }
         */
        RawDataFile[] actRawData = itemSelector.getSelectedRawData();
        if ( (actRawData != null) && (actRawData.length>0)) {

            fileClose.setEnabled(true);

			MZmineProject proj = MZmineProject.getCurrentProject();
			fileExportPeakList.setEnabled(false);
			for (RawDataFile file : actRawData)
				if (proj.hasPeakList(file)) {
					fileExportPeakList.setEnabled(true);
					//ssSimpleDeisotoping.setEnabled(true);
					//ssCombinatorialDeisotoping.setEnabled(true);
					//tsJoinAligner.setEnabled(true);
					//tsFastAligner.setEnabled(true);
					break;
				}


            ssMeanFilter.setEnabled(true);
            ssSGFilter.setEnabled(true);
            ssChromatographicMedianFilter.setEnabled(true);
            ssCropFilter.setEnabled(true);
            ssZoomScanFilter.setEnabled(true);

            ssRecursiveThresholdPicker.setEnabled(true);
			/*
            ssLocalPicker.setEnabled(true);
            ssCentroidPicker.setEnabled(true);
            */

            // batDefine.setEnabled(true);

            /*
             * if (actRawData.hasPeakData()) {
             * ssSimpleDeisotoping.setEnabled(true); //
             * ssCombinatorialDeisotoping.setEnabled(true); DEBUG: Feature //
             * not yet ready ssIncompleteIsotopePatternFilter.setEnabled(true);
             * fileExportPeakList.setEnabled(true);
             * tsJoinAligner.setEnabled(true); tsFastAligner.setEnabled(true); }
             */
            JInternalFrame activeWindow = mainWin.getDesktop()
                    .getSelectedFrame();

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
            anOpenSRView.setEnabled(true);
            anOpenSCVView.setEnabled(true);
            anOpenCDAView.setEnabled(true);
            anOpenSammonsView.setEnabled(true);

            fileExportPeakList.setEnabled(true);

            JInternalFrame activeWindow = mainWin.getDesktop()
                    .getSelectedFrame();

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
