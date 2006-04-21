/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.sf.mzmine.io.Scan;
import net.sf.mzmine.util.FormatCoordinates;

/**
 * 
 */
class ScanPlot extends JPanel implements java.awt.event.ActionListener,
        java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

    private static final int SELECTION_TOLERANCE = 10;
    
    static enum PlotMode { CENTROID, CONTINUOUS };

    private SpectrumVisualizer masterFrame;

    private JPopupMenu popupMenu;
    private JMenuItem zoomOutMenuItem;
    private JMenuItem zoomOutLittleMenuItem;
    private JMenuItem zoomSameToOthersMenuItem;
    private JMenuItem showDataPointsMenuItem;

    private boolean mousePresent = false;
    private int mousePositionX, mousePositionY;
    private int lastClickX, lastClickY;
    private boolean mouseSelection = false;
    
    private PlotMode plotMode;

    public ScanPlot(SpectrumVisualizer masterFrame) {

        this.masterFrame = masterFrame;

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // Create popup-menu
        popupMenu = new JPopupMenu();

        zoomOutMenuItem = new JMenuItem("Zoom out full");
        zoomOutMenuItem.addActionListener(this);
        popupMenu.add(zoomOutMenuItem);

        zoomOutLittleMenuItem = new JMenuItem("Zoom out little");
        zoomOutLittleMenuItem.addActionListener(this);
        popupMenu.add(zoomOutLittleMenuItem);

        zoomSameToOthersMenuItem = new JMenuItem(
                "Set same zoom to other raw data viewers");
        zoomSameToOthersMenuItem.addActionListener(this);
        popupMenu.add(zoomSameToOthersMenuItem);

        popupMenu.addSeparator();

        showDataPointsMenuItem = new JMenuItem("Show data points");
        showDataPointsMenuItem.addActionListener(this);
        popupMenu.add(showDataPointsMenuItem);

        addMouseListener(this);
        addMouseMotionListener(this);

        setMinimumSize(new Dimension(300, 100));
        setPreferredSize(new Dimension(500, 250));
        
        plotMode = PlotMode.CONTINUOUS;

    }

    public void paint(Graphics g) {

        super.paint(g);

        int width = getWidth();
        int height = getHeight();

        Scan scans[] = masterFrame.getScans();
        assert scans != null;

        final double mzValueMin = masterFrame.getZoomMZMin();
        final double mzValueMax = masterFrame.getZoomMZMax();
        final double intValueMin = masterFrame.getZoomIntensityMin();
        final double intValueMax = masterFrame.getZoomIntensityMax();
        final double xAxisStep = (mzValueMax - mzValueMin) / width;
        final double yAxisStep = (intValueMax - intValueMin) / height;


        // Draw selection
        if (mouseSelection) {
            g.setColor(Color.lightGray);
            int selX = Math.min(lastClickX, mousePositionX);
            int selY = Math.min(lastClickY, mousePositionY);
            int selWidth = Math.abs(mousePositionX - lastClickX);
            int selHeight = Math.abs(mousePositionY - lastClickY);
            if ((selWidth > SELECTION_TOLERANCE)
                    && (selHeight > SELECTION_TOLERANCE)) {
                g.drawRect(selX, selY, selWidth, selHeight);
            } else if (selWidth > SELECTION_TOLERANCE) {
                g.drawLine(lastClickX, lastClickY, mousePositionX, lastClickY);
            } else if (selHeight > SELECTION_TOLERANCE) {
                g.drawLine(lastClickX, lastClickY, lastClickX, mousePositionY);
            }
        }

        // Draw MZ value as label on each peak

        /*
         * if (masterFrame.getSpectrumSpectraMode() ==
         * masterFrame.ONE_SPECTRUM_MODE) { g.setColor(Color.black);
         * 
         * for (int pi = 0; pi < peakMZs.length; pi++) {
         * 
         * x1 = (int) (diff_x_scr * ((peakMZs[pi] - minX) / diff_x_dat)); y1 =
         * (int) (h - diff_y_scr ((peakInts[pi] - minY) / diff_y_dat)); if (y1 <
         * 15) { y1 = 15; }
         * 
         * s = formatCoordinates.formatMZValue(peakMZs[pi]); g
         * .drawBytes(s.getBytes(), 0, s.length(), (int) x1, (int) y1); }
         *  } else {
         * 
         * double realX, tmpX1, tmpX2; double interpY; g.setColor(Color.black);
         * 
         * for (int pi = 0; pi < peakMZs.length; pi++) {
         * 
         * realX = peakMZs[pi]; interpY = 0;
         * 
         * for (int ind = 0; ind < (pointsX.length - 1); ind++) { tmpX1 =
         * pointsX[ind]; tmpX2 = pointsX[ind + 1]; if ((tmpX1 <= realX) &&
         * (tmpX2 >= realX)) { interpY = (pointsY[ind] + pointsY[ind + 1]) /
         * ((double) 2); break; } }
         * 
         * x1 = (int) (diff_x_scr * ((realX - minX) / diff_x_dat)); y1 = (int)
         * (h - diff_y_scr ((interpY - minY) / diff_y_dat)); if (y1 < 15) { y1 =
         * 15; }
         * 
         * s = formatCoordinates.formatMZValue(peakMZs[pi]); g
         * .drawBytes(s.getBytes(), 0, s.length(), (int) x1, (int) y1); }
         *  }
         */
        
        // Draw linegraph 
        
        g.setColor(new Color(0,0,224));
        double mzValues[];
        double intensities[];
                         
        
        for (Scan scan: scans) {
            mzValues = scan.getMZValues();
            intensities = scan.getIntensityValues();

            int startIndex = 1, endIndex = mzValues.length - 1;
            while (mzValues[startIndex] < mzValueMin - 1) {
                if (startIndex == mzValues.length)
                    break;
                startIndex++;
            }
            startIndex--;
            while (mzValues[endIndex] > mzValueMax) {
                if (endIndex == 0)
                    break;
                endIndex--;
            }
            
            if (startIndex < endIndex) {

                int x, y, prevx = 0, prevy = 0;

                for (int ind = startIndex; ind <= endIndex; ind++) {
                    
                    x = (int) Math.round((mzValues[ind] - mzValueMin)
                            / xAxisStep);
                    y = height
                            - (int) Math.round((intensities[ind] - intValueMin)
                                    / yAxisStep);

                    if (plotMode == PlotMode.CONTINUOUS) {
                        if (ind > startIndex) 
                            g.drawLine(prevx, prevy, x, y);
                        
                    } else {
                        g.drawLine(x, y, x, 0);
                    }

                    prevx = x;
                    prevy = y;
                }
            }
            
            
        }
        
        /* 
        if (pointsX.length > 0) { prevx = pointsX[0]; prevy = pointsY[0]; }
         * int xw = (int) java.lang.Math.round(diff_x_scr / 100); int yw = (int)
         * java.lang.Math.round(diff_y_scr / 100); if (xw > yw) { xw = yw; }
         * else { yw = xw; }
         * 
         * for (int ind = 1; ind < pointsX.length; ind++) {
         * 
         * x = pointsX[ind]; y = pointsY[ind];
         * 
         * x1 = (int) (diff_x_scr * ((prevx - minX) / diff_x_dat)); x2 = (int)
         * (diff_x_scr * ((x - minX) / diff_x_dat)); y1 = (int) (h - diff_y_scr *
         * ((prevy - minY) / diff_y_dat)); y2 = (int) (h - diff_y_scr * ((y -
         * minY) / diff_y_dat));
         * 
         * if (mainWin.getParameterStorage().getGeneralParameters()
         * .getTypeOfData() ==
         * GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CONTINUOUS) {
         * g.drawLine(x1, y1, x2, y2); }
         * 
         * if (mainWin.getParameterStorage().getGeneralParameters()
         * .getTypeOfData() ==
         * GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CENTROIDS) {
         * g.drawLine(x2, (int) h, x2, y2); }
         * 
         * if ((showDataPoints == true) && (masterFrame.getSpectrumSpectraMode() ==
         * masterFrame.ONE_SPECTRUM_MODE)) { g.setColor(Color.magenta);
         * g.drawRect(x2 - xw, y2 - yw, 2 * xw, 2 * yw); g.setColor(Color.blue); }
         * 
         * prevx = x; prevy = y; }
         *  // Draw cursor position /*x = rawData.getCursorPositionMZ(); x2 =
         * (int) java.lang.Math.round((double) diff_x_scr ((double) (x - minX) /
         * (double) diff_x_dat)); g.setColor(Color.red); g.drawLine(x2, 0, x2,
         * (int) h);
         */

        // draw mouse cursor
        if (mousePresent) {
            /*
             * g.drawLine(mousePositionX - 15, mousePositionY, mousePositionX +
             * 15, mousePositionY); g.drawLine(mousePositionX, 0,
             * mousePositionX, height);
             */
            double mz = mzValueMin + xAxisStep * mousePositionX;
            double intensity = intValueMin + (intValueMax - intValueMin)
                    / height * (height - mousePositionY);
            String positionMZ = "m/z: " + FormatCoordinates.formatMZValue(mz);
            String positionInt = "IC: "
                    + FormatCoordinates.formatIntensityValue(intensity);
            int drawX = mousePositionX + 8;
            int drawY = mousePositionY - 20;

            if (drawX > width
                    - Math.max(positionMZ.length(), positionInt.length()) * 5)
                drawX = mousePositionX
                        - Math.max(positionMZ.length(), positionInt.length())
                        * 5 - 5;
            if (drawY < 5)
                drawY = mousePositionY + 15;
            g.setColor(Color.black);
            g.setFont(g.getFont().deriveFont(10.0f));
            g.drawString(positionMZ, drawX, drawY);
            g.drawString(positionInt, drawX, drawY + 12);
        }

    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        Object src = e.getSource();

        if (src == zoomOutMenuItem) {

            /*
             * rawData.clearSelectionMZ(); mouseAreaStart =
             * rawData.getCursorPositionMZ(); mouseAreaEnd =
             * rawData.getCursorPositionMZ(); //
             * mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ,
             * rawData.getRawDataID()); /* BackgroundThread bt = new
             * BackgroundThread(mainWin, msRun,
             * Visualizer.CHANGETYPE_SELECTION_MZ,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */

        }

        if (src == zoomOutLittleMenuItem) {
            /*
             * double midX = (minX+maxX)/(double)2.0; double tmpMinX, tmpMaxX;
             * 
             * if (((midX-minX)>0) && ((maxX-midX)>0)) { tmpMinX = midX -
             * (midX-minX)*(double)1.5; tmpMaxX = midX +
             * (maxX-midX)*(double)1.5; } else { tmpMinX = minX - (double)0.5;
             * tmpMaxX = maxX + (double)0.5; }
             * 
             * if (tmpMinX<rawData.getDataMinMZ()) { tmpMinX =
             * rawData.getDataMinMZ(); } if (tmpMaxX>rawData.getDataMaxMZ()) {
             * tmpMaxX = rawData.getDataMaxMZ(); }
             * 
             * rawData.setSelectionMZ(tmpMinX, tmpMaxX);
             * 
             * mouseAreaStart = rawData.getCursorPositionMZ(); mouseAreaEnd =
             * rawData.getCursorPositionMZ(); //
             * mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_MZ,
             * rawData.getRawDataID()); /* BackgroundThread bt = new
             * BackgroundThread(mainWin, msRun,
             * Visualizer.CHANGETYPE_SELECTION_MZ,
             * BackgroundThread.TASK_REFRESHVISUALIZERS); bt.start();
             */

        }

        if (src == zoomSameToOthersMenuItem) {
            // mainWin.setSameZoomToOtherRawDatas(rawData,
            // masterFrame.mainWin.SET_SAME_ZOOM_MZ);
        }

        /*
         * if (src == showDataPointsMenuItem) { if (showDataPoints==false) {
         * showDataPointsMenuItem.setText("Hide data points"); showDataPoints =
         * true; } else { showDataPointsMenuItem.setText("Show data points");
         * showDataPoints = false; } repaint(); }
         */

    }

    /**
     * Implementation of MouseListener interface methods
     */
    public void mouseClicked(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {
        mousePresent = true;
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        mousePresent = false;
        repaint();
    }

    public void mouseReleased(MouseEvent e) {

        if (mouseSelection) {

            mouseSelection = false;
            zoomOutMenuItem.setEnabled(true);
            int width = getWidth();
            int height = getHeight();
            double mzValueMin = masterFrame.getZoomMZMin();
            double mzValueMax = masterFrame.getZoomMZMax();
            double intValueMin = masterFrame.getZoomIntensityMin();
            double intValueMax = masterFrame.getZoomIntensityMax();
            int selX = Math.min(lastClickX, mousePositionX);
            int selY = height - Math.max(lastClickY, mousePositionY);
            int selWidth = Math.abs(mousePositionX - lastClickX);
            int selHeight = Math.abs(mousePositionY - lastClickY);
            double xAxisStep = (mzValueMax - mzValueMin) / width;
            double yAxisStep = (intValueMax - intValueMin) / height;

            if (selWidth > SELECTION_TOLERANCE) {
                double newMZMin = mzValueMin + (selX * xAxisStep);
                double newMZMax = mzValueMin + ((selX + selWidth) * xAxisStep);
                if (newMZMin < mzValueMin)
                    newMZMin = mzValueMin;
                if (newMZMax > mzValueMax)
                    newMZMax = mzValueMax;
                masterFrame.setMZRange(newMZMin, newMZMax);
            }
            if (selHeight > SELECTION_TOLERANCE) {
                double newIntMin = intValueMin + (selY * yAxisStep);
                double newIntMax = intValueMin
                        + ((selY + selHeight) * yAxisStep);
                if (newIntMin < intValueMin)
                    newIntMin = intValueMin;
                if (newIntMax > intValueMax)
                    newIntMax = intValueMax;
                masterFrame.setIntensityRange(newIntMin, newIntMax);
            }
            // no need to call repaint(), master frame will repaint
            // automatically
        } else if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        } 

    }

    public void mousePressed(MouseEvent e) {
        lastClickX = e.getX();
        lastClickY = e.getY();
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Implementation of methods for MouseMotionListener interface
     */
    public void mouseDragged(MouseEvent e) {
        mousePositionX = e.getX();
        mousePositionY = e.getY();
        mouseSelection = true;
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        mousePositionX = e.getX();
        mousePositionY = e.getY();
        repaint();
    }

}