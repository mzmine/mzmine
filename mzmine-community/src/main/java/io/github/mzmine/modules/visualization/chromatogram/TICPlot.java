/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.ChartLogics;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartthemes.LabelColorMatch;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;

/**
 * TIC plot.
 * <p>
 * Added the possibility to switch to TIC plot type from a "non-TICVisualizerWindow" context.
 */
public class TICPlot extends EChartViewer implements LabelColorMatch {

  private static final Logger logger = Logger.getLogger(TICPlot.class.getName());

  private static final Shape DATA_POINT_SHAPE = new Ellipse2D.Double(-2.0, -2.0, 5.0, 5.0);
  private static final double AXIS_MARGINS = 0.001;

  protected final JFreeChart chart;
  private final XYPlot plot;
  private final ObjectProperty<ChromatogramCursorPosition> cursorPosition;
  private final BooleanProperty matchLabelColors;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  private final TICPlotRenderer defaultRenderer;
  // properties
  private final ObjectProperty<TICPlotType> plotType;
  protected EStandardChartTheme theme;

  /**
   * Indicates whether we have a request to show spectra visualizer for selected data point. Since
   * the selection (cross-hair) is updated with some delay after clicking with mouse, we cannot open
   * the new visualizer immediately. Therefore we place a request and open the visualizer later in
   * chartProgress()
   */
  private boolean showSpectrumRequest;

  // Label visibility: 0 -> none; 1 -> m/z; 2 -> identities

  private int labelsVisible;
  private boolean havePeakLabels;

  public TICPlot() {

    super(ChartFactory.createXYLineChart("", // title
        "Retention time", // x-axis label
        "Y", // y-axis label
        null, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // create legend?
        true, // generate tooltips?
        false // generate URLs?
    ));

    theme = MZmineCore.getConfiguration().getDefaultChartTheme();

    // Initialize.
    labelsVisible = 1;
    havePeakLabels = false;
    showSpectrumRequest = false;

    setMinWidth(300.0);
    setMinHeight(100);

    setPrefWidth(600.0);
    setPrefHeight(400.0);

    // Plot type
    plotType = new SimpleObjectProperty<>();
    cursorPosition = new SimpleObjectProperty<>();
    matchLabelColors = new SimpleBooleanProperty(false);
    initializeChromatogramMouseListener();
    addMatchLabelColorsListener();

    // Y-axis label.
    final String yAxisLabel =
        (getPlotType() == TICPlotType.BASEPEAK) ? "Base peak intensity" : "Total ion intensity";

    // Initialize the chart by default time series chart from factory.
    chart = getChart();
    chart.getXYPlot().getRangeAxis().setLabel(yAxisLabel);

    // only allow positive values for the axes
    ChartLogics.setAxesTypesPositive(chart);

    // setChart(chart);

    // Title.
    chartTitle = chart.getTitle();

    // Subtitle.
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);

    // Disable maximum size (we don't want scaling).
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);

    // Set the plot properties.
    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // Set cross-hair (selection) properties.
    // if (listener instanceof TICVisualizerWindow) {

    // Set cursor.
    setCursor(Cursor.CROSSHAIR);

    setPlotType(TICPlotType.BASEPEAK);

    // }

    // Set the x-axis (retention time) properties.
    final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    xAxis.setUpperMargin(AXIS_MARGINS);
    xAxis.setLowerMargin(AXIS_MARGINS);

    // Set the y-axis (intensity) properties.
    final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());

    // Set default renderer properties.
    defaultRenderer = new TICPlotRenderer();
    defaultRenderer.setDefaultShapesFilled(true);
    defaultRenderer.setDrawOutlines(false);
    defaultRenderer.setUseFillPaint(true);

    // Set label generator
    final XYItemLabelGenerator labelGenerator = new TICItemLabelGenerator(this);
    defaultRenderer.setDefaultItemLabelGenerator(labelGenerator);
    defaultRenderer.setDefaultItemLabelsVisible(true);

    // Set toolTipGenerator
    final XYToolTipGenerator toolTipGenerator = new TICToolTipGenerator();
    defaultRenderer.setDefaultToolTipGenerator(toolTipGenerator);

    // Set focus state to receive key events.
    // setFocusable(true);
    // Register for mouse-wheel events
    // addMouseWheelListener(this);

    chart.addProgressListener(event -> {
      if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {

        Scene myScene = this.getScene();
        if (myScene == null) {
          return;
        }

        if (showSpectrumRequest) {
          showSpectrumRequest = false;
        }
      }
    });

    ZoomHistory history = getZoomHistory();
    if (history != null) {
      history.clear();
    }
  }

  public int getDatasetCount() {
    return plot.getDatasetCount();
  }


  public void setLegendVisible(boolean state) {
    final LegendTitle legend = getChart().getLegend();
    legend.setVisible(state);
    theme.setShowLegend(state);
    //theme.apply paints first series in white/black which is wrong
  }

  public void switchLegendVisible() {
    setLegendVisible(!theme.isShowLegend());
  }

  public void switchItemLabelsVisible() {
    // Switch to next mode. Include peaks mode only if peak labels are
    // present.
    labelsVisible = (labelsVisible + 1) % (havePeakLabels ? 3 : 2);

    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {

      final XYDataset dataSet = plot.getDataset(i);
      final XYItemRenderer renderer = plot.getRenderer(i);
      if (dataSet instanceof TICDataSet) {
        renderer.setDefaultItemLabelsVisible(labelsVisible == 1, false);
      } else if (dataSet instanceof FeatureDataSet) {
        renderer.setDefaultItemLabelsVisible(labelsVisible == 2, false);
      } else {
        renderer.setDefaultItemLabelsVisible(false, false);
      }
    }
    if (isNotifyChange()) {
      fireChangeEvent();
    }
  }

  public void switchDataPointsVisible() {
    applyWithNotifyChanges(false, () -> {

      Boolean dataPointsVisible = null;
      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int i = 0; i < numDatasets; i++) {

        if (plot.getRenderer(i) instanceof final XYLineAndShapeRenderer renderer) {

          if (dataPointsVisible == null) {
            dataPointsVisible = !renderer.getDefaultShapesVisible();
          }
          renderer.setDefaultShapesVisible(dataPointsVisible);
        }
      }
    });
  }

  public void switchBackground() {
    // Toggle background color
    final Paint color = getChart().getPlot().getBackgroundPaint();
    Color bgColor, liColor;
    if (color.equals(Color.lightGray)) {
      bgColor = Color.white;
      liColor = Color.lightGray;
    } else {
      bgColor = Color.lightGray;
      liColor = Color.white;
    }
    applyWithNotifyChanges(false, () -> {
      getChart().getPlot().setBackgroundPaint(bgColor);
      getChart().getXYPlot().setDomainGridlinePaint(liColor);
      getChart().getXYPlot().setRangeGridlinePaint(liColor);
    });
  }

  public XYPlot getXYPlot() {
    return plot;
  }


  public synchronized void addDataSets(Collection<? extends XYDataset> dataSets) {
    final boolean oldNotify = plot.isNotify();
    plot.setNotify(false);
    dataSets.forEach(this::addDataSet);
    plot.setNotify(oldNotify);
    if (oldNotify) {
      chart.fireChartChanged();
    }
  }

  public synchronized int addDataSet(final XYDataset dataSet) {
    if ((dataSet instanceof TICDataSet) && (((TICDataSet) dataSet).getPlotType()
                                            != getPlotType())) {
      throw new IllegalArgumentException("Added dataset of class '" + dataSet.getClass()
                                         + "' does not have a compatible plotType. Expected '"
                                         + this.getPlotType().toString() + "'");
    }

    try {
      final XYItemRenderer renderer;
      if (dataSet instanceof MzRangeEicDataSet mzdataset) {
        renderer = new FeatureTICRenderer();
        renderer.setSeriesShape(0, DATA_POINT_SHAPE);
        renderer.setDefaultItemLabelsVisible(labelsVisible == 1);
        Color color = mzdataset.getAWTColor();
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesFillPaint(0, color);
      } else {
        renderer = (TICPlotRenderer) defaultRenderer.clone();
        renderer.setSeriesShape(0, DATA_POINT_SHAPE);
        renderer.setDefaultItemLabelsVisible(labelsVisible == 1);
        renderer.setSeriesPaint(0, plot.getDrawingSupplier().getNextPaint());
        renderer.setSeriesFillPaint(0, plot.getDrawingSupplier().getNextFillPaint());
      }
      return addDataSetAndRenderer(dataSet, renderer);
    } catch (CloneNotSupportedException e) {
      logger.log(Level.WARNING, "Unable to clone renderer", e);
    }
    return -1;
  }

  public synchronized int addDataSet(XYDataset dataSet, Color color) {
    XYItemRenderer newRenderer = new DefaultXYItemRenderer();
    newRenderer.setDefaultFillPaint(color);
    return addDataSetAndRenderer(dataSet, newRenderer);
  }

  public synchronized int addTICDataSet(final TICDataSet dataSet, Color color) {
    return addTICDataSet(dataSet, color, color);
  }

  public synchronized int addTICDataSet(final TICDataSet dataSet, Color lineColor,
      Color fillColor) {
    try {
      final TICPlotRenderer renderer = (TICPlotRenderer) defaultRenderer.clone();
      renderer.setSeriesPaint(0, lineColor);
      renderer.setSeriesFillPaint(0, fillColor);
      renderer.setSeriesShape(0, DATA_POINT_SHAPE);
      renderer.setDefaultItemLabelsVisible(labelsVisible == 1);
      return addTICDataSet(dataSet, renderer);
    } catch (CloneNotSupportedException e) {
      logger.log(Level.WARNING, "Unable to clone renderer", e);
    }
    return -1;
  }

  /**
   * Adds a data set with the color specified in the color associated with the raw data file. If a
   * specific color needs to be added, use {@link TICPlot#addTICDataSet(TICDataSet, Color)} or
   * {@link TICPlot#addTICDataSet(TICDataSet, Color, Color)}
   */
  public synchronized int addTICDataSet(final TICDataSet dataSet) {
    Color clr = null;
    if (dataSet.getDataFile() != null) {
      clr = dataSet.getDataFile().getColorAWT();
    }

    try {
      final TICPlotRenderer renderer = (TICPlotRenderer) defaultRenderer.clone();
      if (clr != null) {
        renderer.setSeriesPaint(0, clr);
        renderer.setSeriesFillPaint(0, clr);
      } else {
        renderer.setSeriesPaint(0, plot.getDrawingSupplier().getNextPaint());
        renderer.setSeriesFillPaint(0, plot.getDrawingSupplier().getNextFillPaint());
      }
      renderer.setSeriesShape(0, DATA_POINT_SHAPE);
      renderer.setDefaultItemLabelsVisible(labelsVisible == 1);
      return addTICDataSet(dataSet, renderer);
    } catch (CloneNotSupportedException e) {
      logger.log(Level.WARNING, "Unable to clone renderer", e);
    }
    return -1;
  }

  public synchronized int addTICDataSet(final TICDataSet dataSet, TICPlotRenderer renderer) {
    // Check if the dataSet to be added is compatible with the type of plot.
    if (dataSet.getPlotType() != getPlotType()) {
      throw new IllegalArgumentException("Added dataset of class '" + dataSet.getClass()
                                         + "' does not have a compatible plotType. Expected '"
                                         + this.getPlotType().toString() + "'");
    }
    return addDataSetAndRenderer(dataSet, renderer);
  }

  /**
   * Adds multiple data sets at once. Only triggers {@link JFreeChart#fireChartChanged()} once to
   * save performance.
   */
  public synchronized void addTICDataSets(final Collection<TICDataSet> dataSets) {
    plot.setNotify(false);
    dataSets.forEach(ds -> addTICDataSet(ds));
    plot.setNotify(true);
    chart.fireChartChanged();
  }

  /**
   * Adds a {@link FeatureDataSet} with in the color linked to the feature's raw data file to the
   * plot.
   */
  public synchronized int addFeatureDataSet(final FeatureDataSet dataSet) {
    final FeatureTICRenderer renderer = new FeatureTICRenderer();
    if (dataSet.getFeature() != null && dataSet.getFeature().getRawDataFile() != null
        && dataSet.getFeature().getRawDataFile().getColor() != null) {
      Color clr = dataSet.getFeature().getRawDataFile().getColorAWT();
      renderer.setSeriesPaint(0, clr);
      renderer.setSeriesFillPaint(0, clr);
    } else {
      renderer.setSeriesPaint(0, plot.getDrawingSupplier().getNextPaint());
      renderer.setSeriesFillPaint(0, plot.getDrawingSupplier().getNextFillPaint());
    }
    renderer.setDefaultToolTipGenerator(new TICToolTipGenerator());
    return addDataSetAndRenderer(dataSet, renderer);
  }

  public synchronized void addFeatureDataSets(Collection<FeatureDataSet> dataSets) {
    final boolean oldNotify = plot.isNotify();
    plot.setNotify(false);
    dataSets.forEach(this::addFeatureDataSet);
    plot.setNotify(oldNotify);
    if (oldNotify) {
      chart.fireChartChanged();
    }
  }

  public synchronized void addFeatureDataSet(final FeatureDataSet dataSet,
      final FeatureTICRenderer renderer) {
    addDataSetAndRenderer(dataSet, renderer);
  }

  public synchronized void addLabelledPeakDataSet(final FeatureDataSet dataSet,
      final String label) {
    // Add standard peak data set.
    addFeatureDataSet(dataSet);

    // Do we have a label?
    if (label != null && label.length() > 0) {
      // Add peak label renderer and data set.
      final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, false);
      renderer.setDefaultItemLabelsVisible(labelsVisible == 2);
      renderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());
      addDataSetAndRenderer(dataSet, renderer);
      renderer.setDrawSeriesLineAsPath(true);
      renderer.setDefaultItemLabelGenerator(
          (xyDataSet, series, item) -> ((FeatureDataSet) xyDataSet).isFeature(item) ? label : null);

      havePeakLabels = true;
    }
  }

  public synchronized XYDataset removeDataSet(int index) {
    XYDataset ds = plot.getDataset(index);
    plot.setDataset(index, null);
    plot.setRenderer(index, null);
    return ds;
  }

  /**
   * @param file   The raw data file
   * @param notify If false, the plot is not redrawn. This is useful, if multiple data sets are
   *               added right after and the plot shall not be updated until then.
   */
  public synchronized void removeFeatureDataSetsOfFile(final RawDataFile file, boolean notify) {
    plot.setNotify(false);
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset ds = plot.getDataset(i);
      if (ds instanceof FeatureDataSet pds) {
        if (pds.getFeature().getRawDataFile() == file) {
          plot.setDataset(getXYPlot().indexOf(pds), null);
          plot.setRenderer(getXYPlot().indexOf(pds), null);
        }
      }
    }
    plot.setNotify(true);
    if (notify) {
      chart.fireChartChanged();
    }
  }

  /**
   * @param file The raw data file and notifies the plot.
   */
  public synchronized void removeFeatureDataSetsOfFile(final RawDataFile file) {
    removeFeatureDataSetsOfFile(file, true);
  }

  /**
   * Removes all feature data sets.
   *
   * @param notify If false, the plot is not redrawn. This is useful, if multiple data sets are
   *               added right after and the plot shall not be updated until then.
   */
  public synchronized void removeAllFeatureDataSets(boolean notify) {
    removeAllDataSetsOf(FeatureDataSet.class, notify);
  }

  /**
   * Removes all feature data sets.
   *
   * @param notify If false, the plot is not redrawn. This is useful, if multiple data sets are
   *               added right after and the plot shall not be updated until then.
   */
  public synchronized void removeAllDataSetsOf(Class<? extends XYDataset> clazz, boolean notify) {
    JFreeChartUtils.removeAllDataSetsOf(chart, clazz, notify);
  }

  /**
   * Removes all feature data sets and notifies the plot.
   */
  public synchronized void removeAllFeatureDataSets() {
    removeAllFeatureDataSets(true);
  }

  /**
   * Removes all data sets.
   *
   * @param notify If false, the plot is not redrawn. This is useful, if multiple data sets are
   *               added right after and the plot shall not be updated until then.
   */
  public synchronized void removeAllDataSets(boolean notify) {
    plot.setNotify(false);
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int index = 0; index < numDatasets; index++) {
      plot.setDataset(index, null);
    }
    plot.setNotify(true);
    if (notify) {
      chart.fireChartChanged();
    }
  }

  /**
   * Removes all data sets and notifies the plot.
   */
  public void removeAllDataSets() {
    removeAllDataSets(true);
  }

  public synchronized void removeDatasets(Collection<Integer> indices) {
    plot.setNotify(false);
    for (Integer index : indices) {
      removeDataSet(index);
    }
    plot.setNotify(true);
    chart.fireChartChanged();
  }

  public void setTitle(final String titleText, final String subTitleText) {
    chartTitle.setText(titleText);
    chartSubTitle.setText(subTitleText);
  }

  @NotNull
  public ObjectProperty<TICPlotType> plotTypeProperty() {
    return plotType;
  }

  public TICPlotType getPlotType() {
    return plotType.get();
  }

  public void setPlotType(final TICPlotType plotType) {

    if (getPlotType() == plotType) {
      return;
    }
    /*
     * // Plot type if (visualizer instanceof TICVisualizerWindow) { this.plotType =
     * ((TICVisualizerWindow) visualizer).getPlotType(); } else { }
     */

    plotTypeProperty().set(plotType);
    // Y-axis label.
    String yAxisLabel =
        (getPlotType() == TICPlotType.BASEPEAK) ? "Base peak intensity" : "Total ion intensity";
    getXYPlot().getRangeAxis().setLabel(yAxisLabel);
  }

  public synchronized int addDataSetAndRenderer(final XYDataset dataSet,
      final XYItemRenderer renderer) {
    int nextDatasetId = JFreeChartUtils.getNextDatasetIndex(plot);

    applyWithNotifyChanges(false, () -> {
      if (dataSet instanceof TICDataSet) {
        renderer.setDefaultItemLabelPaint(((TICDataSet) dataSet).getDataFile().getColorAWT());
      } else if (dataSet instanceof FeatureDataSet) {
        renderer.setDefaultItemLabelPaint(
            ((FeatureDataSet) dataSet).getFeature().getRawDataFile().getColorAWT());
      }

      plot.setRenderer(nextDatasetId, renderer, false); // notify on dataset change
      plot.setDataset(nextDatasetId, dataSet);
    });

    return nextDatasetId;
  }


  @Override
  public void setLabelColorMatch(boolean matchColor) {
    matchLabelColors.set(matchColor);
  }

  private void addMatchLabelColorsListener() {
    matchLabelColors.addListener(
        ((observable, oldValue, newValue) -> applyWithNotifyChanges(false, () -> {
          if (newValue) {
            int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
            for (int i = 0; i <numDatasets; i++) {
              XYDataset dataset = getXYPlot().getDataset();
              if (dataset == null) {
                continue;
              }
              XYItemRenderer renderer = getXYPlot().getRendererForDataset(dataset);
              if (dataset instanceof TICDataSet) {
                renderer.setDefaultItemLabelPaint(
                    ((TICDataSet) dataset).getDataFile().getColorAWT());
              } else if (dataset instanceof FeatureDataSet) {
                renderer.setDefaultItemLabelPaint(
                    ((FeatureDataSet) dataset).getFeature().getRawDataFile().getColorAWT());
              }
            }
          } else {
            int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
            for (int i = 0; i < numDatasets; i++) {
              XYDataset dataset = getXYPlot().getDataset();
              if (dataset == null) {
                continue;
              }
              XYItemRenderer renderer = getXYPlot().getRendererForDataset(dataset);
              renderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());
            }
          }
        })));
  }

  public ChromatogramCursorPosition getCursorPosition() {
    return cursorPosition.get();
  }

  public void setCursorPosition(ChromatogramCursorPosition cursorPosition) {
    this.cursorPosition.set(cursorPosition);
  }

  public ObjectProperty<ChromatogramCursorPosition> cursorPositionProperty() {
    return cursorPosition;
  }

  /**
   * Listens to clicks in the chromatogram plot and updates the selected raw data file accordingly.
   */
  private void initializeChromatogramMouseListener() {
    getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA, Event.CLICK, GestureButton.BUTTON1), e -> {
      ChromatogramCursorPosition pos = getCurrentCursorPosition();
      if (pos != null) {
        setCursorPosition(pos);
      }
    }));
  }

  /**
   * @return current cursor position or null
   */
  @Nullable
  private ChromatogramCursorPosition getCurrentCursorPosition() {
    double selectedRT = getXYPlot().getDomainCrosshairValue();
    double selectedIT = getXYPlot().getRangeCrosshairValue();
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset ds = getXYPlot().getDataset(i);
      if (!(ds instanceof TICDataSet dataSet)) {
        continue;
      }
      int index = dataSet.getIndex(selectedRT, selectedIT);
      if (index >= 0) {
        double mz = 0;
        if (getPlotType() == TICPlotType.BASEPEAK) {
          mz = dataSet.getZValue(0, index);
        }
        return new ChromatogramCursorPosition(selectedRT, mz, selectedIT, dataSet.getDataFile(),
            dataSet.getScan(index));
      }
    }
    return null;
  }
}
