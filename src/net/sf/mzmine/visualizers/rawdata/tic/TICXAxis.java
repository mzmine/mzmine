/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.Color;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.FormatCoordinates;


/**
 *
 */
class TICXAxis extends JPanel {

    private final int leftMargin = 50;
    private final int rightMargin = 5;

    private int minX;
    private int maxX;


    /**
     * This method paints the x-axis
     */
    public void paint(Graphics g) {

        super.paint(g);

        FormatCoordinates formatCoordinates = new FormatCoordinates(MainWindow.getInstance()
                .getParameterStorage().getGeneralParameters());

        // Calc some dimensions with depend on the panel width (in pixels)
        // and plot area (in scans)
        int w = getWidth();
        double h = getHeight();

        // / - number of pixels per scan
        int numofscans = maxX - minX;
        double pixelsperscan = (double) (w - leftMargin - rightMargin)
                / (double) numofscans;

        // - number of scans between tic marks
        int scanspertic = 1;
        while ((scanspertic * pixelsperscan) < 60) {
            scanspertic++;
        }

        // - number of pixels between tic marks
        double pixelspertic = (double) scanspertic * pixelsperscan;
        int numoftics = (int) java.lang.Math.floor((double) numofscans
                / (double) scanspertic);

        // Draw axis
        this.setForeground(Color.black);
        g.drawLine((int) leftMargin, 0, (int) (w - rightMargin), 0);

        // Draw tics and numbers
        String tmps;
        double xpos = leftMargin;
        int xval = minX;
        for (int t = 0; t < numoftics; t++) {
            // if (t==(numoftics-1)) { this.setForeground(Color.red); }

            tmps = formatCoordinates.formatRTValue(xval, null); // TODO

            g.drawLine((int) java.lang.Math.round(xpos), 0,
                    (int) java.lang.Math.round(xpos), (int) (h / 4));
            g.drawBytes(tmps.getBytes(), 0, tmps.length(),
                    (int) java.lang.Math.round(xpos), (int) (3 * h / 4));

            xval += scanspertic;
            xpos += pixelspertic;

        }
    }


}