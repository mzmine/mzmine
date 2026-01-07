/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
