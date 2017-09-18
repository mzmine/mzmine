/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;

import com.google.common.collect.Range;

/**
 * Simple lightweight component for plotting peak shape
 */
public class PeakXICComponent extends JComponent {

    private static final long serialVersionUID = 1L;
    public static final Color XICColor = Color.blue;
    public static final Border componentBorder = BorderFactory
            .createLineBorder(Color.lightGray);

    private Feature peak;

    private Range<Double> rtRange;
    private double maxIntensity;

    /**
     * @param peak
     *            Picked peak to plot
     */
    public PeakXICComponent(Feature peak) {
        this(peak, peak.getRawDataPointsIntensityRange().upperEndpoint());
    }

    /**
     * @param peak
     *            Picked peak to plot
     */
    public PeakXICComponent(Feature peak, double maxIntensity) {

        this.peak = peak;

        // find data boundaries
        RawDataFile dataFile = peak.getDataFile();
        this.rtRange = dataFile.getDataRTRange();
        this.maxIntensity = maxIntensity;

        this.setBorder(componentBorder);

        // add tooltip
        setToolTipText(peak.toString());

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

        // If we have no data, just return
        if (scanNumbers.length == 0)
            return;

        // for each datapoint, find [X:Y] coordinates of its point in painted
        // image
        int xValues[] = new int[scanNumbers.length];
        int yValues[] = new int[scanNumbers.length];

        // find one datapoint with maximum intensity in each scan
        for (int i = 0; i < scanNumbers.length; i++) {

            double dataPointIntensity = 0;
            DataPoint dataPoint = peak.getDataPoint(scanNumbers[i]);

            if (dataPoint != null)
                dataPointIntensity = dataPoint.getIntensity();

            // get retention time (X value)
            double retentionTime = dataFile.getScan(scanNumbers[i])
                    .getRetentionTime();

            // calculate [X:Y] coordinates
            final double rtLen = rtRange.upperEndpoint()
                    - rtRange.lowerEndpoint();
            xValues[i] = (int) Math
                    .floor((retentionTime - rtRange.lowerEndpoint()) / rtLen
                            * (size.width - 1));
            yValues[i] = size.height - (int) Math.floor(
                    dataPointIntensity / maxIntensity * (size.height - 1));

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
