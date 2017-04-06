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
import org.jfree.data.general.DatasetChangeEvent;

import com.google.common.collect.Range;

/**
 * This class is responsible for drawing the actual data points.
 * Modified by Owen Myers 2017
 */
class TwoDXYPlot extends BaseXYPlot {

    TwoDXYPlot(TwoDDataSet dataset, Range<Double> rtRange,
	    Range<Double> mzRange, ValueAxis domainAxis, ValueAxis rangeAxis) {

	super(dataset, rtRange, mzRange, domainAxis, rangeAxis);

	this.dataset = dataset;

	totalRTRange = rtRange;
	totalMZRange = mzRange;

    }

    public boolean render(final Graphics2D g2, final Rectangle2D dataArea,
	    int index, PlotRenderingInfo info, CrosshairState crosshairState) {

	// if this is not TwoDDataSet
	if (index != 0)
	    return super.render(g2, dataArea, index, info, crosshairState);

	// prepare some necessary constants
	final int x = (int) dataArea.getX();
	final int y = (int) dataArea.getY();
	final int width = (int) dataArea.getWidth();
	final int height = (int) dataArea.getHeight();

	final double imageRTMin = (double) getDomainAxis().getRange()
		.getLowerBound();
	final double imageRTMax = (double) getDomainAxis().getRange()
		.getUpperBound();
	final double imageRTStep = (imageRTMax - imageRTMin) / width;
	final double imageMZMin = (double) getRangeAxis().getRange()
		.getLowerBound();
	final double imageMZMax = (double) getRangeAxis().getRange()
		.getUpperBound();
	final double imageMZStep = (imageMZMax - imageMZMin) / height;

	if ((zoomOutBitmap != null)
		&& (imageRTMin == totalRTRange.lowerEndpoint())
		&& (imageRTMax == totalRTRange.upperEndpoint())
		&& (imageMZMin == totalMZRange.lowerEndpoint())
		&& (imageMZMax == totalMZRange.upperEndpoint())
		&& (zoomOutBitmap.getWidth() == width)
		&& (zoomOutBitmap.getHeight() == height)) {
	    g2.drawImage(zoomOutBitmap, x, y, null);
	    return true;
	}

	// Save current time
	Date renderStartTime = new Date();

	// prepare a double array of summed intensities
	double values[][] = new double[width][height];
	maxValue = 0; // now this is an instance variable

	for (int i = 0; i < width; i++)
	    for (int j = 0; j < height; j++) {

		double pointRTMin = imageRTMin + (i * imageRTStep);
		double pointRTMax = pointRTMin + imageRTStep;
		double pointMZMin = imageMZMin + (j * imageMZStep);
		double pointMZMax = pointMZMin + imageMZStep;

		double lv = dataset.upperEndpointIntensity(
			Range.closed(pointRTMin, pointRTMax),
			Range.closed(pointMZMin, pointMZMax), plotMode);

		if (logScale) {
		    lv = Math.log10(lv);
		    if (lv < 0 || Double.isInfinite(lv))
			lv = 0;
		    values[i][j] = lv;
		    // values[r.nextInt(width)][r.nextInt(height)] = lv;
		} else {
		    values[i][j] = lv;
		}

		if (lv > maxValue)
		    maxValue = lv;

	    }

	// This should never happen, but just for correctness
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
		Color pointColor = paletteType.getColor(values[i][j]);
		image.setRGB(i, height - j - 1, pointColor.getRGB());
	    }

	// if we are zoomed out, save the values
	if ((imageRTMin == totalRTRange.lowerEndpoint())
		&& (imageRTMax == totalRTRange.upperEndpoint())
		&& (imageMZMin == totalMZRange.lowerEndpoint())
		&& (imageMZMax == totalMZRange.upperEndpoint())) {
	    zoomOutBitmap = image;
	}

	// Paint image
	g2.drawImage(image, x, y, null);

	Date renderFinishTime = new Date();

	logger.finest("Finished rendering 2D visualizer, "
		+ (renderFinishTime.getTime() - renderStartTime.getTime())
		+ " ms");

	return true;

    }
}