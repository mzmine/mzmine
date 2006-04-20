/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.JPanel;

/**
 * This class presents the y-axis of the plot
 */
class TICYAxis extends JPanel {

    private final int topMargin = 0;
    private final int bottomMargin = 0;
    private static final int STEP_PIXELS = 40;

    private DecimalFormat tickFormat;

    private TICVisualizer masterFrame;

    TICYAxis(TICVisualizer masterFrame) {
        this.masterFrame = masterFrame;
        tickFormat = new DecimalFormat("0.00E0");
        setMinimumSize(new Dimension(60, 0));
        setPreferredSize(new Dimension(60, 0));
        setBackground(Color.white);
        setForeground(Color.black);

    }

    /**
     * This method paints the y-axis
     */
    public void paint(Graphics g) {

        super.paint(g);

        int width = getWidth();
        int height = getHeight();

        double intValueMin = masterFrame.getZoomIntensityMin();
        double intValueMax = masterFrame.getZoomIntensityMax();

        double stepIncrement = (intValueMax - intValueMin)
                / (double) (height - topMargin - bottomMargin) * STEP_PIXELS;

        // Draw axis
        g.drawLine(width - 1, 0, width - 1, height);

        // Draw tics and numbers
        String tmps;
        double intensity = intValueMin;

        for (int ypos = bottomMargin; ypos < height - topMargin; ypos += STEP_PIXELS) {

            g.drawLine(width - 10, height - ypos, width, height - ypos);

            if ((ypos > 5) && (ypos < height - topMargin - 5)) {
                tmps = new String(tickFormat.format(intensity));
                g.drawBytes(tmps.getBytes(), 0, tmps.length(), 3,
                        height - ypos + 5);
            }

            intensity += stepIncrement;

        }

    }

}
