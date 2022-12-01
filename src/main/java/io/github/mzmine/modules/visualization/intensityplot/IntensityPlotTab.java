/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.intensityplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;

/**
 *
 */
public class IntensityPlotTab extends MZmineTab {

  private static final Image pointsIcon = FxIconUtil.loadImageFromResources("icons/pointsicon.png");
  private static final Image linesIcon = FxIconUtil.loadImageFromResources("icons/linesicon.png");
  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");

  //private final Scene mainScene;
  private final BorderPane mainPane;

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private IntensityPlotDataset dataset;
  private JFreeChart chart;

  private FeatureList featureList;

  public IntensityPlotTab(ParameterSet parameters) {
    super("Intensity plot", true, false);

    mainPane = new BorderPane();

    featureList = parameters.getParameter(IntensityPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    String title = "Intensity plot [" + featureList + "]";
    String xAxisLabel = parameters.getParameter(IntensityPlotParameters.xAxisValueSource).getValue()
        .toString();
    String yAxisLabel = parameters.getParameter(IntensityPlotParameters.yAxisValueSource).getValue()
        .toString();

    // create dataset
    dataset = new IntensityPlotDataset(parameters);

    // create new JFreeChart
    logger.finest("Creating new chart instance");
    Object xAxisValueSource = parameters.getParameter(IntensityPlotParameters.xAxisValueSource)
        .getValue();
    boolean isCombo = (xAxisValueSource instanceof ParameterWrapper)
        && (!(((ParameterWrapper) xAxisValueSource).getParameter() instanceof DoubleParameter));
    if ((xAxisValueSource == IntensityPlotParameters.rawDataFilesOption) || isCombo) {

      chart = ChartFactory.createLineChart(title, xAxisLabel, yAxisLabel, dataset,
          PlotOrientation.VERTICAL, true, true, false);

      CategoryPlot plot = (CategoryPlot) chart.getPlot();

      // set renderer
      AbstractCategoryItemRenderer renderer = (AbstractCategoryItemRenderer) chart.getCategoryPlot()
          .getRenderer();
      renderer.setDefaultStroke(new BasicStroke(2));
      plot.setRenderer(renderer);
      plot.setBackgroundPaint(Color.white);

      // set tooltip generator
      CategoryToolTipGenerator toolTipGenerator = new IntensityPlotTooltipGenerator();
      renderer.setDefaultToolTipGenerator(toolTipGenerator);

      CategoryAxis xAxis = plot.getDomainAxis();
      xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

      EChartViewer chartPanel = new EChartViewer(chart);
      mainPane.setCenter(chartPanel);

    } else {

      chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset,
          PlotOrientation.VERTICAL, true, true, false);

      XYPlot plot = (XYPlot) chart.getPlot();

      XYErrorRenderer renderer = new XYErrorRenderer();
      renderer.setDefaultStroke(new BasicStroke(2));
      plot.setRenderer(renderer);
      plot.setBackgroundPaint(Color.white);

      // set tooltip generator
      XYToolTipGenerator toolTipGenerator = new IntensityPlotTooltipGenerator();
      renderer.setDefaultToolTipGenerator(toolTipGenerator);

      EChartViewer chartPanel = new EChartViewer(chart);
      mainPane.setCenter(chartPanel);

    }

    chart.setBackgroundPaint(Color.white);

    ToolBar toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);
    mainPane.setRight(toolBar);

    Button linesVisibleButton = new Button(null, new ImageView(linesIcon));
    linesVisibleButton.setTooltip(new Tooltip("Switch lines on/off"));
    linesVisibleButton.setOnAction(e -> {
      Plot plot = chart.getPlot();

      Boolean linesVisible;

      if (plot instanceof CategoryPlot) {
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) ((CategoryPlot) plot).getRenderer();
        linesVisible = renderer.getDefaultLinesVisible();
      } else {
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) ((XYPlot) plot).getRenderer();
        linesVisible = renderer.getDefaultLinesVisible();
        renderer.setDrawSeriesLineAsPath(true);
      }

      // check for null value
      if (linesVisible == null) {
        linesVisible = false;
      }

      // update the icon
      if (linesVisible) {
        linesVisibleButton.setGraphic(new ImageView(linesIcon));
      } else {
        linesVisibleButton.setGraphic(new ImageView(pointsIcon));
      }

      // switch the button
      linesVisible = !linesVisible;

      if (plot instanceof CategoryPlot) {
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) ((CategoryPlot) plot).getRenderer();
        renderer.setDefaultLinesVisible(linesVisible);
      } else {
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) ((XYPlot) plot).getRenderer();
        renderer.setDefaultLinesVisible(linesVisible);
        renderer.setDrawSeriesLineAsPath(true);
      }
    });

    if (chart.getPlot() instanceof XYPlot) {
      Button setupAxesButton = new Button(null, new ImageView(axesIcon));
      setupAxesButton.setTooltip(new Tooltip("Setup ranges for axes"));
      setupAxesButton.setOnAction(e -> {
        AxesSetupDialog dialog = new AxesSetupDialog(MZmineCore.getDesktop().getMainWindow(),
            chart.getXYPlot());
        dialog.show();
      });
      toolBar.getItems().add(setupAxesButton);
    }

    // set title properties
    TextTitle chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);

    LegendTitle legend = chart.getLegend();
    legend.setItemFont(legendFont);
    legend.setBorder(0, 0, 0, 0);

    Plot plot = chart.getPlot();

    // set shape provider
    IntensityPlotDrawingSupplier shapeSupplier = new IntensityPlotDrawingSupplier();
    plot.setDrawingSupplier(shapeSupplier);

    // set y axis properties
    NumberAxis yAxis;
    if (plot instanceof CategoryPlot) {
      yAxis = (NumberAxis) ((CategoryPlot) plot).getRangeAxis();
    } else {
      yAxis = (NumberAxis) ((XYPlot) plot).getRangeAxis();
    }
    NumberFormat yAxisFormat = MZmineCore.getConfiguration().getIntensityFormat();
    if (parameters.getParameter(IntensityPlotParameters.yAxisValueSource).getValue()
        == YAxisValueSource.RT) {
      yAxisFormat = MZmineCore.getConfiguration().getRTFormat();
    }
    yAxis.setNumberFormatOverride(yAxisFormat);

    setContent(mainPane);
  }

  JFreeChart getChart() {
    return chart;
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return featureList.getRawDataFiles();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return new ArrayList<>(Collections.singletonList((ModularFeatureList) featureList));
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
