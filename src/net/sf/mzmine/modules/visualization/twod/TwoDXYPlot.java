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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.logging.Logger;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

/**
 * This class is responsible for drawing the actual data points. It does not
 * used InterpolatingLookupPaintScale, because it is quite slow.
 */
class TwoDXYPlot extends XYPlot {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    TwoDDataSet dataset;

    private TwoDPaletteType paletteType = TwoDPaletteType.PALETTE_GRAY20;

    TwoDXYPlot(TwoDDataSet dataset, ValueAxis domainAxis, ValueAxis rangeAxis) {
        super(dataset, domainAxis, rangeAxis, null);
        this.dataset = dataset;

    }

    public boolean render(final Graphics2D g2, final Rectangle2D dataArea,
            int index, PlotRenderingInfo info, CrosshairState crosshairState) {

        // only render the image once
        if (index != 0)
            return false;

        // Save current time
        Date renderStartTime = new Date();
        // super.render(g2, dataArea, index, info, crosshairState);

        // prepare some necessary constants
        final int x = (int) dataArea.getX();
        final int y = (int) dataArea.getY();
        final int width = (int) dataArea.getWidth();
        final int height = (int) dataArea.getHeight();

        final float imageRTMin = (float) getDomainAxis().getRange().getLowerBound();
        final float imageRTMax = (float) getDomainAxis().getRange().getUpperBound();
        final float imageRTStep = (imageRTMax - imageRTMin) / width;
        final float imageMZMin = (float) getRangeAxis().getRange().getLowerBound();
        final float imageMZMax = (float) getRangeAxis().getRange().getUpperBound();
        final float imageMZStep = (imageMZMax - imageMZMin) / height;

        // prepare a float array of summed intensities
        float values[][] = new float[width][height];
        float maxValue = 0;
        
        

        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {

                float pointRTMin = imageRTMin + (i * imageRTStep);
                float pointRTMax = pointRTMin + imageRTStep;
                float pointMZMin = imageMZMin + (j * imageMZStep);
                float pointMZMax = pointMZMin + imageMZStep;

                values[i][j] = dataset.getMaxIntensity(pointRTMin, pointRTMax,
                        pointMZMin, pointMZMax);

                if (values[i][j] > maxValue)
                    maxValue = values[i][j];
                
               // if ((i == width - 1) && (j == height - 1)) logger.finest("max " + pointRTMax + " " + pointMZMax);

            }

        // if we have no data points, bail out
        if (maxValue == 0)
            return false;

        // Normalize all values
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                values[i][j] /= maxValue;
            }

        // prepare a bitmap of required size
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        // draw image points
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                // float intensity = 1 - (values[i][j] / maxValue);
                // System.out.println("i " + intensity);
                Color pointColor = paletteType.getColor(values[i][j]);
                // new Color(intensity, intensity, intensity); //(Color)
                // paintScale.getPaint(values[i][j]);
                // Color pointColor = (Color)
                // paintScale.getPaint(values[i][j]);
                // g2.setColor(pointColor);
                // g2.drawRect(i + x, y + height - j - 1, 1, 1);
                image.setRGB(i, height - j - 1, pointColor.getRGB());
                // image.setRGB(i, height - j - 1, (int) (255 -
                // (maxValue /
                // values[i][j]) * 255) << 16);
            }

        // Paint image
        g2.drawImage(image, x, y, null);

        Date renderFinishTime = new Date();

        logger.finest("Finished rendering 2D visualizer, "
                + (renderFinishTime.getTime() - renderStartTime.getTime())
                + " ms");

        return true;

    }

    void switchPalette() {
        TwoDPaletteType types[] = TwoDPaletteType.values();
        int newIndex = paletteType.ordinal() + 1;
        if (newIndex >= types.length)
            newIndex = 0;
        paletteType = types[newIndex];
    }

}