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
 *
 */

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.annotations.AbstractXYAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.LineUtils;

public class NeutralLossAnnotation extends AbstractXYAnnotation {

  private final double x1;
  private final double x2;
  private final double y1;
  private final double y2;
  private final String text;
  private final double spaceInPixel;
  private final Paint paint;
  private final Stroke stroke;

  public NeutralLossAnnotation(double x1, double x2, double y1, double y2, String text,
      double spaceInPixel, Paint paint, Stroke stroke) {
    this.x1 = x1;
    this.x2 = x2;
    this.y1 = y1;
    this.y2 = y2;
    this.text = text;
    this.spaceInPixel = spaceInPixel;
    this.paint = paint;

    this.stroke = stroke;
  }

  @Override
  public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea, ValueAxis domainAxis,
      ValueAxis rangeAxis, int i, PlotRenderingInfo plotRenderingInfo) {

    PlotOrientation orientation = plot.getOrientation();
    RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(plot.getDomainAxisLocation(),
        orientation);
    RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(plot.getRangeAxisLocation(),
        orientation);

    float j2DX1 = 0.0f;
    float j2DX2 = 0.0f;
    float j2DY1 = 0.0f;
    float j2DY2 = 0.0f;
    if (orientation == PlotOrientation.VERTICAL) {

      j2DX1 = (float) domainAxis.valueToJava2D(this.x1, dataArea, domainEdge);
      j2DY1 = (float) rangeAxis.valueToJava2D(this.y1, dataArea, rangeEdge);
      j2DX2 = (float) domainAxis.valueToJava2D(this.x2, dataArea, domainEdge);
      j2DY2 = (float) rangeAxis.valueToJava2D(this.y2, dataArea, rangeEdge);
    } else if (orientation == PlotOrientation.HORIZONTAL) {
      j2DY1 = (float) domainAxis.valueToJava2D(this.x1, dataArea, domainEdge);
      j2DX1 = (float) rangeAxis.valueToJava2D(this.y1, dataArea, rangeEdge);
      j2DY2 = (float) domainAxis.valueToJava2D(this.x2, dataArea, domainEdge);
      j2DX2 = (float) rangeAxis.valueToJava2D(this.y2, dataArea, rangeEdge);
    }
    g2.setPaint(this.paint);
    g2.setStroke(this.stroke);
    Line2D line = new Line2D.Float(j2DX1, j2DY1, j2DX2, j2DY2);
    // line is clipped to avoid JRE bug 6574155, for more info
    // see JFreeChart bug 2221495
    boolean visible = LineUtils.clipLine(line, dataArea);
    if (visible) {
      g2.draw(line);
    }

    String toolTip = getToolTipText();
    String url = getURL();
//    if (toolTip != null || url != null) {
//      addEntity(info, ShapeUtils.createLineRegion(line, 1.0f), rendererIndex, toolTip, url);
//    }

  }
}
