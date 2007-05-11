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

package net.sf.mzmine.modules.visualization.rawdata.twod;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;

/**
 * This class is responsible for drawing the actual data points.
 */
class TwoDXYPlot extends XYPlot {

    TwoDDataSet dataset;

    TwoDXYPlot(TwoDDataSet dataset, ValueAxis domainAxis, ValueAxis rangeAxis,
            XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
        this.dataset = dataset;
    }

    public boolean render(Graphics2D g2, Rectangle2D area, int index,
            PlotRenderingInfo info, CrosshairState crosshairState) {

        BufferedImage image = dataset.getCurrentImage();

        if (image != null) {

            AffineTransform transform = AffineTransform.getTranslateInstance(
                    area.getX(), area.getY());

            AffineTransform scaleTransform = AffineTransform.getScaleInstance(
                    area.getWidth() / image.getWidth(), 
                    area.getHeight() / image.getHeight());

            transform.concatenate(scaleTransform);

            g2.drawRenderedImage(image, transform);
            
            return true;
            
        }
        
        return false;

    }

}