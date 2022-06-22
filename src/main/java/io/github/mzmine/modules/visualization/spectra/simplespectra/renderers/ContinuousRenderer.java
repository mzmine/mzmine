/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.main.MZmineCore;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class ContinuousRenderer extends XYLineAndShapeRenderer {

  public static final float TRANSPARENCY = 0.8f;
  public static final AlphaComposite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
      TRANSPARENCY);

  /**
   *
   */
  @Serial
  private static final long serialVersionUID = 1L;

  // data points shape
  private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2, 5, 5);
  private final boolean isTransparent;

  public ContinuousRenderer(Color color, boolean isTransparent) {

    this.isTransparent = isTransparent;

    // Set painting color
    setDefaultPaint(color);
    setDefaultFillPaint(color);
    setUseFillPaint(true);
    setDefaultStroke(MZmineCore.getConfiguration().getDefaultChartTheme().getDefaultDataStroke());
    setDefaultOutlineStroke(
        MZmineCore.getConfiguration().getDefaultChartTheme().getDefaultDataStroke());

    // Set shape properties
    setDefaultShape(dataPointsShape);
    setDefaultShapesFilled(true);
    setDefaultShapesVisible(false);
    setDrawOutlines(false);

    // Set the tooltip generator
    SpectraToolTipGenerator tooltipGenerator = new SpectraToolTipGenerator();
    setDefaultToolTipGenerator(tooltipGenerator);

    setDrawSeriesLineAsPath(true);
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    if (isTransparent) {
      g2.setComposite(alphaComp);
    }

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);

  }

  /**
   * This method returns null, because we don't want to change the colors dynamically.
   */
  public DrawingSupplier getDrawingSupplier() {
    return null;
  }

}
