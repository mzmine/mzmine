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
package net.sf.mzmine.visualizers.rawdata.twod;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.Vector;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.RepaintManager;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.peakpicking.Peak;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.userinterface.mainwindow.Statusbar;
import net.sf.mzmine.util.FormatCoordinates;
import net.sf.mzmine.util.HeatMapColorPicker;
import net.sf.mzmine.util.TransferableImage;
import net.sf.mzmine.visualizers.RawDataVisualizer;




public class RawDataVisualizerTwoDView extends JInternalFrame implements RawDataVisualizer, Printable, InternalFrameListener {

	private JPanel bottomPnl, leftPnl, rightPnl, topPnl;
	private TwoDPlot twodPlot;

	private RawDataAtClient rawData;

	private int mouseAreaStartScan;
	private int mouseAreaEndScan;
	private double mouseAreaStartMZ;
	private double mouseAreaEndMZ;

	private MainWindow mainWin;
	private Statusbar statBar;
	private ItemSelector itemSelector;

	// Palette settings
	private final int[] heatmap_pal_waypoints = {0,2*256,8*256, 16*256, 32*256, 128*256, 65535};
	private final int[][] heatmap_pal_waypointRGBs = { {255,255,255}, {255,0,0}, {255,253,0}, {0,192,0}, {0,128,188}, {0,0,250}, {0,0,0} };

	private final int PALETTE_LINEAR1=1;
	private final int PALETTE_LINEAR2=2;
	private final int PALETTE_LINEAR3=3;
	private final int PALETTE_LOG=4;
	private final int PALETTE_HEATMAP=5;

	private int paletteMode = PALETTE_LINEAR1;
	private HeatMapColorPicker heatMap;

	private double[][] bitmapMatrix;
	private double bitmapMaxIntensity;
	private double bitmapMinIntensity;
	private double dataMaxIntensity;
	private int bitmapWidth;
	private int bitmapHeight;

	private boolean firstRefreshAlreadyDone = false;


	public RawDataVisualizerTwoDView(MainWindow _mainWin) {

		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();
		itemSelector = mainWin.getItemSelector();

		heatMap = new HeatMapColorPicker(heatmap_pal_waypoints, heatmap_pal_waypointRGBs);

		getContentPane().setLayout(new BorderLayout());

		bottomPnl = new TwoDXAxis();
		bottomPnl.setMinimumSize(new Dimension(getWidth(),25));
		bottomPnl.setPreferredSize(new Dimension(getWidth(),25));
		bottomPnl.setBackground(Color.white);
		getContentPane().add(bottomPnl, java.awt.BorderLayout.SOUTH);

		topPnl = new JPanel();
		topPnl.setMinimumSize(new Dimension(getWidth(),5));
		topPnl.setPreferredSize(new Dimension(getWidth(),5));
		topPnl.setBackground(Color.white);
		getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);

		leftPnl = new TwoDYAxis();
		leftPnl.setMinimumSize(new Dimension(100, getHeight()));
		leftPnl.setPreferredSize(new Dimension(100, getHeight()));
		leftPnl.setBackground(Color.white);
		getContentPane().add(leftPnl, java.awt.BorderLayout.WEST);

		rightPnl = new JPanel();
		rightPnl.setMinimumSize(new Dimension(5, getHeight()));
		rightPnl.setPreferredSize(new Dimension(5, getHeight()));
		rightPnl.setBackground(Color.white);
		getContentPane().add(rightPnl, java.awt.BorderLayout.EAST);

		twodPlot = new TwoDPlot(this);
		twodPlot.setBackground(Color.white);
		getContentPane().add(twodPlot, java.awt.BorderLayout.CENTER);

		setTitle("-: 2D-view");

		twodPlot.setVisible(true);

		addInternalFrameListener(this);

		setVisible( true );
        setSize(630, 240);
        setResizable( true );
        setIconifiable( true );

    }

    public void setRawData(RawDataAtClient _rawData) {
		rawData = _rawData;
    }

    public RawDataAtClient getRawData() {
		return rawData;
	}

    public void paint(Graphics g) {
		if (rawData==null) { return; }
		if (rawData.getRawDataUpdatedFlag()) { return; }

		super.paint(g);
	}



	public void adjustBitmapPalette() {

		BufferedImage bi = createBufferedImage();

		twodPlot.setImage(bi);

	}

	private BufferedImage createBufferedImage() {

		// Get suitable color space and maximum intensity value
		ColorSpace cs = null;
		double dataImgMax = Double.MAX_VALUE;
		switch (paletteMode) {
			case PALETTE_LINEAR1:
			case PALETTE_LINEAR2:
			case PALETTE_LINEAR3:
			case PALETTE_LOG:
				cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

				// Scale palette to maximum in the visible part of the data
				dataImgMax = bitmapMaxIntensity;
				break;

			case PALETTE_HEATMAP:
				cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

				// Scale palette to the global maximum of the data
				dataImgMax = dataMaxIntensity;
				break;
		}



		// How many 8-bit components are used for representing shade of color in this color space?
		int nComp = cs.getNumComponents();
		int[] nBits = new int[nComp];
		for (int nb=0; nb<nComp; nb++) { nBits[nb]=8; }

		// Create sample model for storing the image
		ColorModel colorModel = new ComponentColorModel(cs, nBits, false,true,Transparency.OPAQUE,DataBuffer.TYPE_BYTE);
		SampleModel sampleModel = colorModel.createCompatibleSampleModel(bitmapWidth, bitmapHeight);
		DataBuffer dataBuffer = sampleModel.createDataBuffer();

		// Loop through the bitmap and translate each raw intensity to suitable representation in current color space
		byte count=0;
		byte[] b = new byte[nComp];
		double bb;
		double fac;
		for (int xpos=0; xpos<bitmapWidth; xpos++) {
			for (int ypos=0; ypos<bitmapHeight; ypos++) {

				bb = 0;
				switch (paletteMode) {

					case PALETTE_LINEAR1:
						bb = (double)((0.20*dataImgMax-bitmapMatrix[xpos][ypos])/(0.20*dataImgMax));
						if (bb<0) { bb = 0; }
						b[0] = (byte)(255*bb);
						break;

					case PALETTE_LINEAR2:
						bb = (double)((0.05*dataImgMax-bitmapMatrix[xpos][ypos])/(0.05*dataImgMax));
						if (bb<0) { bb = 0; }
						b[0] = (byte)(255*bb);
						break;

					case PALETTE_LINEAR3:
						bb = (double)((0.01*dataImgMax-bitmapMatrix[xpos][ypos])/(0.01*dataImgMax));
						if (bb<0) { bb = 0; }
						b[0] = (byte)(255*bb);
						break;

					case PALETTE_LOG:
						bb = (double)(java.lang.Math.log(bitmapMatrix[xpos][ypos])/java.lang.Math.log(dataImgMax));
						if (bb>1) { bb=1; }
						bb = 1-bb;
						b[0] = (byte)(255*bb);
						break;

					case PALETTE_HEATMAP:
						bb = 65535*(bitmapMatrix[xpos][ypos]/dataImgMax);
						b = heatMap.getColorB((int)java.lang.Math.round(bb));
						break;
				}
				sampleModel.setDataElements(xpos,ypos,b,dataBuffer);
			}

		}

		WritableRaster wr = Raster.createWritableRaster(sampleModel,dataBuffer, new Point(0,0));
		BufferedImage bi = new BufferedImage(colorModel, wr,true,null);

		return bi;

	}


    public void setPaletteMode(int _pal) {
		paletteMode = _pal;
		adjustBitmapPalette();
		twodPlot.repaint();
	/*
		refreshFinalize(Visualizer.CHANGETYPE_INTERNAL);
	*/
	}

	public int getPaletteMode() {
		return paletteMode;
	}

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

/*
	public void closeMe() {
		mainWin.desktop.remove(this);
		dispose();
		msRun = null;
		statBar = null;
		mainWin = null;
		runPick = null;
	}
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


    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////

	class TwoDPlot extends JPanel implements java.awt.event.ActionListener, java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

		private BufferedImage bi;

		private Vector<Peak> peaks;
		private Vector<double[]> isotopeBoxes;

		private int selectionFirstClickScan;
		private double selectionFirstClickMZ;
		private int selectionLastClickScan;
		private double selectionLastClickMZ;

		private JPopupMenu popupMenu;

		private JMenuItem zoomToSelectionMenuItem;
		private JMenuItem zoomOutMenuItem;
		private JMenuItem zoomOutLittleMenuItem;
		private JMenuItem zoomSameToOthersMenuItem;

		private JMenuItem selectNearestPeakMenuItem;

		private JMenuItem setPalLin1MenuItem;
		private JMenuItem setPalLin2MenuItem;
		private JMenuItem setPalLin3MenuItem;
		private JMenuItem setPalLogMenuItem;
		private JMenuItem setPalHeatmapMenuItem;

		private RawDataVisualizerTwoDView masterFrame;

		int minX,maxX;
		double minY, maxY;

		public TwoDPlot(RawDataVisualizerTwoDView _masterFrame) {

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

		    selectNearestPeakMenuItem = new JMenuItem("Select nearest peak");
		    selectNearestPeakMenuItem.addActionListener(this);
		    popupMenu.add(selectNearestPeakMenuItem);

		    popupMenu.addSeparator();

		    setPalLin1MenuItem = new JMenuItem("Set palette: Linear 1");
		    setPalLin1MenuItem.addActionListener(this);
		    if (masterFrame.getPaletteMode()==PALETTE_LINEAR1) { setPalLin1MenuItem.setEnabled(false); } else { setPalLin1MenuItem.setEnabled(true); }
		    popupMenu.add(setPalLin1MenuItem);

		    setPalLin2MenuItem = new JMenuItem("Set palette: Linear 2");
		    setPalLin2MenuItem.addActionListener(this);
		    if (masterFrame.getPaletteMode()==PALETTE_LINEAR2) { setPalLin2MenuItem.setEnabled(false); } else { setPalLin2MenuItem.setEnabled(true); }
		    popupMenu.add(setPalLin2MenuItem);

		    setPalLin3MenuItem = new JMenuItem("Set palette: Linear 3");
		    setPalLin3MenuItem.addActionListener(this);
		    if (masterFrame.getPaletteMode()==PALETTE_LINEAR3) { setPalLin3MenuItem.setEnabled(false); } else { setPalLin3MenuItem.setEnabled(true); }
		    popupMenu.add(setPalLin3MenuItem);

		    setPalLogMenuItem = new JMenuItem("Set palette: Logarithmic");
		    setPalLogMenuItem.addActionListener(this);
		    if (masterFrame.getPaletteMode()==PALETTE_LOG) { setPalLogMenuItem.setEnabled(false); } else { setPalLogMenuItem.setEnabled(true); }
		    popupMenu.add(setPalLogMenuItem);

		    setPalHeatmapMenuItem = new JMenuItem("Set palette: Heatmap");
		    setPalHeatmapMenuItem.addActionListener(this);
		    if (masterFrame.getPaletteMode()==PALETTE_HEATMAP) { setPalHeatmapMenuItem.setEnabled(false); } else { setPalHeatmapMenuItem.setEnabled(true); }
		    popupMenu.add(setPalHeatmapMenuItem);

		    selectionFirstClickScan = -1;
		    selectionFirstClickMZ = -1;
		    selectionLastClickScan = -1;
		    selectionLastClickMZ = -1;

		    addMouseListener(this);
		    addMouseMotionListener(this);

		}


		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }

			double w = getWidth();
			double h = getHeight();

			if (bi != null) {

				Graphics2D g2d = (Graphics2D)g;
				g2d.drawRenderedImage(bi, AffineTransform.getScaleInstance(w/bi.getWidth(),h/bi.getHeight()));

				int x,x1,x2;
				double y,y1,y2;


				// Draw peaks
				if (paletteMode==PALETTE_HEATMAP) {
					g.setColor(Color.black);
				} else {
					g.setColor(Color.green);
				}
				if (peaks!=null) {

					int startScanI;
					int endScanI;
					int tmpI;
					double[] mzs;
					for (Peak p : peaks) {

						startScanI = p.getStartScanNumber();
						endScanI = p.getStopScanNumber();
						mzs = p.getMZDatapoints();
						tmpI = 0;
						for (int scanI=startScanI; scanI<endScanI; scanI++) {
							x1 = (int)java.lang.Math.round((double)w * ((double)(scanI-minX) / (double)(maxX-minX+1)));
							x2 = (int)java.lang.Math.round((double)w * ((double)(scanI+1-minX) / (double)(maxX-minX+1)));
							y1 = (int)java.lang.Math.round((double)h * ((double)(mzs[tmpI]-minY) / (double)(maxY-minY)));
							// This line will give an error, if peaks with length one scan are allowed in peak picker.
							y2 = (int)java.lang.Math.round((double)h * ((double)(mzs[tmpI+1]-minY) / (double)(maxY-minY)));
							tmpI++;
							g.drawLine(x1,(int)(h-y1),x2,(int)(h-y2));
						}

					}

				}

				// Draw isotope boxes
				if (paletteMode==PALETTE_HEATMAP) {
					g.setColor(Color.darkGray);
				} else {
					g.setColor(Color.cyan);
				}

				if (isotopeBoxes!=null) {

					double startScanI;
					double endScanI;
					double startMZ;
					double endMZ;
					int tmpI;
					double[] mzs;
					for (double[] oneBox : isotopeBoxes) {

						startScanI = oneBox[1];
						endScanI = oneBox[3];
						startMZ = oneBox[0]-0.1;
						endMZ = oneBox[2]+0.1;

						x1 = (int)java.lang.Math.round((double)w * ((double)(startScanI-minX) / (double)(maxX-minX+1)));
						x2 = (int)java.lang.Math.round((double)w * ((double)(endScanI+1-minX) / (double)(maxX-minX+1)));
						y1 = (int)java.lang.Math.round((double)h * ((double)(startMZ-minY) / (double)(maxY-minY)));
						y2 = (int)java.lang.Math.round((double)h * ((double)(endMZ-minY) / (double)(maxY-minY)));

						g.drawRect(x1,(int)(h-y2),x2-x1,(int)(y2-y1));

					}


				}


				// Draw Scan cursor position
				x = rawData.getCursorPositionScan();
				x2 = (int)java.lang.Math.round((double)w * ((double)(x-minX) / (double)(maxX-minX+1)));
				g.setColor(Color.red);
				g.drawLine(x2,0,x2,(int)h);

				// Draw MZ cursor position
				y = rawData.getCursorPositionMZ();
				y2 = (int)java.lang.Math.round((double)h * ((double)(y-minY) / (double)(maxY-minY)));
				g.setColor(Color.red);
				g.drawLine(0,(int)(h-y2),(int)w,(int)(h-y2));

				// Draw selection
				x = mouseAreaStartScan;
				x1 = (int)java.lang.Math.round((double)w * ((double)(x-minX) / (double)(maxX-minX+1)));
				x = mouseAreaEndScan;
				x2 = (int)java.lang.Math.round((double)w * ((double)(x-minX) / (double)(maxX-minX+1)));
				y = mouseAreaStartMZ;
				y1 = (int)java.lang.Math.round((double)h * ((double)(y-minY) / (double)(maxY-minY)));
				y = mouseAreaEndMZ;
				y2 = (int)java.lang.Math.round((double)h * ((double)(y-minY) / (double)(maxY-minY)));
				g.setColor(Color.blue);
				g.drawRect(x1,(int)(h-y2),x2-x1,(int)(y2-y1));

			}
		}


		public void setImage(BufferedImage _bi) {
			bi = _bi;
		}

		public void setScale(int _minX, int _maxX, double _minY, double _maxY) {

			minX = _minX;
			maxX = _maxX;
			minY = _minY;
			maxY = _maxY;

		}

		public void setPeaks(Vector<Peak> _peaks) {
			peaks = _peaks;
		}

		public void setIsotopeBoxes(Vector<double[]> _isotopeBoxes) {
			isotopeBoxes = _isotopeBoxes;
		}


		public void actionPerformed(java.awt.event.ActionEvent e) {

			Object src = e.getSource();

			if (src == zoomToSelectionMenuItem) {

				rawData.setSelection(mouseAreaStartScan, mouseAreaEndScan, mouseAreaStartMZ, mouseAreaEndMZ);
				mouseAreaStartScan = rawData.getCursorPositionScan();
				mouseAreaEndScan = rawData.getCursorPositionScan();
				mouseAreaStartMZ = rawData.getCursorPositionMZ();
				mouseAreaEndMZ = rawData.getCursorPositionMZ();

		//		mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_BOTH, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/

				zoomToSelectionMenuItem.setEnabled(false);

			}

			if (src == zoomOutMenuItem) {

				rawData.clearSelection();
				mouseAreaStartScan = rawData.getCursorPositionScan();
				mouseAreaEndScan = rawData.getCursorPositionScan();
				mouseAreaStartMZ = rawData.getCursorPositionMZ();
				mouseAreaEndMZ = rawData.getCursorPositionMZ();

			//	mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_BOTH, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/

			}

			if (src == zoomOutLittleMenuItem) {
				int midX = (int)(java.lang.Math.round( (double)(minX+maxX)/(double)2 ) );
				double midY = (minY+maxY)/(double)2.0;
				int tmpMinX, tmpMaxX;
				double tmpMinY, tmpMaxY;

				if (((midX-minX)>0) && ((maxX-midX)>0)) {
					tmpMinX = (int)(java.lang.Math.round(midX - (midX-minX)*(double)1.5));
					tmpMaxX = (int)(java.lang.Math.round(midX + (maxX-midX)*(double)1.5));
				} else {
					tmpMinX = minX - 1;
					tmpMaxX = maxX + 1;
				}


				if (((midY-minY)>0) && ((maxY-midY)>0)) {
					tmpMinY = midY - (midY-minY)*(double)1.5;
					tmpMaxY = midY + (maxY-midY)*(double)1.5;
				} else {
					tmpMinY = minY - (double)0.5;
					tmpMaxY = maxY + (double)0.5;
				}

				if (tmpMinX<0) {tmpMinX = 0;}
				if (tmpMaxX>(rawData.getNumOfScans()-1)) { tmpMaxX = rawData.getNumOfScans()-1; }
				if (tmpMinY<rawData.getDataMinMZ()) { tmpMinY = rawData.getDataMinMZ(); }
				if (tmpMaxY>rawData.getDataMaxMZ()) { tmpMaxY = rawData.getDataMaxMZ(); }

				rawData.setSelection(tmpMinX, tmpMaxX, tmpMinY, tmpMaxY);


		//		mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, rawData.getRawDataID());

				/*
				BackgroundThread bt = new BackgroundThread(mainWin, msRun, Visualizer.CHANGETYPE_SELECTION_BOTH, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
				*/

			}

			if (src == zoomSameToOthersMenuItem) {
		//		mainWin.setSameZoomToOtherRawDatas(rawData, masterFrame.mainWin.SET_SAME_ZOOM_BOTH);
			}


			if (src == selectNearestPeakMenuItem) {
				// Calc MZ vs ScanNum parameter
				double pixelVertical = (maxY-minY) / bi.getHeight();
				double pixelHorizontal = (rawData.getScanTime(maxX)-rawData.getScanTime(minX)) / bi.getWidth();
				double MZvsScanNum = pixelVertical / pixelHorizontal;

		//		int changeType = rawData.selectNearestPeak(rawData.getCursorPositionMZ(), rawData.getCursorPositionScan(), MZvsScanNum);

		//		mainWin.startRefreshRawDataVisualizers(changeType, rawData.getRawDataID());
			}

			if (src == setPalLin1MenuItem) {
				//setCursor(new Cursor(Cursor.WAIT_CURSOR));
				//masterFrame.mainWin.setMouseWaitCursor();

				masterFrame.setPaletteMode(PALETTE_LINEAR1);
				setPalLin1MenuItem.setEnabled(false);
				setPalLin2MenuItem.setEnabled(true);
				setPalLin3MenuItem.setEnabled(true);
				setPalLogMenuItem.setEnabled(true);
				setPalHeatmapMenuItem.setEnabled(true);

				//masterFrame.mainWin.setMouseDefaultCursor();
				//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			if (src == setPalLin2MenuItem) {
				//setCursor(new Cursor(Cursor.WAIT_CURSOR));
				//masterFrame.mainWin.setMouseWaitCursor();

				masterFrame.setPaletteMode(PALETTE_LINEAR2);
				setPalLin1MenuItem.setEnabled(true);
				setPalLin2MenuItem.setEnabled(false);
				setPalLin3MenuItem.setEnabled(true);
				setPalLogMenuItem.setEnabled(true);
				setPalHeatmapMenuItem.setEnabled(true);

				//masterFrame.mainWin.setMouseDefaultCursor();
				//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			if (src == setPalLin3MenuItem) {
				//setCursor(new Cursor(Cursor.WAIT_CURSOR));
				//masterFrame.mainWin.setMouseWaitCursor();

				masterFrame.setPaletteMode(PALETTE_LINEAR3);
				setPalLin1MenuItem.setEnabled(true);
				setPalLin2MenuItem.setEnabled(true);
				setPalLin3MenuItem.setEnabled(false);
				setPalLogMenuItem.setEnabled(true);
				setPalHeatmapMenuItem.setEnabled(true);

				//masterFrame.mainWin.setMouseDefaultCursor();
				//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			if (src == setPalLogMenuItem) {
				// setCursor(new Cursor(Cursor.WAIT_CURSOR));
				//masterFrame.mainWin.setMouseWaitCursor();

				masterFrame.setPaletteMode(PALETTE_LOG);
				setPalLin1MenuItem.setEnabled(true);
				setPalLin2MenuItem.setEnabled(true);
				setPalLin3MenuItem.setEnabled(true);
				setPalLogMenuItem.setEnabled(false);
				setPalHeatmapMenuItem.setEnabled(true);

				//masterFrame.mainWin.setMouseDefaultCursor();
				//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			if (src == setPalHeatmapMenuItem) {
				//setCursor(new Cursor(Cursor.WAIT_CURSOR));
				//masterFrame.mainWin.setMouseWaitCursor();

				masterFrame.setPaletteMode(PALETTE_HEATMAP);
				setPalLin1MenuItem.setEnabled(true);
				setPalLin2MenuItem.setEnabled(true);
				setPalLin3MenuItem.setEnabled(true);
				setPalLogMenuItem.setEnabled(true);
				setPalHeatmapMenuItem.setEnabled(false);

				//masterFrame.mainWin.setMouseDefaultCursor();
				//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {

		    if (e.getButton()!=MouseEvent.BUTTON1) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
		    } else {
				selectionFirstClickScan = -1;
				selectionFirstClickMZ = -1;
				selectionLastClickScan = -1;
				selectionLastClickMZ = -1;
		    }
		    statBar.setStatusText("");
		}

		private int lastPressedButtonWas;
		public void mousePressed(MouseEvent e) {

			lastPressedButtonWas = e.getButton();

		    if (e.getButton()==MouseEvent.BUTTON1) {
				int w = getWidth();
				double diff_x_dat = maxX-minX+1;
				double diff_x_scr = w;
				int xpos = (int)java.lang.Math.round((minX+ diff_x_dat*e.getX()/diff_x_scr));

				int h = getHeight();
				double diff_y_dat = maxY-minY;
				double diff_y_scr = h;
				double ypos = (minY + diff_y_dat*(double)(h - e.getY())/diff_y_scr);

				selectionFirstClickScan = xpos;
				selectionFirstClickMZ = ypos;

				mouseAreaStartScan = xpos;
				mouseAreaEndScan = xpos;
				mouseAreaStartMZ = ypos;
				mouseAreaEndMZ = ypos;

				zoomToSelectionMenuItem.setEnabled(false);
				rawData.setCursorPosition(xpos, ypos);
				statBar.setCursorPosition(ypos, rawData.getScanTime(xpos));

				int[] tmpRawDataIDs = new int[1];
				tmpRawDataIDs[0] = rawData.getRawDataID();
		//		mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_CURSORPOSITION_BOTH, tmpRawDataIDs);

				//msRun.refreshVisualizers(Visualizer.CHANGETYPE_CURSORPOSITION_BOTH, statBar);
		    }
		    statBar.setStatusText("");

		}

		public void mouseDragged(MouseEvent e) {

			if (lastPressedButtonWas!=MouseEvent.BUTTON1) { return; }

			int w = getWidth();
			double diff_x_dat = maxX-minX+1;
			double diff_x_scr = w;
			int xpos = (int)java.lang.Math.round((minX+ diff_x_dat*e.getX()/diff_x_scr));

			int h = getHeight();
			double diff_y_dat = maxY-minY;
			double diff_y_scr = h;
			double ypos = (minY + diff_y_dat*(double)(h - e.getY())/diff_y_scr);


			if (selectionFirstClickScan == -1) {
				selectionFirstClickScan = xpos;
				selectionFirstClickMZ = ypos;
				rawData.setCursorPosition(xpos,ypos);
			} else {
				selectionLastClickScan = xpos;
				selectionLastClickMZ = ypos;

				if (selectionLastClickScan<minX) { selectionLastClickScan = minX; }
				if (selectionLastClickScan>maxX) { selectionLastClickScan = maxX; }

				if (selectionLastClickMZ<minY) { selectionLastClickMZ = minY; }
				if (selectionLastClickMZ>maxY) { selectionLastClickMZ = maxY; }


				if (selectionLastClickScan>selectionFirstClickScan) {
					mouseAreaStartScan = selectionFirstClickScan;
					mouseAreaEndScan = selectionLastClickScan;
				} else {
					mouseAreaStartScan = selectionLastClickScan;
					mouseAreaEndScan = selectionFirstClickScan;
				}

				if (selectionLastClickMZ>selectionFirstClickMZ) {
					mouseAreaStartMZ = selectionFirstClickMZ;
					mouseAreaEndMZ = selectionLastClickMZ;
				} else {
					mouseAreaStartMZ = selectionLastClickMZ;
					mouseAreaEndMZ = selectionFirstClickMZ;
				}

				zoomToSelectionMenuItem.setEnabled(true);

				repaint();
			}
			statBar.setStatusText("");

		}


		public void mouseMoved(MouseEvent e) {}
	}

	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////


	class TwoDXAxis extends JPanel {

		private final int leftMargin = 100;
		private final int rightMargin = 5;

		private int minX;
		private int maxX;
		// private int numTics;


		public TwoDXAxis() {
			super();
		}


		public void paint(Graphics g) {

			if (rawData==null) { return; }
			if (rawData.getRawDataUpdatedFlag()) { return; }


			super.paint(g);

			FormatCoordinates formatCoordinates = new FormatCoordinates(mainWin.getParameterStorage().getGeneralParameters());

			int w = getWidth();
			double h = getHeight();

			int numofscans = maxX-minX+1;
			double pixelsperscan = (double)(w-leftMargin-rightMargin) / (double)numofscans;
			if (pixelsperscan<=0) { return; }

			int scanspertic = 1;
			while ( (scanspertic * pixelsperscan) < 60 ) { scanspertic++;}

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

				tmps = null;
				tmps = formatCoordinates.formatRTValue(xval, rawData);

				g.drawLine((int)java.lang.Math.round(xpos), 0, (int)java.lang.Math.round(xpos), (int)(h/4));
				g.drawBytes(tmps.getBytes(), 0, tmps.length(), (int)java.lang.Math.round(xpos),(int)(3*h/4));

				xval += scanspertic;
				xpos += pixelspertic;
			}
		}


		public void setScale(int _minX, int _maxX) {
			minX = _minX;
			maxX = _maxX;
		}

	}



	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////



	class TwoDYAxis extends JPanel {

		private final double bottomMargin = (double)0.0;
		private final double topMargin = (double)0.0;

		private double minY;
		private double maxY;
		private int numTics;

		public TwoDYAxis() {
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
				// tmps = new String("" + (int)yval);
				//tmps = new String(tickFormat.format(yval));
				tmps = formatCoordinates.formatMZValue(yval);
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



    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#getRawDataFile()
     */
    public RawDataFile getRawDataFile() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRawDataFile(net.sf.mzmine.io.RawDataFile)
     */
    public void setRawDataFile(RawDataFile newFile) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double, double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double, double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZPosition(double)
     */
    public void setMZPosition(double mz) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTPosition(double)
     */
    public void setRTPosition(double rt) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#attachVisualizer(net.sf.mzmine.visualizers.RawDataVisualizer)
     */
    public void attachVisualizer(RawDataVisualizer visualizer) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#detachVisualizer(net.sf.mzmine.visualizers.RawDataVisualizer)
     */
    public void detachVisualizer(RawDataVisualizer visualizer) {
        // TODO Auto-generated method stub
        
    }



}



