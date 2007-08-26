package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;

public class OldTwoDPlot extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

	
	
	private OldTwoDDataSet dataset;
	
	private Vector<PeakListRow> peakListRows;

	private float selectionFirstClickRT;
	private float selectionFirstClickMZ;
	private float selectionLastClickRT;
	private float selectionLastClickMZ;

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

	private OldTwoDVisualizerWindow visualizerWindow;

	private float mouseCursorPositionRT;
	private float mouseCursorPositionMZ;
	
	private float mouseAreaStartRT;
	private float mouseAreaEndRT;
	private float mouseAreaStartMZ;
	private float mouseAreaEndMZ;
	
	private BufferedImage bitmapImage;
	private float imageScaleMinRT,imageScaleMaxRT;
	private float imageScaleMinMZ, imageScaleMaxMZ;

	public OldTwoDPlot(OldTwoDVisualizerWindow visualizerWindow, OldTwoDDataSet dataset) {

		this.visualizerWindow = visualizerWindow;
		this.dataset = dataset;

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

	    popupMenu.addSeparator();

	    selectNearestPeakMenuItem = new JMenuItem("Select nearest peak");
	    selectNearestPeakMenuItem.addActionListener(this);
	    popupMenu.add(selectNearestPeakMenuItem);

	    popupMenu.addSeparator();

	    selectionFirstClickRT = -1;
	    selectionFirstClickMZ = -1;
	    selectionLastClickRT = -1;
	    selectionLastClickMZ = -1;

	    addMouseListener(this);
	    addMouseMotionListener(this);

	}

	public void datasetUpdating() {
			
		// Check scale
		imageScaleMinMZ = dataset.getCurrentMinMZ();
		imageScaleMaxMZ = dataset.getCurrentMaxMZ();
		
		imageScaleMinRT = dataset.getCurrentMinRT();
		imageScaleMaxRT = dataset.getCurrentMaxRT();		
		
		// Check time since last bitmap construction
		if (false) {
			// Update bitmap
			// Update timestamp
		}
	}
	
	
	public void datasetUpdateReady() {
		System.out.println("Plot: dataset update is ready.");
		
		// Check scale
		imageScaleMinMZ = dataset.getCurrentMinMZ();
		imageScaleMaxMZ = dataset.getCurrentMaxMZ();
		
		imageScaleMinRT = dataset.getCurrentMinRT();
		imageScaleMaxRT = dataset.getCurrentMaxRT();
		
		// Update bitmap
		bitmapImage = constructBitmap();
		
	}
	

	
	private BufferedImage constructBitmap() {

		
		float[][] bitmapMatrix = dataset.getCurrentIntensityMatrix();
		int bitmapYSize = bitmapMatrix[0].length;;
		int bitmapXSize = bitmapMatrix.length;
		
		
		
		// Get suitable color space and maximum intensity value
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		float dataImgMax = dataset.getMaxIntensity();

		//System.out.println("dataImgMax=" + dataImgMax);
		
		// How many 8-bit components are used for representing shade of color in this color space?
		int nComp = cs.getNumComponents();
		int[] nBits = new int[nComp];
		for (int nb=0; nb<nComp; nb++) { nBits[nb]=8; }

		// Create sample model for storing the image
		ColorModel colorModel = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		SampleModel sampleModel = colorModel.createCompatibleSampleModel(bitmapXSize, bitmapYSize);
		DataBuffer dataBuffer = sampleModel.createDataBuffer();

		// Loop through the bitmap and translate each raw intensity to suitable representation in current color space
		byte count=0;
		byte[] b = new byte[nComp];
		double bb;
		double fac;
		for (int xpos=0; xpos<bitmapXSize; xpos++) {
			for (int ypos=0; ypos<bitmapYSize; ypos++) {

				
				
				bb = 0;
				bb = (double)((0.20*dataImgMax-bitmapMatrix[xpos][ypos])/(0.20*dataImgMax));
				//System.out.print("" + bb + ", ");
				if (bb<0) { bb = 0; }
				b[0] = (byte)(255*bb);

				sampleModel.setDataElements(xpos,ypos,b,dataBuffer);
			}
			//System.out.println();

		}
		//System.out.println();

		WritableRaster wr = Raster.createWritableRaster(sampleModel,dataBuffer, new Point(0,0));
		BufferedImage bi = new BufferedImage(colorModel, wr,true,null);

		return bi;

	}

	public void paint(Graphics g) {

		
		if (dataset==null) {
			return; 
		}
		

		
		double w = getWidth();
		double h = getHeight();
		
		if (bitmapImage != null) {

			/*
			System.out.println("plot, width=" + getWidth() + ", height=" + getHeight());
			System.out.println("Bitmap image, width=" + bitmapImage.getWidth() + ", height=" + bitmapImage.getHeight());
			 */
			
			Graphics2D g2d = (Graphics2D)g;
			g2d.drawRenderedImage(bitmapImage, AffineTransform.getScaleInstance(w/bitmapImage.getWidth(),h/bitmapImage.getHeight()));

			float x,y;
			int y1,y2,x1,x2;

			

			// Draw peaks
			g.setColor(Color.green);
			if (peakListRows!=null) {

				
				float[] mzs;
				float[] rts;
				for (PeakListRow row : peakListRows) {

					Peak p = row.getPeaks()[0];

					if (p==null) continue;
					
					int[] scanNumbers = p.getScanNumbers();
					mzs = new float[scanNumbers.length];
					rts = new float[scanNumbers.length];
					
					int ind=0;
					for (int scanNumber : scanNumbers) {					
						mzs[ind] = p.getRawDatapoints(scanNumber)[0][0];
						rts[ind] = dataset.getRetentionTime(scanNumber);
						ind++;
					}

					int prevx = (int)java.lang.Math.round((double)w * ((double)(rts[0]-imageScaleMinRT) / (double)(imageScaleMaxRT-imageScaleMinRT+1)));
					int prevy = (int)java.lang.Math.round((double)h * ((double)(mzs[0]-imageScaleMinMZ) / (double)(imageScaleMaxMZ-imageScaleMinMZ)));
					
					for (ind=0; ind<=mzs.length; ind++) {
						int currx = (int)java.lang.Math.round((double)w * ((double)(rts[ind]-imageScaleMinRT) / (double)(imageScaleMaxRT-imageScaleMinRT+1)));
						int curry = (int)java.lang.Math.round((double)h * ((double)(mzs[ind]-imageScaleMinMZ) / (double)(imageScaleMaxMZ-imageScaleMinMZ)));
						
						g.drawLine(prevx,(int)(h-prevy),currx,(int)(h-prevy));
						g.drawLine(currx,(int)(h-prevy),currx,(int)(h-curry));
						
						prevx = currx;
						prevy = curry;

					}
					int nextx = (int)java.lang.Math.round((double)w * ((double)(rts[rts.length-1]-imageScaleMinRT) / (double)(imageScaleMaxRT-imageScaleMinRT+1)));
					g.drawLine(prevx,(int)(h-prevy),nextx,(int)(h-prevy));
				}

			}

			// Draw Scan cursor position
			x = mouseCursorPositionRT;
			x2 = (int)java.lang.Math.round((double)w * ((double)(x-imageScaleMinRT) / (double)(imageScaleMaxRT-imageScaleMinRT+1)));
			g.setColor(Color.red);
			g.drawLine(x2,0,x2,(int)h);
			

			// Draw MZ cursor position
			y = mouseCursorPositionMZ;
			y2 = (int)java.lang.Math.round((double)h * ((double)(y-imageScaleMinMZ) / (double)(imageScaleMaxMZ-imageScaleMinMZ)));
			g.setColor(Color.red);
			g.drawLine(0,(int)(h-y2),(int)w,(int)(h-y2));
			
			// Draw selection
			x = mouseAreaStartRT;
			x1 = (int)java.lang.Math.round((double)w * ((double)(x-imageScaleMinRT) / (double)(imageScaleMaxRT-imageScaleMinRT+1)));
			x = mouseAreaEndRT;
			x2 = (int)java.lang.Math.round((double)w * ((double)(x-imageScaleMinRT) / (double)(imageScaleMaxRT-imageScaleMinRT+1)));
			y = mouseAreaStartMZ;
			y1 = (int)java.lang.Math.round((double)h * ((double)(y-imageScaleMinMZ) / (double)(imageScaleMaxMZ-imageScaleMinMZ)));
			y = mouseAreaEndMZ;
			y2 = (int)java.lang.Math.round((double)h * ((double)(y-imageScaleMinMZ) / (double)(imageScaleMaxMZ-imageScaleMinMZ)));
			g.setColor(Color.blue);
			g.drawRect(x1,(int)(h-y2),x2-x1,(int)(y2-y1));


		}
	}



	public void actionPerformed(java.awt.event.ActionEvent e) {

		Object src = e.getSource();

		if (src == zoomToSelectionMenuItem) {


			float rangeMinMZ = mouseAreaStartMZ;
			float rangeMaxMZ = mouseAreaEndMZ;
			
			float rangeMinRT = mouseAreaStartRT;
			float rangeMaxRT = mouseAreaEndRT;
			
			visualizerWindow.setZoomRange(1, rangeMinRT, rangeMaxRT, rangeMinMZ, rangeMaxMZ);
			
		}

		if (src == zoomOutMenuItem) {
			visualizerWindow.setFullZoom(1);
		}

		if (src == zoomOutLittleMenuItem) {
			// TODO
		}

		if (src == selectNearestPeakMenuItem) {
			// TODO 
		}

	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {

	    if (e.getButton()!=MouseEvent.BUTTON1) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
	    } else {
			selectionFirstClickRT = -1;
			selectionFirstClickMZ = -1;
			selectionLastClickRT = -1;
			selectionLastClickMZ = -1;
	    }

	}

	private int lastPressedButtonWas;
	public void mousePressed(MouseEvent e) {

		lastPressedButtonWas = e.getButton();

	    if (e.getButton()==MouseEvent.BUTTON1) {
			int w = getWidth();
			double diff_x_dat = imageScaleMaxRT - imageScaleMinRT;
			double diff_x_scr = w;
			double xpos = java.lang.Math.round((imageScaleMinRT + diff_x_dat*e.getX()/diff_x_scr));

			int h = getHeight();
			double diff_y_dat = imageScaleMaxMZ - imageScaleMinMZ;
			double diff_y_scr = h;
			double ypos = (imageScaleMinMZ + diff_y_dat*(double)(h - e.getY())/diff_y_scr);

			selectionFirstClickRT = (float)xpos;
			selectionFirstClickMZ = (float)ypos;

			mouseCursorPositionRT = (float)xpos;
			mouseCursorPositionMZ = (float)ypos;
			
			mouseAreaStartRT = (float)xpos;
			mouseAreaEndRT = (float)xpos;
			mouseAreaStartMZ = (float)ypos;
			mouseAreaEndMZ = (float)ypos;

			zoomToSelectionMenuItem.setEnabled(false);
				
			repaint();
			
	    }

	}

	public void mouseDragged(MouseEvent e) {

		if (lastPressedButtonWas!=MouseEvent.BUTTON1) { return; }

		int w = getWidth();
		double diff_x_dat = imageScaleMaxRT - imageScaleMinRT;
		double diff_x_scr = w;
		int xpos = (int)java.lang.Math.round((imageScaleMinRT + diff_x_dat*e.getX()/diff_x_scr));

		int h = getHeight();
		double diff_y_dat = imageScaleMaxMZ - imageScaleMinMZ;
		double diff_y_scr = h;
		double ypos = (imageScaleMinMZ + diff_y_dat*(double)(h - e.getY())/diff_y_scr);


		if (selectionFirstClickRT == -1) {
			selectionFirstClickRT = xpos;
			selectionFirstClickMZ = (float)ypos;
			//rawData.setCursorPosition(xpos,ypos);
		} else {
			selectionLastClickRT = xpos;
			selectionLastClickMZ = (float)ypos;

			if (selectionLastClickRT<imageScaleMinRT) { selectionLastClickRT = imageScaleMinRT; }
			if (selectionLastClickRT>imageScaleMaxRT) { selectionLastClickRT = imageScaleMaxRT; }

			if (selectionLastClickMZ<imageScaleMinMZ) { selectionLastClickMZ = imageScaleMinMZ; }
			if (selectionLastClickMZ>imageScaleMaxMZ) { selectionLastClickMZ = imageScaleMaxMZ; }


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

			zoomToSelectionMenuItem.setEnabled(true);

			repaint();
		}

	}


	public void mouseMoved(MouseEvent e) {}
}

