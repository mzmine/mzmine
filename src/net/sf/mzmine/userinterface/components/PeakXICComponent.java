/*
 * Copyright 2006-2007 The MZmine Development Team
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
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * Simple lightweight component for plotting peak shape
 */
public class PeakXICComponent extends JComponent {

    private BufferedImage bigImage;
    private Image scaledImage;
    private Dimension myOldSize;

    
    /**
     * @param peak
     * @param width
     * @param height
     */
    public PeakXICComponent(Peak peak, int width, int height) {

        bigImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bigImage.createGraphics();
        g2.setColor(Color.blue);
        
        OpenedRawDataFile dataFile = peak.getDataFile();
        int scanNumbers[] = peak.getScanNumbers();

        int xValues[] = new int[scanNumbers.length];
        int yValues[] = new int[scanNumbers.length];

        double minRT = dataFile.getCurrentFile().getDataMinRT(1);
        double maxRT = dataFile.getCurrentFile().getDataMaxRT(1);
        double rtSpan = maxRT - minRT;
        double peakHeight = peak.getHeight();

        for (int i = 0; i < scanNumbers.length; i++) {

            double dataPoints[][] = peak.getRawDatapoints(scanNumbers[i]);
            // find maximum intensity
            double intensity = 0;
            for (int j = 0; j < dataPoints.length; j++) {
                double[] point = dataPoints[j];
                if (point[1] > intensity)
                    intensity = point[1];
            }
            double retentionTime = dataFile.getCurrentFile().getRetentionTime(
                    scanNumbers[i]);

            xValues[i] = (int) Math.floor((retentionTime - minRT) / rtSpan
                    * (width - 1));
            yValues[i] = height
                    - (int) Math.floor(intensity / peakHeight * (height - 1));

        }

        for (int i = 0; i < xValues.length - 1; i++) {
            g2.drawLine(xValues[i], yValues[i], xValues[i + 1], yValues[i + 1]);
        }
        
        g2.setColor(Color.darkGray);
        g2.drawLine(0, 0, 0, height - 1);
        g2.drawLine(0, height - 1, width - 1, height - 1);

    }

    public void paint(Graphics g) {
        Dimension size = getSize();
        if ((myOldSize == null) || (! size.equals(myOldSize))) {
            scaledImage = bigImage.getScaledInstance(size.width, size.height,
                    Image.SCALE_SMOOTH);
            myOldSize = size;
        }
        g.drawImage(scaledImage, 0, 0, null);
    }

}
