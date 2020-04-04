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

package io.github.mzmine.modules.visualization.neutralloss;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

class NeutralLossPlot extends EChartViewer {

  private JFreeChart chart;

  private XYPlot plot;
  private NeutralLossDataPointRenderer defaultRenderer;

  private boolean showSpectrumRequest;

  private NeutralLossVisualizerWindow visualizer;

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

  NeutralLossPlot() {
    super(ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true,
        false), true, true, false, false, true);
    resetZoomHistory();
    setMouseZoomable(false);

    showSpectrumRequest = false;

    // set the renderer properties
    defaultRenderer = new NeutralLossDataPointRenderer(false, true);
    defaultRenderer.setTransparency(0.4f);
    setSeriesColorRenderer(0, pointColor, dataPointsShape);
    setSeriesColorRenderer(1, searchPrecursorColor, dataPointsShape2);
    setSeriesColorRenderer(2, searchNeutralLossColor, dataPointsShape2);

    // chart properties
    chart = getChart();
    chart.setBackgroundPaint(Color.white);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

    // title
    chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    plot.addRangeMarker(new ValueMarker(0));

    final ContextMenu popupMenu = getContextMenu();
    popupMenu.getItems().add(new SeparatorMenuItem());

    this.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        requestFocus();
        MouseEvent mouseEvent = event.getTrigger();
        if ((mouseEvent.getClickCount() == 2) && (mouseEvent.getButton() == MouseButton.PRIMARY)) {
          // showSpectrum();
          showSpectrumRequest = true;
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        return;
      }
    });


    this.setOnKeyTyped(keyEvent -> {
      if (keyEvent.getCharacter().equals(" ")) {
        showSpectrum();
      }
    });

    chart.addProgressListener(event -> {
      if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
        visualizer.updateTitle();
        if (showSpectrumRequest) {
          showSpectrumRequest = false;
          showSpectrum();
        }
      }
    });

    resetZoomHistory();
  }

  public void showSpectrum() {
    NeutralLossDataSet dataset = (NeutralLossDataSet) plot.getDataset();
    double xValue = plot.getDomainCrosshairValue();
    double yValue = plot.getRangeCrosshairValue();
    NeutralLossDataPoint pos = dataset.getDataPoint(xValue, yValue);
    RawDataFile dataFile = visualizer.getDataFile();
    if (pos != null) {
      SpectraVisualizerModule.showNewSpectrumWindow(dataFile, pos.getScanNumber());
    }

    resetZoomHistory();
  }

  public void resetZoomHistory() {
    ZoomHistory history = getZoomHistory();
    if (history != null) {
      history.clear();
    }
  }

  public void setAxisTypes(Object xAxisType) {

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    // set the X axis (retention time) properties
    final NumberAxis xAxis = (NumberAxis) this.plot.getDomainAxis();
    if (xAxisType.equals(NeutralLossParameters.xAxisPrecursor)) {
      xAxis.setLabel("Precursor m/z");
      xAxis.setNumberFormatOverride(mzFormat);
    } else {
      xAxis.setLabel("Retention time");
      xAxis.setNumberFormatOverride(rtFormat);
    }
    xAxis.setUpperMargin(0);
    xAxis.setLowerMargin(0);
    xAxis.setAutoRangeIncludesZero(false);
    // set the Y axis (intensity) properties
    final NumberAxis yAxis = (NumberAxis) this.plot.getRangeAxis();
    yAxis.setLabel("Neutral loss (Da)");
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setNumberFormatOverride(mzFormat);
    yAxis.setUpperMargin(0);
    yAxis.setLowerMargin(0);
  }

  public void addNeutralLossDataSet(NeutralLossDataSet dataset) {
    plot.setDataset(dataset);
    defaultRenderer.setDefaultToolTipGenerator(dataset);
    plot.setRenderer(defaultRenderer);
  }

  public void setVisualizer(NeutralLossVisualizerWindow visualizer) {
    this.visualizer = visualizer;
  }

  public void setMenuItems() {
    final ContextMenu popupMenu = getContextMenu();

    MenuItem highlightPrecursorMenuItem = new MenuItem("Highlight precursor m/z range...");
    highlightPrecursorMenuItem.setOnAction(event -> {
      NeutralLossSetHighlightDialog dialog =
          new NeutralLossSetHighlightDialog(visualizer, this, "HIGHLIGHT_PRECURSOR");
      dialog.show();
    });
    popupMenu.getItems().add(highlightPrecursorMenuItem);

    MenuItem highlightNLMenuItem = new MenuItem("Highlight neutral loss m/z range...");
    highlightNLMenuItem.setOnAction(event -> {
      NeutralLossSetHighlightDialog dialog =
          new NeutralLossSetHighlightDialog(visualizer, this, "HIGHLIGHT_NEUTRALLOSS");
      dialog.show();
    });
    popupMenu.getItems().add(highlightNLMenuItem);
    popupMenu.getItems().add(new SeparatorMenuItem());
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

  XYPlot getXYPlot() {
    return plot;
  }

}
