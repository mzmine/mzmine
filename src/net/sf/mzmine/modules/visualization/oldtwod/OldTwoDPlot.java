/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.BasicStroke;
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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.util.CursorPosition;

public class OldTwoDPlot extends JPanel implements ActionListener,
        MouseListener, MouseMotionListener, TaskListener {

    private static final float zoomOutLittleFactor = 1.5f;

    private static final float defaultOneSidedXICWidth = 0.05f;

    private OldTwoDDataSet dataset;

    private JPopupMenu popupMenu;

    private JMenuItem zoomToSelectionMenuItem;
    private JMenuItem zoomOutMenuItem;
    private JMenuItem zoomOutLittleMenuItem;

    private JMenuItem showSpectrumPlotMenuItem;
    private JMenuItem showXICPlotMenuItem;

    private JMenuItem finalizePrePeaksMenuItem;
    private JMenuItem deleteNearestPeakMenuItem;

    private OldTwoDVisualizerWindow visualizerWindow;

    private int lastPressedMouseButton;

    private int mouseCursorPositionX;
    private int mouseCursorPositionY;

    private int selectionFirstClickX = -1;
    private int selectionFirstClickY = -1;
    private int selectionLastClickX = -1;
    private int selectionLastClickY = -1;

    private int mouseAreaStartX;
    private int mouseAreaStartY;
    private int mouseAreaStopX;
    private int mouseAreaStopY;

    private BufferedImage bitmapImage;

    private InterpolatingLookupPaintScale paintScale;
    private Color cursorColor;
    private Color selectionColor;
    private Color peakColor;

    private boolean showPeaks = true;

    private MZmineProject project;

    private Vector<PreConstructionPeak> prePeaks;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public OldTwoDPlot(OldTwoDVisualizerWindow visualizerWindow,
            OldTwoDDataSet dataset, InterpolatingLookupPaintScale paintScale) {

        this.visualizerWindow = visualizerWindow;
        this.dataset = dataset;
        this.paintScale = paintScale;

        project = MZmineCore.getCurrentProject();

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

        showSpectrumPlotMenuItem = new JMenuItem("Open a spectrum plot");
        showSpectrumPlotMenuItem.addActionListener(this);
        popupMenu.add(showSpectrumPlotMenuItem);

        showXICPlotMenuItem = new JMenuItem("Open an XIC plot");
        showXICPlotMenuItem.addActionListener(this);
        popupMenu.add(showXICPlotMenuItem);

        popupMenu.addSeparator();

        finalizePrePeaksMenuItem = new JMenuItem("Finalize pre-peaks");
        finalizePrePeaksMenuItem.addActionListener(this);
        popupMenu.add(finalizePrePeaksMenuItem);

        deleteNearestPeakMenuItem = new JMenuItem("Delete nearest (pre-)peak");
        deleteNearestPeakMenuItem.addActionListener(this);
        popupMenu.add(deleteNearestPeakMenuItem);

        prePeaks = new Vector<PreConstructionPeak>();

        addMouseListener(this);
        addMouseMotionListener(this);

    }

    public void datasetUpdating() {
        // TODO
    }

    public void datasetUpdateReady() {

        // Update bitmap
        bitmapImage = constructBitmap();

    }

    protected InterpolatingLookupPaintScale getPaintScale() {
        return paintScale;
    }

    protected void setPaintScale(InterpolatingLookupPaintScale paintScale,
            Color cursorColor, Color selectionColor, Color peakColor) {
        this.paintScale = paintScale;
        this.cursorColor = cursorColor;
        this.selectionColor = selectionColor;
        this.peakColor = peakColor;
    }

    protected void togglePeakDisplay() {
        showPeaks = !showPeaks;
    }

    private BufferedImage constructBitmap() {

        float[][] bitmapMatrix = dataset.getIntensityMatrix();
        int bitmapXSize = bitmapMatrix.length;
        int bitmapYSize = bitmapMatrix[0].length;;

        // Get suitable color space and maximum intensity value
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        float dataImgMax = dataset.getMaxIntensity();

        // How many 8-bit components are used for representing shade of color in
        // this color space?
        int nComp = cs.getNumComponents();
        int[] nBits = new int[nComp];
        for (int nb = 0; nb < nComp; nb++) {
            nBits[nb] = 8;
        }

        // Create sample model for storing the image
        ColorModel colorModel = new ComponentColorModel(cs, nBits, false, true,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(
                bitmapXSize, bitmapYSize);
        DataBuffer dataBuffer = sampleModel.createDataBuffer();

        // Loop through the bitmap and translate each raw intensity to suitable
        // representation in current color space
        byte count = 0;
        byte[] b = new byte[nComp];
        double bb;
        double fac;
        for (int xpos = 0; xpos < bitmapXSize; xpos++) {
            for (int ypos = 0; ypos < bitmapYSize; ypos++) {

                Color color = (Color) paintScale.getPaint(bitmapMatrix[xpos][ypos]);

                b[0] = (byte) color.getRed();
                b[1] = (byte) color.getGreen();
                b[2] = (byte) color.getBlue();

                sampleModel.setDataElements(xpos, ypos, b, dataBuffer);
            }

        }

        WritableRaster wr = Raster.createWritableRaster(sampleModel,
                dataBuffer, new Point(0, 0));
        BufferedImage bi = new BufferedImage(colorModel, wr, true, null);

        return bi;

    }

    public void paint(Graphics g) {

        if (dataset == null) {
            return;
        }

        double w = getWidth();
        double h = getHeight();

        if (bitmapImage != null) {

            Graphics2D g2d = (Graphics2D) g;
            g2d.drawRenderedImage(bitmapImage,
                    AffineTransform.getScaleInstance(
                            w / bitmapImage.getWidth(), h
                                    / bitmapImage.getHeight()));

            // Draw peaks

            g.setColor(peakColor);
            RawDataFile rawDataFile = dataset.getRawDataFile();
            PeakList peakList = visualizerWindow.getSelectedPeakList();

            if ((peakList != null) && (showPeaks)) {

                PeakListRow[] rows = peakList.getRows();
                float[] mzs;
                float[] rts;
                for (PeakListRow row : rows) {

                    Peak p = row.getPeaks()[0];

                    if (p == null)
                        continue;

                    int[] scanNumbers = p.getScanNumbers();
                    mzs = new float[scanNumbers.length];
                    rts = new float[scanNumbers.length];

                    int ind = 0;
                    for (int scanNumber : scanNumbers) {
                        mzs[ind] = p.getDataPoint(scanNumber).getMZ();
                        rts[ind] = rawDataFile.getScan(scanNumber).getRetentionTime();
                        ind++;
                    }

                    int prevx = convertRTToPlotXCoordinate(rts[0]);
                    int prevy = convertMZToPlotYCoordinate(mzs[0]);

                    for (ind = 0; ind < mzs.length; ind++) {
                        int currx = convertRTToPlotXCoordinate(rts[ind]);
                        int curry = convertMZToPlotYCoordinate(mzs[ind]);

                        g.drawLine(prevx, prevy, currx, prevy);
                        g.drawLine(currx, prevy, currx, curry);

                        prevx = currx;
                        prevy = curry;
                    }
                    int nextx = convertRTToPlotXCoordinate(rts[rts.length - 1]);
                    g.drawLine(prevx, prevy, nextx, prevy);

                }

            }

            // Draw pre-peaks if present
            if ((prePeaks != null) && (prePeaks.size() > 0)) {
                for (PreConstructionPeak prePeak : prePeaks) {
                    int startx = convertRTToPlotXCoordinate(prePeak.getPreStartRT());
                    int stopx = convertRTToPlotXCoordinate(prePeak.getPreStopRT());
                    int y = convertMZToPlotYCoordinate(prePeak.getPreMZ());

                    g.setColor(peakColor);
                    g.drawLine(startx, y, stopx, y);
                }
            }

            // Reset previous selected area and initialize new selection to
            // start from current position

            // If no area selected, then draw normal cursor
            if ((mouseAreaStartX < 0) && (mouseAreaStartY < 0)
                    && (mouseAreaStopX < 0) && (mouseAreaStopY < 0)) {

                // Draw cursor position x-coordinate => scan
                g2d.setColor(cursorColor);
                g2d.setStroke(new BasicStroke(0.0f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f,
                        new float[] { 6.0f, 3.0f }, 0.0f));
                g2d.draw(new Line2D.Double(mouseCursorPositionX,
                        mouseCursorPositionY, mouseCursorPositionX, (int) h));
                g2d.draw(new Line2D.Double(mouseCursorPositionX,
                        mouseCursorPositionY, mouseCursorPositionX, 0));

                // Draw cursor position y-coordinate => m/z
                g2d.draw(new Line2D.Double(mouseCursorPositionX,
                        mouseCursorPositionY, (int) w, mouseCursorPositionY));
                g2d.draw(new Line2D.Double(mouseCursorPositionX,
                        mouseCursorPositionY, 0, mouseCursorPositionY));

            } else {

                // When area is selected then do not draw a cursor

                switch (visualizerWindow.getZoomPeakEditMode()) {
                case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_PEAKEDITMODE:

                    // Draw the current pre-pre-peak
                    g2d.setColor(selectionColor);
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.draw(new Line2D.Double(mouseAreaStartX,
                            selectionLastClickY, mouseAreaStopX,
                            selectionLastClickY));

                    break;

                case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_ZOOMMODE:

                    // Draw rectangle over the selected area
                    g2d.setColor(selectionColor);
                    g2d.setStroke(new BasicStroke(0.0f));
                    g2d.setPaint(new Color(
                            (float) selectionColor.getRed() / 255.0f,
                            (float) selectionColor.getGreen() / 255.0f,
                            (float) selectionColor.getBlue() / 255.0f, 0.25f));
                    g2d.fill(new Rectangle2D.Double(mouseAreaStartX,
                            mouseAreaStartY, mouseAreaStopX - mouseAreaStartX,
                            mouseAreaStopY - mouseAreaStartY));

                    break;
                }

            }

        }

    }

    private int convertPlotXCoordinateToIntensityMatrixXIndex(int xCoordinate) {
        return (int) java.lang.Math.floor(((float) xCoordinate / (float) getWidth())
                * (float) dataset.getIntensityMatrix().length);
    }

    private float convertPlotYCoordinateToMZ(int yCoordinate) {
        float dataMZRangeWidth = dataset.getMaxMZ() - dataset.getMinMZ();
        return (((float) getHeight() - (float) yCoordinate) / (float) getHeight())
                * dataMZRangeWidth + dataset.getMinMZ();
    }

    private int convertMZToPlotYCoordinate(float mz) {
        float dataMZRangeWidth = dataset.getMaxMZ() - dataset.getMinMZ();
        return java.lang.Math.round(getHeight()
                - ((mz - dataset.getMinMZ()) / dataMZRangeWidth)
                * (float) getHeight());

    }

    private float convertPlotXCoordinateToRT(int xCoordinate) {
        float dataRTRangeWidth = dataset.getMaxRT() - dataset.getMinRT();
        return ((float) xCoordinate / (float) getWidth()) * dataRTRangeWidth
                + dataset.getMinRT();
    }

    private int convertRTToPlotXCoordinate(float rt) {
        float dataRTRangeWidth = dataset.getMaxRT() - dataset.getMinRT();
        return java.lang.Math.round(((rt - dataset.getMinRT()) / dataRTRangeWidth)
                * (float) getWidth());
    }

    private int convertPlotXCoordinateToXIndex(int xCoordinate) {
        int xSteps = dataset.getIntensityMatrix().length;
        double xStepInPixels = (double) getWidth() / (double) xSteps;

        int nearestStep = 0;
        for (int xStep = 0; xStep < xSteps; xStep++)
            if (java.lang.Math.abs(xStep * xStepInPixels - xCoordinate) < java.lang.Math.abs(nearestStep
                    * xStepInPixels - xCoordinate))
                nearestStep = xStep;

        return nearestStep;

    }

    private int quantizePlotXCoordinate(int xCoordinate) {
        int xSteps = dataset.getIntensityMatrix().length;
        double xStepInPixels = (double) getWidth() / (double) xSteps;

        int nearestStep = 0;
        for (int xStep = 0; xStep < xSteps; xStep++)
            if (java.lang.Math.abs(xStep * xStepInPixels - xCoordinate) < java.lang.Math.abs(nearestStep
                    * xStepInPixels - xCoordinate))
                nearestStep = xStep;

        return (int) java.lang.Math.round(nearestStep * xStepInPixels);

    }

    public void actionPerformed(java.awt.event.ActionEvent e) {

        Object src = e.getSource();

        if (src == zoomToSelectionMenuItem) {

            float rangeMaxMZ = convertPlotYCoordinateToMZ(mouseAreaStartY);
            float rangeMinMZ = convertPlotYCoordinateToMZ(mouseAreaStopY);

            float rangeMinRT = convertPlotXCoordinateToRT(mouseAreaStartX);
            float rangeMaxRT = convertPlotXCoordinateToRT(mouseAreaStopX);

            visualizerWindow.setZoomRange(1, rangeMinRT, rangeMaxRT,
                    rangeMinMZ, rangeMaxMZ);

            mouseCursorPositionX = 0;
            mouseCursorPositionY = 0;

            float mz = convertPlotYCoordinateToMZ(mouseCursorPositionY);
            float rt = convertPlotXCoordinateToRT(mouseCursorPositionX);
            int xIndex = convertPlotXCoordinateToXIndex(mouseCursorPositionX);
            int scanNumber = -1;
            if (dataset.getScanNumber(xIndex) != null)
                scanNumber = dataset.getScanNumber(xIndex);

            CursorPosition curPos = new CursorPosition(rt, mz, 0,
                    dataset.getRawDataFile(), scanNumber);
            visualizerWindow.setCursorPosition(curPos);

            // Reset selected area
            mouseAreaStartX = -1;
            mouseAreaStartY = -1;
            mouseAreaStopX = -1;
            mouseAreaStopY = -1;

        }

        if (src == zoomOutMenuItem) {
            visualizerWindow.setFullZoom(1);

            // Reset selected area
            mouseAreaStartX = -1;
            mouseAreaStartY = -1;
            mouseAreaStopX = -1;
            mouseAreaStopY = -1;
        }

        if (src == zoomOutLittleMenuItem) {

            float currentRangeMidMZ = 0.5f * (dataset.getMinMZ() + dataset.getMaxMZ());
            float newRangeMinMZ = currentRangeMidMZ - zoomOutLittleFactor
                    * (currentRangeMidMZ - dataset.getMinMZ());
            float newRangeMaxMZ = currentRangeMidMZ + zoomOutLittleFactor
                    * (dataset.getMaxMZ() - currentRangeMidMZ);

            float currentRangeMidRT = 0.5f * (dataset.getMinRT() + dataset.getMaxRT());
            float newRangeMinRT = currentRangeMidRT - zoomOutLittleFactor
                    * (currentRangeMidRT - dataset.getMinRT());
            float newRangeMaxRT = currentRangeMidRT + zoomOutLittleFactor
                    * (dataset.getMaxRT() - currentRangeMidRT);

            visualizerWindow.setZoomRange(1, newRangeMinRT, newRangeMaxRT,
                    newRangeMinMZ, newRangeMaxMZ);

            // Reset selected area
            mouseAreaStartX = -1;
            mouseAreaStartY = -1;
            mouseAreaStopX = -1;
            mouseAreaStopY = -1;
        }

        if (src == showSpectrumPlotMenuItem) {
            float cursorRT = convertPlotXCoordinateToRT(mouseCursorPositionX);
            int xIndex = convertPlotXCoordinateToXIndex(mouseCursorPositionX);
            int scanNumber = dataset.getScanNumber(xIndex);

            SpectraVisualizer specVis = SpectraVisualizer.getInstance();
            specVis.showNewSpectrumWindow(dataset.getRawDataFile(), scanNumber);

        }

        if (src == showXICPlotMenuItem) {
            float cursorMZ = convertPlotYCoordinateToMZ(mouseCursorPositionY);
            TICVisualizer tic = TICVisualizer.getInstance();
            RawDataFile dataFile = dataset.getRawDataFile();

            float rtMin = dataFile.getDataMinRT(1);
            float rtMax = dataFile.getDataMaxRT(1);
            float mzMin = cursorMZ - defaultOneSidedXICWidth;
            float mzMax = cursorMZ + defaultOneSidedXICWidth;
            tic.showNewTICVisualizerWindow(new RawDataFile[] { dataFile },
                    null, 1, TICVisualizerParameters.plotTypeBP, rtMin, rtMax,
                    mzMin, mzMax);

        }
		
		if (src == finalizePrePeaksMenuItem) {
			// Initialize a new FinalizePrePeaksTask and run it
			FinalizePrePeaksTask task = new FinalizePrePeaksTask(visualizerWindow.getDataFile(),  prePeaks.toArray(new PreConstructionPeak[0]), visualizerWindow);
			MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH, this);
			
		}		

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {


        // Left-mouse button
        if (e.getButton() == MouseEvent.BUTTON1) {

            switch (visualizerWindow.getZoomPeakEditMode()) {
            case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_PEAKEDITMODE:

                // Create a new PreConstructionPeak
                float mz = convertPlotYCoordinateToMZ(selectionLastClickY);
                float minRT = convertPlotXCoordinateToRT(mouseAreaStartX);
                float maxRT = convertPlotXCoordinateToRT(mouseAreaStopX);

                PreConstructionPeak prePeak = new PreConstructionPeak(
                        visualizerWindow.getDataFile(), mz, minRT, maxRT);
                prePeaks.add(prePeak);
                break;

            case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_ZOOMMODE:
                break;
            }

        }

    }

    public void mousePressed(MouseEvent e) {

        // Store info about pressed button
        lastPressedMouseButton = e.getButton();

        // Right-mouse button? => show pop-up menu
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
        
        // If not the first mouse button pressed, then do nothing
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        // Get x,y position of the cursor on the 2d-plot
        mouseCursorPositionX = quantizePlotXCoordinate(e.getX());
        mouseCursorPositionY = e.getY();

        // Compute m/z, RT and scan number of the position
        float mz = convertPlotYCoordinateToMZ(mouseCursorPositionY);
        float rt = convertPlotXCoordinateToRT(mouseCursorPositionX);
        int xIndex = convertPlotXCoordinateToXIndex(mouseCursorPositionX);
        int scanNumber = -1;
        if (dataset.getScanNumber(xIndex) != null)
            scanNumber = dataset.getScanNumber(xIndex);

        // Set data cursor position on the clicked position
        CursorPosition curPos = new CursorPosition(rt, mz, 0,
                dataset.getRawDataFile(), scanNumber);
        visualizerWindow.setCursorPosition(curPos);

        // Reset previous selected area and initialize new selection to start
        // from current position
        mouseAreaStartX = -1;
        mouseAreaStartY = -1;
        mouseAreaStopX = -1;
        mouseAreaStopY = -1;
        selectionFirstClickX = mouseCursorPositionX;
        selectionFirstClickY = mouseCursorPositionY;

        switch (visualizerWindow.getZoomPeakEditMode()) {
        case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_PEAKEDITMODE:
            break;

        case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_ZOOMMODE:
            // Disable zoom option in pop-up menu (no selected area at the
            // moment)
            zoomToSelectionMenuItem.setEnabled(false);
            break;
        }

        repaint();

    }

    public void mouseDragged(MouseEvent e) {

        // If not the first mouse button pressed, then do nothing
        if (lastPressedMouseButton != MouseEvent.BUTTON1) {
            return;
        }

        // Get x,y position of the cursor on the 2d-plot
        selectionLastClickX = quantizePlotXCoordinate(e.getX());
        selectionLastClickY = e.getY();

        // Define selected area using the first and last (current) clicked
        // position
        if (selectionLastClickX > selectionFirstClickX) {
            mouseAreaStartX = selectionFirstClickX;
            mouseAreaStopX = selectionLastClickX;
        } else {
            mouseAreaStartX = selectionLastClickX;
            mouseAreaStopX = selectionFirstClickX;
        }
        if (selectionLastClickY > selectionFirstClickY) {
            mouseAreaStartY = selectionFirstClickY;
            mouseAreaStopY = selectionLastClickY;
        } else {
            mouseAreaStartY = selectionLastClickY;
            mouseAreaStopY = selectionFirstClickY;
        }

        // Compute m/z, RT and scan number of the position
        float mz = convertPlotYCoordinateToMZ(selectionLastClickY);
        float rt = convertPlotXCoordinateToRT(selectionLastClickX);
        int xIndex = convertPlotXCoordinateToXIndex(selectionLastClickX);
        int scanNumber = -1;
        if (dataset.getScanNumber(xIndex) != null)
            scanNumber = dataset.getScanNumber(xIndex);

        // Set data cursor position on the clicked position
        CursorPosition curPos = new CursorPosition(rt, mz, 0,
                dataset.getRawDataFile(), scanNumber);
        visualizerWindow.setRangeCursorPosition(curPos);

        repaint();

        switch (visualizerWindow.getZoomPeakEditMode()) {
        case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_PEAKEDITMODE:
            break;

        case OldTwoDVisualizerWindow.ZOOMPEAKEDIT_ZOOMMODE:
            // Enable zoom option in pop-up menu (selected area is available
            // now)
            zoomToSelectionMenuItem.setEnabled(true);
            break;
        }

    }

    public void mouseMoved(MouseEvent e) {
    }
	
		public void taskFinished(Task task) {

			// Remove all pre-peaks from visualizer 
			prePeaks.clear();
			
			repaint();
		
	        if (task.getStatus() == Task.TaskStatus.FINISHED) {

	            logger.info("Finished finalizing pre-peaks on "
	                    + visualizerWindow.getDataFile());

	        }

	        if (task.getStatus() == Task.TaskStatus.ERROR) {
	            String msg = "Error while finalizing pre-peaks on file "
	                    + visualizerWindow.getDataFile() + ": " + task.getErrorMessage();
	            logger.severe(msg);

	            MZmineCore.getDesktop().displayErrorMessage(msg);
	        }
		
	}


	public void taskStarted(Task task) {}


}
