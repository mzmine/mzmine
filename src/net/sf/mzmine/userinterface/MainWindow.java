/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.userinterface;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.alignment.AlignmentResultExporter;
import net.sf.mzmine.methods.alignment.AlignmentResultExporterParameterSetupDialog;
import net.sf.mzmine.methods.alignment.AlignmentResultExporterParameters;
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGaps;
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.AlignmentResultProcessorParameters;
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
import net.sf.mzmine.obsoletedatastructures.ParameterStorage;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.obsoletedistributionframework.ClientForCluster;
import net.sf.mzmine.obsoletedistributionframework.ControllerServer;
import net.sf.mzmine.util.GeneralParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizer;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCDAPlotViewParameters;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerCoVarPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerList;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerLogratioPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotView;
import net.sf.mzmine.visualizers.alignmentresult.AlignmentResultVisualizerSammonsPlotViewParameters;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerPeakListView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshRequest;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerRefreshResult;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerSpectrumView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerTICView;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizerTwoDView;
import sunutils.ExampleFileFilter;


/**
 * This class is the main window of application
 *
 */
public class MainWindow extends JFrame implements WindowListener, ActionListener {

    private static MainWindow myInstance;

	// This is the .ini file for GUI program (client for cluster has different .ini file)
	private static final String settingsFilename = "mzmine.ini";

	// These constants are used with setSameZoomToOtherRawDatas method
	public static final int SET_SAME_ZOOM_MZ = 1;
	public static final int SET_SAME_ZOOM_SCAN = 2;
	public static final int SET_SAME_ZOOM_BOTH = 3;


	public static MainWindow getInstance() { return myInstance; }

    // GUI components
    private JDesktopPane desktop;

    private MouseAdapter myMouseAdapter;
    private Cursor myWaitCursor = new Cursor(Cursor.WAIT_CURSOR);
	private Cursor myDefaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    private JMenuBar menuBar;
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
	    private JMenuItem ssWindowedPicker;
	    private JMenuItem ssRecursiveThresholdPicker;
	    private JMenuItem ssLocalPicker;
	    private JMenuItem ssCentroidPicker;
	    private JMenuItem ssSimpleDeisotoping;
	    private JMenuItem ssCombinatorialDeisotoping;
	    private JMenuItem ssIncompleteIsotopePatternFilter;
    private JMenu alignmentMenu;
	    private JMenuItem tsSimpleAligner;
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
    private JSplitPane split;

    private ParameterStorage parameterStorage;

    //private ItemStorage itemStorage;
    private ItemSelector itemSelector;

	// This table maps rawDataIDs to visualizers showing the data
	private Hashtable<Integer, Vector<RawDataVisualizer>> rawDataVisualizers;
	private Hashtable<Integer, Vector<AlignmentResultVisualizer>> alignmentResultVisualizers;

	private ClientForCluster clientForCluster;


	private boolean busyFlag;		// This flag is true when system is busy doing some computational stuff

	private String dataDirectory;		// Stores the last used directory for saving peak lists & alignment results

/*
    private String dataDirectory;
    private String homeDirectory;
*/


	/**
	 * Constructor for MainWindow
	 */
	public MainWindow(String title, ControllerServer controllerServer) {

        assert myInstance == null;
        myInstance = this;
        
		// Connect to MZmine cluster
		// -------------------------
		if (controllerServer==null) {
			// Cluster mode
//			clientForCluster = new ClientForCluster(this);
		} else {
			// Single computer mode
//			clientForCluster = new ClientForCluster(this, controllerServer);
		}



		// Setup data structures
		// ---------------------

		// Load default parameter settings
		parameterStorage = new ParameterStorage();
		//parameterStorage.readParametesFromFile(settingsFilename); (not automatic)


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

		// Initialize method parameter storage (and default parameter values too)
		parameterStorage = new ParameterStorage();

		// Construct menu

		menuBar = new JMenuBar();

		fileMenu = new JMenu(); fileMenu.setText("File");fileMenu.setMnemonic(KeyEvent.VK_F);fileMenu.addActionListener(this);
		fileOpen = new JMenuItem("Open...", KeyEvent.VK_O);fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));fileOpen.addActionListener(this);
		fileClose = new JMenuItem("Close", KeyEvent.VK_C);fileClose.addActionListener(this);fileClose.setEnabled(false);
		fileExportPeakList = new JMenuItem("Export...",KeyEvent.VK_E);fileExportPeakList.addActionListener(this);fileExportPeakList.setEnabled(false);
		fileImportAlignmentResult = new JMenuItem("Import alignment result...", KeyEvent.VK_I);fileImportAlignmentResult.addActionListener(this);fileImportAlignmentResult.setEnabled(true);
		fileSaveParameters = new JMenuItem("Save parameters...", KeyEvent.VK_S);fileSaveParameters.addActionListener(this);fileSaveParameters.setEnabled(true);
		fileLoadParameters = new JMenuItem("Load parameters...", KeyEvent.VK_S);fileLoadParameters.addActionListener(this);fileLoadParameters.setEnabled(true);
		filePrint = new JMenuItem("Print figure...", KeyEvent.VK_P);filePrint.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));filePrint.addActionListener(this);filePrint.setEnabled(false);
		fileExit = new JMenuItem("Exit",KeyEvent.VK_X);fileExit.addActionListener(this);
		fileMenu.add(fileOpen);fileMenu.add(fileClose);
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
		menuBar.add(fileMenu);

		editMenu = new JMenu(); editMenu.setText("Edit");editMenu.setMnemonic(KeyEvent.VK_E);editMenu.addActionListener(this);
		editCopy = new JMenuItem("Copy", KeyEvent.VK_C);editCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));editCopy.addActionListener(this);editCopy.setEnabled(false);
		editMenu.add(editCopy);
		menuBar.add(editMenu);

		filterMenu = new JMenu();filterMenu.setText("Raw data filtering");filterMenu.setMnemonic(KeyEvent.VK_R);filterMenu.addActionListener(this);
		ssMeanFilter = new JMenuItem("Mean filter spectra",KeyEvent.VK_M);ssMeanFilter.addActionListener(this);ssMeanFilter.setEnabled(false);
		ssSGFilter = new JMenuItem("Savitzky-Golay filter spectra",KeyEvent.VK_S);ssSGFilter.addActionListener(this);ssSGFilter.setEnabled(false);
		ssChromatographicMedianFilter = new JMenuItem("Chromatographic median filter",KeyEvent.VK_S);ssChromatographicMedianFilter.addActionListener(this);ssChromatographicMedianFilter.setEnabled(false);
		ssCropFilter = new JMenuItem("Cropping filter",KeyEvent.VK_C);ssCropFilter.addActionListener(this);ssCropFilter.setEnabled(false);
		ssZoomScanFilter = new JMenuItem("Zoom scan filter",KeyEvent.VK_Z);ssZoomScanFilter.addActionListener(this);ssZoomScanFilter.setEnabled(false);
		filterMenu.add(ssMeanFilter);
		filterMenu.add(ssSGFilter);
		filterMenu.add(ssChromatographicMedianFilter);
		filterMenu.add(ssCropFilter);
		filterMenu.add(ssZoomScanFilter);
		menuBar.add(filterMenu);

		peakMenu = new JMenu();peakMenu.setText("Peak detection");peakMenu.setMnemonic(KeyEvent.VK_P);peakMenu.addActionListener(this);
		ssRecursiveThresholdPicker = new JMenuItem();ssRecursiveThresholdPicker.setText("Recursive threshold peak detector");ssRecursiveThresholdPicker.addActionListener(this);ssRecursiveThresholdPicker.setEnabled(false);
		ssLocalPicker = new JMenuItem();ssLocalPicker.setText("Local maxima peak detector");ssLocalPicker.addActionListener(this);ssLocalPicker.setEnabled(false);
		ssCentroidPicker = new JMenuItem();ssCentroidPicker.setText("Centroid peak detector");ssCentroidPicker.addActionListener(this);ssCentroidPicker.setEnabled(false);
		ssSimpleDeisotoping = new JMenuItem();ssSimpleDeisotoping.setText("Simple deisotoper");ssSimpleDeisotoping.addActionListener(this);ssSimpleDeisotoping.setEnabled(false);
		ssCombinatorialDeisotoping = new JMenuItem();ssCombinatorialDeisotoping.setText("Combinatorial deisotoping");ssCombinatorialDeisotoping.addActionListener(this);ssCombinatorialDeisotoping.setEnabled(false);
		ssIncompleteIsotopePatternFilter = new JMenuItem();ssIncompleteIsotopePatternFilter.setText("Filter incomplete isotope patterns");ssIncompleteIsotopePatternFilter.addActionListener(this);ssIncompleteIsotopePatternFilter.setEnabled(false);
		peakMenu.add(ssRecursiveThresholdPicker);
		peakMenu.add(ssLocalPicker);
		peakMenu.add(ssCentroidPicker);
		peakMenu.addSeparator();
		peakMenu.add(ssSimpleDeisotoping);
		//peakMenu.add(ssCombinatorialDeisotoping);
		peakMenu.add(ssIncompleteIsotopePatternFilter);
		menuBar.add(peakMenu);

		alignmentMenu = new JMenu();alignmentMenu.setText("Alignment");alignmentMenu.setMnemonic(KeyEvent.VK_A);alignmentMenu.addActionListener(this);
		tsJoinAligner = new JMenuItem("Slow aligner", KeyEvent.VK_S);tsJoinAligner.addActionListener(this);
		tsFastAligner = new JMenuItem("Fast aligner", KeyEvent.VK_A);tsFastAligner.addActionListener(this);
		tsAlignmentFilter = new JMenuItem("Filter out rare peaks", KeyEvent.VK_R); tsAlignmentFilter.addActionListener(this);
		tsEmptySlotFiller = new JMenuItem("Fill-in empty gaps", KeyEvent.VK_F); tsEmptySlotFiller.addActionListener(this);
		alignmentMenu.add(tsJoinAligner);
		alignmentMenu.add(tsFastAligner);
		alignmentMenu.addSeparator();
		alignmentMenu.add(tsAlignmentFilter);
		alignmentMenu.add(tsEmptySlotFiller);
		menuBar.add(alignmentMenu);

		normalizationMenu = new JMenu();normalizationMenu.setText("Normalization");normalizationMenu.setMnemonic(KeyEvent.VK_N);normalizationMenu.addActionListener(this);
		normLinear = new JMenuItem("Linear normalization", KeyEvent.VK_L);normLinear.addActionListener(this);
		normStdComp = new JMenuItem("Normalization using standards", KeyEvent.VK_N);normStdComp.addActionListener(this);
		normalizationMenu.add(normLinear);
		normalizationMenu.add(normStdComp);
		menuBar.add(normalizationMenu);

		batchMenu = new JMenu();batchMenu.setText("Batch mode");batchMenu.setMnemonic(KeyEvent.VK_B);batchMenu.addActionListener(this);
		batDefine = new JMenuItem("Define batch operations", KeyEvent.VK_R);batDefine.addActionListener(this);
		batchMenu.add(batDefine);
		menuBar.add(batchMenu);

		analysisMenu = new JMenu();analysisMenu.setText("Visualization");analysisMenu.setMnemonic(KeyEvent.VK_V);analysisMenu.addActionListener(this);
		anOpenSRView = new JMenuItem("Logratio plot", KeyEvent.VK_L);anOpenSRView.addActionListener(this);
		anOpenSCVView = new JMenuItem("Coefficient of variation plot", KeyEvent.VK_V);anOpenSCVView.addActionListener(this);
		anOpenCDAView = new JMenuItem("CDA plot of samples", KeyEvent.VK_C);anOpenCDAView.addActionListener(this); anOpenCDAView.setEnabled(false);
		anOpenSammonsView = new JMenuItem("Sammons plot of samples", KeyEvent.VK_S);anOpenSammonsView.addActionListener(this); anOpenSammonsView.setEnabled(false);
		analysisMenu.add(anOpenSRView);
		analysisMenu.add(anOpenSCVView);
		analysisMenu.add(anOpenCDAView);
		analysisMenu.add(anOpenSammonsView);
		menuBar.add(analysisMenu);

		toolsMenu = new JMenu();toolsMenu.setText("Configure");toolsMenu.setMnemonic(KeyEvent.VK_C);toolsMenu.addActionListener(this);
		toolsOptions = new JMenuItem("Preferences...", KeyEvent.VK_P);toolsOptions.addActionListener(this);
		toolsMenu.add(toolsOptions);
		menuBar.add(toolsMenu);

		windowMenu = new JMenu();windowMenu.setText("Window");windowMenu.setMnemonic(KeyEvent.VK_W);windowMenu.addActionListener(this);
		windowTileWindows = new JMenuItem("Tile Windows", KeyEvent.VK_T); windowTileWindows.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK)); windowTileWindows.addActionListener(this);//windowTileWindows.setEnabled(false);
		windowMenu.add(windowTileWindows);
		menuBar.add(windowMenu);

		helpMenu = new JMenu();helpMenu.setText("Help");helpMenu.setMnemonic(KeyEvent.VK_H);helpMenu.addActionListener(this);
		hlpAbout = new JMenuItem("About MZmine...", KeyEvent.VK_A); hlpAbout.addActionListener(this); hlpAbout.setEnabled(true);
		helpMenu.add(hlpAbout);
		menuBar.add(helpMenu);

		setJMenuBar(menuBar);


		// Place objects on main window
		desktop = new JDesktopPane();
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,itemSelector,desktop);

		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(split, BorderLayout.CENTER);
		c.add(statBar,BorderLayout.SOUTH);


		// Initialize window listener for responding to user events
		addWindowListener(this);



		updateMenuAvailability();

		pack();
		setBounds(0,0, 800, 600);
		setLocationRelativeTo(null);

		// Application wants to control closing by itself
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		setTitle(title);

		statBar.setStatusText("Welcome to MZmine!");


	}

	/**
	 * This method returns the desktop
	 */
	public JDesktopPane getDesktop() { return desktop; }

	/**
	 * This method is called to disable/enable GUI before/after doing some computational work
	 */
	public void setBusy(boolean flag) {
		busyFlag = flag;
	}

	/**
	 * This method returns true when all GUI actions should be disabled.
	 */
	public boolean isBusy() {
		return busyFlag;
	}

	/**
	 * This method moves all visualizers of given raw data to the top on desktop
	 */
	public void moveVisualizersToFront(RawDataAtClient rawData) {

		Integer rawDataID = new Integer(rawData.getRawDataID());

		// Get all visualizers for this raw data file
		Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(rawDataID);

		if (visualizers == null) { return; }

		// Move each visualizer window to front
		boolean somethingIsSelected = false;
		JInternalFrame jif = null;
		for (RawDataVisualizer vis : visualizers) {
			jif = (JInternalFrame)vis;
			if (jif.isSelected()) { somethingIsSelected = true; }
			jif.moveToFront();
		}


		if ( (!somethingIsSelected) && (visualizers!=null) ) {
 			try {
				jif = (JInternalFrame)(visualizers.get(0));
				jif.setSelected(true);
			} catch (Exception e) { }
		}


	}

	/**
	 * This method moves all visualizers of given alignment result to the top on desktop
	 */
	public void moveVisualizersToFront(AlignmentResult result) {

		Integer alignmentResultID = new Integer(result.getAlignmentResultID());

		// Get all visualizer for this alignment result
		Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers.get(alignmentResultID);

		if (visualizers==null) { return; }

		// Move each visualizer window to front
		for (AlignmentResultVisualizer vis : visualizers) {
			((JInternalFrame)vis).moveToFront();
		}

		// Select first visualizer window as the active window
		try { ((JInternalFrame)(visualizers.get(0))).setSelected(true);	} catch (Exception e) {}

	}



	/**
	 * Calculates number of raw data files with one or more visible visualizer windows
	 */
	private int numOfRawDataWithVisibleVisualizer(boolean includeIcons) {

		int num = 0;
		int[] allRawDataIDs = itemSelector.getRawDataIDs();

		for (int id : allRawDataIDs) {
			if (rawDataHasVisibleVisualizers(id, includeIcons)) { num++; }
		}


		return num;
	}

	/**
	 * Checks if a raw data file has one or more visible visualizers
	 * @param	rawDataID	raw data file ID
	 * @return	true if raw data file has one or more visible visualizers
	 */
	public boolean rawDataHasVisibleVisualizers(int rawDataID, boolean includeIcons) {
		boolean shouldBeShown = false;

		// Get visualizer vector for this raw data ID
		Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers.get(new Integer(rawDataID));

		// If raw data file has some visualizers, check if any of them is visible
		if (visualizerVector!=null) {
			Enumeration<RawDataVisualizer> visualizers = visualizerVector.elements();
			while (visualizers.hasMoreElements()) {

				RawDataVisualizer vis = visualizers.nextElement();
				JInternalFrame jif = (JInternalFrame)vis;

				if (includeIcons) {
					if 	(jif.isVisible()) { shouldBeShown = true; }
				} else {
					if	( jif.isVisible() && !(jif.isIcon()) ) { shouldBeShown = true; }
				}
			}
		}

		return shouldBeShown;

	}


	private int numOfResultsWithVisibleVisualizer(boolean includeIcons) {

		int num = 0;
		int[] alignmentResultIDs = itemSelector.getAlignmentResultIDs();

		for (int id : alignmentResultIDs) {
			if (alignmentHasVisibleVisualizers(id, includeIcons)) { num++; }
		}

		return num;
	}

	private boolean alignmentHasVisibleVisualizers(int alignmentResultID, boolean includeIcons) {

		// Get visualizer vector for this raw data ID
		Vector<AlignmentResultVisualizer> visualizerVector = alignmentResultVisualizers.get(new Integer(alignmentResultID));

		// If raw data file has some visualizers, check if any of them is visible
		if (visualizerVector!=null) {
			Enumeration<AlignmentResultVisualizer> visualizers = visualizerVector.elements();
			while (visualizers.hasMoreElements()) {
				AlignmentResultVisualizer vis = visualizers.nextElement();
				JInternalFrame jif = (JInternalFrame)vis;
				if (includeIcons) {
					if ( jif.isVisible() ) { return true; }
				} else {
					if ( jif.isVisible() && !(jif.isIcon()) ) { return true; }
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

		boolean shouldBeShown;

		double currentX;
		double currentY;
		JInternalFrame jif;

		// Calculate number of raw data files and alignment results with one or more open visualizer windows
		int numViewableRawDataFiles = numOfRawDataWithVisibleVisualizer(false);
		int numViewableResults = numOfResultsWithVisibleVisualizer(false);

		// Arrange visualizers for raw data files
		if (numViewableRawDataFiles>0) {

			// Determine space that is available for each run's visualizers on the main window
			if (numViewableResults==0) {
				widthForRawDataFiles = desktop.getWidth();
			} else {
				widthForRawDataFiles = goldenCut*desktop.getWidth();
			}

			heightForRawDataFile = desktop.getHeight() / numViewableRawDataFiles;

			currentX=0;
			currentY=0;

			// Arrange visualizers of each raw data file
			//Enumeration<Vector<RawDataVisualizer>> allVisualizerVectors = rawDataVisualizers.elements();
			//while (allVisualizerVectors.hasMoreElements()) {
			int rawDataIDs[] = getItemSelector().getRawDataIDs();
			for (int rawDataID : rawDataIDs) {

				RawDataAtClient rawData = getItemSelector().getRawDataByID(rawDataID);
				Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers.get(new Integer(rawDataID));

				// Get all visualizers for this raw data file
				//Vector<RawDataVisualizer> visualizerVector = allVisualizerVectors.nextElement();
				if (visualizerVector==null) { continue; }


				// Calculate how many of these visualizers should be shown
				int numToBeShown = 0;
				Enumeration<RawDataVisualizer> visualizers = visualizerVector.elements();
				while (visualizers.hasMoreElements()) {
					jif = (JInternalFrame)visualizers.nextElement();
					if ( jif.isVisible() && !(jif.isIcon()) ) { numToBeShown++; }
					//if (jif.isIcon() || !jif.isVisible()) {} else { numToBeShown++; }
				}

				// If there are visible visualizers, then arrange all of the visualizers for this raw data file
				if (numToBeShown>0) {

					if (!rawData.hasPeakData()) {
						// TIC
						jif = (JInternalFrame)visualizerVector.get(0);
						jif.setLocation((int)currentX, (int)currentY);
						jif.setSize((int)(goldenCut*widthForRawDataFiles), (int)(heightForRawDataFile/2.0));
						// Scan
						jif = (JInternalFrame)visualizerVector.get(1);
						jif.setLocation((int)currentX, (int)(currentY + 1 + heightForRawDataFile/2.0));
						jif.setSize((int)(1.0*widthForRawDataFiles), (int)(heightForRawDataFile/2.0));
						// 2D
						jif = (JInternalFrame)visualizerVector.get(2);
						jif.setLocation((int)(currentX+goldenCut*widthForRawDataFiles), (int)currentY);
						jif.setSize((int)((1-goldenCut)*widthForRawDataFiles), (int)(heightForRawDataFile/2.0));
					} else {

						// TIC
						jif = (JInternalFrame)visualizerVector.get(0);
						jif.setLocation((int)currentX, (int)currentY);
						jif.setSize((int)(goldenCut*widthForRawDataFiles), (int)(heightForRawDataFile/3.0));
						// Scan
						jif = (JInternalFrame)visualizerVector.get(1);
						jif.setLocation((int)currentX, (int)(currentY + 1 + heightForRawDataFile/3.0));
						jif.setSize((int)(goldenCut*widthForRawDataFiles), (int)(heightForRawDataFile/3.0));
						// 2D
						jif = (JInternalFrame)visualizerVector.get(2);
						jif.setLocation((int)(currentX+goldenCut*widthForRawDataFiles), (int)currentY);
						jif.setSize((int)((1-goldenCut)*widthForRawDataFiles), (int)(heightForRawDataFile));
						// Peaklist
						jif = (JInternalFrame)visualizerVector.get(3);
						jif.setLocation((int)currentX, (int)(currentY + 1 + heightForRawDataFile*2.0/3.0));
						jif.setSize((int)(goldenCut*widthForRawDataFiles), (int)(heightForRawDataFile/3.0));
					}

					currentY += heightForRawDataFile;

				} //shouldBeShown

			} // raw data files loop

		} // numViewableRawDataFiles>0

		if (numViewableResults>0) {

			currentY = 0;
			currentX = widthForRawDataFiles+1;

			// Determine space available for each results' visualizers on main window
			widthForAlignmentResults = desktop.getWidth() - widthForRawDataFiles;
			heightForAlignmentResult = desktop.getHeight() / numViewableResults;



			Enumeration<Vector<AlignmentResultVisualizer>> allVisualizerVectors = alignmentResultVisualizers.elements();
			while (allVisualizerVectors.hasMoreElements()) {

				Vector<AlignmentResultVisualizer> visualizerVector = allVisualizerVectors.nextElement();
				if (visualizerVector == null) { continue; }

				// Count how many of these visualizers should be shown
				int numToBeShown = 0;
				Enumeration<AlignmentResultVisualizer> visualizers = visualizerVector.elements();
				while (visualizers.hasMoreElements()) {
					jif = (JInternalFrame)visualizers.nextElement();
					if (jif.isIcon() || !jif.isVisible()) {} else { numToBeShown++; }
				}

				// If there are visible visualizers, then arrange all of the visualizers for this raw data file
				if (numToBeShown>0) {
					visualizers = visualizerVector.elements();
					while (visualizers.hasMoreElements()) {
						jif = (JInternalFrame)visualizers.nextElement();
						if (!jif.isIcon() && jif.isVisible()) {
							jif.setLocation((int)currentX, (int)currentY);
							jif.setSize((int)(widthForAlignmentResults), (int)(heightForAlignmentResult/(double)numToBeShown));
							currentY += heightForAlignmentResult/(double)numToBeShown;
						}
					}
				}
			}
		}


	} // Method


	/**
	 * This method creates all four visualizers for a run and adds them to main window desktop
	 */

	public void addRawDataVisualizers(RawDataAtClient rawData) {

		Vector<RawDataVisualizer> visualizers = new Vector<RawDataVisualizer>();


		RawDataVisualizerTICView ticView = new RawDataVisualizerTICView(this);
		ticView.setRawData(rawData);
		//theRun.addVisualizer(ticView);
		ticView.setFrameIcon(null);
		ticView.setVisible(false);
		desktop.add(ticView, javax.swing.JLayeredPane.DEFAULT_LAYER);

		visualizers.add(ticView);


		RawDataVisualizerSpectrumView spectrumView = new RawDataVisualizerSpectrumView(this);
		spectrumView.setRawData(rawData);
		//theRun.addVisualizer(scanView);
		spectrumView.setFrameIcon(null);
		spectrumView.setVisible(false);
		desktop.add(spectrumView, javax.swing.JLayeredPane.DEFAULT_LAYER);

		visualizers.add(spectrumView);


		RawDataVisualizerTwoDView twodView = new RawDataVisualizerTwoDView(this);
		twodView.setRawData(rawData);
		//theRun.addVisualizer(twodView);
		twodView.setFrameIcon(null);
		twodView.setVisible(false);
		desktop.add(twodView, javax.swing.JLayeredPane.DEFAULT_LAYER);

		visualizers.add(twodView);


		RawDataVisualizerPeakListView plView = new RawDataVisualizerPeakListView(this);
		plView.setRawData(rawData);
		plView.setSize(300,200);
		//theRun.addVisualizer(plView);
		plView.setFrameIcon(null);
		plView.setVisible(false);
		desktop.add(plView, javax.swing.JLayeredPane.DEFAULT_LAYER);

		visualizers.add(plView);

		// Add these visualizers to the hashtable
		rawDataVisualizers.put(new Integer(rawData.getRawDataID()), visualizers);

	}

	/**
	 * This method removes all visualizer for given raw data file
	 */
	public void removeRawDataVisualizers(int _rawDataID) {

		Integer rawDataID = new Integer(_rawDataID);

		// Get all visualizers for this raw data file
		Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(rawDataID);

		// Loop through all visualizer of this raw data file
		Enumeration<RawDataVisualizer> visualizersEnum = visualizers.elements();
		while (visualizersEnum.hasMoreElements()) {
			RawDataVisualizer vis = visualizersEnum.nextElement();

			// Remove visualizer window from the main window
			desktop.remove((JInternalFrame)vis);

		}

		// Remove all visualizers from vector
		visualizers.clear();

		// Remove entry from hashtable
		rawDataVisualizers.remove(rawDataID);
	}


	/**
	 * This method shows all available visualizer for all raw data files
	 */
	public void toggleRawDataVisualizers(int[] rawDataIDs, boolean show) {
		for (int i=0; i<rawDataIDs.length; i++) {
			toggleRawDataVisualizers(rawDataIDs[i], show);
		}
	}

	/**
	 * This method shows all available visualizers for a raw data file
	 */
	public void toggleRawDataVisualizers(int rawDataID, boolean show) {

		Integer rawDataIDI = new Integer( rawDataID );

		// Check if this raw data has peak data available
		boolean hasPeaks = itemSelector.getRawDataByID(rawDataID).hasPeakData();

		// Get all visualizers for this raw data file
		Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(rawDataIDI);

		// If this raw data file has any visualizers
		if (visualizers!=null) {
			// Loop through all visualizer of this raw data file
			Enumeration<RawDataVisualizer> visualizersEnum = visualizers.elements();
			while (visualizersEnum.hasMoreElements()) {
				RawDataVisualizer vis = visualizersEnum.nextElement();
				// If this is peak list visualizer
				if (vis instanceof RawDataVisualizerPeakListView) {
					// Then toggle visibility only, if the raw data has peaks
					if (hasPeaks) {
						if (show) {	((JInternalFrame)vis).show(); } else { ((JInternalFrame)vis).hide(); }
					}
				} else {
					// Otherwise just toggle the visualizer
					if (show) {	((JInternalFrame)vis).show(); } else { ((JInternalFrame)vis).hide(); }
				}
			}
		}
	}


	/**
	 * This method shows peak list if some other visualizer for a raw data file is already visible
	 * This method is used after peak picking to "add" peak lists to desktop, but only
	 * for those raw data files that are already being shown on the desktop.
	 */
	public void showRawDataPeakListVisualizer(int rawDataID) {

		// If this raw data file doesn't have any visible visualizers, then do not show peak list either
		if (!rawDataHasVisibleVisualizers(rawDataID, true)) { return; }

		Integer rawDataIDI = new Integer( rawDataID );

		// Check if this raw data has peak data available
		boolean hasPeaks = itemSelector.getRawDataByID(rawDataID).hasPeakData();
		if (!hasPeaks) { return; }

		// Get all visualizers for this raw data file
		Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(rawDataIDI);

		// If this raw data file has any visualizers
		if (visualizers!=null) {
			// Loop through all visualizer of this raw data file
			Enumeration<RawDataVisualizer> visualizersEnum = visualizers.elements();
			while (visualizersEnum.hasMoreElements()) {
				RawDataVisualizer vis = visualizersEnum.nextElement();
				// If this is peak list visualizer
				if (vis instanceof RawDataVisualizerPeakListView) {
					// Then toggle visibility only, if the raw data has peaks
					if (hasPeaks) {
						((JInternalFrame)vis).show();
						break;
					}
				}
			}
		}
	}


	/**
	 * This method refreshes all visualizer for listed raw data files
	 */
	public void startRefreshRawDataVisualizers(int changeType, int[] rawDataIDs) {

		// Loop through all raw data files
		Vector<RawDataVisualizerRefreshRequest> refreshRequestsV = new Vector<RawDataVisualizerRefreshRequest>();
		for (int i=0; i<rawDataIDs.length; i++) {

			// Ask refresh request for a raw data file
			RawDataVisualizerRefreshRequest refreshRequest = getRefreshRequestForRawData(changeType, rawDataIDs[i]);

			// If some visualizer need refresh, then add this request into vector
			if (	(refreshRequest.ticNeedsRawData == true) ||
					(refreshRequest.spectrumNeedsRawData == true) ||
					(refreshRequest.twodNeedsRawData == true) ) {
						refreshRequestsV.add(refreshRequest);
			}

		}

		// If some raw data file needed refresh
		if (refreshRequestsV.size()>0) {

			// Then ask client for cluster to initiate a refresh task on those raw data files
			RawDataVisualizerRefreshRequest[] refreshRequestsA = new RawDataVisualizerRefreshRequest[refreshRequestsV.size()];
			refreshRequestsA = refreshRequestsV.toArray(refreshRequestsA);
			clientForCluster.refreshVisualizers(refreshRequestsA);

		} else {
			// else call afterRefresh-method of every visualizer

			// Create empty refresh result
			RawDataVisualizerRefreshResult emptyRefreshResult = new RawDataVisualizerRefreshResult();
			emptyRefreshResult.changeType = changeType;

			// Call every involved raw data visualizer's afterRefresh method with this refreshResult
			// This is needed because visualizers still may need to do something although they didn't need raw data
			// (for example, they may need to take into account change in peak data)
			for (int rawDataID: rawDataIDs) {
				Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers.get(new Integer(rawDataID));
				Enumeration<RawDataVisualizer> visualizers = visualizerVector.elements();
				while (visualizers.hasMoreElements()) {
					RawDataVisualizer vis = visualizers.nextElement();
					vis.afterRefresh(emptyRefreshResult);
				}
			}

			// Repaint main window
			repaint();
		}

	}

	/**
	 * This method refreshes all visualizer for a raw data file
	 */
	public void startRefreshRawDataVisualizers(int changeType, int rawDataID) {


		// Ask refresh request for a raw data file
		RawDataVisualizerRefreshRequest refreshRequest = getRefreshRequestForRawData(changeType, rawDataID);

		// If some visualizer needs raw data in the refresh?
		if (	refreshRequest.ticNeedsRawData ||
				refreshRequest.spectrumNeedsRawData ||
				refreshRequest.twodNeedsRawData ||
				refreshRequest.peakListNeedsRawData ) {

			// Then ask client for cluster to initiate a refresh task on those raw data files
			RawDataVisualizerRefreshRequest[] refreshRequestsA = new RawDataVisualizerRefreshRequest[1];
			refreshRequestsA[0] = refreshRequest;
			clientForCluster.refreshVisualizers(refreshRequestsA);

		} else {
			// Create empty refresh result
			RawDataVisualizerRefreshResult emptyRefreshResult = new RawDataVisualizerRefreshResult();
			emptyRefreshResult.changeType = changeType;

			Vector<RawDataVisualizer> visualizerVector = rawDataVisualizers.get(new Integer(rawDataID));
			Enumeration<RawDataVisualizer> visualizers = visualizerVector.elements();
			while (visualizers.hasMoreElements()) {
				RawDataVisualizer vis = visualizers.nextElement();
				vis.afterRefresh(emptyRefreshResult);
			}

			// Repaint the main window
			repaint();
		}

	}


	/**
	 * This method queries visualizers of a single raw data file for their refresh request
	 * @param	changeType	Type of change that initiated refresh
	 * @param	rawDataID	Raw data ID
	 * @return	RefreshRequest that defines refresh needs of visualizers
	 */
	private RawDataVisualizerRefreshRequest getRefreshRequestForRawData(int changeType, int rawDataID) {



		Integer rawDataIDI = new Integer(rawDataID);

		// Get all visualizers for this raw data file
		Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(rawDataIDI);

		// Generate refresh request
		RawDataVisualizerRefreshRequest refreshRequest = new RawDataVisualizerRefreshRequest();
		refreshRequest.changeType = changeType;
		refreshRequest.rawDataID = rawDataID;

		if (getParameterStorage().getGeneralParameters().getTypeOfData() == GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CONTINUOUS) {
			refreshRequest.dataType = RawDataVisualizerRefreshRequest.MODE_CONTINUOUS;
		}

		if (getParameterStorage().getGeneralParameters().getTypeOfData() == GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CENTROIDS) {
			refreshRequest.dataType = RawDataVisualizerRefreshRequest.MODE_CENTROIDS;
		}


		// If no visualizers
		if (visualizers == null) { return refreshRequest; }

		// If no visible visualizers
		//if (!rawDataHasVisibleVisualizers(rawDataID)) { return refreshRequest; }

		// Loop through all visualizers for raw data file
		Enumeration<RawDataVisualizer> visualizerEnum = visualizers.elements();
		while (visualizerEnum.hasMoreElements()) {
			RawDataVisualizer vis = visualizerEnum.nextElement();
			refreshRequest = vis.beforeRefresh(refreshRequest);
		}

		return refreshRequest;

	}


	public void doRefreshRawDataVisualizers(RawDataVisualizerRefreshResult refreshResult) {

		// Find visualizers for this raw data file
		Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(new Integer(refreshResult.rawDataID));

		// Offer refreshResults to all visualizers that require it
		for (RawDataVisualizer vis : visualizers) {

			if (vis.getClass() == RawDataVisualizerTICView.class) {
				//if (refreshResult.ticScanNumbers != null) {
					vis.afterRefresh(refreshResult);
				//}
			}

			if (vis.getClass() == RawDataVisualizerSpectrumView.class) {
				//if (refreshResult.spectrumIntensities != null) {
					vis.afterRefresh(refreshResult);
				//}
			}

			if (vis.getClass() == RawDataVisualizerTwoDView.class) {
				//if (refreshResult.twodMatrix != null) {
					vis.afterRefresh(refreshResult);
				//}
			}

			if (vis.getClass() == RawDataVisualizerPeakListView.class) {
				//if (refreshResult.twodMatrix != null) {
					vis.afterRefresh(refreshResult);
				//}

			}
		}

		RawDataAtClient rawData = getItemSelector().getRawDataByID(refreshResult.rawDataID);

		rawData.setRawDataUpdatedFlag(false);

	}


	/**
	 * This method refreshes all visualizers for listed raw data files
	 * @param changeType	What was reason for need to refresh
	 * @param rawDataIDs	Array of raw data ids (raw data files whose visualizers need refreshing)
	 */
	 /*
	public void refreshRawDataVisualizers(int changeType, int[] rawDataIDs) {
		// Loop through all raw data files
		RawDataVisualizerRefreshRequest[] refreshRequests = new RawDataVisualizerRefreshRequest[rawDataIDs.length];
		for (int i=0; i<rawDataIDs.length; i++) {
			// Get all visualizers for this raw data file
			Vector<RawDataVisualizer> visualizers = rawDataVisualizers.get(new Integer(rawDataIDs[i]));

			// Generate refresh request
			RawDataVisualizerRefreshRequest refreshRequest = new RawDataVisualizerRefreshRequest();
			Enumeration<RawDataVisualizer> visualizerEnum = visualizers.elements();
			while (visualizerEnum.hasMoreElements()) {
				RawDataVisualizer vis = visualizerEnum.nextElement();
				vis.beforeRefresh(changeType, refreshRequest);
			}

			refreshRequests[i] = refreshRequest;
		}

		// Ask cluster client to do the refresh
		clientForCluster.refreshVisualizers(refreshRequests);
	}
*/

	public void addAlignmentResultVisualizerList(AlignmentResult alignmentResult) {

		Integer alignmentResultID = new Integer(alignmentResult.getAlignmentResultID());
		Vector<AlignmentResultVisualizer> visualizers;

		// If there is already a visualizer vector available for this alignment result
		if (alignmentResultVisualizers.containsKey(alignmentResultID)) {
			// Then use it
			visualizers = alignmentResultVisualizers.get(alignmentResultID);
		} else {
			// Else create it
			visualizers = new Vector<AlignmentResultVisualizer>();
		}

		// Create new visualizer, assign alignment result to it, place visualizer on desktop
		AlignmentResultVisualizerList listView = new AlignmentResultVisualizerList(this);
		listView.setAlignmentResult(alignmentResult);

		desktop.add(listView, javax.swing.JLayeredPane.DEFAULT_LAYER);

		listView.setFrameIcon(null);
		listView.setVisible(false);

		visualizers.add(listView);

		// Add these visualizers to the hashtable
		alignmentResultVisualizers.put(new Integer(alignmentResult.getAlignmentResultID()), visualizers);

	}

	public void addAlignmentResultVisualizerLogratioPlot(AlignmentResult alignmentResult) {

		// Create new visualizer
		AlignmentResultVisualizerLogratioPlotView logratioView = new AlignmentResultVisualizerLogratioPlotView(this);
		int retval = logratioView.askParameters(alignmentResult);

		if (retval==-1) {
			statBar.setStatusText("Logratio plot cancelled.");
			return;
		}

		Integer alignmentResultID = new Integer(alignmentResult.getAlignmentResultID());
		Vector<AlignmentResultVisualizer> visualizers;

		// If there is already a visualizer vector available for this alignment result
		if (alignmentResultVisualizers.containsKey(alignmentResultID)) {
			// Then use it
			visualizers = alignmentResultVisualizers.get(alignmentResultID);
		} else {
			// Else create it
			visualizers = new Vector<AlignmentResultVisualizer>();
		}

		// Assign alignment result to visualizer, place visualizer on desktop but not yet visible
		logratioView.setAlignmentResult(alignmentResult);
		desktop.add(logratioView, javax.swing.JLayeredPane.DEFAULT_LAYER);
		logratioView.setFrameIcon(null);
		logratioView.setVisible(true);

		visualizers.add(logratioView);

		// Add these visualizers to the hashtable
		alignmentResultVisualizers.put(new Integer(alignmentResult.getAlignmentResultID()), visualizers);

	}

	public void addAlignmentResultVisualizerCoVarPlot(AlignmentResult alignmentResult) {

		// Create new visualizer
		AlignmentResultVisualizerCoVarPlotView covarView = new AlignmentResultVisualizerCoVarPlotView(this);
		int retval = covarView.askParameters(alignmentResult);

		if (retval==-1) {
			statBar.setStatusText("Coefficient of variation plot cancelled.");
			return;
		}

		Integer alignmentResultID = new Integer(alignmentResult.getAlignmentResultID());
		Vector<AlignmentResultVisualizer> visualizers;

		// If there is already a visualizer vector available for this alignment result
		if (alignmentResultVisualizers.containsKey(alignmentResultID)) {
			// Then use it
			visualizers = alignmentResultVisualizers.get(alignmentResultID);
		} else {
			// Else create it
			visualizers = new Vector<AlignmentResultVisualizer>();
		}

		// Assign alignment result to visualizer, place visualizer on desktop but not yet visible
		covarView.setAlignmentResult(alignmentResult);
		desktop.add(covarView, javax.swing.JLayeredPane.DEFAULT_LAYER);
		covarView.setFrameIcon(null);
		covarView.setVisible(true);

		visualizers.add(covarView);

		// Add these visualizers to the hashtable
		alignmentResultVisualizers.put(new Integer(alignmentResult.getAlignmentResultID()), visualizers);

	}

	public void addAlignmentResultVisualizerCDAPlot(AlignmentResult alignmentResult) {

		// Create new visualizer
		AlignmentResultVisualizerCDAPlotView cdaView = new AlignmentResultVisualizerCDAPlotView(this);
		AlignmentResultVisualizerCDAPlotViewParameters params = cdaView.askParameters(alignmentResult, parameterStorage.getAlignmentResultVisualizerCDAPlotViewParameters());

		if (params==null) {
			statBar.setStatusText("CDA plot cancelled.");
			return;
		}

		parameterStorage.setAlignmentResultVisualizerCDAPlotViewParameters(params);

		Integer alignmentResultID = new Integer(alignmentResult.getAlignmentResultID());
		Vector<AlignmentResultVisualizer> visualizers;

		// If there is already a visualizer vector available for this alignment result
		if (alignmentResultVisualizers.containsKey(alignmentResultID)) {
			// Then use it
			visualizers = alignmentResultVisualizers.get(alignmentResultID);
		} else {
			// Else create it
			visualizers = new Vector<AlignmentResultVisualizer>();
		}

		// Assign alignment result to visualizer, place visualizer on desktop but not yet visible
		cdaView.setAlignmentResult(alignmentResult);
		desktop.add(cdaView, javax.swing.JLayeredPane.DEFAULT_LAYER);
		cdaView.setFrameIcon(null);
		cdaView.setVisible(true);

		visualizers.add(cdaView);

		// Add these visualizers to the hashtable
		alignmentResultVisualizers.put(new Integer(alignmentResult.getAlignmentResultID()), visualizers);

	}


	public void addAlignmentResultVisualizerSammonsPlot(AlignmentResult alignmentResult) {


		AlignmentResultVisualizerSammonsPlotView sammonsView = new AlignmentResultVisualizerSammonsPlotView(this);
		AlignmentResultVisualizerSammonsPlotViewParameters params = sammonsView.askParameters(alignmentResult, parameterStorage.getAlignmentResultVisualizerSammonsPlotViewParameters());

		if (params==null) {
			statBar.setStatusText("Sammons plot cancelled.");
			return;
		}

		parameterStorage.setAlignmentResultVisualizerSammonsPlotViewParameters(params);

		Integer alignmentResultID = new Integer(alignmentResult.getAlignmentResultID());
		Vector<AlignmentResultVisualizer> visualizers;

		// If there is already a visualizer vector available for this alignment result
		if (alignmentResultVisualizers.containsKey(alignmentResultID)) {
			// Then use it
			visualizers = alignmentResultVisualizers.get(alignmentResultID);
		} else {
			// Else create it
			visualizers = new Vector<AlignmentResultVisualizer>();
		}

		// Assign alignment result to visualizer, place visualizer on desktop but not yet visible
		sammonsView.setAlignmentResult(alignmentResult);
		desktop.add(sammonsView, javax.swing.JLayeredPane.DEFAULT_LAYER);
		sammonsView.setFrameIcon(null);
		sammonsView.setVisible(true);

		visualizers.add(sammonsView);

		// Add these visualizers to the hashtable
		alignmentResultVisualizers.put(new Integer(alignmentResult.getAlignmentResultID()), visualizers);

	}


	/**
	 * This method removes all visualizer for given raw data file
	 */
	public void removeAlignmentResultVisualizers(int _alignmentResultID) {

		Integer alignmentResultID = new Integer(_alignmentResultID);

		// Get all visualizers for this alignment result
		Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers.get(alignmentResultID);

		// Loop through all visualizer of this alignment result
		Enumeration<AlignmentResultVisualizer> visualizersEnum = visualizers.elements();
		while (visualizersEnum.hasMoreElements()) {
			AlignmentResultVisualizer vis = visualizersEnum.nextElement();

			// Remove visualizer window from the main window
			desktop.remove((JInternalFrame)vis);

		}

		// Remove all visualizers from vector
		visualizers.clear();

		// Remove entry from hashtable
		alignmentResultVisualizers.remove(alignmentResultID);
	}


	public void toggleAlignmentResultVisualizers(int[] alignmentResultIDs, boolean show) {
		for (int alignmentResultID : alignmentResultIDs) {
			toggleAlignmentResultVisualizer(alignmentResultID, show);
		}
	}

	public void toggleAlignmentResultVisualizer(int alignmentResultID, boolean show) {

		Integer alignmentResultIDI = new Integer(alignmentResultID);

		// Get all visualizers for this alignment result
		Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers.get(alignmentResultIDI);

		// If this raw data file has any visualizers
		if (visualizers!=null) {
			// Loop through all visualizer of this raw data file
			Enumeration<AlignmentResultVisualizer> visualizersEnum = visualizers.elements();
			while (visualizersEnum.hasMoreElements()) {
				AlignmentResultVisualizer vis = visualizersEnum.nextElement();
				if (show) {	((JInternalFrame)vis).show(); } else { ((JInternalFrame)vis).hide(); }

			}
		}
	}

	public void refreshAlignmentVisualizers(int alignmentResultID, int changeType) {

		Integer alignmentResultIDI = new Integer(alignmentResultID);

		// Get all visualizers for this alignment result
		Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers.get(alignmentResultIDI);

		// If this raw data file has any visualizers
		if (visualizers!=null) {
			// Loop through all visualizer of this raw data file
			Enumeration<AlignmentResultVisualizer> visualizersEnum = visualizers.elements();
			while (visualizersEnum.hasMoreElements()) {
				AlignmentResultVisualizer vis = visualizersEnum.nextElement();
				vis.refreshVisualizer(changeType);
			}

		}

	}

	/**
	 * This method updates all alignment result visualizers after selected row has changed in the alignment result
	 */
	public void updateAlignmentResultVisualizers(int alignmentResultID) {
		Integer alignmentResultIDI = new Integer(alignmentResultID);

		// Get all visualizers for this alignment result
		Vector<AlignmentResultVisualizer> visualizers = alignmentResultVisualizers.get(alignmentResultIDI);

		// If this alignment result has any visualizers
		if (visualizers!=null) {
			// Loop through all visualizer of this alignment result
			Enumeration<AlignmentResultVisualizer> visualizersEnum = visualizers.elements();
			while (visualizersEnum.hasMoreElements()) {
				AlignmentResultVisualizer vis = visualizersEnum.nextElement();
				vis.updateSelectedRow();
			}
		}
	}


	/**
	 * WindowListener interface implementation
	 */
	public void windowOpened(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		exitMZmine();
	}
	public void windowClosed(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}

	/**
	 * ActionListener interface implementation
	 */
	public void actionPerformed(ActionEvent e) {

		if (isBusy()) { return; }

		Object src = e.getSource();
		
		// File -> Open
		if (src == fileOpen) {

			statBar.setStatusText("Please select a data file to open");

 
			// Open file dialog
			JFileChooser fileOpenChooser = new JFileChooser();
			fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileOpenChooser.setMultiSelectionEnabled(true);
			fileOpenChooser.setDialogTitle("Please select data files to open");
			
             /*if (dataDirectory == null) {
				dataDirectory = clientForCluster.getDataRootPath();
			}
            */
//			fileOpenChooser.setCurrentDirectory(new File(dataDirectory));

			ExampleFileFilter filter = new ExampleFileFilter();
			filter.addExtension("CDF");
			filter.addExtension("nc");
			filter.addExtension("XML");
			filter.addExtension("mzXML");
			filter.setDescription("Raw data files");

			fileOpenChooser.setFileFilter(filter);
			int retval = fileOpenChooser.showOpenDialog(this);

			// If ok to go on with file open
			if(retval == JFileChooser.APPROVE_OPTION) {

				File[] selectedFiles = fileOpenChooser.getSelectedFiles();

                 IOController.getInstance().openFiles(selectedFiles, PreloadLevel.NO_PRELOAD);
                 /* String[] dataFilePaths = new String[selectedFiles.length];


				for (int i=0; i<selectedFiles.length; i++) {
					File f = selectedFiles[i];

					if (f.exists()) {
						dataFilePaths[i] = f.getPath();

						// Update dataDirectory
						String tmppath = f.getPath().substring(0, f.getPath().length()-f.getName().length());
						dataDirectory = new String(tmppath);

					} else {
						displayErrorMessage("File " + f + " does not exist.");
						return;
					}
				}

				clientForCluster.openRawDataFiles(dataFilePaths);
                 */

			} else {
				statBar.setStatusText("File open cancelled.");
			} 

		}

		// File->Close
		if (src == fileClose) {

			// Grab selected raw data files
			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();

			closeRawDataFiles(rawDataIDs);

			int[] alignmentResultIDs = itemSelector.getSelectedAlignmentResultIDs();

			closeAlignmentResults(alignmentResultIDs);

		}

		// File->Export table
		if (src == fileExportPeakList) {

			RawDataAtClient[] rawDatas = itemSelector.getSelectedRawDatas();

			for (RawDataAtClient r : rawDatas) {

				statBar.setStatusText("Writing peak list for file " + r.getNiceName());

				if (!r.hasPeakData()) {
					// No peak list available for active run
					try {
					JOptionPane.showInternalMessageDialog(
													getDesktop(),
													"No peak data available for " + r.getNiceName() + ". Please run a peak picker first.",
													"Sorry",
													JOptionPane.ERROR_MESSAGE
												 );
					} catch (Exception exce ) {}

					statBar.setStatusText("Peak list export failed.");

				} else {

					// Generate name for the peak list file
					StringTokenizer st = new StringTokenizer(r.getNiceName(),".");
					String peakListName="";
					String peakListPath="";
					String toke = "";
					while (st.hasMoreTokens()) {
						toke = st.nextToken();
						if (st.hasMoreTokens()) { peakListName += toke; }
					}
					peakListName += "_MZminePeakList" + "." + "txt";


					// Save file dialog
					JFileChooser fileSaveChooser = new JFileChooser();
					fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
					fileSaveChooser.setMultiSelectionEnabled(false);
					fileSaveChooser.setDialogTitle("Please give file name for peak list of  " + r.getNiceName());
					statBar.setStatusText("Please give file name for peak list of  " + r.getNiceName());
					if (dataDirectory!=null) {
						fileSaveChooser.setCurrentDirectory(new File(dataDirectory));
					}

					fileSaveChooser.setSelectedFile(new File(peakListName));

					ExampleFileFilter filter = new ExampleFileFilter();
					filter.addExtension("txt");
					filter.setDescription("Peak list as tab-delimitted text file");

					fileSaveChooser.setFileFilter(filter);
					int retval = fileSaveChooser.showSaveDialog(this);

					if(retval == JFileChooser.APPROVE_OPTION) {
						File selectedFile = fileSaveChooser.getSelectedFile();

						String tmpfullpath = selectedFile.getPath();
						String datafilename = selectedFile.getName();
						String datafilepath = tmpfullpath.substring(0, tmpfullpath.length()-datafilename.length());
						dataDirectory = new String(datafilepath);

						peakListName = tmpfullpath;

						if (PeakListExporter.writePeakListToFile(r, peakListName)) {
							statBar.setStatusText("Peak list export done.");
						} else {
							displayErrorMessage("Failed to write peak list for raw data " + r.getNiceName());
							statBar.setStatusText("Peak list export failed.");
						}



					} else {
						statBar.setStatusText("Peak list export cancelled.");
					}

				}

			}


			Vector<AlignmentResult> results = itemSelector.getSelectedAlignmentResults();

			if (results.size()>0) {
				AlignmentResultExporterParameters areParams = getParameterStorage().getAlignmentResultExporterParameters();
				GeneralParameters genParams = getParameterStorage().getGeneralParameters();
				AlignmentResultExporterParameterSetupDialog arepsd = new AlignmentResultExporterParameterSetupDialog(genParams, areParams);
				statBar.setStatusText("Please select columns for alignment result exporting.");
				arepsd.showModal(getDesktop());

				if (arepsd.getExitCode()==-1) {
					statBar.setStatusText("Alignment result export cancelled.");
					return;
				}

				parameterStorage.setAlignmentResultExporterParameters(areParams);



				for (int i=0; i<results.size(); i++) {
					AlignmentResult result = results.get(i);

					statBar.setStatusText("Writing alignment result " + result.getNiceName() + " to file.");

					String resultName = result.getNiceName() + ".txt";

					// Save file dialog
					JFileChooser fileSaveChooser = new JFileChooser();
					fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
					fileSaveChooser.setMultiSelectionEnabled(false);
					fileSaveChooser.setDialogTitle("Please give file name for alignment result " + result.getNiceName());
					if (dataDirectory!=null) {
						fileSaveChooser.setCurrentDirectory(new File(dataDirectory));
					}

					fileSaveChooser.setSelectedFile(new File(resultName));

					ExampleFileFilter filter = new ExampleFileFilter();
					filter.addExtension("txt");
					filter.setDescription("Alignment results as tab-delimitted text file");

					fileSaveChooser.setFileFilter(filter);
					int retval = fileSaveChooser.showSaveDialog(this);

					if(retval == JFileChooser.APPROVE_OPTION) {
						File selectedFile = fileSaveChooser.getSelectedFile();

						String tmpfullpath = selectedFile.getPath();
						String datafilename = selectedFile.getName();
						String datafilepath = tmpfullpath.substring(0, tmpfullpath.length()-datafilename.length());
						dataDirectory = new String(datafilepath);

						resultName = datafilepath + datafilename;


						//result.writeResultsToFile(resultName, areParams, this);
						AlignmentResultExporter.exportAlignmentResultToFile(result, resultName, areParams, this);
						statBar.setStatusText("Alignment result export done.");
					} else {
						statBar.setStatusText("Alignment result export cancelled.");
					}
				}

			}




/*
			return;
*/

		}

		if (src == fileLoadParameters) {
			statBar.setStatusText("Please select a parameter file");

			// Open file dialog
			JFileChooser fileOpenChooser = new JFileChooser();
			fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileOpenChooser.setMultiSelectionEnabled(false);
			fileOpenChooser.setDialogTitle("Please select parameter file");
			fileOpenChooser.setCurrentDirectory(new File(clientForCluster.getDataRootPath()));

			ExampleFileFilter filter = new ExampleFileFilter();
			filter.addExtension("XML");
			filter.setDescription("MZmine parameters file");

			fileOpenChooser.setFileFilter(filter);
			int retval = fileOpenChooser.showOpenDialog(this);

			// If ok to go on with file open
			if(retval == JFileChooser.APPROVE_OPTION) {

				File selectedFile = fileOpenChooser.getSelectedFile();
				if (!(selectedFile.exists())) {
					displayErrorMessage("Parameter file " + selectedFile + " does not exist!");
					statBar.setStatusText("Parameter file loading failed.");
					return;
				}
				if (getParameterStorage().readParametesFromFile(selectedFile)) {
					statBar.setStatusText("Parameter file loading done.");
				} else {
					displayErrorMessage("Parameter file " + selectedFile + " loading failed!");
					statBar.setStatusText("Parameter file loading failed.");
				}

			} else {
				statBar.setStatusText("Parameter file loading cancelled.");
			}

		}


		if (src == fileSaveParameters) {

					// Save file dialog
					JFileChooser fileSaveChooser = new JFileChooser();
					fileSaveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
					fileSaveChooser.setMultiSelectionEnabled(false);
					fileSaveChooser.setDialogTitle("Please give file name for parameters file.");

					ExampleFileFilter filter = new ExampleFileFilter();
					filter.addExtension("xml");
					filter.setDescription("MZmine parameters file");

					fileSaveChooser.setFileFilter(filter);
					int retval = fileSaveChooser.showSaveDialog(this);

					if(retval == JFileChooser.APPROVE_OPTION) {
						File selectedFile = fileSaveChooser.getSelectedFile();

						// Ask for overwrite?
						if (selectedFile.exists()) {
						}

						// Write parameters
						getParameterStorage().writeParametersToFile(selectedFile);


						statBar.setStatusText("Parameters written to file.");
					} else {
						statBar.setStatusText("Parameter saving cancelled.");
					}

		}


		if (src == fileImportAlignmentResult) {

			statBar.setStatusText("Please select a result file to import");
			// Open file dialog
			JFileChooser fileOpenChooser = new JFileChooser();
			fileOpenChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fileOpenChooser.setMultiSelectionEnabled(false);
			fileOpenChooser.setDialogTitle("Please select alignment result file to open");
			if (dataDirectory!=null) {
				fileOpenChooser.setCurrentDirectory(new File(dataDirectory));
			 }

			ExampleFileFilter filter = new ExampleFileFilter();
			filter.addExtension("txt");
			filter.setDescription("Tab-delimitted text files");
 			fileOpenChooser.addChoosableFileFilter(filter);

			int retval = fileOpenChooser.showOpenDialog(this);

			// If ok to go on with file open
			if(retval == JFileChooser.APPROVE_OPTION) {

				File f = fileOpenChooser.getSelectedFile();
				String fpath = f.getPath();
				String fname = f.getName();

				String tmpfullpath = f.getPath();
				String datafilename = f.getName();
				String datafilepath = tmpfullpath.substring(0, tmpfullpath.length()-datafilename.length());
				dataDirectory = new String(datafilepath);

				// Create new alignment result
				AlignmentResult ar = AlignmentResultExporter.importAlignmentResultFromFile(datafilepath, datafilename);

				if (ar == null) {
					displayErrorMessage("Could not import alignment result from file " + datafilename + "\n" + "(Maybe it was not exported in Wide format?)");
					return;
				}

				itemSelector.addAlignmentResult(ar);
				addAlignmentResultVisualizerList(ar);

				tileWindows();

				statBar.setStatusText("Result file imported.");
			} else {
				statBar.setStatusText("Result import cancelled");
			}

		}

		if (src == filePrint) {

			JInternalFrame activeWindow = desktop.getSelectedFrame();
			if (activeWindow!=null) {
				if ((activeWindow.getClass() == RawDataVisualizerTICView.class) ||
					(activeWindow.getClass() == RawDataVisualizerTwoDView.class) ||
					(activeWindow.getClass() == RawDataVisualizerSpectrumView.class) ) {

						((RawDataVisualizer)activeWindow).printMe();
				}

				if (activeWindow!=null) {
					if ((activeWindow.getClass() == AlignmentResultVisualizerLogratioPlotView.class) ||
						(activeWindow.getClass() == AlignmentResultVisualizerCoVarPlotView.class) ||
						(activeWindow.getClass() == AlignmentResultVisualizerCDAPlotView.class)
						) {
							((AlignmentResultVisualizer)activeWindow).printMe();
					}
				}
			}
		}

		if (src == editCopy) {
			JInternalFrame activeWindow = desktop.getSelectedFrame();
			if (activeWindow!=null) {
				if ((activeWindow.getClass() == RawDataVisualizerTICView.class) ||
					(activeWindow.getClass() == RawDataVisualizerTwoDView.class) ||
					(activeWindow.getClass() == RawDataVisualizerSpectrumView.class) ) {

						((RawDataVisualizer)activeWindow).copyMe();
				}

				if (activeWindow!=null) {
					if ((activeWindow.getClass() == AlignmentResultVisualizerLogratioPlotView.class) ||
						(activeWindow.getClass() == AlignmentResultVisualizerCoVarPlotView.class) ||
						(activeWindow.getClass() == AlignmentResultVisualizerCDAPlotView.class) ) {
							((AlignmentResultVisualizer)activeWindow).copyMe();
					}
				}
			}
		}

		// File -> Exit
		if (src == fileExit) {

			statBar.setStatusText("Exiting.");
			exitMZmine();

		}

		if (src == toolsOptions) {

			new OptionsWindow(this);

		}

		if (src == ssMeanFilter) {
			// Ask parameters from user
			MeanFilter mf = new MeanFilter();
			MeanFilterParameters mfParam = mf.askParameters(this, parameterStorage.getMeanFilterParameters());
			if (mfParam==null) {
				statBar.setStatusText("Mean filtering cancelled.");
				return;
			}
			parameterStorage.setMeanFilterParameters(mfParam);

			// It seems user didn't cancel
			statBar.setStatusText("Mean filtering spectra.");
			paintNow();

			// Collect raw data IDs and initiate filtering on the cluster
			int[] selectedRawDataIDs = itemSelector.getSelectedRawDataIDs();
			clientForCluster.filterRawDataFiles(selectedRawDataIDs, mfParam);

		}

		if (src == ssSGFilter) {

			// Ask parameters from user
			SavitzkyGolayFilter sf = new SavitzkyGolayFilter();
			SavitzkyGolayFilterParameters sfParam = sf.askParameters(this, parameterStorage.getSavitzkyGolayFilterParameters());
			if (sfParam==null) {
				statBar.setStatusText("Savitzky-Golay filtering cancelled.");
				return;
			}
			parameterStorage.setSavitzkyGolayFilterParameters(sfParam);

			// It seems user didn't cancel
			statBar.setStatusText("Savitzky-Golay filtering spectra.");
			paintNow();

			// Collect raw data IDs and initiate filtering on the cluster
			int[] selectedRawDataIDs = itemSelector.getSelectedRawDataIDs();
			clientForCluster.filterRawDataFiles(selectedRawDataIDs, sfParam);

		}

		if (src == ssChromatographicMedianFilter) {
			// Ask parameters from user
			ChromatographicMedianFilter cmf = new ChromatographicMedianFilter();
			ChromatographicMedianFilterParameters cmfParam = cmf.askParameters(this, parameterStorage.getChromatographicMedianFilterParameters());
			if (cmfParam==null) {
				statBar.setStatusText("Chromatographic median filtering cancelled.");
				return;
			}
			parameterStorage.setChromatographicMedianFilterParameters(cmfParam);

			// It seems user didn't cancel
			statBar.setStatusText("Filtering with chromatographic median filter.");
			paintNow();

			// Collect raw data IDs and initiate filtering on the cluster
			int[] selectedRawDataIDs = itemSelector.getSelectedRawDataIDs();
			clientForCluster.filterRawDataFiles(selectedRawDataIDs, cmfParam);

		}

		if (src == ssCropFilter) {

			// Ask parameters from user
			CropFilter cf = new CropFilter();
			CropFilterParameters cfParam = cf.askParameters(this, parameterStorage.getCropFilterParameters());
			if (cfParam==null) {
				statBar.setStatusText("Crop filtering cancelled.");
				return;
			}
			parameterStorage.setCropFilterParameters(cfParam);

			// It seems user didn't cancel
			statBar.setStatusText("Filtering with cropping filter.");
			paintNow();

			// Collect raw data IDs and initiate filtering on the cluster
			int[] selectedRawDataIDs = itemSelector.getSelectedRawDataIDs();
			clientForCluster.filterRawDataFiles(selectedRawDataIDs, cfParam);


		}

		if (src == ssZoomScanFilter) {

			// Ask parameters from user
			ZoomScanFilter zsf = new ZoomScanFilter();
			ZoomScanFilterParameters zsfParam = zsf.askParameters(this, parameterStorage.getZoomScanFilterParameters());
			if (zsfParam==null) {
				statBar.setStatusText("Zoom scan filtering cancelled.");
				return;
			}
			parameterStorage.setZoomScanFilterParameters(zsfParam);

			// It seems user didn't cancel
			statBar.setStatusText("Filtering with zoom scan filter.");
			paintNow();

			// Collect raw data IDs and initiate filtering on the cluster
			int[] selectedRawDataIDs = itemSelector.getSelectedRawDataIDs();
			clientForCluster.filterRawDataFiles(selectedRawDataIDs, zsfParam);


		}

		if (src == ssRecursiveThresholdPicker) {

			RecursiveThresholdPicker rp = new RecursiveThresholdPicker();

			RecursiveThresholdPickerParameters rpParam = rp.askParameters(this, getParameterStorage().getRecursiveThresholdPickerParameters());
			if (rpParam == null) {
				statBar.setStatusText("Peak picking cancelled.");
				return;
			}
			getParameterStorage().setRecursiveThresholdPickerParameters(rpParam);

			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();

			statBar.setStatusText("Searching for peaks.");
			paintNow();

			// Call cluster controller
			clientForCluster.findPeaks(rawDataIDs, rpParam);

			rp = null;
			rpParam = null;

		}

		if (src == ssLocalPicker) {

			LocalPicker lp = new LocalPicker();

			LocalPickerParameters lpParam = lp.askParameters(this, getParameterStorage().getLocalPickerParameters());
			if (lpParam == null) {
				statBar.setStatusText("Peak picking cancelled.");
				return;
			}
			getParameterStorage().setLocalPickerParameters(lpParam);

			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();

			statBar.setStatusText("Searching for peaks.");
			paintNow();

			// Call cluster controller
			clientForCluster.findPeaks(rawDataIDs, lpParam);

			lp = null;
			lpParam = null;

		}

		if (src == ssCentroidPicker) {

			CentroidPicker cp = new CentroidPicker();

			CentroidPickerParameters cpParam = cp.askParameters(this, getParameterStorage().getCentroidPickerParameters());
			if (cpParam == null) {
				statBar.setStatusText("Peak picking cancelled.");
				return;
			}
			getParameterStorage().setCentroidPickerParameters(cpParam);

			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();

			statBar.setStatusText("Searching for peaks.");
			paintNow();

			// Call cluster controller to start peak picking process
			clientForCluster.findPeaks(rawDataIDs, cpParam);

			cp = null;
			cpParam = null;
		}

		if (src == ssSimpleDeisotoping) {

			SimpleDeisotoper sd = new SimpleDeisotoper();

			SimpleDeisotoperParameters sdParam = sd.askParameters(this, getParameterStorage().getSimpleDeisotoperParameters());
			if (sdParam == null) {
				statBar.setStatusText("Deisotoping cancelled.");
				return;
			}
			getParameterStorage().setSimpleDeisotoperParameters(sdParam);

			Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer, PeakList>();
			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
			for (int i=0; i<rawDataIDs.length; i++) {
				peakLists.put(	new Integer(rawDataIDs[i]),
								itemSelector.getRawDataByID(rawDataIDs[i]).getPeakList() );
			}

			statBar.setStatusText("Deisotoping peak lists.");
			paintNow();

			// Call cluster controller to start peak picking process
			clientForCluster.processPeakLists(peakLists, sdParam);

			sd = null;
			sdParam = null;

		}

		if (src == ssCombinatorialDeisotoping) {

			CombinatorialDeisotoper cd = new CombinatorialDeisotoper();

			CombinatorialDeisotoperParameters cdParam = cd.askParameters(this, getParameterStorage().getCombinatorialDeisotoperParameters());
			if (cdParam == null) {
				statBar.setStatusText("Deisotoping cancelled.");
				return;
			}
			getParameterStorage().setCombinatorialDeisotoperParameters(cdParam);

			Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer, PeakList>();
			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
			for (int i=0; i<rawDataIDs.length; i++) {
				peakLists.put(	new Integer(rawDataIDs[i]),
								itemSelector.getRawDataByID(rawDataIDs[i]).getPeakList() );
			}

			statBar.setStatusText("Deisotoping peak lists.");
			paintNow();

			// Call cluster controller to start peak picking process
			clientForCluster.processPeakLists(peakLists, cdParam);

			cd = null;
			cdParam = null;

		}



		if (src == ssIncompleteIsotopePatternFilter) {

			IncompleteIsotopePatternFilter iif = new IncompleteIsotopePatternFilter();

			IncompleteIsotopePatternFilterParameters iifParam = iif.askParameters(this, getParameterStorage().getIncompleteIsotopePatternFilterParameters());
			if (iifParam == null) {
				statBar.setStatusText("Peak list filtering cancelled.");
				return;
			}
			getParameterStorage().setIncompleteIsotopePatternFilterParameters(iifParam);

			Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer, PeakList>();
			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
			for (int i=0; i<rawDataIDs.length; i++) {
				peakLists.put(	new Integer(rawDataIDs[i]),
				itemSelector.getRawDataByID(rawDataIDs[i]).getPeakList() );
			}

			statBar.setStatusText("Filtering peak lists.");
			paintNow();

			// Call cluster controller to start peak picking process
			clientForCluster.processPeakLists(peakLists, iifParam);


			iif = null;
			iifParam = null;

		}

		if (src == tsJoinAligner) {

			// Make sure that every selected raw data file has a peak list
			Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer, PeakList>();

			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
			for (int rawDataID : rawDataIDs) {
				RawDataAtClient rawData = itemSelector.getRawDataByID(rawDataID);
				if (!(rawData.hasPeakData())) {
					displayErrorMessage("Can't align: " + rawData.getNiceName() + " has no peak list available.");
					peakLists = null;
					return;
				}
				peakLists.put(new Integer(rawDataID), rawData.getPeakList());

			}

			// Show user parameter setup dialog
			JoinAligner ja = new JoinAligner();
			JoinAlignerParameters jaParam = ja.askParameters(this, getParameterStorage().getJoinAlignerParameters());
			if (jaParam==null) {
				statBar.setStatusText("Alignment cancelled.");
				return;
			}
			getParameterStorage().setJoinAlignerParameters(jaParam);

			statBar.setStatusText("Aligning peak lists.");
			paintNow();

			// Call cluster controller
			clientForCluster.doAlignment(peakLists, jaParam);

			ja = null;
			jaParam = null;

		}

		if (src == tsFastAligner) {

			// Make sure that every selected raw data file has a peak list
			Hashtable<Integer, PeakList> peakLists = new Hashtable<Integer, PeakList>();

			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();
			for (int rawDataID : rawDataIDs) {
				RawDataAtClient rawData = itemSelector.getRawDataByID(rawDataID);
				if (!(rawData.hasPeakData())) {
					displayErrorMessage("Can't align: " + rawData.getNiceName() + " has no peak list available.");
					peakLists = null;
					return;
				}
				peakLists.put(new Integer(rawDataID), rawData.getPeakList());

			}

			// Show user parameter setup dialog
			FastAligner fa = new FastAligner();
			FastAlignerParameters faParam = fa.askParameters(this, getParameterStorage().getFastAlignerParameters());
			if (faParam==null) {
				statBar.setStatusText("Alignment cancelled.");
				return;
			}
			getParameterStorage().setFastAlignerParameters(faParam);

			statBar.setStatusText("Aligning peak lists.");
			paintNow();

			// Call cluster controller
			clientForCluster.doAlignment(peakLists, faParam);

			fa = null;
			faParam = null;

		}

		if (src == normLinear) {

			LinearNormalizer ln = new LinearNormalizer();

			LinearNormalizerParameters lnp = ln.askParameters(this, getParameterStorage().getLinearNormalizerParameters());
			if (lnp == null) {
				statBar.setStatusText("Normalization cancelled.");
				return;
			}
			getParameterStorage().setLinearNormalizerParameters(lnp);

			// If normalization by total raw signal, then must use controller to calc total raw signals and finally do normalization
			if (lnp.paramNormalizationType == LinearNormalizerParameters.NORMALIZATIONTYPE_TOTRAWSIGNAL) {

				// Collect raw data IDs from all selected alignment results
				Vector<AlignmentResult> selectedAlignmentResults = itemSelector.getSelectedAlignmentResults();
				Vector<Integer> allRequiredRawDataIDs = new Vector<Integer>();
				// Loop all selected alignment results
				for (AlignmentResult ar :selectedAlignmentResults) {

					// Loop all raw data IDs in current alignment results
					int[] tmpRawDataIDs = ar.getRawDataIDs();
					for (int tmpRawDataID : tmpRawDataIDs) {
						Integer tmpRawDataIDI = new Integer(tmpRawDataID);
						// If this raw data id is not yet included, then add it
						if ( allRequiredRawDataIDs.indexOf(tmpRawDataIDI) == -1 ) { allRequiredRawDataIDs.add(tmpRawDataIDI); }
					}
				}

				// Move required raw Data IDs from Vector<Integer> to int[]
				int[] allRequiredRawDataIDsi = new int[allRequiredRawDataIDs.size()];
				for (int i=0; i<allRequiredRawDataIDs.size(); i++) { allRequiredRawDataIDsi[i] = allRequiredRawDataIDs.get(i).intValue(); }

				// Ask controller to fetch total raw signals for these raw data files
				//Vector<AlignmentResult> selectedAlignmentResults = itemSelector.getSelectedAlignmentResults();
				clientForCluster.calcTotalRawSignal(allRequiredRawDataIDsi, lnp, selectedAlignmentResults);

				// doLinearNormalizationClientSide() will be called by clientForCluster when task completes

			} else {

				// Any other linear normalization method doesn't need access to raw data, so it is possible to call doLinearNormalizationClientSide() immediately
				Vector<AlignmentResult> selectedAlignmentResults = itemSelector.getSelectedAlignmentResults();
				doLinearNormalizationClientSide(lnp, selectedAlignmentResults);
			}

		}


		if (src == normStdComp) {

			StandardCompoundNormalizer scn = new StandardCompoundNormalizer();

			StandardCompoundNormalizerParameters scnp = scn.askParameters(this, getParameterStorage().getStandardCompoundNormalizerParameters());
			if (scnp==null) {
				statBar.setStatusText("Normalization cancelled.");
				return;
			}
			getParameterStorage().setStandardCompoundNormalizerParameters(scnp);

			statBar.setStatusText("Normalizing selected alignment results.");
			paintNow();

			setBusy(true);

			Vector<AlignmentResult> selectedAlignmentResults = itemSelector.getSelectedAlignmentResults();
			Enumeration<AlignmentResult> selectedAlignmentResultEnum = selectedAlignmentResults.elements();
			while (selectedAlignmentResultEnum.hasMoreElements()) {
				AlignmentResult ar = selectedAlignmentResultEnum.nextElement();
				AlignmentResult nar = scn.calcNormalization(this, ar, scnp);

				if (nar==null) {
					if (ar.getNumOfStandardCompounds()==0) {
						displayErrorMessage("Could not normalize " + ar.getNiceName() + ", because it does not have any standard compounds defined.");
					} else {
						displayErrorMessage("Could not normalize " + ar.getNiceName() + ", because of an unknown error.");
					}
				} else {
					itemSelector.addAlignmentResult(nar);
					addAlignmentResultVisualizerList(nar);
				}

			}

			statBar.setStatusText("Normalization done.");

			setBusy(false);

		}


		if (src == tsAlignmentFilter) {

			// Get all selected alignment results
			Vector<AlignmentResult> alignmentResults = itemSelector.getSelectedAlignmentResults();
			if (alignmentResults==null) { return; }

			AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();

			AlignmentResultFilterByGapsParameters arfbgParam= arfbg.askParameters(this, getParameterStorage().getAlignmentResultFilterByGapsParameters());
			if (arfbgParam == null) {
				statBar.setStatusText("Alignment result filtering cancelled.");
				return;
			}
			getParameterStorage().setAlignmentResultFilterByGapsParameters(arfbgParam);

			runAlignmentResultFilteringByGapsClientSide(arfbgParam, alignmentResults);

		}

		if (src == tsEmptySlotFiller) {

			// Get all selected alignment results
			Vector<AlignmentResult> alignmentResults = itemSelector.getSelectedAlignmentResults();
			if (alignmentResults==null) { return; }
			// Check that only a single alignment result was selected
			if (alignmentResults.size()!=1) {
				displayErrorMessage("Please select only a single alignment result for gap filling.");
				statBar.setStatusText("Please select only a single alignment result for gap filling.");
				return;
			}
			AlignmentResult alignmentResult = alignmentResults.get(0);

			// Check that the selected alignment result is not an imported version
			if (alignmentResult.isImported()) {

			}

			SimpleGapFiller sgf = new SimpleGapFiller();

			SimpleGapFillerParameters sgfParam= sgf.askParameters(this, getParameterStorage().getSimpleGapFillerParameters());
			if (sgfParam == null) {
				statBar.setStatusText("Gap filling cancelled.");
				return;
			}
			getParameterStorage().setSimpleGapFillerParameters(sgfParam);


			statBar.setStatusText("Filling empty gaps in alignment result.");
			paintNow();

			clientForCluster.fillGaps(alignmentResult, sgfParam);

		}

		if (src == batDefine) {

			// Get selected rawDataIDs
			int[] rawDataIDs = itemSelector.getSelectedRawDataIDs();

			BatchModeDialog bmd = new BatchModeDialog(this, rawDataIDs);

		}

		if (src == anOpenSRView) {

			// Add a new visualizer to all selected alignment results
			Vector<AlignmentResult> rv = itemSelector.getSelectedAlignmentResults();

			for (AlignmentResult ar : rv ) {
				addAlignmentResultVisualizerLogratioPlot(ar);
			}

			tileWindows();

		}


		if (src == anOpenSCVView) {

			// Add a new visualizer to all selected alignment results
			Vector<AlignmentResult> rv = itemSelector.getSelectedAlignmentResults();

			for (AlignmentResult ar : rv ) {
				addAlignmentResultVisualizerCoVarPlot(ar);
			}

			tileWindows();
		}

		if (src == anOpenCDAView) {
			// Add a new visualizer to all selected alignment results
			setBusy(true);
			Vector<AlignmentResult> rv = itemSelector.getSelectedAlignmentResults();
			for (AlignmentResult ar : rv ) {
				addAlignmentResultVisualizerCDAPlot(ar);
			}
			setBusy(false);
			updateMenuAvailability();
			tileWindows();
		}

		if (src == anOpenSammonsView) {
			// Add a new visualizer to all selected alignment results
			setBusy(true);
			Vector<AlignmentResult> rv = itemSelector.getSelectedAlignmentResults();
			for (AlignmentResult ar : rv ) {

				// Create new visualizer
				//AlignmentResultVisualizerSammonsPlotView sammonsView = new AlignmentResultVisualizerSammonsPlotView(this);
				//sammonsView.askParameters(alignmentResult, parameterStorage.getAlignmentResultVisualizerSammonsPlotViewParameters());
				addAlignmentResultVisualizerSammonsPlot(ar);
			}
			setBusy(false);
			tileWindows();
			updateMenuAvailability();
		}


		if (src == windowTileWindows) {
			tileWindows();
		}


		if (src == hlpAbout) {
			AboutDialog ad = new AboutDialog();
			ad.showModal(getDesktop());
			/*
			desktop.add(ad);
			ad.setLocation((desktop.getWidth()-ad.getWidth()) /2, (desktop.getHeight() - ad.getHeight()) /2);
			ad.setVisible(true);
			*/
		}

	}

	public Statusbar getStatusBar() {
		return statBar;
	}

/*
	public RunSelector getRunSelector() {
		//return runPick;
		return null;
	}
*/

	/**
	 * Update menu elements availability according to
	 * what is currently selected in run selector and on desktop
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

		if (	(numOfRawDataWithVisibleVisualizer(false)>0) ||
				(numOfResultsWithVisibleVisualizer(false)>0) ) {
			windowTileWindows.setEnabled(true);
		}

		RawDataAtClient actRawData = itemSelector.getActiveRawData();
		if (actRawData!=null) {
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
				// ssCombinatorialDeisotoping.setEnabled(true); DEBUG: Feature not yet ready
				ssIncompleteIsotopePatternFilter.setEnabled(true);
				fileExportPeakList.setEnabled(true);
				tsJoinAligner.setEnabled(true);
				tsFastAligner.setEnabled(true);
			}


			JInternalFrame activeWindow = desktop.getSelectedFrame();

			if (activeWindow!=null) {
				if ((activeWindow.getClass() == RawDataVisualizerTICView.class) ||
					(activeWindow.getClass() == RawDataVisualizerTwoDView.class) ||
					(activeWindow.getClass() == RawDataVisualizerSpectrumView.class) ) {
					filePrint.setEnabled(true);
					editCopy.setEnabled(true);
				}
			}
		}


		AlignmentResult actResult = itemSelector.getActiveResult();

		if (actResult!=null) {
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


			JInternalFrame activeWindow = desktop.getSelectedFrame();

			if (activeWindow!=null) {
				if (
					(activeWindow.getClass() == AlignmentResultVisualizerLogratioPlotView.class) ||
					(activeWindow.getClass() == AlignmentResultVisualizerCoVarPlotView.class) ||
					(activeWindow.getClass() == AlignmentResultVisualizerCDAPlotView.class) ||
					(activeWindow.getClass() == AlignmentResultVisualizerSammonsPlotView.class)
				   ) {
					filePrint.setEnabled(true);
					editCopy.setEnabled(true);
				}
			}
		}

		// If at least one run or result is visible, then tile windows is active
		if ( 	(numOfRawDataWithVisibleVisualizer(false)>0) ||
				(numOfResultsWithVisibleVisualizer(false)>0) ) {
			windowTileWindows.setEnabled(true);
		}

	}

	public void paintNow() {
		update(getGraphics());
	}

	/**
	 * This method closes a set of raw data files
	 */
	public void closeRawDataFiles(int[] rawDataIDs) {

			if (rawDataIDs.length>0) {
				// Check that any of these files is not participating any alignment
				String errorMessage = null;
				for (int rawDataID : rawDataIDs) {
					RawDataAtClient rawData = itemSelector.getRawDataByID(rawDataID);
					if (rawData.getAlignmentResultIDs().size()>0) {
						Vector<Integer> alignmentResultIDs = rawData.getAlignmentResultIDs();
						errorMessage = rawData.getNiceName() + " is participating in alignment(s). Before closing the raw data files, please close alignment result(s): ";
						for (Integer alignmentResultID : alignmentResultIDs) {
							errorMessage += itemSelector.getAlignmentResultByID(alignmentResultID.intValue()).getNiceName() + ", ";
						}
					}
					if (errorMessage!=null) {
						displayErrorMessage(errorMessage);
						return;
					}
				}

				// Remove all visualizers for these raw data files
				for (int rawDataID : rawDataIDs) { removeRawDataVisualizers(rawDataID); }

				clientForCluster.closeRawDataFiles(rawDataIDs);

				getStatusBar().setStatusText("Closing " + rawDataIDs.length + " raw data file(s).");

			}

	}

	/**
	 * This method closes a set of alignment results
	 */
	public void closeAlignmentResults(int[] alignmentResultIDs) {

			if (alignmentResultIDs.length>0) {

				for (int alignmentResultID : alignmentResultIDs) {

					if (itemSelector.getAlignmentResultByID(alignmentResultID).isImported()) { continue; }

					// Remove dependency from each involved raw data file
					int[] rawDataIDs = itemSelector.getAlignmentResultByID(alignmentResultID).getRawDataIDs();
					for (int rawDataID : rawDataIDs) {
						RawDataAtClient rawData = itemSelector.getRawDataByID(rawDataID);
						rawData.removeAlignmentResultID(alignmentResultID);
					}
				}

				for (int alignmentResultID : alignmentResultIDs) {
					// Close all visualizers for these alignment results
					removeAlignmentResultVisualizers(alignmentResultID);

					// Remove these alignment results from the item selector
					itemSelector.removeAlignmentResult(itemSelector.getAlignmentResultByID(alignmentResultID));
				}

				getStatusBar().setStatusText("Closed " + alignmentResultIDs.length + " alignment result(s).");

			}

	}


	public void doLinearNormalizationClientSide(LinearNormalizerParameters lnp, Vector<AlignmentResult> originalAlignmentResults) {
		statBar.setStatusText("Normalizing selected alignment results.");
		paintNow();

		setBusy(true);

		LinearNormalizer ln = new LinearNormalizer();
		Enumeration<AlignmentResult> originalAlignmentResultEnum = originalAlignmentResults.elements();
		while (originalAlignmentResultEnum.hasMoreElements()) {
			AlignmentResult ar = originalAlignmentResultEnum.nextElement();
			AlignmentResult nar = ln.calcNormalization(MainWindow.this, ar, lnp);
			itemSelector.addAlignmentResult(nar);
			addAlignmentResultVisualizerList(nar);
		}

		setBusy(false);
		statBar.setStatusText("Normalization done.");
	}


	public void runAlignmentResultFilteringByGapsClientSide(AlignmentResultFilterByGapsParameters arpp, Vector<AlignmentResult> originalAlignmentResults) {
		statBar.setStatusText("Filtering selected alignment results.");
		paintNow();

		setBusy(true);

		AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();

		Enumeration<AlignmentResult> originalAlignmentResultEnum = originalAlignmentResults.elements();
		while (originalAlignmentResultEnum.hasMoreElements()) {
			AlignmentResult ar = originalAlignmentResultEnum.nextElement();
			AlignmentResult nar = arfbg.processAlignment(MainWindow.this, ar, (AlignmentResultProcessorParameters)arpp);
			itemSelector.addAlignmentResult(nar);
			addAlignmentResultVisualizerList(nar);
		}

		setBusy(false);
		statBar.setStatusText("Alignment result filtering done.");

	}

	public AlignmentResult runAlignmentResultFilteringByGapsClientSide(AlignmentResultFilterByGapsParameters arpp, AlignmentResult originalAlignmentResult) {
		statBar.setStatusText("Filtering alignment result.");
		paintNow();

		setBusy(true);

		AlignmentResultFilterByGaps arfbg = new AlignmentResultFilterByGaps();

		AlignmentResult nar = arfbg.processAlignment(MainWindow.this, originalAlignmentResult, (AlignmentResultProcessorParameters)arpp);
		itemSelector.addAlignmentResult(nar);
		addAlignmentResultVisualizerList(nar);

		setBusy(false);
		statBar.setStatusText("Alignment result filtering done.");
		return nar;

	}


	/**
	 * Copies the zoom settings from given run to all other runs.
	 * whatDims parameter defines wheter to copy zoom settings only in mz, rt or both dimensions
	 */

	public void setSameZoomToOtherRawDatas(RawDataAtClient originalRawData, int whatDims) {

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
		int[] modifiedRawDataIDs = new int[rawDatas.length-1];
		int modifiedRawDataIDsInd = 0;

		for (RawDataAtClient r : rawDatas) {

			if (originalRawData!=r) {

				modifiedRawDataIDs[modifiedRawDataIDsInd] = r.getRawDataID();
				modifiedRawDataIDsInd++;

				// Check that selection and cursor position of source run are valid also for this target run
				if (r.getDataMinMZ()>cursorMZ) { tmpCursorMZ = r.getDataMinMZ(); } else { tmpCursorMZ = cursorMZ; }
				if (r.getDataMaxMZ()<cursorMZ) { tmpCursorMZ = r.getDataMaxMZ(); } else { tmpCursorMZ = cursorMZ; }

				if (r.getDataMinMZ()>startMZ) { tmpStartMZ = r.getDataMinMZ(); } else { tmpStartMZ = startMZ; }
				if (r.getDataMaxMZ()<startMZ) { tmpStartMZ = r.getDataMaxMZ(); } else { tmpStartMZ = startMZ; }

				if (r.getDataMinMZ()>endMZ) { tmpEndMZ = r.getDataMinMZ(); } else { tmpEndMZ = endMZ; }
				if (r.getDataMaxMZ()<endMZ) { tmpEndMZ = r.getDataMaxMZ(); } else { tmpEndMZ = endMZ; }

				if ( (r.getNumOfScans()-1) < cursorScan) { tmpCursorScan = r.getNumOfScans()-1; } else { tmpCursorScan = cursorScan; }

				if ( (r.getNumOfScans()-1) < endScan) { tmpEndScan= r.getNumOfScans()-1; } else { tmpEndScan = endScan; }
				tmpStartScan = startScan; // Start scan is 0 for all runs


				if (whatDims == SET_SAME_ZOOM_MZ) {
					r.setSelectionMZ(tmpStartMZ, tmpEndMZ);
					r.setCursorPositionMZ(tmpCursorMZ);
					//r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_MZ, statBar);
				}

				if (whatDims == SET_SAME_ZOOM_SCAN) {
					r.setSelectionScan(tmpStartScan, tmpEndScan);
					r.setCursorPositionScan(tmpCursorScan);
					//r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_SCAN, statBar);
				}

				if (whatDims == SET_SAME_ZOOM_BOTH) {
					r.setSelection(tmpStartScan, tmpEndScan, tmpStartMZ, tmpEndMZ);
					r.setCursorPosition(tmpCursorScan, tmpCursorMZ);
					//r.refreshVisualizers(Visualizer.CHANGETYPE_SELECTION_BOTH, statBar);
				}
			}
		}


		if (whatDims == SET_SAME_ZOOM_MZ) {
			startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ, modifiedRawDataIDs);
		}
		if (whatDims == SET_SAME_ZOOM_SCAN) {
			startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, modifiedRawDataIDs);
		}
		if (whatDims == SET_SAME_ZOOM_BOTH) {
			startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, modifiedRawDataIDs);
		}


	}



	public void setMouseWaitCursor() {
		if (myMouseAdapter == null) {
			myMouseAdapter = new MouseAdapter() {};
			this.getGlassPane().addMouseListener( myMouseAdapter);
			this.getGlassPane().setCursor(myWaitCursor);
		}
		this.getGlassPane().setVisible(true);
	}

	public void setMouseDefaultCursor() {
		this.getGlassPane().setVisible(false);
	}


	public ParameterStorage getParameterStorage() {
		return parameterStorage;
	}

/*
	public FormatCoordinates getFormatCoordinates() {
		return paramSettings.getFormatCoordinates();
	}
*/

	public ItemSelector getItemSelector() {
		return itemSelector;
	}

	public ClientForCluster getClientForCluster() {
		return clientForCluster;
	}


	/**
	 * Prepares everything for quit and then shutdowns the application
	 */
	private boolean exitMZmine() {

		// Ask if use really wants to quit
		int selectedValue = JOptionPane.showInternalConfirmDialog(desktop, "Are you sure you want to exit MZmine?", "Exiting...", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (selectedValue != JOptionPane.YES_OPTION) {
			statBar.setStatusText("Exit cancelled.");
			return false;
		}

		// Close all alignment results
		int[] alignmentResultIDs = itemSelector.getAlignmentResultIDs();
		closeAlignmentResults(alignmentResultIDs);

		// Close all raw data files
		int[] rawDataIDs = itemSelector.getRawDataIDs();
		closeRawDataFiles(rawDataIDs);

		// Disconnect client from cluster
		clientForCluster.disconnectFromController();

		// Save settings
		// (not automatic)

		// Shutdown
		this.dispose();
		System.exit(0);

		return true;

	}

	public void displayErrorMessage(String msg) {
					try {
					statBar.setStatusText(msg);
					JOptionPane.showInternalMessageDialog(
													getDesktop(),
													msg,
													"Sorry",
													JOptionPane.ERROR_MESSAGE
												 );
					} catch (Exception exce ) {}
	}




}

