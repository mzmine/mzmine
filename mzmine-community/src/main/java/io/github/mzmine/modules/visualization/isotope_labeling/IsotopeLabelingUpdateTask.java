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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.progress.TotalFinishedItemsProgress;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

public class IsotopeLabelingUpdateTask extends FxUpdateTask<IsotopeLabelingModel> {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingUpdateTask.class.getName());
  private static final DecimalFormat MZ_FORMAT = new DecimalFormat("0.0000");
  private static final DecimalFormat RT_FORMAT = new DecimalFormat("0.00");

  private final Map<Integer, List<FeatureListRow>> allClusters;
  private final TotalFinishedItemsProgress progress = new TotalFinishedItemsProgress();
  private @Nullable JFreeChart chart;
  private Map<Integer, Map<String, double[]>> processedData = new HashMap<>();

  public IsotopeLabelingUpdateTask(@NotNull IsotopeLabelingModel model,
      Map<Integer, List<FeatureListRow>> allClusters) {
    super("isotope_labeling_update", model);
    this.allClusters = allClusters;

    List<Integer> selectedClusters = model.getSelectedClusters();
    progress.setTotal(selectedClusters != null ? selectedClusters.size() : 0);
  }

  @Override
  public boolean checkPreConditions() {
    if (model.getFeatureLists() == null || model.getFeatureLists().isEmpty()) {
      logger.warning("No feature lists available");
      return false;
    }
    if (allClusters == null || allClusters.isEmpty()) {
      logger.warning("No isotope clusters available");
      return false;
    }
    List<Integer> selectedClusters = model.getSelectedClusters();
    if (selectedClusters == null || selectedClusters.isEmpty()) {
      logger.warning("No isotope clusters selected");
      return false;
    }
    return true;
  }

  @Override
  protected void process() {
    if (!checkPreConditions()) {
      return;
    }

    List<Integer> selectedClusterIds = model.getSelectedClusters();
    boolean multipleCluster = selectedClusterIds.size() > 1;

    // dataset: series = group name, category = isotopologue label
    DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

    // significance annotations: category key → significance string
    // stored as list of (categoryKey, yValue, significanceString)
    List<Object[]> significanceAnnotations = new ArrayList<>();

    String visualizationType = model.getVisualizationType();
    boolean useRelative = "Relative intensities".equals(visualizationType);
    boolean normalizeToBase = model.getNormalizeToBaseIsotopologue();
    int maxIsotopologues = model.getMaxIsotopologues();

    // Track all group names to assign consistent colors later
    Set<String> allGroupNames = new LinkedHashSet<>();
    // Map cluster ID → [baseMz, baseRt] for subtitle annotations
    Map<Integer, double[]> clusterInfo = new LinkedHashMap<>();

    // Pre-compute group colors in fixed order so bars and dots share the same palette
    SimpleColorPalette colorPalette = MZmineCore.getConfiguration().getDefaultColorPalette();
    colorPalette.resetColorCounter();
    Map<String, Color> groupColorMap = new LinkedHashMap<>();
    for (String preferred : List.of("Unlabeled", "Labeled", "Unknown")) {
      groupColorMap.put(preferred, colorPalette.getNextColorAWT());
    }

    // Per-sample dot datasets (populated inside the cluster loop)
    Map<String, Color> pointSeriesColors = new LinkedHashMap<>();
    DefaultCategoryDataset pointsDataset = new DefaultCategoryDataset();

    // Labeled fraction per cluster (fraction of labeled signal that is M+1 and above)
    Map<Integer, Double> labeledFractions = new LinkedHashMap<>();

    for (Integer clusterId : selectedClusterIds) {
      if (isCanceled()) {
        return;
      }

      List<FeatureListRow> rawRows = allClusters.get(clusterId);
      if (rawRows == null || rawRows.isEmpty()) {
        continue;
      }

      // Sort by isotopologue rank into a new mutable list
      List<FeatureListRow> sortedRows = new ArrayList<>(rawRows);
      sortedRows.sort((r1, r2) -> {
        Integer rank1 = IsotopeLabelingModel.getIsotopologueRank(r1);
        Integer rank2 = IsotopeLabelingModel.getIsotopologueRank(r2);
        return Integer.compare(rank1 != null ? rank1 : 0, rank2 != null ? rank2 : 0);
      });

      final List<FeatureListRow> rows =
          sortedRows.size() > maxIsotopologues ? sortedRows.subList(0, maxIsotopologues)
              : sortedRows;

      FeatureListRow baseRow = IsotopeLabelingModel.findBasePeak(rows);
      if (baseRow == null) {
        continue;
      }
      clusterInfo.put(clusterId, new double[]{baseRow.getAverageMZ(), baseRow.getAverageRT()});

      List<RawDataFile> rawDataFiles = baseRow.getRawDataFiles();
      Map<String, String> sampleGroups = model.determineSampleGroups(rawDataFiles);

      // Group files by group name
      Map<String, List<RawDataFile>> groupedFiles = new LinkedHashMap<>();
      for (RawDataFile file : rawDataFiles) {
        String group = sampleGroups.getOrDefault(file.getName(), "Unknown");
        groupedFiles.computeIfAbsent(group, k -> new ArrayList<>()).add(file);
        allGroupNames.add(group);
      }

      // Find the base peak row for normalization
      FeatureListRow normRow = normalizeToBase ? baseRow : null;

      // Per-isotopologue: compute per-group means, SDs, and raw arrays for t-tests
      // rawGroupValues: isotopologue index → group name → double[]
      List<Map<String, double[]>> rawGroupValues = new ArrayList<>();

      for (int i = 0; i < rows.size(); i++) {
        FeatureListRow row = rows.get(i);
        Map<String, double[]> groupValues = new HashMap<>();

        for (Map.Entry<String, List<RawDataFile>> entry : groupedFiles.entrySet()) {
          String groupName = entry.getKey();
          List<RawDataFile> groupFiles = entry.getValue();
          double[] values = collectIntensities(row, rows, groupFiles, useRelative, normRow);
          groupValues.put(groupName, values);
        }

        rawGroupValues.add(groupValues);
      }

      // Add to dataset
      Map<String, double[]> clusterMeans = new LinkedHashMap<>();

      for (int i = 0; i < rows.size(); i++) {
        FeatureListRow row = rows.get(i);
        Integer rank = IsotopeLabelingModel.getIsotopologueRank(row);
        int massShift = rank != null ? rank : i;
        String categoryKey =
            multipleCluster ? ("Cluster " + clusterId + " M+" + massShift) : ("M+" + massShift);

        Map<String, double[]> groupValues = rawGroupValues.get(i);

        for (Map.Entry<String, double[]> entry : groupValues.entrySet()) {
          String groupName = entry.getKey();
          double[] vals = entry.getValue();
          DescriptiveStatistics stats = new DescriptiveStatistics(vals);
          double mean = stats.getMean();
          double sd =
              Double.isNaN(stats.getStandardDeviation()) ? 0.0 : stats.getStandardDeviation();
          dataset.add(mean, sd, groupName, categoryKey);
          clusterMeans.computeIfAbsent(groupName, k -> new double[rows.size()])[i] = mean;

          // Add individual sample dots
          for (int j = 0; j < vals.length; j++) {
            String seriesKey = groupName + "_s" + j;
            pointsDataset.addValue(vals[j], seriesKey, categoryKey);
            pointSeriesColors.putIfAbsent(seriesKey,
                groupColorMap.getOrDefault(groupName, Color.GRAY));
          }
        }
      }

      // t-test between "Labeled" and "Unlabeled" per isotopologue
      TTest tTest = new TTest();
      for (int i = 0; i < rows.size(); i++) {
        FeatureListRow row = rows.get(i);
        Integer rank = IsotopeLabelingModel.getIsotopologueRank(row);
        int massShift = rank != null ? rank : i;
        String categoryKey =
            multipleCluster ? ("Cluster " + clusterId + " M+" + massShift) : ("M+" + massShift);

        Map<String, double[]> groupValues = rawGroupValues.get(i);
        double[] labeled = groupValues.get("Labeled");
        double[] unlabeled = groupValues.get("Unlabeled");

        if (model.getShowSignificanceMarkers() && labeled != null && unlabeled != null
            && labeled.length >= 2 && unlabeled.length >= 2) {
          try {
            double pValue = tTest.tTest(labeled, unlabeled);
            String sig = significanceLabel(pValue);
            if (!sig.isEmpty()) {
              // y position: slightly above the tallest bar (mean + sd)
              double yPos = 0.0;
              for (double[] vals : groupValues.values()) {
                DescriptiveStatistics st = new DescriptiveStatistics(vals);
                double top = st.getMean() + (Double.isNaN(st.getStandardDeviation()) ? 0.0
                    : st.getStandardDeviation());
                if (top > yPos) {
                  yPos = top;
                }
              }
              significanceAnnotations.add(new Object[]{categoryKey, yPos, sig});
            }
          } catch (Exception e) {
            logger.log(Level.FINE, "t-test failed for isotopologue " + i + " cluster " + clusterId,
                e);
          }
        }
      }

      // Compute labeled fraction: fraction of labeled group signal that is M+1 and above
      double[] labeledMeans = clusterMeans.getOrDefault("Labeled", new double[0]);
      if (labeledMeans.length > 0) {
        double total = 0;
        for (double v : labeledMeans) {
          total += v;
        }
        double enriched = total - labeledMeans[0];
        labeledFractions.put(clusterId, total > 0 ? enriched / total : 0.0);
      }

      processedData.put(clusterId, clusterMeans);
      progress.getAndIncrement();
    }

    // Build chart
    chart = buildGroupedBarChart(dataset, visualizationType, normalizeToBase, selectedClusterIds,
        significanceAnnotations, allGroupNames, clusterInfo, pointsDataset, pointSeriesColors,
        groupColorMap, labeledFractions);
  }

  /**
   * Collect per-sample intensities for a single isotopologue row.
   */
  private double[] collectIntensities(FeatureListRow row, List<FeatureListRow> allRows,
      List<RawDataFile> files, boolean useRelative, @Nullable FeatureListRow normRow) {
    List<Double> values = new ArrayList<>();
    for (RawDataFile file : files) {
      Feature feature = row.getFeature(file);
      if (feature == null) {
        continue;
      }
      double intensity = feature.getHeight();
      if (useRelative) {
        if (normRow != null) {
          // Normalize to M+0 intensity
          Feature baseFeature = normRow.getFeature(file);
          if (baseFeature == null || baseFeature.getHeight() <= 0) {
            continue;
          }
          values.add(intensity / baseFeature.getHeight());
        } else {
          // Normalize to total cluster intensity in this sample
          double total = 0.0;
          for (FeatureListRow r : allRows) {
            Feature f = r.getFeature(file);
            if (f != null) {
              total += f.getHeight();
            }
          }
          if (total <= 0) {
            continue;
          }
          values.add(intensity / total);
        }
      } else {
        values.add(intensity);
      }
    }
    return values.stream().mapToDouble(Double::doubleValue).toArray();
  }

  /**
   * Returns the significance label for a p-value.
   */
  private static String significanceLabel(double pValue) {
    if (pValue < 0.001) {
      return "***";
    } else if (pValue < 0.01) {
      return "**";
    } else if (pValue < 0.05) {
      return "*";
    }
    return "";
  }

  /**
   * Build a grouped bar chart with error bars, significance annotations, per-sample dots, and
   * labeled-fraction subtitles.
   */
  private JFreeChart buildGroupedBarChart(DefaultStatisticalCategoryDataset dataset,
      String visualizationType, boolean normalizeToBase, List<Integer> selectedClusterIds,
      List<Object[]> significanceAnnotations, Set<String> groupNames,
      Map<Integer, double[]> clusterInfo, DefaultCategoryDataset pointsDataset,
      Map<String, Color> pointSeriesColors, Map<String, Color> groupColorMap,
      Map<Integer, Double> labeledFractions) {

    String title;
    if (selectedClusterIds.size() == 1) {
      int id = selectedClusterIds.get(0);
      double[] info = clusterInfo.get(id);
      if (info != null) {
        title = "Cluster " + id + "  |  m/z " + MZ_FORMAT.format(info[0]) + "  |  RT "
            + RT_FORMAT.format(info[1]) + " min";
      } else {
        title = "Isotopologue Distribution - Cluster " + id;
      }
    } else {
      title = "Isotopologue Distributions - " + selectedClusterIds.size() + " Clusters";
    }

    String yAxisLabel;
    if ("Relative intensities".equals(visualizationType)) {
      yAxisLabel = normalizeToBase ? "Relative Intensity (normalized to M+0)"
          : "Relative Intensity (fraction of total)";
    } else {
      yAxisLabel = "Intensity";
    }

    JFreeChart jfchart = ChartFactory.createBarChart(title, "Isotopologue", yAxisLabel, dataset,
        PlotOrientation.VERTICAL, true, true, false);

    CategoryPlot plot = jfchart.getCategoryPlot();

    // Replace renderer with StatisticalBarRenderer to show error bars
    StatisticalBarRenderer renderer = new StatisticalBarRenderer();
    renderer.setErrorIndicatorPaint(Color.BLACK);
    renderer.setItemMargin(0.05);
    renderer.setShadowVisible(false);
    plot.setRenderer(renderer);

    // Apply group colors from pre-computed map (consistent across bars and dots)
    for (int i = 0; i < dataset.getRowCount(); i++) {
      String seriesKey = (String) dataset.getRowKey(i);
      Color c = groupColorMap.get(seriesKey);
      if (c != null) {
        renderer.setSeriesPaint(i, c);
      }
    }

    // Axis styling
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setLowerMargin(0.02);
    domainAxis.setUpperMargin(0.02);
    if (selectedClusterIds.size() > 1) {
      domainAxis.setCategoryLabelPositions(
          CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4.0));
    }

    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setAutoRange(true);
    rangeAxis.setAutoRangeIncludesZero(true);

    // Significance annotations
    if (!significanceAnnotations.isEmpty()) {
      // compute overall max to set upper margin for annotations
      double globalMax = 0.0;
      for (Object[] ann : significanceAnnotations) {
        double yVal = (double) ann[1];
        if (yVal > globalMax) {
          globalMax = yVal;
        }
      }
      rangeAxis.setUpperBound(globalMax * 1.25);

      Font sigFont = new Font("SansSerif", Font.BOLD, 14);
      for (Object[] ann : significanceAnnotations) {
        String categoryKey = (String) ann[0];
        double yPos = (double) ann[1] * 1.05;
        String sigLabel = (String) ann[2];
        CategoryTextAnnotation annotation = new CategoryTextAnnotation(sigLabel, categoryKey, yPos);
        annotation.setFont(sigFont);
        annotation.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        plot.addAnnotation(annotation);
      }
    }

    // Overlay individual sample dots on top of bars
    if (pointsDataset.getRowCount() > 0) {
      plot.setDataset(1, pointsDataset);
      LineAndShapeRenderer pointRenderer = new LineAndShapeRenderer(false, true);
      Ellipse2D.Double dot = new Ellipse2D.Double(-3.5, -3.5, 7, 7);
      for (int i = 0; i < pointsDataset.getRowCount(); i++) {
        String sk = (String) pointsDataset.getRowKey(i);
        Color base = pointSeriesColors.getOrDefault(sk, Color.GRAY);
        pointRenderer.setSeriesPaint(i,
            new Color(base.getRed(), base.getGreen(), base.getBlue(), 180));
        pointRenderer.setSeriesShape(i, dot);
        pointRenderer.setSeriesVisibleInLegend(i, false);
      }
      plot.setRenderer(1, pointRenderer);
      plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
    }

    jfchart.setBackgroundPaint(Color.WHITE);

    // Groups subtitle
    String groupSubtitle = "Groups: " + String.join(" vs ", groupNames);
    TextTitle groupSubtitleText = new TextTitle(groupSubtitle);
    groupSubtitleText.setFont(new Font("SansSerif", Font.ITALIC, 11));
    jfchart.addSubtitle(groupSubtitleText);

    // For a single cluster, add labeled-fraction annotation if available
    if (selectedClusterIds.size() == 1) {
      int id = selectedClusterIds.get(0);
      Double fraction = labeledFractions.get(id);
      if (fraction != null && fraction > 0) {
        String fractionText = String.format("%.0f%% incorporated (M+1 and above)", fraction * 100);
        TextTitle ft = new TextTitle(fractionText);
        ft.setFont(new Font("SansSerif", Font.BOLD, 11));
        jfchart.addSubtitle(ft);
      }
    }

    // For multiple clusters, add a subtitle listing each cluster's m/z, RT, and labeled fraction
    if (selectedClusterIds.size() > 1 && !clusterInfo.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Integer id : selectedClusterIds) {
        double[] info = clusterInfo.get(id);
        if (info != null) {
          if (sb.length() > 0) {
            sb.append("   |   ");
          }
          sb.append("Cluster ").append(id).append(": m/z ").append(MZ_FORMAT.format(info[0]))
              .append(", RT ").append(RT_FORMAT.format(info[1])).append(" min");
          Double fraction = labeledFractions.get(id);
          if (fraction != null && fraction > 0) {
            sb.append(String.format(", %.0f%% incorp.", fraction * 100));
          }
        }
      }
      if (sb.length() > 0) {
        TextTitle clusterSubtitle = new TextTitle(sb.toString());
        clusterSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
        jfchart.addSubtitle(clusterSubtitle);
      }
    }

    return jfchart;
  }

  @Override
  protected void updateGuiModel() {
    if (chart == null) {
      return;
    }
    model.setChart(chart);
    model.setProcessedData(processedData);
  }

  @Override
  public String getTaskDescription() {
    return "Updating isotope labeling visualization";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.progress();
  }
}
