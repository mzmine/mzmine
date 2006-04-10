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
package net.sf.mzmine.rawdatavisualizers;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


// Java packages
import java.util.*;
import java.text.DecimalFormat;

import javax.print.attribute.standard.*;
import javax.print.attribute.HashPrintRequestAttributeSet;

import java.awt.*;
import java.awt.print.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.geom.Point2D;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.datatransfer.Clipboard;

import javax.swing.*;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;


/**
 * This class defines the total ion chromatogram visualizer for raw data
 */
public class RawDataVisualizerTICView extends JInternalFrame implements RawDataVisualizer, Printable, InternalFrameListener {

	// Is this visualizer showing a TIC or XIC?
    private static final int TIC_MODE = 1;
    private static final int XIC_MODE = 2;
	private int ticXicMode= TIC_MODE;

	// Components of the window
    private JPanel bottomPnl, leftPnl, rightPnl, topPnl;
    private TICPlot ticPlot;

    private RawDataAtClient rawData;
    //private Vector dataPoints;

	// Mouse selection
    private int mouseAreaStart;
    private int mouseAreaEnd;

    private Statusbar statBar;
    private MainWindow mainWin;
    private ItemSelector itemSelector;

	private boolean firstRefreshAlreadyDone = false;


	/**
	 * Constructor for total ion chromatogram visualizer
	 * @param	_mainWin	Main window of the application: the visualizer will be placed as an internalframe inside this main window.
	 */
    public RawDataVisualizerTICView(MainWindow _mainWin) {

		// Store mainWin and retrieve a couple of GUI components which are often needed here
		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();
		itemSelector = mainWin.getItemSelector();

		// Build this visualizer
		getContentPane().setLayout(new BorderLayout());

		bottomPnl = new TICXAxis();
		bottomPnl.setMinimumSize(new Dimension(getWidth(),25));
		bottomPnl.setPreferredSize(new Dimension(getWidth(),25));
		bottomPnl.setBackground(Color.white);
		getContentPane().add(bottomPnl, java.awt.BorderLayout.SOUTH);

		topPnl = new JPanel();
		topPnl.setMinimumSize(new Dimension(getWidth(),5));
		topPnl.setPreferredSize(new Dimension(getWidth(),5));
		topPnl.setBackground(Color.white);
		getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);

		leftPnl = new TICYAxis();
		leftPnl.setMinimumSize(new Dimension(50, getHeight()));
		leftPnl.setPreferredSize(new Dimension(50, getHeight()));
		leftPnl.setBackground(Color.white);
		getContentPane().add(leftPnl, java.awt.BorderLayout.WEST);

		rightPnl = new JPanel();
		rightPnl.setMinimumSize(new Dimension(5, getHeight()));
		rightPnl.setPreferredSize(new Dimension(5, getHeight()));
		rightPnl.setBackground(Color.white);
		getContentPane().add(rightPnl, java.awt.BorderLayout.EAST);

		ticPlot = new TICPlot(this);
		ticPlot.setBackground(Color.white);
		getContentPane().add(ticPlot, java.awt.BorderLayout.CENTER);
		ticPlot.setVisible(true);

		addInternalFrameListener(this);

		setTitle("-: Total ion chromatogram");
		setResizable( true );
		setIconifiable( true );
    }

	/**
	 * Assign a run object for visualizer
	 * @param	theData		Visualizer will display total ion chromatogram of this dataset
	 */
    public void setRawData(RawDataAtClient _rawData) {
		rawData = _rawData;
    }

	public RawDataAtClient getRawData() {
		return rawData;
	}


	/**
	 * Returns number of the first scan and the last scan needed in displaying the TIC view with current zoom settings
	 */
	public RawDataVisualizerRefreshRequest beforeRefresh(RawDataVisualizerRefreshRequest refreshRequest) {

		// Determine parameters for refreshRequest
		// ---------------------------------------

		// If this it not the very first refresh for the visualizer, it is not necessary to get any raw data for some change types
		if (firstRefreshAlreadyDone) {
			// Change in peak information doesn't affect TIC/XIC visualizer
			if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_PEAKS) { refreshRequest.ticNeedsRawData = false; return refreshRequest; }

			// Change in cursor position doesn't require any new data for TIC/XIC
			if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_MZ) { refreshRequest.ticNeedsRawData = false; return refreshRequest; }
			if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_SCAN) { refreshRequest.ticNeedsRawData = false; return refreshRequest; }
			if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_BOTH) { refreshRequest.ticNeedsRawData = false; return refreshRequest; }

			// In TIC mode, change in m/z zoom has no effect.
			if ((ticXicMode==TIC_MODE) && (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_SELECTION_MZ)) { refreshRequest.ticNeedsRawData = false; return refreshRequest; }
		}

		firstRefreshAlreadyDone = true;

		// None of above cases, some scans are needed to refresh this TIC/XIC
		refreshRequest.ticNeedsRawData = true;
		if (ticXicMode==TIC_MODE) { refreshRequest.ticMode = RawDataVisualizerRefreshRequest.MODE_TIC; }
		if (ticXicMode==XIC_MODE) {
			refreshRequest.ticMode = RawDataVisualizerRefreshRequest.MODE_XIC;

			// If current m/z zoom range is defined, then XIC will be calculated over this range
			if (rawData.getSelectionMZStart()!=-1) {
				refreshRequest.ticStartMZ = rawData.getSelectionMZStart();
				refreshRequest.ticStopMZ = rawData.getSelectionMZEnd();
			} else {
				// Otherwise, XIC is calculated using the full m/z range of the data set.
				refreshRequest.ticStartMZ = rawData.getDataMinMZ();
				refreshRequest.ticStopMZ = rawData.getDataMaxMZ();
			}
		}

		// If scan zoom range has been defined, then all scans in that range are needed
		if (rawData.getSelectionScanStart()!=-1) {
			refreshRequest.ticStartScan = rawData.getSelectionScanStart();
			refreshRequest.ticStopScan = rawData.getSelectionScanEnd();
		} else {
			// If no scan zoom range, then all scans are required
			refreshRequest.ticStartScan = 0;
			refreshRequest.ticStopScan = rawData.getNumOfScans()-1;
		}


		// Setup string for window title
		// -----------------------------

		// For XIC, M/Z range must be in title line
		if (ticXicMode==XIC_MODE) {

			FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());

			// Setup window title for a XIC-type visualizer
			String startStr = formatCoordinates.formatMZValue(refreshRequest.ticStartMZ);
			String stopStr = formatCoordinates.formatMZValue(refreshRequest.ticStopMZ);
			setTitle("" + rawData.getNiceName() + ": eXtracted ion chromatogram, MZ range from " + startStr + " to " + stopStr + "");

		} else {

			// Setup window title for a TIC-type visualizer
			setTitle("" + rawData.getNiceName() + ": Total ion chromatogram");
		}

		return refreshRequest;

	}


	/**
	 *	Implementation of refreshFinalize method (Visualizer interface)
	 */
	public void afterRefresh(RawDataVisualizerRefreshResult refreshResult) {

		// Change in peaks doesn't require any action
		if (refreshResult.changeType==RawDataVisualizer.CHANGETYPE_PEAKS) { return; }

		// Change in MZ cursor position doesn't require any action
		if (refreshResult.changeType==RawDataVisualizer.CHANGETYPE_CURSORPOSITION_MZ) { return; }

		// When in TIC mode, change in MZ selection doesn't require any action
		if ((ticXicMode==TIC_MODE) && (refreshResult.changeType==RawDataVisualizer.CHANGETYPE_SELECTION_MZ)) { return; }

		// Scan cursor position changed, no action (repaint needed, but called elsewhere)
		if ((refreshResult.changeType==RawDataVisualizer.CHANGETYPE_CURSORPOSITION_SCAN) || (refreshResult.changeType==RawDataVisualizer.CHANGETYPE_CURSORPOSITION_BOTH)) { return; }



		// Refresh the plot and coordinate axis with the new data
		if (refreshResult.ticScanNumbers != null) {

			ticPlot.setData(rawData,
							refreshResult.ticScanNumbers,
							refreshResult.ticIntensities,
							refreshResult.ticScanNumbers[0],
							refreshResult.ticScanNumbers[refreshResult.ticScanNumbers.length-1],
							0,
							refreshResult.ticMaxIntensity);
			((TICXAxis)bottomPnl).setScale(refreshResult.ticScanNumbers[0], refreshResult.ticScanNumbers[refreshResult.ticScanNumbers.length-1]);
			((TICYAxis)leftPnl).setScale(0, refreshResult.ticMaxIntensity);
		} else {
		}

	}


	/**
	 * Return TIC/XIC mode which indicates wheter this visualizer is displaying TIC or XIC
	 */
    private int getTicXicMode() {
		return ticXicMode;
	}

	/**
	 * Set TIC/XIC mode. This method will launch immediate background refresh of this visualizer.
	 */
	private void setTicXicMode(int _mode) {
		ticXicMode = _mode;

		RawDataAtClient[] tmpRawDatas = new RawDataAtClient[1];
		tmpRawDatas[0] = rawData;
		mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, rawData.getRawDataID());

/*
		BackgroundThread bt = new BackgroundThread(mainWin, msRun, this, Visualizer.CHANGETYPE_SELECTION_BOTH, BackgroundThread.TASK_REFRESHVISUALIZER);
		bt.start();
*/
	}

	/**
	 * Set TIC/XIC mode. This method will not refresh the visualizer automatically.
	 */
	private void setTicXicModeQuiet(int _mode) {
		ticXicMode = _mode;

	}


	/**
	 * Implementation of the printMe() method (Visualizer interface)
	 */
	public void printMe() {

		// Set default printer parameters
		PrinterJob printJob = PrinterJob.getPrinterJob();
		HashPrintRequestAttributeSet pSet = new HashPrintRequestAttributeSet();
		pSet.add(OrientationRequested.LANDSCAPE);

		// Open print dialog and initiate print job if user confirms
		if (printJob.printDialog(pSet)) {
			printJob.setPrintable(this);
			try {
				printJob.print(pSet);
			} catch (Exception PrintException) {}
		}
	}

	/**
	 * This method is used for drawing the window contents in a separate buffer which will then go to printer.
	 */
	public int print(Graphics g, PageFormat pf, int pi){
		double sx, sy;
		final int titleHeight = 30;

		// Since this visualizer will be printed on a single page, don't try to print pages 2,3,4,...
		if (pi > 0) { return NO_SUCH_PAGE; }

		// Prepare given buffer for drawing the plot in it
		Graphics2D g2 = (Graphics2D)g;
		g2.translate(pf.getImageableX(), pf.getImageableY());

		// Print title of this visualizer
		g2.drawString(this.getTitle(),0,titleHeight-5);

		// Setup transform so that plot will fit on page
		g2.translate(0, titleHeight);

		sx = (double)pf.getImageableWidth()/(double)getContentPane().getWidth();
		sy = (double)(pf.getImageableHeight()-titleHeight)/(double)getContentPane().getHeight();

		g2.transform(AffineTransform.getScaleInstance(sx,sy));

		// Disabling double buffering increases print quality
		RepaintManager currentManager = RepaintManager.currentManager(getContentPane());
		currentManager.setDoubleBufferingEnabled(false);

		// Draw this visualizer to the buffer
		getContentPane().paint(g2);

		// Enable double buffering again (good for screen output)
		currentManager.setDoubleBufferingEnabled(true);

		// Return page ready status
		return Printable.PAGE_EXISTS;

	}

	/**
	 * Implementation of the copyMe() method (Visualizer interface)
	 */
	public void copyMe() {
		// Initialize clipboard
		Toolkit toolkit =    Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();

		// Draw visualizer graphics in a buffered image
		int w = getContentPane().getWidth();
		int h = getContentPane().getHeight();
		BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bi.getGraphics();
		getContentPane().paint(g);

		// Put image to clipboard
		clipboard.setContents(new TransferableImage(bi),null);
	 }


	/**
	 * Implementation of the closeMe() method (Visualizer interface)
	 */
/*
	public void closeMe() {
		mainWin.removeVisualizer(this);
		dispose();
		rawData = null;
		statBar = null;
		mainWin = null;
		itemSelector = null;
	}
*/
	/**
	 * Implementation of methods in InternalFrameListener
	 */
	public void internalFrameActivated(InternalFrameEvent e) {

		//rawData.setActiveVisualizer(this);
		itemSelector.setActiveRawData(rawData);

	}
	public void internalFrameClosed(InternalFrameEvent e) {	}
	public void internalFrameClosing(InternalFrameEvent e) { }
	public void internalFrameDeactivated(InternalFrameEvent e) { }
	public void internalFrameDeiconified(InternalFrameEvent e) { }
	public void internalFrameIconified(InternalFrameEvent e) { }
	public void internalFrameOpened(InternalFrameEvent e) { }



	/**
	 * This subclass handles the drawing of actual plot
	 */
	class TICPlot extends JPanel implements java.awt.event.ActionListener, java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

		private RawDataAtClient rawData;

		private double minY, maxY;
		private int minX, maxX;
		private int[] pointsX;
		private double[] pointsY;

		private int selectionFirstClick;
		private int selectionLastClick;

		private JPopupMenu popupMenu;
		private JMenuItem zoomToSelectionMenuItem;
		private JMenuItem zoomOutMenuItem;
		private JMenuItem zoomOutLittleMenuItem;
		private JMenuItem zoomSameToOthersMenuItem;
		private JMenuItem changeTicXicModeMenuItem;
		private JMenuItem defineXicMenuItem;

		private RawDataVisualizerTICView masterFrame;

		/**
		 * Constructor: initializes the plot panel
		 *
		 */
		public TICPlot(RawDataVisualizerTICView _masterFrame) {

			masterFrame = _masterFrame;

		    // Create popup-menu
		    popupMenu = new JPopupMenu();
		    zoomToSelectionMenuItem = new JMenuItem("Zoom to selection");
		    zoomToSelectionMenuItem .addActionListener(this);
		    zoomToSelectionMenuItem.setEnabled(false);
		    popupMenu.add(zoomToSelectionMenuItem);

		    zoomOutMenuItem = new JMenuItem("Zoom out full");
		    zoomOutMenuItem .addActionListener(this);
		    popupMenu.add(zoomOutMenuItem);

		    zoomOutLittleMenuItem = new JMenuItem("Zoom out little");
		    zoomOutLittleMenuItem.addActionListener(this);
			popupMenu.add(zoomOutLittleMenuItem);

		    zoomSameToOthersMenuItem = new JMenuItem("Set same zoom to other raw data viewers");
			zoomSameToOthersMenuItem.addActionListener(this);
		    popupMenu.add(zoomSameToOthersMenuItem);

			popupMenu.addSeparator();

		    changeTicXicModeMenuItem = new JMenuItem();
		    if (masterFrame.getTicXicMode()==TIC_MODE) {
				changeTicXicModeMenuItem.setText("Switch to XIC");
			} else {
				changeTicXicModeMenuItem.setText("Switch to TIC");
			}
		    changeTicXicModeMenuItem.addActionListener(this);
		    popupMenu.add(changeTicXicModeMenuItem);

			defineXicMenuItem = new JMenuItem("Define range for XIC");
			defineXicMenuItem.addActionListener(this);

		  	popupMenu.add(defineXicMenuItem);

		    selectionFirstClick = -1;
		    selectionLastClick = -1;

		    addMouseListener(this);
		    addMouseMotionListener(this);
		}


		/**
		 * This method paints the plot to this panel
		 */
		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

			super.paint(g);

			if (pointsX!=null) {

				int w = getWidth();
				double h = getHeight();

				int x;
				double y;
				int prevx;
				double prevy;

				int x1,x2,y1,y2;
				prevx = minX;
				prevy = minY;

				int diff_x_dat = maxX-minX;
				double diff_y_dat = maxY-minY;
				int diff_x_scr = w;
				double diff_y_scr = h;

				// Draw selection
				x1 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(mouseAreaStart-minX) / (double)diff_x_dat));
				x2 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(mouseAreaEnd-minX) / (double)diff_x_dat));
				y1 = (int)(h-diff_y_scr * ((minY-minY) / diff_y_dat));
				y2 = (int)(h-diff_y_scr * ((maxY-minY) / diff_y_dat));
				g.setColor(Color.lightGray);
				g.fillRect(x1,y2,x2-x1, y1-y2); //x2-x1,y1-y2);

				// Draw linegraph
				g.setColor(Color.blue);
				for (int ind=0; ind<pointsX.length; ind++) {
					x = (int)pointsX[ind];
					y = pointsY[ind];

					x1 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(prevx-minX) / (double)diff_x_dat));
					x2 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(x-minX) / (double)diff_x_dat));
					y1 = (int)(h-diff_y_scr * ((prevy-minY) / diff_y_dat));
					y2 = (int)(h-diff_y_scr * ((y-minY) / diff_y_dat));

					g.drawLine(x1,y1,x2,y2);

					prevx = x;
					prevy = y;
				}

				// Draw cursor position
				x = rawData.getCursorPositionScan();
				prevy = minY;
				y = maxY;
				x2 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(x-minX) / (double)diff_x_dat));
				y1 = (int)(h-diff_y_scr * ((prevy-minY) / diff_y_dat));
				y2 = (int)(h-diff_y_scr * ((y-minY) / diff_y_dat));
				g.setColor(Color.red);
				g.drawLine(x2,y1,x2,y2);

			}

		}


		/**
		 * Set data points and scales to the plot
		 */
		public void setData(RawDataAtClient _rawData, int[] _pointsX, double[] _pointsY, int _minX, int _maxX, double _minY, double _maxY) {

			rawData = _rawData;
			pointsX = _pointsX;
			pointsY = _pointsY;

			minX = _minX;
			maxX = _maxX;
			minY = _minY;
			maxY = _maxY;

		}


		/**
		 * Implementation of ActionListener interface for this panel
		 */
		public void actionPerformed(java.awt.event.ActionEvent e) {
			Object src = e.getSource();

			// Zoom into selected scan region
			if (src == zoomToSelectionMenuItem) {

				// Set run's scan range to current selected area in this plot
				rawData.setSelectionScan(mouseAreaStart, mouseAreaEnd);

				// Clear selected area in the plot
				mouseAreaStart = rawData.getCursorPositionScan();
				mouseAreaEnd = rawData.getCursorPositionScan();
				zoomToSelectionMenuItem.setEnabled(false);


				// Refresh all visualizers
				RawDataAtClient[] tmpRawDatas = new RawDataAtClient[1];
				tmpRawDatas[0] = rawData;
				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, rawData.getRawDataID());
/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_SCAN, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
*/
			}

			// Show whole scan region
			if (src == zoomOutMenuItem) {

				// Clear run's scan range: reset to full scan range
				rawData.clearSelectionScan();

				// Clear selected area in the plot
				mouseAreaStart = rawData.getCursorPositionScan();
				mouseAreaEnd = rawData.getCursorPositionScan();

				// Refresh all visualizers
				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, rawData.getRawDataID());

/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_SCAN, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
*/
			}

			// Zoom out to little wider scan region
			if (src == zoomOutLittleMenuItem) {

				// Calculate boundaries of the wider scan region based on the current region
				int midX = (int)(java.lang.Math.round( (double)(minX+maxX)/(double)2 ) );
				int tmpMinX, tmpMaxX;

				if (((midX-minX)>0) && ((maxX-midX)>0)) {
					tmpMinX = (int)(java.lang.Math.round(midX - (midX-minX)*1.5));
					tmpMaxX = (int)(java.lang.Math.round(midX + (maxX-midX)*1.5));
				} else {
					tmpMinX = minX - 1;
					tmpMaxX = maxX + 1;
				}

				if (tmpMinX<0) {tmpMinX = 0;}
				if (tmpMaxX>(rawData.getNumOfScans()-1)) { tmpMaxX = rawData.getNumOfScans()-1; }

				// Set run's scan range to selected values
				rawData.setSelectionScan(tmpMinX, tmpMaxX);

				// Clear selected area in the plot
				mouseAreaStart = rawData.getCursorPositionScan();
				mouseAreaEnd = rawData.getCursorPositionScan();
				zoomToSelectionMenuItem.setEnabled(false);

				// Refresh all visualizers
				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, rawData.getRawDataID());

/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_SCAN, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
*/
			}

			// Copy same scan range settings to all other open runs
			if (src == zoomSameToOthersMenuItem) {

				mainWin.setSameZoomToOtherRawDatas(rawData, masterFrame.mainWin.SET_SAME_ZOOM_SCAN);

			}

			// Switch this visualizer between TIC and XIC modes
			if (src == changeTicXicModeMenuItem) {

				if (masterFrame.getTicXicMode()==TIC_MODE) {
					// Switch to XIC view
					masterFrame.setTicXicMode(XIC_MODE);
					changeTicXicModeMenuItem.setText("Switch to TIC");
				} else {
					// Switch to TIC view
					masterFrame.setTicXicMode(TIC_MODE);
					changeTicXicModeMenuItem.setText("Switch to XIC");
				}

			}

			// Show a dialog where user can select range for XIC and switch to that XIC
			if (src == defineXicMenuItem) {

				// Default range is cursor location +- 0.25
				double ricMZ = rawData.getCursorPositionMZ();
				double ricMZDelta = (double)0.25;

				// Show dialog
				ParameterSetupDialogForDefineXIC psd = new ParameterSetupDialogForDefineXIC (mainWin, "Please give centroid and delta MZ values for XIC", ricMZ, ricMZDelta);
				psd.setVisible(true);
				// if cancel was clicked
				if (psd.getExitCode()==-1) {
					statBar.setStatusText("Switch to XIC cancelled.");
					return;
				}

				// Validate given parameter values
				double d;
				int i;
				d = psd.getXicMZ();
				if (d<0) {
					statBar.setStatusText("Error: incorrect parameter values.");
					return;
				}
				ricMZ = d;

				d = psd.getXicMZDelta();
				if (d<0) {
					statBar.setStatusText("Error: incorrect parameter values.");
					return;
				}
				ricMZDelta = d;

				// Set run's mz range
				rawData.setSelectionMZ(ricMZ-ricMZDelta, ricMZ+ricMZDelta);

				// Swtich to XIC mode
				masterFrame.setTicXicModeQuiet(XIC_MODE);
				changeTicXicModeMenuItem.setText("Switch to TIC");

				// Refresh all visualizers
				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ, rawData.getRawDataID());

/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_MZ, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
*/
			}
		}


		/**
		 * Implementation of MouseListener interface methods
		 */
		public void mouseClicked(MouseEvent e) { statBar.setStatusText("");	}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {

			// If it wasn't normal click with first mouse button
		    if (e.getButton()!=MouseEvent.BUTTON1) {
				// then show pop up menu
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			} else {
				// else clear the selection
				selectionFirstClick = -1;
				selectionLastClick = -1;
		    }
		}

		private int lastButtonPressedWas;
		public void mousePressed(MouseEvent e) {

			lastButtonPressedWas = e.getButton();

			// Only interested about normal first mouse button clicks here
		    if (e.getButton()==MouseEvent.BUTTON1) {
				// Calculate scan number corresponding to the x coordinate of mouse cursor when mouse was clicked
				int w = getWidth();
				double diff_x_dat = maxX-minX;
				double diff_x_scr = w;
				int xpos = (int)java.lang.Math.round((minX+ diff_x_dat*e.getX()/diff_x_scr));
				selectionFirstClick = xpos;

				// Clear selected area in the plot
				mouseAreaStart = xpos;
				mouseAreaEnd = xpos;
				zoomToSelectionMenuItem.setEnabled(false);

				// Set run's cursor location over the clicked scan
				rawData.setCursorPositionScan(xpos);
				statBar.setCursorPosition(rawData);

				// Refresh visualizers
				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_CURSORPOSITION_SCAN, rawData.getRawDataID());
/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_CURSORPOSITION_SCAN, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
*/

		    }
		    statBar.setStatusText("");

		}

		/**
		 * Implementation of methods for MouseMotionListener interface
		 */
		public void mouseDragged(MouseEvent e) {

			if (lastButtonPressedWas!=MouseEvent.BUTTON1) { return; }

			// Calculate scan number corresponding to the x coordinate of mouse cursor
			int w = getWidth();
			double diff_x_dat = maxX-minX;
			double diff_x_scr = w;
			int xpos = (int)java.lang.Math.round(minX+ diff_x_dat*e.getX()/diff_x_scr);

			// If area selection process is not underway, then start it and set cursor to the starting point of area selection
			if (selectionFirstClick == -1) {
			    selectionFirstClick = xpos;
			    rawData.setCursorPositionScan(xpos);
			} else {

				// Otherwise, set the end point of selected area to current cursor location
			    selectionLastClick = xpos;

				// Make sure that the area doesn't extend over the full scan range of this run
			    if (selectionLastClick<0) {	selectionLastClick = 0;	}
			    if (selectionLastClick>=rawData.getNumOfScans()) {
				selectionLastClick = rawData.getNumOfScans()-1;
			    }

				// Sort selection first and last click point in acceding order for drawing the selection
			    if (selectionLastClick>selectionFirstClick) {
					mouseAreaStart = selectionFirstClick;
					mouseAreaEnd = selectionLastClick;
			    } else {
					mouseAreaStart = selectionLastClick;
					mouseAreaEnd = selectionFirstClick;
			    }

			    // There is now some selection made and "zoom to selection" item should be enabled in the pop-up menu
			    zoomToSelectionMenuItem.setEnabled(true);
			}

			statBar.setStatusText("");
			repaint();

		}
		public void mouseMoved(MouseEvent e) {}
	}

	/**
	 * This class presents the x-axis of the plot
	 */
	class TICXAxis extends JPanel {

		private final int leftMargin = 50;
		private final int rightMargin = 5;

		private int minX;
		private int maxX;

		private DecimalFormat tickFormat;


		/**
		 * Constructor
		 */
		public TICXAxis() {
			super();
			tickFormat = new DecimalFormat("0.00");
		}

		/**
		 * This method paints the x-axis
		 */
		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

			super.paint(g);

			FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());

			// Calc some dimensions with depend on the panel width (in pixels) and plot area (in scans)
			int w = getWidth();
			double h = getHeight();

			/// - number of pixels per scan
			int numofscans = maxX-minX;
			double pixelsperscan = (double)(w-leftMargin-rightMargin) / (double)numofscans;

			// - number of scans between tic marks
			int scanspertic = 1;
			while ( (scanspertic * pixelsperscan) < 60 ) { scanspertic++;}

			// - number of pixels between tic marks
			double pixelspertic = (double)scanspertic * pixelsperscan;
			int numoftics = (int)java.lang.Math.floor((double)numofscans / (double)scanspertic);

			// Draw axis
			this.setForeground(Color.black);
			g.drawLine((int)leftMargin,0,(int)(w-rightMargin),0);

			// Draw tics and numbers
			String tmps;
			double xpos = leftMargin;
			int xval = minX;
			for (int t=0; t<numoftics; t++) {
				// if (t==(numoftics-1)) { this.setForeground(Color.red); }

				tmps = formatCoordinates.formatRTValue(xval, rawData);

				g.drawLine((int)java.lang.Math.round(xpos), 0, (int)java.lang.Math.round(xpos), (int)(h/4));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)java.lang.Math.round(xpos),(int)(3*h/4));

				xval += scanspertic;
				xpos += pixelspertic;

			}
		}

		/**
		 * Set axis scale
		 */
		public void setScale(int _minX, int _maxX) {
			minX = _minX;
			maxX = _maxX;
		}

	}



	/**
	 * This class presents the y-axis of the plot
	 */
	class TICYAxis extends JPanel {

		private final double bottomMargin = (double)0.0;
		private final double topMargin = (double)0.0;

		private double minY;
		private double  maxY;
		private int numTics;

		private DecimalFormat tickFormat;

		/**
		 * Constructor
		 */
		public TICYAxis() {
			super();
			tickFormat = new DecimalFormat("0.##E0");
		}


		/**
		 * This method paints the y-axis
		 */
		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

			super.paint(g);

			double w = getWidth();
			double h = getHeight();

			// Setup number of tics depending on how tall the panel is
			numTics = 5;
			if (h>250) { numTics = 10; }
			if (h>500) { numTics = 20; }
			if (h>1000) { numTics = 40; }

			// Draw axis
			this.setForeground(Color.black);
			g.drawLine((int)w-1,0,(int)w-1,(int)h);



			// Draw tics and numbers
			String tmps;
			double diff_dat = maxY-minY;
			double diff_scr = h - bottomMargin - topMargin;
			double ypos = bottomMargin;
			double yval = minY;
			for (int t=1; t<=numTics; t++) {
				// tmps = new String("" + (int)yval);
				tmps = new String(tickFormat.format(yval));
				g.drawLine((int)(3*w/4), (int)(h-ypos), (int)(w), (int)(h-ypos));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)(w/4)-4,(int)(h-ypos));

				yval += diff_dat / numTics;
				ypos += diff_scr / numTics;
			}
		}

		/**
		 * Set scale of the axis
		 */
		public void setScale(double _minY, double _maxY) {
			minY = _minY;
			maxY = _maxY;
		}

	}



	private final int RETVAL_USER_CANCEL = -1;
	private final int RETVAL_INCORRECT_PARAMS = -2;
	private final int RETVAL_OK = 1;

	/**
	 * This class defines the parameter setup dialog used to query parameterts in "Define XIC" option
	 */
	class ParameterSetupDialogForDefineXIC extends JDialog implements ActionListener{
		private JTextField txtXicMZ;
		private JTextField txtXicMZDelta;

		private JButton okBtn;
		private JButton cancelBtn;

		private int exitCode;


		/**
		 * Constructor
		 */
		public ParameterSetupDialogForDefineXIC(MainWindow _mainWin, String title, double _ricMZ, double _ricMZDelta) {
			super(_mainWin, title, true);
			exitCode = 0;

			DecimalFormat strFormatter;
			strFormatter = new DecimalFormat("0.000");
			//startStr = new String(strFormatter.format(dataStartMZ))

			// MZ value
			JLabel ricMZLabel = new JLabel("MZ");
			//txtXicMZ= new JTextField(new String(strFormatter.format(_ricMZ)));
			txtXicMZ= new JTextField("" + java.lang.Math.round(_ricMZ*1000)/1000.0);

			// MZ delta value
			JLabel ricMZDeltaLabel= new JLabel("MZ delta");
			//txtXicMZDelta= new JTextField(new String(strFormatter.format(_ricMZDelta)));
			txtXicMZDelta= new JTextField("" + _ricMZDelta);


			JPanel fields = new JPanel();
			fields.setLayout(new GridLayout(2,2));
			fields.add(ricMZLabel);
			fields.add(txtXicMZ);
			fields.add(ricMZDeltaLabel);
			fields.add(txtXicMZDelta);

			// Buttons
			JPanel btnPanel = new JPanel();
			okBtn = new JButton("OK");
			cancelBtn = new JButton("Cancel");

			btnPanel.add(okBtn);
			btnPanel.add(cancelBtn);
			okBtn.addActionListener(this);
			cancelBtn.addActionListener(this);

			// Add it
			getContentPane().add(fields, BorderLayout.CENTER);
			getContentPane().add(btnPanel, BorderLayout.SOUTH);

			setLocationRelativeTo(_mainWin);
			// setPosition(_mainWin.getX()+_mainWin.getWidth()/2, _mainWin.getY()+_mainWin.getHeight()/2);
/*
			setSize(512, 256);
			setResizable( true );
*/
			pack();
		}

		public void actionPerformed(java.awt.event.ActionEvent ae) {
			Object src = ae.getSource();
			if (src==okBtn) {
				exitCode = 1;
				setVisible(false);
			}
			if (src==cancelBtn) {
				exitCode = -1;
				setVisible(false);
			}

		}

		public double getXicMZ() {
			String s = txtXicMZ.getText();
			double d;
			try {
				d = Double.parseDouble(s);
			} catch (NumberFormatException exe) {
				return -1;
			}
			return d;
		}

		public double getXicMZDelta() {
			String s = txtXicMZDelta.getText();
			double d;
			try {
				d = Double.parseDouble(s);
			} catch (NumberFormatException exe) {
				return -1;
			}
			return d;
		}


		public int getExitCode() {
			return exitCode;
		}
	}



}



