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

package io.github.mzmine.gui.chartbasics.gui.javafx;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.value.ObservableValue;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AnnotationChangeEvent;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.MarkerChangeEvent;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.ShadowGenerator;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYDataset;

public class XYPlotWrapper extends XYPlot {

  protected final XYPlot plot;

  public XYPlotWrapper(XYPlot plot) {
    this.plot = plot;
  }

  @Override
  public String getPlotType() {
    if (plot == null) {
      return null;
    }
    return plot.getPlotType();
  }

  @Override
  public PlotOrientation getOrientation() {
    if (plot == null) {
      return null;
    }
    return plot.getOrientation();
  }

  @Override
  public void setOrientation(PlotOrientation orientation) {
    if (plot == null) {
      return;
    }
    plot.setOrientation(orientation);
  }

  @Override
  public RectangleInsets getAxisOffset() {
    if (plot == null) {
      return null;
    }
    return plot.getAxisOffset();
  }

  @Override
  public void setAxisOffset(RectangleInsets offset) {
    if (plot == null) {
      return;
    }
    plot.setAxisOffset(offset);
  }

  @Override
  public ValueAxis getDomainAxis(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getDomainAxis(index);
  }

  @Override
  public void setDomainAxis(int index, ValueAxis axis) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxis(index, axis);
  }

  @Override
  public void setDomainAxis(int index, ValueAxis axis, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxis(index, axis, notify);
  }

  @Override
  public void setDomainAxes(ValueAxis[] axes) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxes(axes);
  }

  @Override
  public AxisLocation getDomainAxisLocation() {
    if (plot == null) {
      return null;
    }

    return plot.getDomainAxisLocation();
  }

  @Override
  public void setDomainAxisLocation(AxisLocation location) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxisLocation(location);
  }

  @Override
  public void setDomainAxisLocation(AxisLocation location, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxisLocation(location, notify);
  }

  @Override
  public RectangleEdge getDomainAxisEdge() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainAxisEdge();
  }

  @Override
  public int getDomainAxisCount() {
    if (plot == null) {
      return 0;
    }
    return plot.getDomainAxisCount();
  }

  @Override
  public void clearDomainAxes() {
    if (plot == null) {
      return;
    }
    plot.clearDomainAxes();
  }

  @Override
  public void configureDomainAxes() {
    if (plot == null) {
      return;
    }
    plot.configureDomainAxes();
  }

  @Override
  public AxisLocation getDomainAxisLocation(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getDomainAxisLocation(index);
  }

  @Override
  public void setDomainAxisLocation(int index, AxisLocation location) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxisLocation(index, location);
  }

  @Override
  public void setDomainAxisLocation(int index, AxisLocation location, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxisLocation(index, location, notify);
  }

  @Override
  public RectangleEdge getDomainAxisEdge(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getDomainAxisEdge(index);
  }

  @Override
  public AxisLocation getRangeAxisLocation() {

    if (plot == null) {
      return null;
    }
    return plot.getRangeAxisLocation();
  }

  @Override
  public void setRangeAxisLocation(AxisLocation location) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxisLocation(location);
  }

  @Override
  public void setRangeAxisLocation(AxisLocation location, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxisLocation(location, notify);
  }

  @Override
  public RectangleEdge getRangeAxisEdge() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeAxisEdge();
  }

  @Override
  public ValueAxis getRangeAxis(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getRangeAxis(index);
  }

  @Override
  public void setRangeAxis(int index, ValueAxis axis) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxis(index, axis);
  }

  @Override
  public void setRangeAxis(int index, ValueAxis axis, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxis(index, axis, notify);
  }

  @Override
  public void setRangeAxes(ValueAxis[] axes) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxes(axes);
  }

  @Override
  public int getRangeAxisCount() {
    if (plot == null) {
      return 0;
    }
    return plot.getRangeAxisCount();
  }

  @Override
  public void clearRangeAxes() {
    if (plot == null) {
      return;
    }
    plot.clearRangeAxes();
  }

  @Override
  public void configureRangeAxes() {
    if (plot == null) {
      return;
    }
    plot.configureRangeAxes();
  }

  @Override
  public AxisLocation getRangeAxisLocation(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getRangeAxisLocation(index);
  }

  @Override
  public void setRangeAxisLocation(int index, AxisLocation location) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxisLocation(index, location);
  }

  @Override
  public void setRangeAxisLocation(int index, AxisLocation location, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxisLocation(index, location, notify);
  }

  @Override
  public RectangleEdge getRangeAxisEdge(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getRangeAxisEdge(index);
  }

  @Override
  public int getDatasetCount() {
    if (plot == null) {
      return 0;
    }
    return plot.getDatasetCount();
  }

  @Override
  public int indexOf(XYDataset dataset) {
    if (plot == null) {
      return -1;
    }
    return plot.indexOf(dataset);
  }

  @Override
  public void mapDatasetToDomainAxis(int index, int axisIndex) {
    if (plot == null) {
      return;
    }
    plot.mapDatasetToDomainAxis(index, axisIndex);
  }

  @Override
  public void mapDatasetToDomainAxes(int index, List axisIndices) {
    if (plot == null) {
      return;
    }
    plot.mapDatasetToDomainAxes(index, axisIndices);
  }

  @Override
  public void mapDatasetToRangeAxis(int index, int axisIndex) {
    if (plot == null) {
      return;
    }
    plot.mapDatasetToRangeAxis(index, axisIndex);
  }

  @Override
  public void mapDatasetToRangeAxes(int index, List axisIndices) {
    if (plot == null) {
      return;
    }
    plot.mapDatasetToRangeAxes(index, axisIndices);
  }

  @Override
  public int getRendererCount() {
    if (plot == null) {
      return 0;
    }
    return plot.getRendererCount();
  }

  @Override
  public void setRenderer(int index, XYItemRenderer renderer, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setRenderer(index, renderer, notify);
  }

  @Override
  public void setRenderers(XYItemRenderer[] renderers) {
    if (plot == null) {
      return;
    }
    plot.setRenderers(renderers);
  }

  @Override
  public DatasetRenderingOrder getDatasetRenderingOrder() {
    if (plot == null) {
      return null;
    }
    return plot.getDatasetRenderingOrder();
  }

  @Override
  public void setDatasetRenderingOrder(DatasetRenderingOrder order) {
    if (plot == null) {
      return;
    }
    plot.setDatasetRenderingOrder(order);
  }

  @Override
  public SeriesRenderingOrder getSeriesRenderingOrder() {
    if (plot == null) {
      return null;
    }
    return plot.getSeriesRenderingOrder();
  }

  @Override
  public void setSeriesRenderingOrder(SeriesRenderingOrder order) {
    if (plot == null) {
      return;
    }
    plot.setSeriesRenderingOrder(order);
  }

  @Override
  public int getIndexOf(XYItemRenderer renderer) {
    if (plot == null) {
      return -1;
    }
    return plot.getIndexOf(renderer);
  }

  @Override
  public XYItemRenderer getRendererForDataset(XYDataset dataset) {
    if (plot == null) {
      return null;
    }
    return plot.getRendererForDataset(dataset);
  }

  @Override
  public int getWeight() {
    if (plot == null) {
      return 0;
    }
    return plot.getWeight();
  }

  @Override
  public void setWeight(int weight) {
    if (plot == null) {
      return;
    }
    plot.setWeight(weight);
  }

  @Override
  public boolean isDomainGridlinesVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainGridlinesVisible();
  }

  @Override
  public void setDomainGridlinesVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setDomainGridlinesVisible(visible);
  }

  @Override
  public boolean isDomainMinorGridlinesVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainMinorGridlinesVisible();
  }

  @Override
  public void setDomainMinorGridlinesVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setDomainMinorGridlinesVisible(visible);
  }

  @Override
  public Stroke getDomainGridlineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainGridlineStroke();
  }

  @Override
  public void setDomainGridlineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setDomainGridlineStroke(stroke);
  }

  @Override
  public Stroke getDomainMinorGridlineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainMinorGridlineStroke();
  }

  @Override
  public void setDomainMinorGridlineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setDomainMinorGridlineStroke(stroke);
  }

  @Override
  public Paint getDomainGridlinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainGridlinePaint();
  }

  @Override
  public void setDomainGridlinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setDomainGridlinePaint(paint);
  }

  @Override
  public Paint getDomainMinorGridlinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainMinorGridlinePaint();
  }

  @Override
  public void setDomainMinorGridlinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setDomainMinorGridlinePaint(paint);
  }

  @Override
  public boolean isRangeGridlinesVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isRangeGridlinesVisible();
  }

  @Override
  public void setRangeGridlinesVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setRangeGridlinesVisible(visible);
  }

  @Override
  public Stroke getRangeGridlineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeGridlineStroke();
  }

  @Override
  public void setRangeGridlineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setRangeGridlineStroke(stroke);
  }

  @Override
  public Paint getRangeGridlinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeGridlinePaint();
  }

  @Override
  public void setRangeGridlinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setRangeGridlinePaint(paint);
  }

  @Override
  public boolean isRangeMinorGridlinesVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isRangeMinorGridlinesVisible();
  }

  @Override
  public void setRangeMinorGridlinesVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setRangeMinorGridlinesVisible(visible);
  }

  @Override
  public Stroke getRangeMinorGridlineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeMinorGridlineStroke();
  }

  @Override
  public void setRangeMinorGridlineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setRangeMinorGridlineStroke(stroke);
  }

  @Override
  public Paint getRangeMinorGridlinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeMinorGridlinePaint();
  }

  @Override
  public void setRangeMinorGridlinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setRangeMinorGridlinePaint(paint);
  }

  @Override
  public boolean isDomainZeroBaselineVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainZeroBaselineVisible();
  }

  @Override
  public void setDomainZeroBaselineVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setDomainZeroBaselineVisible(visible);
  }

  @Override
  public Stroke getDomainZeroBaselineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainZeroBaselineStroke();
  }

  @Override
  public void setDomainZeroBaselineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setDomainZeroBaselineStroke(stroke);
  }

  @Override
  public Paint getDomainZeroBaselinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainZeroBaselinePaint();
  }

  @Override
  public void setDomainZeroBaselinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setDomainZeroBaselinePaint(paint);
  }

  @Override
  public boolean isRangeZeroBaselineVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isRangeZeroBaselineVisible();
  }

  @Override
  public void setRangeZeroBaselineVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setRangeZeroBaselineVisible(visible);
  }

  @Override
  public Stroke getRangeZeroBaselineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeZeroBaselineStroke();
  }

  @Override
  public void setRangeZeroBaselineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setRangeZeroBaselineStroke(stroke);
  }

  @Override
  public Paint getRangeZeroBaselinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeZeroBaselinePaint();
  }

  @Override
  public void setRangeZeroBaselinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setRangeZeroBaselinePaint(paint);
  }

  @Override
  public Paint getDomainTickBandPaint() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainTickBandPaint();
  }

  @Override
  public void setDomainTickBandPaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setDomainTickBandPaint(paint);
  }

  @Override
  public Paint getRangeTickBandPaint() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeTickBandPaint();
  }

  @Override
  public void setRangeTickBandPaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setRangeTickBandPaint(paint);
  }

  @Override
  public Point2D getQuadrantOrigin() {
    if (plot == null) {
      return null;
    }
    return plot.getQuadrantOrigin();
  }

  @Override
  public void setQuadrantOrigin(Point2D origin) {
    if (plot == null) {
      return;
    }
    plot.setQuadrantOrigin(origin);
  }

  @Override
  public Paint getQuadrantPaint(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getQuadrantPaint(index);
  }

  @Override
  public void setQuadrantPaint(int index, Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setQuadrantPaint(index, paint);
  }

  @Override
  public void addDomainMarker(Marker marker) {
    if (plot == null) {
      return;
    }
    plot.addDomainMarker(marker);
  }

  @Override
  public void clearDomainMarkers(int index) {
    if (plot == null) {
      return;
    }
    plot.clearDomainMarkers(index);
  }

  @Override
  public void addDomainMarker(int index, Marker marker, Layer layer, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.addDomainMarker(index, marker, layer, notify);
  }

  @Override
  public boolean removeDomainMarker(Marker marker) {
    if (plot == null) {
      return false;
    }
    return plot.removeDomainMarker(marker);
  }

  @Override
  public boolean removeDomainMarker(int index, Marker marker, Layer layer) {

    if (plot == null) {
      return false;
    }
    return plot.removeDomainMarker(index, marker, layer);
  }

  @Override
  public boolean removeDomainMarker(int index, Marker marker, Layer layer, boolean notify) {
    if (plot == null) {
      return false;
    }
    return plot.removeDomainMarker(index, marker, layer, notify);
  }

  @Override
  public void addRangeMarker(Marker marker) {
    if (plot == null) {
      return;
    }
    plot.addRangeMarker(marker);
  }

  @Override
  public void addRangeMarker(int index, Marker marker, Layer layer, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.addRangeMarker(index, marker, layer, notify);
  }

  @Override
  public void clearRangeMarkers(int index) {
    if (plot == null) {
      return;
    }
    plot.clearRangeMarkers(index);
  }

  @Override
  public boolean removeRangeMarker(Marker marker) {
    if (plot == null) {
      return false;
    }
    return plot.removeRangeMarker(marker);
  }

  @Override
  public boolean removeRangeMarker(int index, Marker marker, Layer layer) {
    if (plot == null) {
      return false;
    }
    return plot.removeRangeMarker(index, marker, layer);
  }

  @Override
  public boolean removeRangeMarker(int index, Marker marker, Layer layer, boolean notify) {
    if (plot == null) {
      return false;
    }
    return plot.removeRangeMarker(index, marker, layer, notify);
  }

  @Override
  public void addAnnotation(XYAnnotation annotation) {
    if (plot == null) {
      return;
    }
    plot.addAnnotation(annotation);
  }

  @Override
  public void addAnnotation(XYAnnotation annotation, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.addAnnotation(annotation, notify);
  }

  @Override
  public boolean removeAnnotation(XYAnnotation annotation) {
    if (plot == null) {
      return false;
    }
    return plot.removeAnnotation(annotation);
  }

  @Override
  public boolean removeAnnotation(XYAnnotation annotation, boolean notify) {
    if (plot == null) {
      return false;
    }
    return plot.removeAnnotation(annotation, notify);
  }

  @Override
  public List getAnnotations() {
    if (plot == null) {
      return null;
    }
    return plot.getAnnotations();
  }

  @Override
  public void clearAnnotations() {
    if (plot == null) {
      return;
    }
    plot.clearAnnotations();
  }

  @Override
  public ShadowGenerator getShadowGenerator() {
    if (plot == null) {
      return null;
    }
    return plot.getShadowGenerator();
  }

  @Override
  public void setShadowGenerator(ShadowGenerator generator) {
    if (plot == null) {
      return;
    }
    plot.setShadowGenerator(generator);
  }

  @Override
  public JFreeChart getChart() {
    if (plot == null) {
      return null;
    }
    return plot.getChart();
  }

  @Override
  public void setChart(JFreeChart chart) {
    if (plot == null) {
      return;
    }
    plot.setChart(chart);
  }

  @Override
  public boolean fetchElementHintingFlag() {
    if (plot == null) {
      return false;
    }
    return plot.fetchElementHintingFlag();
  }

  @Override
  public DatasetGroup getDatasetGroup() {
    if (plot == null) {
      return null;
    }
    return plot.getDatasetGroup();
  }

  @Override
  public String getNoDataMessage() {
    if (plot == null) {
      return null;
    }
    return plot.getNoDataMessage();
  }

  @Override
  public void setNoDataMessage(String message) {
    if (plot == null) {
      return;
    }
    plot.setNoDataMessage(message);
  }

  @Override
  public Font getNoDataMessageFont() {
    if (plot == null) {
      return null;
    }
    return plot.getNoDataMessageFont();
  }

  @Override
  public void setNoDataMessageFont(Font font) {
    if (plot == null) {
      return;
    }
    plot.setNoDataMessageFont(font);
  }

  @Override
  public Paint getNoDataMessagePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getNoDataMessagePaint();
  }

  @Override
  public void setNoDataMessagePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setNoDataMessagePaint(paint);
  }

  @Override
  public Plot getParent() {
    if (plot == null) {
      return null;
    }
    return plot.getParent();
  }

  @Override
  public void setParent(Plot parent) {
    if (plot == null) {
      return;
    }
    plot.setParent(parent);
  }

  @Override
  public Plot getRootPlot() {
    if (plot == null) {
      return null;
    }
    return plot.getRootPlot();
  }

  @Override
  public boolean isSubplot() {
    if (plot == null) {
      return false;
    }
    return plot.isSubplot();
  }

  @Override
  public RectangleInsets getInsets() {
    if (plot == null) {
      return null;
    }
    return plot.getInsets();
  }

  @Override
  public void setInsets(RectangleInsets insets) {
    if (plot == null) {
      return;
    }
    plot.setInsets(insets);
  }

  @Override
  public void setInsets(RectangleInsets insets, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setInsets(insets, notify);
  }

  @Override
  public Paint getBackgroundPaint() {
    if (plot == null) {
      return null;
    }
    return plot.getBackgroundPaint();
  }

  @Override
  public void setBackgroundPaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setBackgroundPaint(paint);
  }

  @Override
  public float getBackgroundAlpha() {
    if (plot == null) {
      return 0.0f;
    }
    return plot.getBackgroundAlpha();
  }

  @Override
  public void setBackgroundAlpha(float alpha) {
    if (plot == null) {
      return;
    }
    plot.setBackgroundAlpha(alpha);
  }

  @Override
  public DrawingSupplier getDrawingSupplier() {
    if (plot == null) {
      return null;
    }
    return plot.getDrawingSupplier();
  }

  @Override
  public void setDrawingSupplier(DrawingSupplier supplier) {
    if (plot == null) {
      return;
    }
    plot.setDrawingSupplier(supplier);
  }

  @Override
  public void setDrawingSupplier(DrawingSupplier supplier, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setDrawingSupplier(supplier, notify);
  }

  @Override
  public Image getBackgroundImage() {
    if (plot == null) {
      return null;
    }
    return plot.getBackgroundImage();
  }

  @Override
  public void setBackgroundImage(Image image) {
    if (plot == null) {
      return;
    }
    plot.setBackgroundImage(image);
  }

  @Override
  public int getBackgroundImageAlignment() {
    if (plot == null) {
      return 0;
    }
    return plot.getBackgroundImageAlignment();
  }

  @Override
  public void setBackgroundImageAlignment(int alignment) {
    if (plot == null) {
      return;
    }
    plot.setBackgroundImageAlignment(alignment);
  }

  @Override
  public float getBackgroundImageAlpha() {
    if (plot == null) {
      return 0.0f;
    }
    return plot.getBackgroundImageAlpha();
  }

  @Override
  public void setBackgroundImageAlpha(float alpha) {
    if (plot == null) {
      return;
    }
    plot.setBackgroundImageAlpha(alpha);
  }

  @Override
  public boolean isOutlineVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isOutlineVisible();
  }

  @Override
  public void setOutlineVisible(boolean visible) {
    if (plot == null) {
      return;
    }
    plot.setOutlineVisible(visible);
  }

  @Override
  public Stroke getOutlineStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getOutlineStroke();
  }

  @Override
  public void setOutlineStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setOutlineStroke(stroke);
  }

  @Override
  public Paint getOutlinePaint() {
    if (plot == null) {
      return null;
    }
    return plot.getOutlinePaint();
  }

  @Override
  public void setOutlinePaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setOutlinePaint(paint);
  }

  @Override
  public float getForegroundAlpha() {
    if (plot == null) {
      return 0.0f;
    }
    return plot.getForegroundAlpha();
  }

  @Override
  public void setForegroundAlpha(float alpha) {
    if (plot == null) {
      return;
    }
    plot.setForegroundAlpha(alpha);
  }

  @Override
  public boolean isNotify() {
    if (plot == null) {
      return false;
    }
    return plot.isNotify();
  }

  @Override
  public void setNotify(boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setNotify(notify);
  }

  @Override
  public void addChangeListener(PlotChangeListener listener) {
    if (plot == null) {
      return;
    }
    plot.addChangeListener(listener);
  }

  @Override
  public void removeChangeListener(PlotChangeListener listener) {
    if (plot == null) {
      return;
    }
    plot.removeChangeListener(listener);
  }

  @Override
  public void notifyListeners(PlotChangeEvent event) {
    if (plot == null) {
      return;
    }
    plot.notifyListeners(event);
  }

  @Override
  public void drawBackgroundImage(Graphics2D g2, Rectangle2D area) {
    if (plot == null) {
      return;
    }
    plot.drawBackgroundImage(g2, area);
  }

  @Override
  public void drawOutline(Graphics2D g2, Rectangle2D area) {
    if (plot == null) {
      return;
    }
    plot.drawOutline(g2, area);
  }

  @Override
  public void zoom(double percent) {
    if (plot == null) {
      return;
    }
    plot.zoom(percent);
  }

  @Override
  public void axisChanged(AxisChangeEvent event) {
    if (plot == null) {
      return;
    }
    plot.axisChanged(event);
  }

  @Override
  public void markerChanged(MarkerChangeEvent event) {
    if (plot == null) {
      return;
    }
    plot.markerChanged(event);
  }

  @Override
  public void drawBackground(Graphics2D g2, Rectangle2D area) {
    if (plot == null) {
      return;
    }
    plot.drawBackground(g2, area);
  }

  @Override
  public void drawDomainTickBands(Graphics2D g2, Rectangle2D dataArea, List ticks) {
    if (plot == null) {
      return;
    }
    plot.drawDomainTickBands(g2, dataArea, ticks);
  }

  @Override
  public void drawRangeTickBands(Graphics2D g2, Rectangle2D dataArea, List ticks) {
    if (plot == null) {
      return;
    }
    plot.drawRangeTickBands(g2, dataArea, ticks);
  }

  @Override
  public boolean render(Graphics2D g2, Rectangle2D dataArea, int index, PlotRenderingInfo info,
      CrosshairState crosshairState) {

    if (plot == null) {
      return false;
    }
    return plot.render(g2, dataArea, index, info, crosshairState);
  }

  @Override
  public ValueAxis getDomainAxisForDataset(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getDomainAxisForDataset(index);
  }

  @Override
  public ValueAxis getRangeAxisForDataset(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getRangeAxisForDataset(index);
  }

  @Override
  public Collection getDomainMarkers(Layer layer) {
    if (plot == null) {
      return null;
    }
    return plot.getDomainMarkers(layer);
  }

  @Override
  public Collection getRangeMarkers(Layer layer) {
    if (plot == null) {
      return null;
    }
    return plot.getRangeMarkers(layer);
  }

  @Override
  public void handleClick(int x, int y, PlotRenderingInfo info) {
    if (plot == null) {
      return;
    }
    plot.handleClick(x, y, info);
  }

  @Override
  public int getDomainAxisIndex(ValueAxis axis) {
    if (plot == null) {
      return -1;
    }
    return plot.getDomainAxisIndex(axis);
  }

  @Override
  public int getRangeAxisIndex(ValueAxis axis) {
    if (plot == null) {
      return -1;
    }
    return plot.getRangeAxisIndex(axis);
  }

  @Override
  public Range getDataRange(ValueAxis axis) {
    if (plot == null) {
      return null;
    }
    return plot.getDataRange(axis);
  }

  @Override
  public void annotationChanged(AnnotationChangeEvent event) {
    if (plot == null) {
      return;
    }
    plot.annotationChanged(event);
  }

  @Override
  public void rendererChanged(RendererChangeEvent event) {
    if (plot == null) {
      return;
    }
    plot.rendererChanged(event);
  }

  @Override
  public boolean isDomainCrosshairVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainCrosshairVisible();
  }

  @Override
  public void setDomainCrosshairVisible(boolean flag) {
    if (plot == null) {
      return;
    }
    plot.setDomainCrosshairVisible(flag);
  }

  @Override
  public boolean isDomainCrosshairLockedOnData() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainCrosshairLockedOnData();
  }

  @Override
  public void setDomainCrosshairLockedOnData(boolean flag) {
    if (plot == null) {
      return;
    }
    plot.setDomainCrosshairLockedOnData(flag);
  }

  @Override
  public double getDomainCrosshairValue() {
    if (plot == null) {
      return -1;
    }
    return plot.getDomainCrosshairValue();
  }

  @Override
  public void setDomainCrosshairValue(double value) {
    if (plot == null) {
      return;
    }
    plot.setDomainCrosshairValue(value);
  }

  @Override
  public void setDomainCrosshairValue(double value, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setDomainCrosshairValue(value, notify);
  }

  @Override
  public Stroke getDomainCrosshairStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainCrosshairStroke();
  }

  @Override
  public void setDomainCrosshairStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setDomainCrosshairStroke(stroke);
  }

  @Override
  public Paint getDomainCrosshairPaint() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainCrosshairPaint();
  }

  @Override
  public void setDomainCrosshairPaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setDomainCrosshairPaint(paint);
  }

  @Override
  public boolean isRangeCrosshairVisible() {
    if (plot == null) {
      return false;
    }
    return plot.isRangeCrosshairVisible();
  }

  @Override
  public void setRangeCrosshairVisible(boolean flag) {
    if (plot == null) {
      return;
    }
    plot.setRangeCrosshairVisible(flag);
  }

  @Override
  public boolean isRangeCrosshairLockedOnData() {
    if (plot == null) {
      return false;
    }
    return plot.isRangeCrosshairLockedOnData();
  }

  @Override
  public void setRangeCrosshairLockedOnData(boolean flag) {
    if (plot == null) {
      return;
    }
    plot.setRangeCrosshairLockedOnData(flag);
  }

  @Override
  public double getRangeCrosshairValue() {
    if (plot == null) {
      return -1;
    }
    return plot.getRangeCrosshairValue();
  }

  @Override
  public void setRangeCrosshairValue(double value) {
    if (plot == null) {
      return;
    }
    plot.setRangeCrosshairValue(value);
  }

  @Override
  public void setRangeCrosshairValue(double value, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setRangeCrosshairValue(value, notify);
  }

  @Override
  public Stroke getRangeCrosshairStroke() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeCrosshairStroke();
  }

  @Override
  public void setRangeCrosshairStroke(Stroke stroke) {
    if (plot == null) {
      return;
    }
    plot.setRangeCrosshairStroke(stroke);
  }

  @Override
  public Paint getRangeCrosshairPaint() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeCrosshairPaint();
  }

  @Override
  public void setRangeCrosshairPaint(Paint paint) {
    if (plot == null) {
      return;
    }
    plot.setRangeCrosshairPaint(paint);
  }

  @Override
  public AxisSpace getFixedDomainAxisSpace() {
    if (plot == null) {
      return null;
    }
    return plot.getFixedDomainAxisSpace();
  }

  @Override
  public void setFixedDomainAxisSpace(AxisSpace space) {
    if (plot == null) {
      return;
    }
    plot.setFixedDomainAxisSpace(space);
  }

  @Override
  public void setFixedDomainAxisSpace(AxisSpace space, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setFixedDomainAxisSpace(space, notify);
  }

  @Override
  public AxisSpace getFixedRangeAxisSpace() {
    if (plot == null) {
      return null;
    }
    return plot.getFixedRangeAxisSpace();
  }

  @Override
  public void setFixedRangeAxisSpace(AxisSpace space) {
    if (plot == null) {
      return;
    }
    plot.setFixedRangeAxisSpace(space);
  }

  @Override
  public void setFixedRangeAxisSpace(AxisSpace space, boolean notify) {
    if (plot == null) {
      return;
    }
    plot.setFixedRangeAxisSpace(space, notify);
  }

  @Override
  public boolean isDomainPannable() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainPannable();
  }

  @Override
  public void setDomainPannable(boolean pannable) {
    if (plot == null) {
      return;
    }
    plot.setDomainPannable(pannable);
  }

  @Override
  public boolean isRangePannable() {
    if (plot == null) {
      return false;
    }
    return plot.isRangePannable();
  }

  @Override
  public void setRangePannable(boolean pannable) {
    if (plot == null) {
      return;
    }
    plot.setRangePannable(pannable);
  }

  @Override
  public void panDomainAxes(double percent, PlotRenderingInfo info, Point2D source) {
    if (plot == null) {
      return;
    }
    plot.panDomainAxes(percent, info, source);
  }

  @Override
  public void panRangeAxes(double percent, PlotRenderingInfo info, Point2D source) {
    if (plot == null) {
      return;
    }
    plot.panRangeAxes(percent, info, source);
  }

  @Override
  public void zoomDomainAxes(double factor, PlotRenderingInfo info, Point2D source) {
    if (plot == null) {
      return;
    }
    plot.zoomDomainAxes(factor, info, source);
  }

  @Override
  public void zoomDomainAxes(double factor, PlotRenderingInfo info, Point2D source,
      boolean useAnchor) {
    if (plot == null) {
      return;
    }
    plot.zoomDomainAxes(factor, info, source, useAnchor);
  }

  @Override
  public void zoomDomainAxes(double lowerPercent, double upperPercent, PlotRenderingInfo info,
      Point2D source) {
    if (plot == null) {
      return;
    }
    plot.zoomDomainAxes(lowerPercent, upperPercent, info, source);
  }

  @Override
  public void zoomRangeAxes(double factor, PlotRenderingInfo info, Point2D source) {
    if (plot == null) {
      return;
    }
    plot.zoomRangeAxes(factor, info, source);
  }

  @Override
  public void zoomRangeAxes(double factor, PlotRenderingInfo info, Point2D source,
      boolean useAnchor) {
    if (plot == null) {
      return;
    }
    plot.zoomRangeAxes(factor, info, source, useAnchor);
  }

  @Override
  public void zoomRangeAxes(double lowerPercent, double upperPercent, PlotRenderingInfo info,
      Point2D source) {
    if (plot == null) {
      return;
    }
    plot.zoomRangeAxes(lowerPercent, upperPercent, info, source);
  }

  @Override
  public boolean isDomainZoomable() {
    if (plot == null) {
      return false;
    }
    return plot.isDomainZoomable();
  }

  @Override
  public boolean isRangeZoomable() {
    if (plot == null) {
      return false;
    }
    return plot.isRangeZoomable();
  }

  @Override
  public int getSeriesCount() {
    if (plot == null) {
      return 0;
    }
    return plot.getSeriesCount();
  }

  @Override
  public LegendItemCollection getFixedLegendItems() {
    if (plot == null) {
      return null;
    }
    return plot.getFixedLegendItems();
  }

  @Override
  public void setFixedLegendItems(LegendItemCollection items) {
    if (plot == null) {
      return;
    }
    plot.setFixedLegendItems(items);
  }

  @Override
  public LegendItemCollection getLegendItems() {
    if (plot == null) {
      return null;
    }

    return plot.getLegendItems();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new XYPlotWrapper((XYPlot) plot.clone());
  }

  @Override
  public void addDomainMarker(int index, org.jfree.chart.plot.Marker marker, Layer layer) {
    if (plot == null) {
      return;
    }
    plot.addDomainMarker(index, marker, layer);
  }

  @Override
  public void addDomainMarker(org.jfree.chart.plot.Marker marker, Layer layer) {
    if (plot == null) {
      return;
    }
    plot.addDomainMarker(marker, layer);
  }

  @Override
  public void addRangeMarker(int index, org.jfree.chart.plot.Marker marker, Layer layer) {
    if (plot == null) {
      return;
    }
    plot.addRangeMarker(index, marker, layer);
  }

  @Override
  public void addRangeMarker(org.jfree.chart.plot.Marker marker, Layer layer) {
    if (plot == null) {
      return;
    }
    plot.addRangeMarker(marker, layer);
  }

  @Override
  public void clearDomainMarkers() {
    if (plot == null) {
      return;
    }
    plot.clearDomainMarkers();
  }

  @Override
  public void clearRangeMarkers() {
    if (plot == null) {
      return;
    }
    plot.clearRangeMarkers();
  }

  @Override
  public void datasetChanged(org.jfree.data.general.DatasetChangeEvent event) {
    if (plot == null) {
      return;
    }
    plot.datasetChanged(event);
  }

  @Override
  public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState,
      PlotRenderingInfo info) {
    if (plot == null) {
      return;
    }
    plot.draw(g2, area, anchor, parentState, info);
  }

  @Override
  public Collection getDomainMarkers(int index, Layer layer) {
    if (plot == null) {
      return List.of();
    }
    return plot.getDomainMarkers(index, layer);
  }

  @Override
  public Collection getRangeMarkers(int index, Layer layer) {
    if (plot == null) {
      return List.of();
    }
    return plot.getRangeMarkers(index, layer);
  }

  @Override
  public XYDataset getDataset() {
    if (plot == null) {
      return null;
    }
    return plot.getDataset();
  }

  @Override
  public XYDataset getDataset(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getDataset(index);
  }

  @Override
  public void setDataset(XYDataset dataset) {
    if (plot == null) {
      return;
    }
    plot.setDataset(dataset);
  }

  @Override
  public void setDataset(int index, XYDataset dataset) {
    if (plot == null) {
      return;
    }
    plot.setDataset(index, dataset);
  }

  @Override
  public boolean removeDomainMarker(Marker marker, Layer layer) {
    if (plot == null) {
      return false;
    }
    return plot.removeDomainMarker(marker, layer);
  }

  @Override
  public boolean removeRangeMarker(org.jfree.chart.plot.Marker marker, Layer layer) {
    if (plot == null) {
      return false;
    }
    return plot.removeRangeMarker(marker, layer);
  }

  @Override
  public XYItemRenderer getRenderer() {
    if (plot == null) {
      return null;
    }
    return plot.getRenderer();
  }

  @Override
  public XYItemRenderer getRenderer(int index) {
    if (plot == null) {
      return null;
    }
    return plot.getRenderer(index);
  }

  @Override
  public void setRenderer(XYItemRenderer renderer) {
    if (plot == null) {
      return;
    }
    plot.setRenderer(renderer);
  }

  @Override
  public void setRenderer(int index, XYItemRenderer renderer) {
    if (plot == null) {
      return;
    }
    plot.setRenderer(index, renderer);
  }

  @Override
  public ValueAxis getDomainAxis() {
    if (plot == null) {
      return null;
    }
    return plot.getDomainAxis();
  }

  @Override
  public void setDomainAxis(ValueAxis axis) {
    if (plot == null) {
      return;
    }
    plot.setDomainAxis(axis);
  }

  @Override
  public ValueAxis getRangeAxis() {
    if (plot == null) {
      return null;
    }
    return plot.getRangeAxis();
  }

  @Override
  public void setRangeAxis(ValueAxis axis) {
    if (plot == null) {
      return;
    }
    plot.setRangeAxis(axis);
  }

  /**
   * Will set the chart.notify to tempState, perform logic that changes the chart, and reset to the
   * old notify state. If the old notify was true, a chart change event is fired. The old notify
   * will be false if this call is one of many boxed calls within methods.
   *
   * @param tempState usually false to avoid updating of a chart at every change event
   * @param logic     the logic that updates the chart
   */
  public void applyWithNotifyChanges(boolean tempState, Runnable logic) {
    if (plot == null) {
      return;
    }
    // use chart notify to stop upper level updates from happening
    final JFreeChart chart = getChart();
    applyWithNotifyChanges(tempState, chart == null ? plot.isNotify() : chart.isNotify(), logic);
  }

  /**
   * Will set the chart.notify to tempState, perform logic that changes the chart, and reset to the
   * old notify state. If the old notify was true, a chart change event is fired. The old notify
   * will be false if this call is one of many boxed calls within methods.
   *
   * @param tempState     usually false to avoid updating of a chart at every change event
   * @param logic         the logic that updates the chart
   * @param afterRunState the new state after running logic. If true, the chart is updated.
   */
  public void applyWithNotifyChanges(boolean tempState, boolean afterRunState, Runnable logic) {
    if (plot == null) {
      return;
    }
    final JFreeChart chart = getChart();
    if (chart != null) {
      chart.setNotify(tempState);
    } else {
      plot.setNotify(tempState);
    }
    try {
      // perform changes that t
      logic.run();
    } finally {
      // reset to old state and run changes if true
      // setting to true will automatically trigger a draw event
      if (chart != null) {
        chart.setNotify(afterRunState);
      } else {
        plot.setNotify(afterRunState);
      }
    }
  }

  /**
   * Listen to property value change and apply logic with notify changes false
   *
   * @param newValueConsumer the logic that updates the chart
   */
  public <T> void applyNotifyLater(ObservableValue<T> property, Consumer<T> newValueConsumer) {
    if (plot == null) {
      return;
    }
    property.subscribe((_, nv) -> {
      applyWithNotifyChanges(false, () -> newValueConsumer.accept(nv));
    });
  }

}
