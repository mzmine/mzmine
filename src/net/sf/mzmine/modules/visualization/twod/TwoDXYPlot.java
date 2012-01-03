/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import net.sf.mzmine.util.Range;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetChangeEvent;

/**
 * This class is responsible for drawing the actual data points.
 */
class TwoDXYPlot extends XYPlot {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Range totalRTRange, totalMZRange;
	private BufferedImage zoomOutBitmap;

	private TwoDDataSet dataset;

	private TwoDPaletteType paletteType = TwoDPaletteType.PALETTE_GRAY20;

	private PlotMode plotMode = PlotMode.UNDEFINED;

	TwoDXYPlot(TwoDDataSet dataset, Range rtRange, Range mzRange, ValueAxis domainAxis, ValueAxis rangeAxis) {

		super(dataset, domainAxis, rangeAxis, null);

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

		final double imageRTMin = (double) getDomainAxis().getRange().getLowerBound();
		final double imageRTMax = (double) getDomainAxis().getRange().getUpperBound();
		final double imageRTStep = (imageRTMax - imageRTMin) / width;
		final double imageMZMin = (double) getRangeAxis().getRange().getLowerBound();
		final double imageMZMax = (double) getRangeAxis().getRange().getUpperBound();
		final double imageMZStep = (imageMZMax - imageMZMin) / height;

		if ((zoomOutBitmap != null) && (imageRTMin == totalRTRange.getMin())
				&& (imageRTMax == totalRTRange.getMax()) && (imageMZMin == totalMZRange.getMin())
				&& (imageMZMax == totalMZRange.getMax())
				&& (zoomOutBitmap.getWidth() == width)
				&& (zoomOutBitmap.getHeight() == height)) {
			g2.drawImage(zoomOutBitmap, x, y, null);
			return true;
		}

		// Save current time
		Date renderStartTime = new Date();

		// prepare a double array of summed intensities
		double values[][] = new double[width][height];
		double maxValue = 0;

		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++) {

				double pointRTMin = imageRTMin + (i * imageRTStep);
				double pointRTMax = pointRTMin + imageRTStep;
				double pointMZMin = imageMZMin + (j * imageMZStep);
				double pointMZMax = pointMZMin + imageMZStep;

				values[i][j] = dataset.getMaxIntensity(new Range(pointRTMin, pointRTMax),
						new Range(pointMZMin, pointMZMax), plotMode);

				if (values[i][j] > maxValue)
					maxValue = values[i][j];

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
		if ((imageRTMin == totalRTRange.getMin()) && (imageRTMax == totalRTRange.getMax())
				&& (imageMZMin == totalMZRange.getMin()) && (imageMZMax == totalMZRange.getMax())) {
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

	Range getDomainRange(){
		return new Range(getDomainAxis().getRange().getLowerBound(),getDomainAxis().getRange().getUpperBound());
	}

	Range getAxisRange(){
		return new Range(getRangeAxis().getRange().getLowerBound(),getRangeAxis().getRange().getUpperBound());
	}

	void switchPalette() {
		TwoDPaletteType types[] = TwoDPaletteType.values();
		int newIndex = paletteType.ordinal() + 1;
		if (newIndex >= types.length)
			newIndex = 0;
		paletteType = types[newIndex];
		zoomOutBitmap = null;
		datasetChanged(new DatasetChangeEvent(dataset, dataset));
	}

	PlotMode getPlotMode() {
		return plotMode;
	}

	void setPlotMode(PlotMode plotMode) {
		this.plotMode = plotMode;

		// clear the zoom out image cache
		zoomOutBitmap = null;
		
		datasetChanged(new DatasetChangeEvent(dataset, dataset));
	}

}