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

package io.github.mzmine.gui.chartbasics.simplechart;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxJFreeChart;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxJFreeChartModel;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxXYPlot;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.PlotCursorUtils;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleXYLabelGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ExampleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.logging.Logger;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Generic plot class that can be used to plot everything that implements the
 * {@link PlotXYDataProvider} interface or is a {@link ColoredXYDataset}.
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 * @see ExampleXYProvider
 * @see ColoredXYDataset
 * @see PlotXYDataProvider
 */
public class SimpleXYChart<T extends PlotXYDataProvider> extends EChartViewer implements
    SimpleChart<T> {

  private static final double AXIS_MARGINS = 0.01;
  private static final Logger logger = Logger.getLogger(SimpleXYChart.class.getName());

  protected final ObjectProperty<XYItemRenderer> defaultRenderer = new SimpleObjectProperty<>();
  protected final BooleanProperty itemLabelsVisible = new SimpleBooleanProperty(true);
  protected final BooleanProperty legendItemsVisible = new SimpleBooleanProperty(true);

  private final FxXYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  // taken from only plot
  private final ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty;

  protected EStandardChartTheme theme;
  protected SimpleXYLabelGenerator defaultLabelGenerator;
  protected SimpleToolTipGenerator defaultToolTipGenerator = new SimpleToolTipGenerator();
  protected ColoredXYLineRenderer defaultLineRenderer = new ColoredXYLineRenderer();
  protected ColoredAreaShapeRenderer defaultShapeRenderer = new ColoredAreaShapeRenderer();

  public SimpleXYChart() {
    this("x", "y");
  }

  public SimpleXYChart(@NamedArg("title") String title) {
    this(title, "x", "y");
  }

  public SimpleXYChart(@NamedArg("xlabel") String xLabel, @NamedArg("ylabel") String yLabel) {
    this("", xLabel, yLabel);
  }

  public SimpleXYChart(@NamedArg("title") @NotNull String title, @NamedArg("xlabel") String xLabel,
      @NamedArg("ylabel") String yLabel) {
    this(title, xLabel, yLabel, PlotOrientation.VERTICAL, true, true);
  }

  public SimpleXYChart(@NamedArg("title") @NotNull String title, @NamedArg("xlabel") String xLabel,
      @NamedArg("ylabel") String yLabel, @NamedArg("orientation") PlotOrientation orientation,
      @NamedArg("legend") boolean createLegend, @NamedArg("tooltips") boolean showTooltips) {
    final FxJFreeChart internalChart = FxChartFactory.createXYLineChart(title, xLabel, yLabel, null,
        orientation, createLegend, showTooltips, false);

    super(internalChart, true, true, true, true, true);

    plot = (FxXYPlot) getChart().getXYPlot();
    cursorPositionProperty = plot.cursorPositionProperty(); // use cursor from plot

    chartTitle = new TextTitle(title);
    getChart().setTitle(chartTitle);
    chartSubTitle = new TextTitle();
    getChart().addSubtitle(chartSubTitle);
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    setCursor(Cursor.DEFAULT);

    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(this);

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

    // equals check is done internally
    cursorPositionProperty.addListener((_, _, newPosition) -> {
      if (newPosition != null) {
        // notify only for second change and only if notification is not disabled
        plot.setDomainCrosshairValue(newPosition.getDomainValue(), false);
        plot.setRangeCrosshairValue(newPosition.getRangeValue(), plot.isNotify());
      }
    });
  }

  @Override
  public FxJFreeChart getChart() {
    // only uses this type internally in constructor
    return (FxJFreeChart) super.getChart();
  }

  public FxJFreeChartModel getChartModel() {
    return getChart().getChartModel();
  }

  private void initLabelListeners() {
    // automatically update label visibility
    itemLabelsVisible.addListener((obs, old, newVal) -> {
      for (int i = 0; i < getXYPlot().getRendererCount(); i++) {
        XYItemRenderer renderer = getXYPlot().getRenderer(i);
        if (renderer != null) {
          renderer.setDefaultItemLabelsVisible(newVal, false);
        }
      }
      getChart().fireChartChanged();
    });

    legendItemsVisible.addListener((obs, old, newVal) -> {
      for (int i = 0; i < getXYPlot().getRendererCount(); i++) {
        XYItemRenderer renderer = getXYPlot().getRenderer(i);
        if (renderer != null) {
          renderer.setDefaultSeriesVisibleInLegend(newVal, false);
        }
      }
      final LegendTitle legend = getChart().getLegend();
      if (legend != null) {
        legend.setVisible(newVal);
      }
      getChart().fireChartChanged();
    });
  }

  public synchronized int addDataset(XYDataset dataset, XYItemRenderer renderer) {
    prepareRenderer(renderer);
    return plot.addDataset(dataset, renderer);
  }

  /**
   * apply renderer config like item visibility and legend visibility.
   *
   * @return the input renderer instance is returned for convenience
   */
  private XYItemRenderer prepareRenderer(XYItemRenderer renderer) {
    if (renderer.getDefaultItemLabelsVisible() != isItemLabelsVisible()) {
      renderer.setDefaultItemLabelsVisible(isItemLabelsVisible(), false);
    }
    if (renderer.getDefaultSeriesVisibleInLegend() != isLegendItemsVisible()) {
      renderer.setDefaultSeriesVisibleInLegend(isLegendItemsVisible(), false);
    }
    return renderer;
  }

  @NotNull
  private XYDataset providerToDataset(T datasetProvider) {
    return datasetProvider instanceof XYDataset dataset ? dataset
        : new ColoredXYDataset(datasetProvider);
  }

  /**
   * Creates a dataset and sets it as the chart's main data set. Removes all other datasets.
   *
   * @param dataProvider The data provider
   */
  public void setDataset(T dataProvider) {
    setDataset(providerToDataset(dataProvider));
  }

  /**
   * @param dataset the main dataset. null to clear the plot. Removes all other datasets.
   */
  public synchronized void setDataset(@Nullable XYDataset dataset) {
    if (dataset == null) {
      removeAllDatasets();
      return;
    }
    // single event set all datasets to just one
    plot.setDatasets(List.of(dataset), List.of(prepareRenderer(defaultRenderer.get())));
  }

  @Override
  public synchronized int addDataset(T datasetProvider) {
    return addDataset(datasetProvider, defaultRenderer.get());
  }

  @Override
  public synchronized int addDataset(T datasetProvider, XYItemRenderer renderer) {
    return addDataset(providerToDataset(datasetProvider), renderer);
  }

  /**
   * @param dataset
   * @return the dataset index
   */
  public synchronized int addDataset(XYDataset dataset) {
    return addDataset(dataset, defaultRenderer.get());
  }

  @Nullable
  public synchronized XYDataset removeDataSet(int index) {
    return removeDataSet(index, true);
  }

  /**
   * @param index  The dataset index
   * @param notify If listeners shall be notified.
   * @return The dataset or null
   * @see XYPlot#indexOf(XYDataset)
   */
  @Nullable
  public synchronized XYDataset removeDataSet(int index, boolean notify) {
    return plot.removeDataSet(index);
  }

  public void addDatasetProviders(Collection<T> datasetProviders) {
    final List<@NotNull XYDataset> datasets = datasetProviders.stream().filter(Objects::nonNull)
        .map(this::providerToDataset).toList();
    final XYItemRenderer renderer = prepareRenderer(defaultRenderer.get());

    // reduces the number of events
    plot.addDatasets(datasets, renderer);
  }

  public void addDatasets(SequencedCollection<? extends ColoredXYDataset> datasets) {
    final XYItemRenderer renderer = prepareRenderer(defaultRenderer.get());
    // reduces the number of events
    plot.addDatasets(datasets, renderer);
  }

  public void setDatasets(@NotNull SequencedCollection<? extends ColoredXYDataset> datasets) {
    plot.setDatasets(datasets);
  }

  public void setDatasetsAndRenderers(@NotNull List<@NotNull DatasetAndRenderer> datasets) {
    final List<ColoredXYDataset> data = datasets.stream().map(DatasetAndRenderer::dataset).toList();
    final List<XYItemRenderer> renderers = datasets.stream().map(DatasetAndRenderer::renderer)
        .map(r -> r != null ? r : defaultRenderer.get()).map(this::prepareRenderer).toList();

    plot.setDatasets(data, renderers);
  }

  public synchronized void removeAllDatasets() {
    plot.removeAllDatasets();
  }

  @Override
  public LinkedHashMap<Integer, XYDataset> getAllDatasets() {
    return new LinkedHashMap<>(plot.getDatasets());
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

  public boolean isLegendItemsVisible() {
    return legendItemsVisible.get();
  }

  @Override
  public void setLegendItemsVisible(boolean visible) {
    legendItemsVisible.set(visible);
  }

  public BooleanProperty legendItemsVisibleProperty() {
    return legendItemsVisible;
  }

  @Override
  public void setShowCrosshair(boolean show) {
    getXYPlot().setDomainCrosshairVisible(show);
    getXYPlot().setRangeCrosshairVisible(show);
  }

  public FxXYPlot getXYPlot() {
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
    if (Objects.equals(cursorPosition, cursorPositionProperty().get())) {
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
    PlotCursorUtils.addMouseListener(this, plot, cursorPositionProperty);
  }

  @Override
  public void addContextMenuItem(String title, EventHandler<ActionEvent> ai) {
    addMenuItem(getContextMenu(), title, ai);
  }

  @Override
  public void addDatasetChangeListener(DatasetChangeListener listener) {
    plot.addDatasetChangeListener(listener);
  }

  public boolean isItemLabelsVisible() {
    return itemLabelsVisible.get();
  }

  public void setItemLabelsVisible(boolean itemLabelsVisible) {
    this.itemLabelsVisible.set(itemLabelsVisible);
  }

  public BooleanProperty itemLabelsVisibleProperty() {
    return itemLabelsVisible;
  }

  public void addRegression(ColoredXYDataset dataset, int series) {
    var regressionRenderer = new XYLineAndShapeRenderer(true, false);
    regressionRenderer.setSeriesPaint(series, dataset.getAWTColor());

    double[] coefficients = Regression.getOLSRegression(dataset, series);
    double b = coefficients[0]; // intercept
    double m = coefficients[1]; // slope

    Range<Double> xrange = dataset.getDomainValueRange();
    XYSeries trend = new XYSeries(String.format("y=%.3f+%.3f", b, m));
    double x = xrange.lowerEndpoint();
    trend.add(x, m * x + b);
    x = xrange.upperEndpoint();
    trend.add(x, m * x + b);

    addDataset(new XYSeriesCollection(trend), regressionRenderer);
  }

}
