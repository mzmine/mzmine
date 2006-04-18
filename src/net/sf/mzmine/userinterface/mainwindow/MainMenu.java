/**
 * 
 */
package net.sf.mzmine.userinterface.mainwindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.alignment.AlignmentResultExporter;
import net.sf.mzmine.methods.alignment.AlignmentResultExporterParameterSetupDialog;
import net.sf.mzmine.methods.alignment.AlignmentResultExporterParameters;
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGaps;
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.FastAligner;
import net.sf.mzmine.methods.alignment.FastAlignerParameters;
import net.sf.mzmine.methods.alignment.JoinAligner;
import net.sf.mzmine.methods.alignment.JoinAlignerParameters;
import net.sf.mzmine.methods.alignment.LinearNormalizer;
import net.sf.mzmine.methods.alignment.LinearNormalizerParameters;
import net.sf.mzmine.methods.alignment.SimpleGapFiller;
import net.sf.mzmine.methods.alignment.SimpleGapFillerParameters;
import net.sf.mzmine.methods.alignment.StandardCompoundNormalizer;
import net.sf.mzmine.methods.alignment.StandardCompoundNormalizerParameters;
import net.sf.mzmine.methods.filtering.ChromatographicMedianFilter;
import net.sf.mzmine.methods.filtering.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.filtering.CropFilter;
import net.sf.mzmine.methods.filtering.CropFilterParameters;
import net.sf.mzmine.methods.filtering.MeanFilter;
import net.sf.mzmine.methods.filtering.MeanFilterParameters;
import net.sf.mzmine.methods.filtering.SavitzkyGolayFilter;
import net.sf.mzmine.methods.filtering.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.filtering.ZoomScanFilter;
import net.sf.mzmine.methods.filtering.ZoomScanFilterParameters;
import net.sf.mzmine.methods.peakpicking.CentroidPicker;
import net.sf.mzmine.methods.peakpicking.CentroidPickerParameters;
import net.sf.mzmine.methods.peakpicking.CombinatorialDeisotoper;
import net.sf.mzmine.methods.peakpicking.CombinatorialDeisotoperParameters;
import net.sf.mzmine.methods.peakpicking.IncompleteIsotopePatternFilter;
import net.sf.mzmine.methods.peakpicking.IncompleteIsotopePatternFilterParameters;
import net.sf.mzmine.methods.peakpicking.LocalPicker;
import net.sf.mzmine.methods.peakpicking.LocalPickerParameters;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListExporter;
import net.sf.mzmine.methods.peakpicking.RecursiveThresholdPicker;
import net.sf.mzmine.methods.peakpicking.RecursiveThresholdPickerParameters;
import net.sf.mzmine.methods.peakpicking.SimpleDeisotoper;
import net.sf.mzmine.methods.peakpicking.SimpleDeisotoperParameters;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.userinterface.dialogs.AboutDialog;
import net.sf.mzmine.userinterface.dialogs.BatchModeDialog;
import net.sf.mzmine.userinterface.dialogs.OptionsWindow;
import net.sf.mzmine.util.GeneralParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizer;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCoVarPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerLogratioPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerSpectrumView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerTICView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerTwoDView;
import sunutils.ExampleFileFilter;

/**
 * 
 */
class MainMenu extends JMenuBar implements ActionListener {

    private JMenu fileMenu;
    private JMenuItem fileOpen;
    private JMenuItem fileClose;
    private JMenuItem fileExportPeakList;
    private JMenuItem fileImportAlignmentResult;
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
        fileExportPeakList = new JMenuItem("Export...", KeyEvent.VK_E);
        fileExportPeakList.addActionListener(this);
        fileExportPeakList.setEnabled(false);
        fileImportAlignmentResult = new JMenuItem("Import alignment result...",
                KeyEvent.VK_I);
        fileImportAlignmentResult.addActionListener(this);
        fileImportAlignmentResult.setEnabled(true);
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
        fileMenu.add(fileImportAlignmentResult);
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

            // statBar.setStatusText("Please select a data file to open");

            // Open file dialog
            JFileChooser fileOpenChooser = new JFileChooser();
            fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            fileOpenChooser.setMultiSelectionEnabled(true);
            fileOpenChooser.setDialogTitle("Please select data files to open");

            /*
             * if (dataDirectory == null) { dataDirectory =
             * clientForCluster.getDataRootPath(); }
             */
            // fileOpenChooser.setCurrentDirectory(new File(dataDirectory));
            ExampleFileFilter filter = new ExampleFileFilter();
            filter.addExtension("CDF");
            filter.addExtension("nc");
            filter.addExtension("XML");
            filter.addExtension("mzXML");
            filter.setDescription("Raw data files");

            fileOpenChooser.setFileFilter(filter);
            int retval = fileOpenChooser.showOpenDialog(this);

            // If ok to go on with file open
            if (retval == JFileChooser.APPROVE_OPTION) {

                File[] selectedFiles = fileOpenChooser.getSelectedFiles();

                IOController.getInstance().openFiles(selectedFiles,
                        PreloadLevel.NO_PRELOAD);
                /*
                 * String[] dataFilePaths = new String[selectedFiles.length];
                 * 
                 * 
                 * for (int i=0; i<selectedFiles.length; i++) { File f =
                 * selectedFiles[i];
                 * 
                 * if (f.exists()) { dataFilePaths[i] = f.getPath(); // Update
                 * dataDirectory String tmppath = f.getPath().substring(0,
                 * f.getPath().length()-f.getName().length()); dataDirectory =
                 * new String(tmppath); } else { displayErrorMessage("File " + f + "
                 * does not exist."); return; } }
                 * 
                 * clientForCluster.openRawDataFiles(dataFilePaths);
                 */

            } else {
                statBar.setStatusText("File open cancelled.");
            }

        }

        // File->Close
        if (src == fileClose) {

            // Grab selected raw data files
            int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();

            mainWin.closeRawDataFiles(rawDataIDs);

            int[] alignmentResultIDs = itemSelector
                    .getSelectedAlignmentResultIDs();

            mainWin.closeAlignmentResults(alignmentResultIDs);

        }

        // File->Export table
        /*
         * if (src == fileExportPeakList) {
         * 
         * RawDataAtClient[] rawDatas = itemSelector.getSelectedRawDatas();
         * 
         * for (RawDataAtClient r : rawDatas) {
         * 
         * statBar.setStatusText("Writing peak list for file " +
         * r.getNiceName());
         * 
         * if (!r.hasPeakData()) { // No peak list available for active run try {
         * JOptionPane.showInternalMessageDialog( mainWin.mainWin.getDesktop,
         * "No peak data available for " + r.getNiceName() + ". Please run a
         * peak picker first.", "Sorry", JOptionPane.ERROR_MESSAGE ); } catch
         * (Exception exce ) {}
         * 
         * statBar.setStatusText("Peak list export failed."); } else { //
         * Generate name for the peak list file StringTokenizer st = new
         * StringTokenizer(r.getNiceName(),"."); String peakListName=""; String
         * peakListPath=""; String toke = ""; while (st.hasMoreTokens()) { toke =
         * st.nextToken(); if (st.hasMoreTokens()) { peakListName += toke; } }
         * peakListName += "_MZminePeakList" + "." + "txt"; // Save file dialog
         * JFileChooser fileSaveChooser = new JFileChooser();
         * fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
         * fileSaveChooser.setMultiSelectionEnabled(false);
         * fileSaveChooser.setDialogTitle("Please give file name for peak list
         * of " + r.getNiceName()); statBar.setStatusText("Please give file name
         * for peak list of " + r.getNiceName()); //if (dataDirectory!=null) {
         * //fileSaveChooser.setCurrentDirectory(new File(dataDirectory)); //}
         * 
         * fileSaveChooser.setSelectedFile(new File(peakListName));
         * 
         * ExampleFileFilter filter = new ExampleFileFilter();
         * filter.addExtension("txt"); filter.setDescription("Peak list as
         * tab-delimitted text file");
         * 
         * fileSaveChooser.setFileFilter(filter); int retval =
         * fileSaveChooser.showSaveDialog(this);
         * 
         * if(retval == JFileChooser.APPROVE_OPTION) { File selectedFile =
         * fileSaveChooser.getSelectedFile();
         * 
         * String tmpfullpath = selectedFile.getPath(); String datafilename =
         * selectedFile.getName(); String datafilepath =
         * tmpfullpath.substring(0, tmpfullpath.length()-datafilename.length()); //
         * dataDirectory = new String(datafilepath);
         * 
         * peakListName = tmpfullpath;
         * 
         * if (PeakListExporter.writePeakListToFile(r, peakListName)) {
         * statBar.setStatusText("Peak list export done."); } else {
         * mainWin.displayErrorMessage("Failed to write peak list for raw data " +
         * r.getNiceName()); statBar.setStatusText("Peak list export failed."); }
         *  } else { statBar.setStatusText("Peak list export cancelled."); } } }
         * 
         * 
         * Vector<AlignmentResult> results =
         * itemSelector.getSelectedAlignmentResults();
         * 
         * if (results.size()>0) { AlignmentResultExporterParameters areParams =
         * mainWin.getParameterStorage().getAlignmentResultExporterParameters();
         * GeneralParameters genParams =
         * mainWin.getParameterStorage().getGeneralParameters();
         * AlignmentResultExporterParameterSetupDialog arepsd = new
         * AlignmentResultExporterParameterSetupDialog(genParams, areParams);
         * statBar.setStatusText("Please select columns for alignment result
         * exporting."); arepsd.showModal(mainWin.getDesktop);
         * 
         * if (arepsd.getExitCode()==-1) { statBar.setStatusText("Alignment
         * result export cancelled."); return; }
         * 
         * parameterStorage.setAlignmentResultExporterParameters(areParams);
         * 
         * 
         * 
         * for (int i=0; i<results.size(); i++) { AlignmentResult result =
         * results.get(i);
         * 
         * statBar.setStatusText("Writing alignment result " +
         * result.getNiceName() + " to file.");
         * 
         * String resultName = result.getNiceName() + ".txt"; // Save file
         * dialog JFileChooser fileSaveChooser = new JFileChooser();
         * fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
         * fileSaveChooser.setMultiSelectionEnabled(false);
         * fileSaveChooser.setDialogTitle("Please give file name for alignment
         * result " + result.getNiceName()); if (dataDirectory!=null) {
         * fileSaveChooser.setCurrentDirectory(new File(dataDirectory)); }
         * 
         * fileSaveChooser.setSelectedFile(new File(resultName));
         * 
         * ExampleFileFilter filter = new ExampleFileFilter();
         * filter.addExtension("txt"); filter.setDescription("Alignment results
         * as tab-delimitted text file");
         * 
         * fileSaveChooser.setFileFilter(filter); int retval =
         * fileSaveChooser.showSaveDialog(this);
         * 
         * if(retval == JFileChooser.APPROVE_OPTION) { File selectedFile =
         * fileSaveChooser.getSelectedFile();
         * 
         * String tmpfullpath = selectedFile.getPath(); String datafilename =
         * selectedFile.getName(); String datafilepath =
         * tmpfullpath.substring(0, tmpfullpath.length()-datafilename.length());
         * dataDirectory = new String(datafilepath);
         * 
         * resultName = datafilepath + datafilename;
         * 
         * 
         * //result.writeResultsToFile(resultName, areParams, this);
         * AlignmentResultExporter.exportAlignmentResultToFile(result,
         * resultName, areParams, this); statBar.setStatusText("Alignment result
         * export done."); } else { statBar.setStatusText("Alignment result
         * export cancelled."); } } }
         * 
         * 
         *  }
         * 
         * if (src == fileLoadParameters) { statBar.setStatusText("Please select
         * a parameter file"); // Open file dialog JFileChooser fileOpenChooser =
         * new JFileChooser();
         * fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
         * fileOpenChooser.setMultiSelectionEnabled(false);
         * fileOpenChooser.setDialogTitle("Please select parameter file"); //
         * fileOpenChooser.setCurrentDirectory(new
         * File(clientForCluster.getDataRootPath()));
         * 
         * ExampleFileFilter filter = new ExampleFileFilter();
         * filter.addExtension("XML"); filter.setDescription("MZmine parameters
         * file");
         * 
         * fileOpenChooser.setFileFilter(filter); int retval =
         * fileOpenChooser.showOpenDialog(this); // If ok to go on with file
         * open if(retval == JFileChooser.APPROVE_OPTION) {
         * 
         * File selectedFile = fileOpenChooser.getSelectedFile(); if
         * (!(selectedFile.exists())) { displayErrorMessage("Parameter file " +
         * selectedFile + " does not exist!"); statBar.setStatusText("Parameter
         * file loading failed."); return; } if
         * (mainWin.getParameterStorage().readParametesFromFile(selectedFile)) {
         * statBar.setStatusText("Parameter file loading done."); } else {
         * displayErrorMessage("Parameter file " + selectedFile + " loading
         * failed!"); statBar.setStatusText("Parameter file loading failed."); } }
         * else { statBar.setStatusText("Parameter file loading cancelled."); } }
         * 
         * 
         * if (src == fileSaveParameters) { // Save file dialog JFileChooser
         * fileSaveChooser = new JFileChooser();
         * fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
         * fileSaveChooser.setMultiSelectionEnabled(false);
         * fileSaveChooser.setDialogTitle("Please give file name for parameters
         * file.");
         * 
         * ExampleFileFilter filter = new ExampleFileFilter();
         * filter.addExtension("xml"); filter.setDescription("MZmine parameters
         * file");
         * 
         * fileSaveChooser.setFileFilter(filter); int retval =
         * fileSaveChooser.showSaveDialog(this);
         * 
         * if(retval == JFileChooser.APPROVE_OPTION) { File selectedFile =
         * fileSaveChooser.getSelectedFile(); // Ask for overwrite? if
         * (selectedFile.exists()) { } // Write parameters
         * mainWin.getParameterStorage().writeParametersToFile(selectedFile);
         * 
         * 
         * statBar.setStatusText("Parameters written to file."); } else {
         * statBar.setStatusText("Parameter saving cancelled."); } }
         * 
         * 
         * if (src == fileImportAlignmentResult) {
         * 
         * statBar.setStatusText("Please select a result file to import"); //
         * Open file dialog JFileChooser fileOpenChooser = new JFileChooser();
         * fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
         * fileOpenChooser.setMultiSelectionEnabled(false);
         * fileOpenChooser.setDialogTitle("Please select alignment result file
         * to open"); if (dataDirectory!=null) {
         * fileOpenChooser.setCurrentDirectory(new File(dataDirectory)); }
         * 
         * ExampleFileFilter filter = new ExampleFileFilter();
         * filter.addExtension("txt"); filter.setDescription("Tab-delimitted
         * text files"); fileOpenChooser.addChoosableFileFilter(filter);
         * 
         * int retval = fileOpenChooser.showOpenDialog(this); // If ok to go on
         * with file open if(retval == JFileChooser.APPROVE_OPTION) {
         * 
         * File f = fileOpenChooser.getSelectedFile(); String fpath =
         * f.getPath(); String fname = f.getName();
         * 
         * String tmpfullpath = f.getPath(); String datafilename = f.getName();
         * String datafilepath = tmpfullpath.substring(0,
         * tmpfullpath.length()-datafilename.length()); dataDirectory = new
         * String(datafilepath); // Create new alignment result AlignmentResult
         * ar =
         * AlignmentResultExporter.importAlignmentResultFromFile(datafilepath,
         * datafilename);
         * 
         * if (ar == null) { displayErrorMessage("Could not import alignment
         * result from file " + datafilename + "\n" + "(Maybe it was not
         * exported in Wide format?)"); return; }
         * 
         * itemSelector.addAlignmentResult(ar);
         * addAlignmentResultVisualizerList(ar); // tileWindows();
         * 
         * statBar.setStatusText("Result file imported."); } else {
         * statBar.setStatusText("Result import cancelled"); } }
         * 
         * if (src == filePrint) {
         * 
         * JInternalFrame activeWindow = desktop.getSelectedFrame(); if
         * (activeWindow!=null) { if ((activeWindow.getClass() ==
         * RawDataVisualizerTICView.class) || (activeWindow.getClass() ==
         * RawDataVisualizerTwoDView.class) || (activeWindow.getClass() ==
         * RawDataVisualizerSpectrumView.class) ) {
         * 
         * ((RawDataVisualizer)activeWindow).printMe(); }
         * 
         * if (activeWindow!=null) { if ((activeWindow.getClass() ==
         * AlignmentResultVisualizerLogratioPlotView.class) ||
         * (activeWindow.getClass() ==
         * AlignmentResultVisualizerCoVarPlotView.class) ||
         * (activeWindow.getClass() ==
         * AlignmentResultVisualizerCDAPlotView.class) ) {
         * ((AlignmentResultVisualizer)activeWindow).printMe(); } } } }
         * 
         * if (src == editCopy) { JInternalFrame activeWindow =
         * desktop.getSelectedFrame(); if (activeWindow!=null) { if
         * ((activeWindow.getClass() == RawDataVisualizerTICView.class) ||
         * (activeWindow.getClass() == RawDataVisualizerTwoDView.class) ||
         * (activeWindow.getClass() == RawDataVisualizerSpectrumView.class) ) {
         * 
         * ((RawDataVisualizer)activeWindow).copyMe(); }
         * 
         * if (activeWindow!=null) { if ((activeWindow.getClass() ==
         * AlignmentResultVisualizerLogratioPlotView.class) ||
         * (activeWindow.getClass() ==
         * AlignmentResultVisualizerCoVarPlotView.class) ||
         * (activeWindow.getClass() ==
         * AlignmentResultVisualizerCDAPlotView.class) ) {
         * ((AlignmentResultVisualizer)activeWindow).copyMe(); } } } } // File ->
         * Exit if (src == fileExit) {
         * 
         * statBar.setStatusText("Exiting."); exitMZmine(); }
         * 
         * if (src == toolsOptions) {
         * 
         * new OptionsWindow(this); }
         * 
         * if (src == ssMeanFilter) { // Ask parameters from user MeanFilter mf =
         * new MeanFilter(); MeanFilterParameters mfParam =
         * mf.askParameters(this, parameterStorage.getMeanFilterParameters());
         * if (mfParam==null) { statBar.setStatusText("Mean filtering
         * cancelled."); return; }
         * parameterStorage.setMeanFilterParameters(mfParam); // It seems user
         * didn't cancel statBar.setStatusText("Mean filtering spectra.");
         * paintNow(); // Collect raw data IDs and initiate filtering on the
         * cluster int[] selectedRawDataIDs =
         * itemSelector.getSelectedRawDataIDs(); //
         * clientForCluster.filterRawDataFiles(selectedRawDataIDs, mfParam); }
         * 
         * if (src == ssSGFilter) { // Ask parameters from user
         * SavitzkyGolayFilter sf = new SavitzkyGolayFilter();
         * SavitzkyGolayFilterParameters sfParam = sf.askParameters(this,
         * parameterStorage.getSavitzkyGolayFilterParameters()); if
         * (sfParam==null) { statBar.setStatusText("Savitzky-Golay filtering
         * cancelled."); return; }
         * parameterStorage.setSavitzkyGolayFilterParameters(sfParam); // It
         * seems user didn't cancel statBar.setStatusText("Savitzky-Golay
         * filtering spectra."); paintNow(); // Collect raw data IDs and
         * initiate filtering on the cluster int[] selectedRawDataIDs =
         * itemSelector.getSelectedRawDataIDs(); //
         * clientForCluster.filterRawDataFiles(selectedRawDataIDs, sfParam); }
         * 
         * if (src == ssChromatographicMedianFilter) { // Ask parameters from
         * user ChromatographicMedianFilter cmf = new
         * ChromatographicMedianFilter(); ChromatographicMedianFilterParameters
         * cmfParam = cmf.askParameters(this,
         * parameterStorage.getChromatographicMedianFilterParameters()); if
         * (cmfParam==null) { statBar.setStatusText("Chromatographic median
         * filtering cancelled."); return; }
         * parameterStorage.setChromatographicMedianFilterParameters(cmfParam); //
         * It seems user didn't cancel statBar.setStatusText("Filtering with
         * chromatographic median filter."); paintNow(); // Collect raw data IDs
         * and initiate filtering on the cluster int[] selectedRawDataIDs =
         * itemSelector.getSelectedRawDataIDs(); //
         * clientForCluster.filterRawDataFiles(selectedRawDataIDs, cmfParam); }
         * 
         * if (src == ssCropFilter) { // Ask parameters from user CropFilter cf =
         * new CropFilter(); CropFilterParameters cfParam =
         * cf.askParameters(this, parameterStorage.getCropFilterParameters());
         * if (cfParam==null) { statBar.setStatusText("Crop filtering
         * cancelled."); return; }
         * parameterStorage.setCropFilterParameters(cfParam); // It seems user
         * didn't cancel statBar.setStatusText("Filtering with cropping
         * filter."); paintNow(); // Collect raw data IDs and initiate filtering
         * on the cluster int[] selectedRawDataIDs =
         * itemSelector.getSelectedRawDataIDs(); //
         * clientForCluster.filterRawDataFiles(selectedRawDataIDs, cfParam); }
         * 
         * if (src == ssZoomScanFilter) { // Ask parameters from user
         * ZoomScanFilter zsf = new ZoomScanFilter(); ZoomScanFilterParameters
         * zsfParam = zsf.askParameters(this,
         * parameterStorage.getZoomScanFilterParameters()); if (zsfParam==null) {
         * statBar.setStatusText("Zoom scan filtering cancelled."); return; }
         * parameterStorage.setZoomScanFilterParameters(zsfParam); // It seems
         * user didn't cancel statBar.setStatusText("Filtering with zoom scan
         * filter."); paintNow(); // Collect raw data IDs and initiate filtering
         * on the cluster int[] selectedRawDataIDs =
         * itemSelector.getSelectedRawDataIDs(); //
         * clientForCluster.filterRawDataFiles(selectedRawDataIDs, zsfParam); }
         * 
         * if (src == ssRecursiveThresholdPicker) {
         * 
         * RecursiveThresholdPicker rp = new RecursiveThresholdPicker();
         * 
         * RecursiveThresholdPickerParameters rpParam = rp.askParameters(this,
         * mainWin.getParameterStorage().getRecursiveThresholdPickerParameters());
         * if (rpParam == null) { statBar.setStatusText("Peak picking
         * cancelled."); return; }
         * mainWin.getParameterStorage().setRecursiveThresholdPickerParameters(rpParam);
         * 
         * int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
         * 
         * statBar.setStatusText("Searching for peaks."); paintNow(); // Call
         * cluster controller // clientForCluster.findPeaks(rawDataIDs,
         * rpParam);
         * 
         * rp = null; rpParam = null; }
         * 
         * if (src == ssLocalPicker) {
         * 
         * LocalPicker lp = new LocalPicker();
         * 
         * LocalPickerParameters lpParam = lp.askParameters(this,
         * mainWin.getParameterStorage().getLocalPickerParameters()); if
         * (lpParam == null) { statBar.setStatusText("Peak picking cancelled.");
         * return; }
         * mainWin.getParameterStorage().setLocalPickerParameters(lpParam);
         * 
         * int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
         * 
         * statBar.setStatusText("Searching for peaks."); paintNow(); // Call
         * cluster controller // clientForCluster.findPeaks(rawDataIDs,
         * lpParam);
         * 
         * lp = null; lpParam = null; }
         * 
         * if (src == ssCentroidPicker) {
         * 
         * CentroidPicker cp = new CentroidPicker();
         * 
         * CentroidPickerParameters cpParam = cp.askParameters(this,
         * mainWin.getParameterStorage().getCentroidPickerParameters()); if
         * (cpParam == null) { statBar.setStatusText("Peak picking cancelled.");
         * return; }
         * mainWin.getParameterStorage().setCentroidPickerParameters(cpParam);
         * 
         * int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
         * 
         * statBar.setStatusText("Searching for peaks."); paintNow(); // Call
         * cluster controller to start peak picking process //
         * clientForCluster.findPeaks(rawDataIDs, cpParam);
         * 
         * cp = null; cpParam = null; }
         * 
         * if (src == ssSimpleDeisotoping) {
         * 
         * SimpleDeisotoper sd = new SimpleDeisotoper();
         * 
         * SimpleDeisotoperParameters sdParam = sd.askParameters(this,
         * mainWin.getParameterStorage().getSimpleDeisotoperParameters()); if
         * (sdParam == null) { statBar.setStatusText("Deisotoping cancelled.");
         * return; }
         * mainWin.getParameterStorage().setSimpleDeisotoperParameters(sdParam);
         * 
         * Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer,
         * PeakList>(); int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
         * for (int i=0; i<rawDataIDs.length; i++) { peakLists.put( new
         * Integer(rawDataIDs[i]),
         * itemSelector.getRawDataByID(rawDataIDs[i]).getPeakList() ); }
         * 
         * statBar.setStatusText("Deisotoping peak lists."); paintNow(); // Call
         * cluster controller to start peak picking process //
         * clientForCluster.processPeakLists(peakLists, sdParam);
         * 
         * sd = null; sdParam = null; }
         * 
         * if (src == ssCombinatorialDeisotoping) {
         * 
         * CombinatorialDeisotoper cd = new CombinatorialDeisotoper();
         * 
         * CombinatorialDeisotoperParameters cdParam = cd.askParameters(this,
         * mainWin.getParameterStorage().getCombinatorialDeisotoperParameters());
         * if (cdParam == null) { statBar.setStatusText("Deisotoping
         * cancelled."); return; }
         * mainWin.getParameterStorage().setCombinatorialDeisotoperParameters(cdParam);
         * 
         * Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer,
         * PeakList>(); int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
         * for (int i=0; i<rawDataIDs.length; i++) { peakLists.put( new
         * Integer(rawDataIDs[i]),
         * itemSelector.getRawDataByID(rawDataIDs[i]).getPeakList() ); }
         * 
         * statBar.setStatusText("Deisotoping peak lists."); paintNow(); // Call
         * cluster controller to start peak picking process //
         * clientForCluster.processPeakLists(peakLists, cdParam);
         * 
         * cd = null; cdParam = null; }
         * 
         * 
         * 
         * if (src == ssIncompleteIsotopePatternFilter) {
         * 
         * IncompleteIsotopePatternFilter iif = new
         * IncompleteIsotopePatternFilter();
         * 
         * IncompleteIsotopePatternFilterParameters iifParam =
         * iif.askParameters(this,
         * mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters());
         * if (iifParam == null) { statBar.setStatusText("Peak list filtering
         * cancelled."); return; }
         * mainWin.getParameterStorage().setIncompleteIsotopePatternFilterParameters(iifParam);
         * 
         * Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer,
         * PeakList>(); int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
         * for (int i=0; i<rawDataIDs.length; i++) { peakLists.put( new
         * Integer(rawDataIDs[i]),
         * itemSelector.getRawDataByID(rawDataIDs[i]).getPeakList() ); }
         * 
         * statBar.setStatusText("Filtering peak lists."); paintNow(); // Call
         * cluster controller to start peak picking process //
         * clientForCluster.processPeakLists(peakLists, iifParam);
         * 
         * 
         * iif = null; iifParam = null; }
         * 
         * if (src == tsJoinAligner) { // Make sure that every selected raw data
         * file has a peak list Hashtable<Integer, PeakList> peakLists = new
         * Hashtable<Integer, PeakList>();
         * 
         * int[] rawDataIDs = itemSelector.getSelectedRawDataIDs(); for (int
         * rawDataID : rawDataIDs) { RawDataAtClient rawData =
         * itemSelector.getRawDataByID(rawDataID); if (!(rawData.hasPeakData())) {
         * displayErrorMessage("Can't align: " + rawData.getNiceName() + " has
         * no peak list available."); peakLists = null; return; }
         * peakLists.put(new Integer(rawDataID), rawData.getPeakList()); } //
         * Show user parameter setup dialog JoinAligner ja = new JoinAligner();
         * JoinAlignerParameters jaParam = ja.askParameters(this,
         * mainWin.getParameterStorage().getJoinAlignerParameters()); if
         * (jaParam==null) { statBar.setStatusText("Alignment cancelled.");
         * return; }
         * mainWin.getParameterStorage().setJoinAlignerParameters(jaParam);
         * 
         * statBar.setStatusText("Aligning peak lists."); paintNow(); // Call
         * cluster controller // clientForCluster.doAlignment(peakLists,
         * jaParam);
         * 
         * ja = null; jaParam = null; }
         * 
         * if (src == tsFastAligner) { // Make sure that every selected raw data
         * file has a peak list Hashtable<Integer, PeakList> peakLists = new
         * Hashtable<Integer, PeakList>();
         * 
         * int[] rawDataIDs = itemSelector.getSelectedRawDataIDs(); for (int
         * rawDataID : rawDataIDs) { RawDataAtClient rawData =
         * itemSelector.getRawDataByID(rawDataID); if (!(rawData.hasPeakData())) {
         * displayErrorMessage("Can't align: " + rawData.getNiceName() + " has
         * no peak list available."); peakLists = null; return; }
         * peakLists.put(new Integer(rawDataID), rawData.getPeakList()); } //
         * Show user parameter setup dialog FastAligner fa = new FastAligner();
         * FastAlignerParameters faParam = fa.askParameters(this,
         * mainWin.getParameterStorage().getFastAlignerParameters()); if
         * (faParam==null) { statBar.setStatusText("Alignment cancelled.");
         * return; }
         * mainWin.getParameterStorage().setFastAlignerParameters(faParam);
         * 
         * statBar.setStatusText("Aligning peak lists."); paintNow(); // Call
         * cluster controller // clientForCluster.doAlignment(peakLists,
         * faParam);
         * 
         * fa = null; faParam = null; }
         * 
         * if (src == normLinear) {
         * 
         * LinearNormalizer ln = new LinearNormalizer();
         * 
         * LinearNormalizerParameters lnp = ln.askParameters(this,
         * mainWin.getParameterStorage().getLinearNormalizerParameters()); if
         * (lnp == null) { statBar.setStatusText("Normalization cancelled.");
         * return; }
         * mainWin.getParameterStorage().setLinearNormalizerParameters(lnp); //
         * If normalization by total raw signal, then must use controller to
         * calc total raw signals and finally do normalization if
         * (lnp.paramNormalizationType ==
         * LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL) { //
         * Collect raw data IDs from all selected alignment results Vector<AlignmentResult>
         * selectedAlignmentResults =
         * itemSelector.getSelectedAlignmentResults(); Vector<Integer>
         * allRequiredRawDataIDs = new Vector<Integer>(); // Loop all selected
         * alignment results for (AlignmentResult ar :selectedAlignmentResults) { //
         * Loop all raw data IDs in current alignment results int[]
         * tmpRawDataIDs = ar.getRawDataIDs(); for (int tmpRawDataID :
         * tmpRawDataIDs) { Integer tmpRawDataIDI = new Integer(tmpRawDataID); //
         * If this raw data id is not yet included, then add it if (
         * allRequiredRawDataIDs.indexOf(tmpRawDataIDI) == -1 ) {
         * allRequiredRawDataIDs.add(tmpRawDataIDI); } } } // Move required raw
         * Data IDs from Vector<Integer> to int[] int[] allRequiredRawDataIDsi =
         * new int[allRequiredRawDataIDs.size()]; for (int i=0; i<allRequiredRawDataIDs.size();
         * i++) { allRequiredRawDataIDsi[i] =
         * allRequiredRawDataIDs.get(i).intValue(); } // Ask controller to fetch
         * total raw signals for these raw data files //Vector<AlignmentResult>
         * selectedAlignmentResults =
         * itemSelector.getSelectedAlignmentResults(); //
         * clientForCluster.calcTotalRawSignal(allRequiredRawDataIDsi, lnp,
         * selectedAlignmentResults); // doLinearNormalizationClientSide() will
         * be called by clientForCluster when task completes } else { // Any
         * other linear normalization method doesn't need access to raw data, so
         * it is possible to call doLinearNormalizationClientSide() immediately
         * Vector<AlignmentResult> selectedAlignmentResults =
         * itemSelector.getSelectedAlignmentResults();
         * doLinearNormalizationClientSide(lnp, selectedAlignmentResults); } }
         * 
         * 
         * if (src == normStdComp) {
         * 
         * StandardCompoundNormalizer scn = new StandardCompoundNormalizer();
         * 
         * StandardCompoundNormalizerParameters scnp = scn.askParameters(this,
         * mainWin.getParameterStorage().getStandardCompoundNormalizerParameters());
         * if (scnp==null) { statBar.setStatusText("Normalization cancelled.");
         * return; }
         * mainWin.getParameterStorage().setStandardCompoundNormalizerParameters(scnp);
         * 
         * statBar.setStatusText("Normalizing selected alignment results.");
         * paintNow(); // setBusy(true);
         * 
         * Vector<AlignmentResult> selectedAlignmentResults =
         * itemSelector.getSelectedAlignmentResults(); Enumeration<AlignmentResult>
         * selectedAlignmentResultEnum = selectedAlignmentResults.elements();
         * while (selectedAlignmentResultEnum.hasMoreElements()) {
         * AlignmentResult ar = selectedAlignmentResultEnum.nextElement();
         * AlignmentResult nar = scn.calcNormalization(this, ar, scnp);
         * 
         * if (nar==null) { if (ar.getNumOfStandardCompounds()==0) {
         * displayErrorMessage("Could not normalize " + ar.getNiceName() + ",
         * because it does not have any standard compounds defined."); } else {
         * displayErrorMessage("Could not normalize " + ar.getNiceName() + ",
         * because of an unknown error."); } } else {
         * itemSelector.addAlignmentResult(nar);
         * addAlignmentResultVisualizerList(nar); } }
         * 
         * statBar.setStatusText("Normalization done."); // setBusy(false); }
         * 
         * 
         * if (src == tsAlignmentFilter) { // Get all selected alignment results
         * Vector<AlignmentResult> alignmentResults =
         * itemSelector.getSelectedAlignmentResults(); if
         * (alignmentResults==null) { return; }
         * 
         * AlignmentResultFilterByGaps arfbg = new
         * AlignmentResultFilterByGaps();
         * 
         * AlignmentResultFilterByGapsParameters arfbgParam=
         * arfbg.askParameters(this,
         * mainWin.getParameterStorage().getAlignmentResultFilterByGapsParameters());
         * if (arfbgParam == null) { statBar.setStatusText("Alignment result
         * filtering cancelled."); return; }
         * mainWin.getParameterStorage().setAlignmentResultFilterByGapsParameters(arfbgParam);
         * 
         * runAlignmentResultFilteringByGapsClientSide(arfbgParam,
         * alignmentResults); }
         * 
         * if (src == tsEmptySlotFiller) { // Get all selected alignment results
         * Vector<AlignmentResult> alignmentResults =
         * itemSelector.getSelectedAlignmentResults(); if
         * (alignmentResults==null) { return; } // Check that only a single
         * alignment result was selected if (alignmentResults.size()!=1) {
         * displayErrorMessage("Please select only a single alignment result for
         * gap filling."); statBar.setStatusText("Please select only a single
         * alignment result for gap filling."); return; } AlignmentResult
         * alignmentResult = alignmentResults.get(0); // Check that the selected
         * alignment result is not an imported version if
         * (alignmentResult.isImported()) { }
         * 
         * SimpleGapFiller sgf = new SimpleGapFiller();
         * 
         * SimpleGapFillerParameters sgfParam= sgf.askParameters(this,
         * mainWin.getParameterStorage().getSimpleGapFillerParameters()); if
         * (sgfParam == null) { statBar.setStatusText("Gap filling cancelled.");
         * return; }
         * mainWin.getParameterStorage().setSimpleGapFillerParameters(sgfParam);
         * 
         * 
         * statBar.setStatusText("Filling empty gaps in alignment result."); //
         * paintNow(); // clientForCluster.fillGaps(alignmentResult, sgfParam); }
         * 
         * if (src == batDefine) { // Get selected rawDataIDs int[] rawDataIDs =
         * itemSelector.getSelectedRawDataIDs();
         * 
         * BatchModeDialog bmd = new BatchModeDialog(mainWin, rawDataIDs); }
         * 
         * if (src == anOpenSRView) { // Add a new visualizer to all selected
         * alignment results Vector<AlignmentResult> rv =
         * itemSelector.getSelectedAlignmentResults();
         * 
         * for (AlignmentResult ar : rv ) {
         * mainWin.addAlignmentResultVisualizerLogratioPlot(ar); } // //
         * tileWindows(); }
         * 
         * 
         * if (src == anOpenSCVView) { // Add a new visualizer to all selected
         * alignment results Vector<AlignmentResult> rv =
         * itemSelector.getSelectedAlignmentResults();
         * 
         * for (AlignmentResult ar : rv ) {
         * mainWin.addAlignmentResultVisualizerCoVarPlot(ar); } //
         * tileWindows(); }
         * 
         * if (src == anOpenCDAView) { // Add a new visualizer to all selected
         * alignment results // setBusy(true); Vector<AlignmentResult> rv =
         * itemSelector.getSelectedAlignmentResults(); for (AlignmentResult ar :
         * rv ) { mainWin.addAlignmentResultVisualizerCDAPlot(ar); } //
         * setBusy(false); updateMenuAvailability(); // tileWindows(); }
         * 
         * if (src == anOpenSammonsView) { // Add a new visualizer to all
         * selected alignment results // setBusy(true); Vector<AlignmentResult>
         * rv = itemSelector.getSelectedAlignmentResults(); for (AlignmentResult
         * ar : rv ) { // Create new visualizer
         * //AlignmentResultVisualizerSammonsPlotView sammonsView = new
         * AlignmentResultVisualizerSammonsPlotView(this);
         * //sammonsView.askParameters(alignmentResult,
         * parameterStorage.getAlignmentResultVisualizerSammonsPlotViewParameters());
         * mainWin.addAlignmentResultVisualizerSammonsPlot(ar); } //
         * setBusy(false); // tileWindows(); updateMenuAvailability(); }
         * 
         * 
         * if (src == windowTileWindows) { // tileWindows(); }
         * 
         * 
         * if (src == hlpAbout) { AboutDialog ad = new AboutDialog();
         * ad.showModal(mainWin.getDesktop); }
         * 
         */

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

        fileExportPeakList.setText("Export...");

        /*
         * if ( (numOfRawDataWithVisibleVisualizer(false)>0) ||
         * (numOfResultsWithVisibleVisualizer(false)>0) ) {
         * windowTileWindows.setEnabled(true); }
         */
        RawDataAtClient actRawData = itemSelector.getActiveRawData();
        if (actRawData != null) {
            fileClose.setEnabled(true);

            ssMeanFilter.setEnabled(true);
            ssSGFilter.setEnabled(true);
            ssChromatographicMedianFilter.setEnabled(true);
            ssCropFilter.setEnabled(true);
            ssZoomScanFilter.setEnabled(true);
            ssRecursiveThresholdPicker.setEnabled(true);
            ssLocalPicker.setEnabled(true);
            ssCentroidPicker.setEnabled(true);

            batDefine.setEnabled(true);

            if (actRawData.hasPeakData()) {
                ssSimpleDeisotoping.setEnabled(true);
                // ssCombinatorialDeisotoping.setEnabled(true); DEBUG: Feature
                // not yet ready
                ssIncompleteIsotopePatternFilter.setEnabled(true);
                fileExportPeakList.setEnabled(true);
                tsJoinAligner.setEnabled(true);
                tsFastAligner.setEnabled(true);
            }

            JInternalFrame activeWindow = mainWin.getDesktop()
                    .getSelectedFrame();

            if (activeWindow != null) {
                if ((activeWindow.getClass() == RawDataVisualizerTICView.class)
                        || (activeWindow.getClass() == RawDataVisualizerTwoDView.class)
                        || (activeWindow.getClass() == RawDataVisualizerSpectrumView.class)) {
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
