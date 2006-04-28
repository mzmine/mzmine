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


package net.sf.mzmine.visualizers.alignmentresult;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.RepaintManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.userinterface.components.Colorbar;
import net.sf.mzmine.userinterface.dialogs.SelectTwoGroupsDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.userinterface.mainwindow.Statusbar;
import net.sf.mzmine.util.GeneralParameters;
import net.sf.mzmine.util.HeatMapColorPicker;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.TransferableImage;




/**
 * This class is used to draw a spatial logratio plot between two groups of runs in one alignment result
 *
 */
public class AlignmentResultVisualizerLogratioPlotView extends JInternalFrame implements Printable, AlignmentResultVisualizer, InternalFrameListener {

	private static final double marginSize = (double)0.02; // How much extra margin is added to the axis in full zoom

	private double paramLogratioThresholdLevel = (double)1.5;	// Logratio-level which gives total red or green colour.
	private double paramLogratioThresholdLevelMax = (double)3.0;	// User can control above parameter within range 0..this
	private double paramAvgIntThresholdLevel;	// Intensity thresholding level

	private int[] heatmap_pal_waypoints = {-30, -15, 0, 15, 30};
	private String[] heatmap_pal_waypointLabels = {"-3.0", "-1.5", "0", "1.5", "3.0"};
	private int[][] heatmap_pal_waypointRGBs = { {0,255,0}, {0,255,0}, {0,0,0}, {255,0,0}, {255,0,0} };

	private MainWindow mainWin;
	private Statusbar statBar;

	private AlignmentResult alignmentResult;

	private int[] groupOneIDs;
	private int[] groupTwoIDs;

	private JPanel rightPnl;
	private Colorbar colorPnl;
	private OptionsPanelVertical sliderPnl;
	private OptionsPanelHorizontal topPnl;
	private PlotYAxis leftPnl;
	private PlotXAxis bottomPnl;

	private PlotArea PlotArea;

	private HeatMapColorPicker heatMap;

	private int retval;


	/**
	 * Constructor: builds visualizer frame, but doesn't set any data yet.
	 *
	 * @param _mainWin	Main window of Masso
	 */
	public AlignmentResultVisualizerLogratioPlotView(MainWindow _mainWin) {
		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();

		heatMap = new HeatMapColorPicker(heatmap_pal_waypoints, heatmap_pal_waypointRGBs);

		// Build this visualizer
		getContentPane().setLayout(new BorderLayout());

		bottomPnl = new PlotXAxis(); //TICXAxis();
		bottomPnl.setMinimumSize(new Dimension(getWidth(),25));
		bottomPnl.setPreferredSize(new Dimension(getWidth(),25));
		bottomPnl.setBackground(Color.white);
		getContentPane().add(bottomPnl, java.awt.BorderLayout.SOUTH);

		topPnl = new OptionsPanelHorizontal();
		topPnl.setMinimumSize(new Dimension(getWidth(),45));
		topPnl.setPreferredSize(new Dimension(getWidth(),45));
		getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);

		leftPnl = new PlotYAxis();  // TICYAxis();
		leftPnl.setMinimumSize(new Dimension(100, getHeight()));
		leftPnl.setPreferredSize(new Dimension(100, getHeight()));
		leftPnl.setBackground(Color.white);
		getContentPane().add(leftPnl, java.awt.BorderLayout.WEST);


		rightPnl = new JPanel();
		rightPnl.setMinimumSize(new Dimension(70, getHeight()));
		rightPnl.setPreferredSize(new Dimension(70, getHeight()));
		rightPnl.setLayout(new BorderLayout());
		getContentPane().add(rightPnl, java.awt.BorderLayout.EAST);

		colorPnl = new Colorbar(heatMap, heatmap_pal_waypointLabels, 256);
		sliderPnl = new OptionsPanelVertical();

		rightPnl.add(colorPnl, java.awt.BorderLayout.CENTER);
		rightPnl.add(sliderPnl, java.awt.BorderLayout.EAST);


		PlotArea = new PlotArea(this);
		PlotArea.setBackground(Color.white);
		getContentPane().add(PlotArea, java.awt.BorderLayout.CENTER);

		setResizable( true );
		setIconifiable( true );

		addInternalFrameListener(this);

	}


	/**
	 * Asks user to define two groups of runs and prepares the plot.
	 * Also prepares data for plot
	 *
	 * @param	_alignmentResult	Alignment result to be displayed by this visualizer
	 * @return	RETVAL_OK if user selected two groups of samples, RETVAL_CANCEL if didn't
	 */
	public int askParameters(AlignmentResult _alignmentResult) {
		alignmentResult = _alignmentResult;
		setTitle(alignmentResult.getNiceName() + ": logratio plot");


		// Collect raw data IDs and names for dialog
		int[] rawDataIDs = alignmentResult.getRawDataIDs();
		RawDataPlaceHolder[] rawDatas = new RawDataPlaceHolder[rawDataIDs.length];
		if (alignmentResult.isImported()) {
			for (int i=0; i<rawDataIDs.length; i++) { rawDatas[i] = new RawDataPlaceHolder(alignmentResult.getImportedRawDataName(rawDataIDs[i]), rawDataIDs[i]);  }
		} else {
		//	for (int i=0; i<rawDataIDs.length; i++) { rawDatas[i] = new RawDataPlaceHolder(mainWin.getItemSelector().getRawDataByID(rawDataIDs[i]).getNiceName(), rawDataIDs[i]); }
		}


		// Show user a dialog for selecting two groups of runs from this aligment

		SelectTwoGroupsDialog stgd = new SelectTwoGroupsDialog(mainWin,
																"Logratio plot",
																"Select raw data files for group one",
																"Select raw data files for group two",
																rawDatas, null, null);
		stgd.show();
/*
		stgd.setLocationRelativeTo(mainWin);
		stgd.setVisible(true);
*/
		retval = stgd.getExitCode();


		// Check if user clicked cancel
		if (retval == -1) { return -1; }

		// Get index numbers for runs
		Vector<RawDataPlaceHolder> tmpRawDatas = stgd.getSelectedItems(1);
		groupOneIDs = new int[tmpRawDatas.size()];
		for (int ind=0; ind<tmpRawDatas.size(); ind++) {
			groupOneIDs[ind] = tmpRawDatas.get(ind).getRawDataID(); }

		tmpRawDatas = stgd.getSelectedItems(2);
		groupTwoIDs = new int[tmpRawDatas.size()];
		for (int ind=0; ind<tmpRawDatas.size(); ind++) {
			groupTwoIDs[ind] = tmpRawDatas.get(ind).getRawDataID(); }

		// Prepare data for the plot
		preparePlot();

		return 1;

	}

	public void setAlignmentResult(AlignmentResult alignmentResult) {
		// Logratio plot doesn't use this method
		// Required data is given by user in parameter setup dialog
	}

	/**
	 * This method is called when peak measuring is switched between height / area.
	 * Logratios are always calculated using the currently selected mode
	 */
	public void refreshVisualizer(int changeType) {
		if (changeType == AlignmentResultVisualizer.CHANGETYPE_PEAK_MEASURING_SETTING) {
			preparePlot();
		}
	}


	/**
	 * This function calculates logratios between average peak intensities of two groups
	 */
	private void preparePlot() {

		if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
			setTitle(alignmentResult.getNiceName() + ": Logratios of average peak heights.");
		}
		if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
			setTitle(alignmentResult.getNiceName() + ": Logratios of average peak areas.");
		}


		int numOfPeaks = alignmentResult.getNumOfRows();

		int[] tmp_alignmentRowValues = new int[numOfPeaks];
		double[] tmp_mzValues = new double[numOfPeaks];
		double[] tmp_rtValues = new double[numOfPeaks];
		double[] tmp_logratioValues = new double[numOfPeaks];
		double[] tmp_avgMeasurementValues = new double[numOfPeaks];
		int numOfValues = 0;

		double minMZ = Double.MAX_VALUE;
		double minRT = Double.MAX_VALUE;
		double maxMZ = Double.MIN_VALUE;
		double maxRT = Double.MIN_VALUE;
		double tmpMZ, tmpRT;

		int numOfGoodPeaks;

		double groupOneMeasurementSum, groupTwoMeasurementSum;
		int groupOneMeasurementNum, groupTwoMeasurementNum;
		double groupOneMeasurementAvg, groupTwoMeasurementAvg;

		// Calculate average height/area in both groups for every peak
		for (int rowInd=0; rowInd<alignmentResult.getNumOfRows(); rowInd++) {

			groupOneMeasurementNum = 0; groupOneMeasurementSum = 0;
			groupTwoMeasurementNum = 0; groupTwoMeasurementSum = 0;


			// Average among group one
			if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
				for (int runInd : groupOneIDs) {
					if ((alignmentResult.getPeakStatus(runInd, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) ||
					 	(alignmentResult.getPeakStatus(runInd, rowInd)==AlignmentResult.PEAKSTATUS_ESTIMATED)) {
						 	groupOneMeasurementNum++;
							groupOneMeasurementSum += alignmentResult.getPeakHeight(runInd, rowInd);
					}
				}
			}

			if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
				for (int runInd : groupOneIDs) {
					if (alignmentResult.getPeakStatus(runInd, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) {
						 	groupOneMeasurementNum++;
							groupOneMeasurementSum += alignmentResult.getPeakArea(runInd, rowInd);
					}
				}
			}


			// Average among "target" group
			if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
				for (int runInd : groupTwoIDs) {
					if ((alignmentResult.getPeakStatus(runInd, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) ||
					 	(alignmentResult.getPeakStatus(runInd, rowInd)==AlignmentResult.PEAKSTATUS_ESTIMATED)) {
						 	groupTwoMeasurementNum++;
							groupTwoMeasurementSum += alignmentResult.getPeakHeight(runInd, rowInd);
					}
				}
			}

			if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
				for (int runInd : groupTwoIDs) {
					if (alignmentResult.getPeakStatus(runInd, rowInd)==AlignmentResult.PEAKSTATUS_DETECTED) {
						 	groupTwoMeasurementNum++;
							groupTwoMeasurementSum += alignmentResult.getPeakArea(runInd, rowInd);
					}
				}
			}


			// If there were at least one intensity measurement in both groups, add this peak to the plot
			if ( (groupOneMeasurementNum>0) && (groupTwoMeasurementNum>0) ) {

				groupOneMeasurementAvg = groupOneMeasurementSum / (double)groupOneMeasurementNum;
				groupTwoMeasurementAvg = groupTwoMeasurementSum / (double)groupTwoMeasurementNum;

				tmp_alignmentRowValues[numOfValues] = rowInd;
				tmp_mzValues[numOfValues] = alignmentResult.getAverageMZ(rowInd);
				tmp_rtValues[numOfValues] = alignmentResult.getAverageRT(rowInd);

				if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) { tmp_avgMeasurementValues[numOfValues] = alignmentResult.getAverageHeight(rowInd); }
				if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) { tmp_avgMeasurementValues[numOfValues] = alignmentResult.getAverageArea(rowInd); }

				tmp_logratioValues[numOfValues] = (double)( java.lang.Math.log( (double)groupOneMeasurementAvg / (double)groupTwoMeasurementAvg ) / java.lang.Math.log(2.0) );

				// Also control what are the minimum and maximum mz and rt value of all peaks that are added to the plot
				if (minMZ>=tmp_mzValues[numOfValues]) { minMZ = tmp_mzValues[numOfValues]; }
				if (maxMZ<tmp_mzValues[numOfValues]) { maxMZ = tmp_mzValues[numOfValues]; }
				if (minRT>=tmp_rtValues[numOfValues]) { minRT = tmp_rtValues[numOfValues]; }
				if (maxRT<tmp_rtValues[numOfValues]) { maxRT = tmp_rtValues[numOfValues]; }

				numOfValues++;
			}
		}

		// Add small margins to plot
		minMZ=minMZ-marginSize*(maxMZ-minMZ);
		maxMZ=maxMZ+marginSize*(maxMZ-minMZ);
		minRT=minRT-marginSize*(maxRT-minRT);
		maxRT=maxRT+marginSize*(maxRT-minRT);
		if (minMZ<0) { minMZ = 0; }
		if (minRT<0) { minRT = 0; }


		// Finally move data to vectors that have correct length
		int[] alignmentRowValues = new int[numOfValues];
		double[] mzValues = new double[numOfValues];
		double[] rtValues = new double[numOfValues];
		double[] logratioValues = new double[numOfValues];
		double[] avgMeasurementValues = new double[numOfValues];

		for (int ind=0; ind<numOfValues; ind++) {
			alignmentRowValues[ind] = tmp_alignmentRowValues[ind];
			mzValues[ind] = tmp_mzValues[ind];
			rtValues[ind] = tmp_rtValues[ind];
			avgMeasurementValues[ind] = tmp_avgMeasurementValues[ind];
			logratioValues[ind] = tmp_logratioValues[ind];
		}


		// Calculate quantile values that will be used for filtering data by average peak intensity
		double[] quantiles = new double[10];
		double[] quantileLabels = new double[quantiles.length];
		for (int ind=0; ind<10; ind++) {
			quantiles[ind] = (double)ind/(double)10 * (double)1;
			quantileLabels[ind] = 1-quantiles[ind];
		}
		double[] quantileValues = MyMath.calcQuantile(avgMeasurementValues, quantiles);

		// Set thresholding levels to slider
		topPnl.setupIntThresholdSlider(quantileLabels, quantileValues);

		// Set plot data
		PlotArea.setData(alignmentRowValues, mzValues, rtValues, logratioValues, avgMeasurementValues, minRT, maxRT, minMZ, maxMZ);

	}


	/**
	 * Implementation of methods in InternalFrameListener
	 */
	public void internalFrameActivated(InternalFrameEvent e) {
		// When this frame is selected...
		// - Set this as active visualizer for the alignment result
		//alignmentResult.setActiveVisualizer(this);
		// - Select the alignment result in the menu
		mainWin.getItemSelector().setActiveAlignmentResult(alignmentResult);
	}
	public void internalFrameClosed(InternalFrameEvent e) {	}
	public void internalFrameClosing(InternalFrameEvent e) { }
	public void internalFrameDeactivated(InternalFrameEvent e) { }
	public void internalFrameDeiconified(InternalFrameEvent e) { }
	public void internalFrameIconified(InternalFrameEvent e) { }
	public void internalFrameOpened(InternalFrameEvent e) { }


	/**
	 * Implementation of AlignmentResultVisualizer interface
	 */
	public void updateSelectedRow() {
		int rowNum = alignmentResult.getSelectedRow();
		// Actual selection (cursor movement) is done in the plot panel
		PlotArea.selectAlignmentRow(rowNum);
	}

	/**
	 * Implementation of AlignmentResultVisualizer interface
	 */
	public void printMe() {
		PrinterJob printJob = PrinterJob.getPrinterJob();
		HashPrintRequestAttributeSet pSet = new HashPrintRequestAttributeSet();

		if (printJob.printDialog(pSet)) {
			printJob.setPrintable(this);
			try {
				printJob.print(pSet);
			} catch (Exception PrintException) {}
		}
	}

	/**
	 * Implementation of Printable interface
	 */
	public int print(Graphics g, PageFormat pf, int pi){
		double sx, sy;
		final int titleHeight = 30;

	     if (pi > 0) { return NO_SUCH_PAGE; }
	     else {

	        Graphics2D g2 = (Graphics2D)g;
	        g2.translate(pf.getImageableX(), pf.getImageableY());


			g2.drawString(this.getTitle(),0,titleHeight-5);

			g2.translate(0, titleHeight);


	        sx = (double)pf.getImageableWidth()/(double)getContentPane().getWidth();
	        sy = (double)(pf.getImageableHeight()-titleHeight)/(double)getContentPane().getHeight();

	        g2.transform(AffineTransform.getScaleInstance(sx,sy));

			RepaintManager currentManager = RepaintManager.currentManager(getContentPane());
    		currentManager.setDoubleBufferingEnabled(false);

	        getContentPane().paint(g2);

	        currentManager.setDoubleBufferingEnabled(true);
	        return Printable.PAGE_EXISTS;
		}

	 }

	/**
	 * Implementation of AlignmentResultVisualizer interface
	 */
	 public void copyMe() {
		// Initialize clipboard
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();

		// Draw visualizer graphics
		int w = getContentPane().getWidth();
		int h = getContentPane().getHeight();
		BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bi.getGraphics();
		//g.setTransform(AffineTransform.getTranslateInstance(-r.x, -r.y);
		getContentPane().paint(g);

		// Put image to clipboard
		clipboard.setContents(new TransferableImage(bi),null);
	}


	/**
	 * Implementation of AlignmentResultVisualizer interface
	 */
	 /*
	public void closeMe() {
		dispose();
		alignmentResult = null;
	}
*/


	/**
	 * This class is the panel where plot is drawn
	 */
	private class PlotArea extends JPanel implements ActionListener, java.awt.event.MouseListener , java.awt.event.MouseMotionListener {


		private AlignmentResultVisualizerLogratioPlotView masterFrame;
		private double minMZ;
		private double maxMZ;
		private double minRT;
		private double maxRT;

		private double zoomMinMZ;
		private double zoomMaxMZ;
		private double zoomMinRT;
		private double zoomMaxRT;

		private double cursorPositionMZ;
		private double cursorPositionRT;

		private double mouseAreaStartMZ;
		private double mouseAreaEndMZ;
		private double mouseAreaStartRT;
		private double mouseAreaEndRT;

		private double selectionFirstClickMZ;
		private double selectionFirstClickRT;
		private double selectionLastClickMZ;
		private double selectionLastClickRT;

		private int[] alignmentRowValues = null;
		private double[] mzValues = null;
		private double[] rtValues = null;
		private double[] lrValues = null;
		private double[] avgIntValues = null;

		private JPopupMenu popupMenu;

		private JMenuItem zoomToSelectionMenuItem;
		private JMenuItem zoomOutMenuItem;
		private JMenuItem zoomOutLittleMenuItem;

		private JMenuItem selectNearestPeakMenuItem;


		/**
		 * Constructor: initializes panel
		 *
		 * @param	_masterFrame	SpatialLogratioPlotView frame where this panel is located
		 */
		public PlotArea(AlignmentResultVisualizerLogratioPlotView _masterFrame) {
			masterFrame = _masterFrame;
		    popupMenu = new JPopupMenu();
		    zoomToSelectionMenuItem = new JMenuItem("Zoom to selection");
		    zoomToSelectionMenuItem.addActionListener(this);
		    zoomToSelectionMenuItem.setEnabled(false);
		    popupMenu.add(zoomToSelectionMenuItem);

		    zoomOutMenuItem = new JMenuItem("Zoom out full");
		    zoomOutMenuItem.addActionListener(this);
		    popupMenu.add(zoomOutMenuItem);

			zoomOutLittleMenuItem = new JMenuItem("Zoom out little");
			zoomOutLittleMenuItem.addActionListener(this);
		    popupMenu.add(zoomOutLittleMenuItem);

		    popupMenu.addSeparator();

		    selectNearestPeakMenuItem = new JMenuItem("Select nearest peak");
		    selectNearestPeakMenuItem.addActionListener(this);
		    popupMenu.add(selectNearestPeakMenuItem);

			addMouseListener(this);
			addMouseMotionListener(this);

		}


		/**
		 * Implementation of ActionListener interface
		 */
		public void actionPerformed(java.awt.event.ActionEvent e) {

			Object src = e.getSource();

			// Pop-up menu: Set zoom to currently selected area
			if (src == zoomToSelectionMenuItem) {
				zoomMinMZ = mouseAreaStartMZ;
				zoomMaxMZ = mouseAreaEndMZ;
				zoomMinRT = mouseAreaStartRT;
				zoomMaxRT = mouseAreaEndRT;
				bottomPnl.setScale(zoomMinRT, zoomMaxRT);
				leftPnl.setScale(zoomMinMZ, zoomMaxMZ);
				repaint();
			}


			// Pop-up menu: Zoom out to full plot
			if (src == zoomOutMenuItem) {
				zoomMinMZ = minMZ;
				zoomMaxMZ = maxMZ;
				zoomMinRT = minRT;
				zoomMaxRT = maxRT;
				bottomPnl.setScale(zoomMinRT, zoomMaxRT);
				leftPnl.setScale(zoomMinMZ, zoomMaxMZ);
				repaint();
			}

			// Pop-up menu: Zoom out a little
			if (src == zoomOutLittleMenuItem) {
				zoomOutLittle();

				bottomPnl.setScale(zoomMinRT, zoomMaxRT);
				leftPnl.setScale(zoomMinMZ, zoomMaxMZ);

				repaint();
			}

			// Pop-up menu: Search and select peak nearest to current cursor location
			if (src == selectNearestPeakMenuItem) {

				int nearestInd = -1;
				double nearestDist = Double.MAX_VALUE;
				double tmpMZ, tmpRT, tmpInt;
				double tmpDist;

				// Calculate ratio that defines current "stretching" of the plot window
				// (This factor is used in distance measuring)
				double dataPerPixelMZ = (zoomMaxMZ-zoomMinMZ) / getHeight();
				double dataPerPixelRT = (zoomMaxRT-zoomMinRT) / getWidth();
				double ratioMZvsRT = dataPerPixelMZ / dataPerPixelRT;

				// Search for nearest peak
				for(int ind=0; ind<alignmentRowValues.length; ind++) {

					tmpMZ = mzValues[ind];
					tmpRT = rtValues[ind];
					tmpInt = avgIntValues[ind];

					// Only measure distance if this peak is within current visible area of the plot and visible with current threshold level
					if (((tmpMZ>=zoomMinMZ) && (tmpMZ<=zoomMaxMZ)) &&
						((tmpRT>=zoomMinRT) && (tmpRT<=zoomMaxRT)) &&
						( tmpInt >= paramAvgIntThresholdLevel)) {
						tmpDist = java.lang.Math.abs(tmpMZ-cursorPositionMZ)+ratioMZvsRT*java.lang.Math.abs(tmpRT-cursorPositionRT);

						// Check if this is currently the nearest peak
						if (tmpDist<=nearestDist) {
							nearestInd = ind;
							nearestDist = tmpDist;
						}
					}
				}

				// If nearest peak was found, set cursor on it
				if (nearestInd!=-1) {
					cursorPositionMZ = mzValues[nearestInd];
					cursorPositionRT = rtValues[nearestInd];
				}

				// And select this item in all other visualizers diplaying this same alignment result
				alignmentResult.setSelectedRow(alignmentRowValues[nearestInd]);
		//		mainWin.updateAlignmentResultVisualizers(alignmentResult.getAlignmentResultID());
				repaint();
			}

		}


		/**
		 * This method moves cursor over the given alignment result list item
		 */
		public void selectAlignmentRow(int rowNum) {
			int matchInd = -1;
			int ind = 0;
			double mz,rt;

			// Search for this alignment row among data points of this plot
			while( (matchInd==-1) && (ind<alignmentRowValues.length)) {
				if (alignmentRowValues[ind]==rowNum) {matchInd = ind; }
				ind++;
			}

			// If this row was found
			if (matchInd!=-1) {
				// Set cursor position over the data point
				cursorPositionMZ = mzValues[matchInd];
				cursorPositionRT = rtValues[matchInd];

				// Make sure that this data point is inside current visible area

				// While it is outside current visible area...
				while (	(cursorPositionMZ<zoomMinMZ) || (cursorPositionMZ>zoomMaxMZ) ||
						(cursorPositionRT<zoomMinRT) || (cursorPositionRT>zoomMaxRT) ) {
					zoomOutLittle();
				}

				// Make sure that this data point is draw with current intensity threshold settings
				while (paramAvgIntThresholdLevel > avgIntValues[matchInd]) {
					if ((topPnl.decreaseThresholdLevel())==false) {
						// This should never happen: Threshold level can't be set to any lower level, but still the peak is not visible
						mainWin.displayErrorMessage("Internal error while setting intensity threshold level");
						break;
					}
				}

				bottomPnl.setScale(zoomMinRT, zoomMaxRT);
				leftPnl.setScale(zoomMinMZ, zoomMaxMZ);

				repaint();
			}

		}

		/**
		 * Zooms out little from the current view
		 */
		private void zoomOutLittle() {
			double midX = (zoomMinRT+zoomMaxRT)/(double)2.0;
			double midY = (zoomMinMZ+zoomMaxMZ)/(double)2.0;
			double tmpMinX, tmpMaxX;
			double tmpMinY, tmpMaxY;

			// Expand the range a little
			if (((midX-zoomMinRT)>0) && ((zoomMaxRT-midX)>0)) {
				tmpMinX = (int)(java.lang.Math.round(midX - (midX-zoomMinRT)*1.5));
				tmpMaxX = (int)(java.lang.Math.round(midX + (zoomMaxRT-midX)*1.5));
			} else {
				tmpMinX = zoomMinRT - 1;
				tmpMaxX = zoomMaxRT + 1;
			}

			if (((midY-zoomMinMZ)>0) && ((zoomMaxMZ-midY)>0)) {
				tmpMinY = midY - (midY-zoomMinMZ)*(double)1.5;
				tmpMaxY = midY + (zoomMaxMZ-midY)*(double)1.5;
			} else {
				tmpMinY = zoomMinMZ - 1;
				tmpMaxY = zoomMaxMZ + 1;
			}

			//  Check that it didn't over expand
			if (tmpMinX<minRT) {tmpMinX = minRT;}
			if (tmpMaxX>maxRT) { tmpMaxX = maxRT; }
			if (tmpMinY<minMZ) { tmpMinY = minMZ; }
			if (tmpMaxY>maxMZ) { tmpMaxY = maxMZ; }

			// Set new zoom
			zoomMinRT = tmpMinX;
			zoomMaxRT = tmpMaxX;
			zoomMinMZ = tmpMinY;
			zoomMaxMZ = tmpMaxY;

		}


		/**
		 * This method paints the plot to this panel
		 */
		public void paint(Graphics g) {
			super.paint(g);

			double x,y;
			int x1,y1,x2,y2;
			int radius;
			double mz,rt,lr, ai;
			double red, green;
			Color c;


			if (rtValues!=null) {

				double diff_x_dat = zoomMaxRT-zoomMinRT;
				double diff_y_dat = zoomMaxMZ-zoomMinMZ;
				double diff_x_scr = getWidth();
				double diff_y_scr = getHeight();

				if (diff_x_scr<=0) { return; }
				if (diff_y_scr<=0) { return; }

				if (diff_x_scr<diff_y_scr) {
					radius = (int)java.lang.Math.round(diff_x_scr*0.01);
				} else {
					radius = (int)java.lang.Math.round(diff_y_scr*0.01);
				}
				if (radius<1) { radius=1; }


				for (int ind=0; ind<rtValues.length; ind++) {
					rt = rtValues[ind];
					mz = mzValues[ind];
					lr = lrValues[ind];
					ai = avgIntValues[ind];

					if (ai>=paramAvgIntThresholdLevel) {

						x1 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(rt-zoomMinRT) / (double)diff_x_dat));
						y1 = (int)(diff_y_scr * (1 - ((mz-zoomMinMZ) / diff_y_dat)) );

						// Calculate the shade of red or green for this logratio
						c = heatMap.getColorC((int)java.lang.Math.round(lr*10));
/*
						red = 0;
						green = 0;

						if (lr<=0) { red = 0; }
						if ( (lr>0) && (lr<paramLogratioThresholdLevel) ) {	red = (double)(lr/paramLogratioThresholdLevel);	}
						if (lr>=paramLogratioThresholdLevel) { red = 1; }

						if (lr>=0) { green = 0; }
						if ( (lr<0) && (lr>(-paramLogratioThresholdLevel)) ) {	green = (double)(lr/(-paramLogratioThresholdLevel));	}
						if (lr<=(-paramLogratioThresholdLevel)) { green = 1; }
*/
						// Draw a spot
						//g.setColor(Color.red);
						//g.setColor(new Color(red,green,(double)0.0));
						g.setColor(c);
						g.fillOval(x1-radius,y1-radius,2*radius, 2*radius);
					}
				}

				// Draw Scan cursor position
				x = cursorPositionRT;
				x2 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(x-zoomMinRT) / (double)(zoomMaxRT-zoomMinRT)));
				g.setColor(Color.red);
				g.drawLine(x2,0,x2,(int)diff_y_scr);

				// Draw MZ cursor position
				y = cursorPositionMZ;
				y2 = (int)java.lang.Math.round((double)diff_y_scr * ((double)(y-zoomMinMZ) / (double)(zoomMaxMZ-zoomMinMZ)));
				g.setColor(Color.red);
				g.drawLine(0,(int)(diff_y_scr-y2),(int)diff_x_scr,(int)(diff_y_scr-y2));

				// Draw selection
				x = mouseAreaStartRT;
				x1 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(x-zoomMinRT) / (double)(zoomMaxRT-zoomMinRT)));
				x = mouseAreaEndRT;
				x2 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(x-zoomMinRT) / (double)(zoomMaxRT-zoomMinRT)));
				y = mouseAreaStartMZ;
				y1 = (int)java.lang.Math.round((double)diff_y_scr * ((double)(y-zoomMinMZ) / (double)(zoomMaxMZ-zoomMinMZ)));
				y = mouseAreaEndMZ;
				y2 = (int)java.lang.Math.round((double)diff_y_scr * ((double)(y-zoomMinMZ) / (double)(zoomMaxMZ-zoomMinMZ)));
				g.setColor(Color.blue);
				g.drawRect(x1,(int)(diff_y_scr-y2),x2-x1,(int)(y2-y1));
			}
		}


		/**
		 * Sets data for plotting
		 */
		public void setData(int[] _alignmentRowValues, double[] _mzValues, double[] _rtValues, double[] _lrValues, double[] _avgIntValues, double _minRT, double _maxRT, double _minMZ, double _maxMZ) {

			alignmentRowValues = _alignmentRowValues;
			minMZ = _minMZ;
			maxMZ = _maxMZ;
			minRT = _minRT;
			maxRT = _maxRT;

			zoomMinMZ = minMZ;
			zoomMaxMZ = maxMZ;
			zoomMinRT = minRT;
			zoomMaxRT = maxRT;

			mzValues = _mzValues;
			rtValues = _rtValues;
			lrValues = _lrValues;
			avgIntValues = _avgIntValues;

			bottomPnl.setScale(zoomMinRT, zoomMaxRT);
			leftPnl.setScale(zoomMinMZ, zoomMaxMZ);

		}

		/**
		 * Implementation of MouseListener interface
		 */
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {

		    if (e.getButton()!=MouseEvent.BUTTON1) {
				// Not standard left-click: show pop-up menu
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
		    } else {
				// Normal left-click, clear selection
				selectionFirstClickRT = -1;
				selectionFirstClickMZ = -1;
				selectionLastClickRT = -1;
				selectionLastClickMZ = -1;
		    }
		    // Clear status bar
		    statBar.setStatusText("");
		}


		private int lastPressedButtonWas;
		/**
		 * Implementation of MouseMotionListener interface
		 */
		public void mousePressed(MouseEvent e) {

			lastPressedButtonWas = e.getButton();

			// If left button is pressed, area selection is starting
		    if (e.getButton()==MouseEvent.BUTTON1) {

				// Calculate current cursor location
				int w = getWidth();
				double diff_x_dat = zoomMaxRT-zoomMinRT;
				double diff_x_scr = w;
				double xpos = (zoomMinRT+ diff_x_dat*e.getX()/diff_x_scr);

				int h = getHeight();
				double diff_y_dat = zoomMaxMZ-zoomMinMZ;
				double diff_y_scr = h;
				double ypos = (zoomMinMZ + diff_y_dat*(double)(h - e.getY())/diff_y_scr);

				selectionFirstClickRT = xpos;
				selectionFirstClickMZ = ypos;

				mouseAreaStartRT = xpos;
				mouseAreaEndRT = xpos;
				mouseAreaStartMZ = ypos;
				mouseAreaEndMZ = ypos;

				// 	Set cursor to current location
				cursorPositionRT = xpos;
				cursorPositionMZ = ypos;

				zoomToSelectionMenuItem.setEnabled(false);

				repaint();

		    }
		    statBar.setStatusText("");

		}

		public void mouseDragged(MouseEvent e) {

			// If it wasn't left mouse button, then this is not area selection
			if (lastPressedButtonWas!=MouseEvent.BUTTON1) { return; }

			// Calculate current cursor location
			int w = getWidth();
			double diff_x_dat = zoomMaxRT-zoomMinRT;
			double diff_x_scr = w;
			double xpos = zoomMinRT+ diff_x_dat*e.getX()/diff_x_scr;

			int h = getHeight();
			double diff_y_dat = zoomMaxMZ-zoomMinMZ;
			double diff_y_scr = h;
			double ypos = (zoomMinMZ + diff_y_dat*(double)(h - e.getY())/diff_y_scr);

			// Calculate selected area
			if (selectionFirstClickRT == -1) {
				selectionFirstClickRT = xpos;
				selectionFirstClickMZ = ypos;

			} else {

				selectionLastClickRT = xpos;
				selectionLastClickMZ = ypos;

				if (selectionLastClickRT<zoomMinRT) { selectionLastClickRT = zoomMinRT; }
				if (selectionLastClickRT>(zoomMaxRT)) { selectionLastClickRT = zoomMaxRT; }

				if (selectionLastClickMZ<zoomMinMZ) { selectionLastClickMZ = zoomMinMZ; }
				if (selectionLastClickMZ>zoomMaxMZ) { selectionLastClickMZ = zoomMaxMZ; }


				if (selectionLastClickRT>selectionFirstClickRT) {
					mouseAreaStartRT = selectionFirstClickRT;
					mouseAreaEndRT = selectionLastClickRT;
				} else {
					mouseAreaStartRT = selectionLastClickRT;
					mouseAreaEndRT = selectionFirstClickRT;
				}

				if (selectionLastClickMZ>selectionFirstClickMZ) {
					mouseAreaStartMZ = selectionFirstClickMZ;
					mouseAreaEndMZ = selectionLastClickMZ;
				} else {
					mouseAreaStartMZ = selectionLastClickMZ;
					mouseAreaEndMZ = selectionFirstClickMZ;
				}

				// Enable zoom to selection in pop-up menu
				zoomToSelectionMenuItem.setEnabled(true);

				repaint();
			}
			statBar.setStatusText("");

		}

		/**
		 * Implementation of MouseMotionListener interface
		 */
		public void mouseMoved(MouseEvent e) {}

	}


	private class PlotXAxis extends JPanel {

		private final int leftMargin = 100;
		private final int rightMargin = 70;

		private double minX;
		private double maxX;
		// private int numTics;


		public PlotXAxis() {
			super();
		}


		public void paint(Graphics g) {

			super.paint(g);

						int w = getWidth();
			double h = getHeight();

			if (w<=0) { return; }
			if (h<=0) { return; }

			double dataRange = maxX-minX+1;
			double pixelsPerUnit = (double)(w-leftMargin-rightMargin) / dataRange;

			if (pixelsPerUnit<=0) { return; }

			double unitsPerTic = 0;
			while ( (unitsPerTic * pixelsPerUnit) < 60 ) { unitsPerTic++;}

			double pixelsPerTic = (double)unitsPerTic * pixelsPerUnit;
			int numoftics = (int)java.lang.Math.floor((double)dataRange / (double)unitsPerTic);


			// Draw axis
			this.setForeground(Color.black);
			g.drawLine((int)leftMargin,0,(int)(w-rightMargin),0);


			// Draw tics and numbers
			String tmps;
			double xpos = leftMargin;
			double xval = minX;
			for (int t=0; t<numoftics; t++) {
				// if (t==(numoftics-1)) { this.setForeground(Color.red); }

				tmps = "";//FormatCoordinates.formatRTValue(xval);

				g.drawLine((int)java.lang.Math.round(xpos), 0, (int)java.lang.Math.round(xpos), (int)(h/4));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)java.lang.Math.round(xpos),(int)(3*h/4));

				xval += unitsPerTic;
				xpos += pixelsPerTic;
			}
		}


		public void setScale(double _minX, double _maxX) {
			minX = _minX;
			maxX = _maxX;
			repaint();
		}

	}


	/**
	 * This class is used to draw y-axis for the plot
	 */
	private class PlotYAxis extends JPanel {

		private final double bottomMargin = (double)0.0;
		private final double topMargin = (double)0.0;

		private double minY;
		private double maxY;
		private int numTics;

		public PlotYAxis() {
			super();
		}

		public void paint(Graphics g) {

			super.paint(g);

			double w = getWidth();
			double h = getHeight();

			numTics = 5;
			if (h>250) { numTics = 10; }
			if (h>500) { numTics = 20; }
			if (h>1000) { numTics = 40; }

			this.setForeground(Color.black);
			g.drawLine((int)w-1,0,(int)w-1,(int)h);

			String tmps;


			double diff_dat = maxY-minY;
			double diff_scr = h - bottomMargin - topMargin;
			double ypos = bottomMargin;
			double yval = minY;
			for (int t=1; t<=numTics; t++) {

				tmps = "";//FormatCoordinates.formatMZValue(yval);

				g.drawLine((int)(3*w/4), (int)(h-ypos), (int)(w), (int)(h-ypos));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)(w/4)-4,(int)(h-ypos));

				yval += diff_dat / numTics;
				ypos += diff_scr / numTics;
			}
		}

		public void setScale(double _minY, double _maxY) {
			minY = _minY;
			maxY = _maxY;
			repaint();
		}

	}



	/**
	 * This class is used to draw slider for controlling colouring
	 */
	private class OptionsPanelVertical extends javax.swing.JPanel implements ChangeListener {
		private DecimalFormat tickFormat;
	    /**
	     * Creates new form PlotAreaOptions
	     */
	    public OptionsPanelVertical() {
	        initComponents();
	    }

	    /**
		 * This method is called from within the constructor to
	     * initialize the form.
	     */
	    private void initComponents() {//GEN-BEGIN:initComponents
			setLayout(new BorderLayout());

	        slideThreshold = new javax.swing.JSlider();
	        slideThreshold.setOrientation(JSlider.VERTICAL);
	        slideThreshold.setMinimum(0);
	        slideThreshold.setMaximum((int)(paramLogratioThresholdLevelMax*10));
	        slideThreshold.setValue((int)(paramLogratioThresholdLevel*10));


			// Create labels (1/10th of the actual slider value)
	        Hashtable labelTable = new Hashtable();
	        double curLabel = 0;
	        tickFormat = new DecimalFormat("0.0");
	        while (curLabel<=paramLogratioThresholdLevelMax) {
				labelTable.put(new Integer((int)(curLabel*10)), new JLabel(tickFormat.format(curLabel)));
				curLabel += 1.0;
			}
			//slideThreshold.setLabelTable(labelTable);
			//slideThreshold.setPaintLabels(true);
			slideThreshold.addChangeListener(this);

	        add(slideThreshold, BorderLayout.EAST);


	    }//GEN-END:initComponents


	    private javax.swing.JLabel lblThreshold;
	    private javax.swing.JSlider slideThreshold;

	    public void stateChanged(ChangeEvent e) {
		    JSlider source = (JSlider)e.getSource();
		    //if (!source.getValueIsAdjusting()) {
				int value = source.getValue();
		        paramLogratioThresholdLevel = (double)value/(double)10.0;

				heatmap_pal_waypoints[1] = -value;
				heatmap_pal_waypoints[3] = value;
				heatmap_pal_waypointLabels[1] = tickFormat.format(-paramLogratioThresholdLevel);
				heatmap_pal_waypointLabels[3] = tickFormat.format(paramLogratioThresholdLevel);

				heatMap.setIntensityLevels(heatmap_pal_waypoints);
				colorPnl.setIntensityLabels(heatmap_pal_waypointLabels);
				colorPnl.repaint();

		        PlotArea.repaint();
			//}
    	}

	}

	/**
	 * This class is used to draw slider for controlling intensity thresholding
	 */
	private class OptionsPanelHorizontal extends javax.swing.JPanel implements ChangeListener /*, ItemListener*/ {

		double[] thresholdLevelLabels;
		double[] thresholdLevelValues;

	    /** Creates new form PlotAreaOptions */
	    public OptionsPanelHorizontal() {
	        initComponents();
	    }


	    /**
	     * This method is called from within the constructor to
	     * initialize the form.
	     */
	    private void initComponents() {//GEN-BEGIN:initComponents
	    	setLayout(new BorderLayout());

	        lblThreshold = new javax.swing.JLabel();
	        slideThreshold = new javax.swing.JSlider();

	        lblThreshold.setText("% of strongest peaks");

	        add(slideThreshold, BorderLayout.CENTER);
	        add(lblThreshold, BorderLayout.EAST);

	    }//GEN-END:initComponents


		/**
		 * This methods sets the value to slider
		 */
		public void setupIntThresholdSlider(double[] _thresholdLevelLabels, double[] _thresholdLevelValues) {

			thresholdLevelLabels = _thresholdLevelLabels;
			thresholdLevelValues = _thresholdLevelValues;

			// Set max, min and current value
			slideThreshold.setMinimum(0);
			slideThreshold.setMaximum(thresholdLevelValues.length-1);
			slideThreshold.setValue(0);

	        Hashtable labelTable = new Hashtable();
	        DecimalFormat tickFormat = new DecimalFormat("0");
	        int tickNum = 0;

	        // Setup slider labels
	        while (tickNum<thresholdLevelLabels.length) {
				labelTable.put(new Integer(tickNum), new JLabel(tickFormat.format(100*thresholdLevelLabels[tickNum])));
				tickNum++;
			}
			slideThreshold.setLabelTable(labelTable);
			slideThreshold.setPaintLabels(true);
			slideThreshold.setSnapToTicks(true);
			slideThreshold.addChangeListener(this);

		}

		/**
		 * Decreases intensity threshold level one step
		 */
		public boolean decreaseThresholdLevel() {
			int currentState = slideThreshold.getValue();
			if (currentState==0) { return false;}
			currentState--;
			slideThreshold.setValue(currentState);
			return true;
		}

		/**
		 * Implementation of change ChangeListener interface
		 */
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			int thresholdLevelIndex;
			//if (!source.getValueIsAdjusting()) {
				thresholdLevelIndex = source.getValue();
				paramAvgIntThresholdLevel = (double)thresholdLevelValues[thresholdLevelIndex];
				PlotArea.repaint();
			//}
		}

	    // Variables declaration - do not modify//GEN-BEGIN:variables
	    private javax.swing.JPanel pnlThreshold;
	    private javax.swing.JLabel lblThreshold;
	    private javax.swing.JSlider slideThreshold;
	    // End of variables declaration//GEN-END:variables

	}



	private class RawDataPlaceHolder {
		private String niceName;
		private int rawDataID;

		public RawDataPlaceHolder(String _niceName, int _rawDataID) {
			niceName = _niceName;
			rawDataID = _rawDataID;
		}

		public String toString() {
			return niceName;
		}

		public int getRawDataID() {
			return rawDataID;
		}
	}

}