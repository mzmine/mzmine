/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.twod;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.FormatCoordinates;


/**
 *
 */
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
            // tmps = new String("" + (int)yval);
            //tmps = new String(tickFormat.format(yval));
            tmps = FormatCoordinates.formatMZValue(yval);
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