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

package net.sf.mzmine.util.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

/**
 * Simple lightweight component for plotting peak shape
 */
public class CombinedXICComponent extends JComponent {

    public static final Border componentBorder = BorderFactory.createLineBorder(Color.lightGray);

    // plot colors for plotted files, circulated by numberOfDataSets
    public static final Color[] plotColors = { new Color(0, 0, 192), // blue
            new Color(192, 0, 0), // red
            new Color(0, 192, 0), // green
            Color.magenta, Color.cyan, Color.orange };

    private PeakListRow peaks;

    private Range rtRange;
    private float maxIntensity;

    /**
     * @param peak Picked peak to plot
     */
    public CombinedXICComponent(PeakListRow peaks) {
        this(peaks, peaks.getDataPointMaxIntensity());
    }

    /**
     * @param peak Picked peak to plot
     */
    public CombinedXICComponent(PeakListRow peaks, float maxIntensity) {

        this.peaks = peaks;

        // find data boundaries
        for (RawDataFile dataFile : peaks.getRawDataFiles()) {
            if (rtRange == null)
                rtRange = dataFile.getDataRTRange(1);
            else
                rtRange.extendRange(dataFile.getDataRTRange(1));
        }

        this.maxIntensity = maxIntensity;

        this.setBorder(componentBorder);

        StringBuffer toolTip = new StringBuffer();
        toolTip.append("<html>");

        int colorIndex = 0;

        NumberFormat intensityFormat = MZmineCore.getIntensityFormat();
        
        for (RawDataFile dataFile : peaks.getRawDataFiles()) {

            ChromatographicPeak peak = peaks.getPeak(dataFile);

            // if we have no data, just return
            if (peak.getScanNumbers().length == 0)
                continue;

            String htmlColor = Integer.toHexString(
                    plotColors[colorIndex].getRGB()).substring(2);

            toolTip.append("<font color='#" + htmlColor + "'>");
            toolTip.append(dataFile.getFileName());
            toolTip.append("</font>: ");
            toolTip.append(peak.toString());
            toolTip.append(", <b>" + intensityFormat.format(peak.getHeight()) + "</b>");
            toolTip.append("<br>");
            colorIndex++;

        }

        toolTip.append("</html>");

        // add tooltip
        setToolTipText(toolTip.toString());

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

        for (RawDataFile dataFile : peaks.getRawDataFiles()) {

            ChromatographicPeak peak = peaks.getPeak(dataFile);

            // if we have no data, just return
            if ((peak == null) || (peak.getScanNumbers().length == 0))
                continue;

            // get scan numbers, one data point per each scan
            int scanNumbers[] = peak.getScanNumbers();

            // set color for current XIC
            g2.setColor(plotColors[colorIndex]);
            colorIndex++;

            // for each datapoint, find [X:Y] coordinates of its point in
            // painted image
            int xValues[] = new int[scanNumbers.length + 2];
            int yValues[] = new int[scanNumbers.length + 2];

            // find one datapoint with maximum intensity in each scan
            for (int i = 0; i < scanNumbers.length; i++) {

                float dataPointIntensity = 0;
                DataPoint dataPoint = peak.getMzPeak(scanNumbers[i]);

                if (dataPoint != null)
                    dataPointIntensity = dataPoint.getIntensity();

                // get retention time (X value)
                float retentionTime = dataFile.getScan(scanNumbers[i]).getRetentionTime();

                // calculate [X:Y] coordinates
                xValues[i + 1] = (int) Math.floor((retentionTime - rtRange.getMin())
                        / rtRange.getSize() * (size.width - 1));
                yValues[i + 1] = size.height
                        - (int) Math.floor(dataPointIntensity / maxIntensity
                                * (size.height - 1));

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
