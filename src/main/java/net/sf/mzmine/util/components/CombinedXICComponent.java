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

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;

/**
 * Simple lightweight component for plotting peak shape
 */
public class CombinedXICComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    public static final Border componentBorder = BorderFactory
            .createLineBorder(Color.lightGray);

    // plot colors for plotted files, circulated by numberOfDataSets
    public static final Color[] plotColors = { new Color(0, 0, 192), // blue
            new Color(192, 0, 0), // red
            new Color(0, 192, 0), // green
            Color.magenta, Color.cyan, Color.orange };

    private Feature[] peaks;

    private Range<Double> rtRange;
    private double maxIntensity;

    /**
     * @param ChromatographicPeak
     *            [] Picked peaks to plot
     */
    public CombinedXICComponent(Feature[] peaks, int id) {

        // We use the tool tip text as a id for customTooltipProvider
        if (id >= 0)
            setToolTipText(ComponentToolTipManager.CUSTOM + id);

        double maxIntensity = 0;
        this.peaks = peaks;

        // find data boundaries
        for (Feature peak : peaks) {
            if (peak == null)
                continue;

            maxIntensity = Math.max(maxIntensity,
                    peak.getRawDataPointsIntensityRange().upperEndpoint());
            if (rtRange == null)
                rtRange = peak.getDataFile().getDataRTRange();
            else
                rtRange = rtRange.span(peak.getDataFile().getDataRTRange());
        }

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

        int colorIndex = 0;
        
        for (Feature peak : peaks) {

            // set color for current XIC
            g2.setColor(plotColors[colorIndex]);
            colorIndex = (colorIndex + 1) % plotColors.length;

            // if we have no data, just return
            if ((peak == null) || (peak.getScanNumbers().length == 0))
                continue;

            // get scan numbers, one data point per each scan
            int scanNumbers[] = peak.getScanNumbers();

            // for each datapoint, find [X:Y] coordinates of its point in
            // painted image
            int xValues[] = new int[scanNumbers.length + 2];
            int yValues[] = new int[scanNumbers.length + 2];
            
            // find one datapoint with maximum intensity in each scan
            for (int i = 0; i < scanNumbers.length; i++) {

                double dataPointIntensity = 0;
                DataPoint dataPoint = peak.getDataPoint(scanNumbers[i]);

                if (dataPoint != null)
                    dataPointIntensity = dataPoint.getIntensity();

                // get retention time (X value)
                double retentionTime = peak.getDataFile()
                        .getScan(scanNumbers[i]).getRetentionTime();

                // calculate [X:Y] coordinates
                xValues[i + 1] = (int) Math
                        .floor((retentionTime - rtRange.lowerEndpoint())
                                / (rtRange.upperEndpoint()
                                        - rtRange.lowerEndpoint())
                        * (size.width - 1));
                yValues[i + 1] = size.height - (int) Math.floor(
                        dataPointIntensity / maxIntensity * (size.height - 1));
            }
            
            // add first point
            xValues[0] = xValues[1];
            yValues[0] = size.height - 1;

            // add terminal point
            xValues[xValues.length - 1] = xValues[xValues.length - 2];
            yValues[yValues.length - 1] = size.height - 1;

            // draw the peak shape
            g2.drawPolyline(xValues, yValues, xValues.length);

        }

    }

}
