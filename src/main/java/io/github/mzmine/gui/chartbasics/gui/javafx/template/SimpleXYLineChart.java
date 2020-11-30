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

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import java.awt.Color;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;

public class SimpleXYLineChart<T extends PlotDatasetProvider> extends
    EChartViewer /*implements LabelColorMatch*/ {

  private static final double AXIS_MARGINS = 0.001;
  private static Logger logger = Logger.getLogger(SimpleXYLineChart.class.getName());
  protected final JFreeChart chart;
  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;

  private final ObjectProperty<PlotCursorPosition> cursorPositionProperty;

  protected EStandardChartTheme theme;
  //  private SimpleLabelGenerator<DatasetType> labelGenerator;
//  private SimpleToolTipGenerator<DatasetType> toolTipGenerator;
  protected ColoredXYRenderer defaultRenderer;

  private int nextDataSetNum;
  private int labelsVisible;

  public SimpleXYLineChart() {
    this("x", "y");
  }

  public SimpleXYLineChart(@NamedArg("xlabel") String xLabel, @NamedArg("ylabel") String yLabel) {
    this(null, xLabel, yLabel);
  }

  public SimpleXYLineChart(@NamedArg("title") String title, @NamedArg("xlabel") String xLabel,
      @NamedArg("ylabel") String yLabel) {
    this(title, xLabel, yLabel, PlotOrientation.HORIZONTAL, true, true);
  }

  public SimpleXYLineChart(@NamedArg("title") String title, @NamedArg("xlabel") String xLabel,
      @NamedArg("ylabel") String yLabel, @NamedArg("orientation") PlotOrientation orientation,
      @NamedArg("legend") boolean createLegend, @NamedArg("tooltips") boolean showTooltips) {
    super(ChartFactory.createXYLineChart(title, xLabel, yLabel, null, orientation, createLegend,
        showTooltips, false), true, true, true, true, true);

    chart = getChart();
    plot = chart.getXYPlot();
    chartTitle = new TextTitle(title);
    chart.setTitle(chartTitle);
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    setCursor(Cursor.DEFAULT);

    nextDataSetNum = 0;
    labelsVisible = 1;

    final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setUpperMargin(AXIS_MARGINS);
    xAxis.setLowerMargin(AXIS_MARGINS);

    cursorPositionProperty = new SimpleObjectProperty<>();
    initializeChromatogramMouseListener();

    ZoomHistory history = getZoomHistory();
    if (history != null) {
      history.clear();
    }

    theme.apply(chart);
    defaultRenderer = new ColoredXYRenderer();
    plot.setRenderer(defaultRenderer);
  }

  public synchronized int addDataset(T datasetProvider) {
    ColoredXYDataset dataset = new ColoredXYDataset(datasetProvider);
    plot.setDataset(nextDataSetNum, dataset);
    plot.setRenderer(nextDataSetNum, defaultRenderer);
    nextDataSetNum++;
    return nextDataSetNum - 1;
  }

  /**
   * @param dataset
   * @return the dataset index
   */
  public synchronized int addDataset(XYDataset dataset) {
    plot.setDataset(nextDataSetNum, dataset);
    plot.setRenderer(nextDataSetNum, defaultRenderer);
    nextDataSetNum++;
    return nextDataSetNum - 1;
  }

  public synchronized XYDataset removeDataSet(int index) {
    XYDataset ds = plot.getDataset(index);
    plot.setDataset(index, null);
    plot.setRenderer(index, null);
    return ds;
  }

  /**
   * @param datasetProviders
   * @return Mapping of the dataset index and the provider values.
   */
  public Map<Integer, T> addDatasets(Collection<T> datasetProviders) {
    chart.setNotify(false);
    HashMap<Integer, T> map = new HashMap<>();
    for (T datasetProvider : datasetProviders) {
      map.put(this.addDataset(datasetProvider), datasetProvider);
    }
    chart.setNotify(true);
    chart.fireChartChanged();
    return map;
  }

  public synchronized void removeAllDatasets() {
    chart.setNotify(false);
    for (int i = 0; i < nextDataSetNum; i++) {
      plot.setDataset(i, null);
      plot.setRenderer(i, null);
    }
    chart.setNotify(true);
    chart.fireChartChanged();
  }

  public void setDomainAxisLabel(String label) {
    plot.getDomainAxis().setLabel(label);
  }

  public void setRangeAxisLabel(String label) {
    plot.getRangeAxis().setLabel(label);
  }

  public void setDomainAxisNumberFormatOverride(NumberFormat format) {
    ((NumberAxis) plot.getDomainAxis()).setNumberFormatOverride(format);
  }

  public void setRangeAxisNumberFormatOverride(NumberFormat format) {
    ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(format);
  }

  public void switchLegendVisible() {
    // Toggle legend visibility.
    final LegendTitle legend = getChart().getLegend();
    legend.setVisible(!legend.isVisible());
  }

  public void switchItemLabelsVisible() {
    labelsVisible = (labelsVisible == 1) ? 0 : 1;
    final int dataSetCount = plot.getDatasetCount();
    for (int i = 0; i < dataSetCount; i++) {
      final XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultItemLabelsVisible(labelsVisible == 1);
    }
  }

  public void switchBackground() {
    // Toggle background color
    final Paint color = getChart().getPlot().getBackgroundPaint();
    Color bgColor, liColor;
    if (color.equals(Color.darkGray)) {
      bgColor = Color.white;
      liColor = Color.darkGray;
    } else {
      bgColor = Color.darkGray;
      liColor = Color.white;
    }
    getChart().getPlot().setBackgroundPaint(bgColor);
    getChart().getXYPlot().setDomainGridlinePaint(liColor);
    getChart().getXYPlot().setRangeGridlinePaint(liColor);
  }

  public XYPlot getXYPlot() {
    return plot;
  }

  public PlotCursorPosition getCursorPosition() {
    return cursorPositionProperty.get();
  }

  public void setCursorPosition(PlotCursorPosition cursorPosition) {
    this.cursorPositionProperty.set(cursorPosition);
  }

  public ObjectProperty<PlotCursorPosition> cursorPositionProperty() {
    return cursorPositionProperty;
  }

  /**
   * Listens to clicks in the chromatogram plot and updates the selected raw data file accordingly.
   */
  private void initializeChromatogramMouseListener() {
    getCanvas().addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        if (event.getTrigger().getButton() == MouseButton.PRIMARY) {
          PlotCursorPosition pos = getCurrentCursorPosition();
          if (pos != null) {
            setCursorPosition(pos);
          }
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        // currently not in use
      }
    });
  }

  /**
   * @return current cursor position or null
   */
  private PlotCursorPosition getCurrentCursorPosition() {
    double domainValue = getXYPlot().getDomainCrosshairValue();
    double rangeValue = getXYPlot().getRangeCrosshairValue();

    return new PlotCursorPosition(domainValue, rangeValue);
  }
}
