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
package net.sf.mzmine.visualizers.rawdata;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.RepaintManager;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.methods.peakpicking.Peak;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.userinterface.ItemSelector;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.userinterface.Statusbar;
import net.sf.mzmine.util.FormatCoordinates;
import net.sf.mzmine.util.GeneralParameters;
import net.sf.mzmine.util.TransferableImage;


public class RawDataVisualizerSpectrumView extends JInternalFrame implements RawDataVisualizer, Printable, InternalFrameListener {

	private JPanel bottomPnl, leftPnl, rightPnl, topPnl;
	private ScanPlot scanPlot;

	private RawDataAtClient rawData;
	//private Vector dataPoints;

	private double mouseAreaStart;
	private double mouseAreaEnd;

	private int spectraCombinationStartScan;
	private int spectraCombinationStopScan;

	protected final int ONE_SPECTRUM_MODE = 1;
	protected final int COMBINATION_SPECTRA_MODE = 2;
	private int spectrumSpectraMode=ONE_SPECTRUM_MODE;

	private Statusbar statBar;
	private MainWindow mainWin;
	private ItemSelector itemSelector;



	public RawDataVisualizerSpectrumView(MainWindow _mainWin) {

		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();
		itemSelector = mainWin.getItemSelector();

		getContentPane().setLayout(new BorderLayout());

		bottomPnl = new ScanXAxis();
		bottomPnl.setMinimumSize(new Dimension(getWidth(),25));
		bottomPnl.setPreferredSize(new Dimension(getWidth(),25));
		bottomPnl.setBackground(Color.white);
		getContentPane().add(bottomPnl, java.awt.BorderLayout.SOUTH);

		topPnl = new JPanel();
		topPnl.setMinimumSize(new Dimension(getWidth(),5));
		topPnl.setPreferredSize(new Dimension(getWidth(),5));
		topPnl.setBackground(Color.white);
		getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);

		leftPnl = new ScanYAxis();
		leftPnl.setMinimumSize(new Dimension(50, getHeight()));
		leftPnl.setPreferredSize(new Dimension(50, getHeight()));
		leftPnl.setBackground(Color.white);
		getContentPane().add(leftPnl, java.awt.BorderLayout.WEST);

		rightPnl = new JPanel();
		rightPnl.setMinimumSize(new Dimension(5, getHeight()));
		rightPnl.setPreferredSize(new Dimension(5, getHeight()));
		rightPnl.setBackground(Color.white);
		getContentPane().add(rightPnl, java.awt.BorderLayout.EAST);

		scanPlot = new ScanPlot(this);
		scanPlot.setBackground(Color.white);
		getContentPane().add(scanPlot, java.awt.BorderLayout.CENTER);

		scanPlot.setVisible(true);

		addInternalFrameListener(this);

		setTitle("-: Spectrum");
		setVisible( true );
        setSize(630, 240);
        setResizable( true );
        setIconifiable( true );
	}

	public void setRawData(RawDataAtClient _rawData) {

		// Retrieve scan at cursor's position
		rawData = _rawData;

	}

	public RawDataAtClient getRawData() {
		return rawData;
	}


	public RawDataVisualizerRefreshRequest beforeRefresh(RawDataVisualizerRefreshRequest refreshRequest) {


		// Change in MZ cursor, no need to refresh
		if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_MZ) {
			refreshRequest.spectrumNeedsRawData = false;
			return refreshRequest;
		}

		// When in combination mode, change in RT cursor position (or both RT & MZ cursor pos.) doesn't need refreshing
		if (spectrumSpectraMode==COMBINATION_SPECTRA_MODE) {
			if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_SCAN) {
				refreshRequest.spectrumNeedsRawData = false;
				return refreshRequest;
			}
			if (refreshRequest.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_BOTH) {
				refreshRequest.spectrumNeedsRawData = false;
				return refreshRequest;
			}

		}

		// There seems to be some need for refresh
		refreshRequest.spectrumNeedsRawData = true;

		// If MZ area is zoomed
		if (rawData.getSelectionMZStart()!=-1) {
			// then request datapoints within that MZ range
			refreshRequest.spectrumStartMZ = rawData.getSelectionMZStart();
			refreshRequest.spectrumStopMZ = rawData.getSelectionMZEnd();
		} else {
			// otherwise request datapoints within the whole MZ range of the raw data
			refreshRequest.spectrumStartMZ = rawData.getDataMinMZ();
			refreshRequest.spectrumStopMZ = rawData.getDataMaxMZ();
		}

		FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());

		if (spectrumSpectraMode==ONE_SPECTRUM_MODE) {
			refreshRequest.spectrumMode = RawDataVisualizerRefreshRequest.MODE_SINGLESPECTRUM;
			refreshRequest.spectrumStartScan = rawData.getCursorPositionScan();
			refreshRequest.spectrumStopScan = rawData.getCursorPositionScan();


			String tmps = formatCoordinates.formatRTValue(rawData.getCursorPositionScan(), rawData);
			setTitle("" + rawData.getNiceName() + ": Spectrum, time " + tmps);

		}

		if (spectrumSpectraMode==COMBINATION_SPECTRA_MODE) {

			refreshRequest.spectrumMode = RawDataVisualizerRefreshRequest.MODE_COMBINEDSPECTRA;

			// If there is no selected scan range
			if (rawData.getSelectionScanStart()==-1) {
					// Then combine all available scans
					refreshRequest.spectrumStartScan = 0;
					refreshRequest.spectrumStopScan = rawData.getNumOfScans()-1; }
			else {
					// Othewise combine only scans insisde selection
					refreshRequest.spectrumStartScan = rawData.getSelectionScanStart();
					refreshRequest.spectrumStopScan = rawData.getSelectionScanEnd();
			}

			refreshRequest.spectrumXResolution = this.getWidth() - 50 -5;

			String tmps_start = formatCoordinates.formatRTValue(refreshRequest.spectrumStartScan, rawData);
			String tmps_end = formatCoordinates.formatRTValue(refreshRequest.spectrumStopScan, rawData);

			setTitle("" + rawData.getNiceName() + ": Combination of spectra, from time " + tmps_start + " to " + tmps_end + "");

		}

		return refreshRequest;

	}

	public void afterRefresh(RawDataVisualizerRefreshResult refreshResult) {

		// Change in MZ cursor, no need to refresh
		if (refreshResult.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_MZ) { return; }


		// When in combination mode, change in RT cursor position (or both RT & MZ cursor pos.) doesn't need refreshing
		if (spectrumSpectraMode==COMBINATION_SPECTRA_MODE) {
			if (refreshResult.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_SCAN) { return; }
			if (refreshResult.changeType==RawDataVisualizerTICView.CHANGETYPE_CURSORPOSITION_BOTH) { return; }
		}


		if ( refreshResult.spectrumMZValues != null) {
			// Update scale
			spectraCombinationStartScan = refreshResult.spectrumCombinationStartScan;
			spectraCombinationStopScan = refreshResult.spectrumCombinationStopScan;
			scanPlot.setScale(refreshResult.spectrumMinMZValue,	refreshResult.spectrumMaxMZValue, 0, refreshResult.spectrumMaxIntensity);
			((ScanXAxis)bottomPnl).setScale(refreshResult.spectrumMinMZValue, refreshResult.spectrumMaxMZValue);
			((ScanYAxis)leftPnl).setScale(0, refreshResult.spectrumMaxIntensity);


			// Update plot panels
			scanPlot.setData(	refreshResult.spectrumMZValues,	refreshResult.spectrumIntensities);
		}

		// Peak handling (missing!)

		if (rawData.hasPeakData()) {

			double[] peakMZs = null;
			double[] peakInts = null;

			if (spectrumSpectraMode==ONE_SPECTRUM_MODE) {

				Vector<Peak> peaks = rawData.getPeakList().getPeaksForScans(rawData.getCursorPositionScan(), rawData.getCursorPositionScan());
				if (peaks!=null) {
					peakMZs = new double[peaks.size()];
					peakInts = new double[peaks.size()];
					for (int pi=0; pi<peaks.size(); pi++) {
						Peak p = peaks.get(pi);
						peakMZs[pi] = p.getMZAtScan(rawData.getCursorPositionScan());
						peakInts[pi] = p.getIntensityAtScan(rawData.getCursorPositionScan());
					}
				}
			}
			if (spectrumSpectraMode==COMBINATION_SPECTRA_MODE) {

				Vector<Peak> peaks = rawData.getPeakList().getPeaksForScans(spectraCombinationStartScan, spectraCombinationStopScan);
				if (peaks!=null) {
					peakMZs = new double[peaks.size()];
					peakInts = new double[peaks.size()];
					for (int pi=0; pi<peaks.size(); pi++) {
						Peak p = peaks.get(pi);
						peakMZs[pi] = p.getMZ();
						peakInts[pi] = p.getMedianIntensity();
					}
				}
			}

			scanPlot.setPeaks(peakMZs, peakInts);
		}





	}



	private void setSpectrumSpectraMode(int _mode) {
		spectrumSpectraMode = _mode;

		mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_SCAN, rawData.getRawDataID());

		/*
		BackgroundThread bt = new BackgroundThread(mainWin, msRun, this, Visualizer.CHANGETYPE_INTERNAL, BackgroundThread.TASK_REFRESHVISUALIZER);
		bt.start();
		*/
	}

	private int getSpectrumSpectraMode() {
		return spectrumSpectraMode;
	}


	public void printMe() {
		PrinterJob printJob = PrinterJob.getPrinterJob();

		HashPrintRequestAttributeSet pSet = new HashPrintRequestAttributeSet();
		pSet.add(OrientationRequested.LANDSCAPE);

		if (printJob.printDialog(pSet)) {
			printJob.setPrintable(this);
			try {
				printJob.print(pSet);
			} catch (Exception PrintException) {}
		}

	}

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

	 public void copyMe() {
		// Initialize clipboard
		Toolkit toolkit =    Toolkit.getDefaultToolkit();
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
/*
	public void closeMe() {
		mainWin.desktop.remove(this);
		dispose();
		msRun = null;
		dataPoints = null;
		statBar = null;
		mainWin = null;
		runPick = null;
	}
*/

	public void paint(Graphics g) {

		if (rawData==null) { return; }
		if (rawData.getRawDataUpdatedFlag()) { return; }

		super.paint(g);

		FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());

		// Set title for the plot window
		if (spectrumSpectraMode==ONE_SPECTRUM_MODE) {
			String tmps = formatCoordinates.formatRTValue(rawData.getCursorPositionScan(), rawData);
			setTitle("" + rawData.getNiceName() + ": Spectrum, time " + tmps);
		}

		if (spectrumSpectraMode==COMBINATION_SPECTRA_MODE) {

			String tmps_start = formatCoordinates.formatRTValue(spectraCombinationStartScan, rawData);
			String tmps_end = formatCoordinates.formatRTValue(spectraCombinationStopScan, rawData);
			setTitle("" + rawData.getNiceName() + ": Combination of spectra, from time " + tmps_start + " to " + tmps_end + "");
		}

	}





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




	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////

	class ScanPlot extends JPanel implements java.awt.event.ActionListener, java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

		//private Vector ticPoints;
		private double[] pointsX;
		private double[] pointsY;
		private double minY, maxY;
		private double minX, maxX;
		private RawDataVisualizerSpectrumView masterFrame;

		private double selectionFirstClick;
		private double selectionLastClick;

		private JPopupMenu popupMenu;
		private JMenuItem zoomToSelectionMenuItem;
		private JMenuItem zoomOutMenuItem;
		private JMenuItem zoomOutLittleMenuItem;
		private JMenuItem zoomSameToOthersMenuItem;

		private JMenuItem showDataPointsMenuItem;
		private JMenuItem changeSpectrumSpectraMenuItem;

		// private Vector peaks;
		private double[] peakMZs;
		private double[] peakInts;

		private DecimalFormat tickFormat;

		private boolean showDataPoints = false;



		public ScanPlot(RawDataVisualizerSpectrumView _masterFrame) {

			masterFrame = _masterFrame;
		    // Create popup-menu
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

		    zoomSameToOthersMenuItem = new JMenuItem("Set same zoom to other raw data viewers");
			zoomSameToOthersMenuItem.addActionListener(this);
		    popupMenu.add(zoomSameToOthersMenuItem);

		    popupMenu.addSeparator();

			changeSpectrumSpectraMenuItem = new JMenuItem();
			if (masterFrame.getSpectrumSpectraMode()==masterFrame.ONE_SPECTRUM_MODE) { changeSpectrumSpectraMenuItem.setText("Display combination of spectra");	}
			else { changeSpectrumSpectraMenuItem.setText("Display one spectrum"); }

		    showDataPointsMenuItem = new JMenuItem("Show data points");
		    showDataPointsMenuItem.addActionListener(this);
		    popupMenu.add(showDataPointsMenuItem);

		    changeSpectrumSpectraMenuItem.addActionListener(this);
		    popupMenu.add(changeSpectrumSpectraMenuItem);

		    selectionFirstClick = -1;
		    selectionLastClick = - 1;

		    tickFormat = new DecimalFormat("0.000");

		    addMouseListener(this);
		    addMouseMotionListener(this);

		}

		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

			super.paint(g);

			FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());


			if (pointsX!=null) {

				double w = getWidth();
				double h = getHeight();

				Enumeration pe;

				double x,y;
				double prevx, prevy;
				java.awt.geom.Point2D p;
				String s;

				int x1,x2,x3,y1,y2,y3;
				prevx = minX;
				prevy = minY;

				double diff_x_dat = maxX-minX;
				double diff_y_dat = maxY-minY;
				double diff_x_scr = w;
				double diff_y_scr = h;

				// Draw selection
				x1 = (int)(diff_x_scr * ((mouseAreaStart-minX) / diff_x_dat));
				x2 = (int)(diff_x_scr * ((mouseAreaEnd-minX) / diff_x_dat));
				y1 = (int)(h-diff_y_scr * ((minY-minY) / diff_y_dat));
				y2 = (int)(h-diff_y_scr * ((maxY-minY) / diff_y_dat));
				g.setColor(Color.lightGray);
				g.fillRect(x1,y2,x2-x1, y1-y2); //x2-x1,y1-y2);


				// Draw MZ value as label on each peak

				if (peakMZs!=null) {

					if (masterFrame.getSpectrumSpectraMode()==masterFrame.ONE_SPECTRUM_MODE) {
						g.setColor(Color.black);

						for (int pi=0; pi<peakMZs.length; pi++) {

							x1 = (int)(diff_x_scr * ((peakMZs[pi]-minX) / diff_x_dat));
							y1 = (int)(h-diff_y_scr * ((peakInts[pi]-minY) / diff_y_dat));
							if (y1<15) { y1 = 15; }

							s = formatCoordinates.formatMZValue(peakMZs[pi]);
							g.drawBytes(s.getBytes(), 0, s.length(), (int)x1, (int)y1);
						}

					} else {

						double realX, tmpX1, tmpX2;
						double interpY;
						g.setColor(Color.black);

						for (int pi=0; pi<peakMZs.length; pi++) {

							realX = peakMZs[pi];
							interpY = 0;

							for (int ind=0; ind<(pointsX.length-1); ind++) {
								tmpX1 = pointsX[ind];
								tmpX2 = pointsX[ind+1];
								if ( (tmpX1<=realX) && (tmpX2>=realX) ) {
									interpY = (pointsY[ind]+pointsY[ind+1])/((double)2);
									break;
								}
							}

							x1 = (int)(diff_x_scr * ((realX-minX) / diff_x_dat));
							y1 = (int)(h-diff_y_scr * ((interpY-minY) / diff_y_dat));
							if (y1<15) { y1 = 15; }

							s = formatCoordinates.formatMZValue(peakMZs[pi]);
							g.drawBytes(s.getBytes(), 0, s.length(), (int)x1, (int)y1);
						}
					}
				}


				// Draw linegraph
				g.setColor(Color.blue);

				if (pointsX.length>0) {
					prevx = pointsX[0];
					prevy = pointsY[0];
				}
				int xw=(int)java.lang.Math.round(diff_x_scr/100);
				int yw=(int)java.lang.Math.round(diff_y_scr/100);
				if (xw>yw) { xw=yw;	} else { yw=xw; }


				for (int ind=1; ind<pointsX.length; ind++) {

					x = pointsX[ind];
					y = pointsY[ind];

					x1 = (int)(diff_x_scr * ((prevx-minX) / diff_x_dat));
					x2 = (int)(diff_x_scr * ((x-minX) / diff_x_dat));
					y1 = (int)(h-diff_y_scr * ((prevy-minY) / diff_y_dat));
					y2 = (int)(h-diff_y_scr * ((y-minY) / diff_y_dat));

					if (mainWin.getParameterStorage().getGeneralParameters().getTypeOfData() == GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CONTINUOUS) {
						g.drawLine(x1,y1,x2,y2);
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getTypeOfData() == GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CENTROIDS) {
						g.drawLine(x2,(int)h,x2,y2);
					}

					if ( (showDataPoints==true) && (masterFrame.getSpectrumSpectraMode()==masterFrame.ONE_SPECTRUM_MODE) ) {
						g.setColor(Color.magenta);
						g.drawRect(	x2-xw, y2-yw, 2*xw, 2*yw);
						g.setColor(Color.blue);
					}

					prevx = x;
					prevy = y;
				}

				// Draw cursor position
				x = rawData.getCursorPositionMZ();
				x2 = (int)java.lang.Math.round((double)diff_x_scr * ((double)(x-minX) / (double)diff_x_dat));
				g.setColor(Color.red);
				g.drawLine(x2,0,x2,(int)h);
			}
		}


		public void setPeaks(double[] _peakMZs, double[] _peakInts) {
			peakMZs = _peakMZs;
			peakInts = _peakInts;
		}

		public void setScale(double _minX, double _maxX, double _minY, double _maxY) {
			minX = _minX;
			maxX = _maxX;
			minY = _minY;
			maxY = _maxY;
		}

		public void setData(double[] _pointsX, double[] _pointsY) {
			pointsX = _pointsX;
			pointsY = _pointsY;
		}

		public void actionPerformed(java.awt.event.ActionEvent e) {
			Object src = e.getSource();

			if (src == zoomToSelectionMenuItem) {

				rawData.setSelectionMZ(mouseAreaStart, mouseAreaEnd);
				mouseAreaStart = rawData.getCursorPositionMZ();
				mouseAreaEnd = rawData.getCursorPositionMZ();
				zoomToSelectionMenuItem.setEnabled(false);

				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_MZ, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/
			}

			if (src == zoomOutMenuItem) {

				rawData.clearSelectionMZ();
				mouseAreaStart = rawData.getCursorPositionMZ();
				mouseAreaEnd = rawData.getCursorPositionMZ();

				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_MZ, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/

			}


			if (src == zoomOutLittleMenuItem) {
				double midX = (minX+maxX)/(double)2.0;
				double tmpMinX, tmpMaxX;

				if (((midX-minX)>0) && ((maxX-midX)>0)) {
					tmpMinX = midX - (midX-minX)*(double)1.5;
					tmpMaxX = midX + (maxX-midX)*(double)1.5;
				} else {
					tmpMinX = minX - (double)0.5;
					tmpMaxX = maxX + (double)0.5;
				}

				if (tmpMinX<rawData.getDataMinMZ()) { tmpMinX = rawData.getDataMinMZ(); }
				if (tmpMaxX>rawData.getDataMaxMZ()) { tmpMaxX = rawData.getDataMaxMZ(); }

				rawData.setSelectionMZ(tmpMinX, tmpMaxX);

				mouseAreaStart = rawData.getCursorPositionMZ();
				mouseAreaEnd = rawData.getCursorPositionMZ();

				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_MZ, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/

			}

			if (src == zoomSameToOthersMenuItem) {
				mainWin.setSameZoomToOtherRawDatas(rawData, masterFrame.mainWin.SET_SAME_ZOOM_MZ);
			}

			if (src == changeSpectrumSpectraMenuItem) {

				if (masterFrame.getSpectrumSpectraMode()==1) {
					// Switch to combination of spectra
					masterFrame.setSpectrumSpectraMode(2);
					changeSpectrumSpectraMenuItem.setText("Display one spectrum");
					showDataPointsMenuItem.setEnabled(false);
				} else {
					// Switch to one spectrum
					masterFrame.setSpectrumSpectraMode(1);
					showDataPointsMenuItem.setEnabled(true);
					changeSpectrumSpectraMenuItem.setText("Display combination of spectra");
				}

			}

			if (src == showDataPointsMenuItem) {
				if (showDataPoints==false) {
					showDataPointsMenuItem.setText("Hide data points");
					showDataPoints = true;
				} else {
					showDataPointsMenuItem.setText("Show data points");
					showDataPoints = false;
				}
				repaint();
			}


		}




		public void mouseClicked(MouseEvent e) {
			statBar.setStatusText("");
		}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {
		    //if (e.isPopupTrigger()) {
		    if (e.getButton()!=MouseEvent.BUTTON1) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
		    } else {
			selectionFirstClick = -1;
			selectionLastClick = -1;
		    }

		}


		private int lastPressedButtonWas;

		public void mousePressed(MouseEvent e) {

			lastPressedButtonWas = e.getButton();
			if (e.getButton()==MouseEvent.BUTTON1) {
				int w = getWidth();
				double diff_x_dat = maxX-minX;
				double diff_x_scr = w;
				double xpos = (double)(minX+ diff_x_dat*(double)(e.getX()/diff_x_scr));
				selectionFirstClick = xpos;
				mouseAreaStart = xpos;
				mouseAreaEnd = xpos;
				rawData.setCursorPositionMZ(xpos);
				statBar.setCursorPositionMZ(xpos);

				mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_CURSORPOSITION_MZ, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_CURSORPOSITION_MZ, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/

				zoomToSelectionMenuItem.setEnabled(false);
				repaint();
			}
			statBar.setStatusText("");
		}

		public void mouseDragged(MouseEvent e) {

			if (lastPressedButtonWas!=MouseEvent.BUTTON1) { return; }

			int w = getWidth();
			double diff_x_dat = maxX-minX;
			double diff_x_scr = w;
			double xpos = (minX+ diff_x_dat*e.getX()/diff_x_scr);

			if (selectionFirstClick == -1) {
			    selectionFirstClick = xpos;
			    rawData.setCursorPositionMZ(xpos);
			} else {
			    selectionLastClick = xpos;

			    if (selectionLastClick<rawData.getDataMinMZ())	{ selectionLastClick = rawData.getDataMinMZ(); }
			    if (selectionLastClick>=rawData.getDataMaxMZ())	{ selectionLastClick = rawData.getDataMaxMZ(); }

			    if (selectionLastClick>selectionFirstClick) {
					mouseAreaStart = selectionFirstClick;
					mouseAreaEnd = selectionLastClick;
			    } else {
					mouseAreaStart = selectionLastClick;
					mouseAreaEnd = selectionFirstClick;
			    }

			    zoomToSelectionMenuItem.setEnabled(true);

			}
			statBar.setStatusText("");
			repaint();

		}
		public void mouseMoved(MouseEvent e) {}

	}



	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////


	class ScanXAxis extends JPanel {

		private final double leftMargin = (double)50.0;
		private final double rightMargin = (double)5.0;

		private double minX;
		private double maxX;
		private int numTics;

		public ScanXAxis() {
			super();
		}


		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

			super.paint(g);

			FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());

			double w = getWidth();
			double h = getHeight();

			numTics = 5;
			if (w>600) { numTics = 10; }
			if (w>1200) { numTics = 20; }
			if (w>2400) { numTics = 40; }


			// Draw axis
			this.setForeground(Color.black);
			g.drawLine((int)leftMargin,0,(int)(w-rightMargin),0);

			// Draw tics and numbers
			String tmps;
			double diff_dat = maxX - minX;
			double diff_scr = w-rightMargin-leftMargin;
			double xpos = leftMargin;
			double xval = minX;
			for (int t=1; t<=numTics; t++) {

				tmps = formatCoordinates.formatMZValue(xval);
				g.drawLine((int)xpos, 0, (int)xpos, (int)(h/4));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)xpos,(int)(3*h/4));

				xval += diff_dat / numTics;
				xpos += diff_scr / numTics;
			}
		}

		public void setScale(double _minX, double _maxX) {
			minX = _minX;
			maxX = _maxX;
		}

	}


	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////



	class ScanYAxis extends JPanel {


		private final double bottomMargin = (double)0.0;
		private final double topMargin = (double)0.0;

		private double minY;
		private double maxY;
		private int numTics;

		private DecimalFormat tickFormat;

		public ScanYAxis() {
			super();
			//tickFormat = new DecimalFormat("0.##E0");
			tickFormat = new DecimalFormat("0.#E0");
		}


		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

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
				tmps = new String(tickFormat.format(yval));

				g.drawLine((int)(3*w/4), (int)(h-ypos), (int)(w), (int)(h-ypos));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)(w/4)-4,(int)(h-ypos));

				yval += diff_dat / numTics;
				ypos += diff_scr / numTics;
			}
		}


		public void setScale(double _minY, double _maxY) {
			minY = _minY;
			maxY = _maxY;
		}

	}

}