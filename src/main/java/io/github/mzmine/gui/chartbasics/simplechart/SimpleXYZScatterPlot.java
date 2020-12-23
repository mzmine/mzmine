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
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
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
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * @author https://github.com/SteffenHeu & https://github.com/Annexhc
 */
public class SimpleXYZScatterPlot<T extends PlotXYZDataProvider> extends EChartViewer implements
    SimpleChart {

  protected static final Logger logger = Logger.getLogger(SimpleXYZScatterPlot.class.getName());

  protected static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
  protected final JFreeChart chart;
  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  protected ColoredXYSmallBlockRenderer blockRenderer;

  private final ObjectProperty<PlotCursorPosition> cursorPositionProperty;
  private final List<DatasetsChangedListener> datasetListeners;

  public SimpleXYZScatterPlot(@Nonnull String title) {

    super(ChartFactory.createScatterPlot("", "x", "y", null,
        PlotOrientation.VERTICAL, true, false, true), true, true, true, true, true);

    chart = getChart();
    chartTitle = new TextTitle(title);
    chart.setTitle(chartTitle);
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);
    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    blockRenderer = new ColoredXYSmallBlockRenderer();
    setCursor(Cursor.DEFAULT);

    cursorPositionProperty = new SimpleObjectProperty<>(new PlotCursorPosition(0, 0, -1, null));
    initializeMouseListener();
    datasetListeners = new ArrayList<>();

    plot.setRenderer(blockRenderer);
    initializePlot();

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
  }

  /**
   * @param dataset the dataset. null to clear the plot.
   */
  public void setDataset(@Nullable ColoredXYZDataset dataset) {
    plot.setDataset(dataset);
    onDatasetChanged(dataset);
    if (dataset != null) {
      dataset.addChangeListener(event -> onDatasetChanged((XYZDataset) event.getSource()));
    }
    notifyDatasetsChangedListeners();
  }

  /**
   * Creates a dataset and sets it as the chart's main data set.
   *
   * @param dataProvider The data provider
   */
  public void setDataset(T dataProvider) {
    ColoredXYZDataset dataset = new ColoredXYZDataset(dataProvider);
    setDataset(dataset);
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

  @Override
  public void addContextMenuItem(String title, EventHandler<ActionEvent> ai) {
    logger.info("call");
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
  private LookupPaintScale makePaintScale(XYZDataset dataset) {
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
    LookupPaintScale paintScale = makePaintScale(dataset);
    updateRenderer(paintScale);
    if (dataset instanceof ColoredXYZDataset
        && ((ColoredXYZDataset) dataset).getStatus() == TaskStatus.FINISHED) {
      PaintScaleLegend legend = generateLegend(((ColoredXYZDataset) dataset).getMinZValue(),
          ((ColoredXYZDataset) dataset).getMaxZValue(), paintScale);
      chart.clearSubtitles();
      chart.addSubtitle(legend);
    }
    chart.fireChartChanged();
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
  private PaintScaleLegend generateLegend(double min, double max, @Nonnull LookupPaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend newLegend = new PaintScaleLegend(scale, scaleAxis);
    newLegend.setPadding(5, 0, 5, 0);
    newLegend.setStripOutlineVisible(false);
    newLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    newLegend.setAxisOffset(5.0);
    newLegend.setSubdivisionCount(500);
    newLegend.setPosition(RectangleEdge.RIGHT);
    newLegend.getAxis().setLabelFont(legendFont);
    newLegend.getAxis().setTickLabelFont(legendFont);
    return newLegend;
  }

  /**
   * updates the renderer to the block sizes & paint scale provided by the dataset.
   *
   * @param paintScale
   */
  private void updateRenderer(LookupPaintScale paintScale) {
    XYDataset dataset = plot.getDataset();
    if (!(dataset instanceof XYZDataset)) {
      // maybe add a case for that later
      return;
    }
    if (!(dataset instanceof ColoredXYZDataset)) {
      return;
    }
    if(((ColoredXYZDataset) dataset).getStatus() != TaskStatus.FINISHED) {
      return;
    }
    ColoredXYZDataset xyz = (ColoredXYZDataset) dataset;
    blockRenderer.setBlockHeight(xyz.getBoxHeight());
    blockRenderer.setBlockWidth(xyz.getBoxWidth());
    blockRenderer.setPaintScale(paintScale);
  }

}
