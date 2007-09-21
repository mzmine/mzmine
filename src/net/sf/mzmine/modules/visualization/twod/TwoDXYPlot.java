/*
 * Copyright 2006 The MZmine Development Team
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

import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

/**
 * This class is responsible for drawing the actual data points.
 */
class TwoDXYPlot extends XYPlot {

    TwoDDataSet dataset;

    TwoDXYPlot(TwoDDataSet dataset, ValueAxis domainAxis, ValueAxis rangeAxis) {
        super(dataset, domainAxis, rangeAxis, null);
        this.dataset = dataset;
        
        
    }
    
    public boolean render(Graphics2D g2,
            Rectangle2D dataArea,
            int index,
            PlotRenderingInfo info,
            CrosshairState crosshairState)
    {
     
        //super.render(g2, dataArea, index, info, crosshairState);
        
        // only render the image once
        if (index != 0) return false;
        
        // prepare some necessary constants
        final int x = (int) dataArea.getX();
        final int y = (int) dataArea.getY();
        final int width = (int) dataArea.getWidth();
        final int height = (int) dataArea.getHeight();
        
        final float imageRTMin = (float) getDomainAxis().getRange().getLowerBound();
        final float imageRTMax = (float) getDomainAxis().getRange().getUpperBound();
        final float imageRTStep = (imageRTMax - imageRTMin) / height;
        final float imageMZMin = (float) getRangeAxis().getRange().getLowerBound();
        final float imageMZMax = (float) getRangeAxis().getRange().getUpperBound();
        final float imageMZStep = (imageMZMax - imageMZMin) / height;
        
        // prepare a float array of summed intensities 
        float summedValues[][] = new float[width][height];
        float maxValue = 0;
        
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                
                float pointRTMin = imageRTMin + (i * imageRTStep);
                float pointRTMax = pointRTMin + imageRTStep;
                float pointMZMin = imageMZMin + (j * imageMZStep);
                float pointMZMax = pointMZMin + imageMZStep;
                
                summedValues[i][j] = dataset.getSummedIntensity(pointRTMin, pointRTMax, pointMZMin, pointMZMax);
                
                if (summedValues[i][j] > maxValue) maxValue = summedValues[i][j];
                
            }
        
        // if we have no data points, bail out
        if (maxValue == 0) return false;

        InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
        paintScale.add(0.0, Color.white);
        paintScale.add(0.2*maxValue, Color.black);
        
        // prepare a bitmap of required size
        BufferedImage image = new BufferedImage(width,
                height, BufferedImage.TYPE_INT_RGB);
        
        // draw image points
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
            {
                Color pointColor = (Color) paintScale.getPaint(summedValues[i][j]);
                image.setRGB(i, height - j - 1, pointColor.getRGB());
            }
        
        // paint image
        g2.drawImage(image, x, y, null);
    
        return true;
    }


    

}