/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.HistogramChartFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.color.Colors;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.InterSampleRtStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataParameterEstimation;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Dashboard that shows all {@link DataFileStatistics} plots in a grid. Supports three display
 * modes: single file, overlay (all files with distinct colors), and accumulated (merged data).
 */
public class DataFileStatisticsDashboardPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(
      DataFileStatisticsDashboardPane.class.getName());

  private static final int GRID_COLUMNS = 2;

  private static final Color MEAN_COLOR = ConfigService.getDefaultColorPalette()
      .getNeutralColorAWT();
  private static final Color MEDIAN_COLOR = ConfigService.getDefaultColorPalette()
      .getNegativeColorAWT();
  private static final Color SIGMA_1_COLOR = Colors.MAGENTA;
  private static final Color SIGMA_2_COLOR = SIGMA_1_COLOR.brighter();
  private static final Color SIGMA_3_COLOR = SIGMA_2_COLOR.brighter();
  private static final Color RT_DISTRIBUTION_COLOR = new Color(70, 120, 200);
  private static final Color RT_BOUND_COLOR = new Color(190, 50, 50);
  private static final Color RAW_DATA_ESTIMATE_COLOR = ConfigService.getDefaultColorPalette()
      .getPositiveColorAWT();
  private static final BasicStroke SOLID_STROKE = new BasicStroke(1.5f);
  private static final BasicStroke DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER, 1, new float[]{5f, 3f}, 0);
  private static Color TRANSPARENT = new Color(0, 0, 0, 0);

  private final List<DataFileStatistics> statistics;
  private final Map<DataFileStatistics, Color> fileColors;
  private final @NotNull InterSampleRtStatistics interSampleRtStatistics;
  private final @NotNull MassDetectorWizardOptions massDetectorType;

  private final ComboBox<StatisticsDisplayMode> modeCombo = new ComboBox<>();
  private final ComboBox<DataFileStatistics> fileCombo = new ComboBox<>();
  private final ObservableList<DataFileStatistics> fileItems = FXCollections.observableArrayList();
  private final GridPane chartGrid = new GridPane();

  public DataFileStatisticsDashboardPane(@NotNull List<DataFileStatistics> statistics,
      @NotNull InterSampleRtStatistics interSampleRtStatistics) {
    this(statistics, interSampleRtStatistics,
        RawDataParameterEstimation.inferMassDetectorType(statistics));
  }

  public DataFileStatisticsDashboardPane(@NotNull List<DataFileStatistics> statistics,
      @NotNull InterSampleRtStatistics interSampleRtStatistics,
      @NotNull MassDetectorWizardOptions massDetectorType) {
    this.statistics = List.copyOf(statistics);
    this.fileColors = assignColors(this.statistics);
    this.interSampleRtStatistics = interSampleRtStatistics;
    this.massDetectorType = massDetectorType;

    initControls();
    initGrid();

    final Label fileLabel = FxLayout.bindManagedToVisible(new Label("File:"));
    fileLabel.visibleProperty().bind(fileCombo.visibleProperty());
    final FlowPane controls = new FlowPane(8, 8, new Label("Display:"), modeCombo, fileLabel,
        FxLayout.bindManagedToVisible(fileCombo));

    final ScrollPane scroll = new ScrollPane(chartGrid);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);

    setTop(controls);
    setCenter(scroll);

    // initial render
    rebuildAllCharts();
  }

  private void initControls() {
    modeCombo.setItems(FXCollections.observableArrayList(StatisticsDisplayMode.values()));
    modeCombo.setValue(
        statistics.size() > 1 ? StatisticsDisplayMode.OVERLAY : StatisticsDisplayMode.SINGLE_FILE);
    modeCombo.setOnAction(_ -> {
      final boolean singleFile = modeCombo.getValue() == StatisticsDisplayMode.SINGLE_FILE;
      fileCombo.setVisible(singleFile);
      rebuildAllCharts();
    });

    fileItems.setAll(statistics);
    fileCombo.setItems(fileItems);
    fileCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(@Nullable DataFileStatistics stat) {
        return stat != null ? stat.file().getName() : "";
      }

      @Override
      public @Nullable DataFileStatistics fromString(String string) {
        return null;
      }
    });
    if (!statistics.isEmpty()) {
      fileCombo.setValue(statistics.getFirst());
    }
    fileCombo.setVisible(modeCombo.getValue() == StatisticsDisplayMode.SINGLE_FILE);
    fileCombo.setOnAction(_ -> rebuildAllCharts());
  }

  private void initGrid() {
    chartGrid.setHgap(8);
    chartGrid.setVgap(8);
    chartGrid.setPadding(new Insets(8));

    for (int col = 0; col < GRID_COLUMNS; col++) {
      final ColumnConstraints cc = new ColumnConstraints();
      cc.setPercentWidth(100.0 / GRID_COLUMNS);
      cc.setHgrow(Priority.ALWAYS);
      chartGrid.getColumnConstraints().add(cc);
    }
  }

  private void rebuildAllCharts() {
    chartGrid.getChildren().clear();
    chartGrid.getRowConstraints().clear();

    final StatisticsDisplayMode mode = modeCombo.getValue();
    final StatisticsPlotType[] types = StatisticsPlotType.values();

    int col = 0;
    int row = 0;
    for (StatisticsPlotType type : types) {
      final Node chart;
      if (type.isHistogram()) {
        chart = createHistogramChart(type, mode);
      } else {
        chart = createToleranceBarChart(mode);
      }

      if (chart != null) {
        GridPane.setHgrow(chart, Priority.ALWAYS);
        GridPane.setVgrow(chart, Priority.ALWAYS);
        chartGrid.add(chart, col, row);
      }

      col++;
      if (col >= GRID_COLUMNS) {
        col = 0;
        row++;
      }
    }

    final Node rtToleranceChart = createRtToleranceChart();
    if (rtToleranceChart != null) {
      GridPane.setHgrow(rtToleranceChart, Priority.ALWAYS);
      GridPane.setVgrow(rtToleranceChart, Priority.ALWAYS);
      chartGrid.add(rtToleranceChart, col, row);
      col++;
      if (col >= GRID_COLUMNS) {
        col = 0;
        row++;
      }
    }

    // add row constraints for equal height
    final int totalRows = row + (col > 0 ? 1 : 0);
    for (int r = 0; r < totalRows; r++) {
      final RowConstraints rc = new RowConstraints();
      rc.setPercentHeight(100.0 / totalRows);
      rc.setVgrow(Priority.ALWAYS);
      chartGrid.getRowConstraints().add(rc);
    }
  }

  private @Nullable Node createHistogramChart(@NotNull StatisticsPlotType type,
      @NotNull StatisticsDisplayMode mode) {
    final List<DataFileStatistics> filesToPlot = getFilesForMode(mode);
    if (filesToPlot.isEmpty()) {
      return null;
    }

    // collect all data arrays to compute global bounds
    final List<double[]> allData = new ArrayList<>();
    for (DataFileStatistics stat : filesToPlot) {
      final double[] data = type.extractData(stat);
      if (data.length > 0) {
        allData.add(data);
      }
    }
    if (allData.isEmpty()) {
      return null;
    }

    double globalMin = Double.MAX_VALUE;
    double globalMax = -Double.MAX_VALUE;
    for (double[] data : allData) {
      globalMin = Math.min(globalMin, HistogramChartFactory.getMin(data));
      globalMax = Math.max(globalMax, HistogramChartFactory.getMax(data));
    }

    // decision: auto bin width when defaultBinWidth < 0
    final double binWidth;
    if (type.defaultBinWidth() > 0) {
      binWidth = type.defaultBinWidth();
    } else {
      final double range = globalMax - globalMin;
      binWidth = range > 0 ? range / 30.0 : 1.0;
    }

    if (mode == StatisticsDisplayMode.ACCUMULATED) {
      // merge all arrays into one
      final double[] merged = allData.stream().flatMapToDouble(DoubleStream::of).toArray();
      final XYSeries series = HistogramChartFactory.createHistoSeries(merged, binWidth, globalMin,
          globalMax, null);
      series.setKey("All files");
      final JFreeChart chart = HistogramChartFactory.createHistogram(series, binWidth,
          type.yAxisLabel());
      chart.setTitle(type.label());
      addMarkers(chart, merged, type, massDetectorType);
      if (type.hasGaussianFit()) {
        addGaussianFit(chart, merged, binWidth, 1);
      }
      return new EChartViewer(chart, true, true, true, true, true);
    }

    // SINGLE_FILE or OVERLAY: one series per file
    final XYSeriesCollection collection = new XYSeriesCollection();
    final List<Color> seriesColors = new ArrayList<>();

    for (int i = 0; i < filesToPlot.size(); i++) {
      final DataFileStatistics stat = filesToPlot.get(i);
      final double[] data = type.extractData(stat);
      if (data.length == 0) {
        continue;
      }
      final XYSeries series = HistogramChartFactory.createHistoSeries(data, binWidth, globalMin,
          globalMax, null);
      series.setKey(stat.file().getName());
      collection.addSeries(series);
      seriesColors.add(fileColors.getOrDefault(stat, Color.BLACK));
    }

    if (collection.getSeriesCount() == 0) {
      return null;
    }

    final XYBarDataset barDataset = new XYBarDataset(collection, binWidth);
    final JFreeChart chart = FxChartFactory.createXYBarChart(type.label(), type.xAxisLabel(), false,
        type.yAxisLabel(), barDataset, PlotOrientation.VERTICAL, seriesColors.size() > 1, true,
        false);
    styleXYBarChart(chart, seriesColors);

    // markers and optional Gaussian fit on the combined data of all plotted files
    // in overlay mode, divide Gaussian amplitude by file count so it matches per-file bar heights
    final double[] combinedData = allData.stream().flatMapToDouble(DoubleStream::of).toArray();
    addMarkers(chart, combinedData, type, massDetectorType);
    if (type.hasGaussianFit()) {
      addGaussianFit(chart, combinedData, binWidth, allData.size());
    }

    return new EChartViewer(chart, true, true, true, true, true);
  }

  private @Nullable Node createToleranceBarChart(@NotNull StatisticsDisplayMode mode) {
    final List<DataFileStatistics> filesToPlot = getFilesForMode(mode);
    if (filesToPlot.isEmpty()) {
      return null;
    }

    final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    if (mode == StatisticsDisplayMode.ACCUMULATED) {
      // sum counts across all files
      final Map<MZTolerance, Integer> totalCounts = new LinkedHashMap<>();
      for (DataFileStatistics stat : filesToPlot) {
        stat.extractToleranceCounts()
            .forEach((key, count) -> totalCounts.merge(key, count, Integer::sum));
      }
      for (Map.Entry<MZTolerance, Integer> entry : totalCounts.entrySet()) {
        dataset.addValue(entry.getValue(), "All files", entry.getKey().toString());
      }
    } else {
      // SINGLE_FILE or OVERLAY: one series per file
      for (DataFileStatistics stat : filesToPlot) {
        final String seriesKey = stat.file().getName();
        final Map<MZTolerance, Integer> counts = stat.extractToleranceCounts();
        for (Map.Entry<MZTolerance, Integer> entry : counts.entrySet()) {
          dataset.addValue(entry.getValue(), seriesKey, entry.getKey().toString());
        }
      }
    }

    // assumption: FxChartFactory has no category bar chart, so using ChartFactory directly
    final JFreeChart chart = ChartFactory.createBarChart(
        StatisticsPlotType.BEST_TOLERANCE_FREQUENCY.label(),
        StatisticsPlotType.BEST_TOLERANCE_FREQUENCY.xAxisLabel(),
        StatisticsPlotType.BEST_TOLERANCE_FREQUENCY.yAxisLabel(), dataset, PlotOrientation.VERTICAL,
        filesToPlot.size() > 1, true, false);
    chart.setBackgroundPaint(new Color(230, 230, 230));

    final MZTolerance estimatedTolerance = RawDataParameterEstimation.estimateMzTolerance(
        filesToPlot);
    final CategoryMarker estimateMarker = new CategoryMarker(estimatedTolerance.toString(),
        new Color(RAW_DATA_ESTIMATE_COLOR.getRed() / 255f,
            RAW_DATA_ESTIMATE_COLOR.getGreen() / 255f, RAW_DATA_ESTIMATE_COLOR.getBlue() / 255f,
            0.2f), new BasicStroke(2f));
//    estimateMarker.setDrawAsLine(true);
    estimateMarker.setLabel("Raw-data m/z tolerance estimate: " + estimatedTolerance);
    estimateMarker.setLabelBackgroundColor(
        ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : TRANSPARENT);
    chart.getCategoryPlot().addDomainMarker(estimateMarker);

    // apply file colors if overlay
    if (mode != StatisticsDisplayMode.ACCUMULATED && filesToPlot.size() > 1) {
      final var renderer = chart.getCategoryPlot().getRenderer();
      for (int i = 0; i < filesToPlot.size(); i++) {
        final Color color = fileColors.getOrDefault(filesToPlot.get(i), Color.BLACK);
        renderer.setSeriesPaint(i,
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
      }
    }

    return new EChartViewer(chart, true, true, true, true, true);
  }

  /**
   * Shows the cross-file RT deviations used to derive the initial tolerance and its search bounds.
   * This statistic necessarily combines files, so it is unchanged by the per-file display mode.
   */
  private @Nullable Node createRtToleranceChart() {
    if (interSampleRtStatistics.isEmpty()) {
      return null;
    }

    final double[] deviations = interSampleRtStatistics.deviations();
    final double observedMin = HistogramChartFactory.getMin(deviations);
    final double observedMax = HistogramChartFactory.getMax(deviations);
    final double observedRange = observedMax - observedMin;
    final double binWidth = observedRange > 0d ? observedRange / 30d : 0.001d;
    final XYSeries series = HistogramChartFactory.createHistoSeries(deviations, binWidth,
        observedMin, observedMax, null);
    series.setKey("All files");

    final XYSeriesCollection collection = new XYSeriesCollection(series);
    final XYBarDataset dataset = new XYBarDataset(collection, binWidth);
    final JFreeChart chart = FxChartFactory.createXYBarChart("Inter-sample RT tolerance",
        "Absolute RT deviation / min", false, "Frequency", dataset, PlotOrientation.VERTICAL, false,
        true, false);
    styleXYBarChart(chart, List.of(RT_DISTRIBUTION_COLOR));

    final XYPlot plot = chart.getXYPlot();
    final ValueMarker estimate = new ValueMarker(interSampleRtStatistics.estimatedTolerance(),
        MEDIAN_COLOR, SOLID_STROKE);
    estimate.setLabel("Estimate: %.4g min".formatted(interSampleRtStatistics.estimatedTolerance()));
    estimate.setLabelBackgroundColor(
        ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : TRANSPARENT);
    plot.addDomainMarker(estimate);

    final ValueMarker upperBound = new ValueMarker(interSampleRtStatistics.upperSearchBound(),
        RT_BOUND_COLOR, DASHED_STROKE);
    upperBound.setLabel("Search max: %.4g".formatted(interSampleRtStatistics.upperSearchBound()));
    upperBound.setLabelBackgroundColor(
        ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : TRANSPARENT);
    upperBound.setLabelOffset(new RectangleInsets(14, 0, 0, 0));
    plot.addDomainMarker(upperBound);

    final double displayUpper = Math.max(observedMax, interSampleRtStatistics.upperSearchBound());
    if (displayUpper > 0d) {
      plot.getDomainAxis().setRange(0d, displayUpper * 1.03d);
    }
    return new EChartViewer(chart, true, true, true, true, true);
  }

  private @NotNull List<DataFileStatistics> getFilesForMode(@NotNull StatisticsDisplayMode mode) {
    return switch (mode) {
      case SINGLE_FILE -> {
        final DataFileStatistics selected = fileCombo.getValue();
        yield selected != null ? List.of(selected) : List.of();
      }
      case OVERLAY, ACCUMULATED -> statistics;
    };
  }

  /**
   * Fits a Gaussian curve to an accumulated histogram built from the combined raw data and adds it
   * on top of all datasets. The accumulated histogram is built with the same bin width as the
   * displayed bars, so the Gaussian fit matches the visible peak shape regardless of display mode.
   *
   * @param chart    the histogram chart
   * @param data     the combined raw data values (all files merged)
   * @param binWidth the histogram bin width used for the displayed bars
   * @param numFiles number of files contributing to the data — the Gaussian amplitude is divided by
   *                 this so it matches per-file bar heights in overlay mode (pass 1 for accumulated
   *                 or single-file mode)
   */
  private static void addGaussianFit(@NotNull JFreeChart chart, double @NotNull [] data,
      double binWidth, int numFiles) {
    if (data.length < 3) {
      return;
    }

    final XYPlot plot = chart.getXYPlot();

    try {
      // build an accumulated histogram series from the combined data with the same bin width
      final double min = HistogramChartFactory.getMin(data);
      final double max = HistogramChartFactory.getMax(data);
      final XYSeries accumulatedSeries = HistogramChartFactory.createHistoSeries(data, binWidth,
          min, max, null);
      if (accumulatedSeries.getItemCount() < 3) {
        return;
      }

      // fit {normFactor, mean, sigma} to the accumulated histogram bar heights,
      // then scale down by numFiles so the curve matches per-file bar heights in overlay mode
      final double[] fit = HistogramChartFactory.gaussianFit(accumulatedSeries, min, max);
      final double normFactor = fit[0] / numFiles;
      final double mean = fit[1];
      final double sigma = Math.abs(fit[2]);
      if (sigma <= 0) {
        return;
      }

      // decision: draw over mean ± 4σ with dense sampling so the curve is smooth in the peak
      // region and doesn't waste resolution on the long data tail
      final double drawMin = mean - 4 * sigma;
      final double drawMax = mean + 4 * sigma;

      final int steps = 500;
      final XYSeries gaussianSeries = new XYSeries(
          "Gaussian: %.4g \u00B1 %.4g".formatted(mean, sigma));
      for (int i = 0; i <= steps; i++) {
        final double x = drawMin + (drawMax - drawMin) * i / steps;
        final double exponent = -0.5 * Math.pow((x - mean) / sigma, 2);
        final double y = normFactor * Math.exp(exponent);
        gaussianSeries.add(x, y);
      }

      // add as the last dataset and ensure FORWARD rendering order so it draws on top of bars
      final XYSeriesCollection gaussianDataset = new XYSeriesCollection(gaussianSeries);
      final int index = plot.getDatasetCount();
      plot.setDataset(index, gaussianDataset);
      final var lineRenderer = new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
      lineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
      plot.setRenderer(index, lineRenderer);
      plot.setDatasetRenderingOrder(org.jfree.chart.plot.DatasetRenderingOrder.FORWARD);
    } catch (Exception e) {
      logger.log(Level.FINE, "Gaussian fit failed", e);
    }
  }

  /**
   * Adds value markers for mean, median, and 1/2/3 sigma to the histogram plot.
   *
   * @param chart            the histogram chart to annotate
   * @param data             the raw data values used to compute statistics
   * @param type             statistic represented by the histogram
   * @param massDetectorType determines whether an absolute noise estimate is meaningful
   */
  private static void addMarkers(@NotNull JFreeChart chart, double @NotNull [] data,
      @NotNull StatisticsPlotType type, @NotNull MassDetectorWizardOptions massDetectorType) {
    if (data.length == 0) {
      return;
    }

    final XYPlot plot = chart.getXYPlot();

    final double mean = MathUtils.calcAvg(data);
    final double[] sorted = data.clone();
    Arrays.sort(sorted);
    final double median = MathUtils.calcMedian(sorted);
    final double sigma = data.length >= 3 ? MathUtils.calcStd(data) : 0d;

    // mean marker (solid blue)
    final ValueMarker meanMarker = new ValueMarker(mean, MEAN_COLOR, SOLID_STROKE);
    meanMarker.setLabel("Mean: %.4f".formatted(mean));
    meanMarker.setLabelBackgroundColor(
        ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : TRANSPARENT);
    plot.addDomainMarker(meanMarker);
    // median marker (solid green)
    final ValueMarker medianMarker = new ValueMarker(median, MEDIAN_COLOR, SOLID_STROKE);
    final Double rawDataEstimate = rawDataEstimate(type, data, massDetectorType);
    final boolean estimateIsMedian =
        rawDataEstimate != null && Double.compare(rawDataEstimate, median) == 0;
    final String estimateTarget = rawDataEstimateTarget(type);
    medianMarker.setLabel(
        (estimateIsMedian ? "Median / raw-data %s estimate: %.4g".formatted(estimateTarget, median)
            : "Median: %.4f".formatted(median)));
    medianMarker.setLabelOffset(new RectangleInsets(14, 0, 0, 0));
    medianMarker.setLabelBackgroundColor(
        ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : TRANSPARENT);
    plot.addDomainMarker(medianMarker);

    if (rawDataEstimate != null && !estimateIsMedian) {
      final ValueMarker estimateMarker = new ValueMarker(rawDataEstimate, RAW_DATA_ESTIMATE_COLOR,
          SOLID_STROKE);
      estimateMarker.setLabelOffset(new RectangleInsets(28, 0, 0, 0));
      estimateMarker.setLabel("%s estimate: %.4g".formatted(estimateTarget, rawDataEstimate));
      estimateMarker.setLabelBackgroundColor(
          ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : TRANSPARENT);
      plot.addDomainMarker(estimateMarker);
    }

    // sigma markers (dashed magenta, decreasing opacity)
    if (sigma > 0) {
      plot.addDomainMarker(new ValueMarker(mean - sigma, SIGMA_1_COLOR, DASHED_STROKE));
      plot.addDomainMarker(new ValueMarker(mean + sigma, SIGMA_1_COLOR, DASHED_STROKE));
      plot.addDomainMarker(new ValueMarker(mean - 2 * sigma, SIGMA_2_COLOR, DASHED_STROKE));
      plot.addDomainMarker(new ValueMarker(mean + 2 * sigma, SIGMA_2_COLOR, DASHED_STROKE));
      plot.addDomainMarker(new ValueMarker(mean - 3 * sigma, SIGMA_3_COLOR, DASHED_STROKE));
      plot.addDomainMarker(new ValueMarker(mean + 3 * sigma, SIGMA_3_COLOR, DASHED_STROKE));
    }
  }

  static @Nullable Double rawDataEstimate(@NotNull StatisticsPlotType type, double @NotNull [] data,
      @NotNull MassDetectorWizardOptions massDetectorType) {
    return switch (type) {
      case FWHM -> RawDataParameterEstimation.estimateFwhm(data);
      case LOWEST_ISOTOPE_HEIGHT -> RawDataParameterEstimation.estimateMinHeight(data);
      case EDGE_INTENSITY -> massDetectorType == MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL
          ? RawDataParameterEstimation.estimateAbsoluteNoiseLevel(data) : null;
      case ISOTOPE_DATA_POINTS -> RawDataParameterEstimation.estimateMinConsecutiveScans(data);
      case BEST_TOLERANCE_FREQUENCY -> null;
    };
  }

  static @NotNull String rawDataEstimateTarget(@NotNull StatisticsPlotType type) {
    return switch (type) {
      case FWHM -> "FWHM";
      case LOWEST_ISOTOPE_HEIGHT -> "minimum height";
      case EDGE_INTENSITY -> "MS1 noise level";
      case ISOTOPE_DATA_POINTS -> "minimum consecutive scans";
      case BEST_TOLERANCE_FREQUENCY -> "";
    };
  }

  private static void styleXYBarChart(@NotNull JFreeChart chart,
      @NotNull List<Color> seriesColors) {
    final XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setDomainGridlinePaint(new Color(150, 150, 150));
    plot.setRangeGridlinePaint(new Color(150, 150, 150));
    plot.setForegroundAlpha(0.7f);

    final XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
    renderer.setShadowVisible(false);
    renderer.setBarPainter(new StandardXYBarPainter());
    renderer.setDrawBarOutline(false);

    for (int i = 0; i < seriesColors.size(); i++) {
      final Color c = seriesColors.get(i);
      renderer.setSeriesPaint(i, new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
    }
  }

  private static @NotNull Map<DataFileStatistics, Color> assignColors(
      @NotNull List<DataFileStatistics> stats) {
    final var palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    final Map<DataFileStatistics, Color> colors = new LinkedHashMap<>();
    for (DataFileStatistics stat : stats) {
      colors.put(stat, palette.getNextColorAWT());
    }
    return colors;
  }
}
