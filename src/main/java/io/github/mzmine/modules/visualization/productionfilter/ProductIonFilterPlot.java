/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.productionfilter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.GUIUtils;

class ProductIonFilterPlot extends ChartPanel {

  private static final long serialVersionUID = 1L;

  private JFreeChart chart;

  private XYPlot plot;
  private ProductIonFilterDataPointRenderer defaultRenderer;

  private boolean showSpectrumRequest = false;

  private ProductIonFilterVisualizerWindow visualizer;

  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5, 3}, 0);

  // title font
  private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

  // Item's shape, small circle
  private static final Shape dataPointsShape = new Ellipse2D.Double(-1, -1, 2, 2);
  private static final Shape dataPointsShape2 = new Ellipse2D.Double(-1, -1, 3, 3);

  // Series colors
  private static final Color pointColor = Color.blue;
  private static final Color searchPrecursorColor = Color.green;
  private static final Color searchNeutralLossColor = Color.orange;

  private TextTitle chartTitle;

  private Range<Double> highlightedPrecursorRange = Range.singleton(Double.NEGATIVE_INFINITY);
  private Range<Double> highlightedNeutralLossRange = Range.singleton(Double.NEGATIVE_INFINITY);

  ProductIonFilterPlot(ProductIonFilterVisualizerWindow visualizer, ProductIonFilterDataSet dataset,
      Object xAxisType) {

    super(null, true);

    this.visualizer = visualizer;

    setBackground(Color.white);
    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis;
    if (xAxisType.equals(ProductIonFilterParameters.xAxisPrecursor)) {
      xAxis = new NumberAxis("Precursor ion m/z");
      xAxis.setNumberFormatOverride(mzFormat);
    } else {
      xAxis = new NumberAxis("Retention time");
      xAxis.setNumberFormatOverride(rtFormat);
    }
    xAxis.setUpperMargin(0);
    xAxis.setLowerMargin(0);
    xAxis.setAutoRangeIncludesZero(false);

    // set the Y axis (intensity) properties
    NumberAxis yAxis = new NumberAxis("Product ion m/z");
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setNumberFormatOverride(mzFormat);
    yAxis.setUpperMargin(0);
    yAxis.setLowerMargin(0);

    // set the renderer properties
    defaultRenderer = new ProductIonFilterDataPointRenderer(false, true);
    defaultRenderer.setTransparency(0.4f);
    setSeriesColorRenderer(0, pointColor, dataPointsShape);
    setSeriesColorRenderer(1, searchPrecursorColor, dataPointsShape2);
    setSeriesColorRenderer(2, searchNeutralLossColor, dataPointsShape2);

    // tooltips
    defaultRenderer.setDefaultToolTipGenerator(dataset);

    // set the plot properties
    plot = new XYPlot(dataset, xAxis, yAxis, defaultRenderer);
    plot.setBackgroundPaint(Color.white);
    plot.setRenderer(defaultRenderer);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

    // chart properties
    chart = new JFreeChart("", titleFont, plot, false);
    chart.setBackgroundPaint(Color.white);

    setChart(chart);

    // title
    chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);

    // disable maximum size (we don't want scaling)
    setMaximumDrawWidth(Integer.MAX_VALUE);
    setMaximumDrawHeight(Integer.MAX_VALUE);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    plot.addRangeMarker(new ValueMarker(0));

    // set focusable state to receive key events
    setFocusable(true);

    // register key handlers
    GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("SPACE"), visualizer, "SHOW_SPECTRUM");

    // add items to popup menu
    JPopupMenu popupMenu = getPopupMenu();
    popupMenu.addSeparator();

    JMenuItem highLightPrecursorRange = new JMenuItem("Highlight precursor m/z range...");
    highLightPrecursorRange.addActionListener(visualizer);
    highLightPrecursorRange.setActionCommand("HIGHLIGHT_PRECURSOR");
    popupMenu.add(highLightPrecursorRange);

    JMenuItem highLightNeutralLossRange = new JMenuItem("Highlight neutral loss m/z range...");
    highLightNeutralLossRange.addActionListener(visualizer);
    highLightNeutralLossRange.setActionCommand("HIGHLIGHT_NEUTRALLOSS");
    popupMenu.add(highLightNeutralLossRange);

  }

  private void setSeriesColorRenderer(int series, Color color, Shape shape) {
    defaultRenderer.setSeriesPaint(series, color);
    defaultRenderer.setSeriesFillPaint(series, color);
    defaultRenderer.setSeriesShape(series, shape);
  }

  void setTitle(String title) {
    chartTitle.setText(title);
  }

  /**
   * @return Returns the highlightedPrecursorRange.
   */
  Range<Double> getHighlightedPrecursorRange() {
    return highlightedPrecursorRange;
  }

  /**
   * @param range The highlightedPrecursorRange to set.
   */
  void setHighlightedPrecursorRange(Range<Double> range) {
    this.highlightedPrecursorRange = range;
  }

  /**
   * @return Returns the highlightedNeutralLossRange.
   */
  Range<Double> getHighlightedNeutralLossRange() {
    return highlightedNeutralLossRange;
  }

  /**
   * @param range The highlightedNeutralLossRange to set.
   */
  void setHighlightedNeutralLossRange(Range<Double> range) {
    this.highlightedNeutralLossRange = range;
  }

  /**
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseClicked(MouseEvent event) {

    // let the parent handle the event (selection etc.)
    super.mouseClicked(event);

    // request focus to receive key events
    requestFocus();

    // if user double-clicked left button, place a request to open a
    // spectrum
    if ((event.getButton() == MouseEvent.BUTTON1) && (event.getClickCount() == 2)) {
      showSpectrumRequest = true;
    }

  }

  /**
   * @see org.jfree.chart.event.ChartProgressListener#chartProgress(org.jfree.chart.event.ChartProgressEvent)
   */
  @Override
  public void chartProgress(ChartProgressEvent event) {

    super.chartProgress(event);

    if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {

      visualizer.updateTitle();

      if (showSpectrumRequest) {
        showSpectrumRequest = false;
        visualizer.actionPerformed(
            new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, "SHOW_SPECTRUM"));
      }
    }

  }

  XYPlot getXYPlot() {
    return plot;
  }
}
