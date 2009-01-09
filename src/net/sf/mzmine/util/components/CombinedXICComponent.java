/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.PeakListRow;
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

    //private PeakListRow peaks;
    private ChromatographicPeak[] peaks;

    private Range rtRange;
    private double maxIntensity;

    /**
     * @param PeakListRow Picked peak to plot
     */
    public CombinedXICComponent(PeakListRow row) {
    	this(row.getPeaks());
    }

    /**
     * @param ChromatographicPeak[] Picked peaks to plot
     */
    public CombinedXICComponent(ChromatographicPeak[] peaks) {

    	double maxIntensity = 0;
    	this.peaks = peaks;

        // find data boundaries
        for (ChromatographicPeak peak : peaks) {
        	maxIntensity = Math.max(maxIntensity, peak.getRawDataPointsIntensityRange().getMax());
            if (rtRange == null)
                rtRange = peak.getDataFile().getDataRTRange(1);
            else
                rtRange.extendRange(peak.getDataFile().getDataRTRange(1));
        }

        this.maxIntensity = maxIntensity;

        this.setBorder(componentBorder);
        
        setToolTipContent();
        
    }
    
    private void setToolTipContent(){

        StringBuffer toolTip = new StringBuffer();
        toolTip.append("<html>");

        int colorIndex = 0;

        NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

        for (ChromatographicPeak peak : peaks) {

            //ChromatographicPeak peak = peaks.getPeak(dataFile);

            // if we have no data, just return
            if (peak.getScanNumbers().length == 0)
                continue;

            String htmlColor = Integer.toHexString(
                    plotColors[colorIndex].getRGB()).substring(2);

            toolTip.append("<font color='#" + htmlColor + "'>");
            toolTip.append(peak.getDataFile().getFileName());
            toolTip.append("</font>: ");
            toolTip.append(peak.toString());
            toolTip.append(", <b>" + intensityFormat.format(peak.getHeight())
                    + "</b>");
            toolTip.append("<br>");
            colorIndex = (colorIndex + 1) % plotColors.length;

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

        for (ChromatographicPeak peak : peaks) {

            //ChromatographicPeak peak = peaks.getPeak(dataFile);

            // if we have no data, just return
            if ((peak == null) || (peak.getScanNumbers().length == 0))
                continue;

            // get scan numbers, one data point per each scan
            int scanNumbers[] = peak.getScanNumbers();

            // set color for current XIC
            g2.setColor(plotColors[colorIndex]);
            colorIndex = (colorIndex + 1) % plotColors.length;

            // for each datapoint, find [X:Y] coordinates of its point in
            // painted image
            int xValues[] = new int[scanNumbers.length + 2];
            int yValues[] = new int[scanNumbers.length + 2];

            // find one datapoint with maximum intensity in each scan
            for (int i = 0; i < scanNumbers.length; i++) {

                double dataPointIntensity = 0;
                MzDataPoint dataPoint = peak.getMzPeak(scanNumbers[i]);

                if (dataPoint != null)
                    dataPointIntensity = dataPoint.getIntensity();

                // get retention time (X value)
                double retentionTime = peak.getDataFile().getScan(scanNumbers[i]).getRetentionTime();

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
