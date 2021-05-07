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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleXYLabelGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ExampleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javax.annotation.Nonnull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;

/**
 * Generic plot class that can be used to plot everything that implements the {@link
 * PlotXYDataProvider} interface or is a {@link ColoredXYDataset}.
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 * @see ExampleXYProvider
 * @see ColoredXYDataset
 * @see PlotXYDataProvider
 */
public class SimpleXYChart<T extends PlotXYDataProvider> extends
    EChartViewer implements SimpleChart<T> {

  private static final double AXIS_MARGINS = 0.01;
  private static Logger logger = Logger.getLogger(SimpleXYChart.class.getName());

  protected final JFreeChart chart;
  protected final ObjectProperty<XYItemRenderer> defaultRenderer;
  protected final BooleanProperty itemLabelsVisible = new SimpleBooleanProperty(true);
  protected final BooleanProperty legendItemsVisible = new SimpleBooleanProperty(true);

  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  private final ObjectProperty<PlotCursorPosition> cursorPositionProperty = new SimpleObjectProperty<>(
      new PlotCursorPosition(0, 0, -1, null));

  private final List<DatasetChangeListener> datasetListeners;
  protected EStandardChartTheme theme;
  protected SimpleXYLabelGenerator defaultLabelGenerator;
  protected SimpleToolTipGenerator defaultToolTipGenerator;
  protected ColoredXYLineRenderer defaultLineRenderer;
  protected ColoredXYShapeRenderer defaultShapeRenderer;

  private int nextDataSetNum;

  public SimpleXYChart() {
    this("x", "y");
  }

  public SimpleXYChart(@NamedArg("title") String title) {
    this(title, "x", "y");
  }

  public SimpleXYChart(@NamedArg("xlabel") String xLabel,
      @NamedArg("ylabel") String yLabel) {
    this("", xLabel, yLabel);
  }

  public SimpleXYChart(@NamedArg("title") @Nonnull String title,
      @NamedArg("xlabel") String xLabel,
      @NamedArg("ylabel") String yLabel) {
    this(title, xLabel, yLabel, PlotOrientation.VERTICAL, true, true);
  }

  public SimpleXYChart(@NamedArg("title") @Nonnull String title,
      @NamedArg("xlabel") String xLabel,
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

    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(this);

    nextDataSetNum = 0;

    final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setUpperMargin(AXIS_MARGINS);
    xAxis.setLowerMargin(AXIS_MARGINS);

    initializeMouseListener();
    initLabelListeners();

    ZoomHistory history = getZoomHistory();
    if (history != null) {
      history.clear();
    }

    defaultLabelGenerator = new SimpleXYLabelGenerator(this);
    defaultToolTipGenerator = new SimpleToolTipGenerator();
    defaultShapeRenderer = new ColoredXYShapeRenderer();
    defaultLineRenderer = new ColoredXYLineRenderer();
    defaultRenderer = new SimpleObjectProperty<>();
    defaultRenderer.addListener((obs, old, newValue) -> {
      newValue.setDefaultItemLabelsVisible(true);
      newValue.setDefaultToolTipGenerator(defaultToolTipGenerator);
      newValue.setDefaultItemLabelGenerator(defaultLabelGenerator);
      getXYPlot().rendererChanged(new RendererChangeEvent(newValue));
    });
    defaultRenderer.set(defaultLineRenderer);
    plot.setRenderer(defaultRenderer.get());

    defaultLineRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());
    defaultShapeRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());

    datasetListeners = new ArrayList<>();
  }

  private void initLabelListeners() {
    // automatically update label visibility
    itemLabelsVisible.addListener((obs, old, newVal) -> {
      for(int i = 0; i < getXYPlot().getRendererCount(); i++) {
        XYItemRenderer renderer = getXYPlot().getRenderer(i);
        if(renderer != null) {
          renderer.setDefaultItemLabelsVisible(newVal, false);
        }
      }
      getChart().fireChartChanged();
    });

    legendItemsVisible.addListener((obs, old, newVal) -> {
      for(int i = 0; i < getXYPlot().getRendererCount(); i++) {
        XYItemRenderer renderer = getXYPlot().getRenderer(i);
        if(renderer != null) {
          renderer.setDefaultSeriesVisibleInLegend(newVal, false);
        }
      }
      final LegendTitle legend = getChart().getLegend();
      if(legend != null) {
        legend.setVisible(newVal);
      }
      getChart().fireChartChanged();
    });
  }

  public synchronized int addDataset(XYDataset dataset, XYItemRenderer renderer) {
    assert Platform.isFxApplicationThread();
    // jfreechart renderers dont check if the value actually changed and notify either way
    if(renderer.getDefaultItemLabelsVisible() != isItemLabelsVisible()) {
      renderer.setDefaultItemLabelsVisible(isItemLabelsVisible());
    }
    if (renderer.getDefaultSeriesVisibleInLegend() != isLegendItemsVisible()) {
      renderer.setDefaultItemLabelsVisible(isLegendItemsVisible());
    }
    plot.setDataset(nextDataSetNum, dataset);
    plot.setRenderer(nextDataSetNum, renderer);
    nextDataSetNum++;
    notifyDatasetChangeListeners(new DatasetChangeEvent(this, dataset));
    return nextDataSetNum - 1;
  }

  @Override
  public synchronized int addDataset(T datasetProvider) {
    if (datasetProvider instanceof XYDataset) {
      return addDataset((XYDataset) datasetProvider);
    }
    ColoredXYDataset dataset = new ColoredXYDataset(datasetProvider);
    return addDataset(dataset, defaultRenderer.get());
  }

  /**
   * @param dataset
   * @return the dataset index
   */
  public synchronized int addDataset(XYDataset dataset) {
    assert Platform.isFxApplicationThread();
    return addDataset(dataset, defaultRenderer.get());
  }

  public synchronized XYDataset removeDataSet(int index) {
    assert Platform.isFxApplicationThread();
    XYDataset ds = plot.getDataset(index);
    if (ds instanceof Task) { // stop calculation in case it's still running
      ((Task) ds).cancel();
    }
    plot.setDataset(index, null);
    plot.setRenderer(index, null);
    if(ds != null) {
      ds.removeChangeListener(getXYPlot());
    }
//    notifyDatasetChangeListeners();
    return ds;
  }

  /**
   * @param datasetProviders
   * @return Mapping of the dataset index and the provider values.
   */
  public Map<Integer, T> addDatasetProviders(Collection<T> datasetProviders) {
    assert Platform.isFxApplicationThread();
    chart.setNotify(false);
    HashMap<Integer, T> map = new HashMap<>();
    for (T datasetProvider : datasetProviders) {
      map.put(this.addDataset(datasetProvider), datasetProvider);
    }
    chart.setNotify(true);
    chart.fireChartChanged();
    // todo maybe notify for each dataset
    notifyDatasetChangeListeners(new DatasetChangeEvent(this, null));
    return map;
  }

  /**
   * @param datasets
   * @return Mapping of the dataset index and the datasets.
   */
  public Map<Integer, ColoredXYDataset> addDatasets(
      Collection<? extends ColoredXYDataset> datasets) {
    chart.setNotify(false);
    HashMap<Integer, ColoredXYDataset> map = new HashMap<>();
    for (ColoredXYDataset dataset : datasets) {
      map.put(this.addDataset(dataset), dataset);
    }
    chart.setNotify(true);
    chart.fireChartChanged();
    // todo maybe notify for each dataset
    notifyDatasetChangeListeners(new DatasetChangeEvent(this, null));
    return map;
  }

  public synchronized void removeAllDatasets() {
    assert Platform.isFxApplicationThread();
    chart.setNotify(false);
    plot.setNotify(false);
    for (int i = 0; i < nextDataSetNum; i++) {
      XYDataset ds = plot.getDataset(i);
      if (ds instanceof Task) {
        ((Task) ds).cancel();
      }
      plot.setDataset(i, null);
      plot.setRenderer(i, null);
      ds.removeChangeListener(getXYPlot());
    }
    plot.setNotify(true);
    chart.setNotify(true);
    chart.fireChartChanged();
    notifyDatasetChangeListeners(new DatasetChangeEvent(this, null));
    nextDataSetNum = 0;
  }

  @Override
  public LinkedHashMap<Integer, XYDataset> getAllDatasets() {
    final LinkedHashMap<Integer, XYDataset> datasetMap = new LinkedHashMap<Integer, XYDataset>();

    for (int i = 0; i < nextDataSetNum; i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset != null) {
        datasetMap.put(i, dataset);
      }
    }
    return datasetMap;
  }

  @Override
  public void setDomainAxisLabel(String label) {
    plot.getDomainAxis().setLabel(label);
  }

  @Override
  public void setRangeAxisLabel(String label) {
    plot.getRangeAxis().setLabel(label);
  }

  @Override
  public void setDomainAxisNumberFormatOverride(NumberFormat format) {
    ((NumberAxis) plot.getDomainAxis()).setNumberFormatOverride(format);
  }

  @Override
  public void setRangeAxisNumberFormatOverride(NumberFormat format) {
    ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(format);
  }

  @Override
  public void setLegendItemsVisible(boolean visible) {
    legendItemsVisible.set(visible);
  }

  public boolean isLegendItemsVisible() {
    return legendItemsVisible.get();
  }

  public BooleanProperty legendItemsVisibleProperty() {
    return legendItemsVisible;
  }

  @Override
  public void setShowCrosshair(boolean show) {
    getXYPlot().setDomainCrosshairVisible(show);
    getXYPlot().setRangeCrosshairVisible(show);
  }

  public XYPlot getXYPlot() {
    return plot;
  }

  public XYItemRenderer getDefaultRenderer() {
    return defaultRenderer.get();
  }

  public void setDefaultRenderer(XYItemRenderer defaultRenderer) {
    this.defaultRenderer.set(defaultRenderer);
  }

  public ObjectProperty<XYItemRenderer> defaultRendererProperty() {
    return defaultRenderer;
  }

  @Override
  public PlotCursorPosition getCursorPosition() {
    return cursorPositionProperty.get();
  }

  @Override
  public void setCursorPosition(PlotCursorPosition cursorPosition) {
    if (cursorPosition.equals(cursorPositionProperty().get())) {
      return;
    }
    this.cursorPositionProperty.set(cursorPosition);
  }

  @Override
  public ObjectProperty<PlotCursorPosition> cursorPositionProperty() {
    return cursorPositionProperty;
  }

  /**
   * Listens to clicks in the chromatogram plot and updates the selected raw data file accordingly.
   */
  private void initializeMouseListener() {
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

  private PlotCursorPosition getCurrentCursorPosition() {
    double domainValue = getXYPlot().getDomainCrosshairValue();
    double rangeValue = getXYPlot().getRangeCrosshairValue();

    // mabye there is a more efficient way of searching for the selected value index.
    int index = -1;
    int datasetIndex = -1;
    for (int i = 0; i < nextDataSetNum; i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset instanceof ColoredXYDataset) {
        index = ((ColoredXYDataset) dataset).getValueIndex(domainValue, rangeValue);
      }
      if (index != -1) {
        datasetIndex = i;
        break;
      }
    }

    return (index != -1) ?
        new PlotCursorPosition(domainValue, rangeValue, index,
            plot.getDataset(datasetIndex)) : null;
  }

  @Override
  public void addContextMenuItem(String title, EventHandler<ActionEvent> ai) {
    addMenuItem(getContextMenu(), title, ai);
  }

  @Override
  public void addDatasetChangeListener(DatasetChangeListener listener) {
    datasetListeners.add(listener);
  }

  @Override
  public void removeDatasetChangeListener(DatasetChangeListener listener) {
    datasetListeners.remove(listener);
  }

  @Override
  public void clearDatasetChangeListeners() {
    datasetListeners.clear();
  }

  public void notifyDatasetChangeListeners(DatasetChangeEvent event) {
    for (DatasetChangeListener listener : datasetListeners) {
      listener.datasetChanged(event);
    }
  }

  public boolean isItemLabelsVisible() {
    return itemLabelsVisible.get();
  }

  public BooleanProperty itemLabelsVisibleProperty() {
    return itemLabelsVisible;
  }

  public void setItemLabelsVisible(boolean itemLabelsVisible) {
    this.itemLabelsVisible.set(itemLabelsVisible);
  }
}
