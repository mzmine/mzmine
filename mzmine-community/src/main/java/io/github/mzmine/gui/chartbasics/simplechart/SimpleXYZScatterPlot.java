/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PaintScaleProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYSmallBlockRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.fx.FXGraphics2D;
import org.jfree.fx.FXHints;

/**
 * Used to plot XYZ datasets in a scatterplot-type of plot. Used to display spatial distribution in
 * imaging and ion mobility heatmaps.
 *
 * @author https://github.com/SteffenHeu & https://github.com/Annexhc
 */
public class SimpleXYZScatterPlot<T extends PlotXYZDataProvider> extends EChartViewer implements
    SimpleChart<T> {

  protected static final Logger logger = Logger.getLogger(SimpleXYZScatterPlot.class.getName());

  protected static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
  protected final Color legendBg = new Color(0, 0, 0, 0); // bg is transparent
  protected final JFreeChart chart;

  protected final ObjectProperty<PlotCursorPosition> cursorPositionProperty;
  protected final List<DatasetChangeListener> datasetListeners;
  protected final ObjectProperty<XYItemRenderer> defaultRenderer;
  protected final BooleanProperty itemLabelsVisible = new SimpleBooleanProperty(false);
  protected final BooleanProperty legendItemsVisible = new SimpleBooleanProperty(true);

  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  /**
   * May contain null value. Only used if paintscale different to the paintscale of dataset 0 shall
   * be used as legend.
   */
  private final ObjectProperty<PaintScale> legendPaintScale = new SimpleObjectProperty<>(null);
  protected RectangleEdge defaultPaintscaleLocation = RectangleEdge.RIGHT;
  protected NumberFormat legendAxisFormat;
  private int nextDataSetNum;
  private Canvas legendCanvas;
  private String legendLabel = null;
  /**
   * Needs to be stored in case a separate legend canvas is used, so we can redraw the legend when
   * the canvas is resized.
   */
  private Title currentLegend = null;

  private boolean legendVisible = true;

  public SimpleXYZScatterPlot() {
    this("");
  }

  public SimpleXYZScatterPlot(@NotNull String title) {

    super(ChartFactory.createScatterPlot("", "x", "y", null, PlotOrientation.VERTICAL, true, false,
        true), true, true, true, true, false);

    chart = getChart();
    chartTitle = new TextTitle(title);
    chart.setTitle(chartTitle);
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);
    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    defaultRenderer = new SimpleObjectProperty<>(new ColoredXYSmallBlockRenderer());
    legendAxisFormat = new DecimalFormat("0.##E0");
    setCursor(Cursor.DEFAULT);
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(this);

    cursorPositionProperty = new SimpleObjectProperty<>(new PlotCursorPosition(0, 0, -1, null));
    initializeMouseListener();
    initLabelListeners();

    datasetListeners = new ArrayList<>();

    plot.setRenderer(defaultRenderer.get());
    initializePlot();
    nextDataSetNum = 0;

    legendPaintScale.addListener(((observable, oldValue, newValue) -> updateLegend()));
  }

  /**
   * Updates the legend to the paint scale currently set via
   * {@link #setLegendPaintScale(PaintScale)} or the paint scale from data set with index 0. If
   * neither is set, the legend is removed.
   */
  public void updateLegend() {
    final XYDataset dataset = getXYPlot().getDataset(0);
    if (dataset == null) {
      return;
    }
    getChart().clearSubtitles();
    if (!legendVisible) {
      return;
    }

    if (dataset instanceof PaintScaleProvider) {
      final PaintScale paintScale;
      if (this.legendPaintScale.get() != null) {
        paintScale = legendPaintScale.get();
      } else if (dataset instanceof XYZDataset) {
        paintScale = makePaintScale((XYZDataset) dataset);
      } else {
        return;
      }

      PaintScaleLegend legend = generateLegend(paintScale);
      if (legendCanvas != null) {
        drawLegendToSeparateCanvas(legend);
        currentLegend = legend;
      } else {
        getChart().addSubtitle(legend);
        currentLegend = null;
      }
    } else {
      LegendTitle legend = new LegendTitle(getChart().getXYPlot());
      legend.setPosition(RectangleEdge.BOTTOM);
      final EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
      legend.setBackgroundPaint(legendBg);
      legend.setItemFont(theme.getRegularFont());
      legend.setItemPaint(theme.getLegendItemPaint());
      getChart().addLegend(legend);
    }
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

  /**
   * @param dataset the main dataset. null to clear the plot. Removes all other datasets.
   */
  public synchronized void setDataset(@Nullable ColoredXYZDataset dataset) {

    removeAllDatasets();
    if (dataset == null) {
      return;
    }

    plot.setDataset(dataset);
    plot.setRenderer(defaultRenderer.get());
    if (dataset.getStatus() == TaskStatus.FINISHED) {
      datasetChanged(new DatasetChangeEvent(this, dataset));
    }
    dataset.addChangeListener(
        event -> datasetChanged(new DatasetChangeEvent(this, event.getDataset())));
    if (nextDataSetNum == 0) {
      nextDataSetNum++;
    }

    notifyDatasetChangeListeners(new DatasetChangeEvent(this, dataset));
  }

  /**
   * Creates a dataset and sets it as the chart's main data set. Removes all other datasets.
   *
   * @param dataProvider The data provider
   */
  public void setDataset(T dataProvider) {
    ColoredXYZDataset dataset = new ColoredXYZDataset(dataProvider);
    setDataset(dataset);
  }

  /**
   * @return The dataset index.
   */
  public synchronized int addDataset(XYZDataset dataset, XYItemRenderer renderer) {

    // jfreechart renderers dont check if the value actually changed and notify either way
    if (renderer.getDefaultItemLabelsVisible() != isItemLabelsVisible()) {
      renderer.setDefaultItemLabelsVisible(isItemLabelsVisible());
    }
    if (renderer.getDefaultSeriesVisibleInLegend() != isLegendItemsVisible()) {
      renderer.setDefaultItemLabelsVisible(isLegendItemsVisible());
    }
    plot.setDataset(nextDataSetNum, dataset);
    plot.setRenderer(nextDataSetNum, renderer);
    nextDataSetNum++;

    if (chart.isNotify()) {
      notifyDatasetChangeListeners(new DatasetChangeEvent(this, dataset));
    }
    if (dataset instanceof ColoredXYZDataset) {
      dataset.addChangeListener(e -> datasetChanged(new DatasetChangeEvent(this, e.getDataset())));
    }

    return nextDataSetNum - 1;
  }

  /**
   * Adds a dataset with the default renderer.
   *
   * @param datasetProvider The provider
   * @return The dataset index
   */
  public synchronized int addDataset(T datasetProvider) {

    if (datasetProvider instanceof XYZDataset) {
      return addDataset((XYZDataset) datasetProvider, defaultRenderer.get());
    }
    ColoredXYZDataset dataset = new ColoredXYZDataset(datasetProvider);
    return addDataset(dataset, defaultRenderer.get());
  }

  @Override
  public synchronized int addDataset(T datasetProvider, XYItemRenderer renderer) {
    if (datasetProvider instanceof XYZDataset) {
      return addDataset((XYZDataset) datasetProvider, renderer);
    }
    ColoredXYZDataset dataset = new ColoredXYZDataset(datasetProvider);
    return addDataset(dataset, renderer);
  }

  public void addDatasetsAndRenderers(Map<XYZDataset, XYItemRenderer> datasetsAndRenderers) {
    getChart().setNotify(false);
    getXYPlot().setNotify(false);
    datasetsAndRenderers.forEach(this::addDataset);
    getXYPlot().setNotify(true);
    getChart().setNotify(true);
    getChart().fireChartChanged();
  }

  public synchronized void removeAllDatasets() {

    chart.setNotify(false);
    plot.setNotify(false);
    for (int i = 0; i < nextDataSetNum; i++) {
      XYDataset ds = plot.getDataset(i);
      if (ds instanceof Task) {
        ((Task) ds).cancel();
      }
      plot.setDataset(i, null);
      plot.setRenderer(i, null);
      if (ds != null) {
        ds.removeChangeListener(getXYPlot());
      }
    }
    plot.setNotify(true);
    chart.setNotify(true);
    chart.fireChartChanged();
    notifyDatasetChangeListeners(new DatasetChangeEvent(this, null));
    nextDataSetNum = 0;
  }

  @Override
  public void setShowCrosshair(boolean show) {
    getXYPlot().setDomainCrosshairVisible(show);
    getXYPlot().setRangeCrosshairVisible(show);
  }

  @Override
  public XYItemRenderer getDefaultRenderer() {
    return defaultRenderer.get();
  }

  @Override
  public void setDefaultRenderer(XYItemRenderer defaultRenderer) {
    this.defaultRenderer.set(defaultRenderer);
  }

  @Override
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
          setCursorPosition(pos);
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
  @NotNull
  private PlotCursorPosition getCurrentCursorPosition() {
    final double domainValue = getXYPlot().getDomainCrosshairValue();
    final double rangeValue = getXYPlot().getRangeCrosshairValue();
    double zValue = PlotCursorPosition.DEFAULT_Z_VALUE;

    // mabye there is a more efficient way of searching for the selected value index.
    int index = -1;
    int datasetIndex = -1;
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset instanceof ColoredXYZDataset) {
        index = ((ColoredXYZDataset) dataset).getValueIndex(domainValue, rangeValue);
      } else if (dataset instanceof ColoredXYZPieDataset) {
        index = ((ColoredXYZPieDataset) dataset).getValueIndex(domainValue, rangeValue);
      }
      if (index != -1) {
        datasetIndex = i;
        if (dataset instanceof ColoredXYZDataset) {
          zValue = ((ColoredXYZDataset) dataset).getZValue(0, index);
        }
        break;
      }
    }

    return (index != -1) ? new PlotCursorPosition(domainValue, rangeValue, zValue, index,
        plot.getDataset(datasetIndex))
        : new PlotCursorPosition(domainValue, rangeValue, index, null);
  }

  public XYPlot getXYPlot() {
    return plot;
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

  public void setLegendNumberFormatOverride(NumberFormat format) {
    this.legendAxisFormat = format;
    datasetChanged(new DatasetChangeEvent(this, getXYPlot().getDataset()));
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
    if (!chart.isNotify()) {
      return;
    }

    for (DatasetChangeListener listener : datasetListeners) {
      listener.datasetChanged(event);
    }
  }

  private void initializePlot() {
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
  }

  /**
   * @param dataset
   * @return Paint scale based on the datasets min and max values.
   */
  private PaintScale makePaintScale(XYZDataset dataset) {
    if (!(dataset instanceof ColoredXYZDataset xyz)
        || ((ColoredXYZDataset) dataset).getPaintScale() == null
        || ((ColoredXYZDataset) dataset).getStatus() != TaskStatus.FINISHED) {
      org.jfree.data.Range range = Objects.requireNonNullElse(DatasetUtils.findZBounds(dataset),
          new Range(0d, 1d));
      return new LookupPaintScale(range.getLowerBound(), range.getUpperBound(), Color.BLACK);
    } else {
      return xyz.getPaintScale();
    }
  }

  /**
   * @param event Called when the dataset is changed, e.g. when the calculation finished.
   */
  @Override
  public void datasetChanged(DatasetChangeEvent event) {
    super.datasetChanged(event);

    if (!(event.getDataset() instanceof XYZDataset dataset)) {
      return;
    }

    if (getXYPlot().indexOf((XYDataset) event.getDataset()) == 0) {
      updateLegend();
    }

    MZmineCore.getConfiguration().getDefaultChartTheme().applyToLegend(chart);
    if (chart.isNotify()) {
      chart.fireChartChanged();
    }

    notifyDatasetChangeListeners(event);
  }

  public Canvas getLegendCanvas() {
    return legendCanvas;
  }

  /**
   * Will draw the legend to a separate canvas on the next dataset changed event. Make sure the
   * canvas is appropriately sized.
   *
   * @param canvas The canvas.
   */
  public void setLegendCanvas(@Nullable Canvas canvas) {
    this.legendCanvas = canvas;

    widthProperty().addListener(((observable, oldValue, newValue) -> {
      if (defaultPaintscaleLocation == RectangleEdge.BOTTOM
          || defaultPaintscaleLocation == RectangleEdge.TOP) {
        legendCanvas.setWidth(newValue.doubleValue());
      }
    }));

    heightProperty().addListener(((observable, oldValue, newValue) -> {
      if (defaultPaintscaleLocation == RectangleEdge.LEFT
          || defaultPaintscaleLocation == RectangleEdge.RIGHT) {
        legendCanvas.setHeight(newValue.doubleValue());
      }
    }));

    legendCanvas.widthProperty().addListener(((observable, oldValue, newValue) -> {
      if (currentLegend != null) {
        drawLegendToSeparateCanvas(currentLegend);
      }
    }));
  }

  public void setLegendAxisLabel(@Nullable String label) {
    legendLabel = label;
  }

  private void drawLegendToSeparateCanvas(Title legend) {
    assert legendCanvas != null;
    GraphicsContext gc = legendCanvas.getGraphicsContext2D();
    FXGraphics2D g2 = new FXGraphics2D(gc);
    gc.clearRect(0, 0, legendCanvas.getWidth(), legendCanvas.getHeight()); // clear canvas
    g2.setRenderingHint(FXHints.KEY_USE_FX_FONT_METRICS, true);
    g2.setZeroStrokeWidth(0.1);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    legend.draw(g2, new Rectangle((int) legendCanvas.getWidth(), (int) legendCanvas.getHeight()));
  }

  /**
   * @return Mapping of datasetIndex -> Dataset
   */
  @Override
  public LinkedHashMap<Integer, XYDataset> getAllDatasets() {
    final LinkedHashMap<Integer, XYDataset> datasetMap = new LinkedHashMap<>();
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset != null) {
        datasetMap.put(i, dataset);
      }
    }
    return datasetMap;
  }

  /**
   * @param scale The paint scale
   * @return Legend based on the {@link LookupPaintScale}.
   */
  private PaintScaleLegend generateLegend(@NotNull PaintScale scale) {
    Paint axisPaint = getXYPlot().getDomainAxis().getAxisLinePaint();
    Font axisLabelFont = getXYPlot().getDomainAxis().getLabelFont();
    Font axisTickLabelFont = getXYPlot().getDomainAxis().getTickLabelFont();

    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(scale.getLowerBound(),
        Math.max(scale.getUpperBound(), scale.getUpperBound() * 1E-10));
    scaleAxis.setAxisLinePaint(axisPaint);
    scaleAxis.setTickMarkPaint(axisPaint);
    scaleAxis.setNumberFormatOverride(legendAxisFormat);
    scaleAxis.setLabelFont(axisLabelFont);
    scaleAxis.setLabelPaint(axisPaint);
    scaleAxis.setTickLabelFont(axisTickLabelFont);
    scaleAxis.setTickLabelPaint(axisPaint);
    if (legendLabel != null) {
      scaleAxis.setLabel(legendLabel);
    }
    PaintScaleLegend newLegend = new PaintScaleLegend(scale, scaleAxis);
    newLegend.setPadding(5, 0, 5, 0);
    newLegend.setStripOutlineVisible(false);
    newLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    newLegend.setAxisOffset(5.0);
    newLegend.setSubdivisionCount(500);
    newLegend.setPosition(defaultPaintscaleLocation);
    newLegend.setBackgroundPaint(legendBg);
    return newLegend;
  }

  public PaintScale getLegendPaintScale() {
    return legendPaintScale.get();
  }

  public void setLegendPaintScale(PaintScale legendPaintScale) {
    this.legendPaintScale.set(legendPaintScale);
  }

  public ObjectProperty<PaintScale> legendPaintScaleProperty() {
    return legendPaintScale;
  }

  public RectangleEdge getDefaultPaintscaleLocation() {
    return defaultPaintscaleLocation;
  }

  public void setDefaultPaintscaleLocation(RectangleEdge defaultPaintscaleLocation) {
    this.defaultPaintscaleLocation = defaultPaintscaleLocation;
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

  public boolean isLegendVisible() {
    return legendVisible;
  }

  public void setLegendVisible(boolean legendVisible) {
    this.legendVisible = legendVisible;
    updateLegend();
  }
}
