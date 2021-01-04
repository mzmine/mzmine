/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYSmallBlockRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
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
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.general.DatasetChangeListener;
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
  protected final JFreeChart chart;

  protected final ObjectProperty<PlotCursorPosition> cursorPositionProperty;
  protected final List<DatasetsChangedListener> datasetListeners;
  protected final ObjectProperty<XYItemRenderer> defaultRenderer;
  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  protected RectangleEdge defaultPaintscaleLocation = RectangleEdge.RIGHT;
  protected ColoredXYSmallBlockRenderer blockRenderer;
  protected NumberFormat legendAxisFormat;
  private int nextDataSetNum;
  private Canvas legendCanvas;

  public SimpleXYZScatterPlot() {
    this("");
  }

  public SimpleXYZScatterPlot(@Nonnull String title) {

    super(ChartFactory.createScatterPlot("", "x", "y", null,
        PlotOrientation.VERTICAL, true, false, true), true, true, true, true, false);

    chart = getChart();
    chartTitle = new TextTitle(title);
    chart.setTitle(chartTitle);
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);
    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    blockRenderer = new ColoredXYSmallBlockRenderer();
    defaultRenderer = new SimpleObjectProperty<>(blockRenderer);
    legendAxisFormat = new DecimalFormat("0.##E0");
    setCursor(Cursor.DEFAULT);

    cursorPositionProperty = new SimpleObjectProperty<>(new PlotCursorPosition(0, 0, -1, null));
    initializeMouseListener();
    datasetListeners = new ArrayList<>();

    plot.setRenderer(defaultRenderer.get());
    initializePlot();
    nextDataSetNum = 0;

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
  }

  /**
   * @param dataset the dataset. null to clear the plot. Removes all other datasets.
   */
  public void setDataset(@Nullable ColoredXYZDataset dataset) {
    removeAllDatasets();
    plot.setDataset(dataset);
    plot.setRenderer(defaultRenderer.get());
    onDatasetChanged(dataset);
    if (dataset != null) {
      dataset.addChangeListener(event -> onDatasetChanged((XYZDataset) event.getSource()));
    }
    if (nextDataSetNum == 0) {
      nextDataSetNum++;
    }
    notifyDatasetsChangedListeners();
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
   * @param dataset
   * @param renderer
   * @return The dataset index.
   */
  public synchronized int addDataset(XYZDataset dataset, XYItemRenderer renderer) {
    plot.setDataset(nextDataSetNum, dataset);
    plot.setRenderer(nextDataSetNum, renderer);
    nextDataSetNum++;
    notifyDatasetsChangedListeners();
    return nextDataSetNum - 1;
  }

  /**
   * Adds a dataset with the default renderer.
   *
   * @param datasetProvider
   * @return The dataset index
   */
  public synchronized int addDataset(T datasetProvider) {
    if (datasetProvider instanceof XYZDataset) {
      return addDataset((XYZDataset) datasetProvider, plot.getRenderer());
    }
    ColoredXYZDataset dataset = new ColoredXYZDataset(datasetProvider);
    return addDataset(dataset, defaultRenderer.get());
  }

  public synchronized void removeAllDatasets() {
    chart.setNotify(false);
    for (int i = 0; i < nextDataSetNum; i++) {
      XYDataset ds = plot.getDataset(i);
      if (ds instanceof Task) {
        ((Task) ds).cancel();
      }
      plot.setDataset(i, null);
      plot.setRenderer(i, null);
    }
    chart.setNotify(true);
    chart.fireChartChanged();
    notifyDatasetsChangedListeners();
  }

  @Override
  public void switchLegendVisible() {
    // Toggle legend visibility.
    final LegendTitle legend = getChart().getLegend();
    legend.setVisible(!legend.isVisible());
  }

  @Override
  public void switchItemLabelsVisible() {
    // no items in standard xyz plot.
  }

  @Override
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
    final double domainValue = getXYPlot().getDomainCrosshairValue();
    final double rangeValue = getXYPlot().getRangeCrosshairValue();
    double zValue = PlotCursorPosition.DEFAULT_Z_VALUE;

    // mabye there is a more efficient way of searching for the selected value index.
    int index = -1;
    int datasetIndex = -1;
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset instanceof ColoredXYZDataset) {
        index = ((ColoredXYZDataset) dataset).getValueIndex(domainValue, rangeValue);
      }
      if (index != -1) {
        datasetIndex = i;
        zValue = ((ColoredXYZDataset) dataset).getZValue(0, index);
        break;
      }
    }

    return (index != -1) ?
        new PlotCursorPosition(domainValue, rangeValue, zValue, index,
            plot.getDataset(datasetIndex)) :
        new PlotCursorPosition(domainValue,
            rangeValue, index, null);
  }

  @Override
  public Plot getPlot() {
    return plot;
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
    onDatasetChanged((XYZDataset) getXYPlot().getDataset());
  }

  @Override
  public void addContextMenuItem(String title, EventHandler<ActionEvent> ai) {
    addMenuItem(getContextMenu(), title, ai);
  }


  @Override
  public void addDatasetsChangedListener(DatasetsChangedListener listener) {
    datasetListeners.add(listener);
  }

  @Override
  public void removeDatasetsChangedListener(DatasetsChangedListener listener) {
    datasetListeners.remove(listener);
  }

  @Override
  public void clearDatasetsChangedListeners(DatasetChangeListener listener) {
    datasetListeners.clear();
  }

  private void notifyDatasetsChangedListeners() {
    Map<Integer, XYDataset> datasets = getAllDatasets();

    for (DatasetsChangedListener listener : datasetListeners) {
      listener.datasetsChanged(datasets);
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
    if (!(dataset instanceof ColoredXYZDataset)
        || ((ColoredXYZDataset) dataset).getPaintScale() == null
        || ((ColoredXYZDataset) dataset).getStatus() != TaskStatus.FINISHED) {
      LookupPaintScale ps = new LookupPaintScale(0, 10000, Color.BLACK);
      return ps;
    } else {
      ColoredXYZDataset xyz = (ColoredXYZDataset) dataset;
      return xyz.getPaintScale();
    }
  }

  /**
   * @param dataset Called when the dataset is changed, e.g. when the calculation finished.
   */
  private void onDatasetChanged(XYZDataset dataset) {
    if (dataset == null) {
      return;
    }
    PaintScale paintScale = makePaintScale(dataset);
    updateRenderer(paintScale);
    if (dataset instanceof ColoredXYZDataset
        && ((ColoredXYZDataset) dataset).getStatus() == TaskStatus.FINISHED) {
      PaintScaleLegend legend = generateLegend(((ColoredXYZDataset) dataset).getMinZValue(),
          ((ColoredXYZDataset) dataset).getMaxZValue(), paintScale);
      chart.clearSubtitles();
      if (legendCanvas != null) {
        drawLegendToSeparateCanvas(legend);
      } else {
        chart.addSubtitle(legend);
      }
    }
    chart.fireChartChanged();
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
  }

  private void drawLegendToSeparateCanvas(PaintScaleLegend legend) {
    GraphicsContext gc = legendCanvas.getGraphicsContext2D();
    FXGraphics2D g2 = new FXGraphics2D(gc);
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
    final LinkedHashMap<Integer, XYDataset> datasetMap = new LinkedHashMap<Integer, XYDataset>();

    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset != null) {
        datasetMap.put(i, dataset);
      }
    }
    return datasetMap;
  }

  /**
   * @param min
   * @param max
   * @param scale
   * @return Legend based on the {@link LookupPaintScale}.
   */
  private PaintScaleLegend generateLegend(double min, double max, @Nonnull PaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    scaleAxis.setNumberFormatOverride(legendAxisFormat);
    PaintScaleLegend newLegend = new PaintScaleLegend(scale, scaleAxis);
    newLegend.setPadding(5, 0, 5, 0);
    newLegend.setStripOutlineVisible(false);
    newLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    newLegend.setAxisOffset(5.0);
    newLegend.setSubdivisionCount(500);
    newLegend.setPosition(defaultPaintscaleLocation);
    newLegend.getAxis().setLabelFont(legendFont);
    newLegend.getAxis().setTickLabelFont(legendFont);
//    double h =
//        newLegend.getHeight() + newLegend.getStripWidth() + newLegend.getAxisOffset() ;
    return newLegend;
  }

  /**
   * updates the renderer to the block sizes & paint scale provided by the dataset.
   *
   * @param paintScale
   */
  private void updateRenderer(PaintScale paintScale) {
    XYDataset dataset = plot.getDataset();
    if (!(dataset instanceof XYZDataset)) {
      // maybe add a case for that later
      return;
    }
    if (!(dataset instanceof ColoredXYZDataset)) {
      return;
    }
    if (((ColoredXYZDataset) dataset).getStatus() != TaskStatus.FINISHED) {
      return;
    }
    ColoredXYZDataset xyz = (ColoredXYZDataset) dataset;
    blockRenderer.setBlockHeight(xyz.getBoxHeight());
    blockRenderer.setBlockWidth(xyz.getBoxWidth());
    blockRenderer.setPaintScale(paintScale);
  }

  public RectangleEdge getDefaultPaintscaleLocation() {
    return defaultPaintscaleLocation;
  }

  public void setDefaultPaintscaleLocation(RectangleEdge defaultPaintscaleLocation) {
    this.defaultPaintscaleLocation = defaultPaintscaleLocation;
  }


}