/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.twod;

import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Date;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;

import com.google.common.collect.Range;

/**
 * This class is responsible for drawing the actual data points. Modified by Owen Myers 2017
 */
class TwoDXYPlot extends BaseXYPlot {
  boolean datasetChanged = false;

  TwoDXYPlot(TwoDDataSet dataset, Range<Float> rtRange, Range<Double> mzRange,
      ValueAxis domainAxis, ValueAxis rangeAxis) {

    super(dataset, rtRange, mzRange, domainAxis, rangeAxis);

    this.dataset = dataset;

    totalRTRange = rtRange;
    totalMZRange = mzRange;

  }

  public boolean render(final Graphics2D g2, final Rectangle2D dataArea, int index,
      PlotRenderingInfo info, CrosshairState crosshairState) {

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

    if ((zoomOutBitmap != null) && (imageRTMin == totalRTRange.lowerEndpoint())
        && (imageRTMax == totalRTRange.upperEndpoint())
        && (imageMZMin == totalMZRange.lowerEndpoint())
        && (imageMZMax == totalMZRange.upperEndpoint()) && (zoomOutBitmap.getWidth() == width)
        && (zoomOutBitmap.getHeight() == height)
        && (!datasetChanged)) {
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

        double lv = dataset.upperEndpointIntensity(RangeUtils.toFloatRange(Range.closed(pointRTMin, pointRTMax)),
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
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    // draw image points
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++) {
        Color pointColor = paletteType.getColor(values[i][j]);
        image.setRGB(i, height - j - 1, pointColor.getRGB());
      }

    // if we are zoomed out, save the values
    if ((imageRTMin == totalRTRange.lowerEndpoint()) && (imageRTMax == totalRTRange.upperEndpoint())
        && (imageMZMin == totalMZRange.lowerEndpoint())
        && (imageMZMax == totalMZRange.upperEndpoint())) {
      zoomOutBitmap = image;
    }

    // Paint image
    g2.drawImage(image, x, y, null);

    // Set datasetChanged to false until setDataset is not called
    datasetChanged = false;

    Date renderFinishTime = new Date();

    logger.finest("Finished rendering 2D visualizer, "
        + (renderFinishTime.getTime() - renderStartTime.getTime()) + " ms");

    return true;

  }

  public void setDataset(TwoDDataSet dataset) {
    super.setDataset(dataset);
    datasetChanged = true;
  }
}
