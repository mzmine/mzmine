/**
 * 
 */
package net.sf.mzmine.userinterface.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.sf.mzmine.util.format.ValueFormat;

/**
 * This class presents the y-axis of the plot
 */
public class YAxis extends JPanel {

    private static final int MIN_WIDTH = 50;
    private static final int DEFAULT_STEP_PIXELS = 40;

    private int topMargin, bottomMargin;

    private double min, max;

    private ValueFormat format;

    public YAxis(double min, double max, int topMargin, int bottomMargin,
            ValueFormat format) {

        this.min = min;
        this.max = max;
        this.topMargin = topMargin;
        this.bottomMargin = bottomMargin;
        this.format = format;

        setMinimumSize(new Dimension(MIN_WIDTH, 0));
        setPreferredSize(new Dimension(MIN_WIDTH, 0));
        setBackground(Color.white);

    }

    public void setRange(double min, double max) {
        this.min = min;
        this.max = max;
        repaint();
    }

    /**
     * This method paints the y-axis
     */
    public void paint(Graphics g) {

        super.paint(g);

        int width = getWidth();
        int height = getHeight();

        int lowestMark = Math.max(bottomMargin, 5);
        int highestMark = height - Math.max(topMargin, 5);
        int numberOfMarks = (highestMark - lowestMark) / DEFAULT_STEP_PIXELS;
        double yAxisStep = (max - min)
                / (height - topMargin - bottomMargin - 1);
        int stepIncrement = (highestMark - lowestMark) / numberOfMarks;

        // Draw axis
        g.drawLine(width - 1, 0, width - 1, height);

        // Draw tics and numbers
        g.setFont(g.getFont().deriveFont(11.0f));
        String tmps;
        double value;

        for (int ypos = lowestMark; ypos <= highestMark; ypos += stepIncrement) {

            g.drawLine(width - 8, height - ypos, width, height - ypos);
            value = min + ((ypos - bottomMargin) * yAxisStep);
            tmps = new String(format.format(value));
            g
                    .drawBytes(tmps.getBytes(), 0, tmps.length(), 3, height
                            - ypos + 5);

        }

    }

}
