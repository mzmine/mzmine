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

package net.sf.mzmine.userinterface.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;

/**
 * Simple lightweight component for plotting peak shape
 */
public class PeakXICComponent extends JComponent {

    public static final Color XICColor = Color.blue;
    public static final Border componentBorder = BorderFactory.createLineBorder(Color.lightGray);

    private Peak peak;

    private float minRT, maxRT, rtSpan, maxIntensity;

    /**
     * @param peak Picked peak to plot
     */
    public PeakXICComponent(Peak peak) {
        this(peak, peak.getDataPointMaxIntensity());
    }

    /**
     * @param peak Picked peak to plot
     */
    public PeakXICComponent(Peak peak, float maxIntensity) {

        this.peak = peak;

        // find data boundaries
        RawDataFile dataFile = peak.getDataFile();
        this.minRT = dataFile.getDataMinRT(1);
        this.maxRT = dataFile.getDataMaxRT(1);
        this.rtSpan = maxRT - minRT;
        this.maxIntensity = maxIntensity;

        this.setBorder(componentBorder);

    }

    public void paint(Graphics g) {

        super.paint(g);

        // use Graphics2D for antialiasing
        Graphics2D g2 = (Graphics2D) g;

        // turn on antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // get canvas size
        Dimension size = getSize();

        // get scan numbers, one data point per each scan
        RawDataFile dataFile = peak.getDataFile();
        int scanNumbers[] = peak.getScanNumbers();

        // for each datapoint, find [X:Y] coordinates of its point in painted
        // image
        int xValues[] = new int[scanNumbers.length];
        int yValues[] = new int[scanNumbers.length];

        // find one datapoint with maximum intensity in each scan
        for (int i = 0; i < scanNumbers.length; i++) {

            DataPoint dataPoint = peak.getDatapoint(scanNumbers[i]);

            // get retention time (X value)
            float retentionTime = dataFile.getScan(scanNumbers[i]).getRetentionTime();

            // calculate [X:Y] coordinates
            xValues[i] = (int) Math.floor((retentionTime - minRT) / rtSpan
                    * (size.width - 1));
            yValues[i] = size.height
                    - (int) Math.floor(dataPoint.getIntensity() / maxIntensity
                            * (size.height - 1));

        }

        // create a path for a peak polygon
        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        path.moveTo(xValues[0], size.height - 1);

        // add data points to the path
        for (int i = 0; i < (xValues.length - 1); i++) {
            path.lineTo(xValues[i + 1], yValues[i + 1]);
        }
        path.lineTo(xValues[xValues.length - 1], size.height - 1);

        // close the path to form a polygon
        path.closePath();

        // fill the peak area
        g2.setColor(XICColor);
        g2.fill(path);

    }

}
