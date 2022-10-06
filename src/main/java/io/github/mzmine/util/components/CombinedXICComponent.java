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

package io.github.mzmine.util.components;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import org.jfree.fx.FXGraphics2D;

/**
 * Simple lightweight component for plotting peak shape
 */
public class CombinedXICComponent extends Canvas {

  // public static final Border componentBorder = BorderFactory.createLineBorder(Color.lightGray);

  // plot colors for plotted files, circulated by numberOfDataSets
  public static final Color[] plotColors = {new Color(0, 0, 192), // blue
      new Color(192, 0, 0), // red
      new Color(0, 192, 0), // green
      Color.magenta, Color.cyan, Color.orange};

  private Feature[] peaks;

  private Range<Float> rtRange;
  private double maxIntensity;

  /**
   * @param peaks [] Picked peaks to plot
   */
  public CombinedXICComponent(Feature[] peaks, int id) {

    // We use the tool tip text as a id for customTooltipProvider
    if (id >= 0) {
      Tooltip tooltip = new Tooltip(ComponentToolTipManager.CUSTOM + id);
      Tooltip.install(this, tooltip);
    }

    double maxIntensity = 0;
    this.peaks = peaks;

    // find data boundaries
    for (Feature peak : peaks) {
      if (peak == null)
        continue;

      maxIntensity = Math.max(maxIntensity, peak.getRawDataPointsIntensityRange().upperEndpoint());
      if (rtRange == null)
        rtRange = peak.getRawDataFile().getDataRTRange();
      else
        rtRange = rtRange.span(peak.getRawDataFile().getDataRTRange());
    }

    this.maxIntensity = maxIntensity;

    paint();

    widthProperty().addListener(e -> paint());
    heightProperty().addListener(e -> paint());

  }

  @Override
  public boolean isResizable() {
    return true;
  }

  private void paint() {

    // use Graphics2D for antialiasing
    Graphics2D g2 = new FXGraphics2D(this.getGraphicsContext2D());

    // turn on antialiasing
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int colorIndex = 0;

    for (Feature peak : peaks) {

      // set color for current XIC
      g2.setColor(plotColors[colorIndex]);
      colorIndex = (colorIndex + 1) % plotColors.length;

      // if we have no data, just return
      if ((peak == null) || (peak.getScanNumbers().size() == 0))
        continue;

      // get scan numbers, one data point per each scan
      List<Scan> scans = peak.getScanNumbers();
      int numberOfScans = scans.size();

      // for each datapoint, find [X:Y] coordinates of its point in
      // painted image
      int xValues[] = new int[numberOfScans + 2];
      int yValues[] = new int[numberOfScans + 2];

      // find one datapoint with maximum intensity in each scan
      for (int i = 0; i < numberOfScans; i++) {

        double dataPointIntensity = 0;
        DataPoint dataPoint = peak.getDataPointAtIndex(i);

        if (dataPoint != null)
          dataPointIntensity = dataPoint.getIntensity();

        // get retention time (X value)
        float retentionTime = scans.get(i).getRetentionTime();

        // calculate [X:Y] coordinates
        xValues[i + 1] = (int) Math.floor((retentionTime - rtRange.lowerEndpoint())
            / (rtRange.upperEndpoint() - rtRange.lowerEndpoint()) * ((int) getWidth() - 1));
        yValues[i + 1] = (int) getHeight()
            - (int) Math.floor(dataPointIntensity / maxIntensity * ((int) getHeight() - 1));
      }

      // add first point
      xValues[0] = xValues[1];
      yValues[0] = (int) getHeight() - 1;

      // add terminal point
      xValues[xValues.length - 1] = xValues[xValues.length - 2];
      yValues[yValues.length - 1] = (int) getHeight() - 1;

      // draw the peak shape
      g2.drawPolyline(xValues, yValues, xValues.length);

    }

  }

}
