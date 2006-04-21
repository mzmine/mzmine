/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.sf.mzmine.util.FormatCoordinates;


/**
 *
 */
class ScanXAxis extends JPanel {

    private static final int leftMargin = 60;
    private static final int rightMargin = 5;
    private static final int MIN_HEIGHT = 25;
    private static final int STEP_PIXELS = 60;
    
    private SpectrumVisualizer masterFrame;
    

    public ScanXAxis(SpectrumVisualizer masterFrame) {
        this.masterFrame = masterFrame;
        setMinimumSize(new Dimension(0, MIN_HEIGHT));
        setPreferredSize(new Dimension(0, MIN_HEIGHT));
        setBackground(Color.white);
        setForeground(Color.black);
    }


    public void paint(Graphics g) {

        super.paint(g);

        // Calc some dimensions with depend on the panel width (in pixels)
        // and plot area (in scans)
        int width = getWidth();
        int height = getHeight();

        double mzValueMin = masterFrame.getZoomMZMin();
        double mzValueMax = masterFrame.getZoomMZMax();

        double stepIncrement = (mzValueMax - mzValueMin)
                / (double) (width - leftMargin - rightMargin) * STEP_PIXELS;

        // Draw axis

        g.drawLine(leftMargin, 0, width - rightMargin, 0);

        // Draw tics and numbers
        String tmps;
        double mz = mzValueMin;

        for (int xpos = leftMargin; xpos < width - rightMargin; xpos += STEP_PIXELS) {

            g.drawLine(xpos, 0, xpos, height / 4);

            if (xpos < width - rightMargin - 20) {
                tmps = FormatCoordinates.formatMZValue(mz);
                g.drawBytes(tmps.getBytes(), 0, tmps.length(), xpos - (tmps.length() * 4),
                        (int) (3 * height / 4));
            }
            mz += stepIncrement;

        }
    }

}

