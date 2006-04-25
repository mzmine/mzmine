/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.sf.mzmine.io.Scan;
import net.sf.mzmine.obsoletedatastructures.FormatCoordinates;

/**
 * 
 */
class SpectrumPlot extends JPanel implements 
        MouseListener, MouseMotionListener {

    static final int SELECTION_TOLERANCE = 10;
    
    static enum PlotMode { CENTROID, CONTINUOUS };
    
    static final Color plotColor = new Color(0,0,224); 

    private SpectrumVisualizer masterFrame;
    
    private boolean mousePresent = false;
    private int mousePositionX, mousePositionY;
    private int lastClickX, lastClickY;
    private boolean mouseSelection = false;
    private boolean showDataPoints = false;
    
    private PlotMode plotMode = PlotMode.CONTINUOUS;
    
    private double mzValueMin;
    private double mzValueMax;
    private double intValueMin;
    private double intValueMax;

    SpectrumPlot(SpectrumVisualizer masterFrame) {

        this.masterFrame = masterFrame;

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));


        addMouseListener(this);
        addMouseMotionListener(this);

        setMinimumSize(new Dimension(300, 100));
        setPreferredSize(new Dimension(500, 250));
        
        plotMode = PlotMode.CONTINUOUS;

    }
    
    void setMZRange(double min, double max) {
        mzValueMin = min;
        mzValueMax = max;
        repaint();
    }
    
    void setIntensityRange(double min, double max) {
        intValueMin = min;
        intValueMax = max;
        repaint();
    }

    public void paint(Graphics g) {

        super.paint(g);

        int width = getWidth();
        int height = getHeight();

        Scan scans[] = masterFrame.getScans();
        assert scans != null;

        double xAxisStep = (mzValueMax - mzValueMin) / width;
        double yAxisStep = (intValueMax - intValueMin) / height;


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

        // TODO: Draw MZ value as label on each peak
        
        
        // Draw linegraph 
        g.setColor(plotColor);
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

                    if (showDataPoints) {
                        g.fillOval(x - 2, y - 2, 4, 4);
                    }
                    
                    if (plotMode == PlotMode.CONTINUOUS) {
                        if (ind > startIndex) 
                            g.drawLine(prevx, prevy, x, y);
                        
                    } else {
                        g.drawLine(x, y, x, height);
                    }

                    prevx = x;
                    prevy = y;
                }
            }
            
            
        }
        
 
        // draw mouse cursor
        if (mousePresent) {
 
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

    boolean getShowDataPoints() {
        return showDataPoints;
    }
    
    /**
     * @param showDataPoints The showDataPoints to set.
     */
    void setShowDataPoints(boolean showDataPoints) {
        this.showDataPoints = showDataPoints;
        repaint();
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
            
            int width = getWidth();
            int height = getHeight();
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
            masterFrame.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
        } 

    }

    public void mousePressed(MouseEvent e) {
        lastClickX = e.getX();
        lastClickY = e.getY();
        if (e.isPopupTrigger()) {
            masterFrame.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
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
    
    /**
     * @param plotMode The plotMode to set.
     */
    void setPlotMode(PlotMode plotMode) {
        this.plotMode = plotMode;
        repaint();
    }
    
    PlotMode getPlotMode() {
        return plotMode;
    }

}