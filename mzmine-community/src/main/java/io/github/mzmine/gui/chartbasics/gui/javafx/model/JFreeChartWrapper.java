/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.TitleChangeEvent;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.RectangleInsets;

public class JFreeChartWrapper extends JFreeChart {


  protected final JFreeChart chart;
  private static final MethodHandle drawTitleHandle;
  private static final MethodHandle notifyProgressListenersHandle;
  private static final MethodHandle notifyChangeListenersHandle;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      drawTitleHandle = lookup.findVirtual(JFreeChart.class, "drawTitle",
          MethodType.methodType(EntityCollection.class, Title.class, Graphics2D.class,
              Rectangle2D.class, boolean.class));
      notifyProgressListenersHandle = lookup.findVirtual(JFreeChart.class, "notifyListeners",
          MethodType.methodType(void.class, ChartProgressEvent.class));
      notifyChangeListenersHandle = lookup.findVirtual(JFreeChart.class, "notifyListeners",
          MethodType.methodType(void.class, ChartChangeEvent.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public JFreeChartWrapper(JFreeChart chart, @Nullable FxXYPlotWrapper wrappedPlot) {
    super(chart.getTitle().getText(), chart.getTitle().getFont(),
        wrappedPlot == null ? chart.getPlot() : wrappedPlot, chart.getLegend() != null);
    this.chart = chart;
  }

  @Override
  public String getID() {
    return chart != null ? chart.getID() : null;
  }

  @Override
  public void setID(String id) {

    if (chart != null) {
      chart.setID(id);
    }
  }

  @Override
  public boolean getElementHinting() {
    return chart != null ? chart.getElementHinting() : false;
  }

  @Override
  public void setElementHinting(boolean hinting) {
    if (chart != null) {
      chart.setElementHinting(hinting);
    }
  }

  @Override
  public RenderingHints getRenderingHints() {
    return chart != null ? chart.getRenderingHints() : null;
  }

  @Override
  public void setRenderingHints(RenderingHints renderingHints) {
    if (chart != null) {
      chart.setRenderingHints(renderingHints);
    }
  }

  @Override
  public boolean isBorderVisible() {
    return chart != null && chart.isBorderVisible();
  }

  @Override
  public void setBorderVisible(boolean visible) {
    if (chart != null) {
      chart.setBorderVisible(visible);
    }
  }

  @Override
  public Stroke getBorderStroke() {
    return chart != null ? chart.getBorderStroke() : null;
  }

  @Override
  public void setBorderStroke(Stroke stroke) {
    if (chart != null) {
      chart.setBorderStroke(stroke);
    }
  }

  @Override
  public Paint getBorderPaint() {
    return chart != null ? chart.getBorderPaint() : null;
  }

  @Override
  public void setBorderPaint(Paint paint) {
    if (chart != null) {
      chart.setBorderPaint(paint);
    }
  }

  @Override
  public RectangleInsets getPadding() {
    return chart != null ? chart.getPadding() : null;
  }

  @Override
  public void setPadding(RectangleInsets padding) {
    if (chart != null) {
      chart.setPadding(padding);
    }
  }

  @Override
  public TextTitle getTitle() {
    return chart != null ? chart.getTitle() : null;
  }

  @Override
  public void setTitle(TextTitle title) {
    if (chart != null) {
      chart.setTitle(title);
    }
  }

  @Override
  public void setTitle(String text) {
    if (chart != null) {
      chart.setTitle(text);
    }
  }

  @Override
  public void addLegend(LegendTitle legend) {
    if (chart != null) {
      chart.addLegend(legend);
    }
  }

  @Override
  public LegendTitle getLegend() {
    return chart != null ? chart.getLegend() : null;
  }

  @Override
  public LegendTitle getLegend(int index) {
    return chart != null ? chart.getLegend(index) : null;
  }

  @Override
  public void removeLegend() {
    if (chart != null) {
      chart.removeLegend();
    }
  }

  @Override
  public List getSubtitles() {
    return chart != null ? chart.getSubtitles() : null;
  }

  @Override
  public void setSubtitles(List subtitles) {
    if (chart != null) {
      chart.setSubtitles(subtitles);
    }
  }

  @Override
  public int getSubtitleCount() {
    return chart != null ? chart.getSubtitleCount() : 0;
  }

  @Override
  public Title getSubtitle(int index) {
    return chart != null ? chart.getSubtitle(index) : null;
  }

  @Override
  public void addSubtitle(Title subtitle) {
    if (chart != null) {
      chart.addSubtitle(subtitle);
    }
  }

  @Override
  public void addSubtitle(int index, Title subtitle) {
    if (chart != null) {
      chart.addSubtitle(index, subtitle);
    }
  }

  @Override
  public void clearSubtitles() {
    if (chart != null) {
      chart.clearSubtitles();
    }
  }

  @Override
  public void removeSubtitle(Title title) {
    if (chart != null) {
      chart.removeSubtitle(title);
    }
  }

  @Override
  public Plot getPlot() {
    return chart != null ? chart.getPlot() : null;
  }

  @Override
  public CategoryPlot getCategoryPlot() {
    return chart != null ? chart.getCategoryPlot() : null;
  }

  @Override
  public XYPlot getXYPlot() {
    return chart != null ? chart.getXYPlot() : null;
  }

  @Override
  public boolean getAntiAlias() {
    return chart != null && chart.getAntiAlias();
  }

  @Override
  public void setAntiAlias(boolean flag) {
    if (chart != null) {
      chart.setAntiAlias(flag);
    }
  }

  @Override
  public Object getTextAntiAlias() {
    return chart != null ? chart.getTextAntiAlias() : null;
  }

  @Override
  public void setTextAntiAlias(boolean flag) {
    if (chart != null) {
      chart.setTextAntiAlias(flag);
    }
  }

  @Override
  public void setTextAntiAlias(Object val) {
    if (chart != null) {
      chart.setTextAntiAlias(val);
    }
  }

  @Override
  public Paint getBackgroundPaint() {
    return chart != null ? chart.getBackgroundPaint() : null;
  }

  @Override
  public void setBackgroundPaint(Paint paint) {
    if (chart != null) {
      chart.setBackgroundPaint(paint);
    }
  }

  @Override
  public Image getBackgroundImage() {
    return chart != null ? chart.getBackgroundImage() : null;
  }

  @Override
  public void setBackgroundImage(Image image) {
    if (chart != null) {
      chart.setBackgroundImage(image);
    }
  }

  @Override
  public int getBackgroundImageAlignment() {
    return chart != null ? chart.getBackgroundImageAlignment() : 0;
  }

  @Override
  public void setBackgroundImageAlignment(int alignment) {
    if (chart != null) {
      chart.setBackgroundImageAlignment(alignment);
    }
  }

  @Override
  public float getBackgroundImageAlpha() {
    return chart != null ? chart.getBackgroundImageAlpha() : 0f;
  }

  @Override
  public void setBackgroundImageAlpha(float alpha) {
    if (chart != null) {
      chart.setBackgroundImageAlpha(alpha);
    }
  }

  @Override
  public boolean isNotify() {
    return chart != null && chart.isNotify();
  }

  @Override
  public void setNotify(boolean notify) {
    if (chart != null) {
      chart.setNotify(notify);
    }
  }

  @Override
  public void draw(Graphics2D g2, Rectangle2D area) {
    if (chart != null) {
      chart.draw(g2, area);
    }
  }

  @Override
  public void draw(Graphics2D g2, Rectangle2D area, ChartRenderingInfo info) {
    if (chart != null) {
      chart.draw(g2, area, info);
    }
  }

  @Override
  public void draw(Graphics2D g2, Rectangle2D chartArea, Point2D anchor, ChartRenderingInfo info) {
    if (chart != null) {
      chart.draw(g2, chartArea, anchor, info);
    }
  }

  @Override
  public EntityCollection drawTitle(Title t, Graphics2D g2, Rectangle2D area, boolean entities) {

    if (chart != null) {
      try {
        return (EntityCollection) drawTitleHandle.invoke(chart, t, g2, area, entities);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Override
  public BufferedImage createBufferedImage(int width, int height) {
    return chart != null ? chart.createBufferedImage(width, height) : null;
  }

  @Override
  public BufferedImage createBufferedImage(int width, int height, ChartRenderingInfo info) {
    return chart != null ? chart.createBufferedImage(width, height, info) : null;
  }

  @Override
  public BufferedImage createBufferedImage(int width, int height, int imageType,
      ChartRenderingInfo info) {
    return chart != null ? chart.createBufferedImage(width, height, imageType, info) : null;
  }

  @Override
  public BufferedImage createBufferedImage(int imageWidth, int imageHeight, double drawWidth,
      double drawHeight, ChartRenderingInfo info) {
    return chart != null ? chart.createBufferedImage(imageWidth, imageHeight, drawWidth, drawHeight,
        info) : null;
  }

  @Override
  public void handleClick(int x, int y, ChartRenderingInfo info) {
    if (chart != null) {
      chart.handleClick(x, y, info);
    }
  }

  @Override
  public void addChangeListener(ChartChangeListener listener) {
    if (chart != null) {
      chart.addChangeListener(listener);
    }
  }

  @Override
  public void removeChangeListener(ChartChangeListener listener) {
    if (chart != null) {
      chart.removeChangeListener(listener);
    }
  }

  @Override
  public void fireChartChanged() {
    if (chart != null) {
      chart.fireChartChanged();
    }
  }

  @Override
  public void notifyListeners(ChartChangeEvent event) {
    if (chart != null) {
      try {
        notifyChangeListenersHandle.invoke(chart, event);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void addProgressListener(ChartProgressListener listener) {
    if (chart != null) {
      chart.addProgressListener(listener);
    }
  }

  @Override
  public void removeProgressListener(ChartProgressListener listener) {
    if (chart != null) {
      chart.removeProgressListener(listener);
    }
  }

  @Override
  public void notifyListeners(ChartProgressEvent event) {
    if (chart != null) {

      try {
        notifyProgressListenersHandle.invoke(chart, event);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void titleChanged(TitleChangeEvent event) {
    if (chart != null) {
      chart.titleChanged(event);
    }
  }

  @Override
  public void plotChanged(PlotChangeEvent event) {
    if (chart != null) {
      chart.plotChanged(event);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return chart != null && chart.equals(obj);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return chart != null ? chart.clone() : null;
  }
}
