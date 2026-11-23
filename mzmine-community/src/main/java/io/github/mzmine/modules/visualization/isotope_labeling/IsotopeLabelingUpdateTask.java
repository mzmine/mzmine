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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

/**
 * Builds a bar chart (stacked or grouped) for isotopologue distributions.
 *
 * <p><b>Stacked mode</b> — series = isotopologues ("M+0", "M+1", …), categories = sample groups
 * ("Labeled", "Unlabeled"). Each group shows a single bar with isotopologue contributions stacked.
 *
 * <p><b>Grouped mode</b> — series = sample groups, categories = isotopologues. Each isotopologue
 * shows Labeled and Unlabeled bars side-by-side.
 */
public class IsotopeLabelingUpdateTask extends FxUpdateTask<IsotopeLabelingModel> {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingUpdateTask.class.getName());
  private static final DecimalFormat MZ_FORMAT = new DecimalFormat("0.0000");
  private static final DecimalFormat RT_FORMAT = new DecimalFormat("0.00");

  private final Map<Integer, List<FeatureListRow>> allClusters;
  // Snapshot taken on the FX thread at construction time — avoids reading a JavaFX property
  // from the background task thread where SimpleBooleanProperty has no volatile guarantee.
  private final boolean stackedBars;
  private final TotalFinishedItemsProgress progress = new TotalFinishedItemsProgress();
  private @Nullable JFreeChart chart;
  private Map<Integer, Map<String, double[]>> processedData = new HashMap<>();
  private Map<Integer, Double> computedFractions = new HashMap<>();

  public IsotopeLabelingUpdateTask(@NotNull IsotopeLabelingModel model,
      Map<Integer, List<FeatureListRow>> allClusters) {
    super("isotope_labeling_update", model);
    this.allClusters = allClusters;
    this.stackedBars = model.isStackedBars(); // capture on FX thread

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

    if (stackedBars) {
      processStacked();
    } else {
      processGrouped();
    }
  }

  // ── Stacked mode ────────────────────────────────────────────────────────────────────────────

  /**
   * Stacked layout: series = isotopologues ("M+0", …), categories = groups ("Labeled",
   * "Unlabeled"). {@link BarJitterRenderer} draws dots at y = stack_base + sample_value.
   */
  private void processStacked() {
    List<Integer> selectedClusterIds = model.getSelectedClusters();
    boolean multipleCluster = selectedClusterIds.size() > 1;

    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    // categoryKey(group) → isotopologueKey → per-sample values
    Map<String, Map<String, double[]>> jitterValues = new LinkedHashMap<>();
    List<Object[]> significanceAnnotations = new ArrayList<>();

    String visualizationType = model.getVisualizationType();
    boolean useRelative = "Relative intensities".equals(visualizationType);
    int normalizationRank = model.getNormalizationRank();
    int maxIsotopologues = model.getMaxIsotopologues();

    Set<String> allGroupNames = new LinkedHashSet<>();
    Map<Integer, double[]> clusterInfo = new LinkedHashMap<>();
    Map<Integer, Double> labeledFractions = new LinkedHashMap<>();

    for (Integer clusterId : selectedClusterIds) {
      if (isCanceled()) {
        return;
      }
      ClusterData cd = collectClusterData(clusterId, useRelative, normalizationRank,
          maxIsotopologues, multipleCluster);
      if (cd == null) {
        continue;
      }
      clusterInfo.put(clusterId, new double[]{cd.baseMZ, cd.baseRT});
      allGroupNames.addAll(cd.groupedFiles.keySet());

      // Populate dataset: series = isotopologue, category = group
      Map<String, double[]> clusterMeans = new LinkedHashMap<>();
      for (int i = 0; i < cd.rows.size(); i++) {
        Integer rank = IsotopeLabelingModel.getIsotopologueRank(cd.rows.get(i));
        String isoKey = "M+" + (rank != null ? rank : i);
        Map<String, double[]> groupValues = cd.rawGroupValues.get(i);
        for (Map.Entry<String, double[]> entry : groupValues.entrySet()) {
          String groupName = entry.getKey();
          double[] vals = entry.getValue();
          double mean = safeMean(vals);
          String catKey = multipleCluster ? ("C" + clusterId + " " + groupName) : groupName;
          dataset.addValue(mean, isoKey, catKey);
          clusterMeans.computeIfAbsent(groupName, k -> new double[cd.rows.size()])[i] = mean;
          jitterValues.computeIfAbsent(catKey, k -> new LinkedHashMap<>()).put(isoKey, vals);
        }
      }

      // t-tests per isotopologue
      String labeledCatKey = multipleCluster ? ("C" + clusterId + " Labeled") : "Labeled";
      collectSignificance(cd, multipleCluster, clusterId, labeledCatKey, significanceAnnotations);

      // Labeled fractional contribution
      double[] labeledMeans = clusterMeans.getOrDefault("Labeled", new double[0]);
      double fc = fractionalContribution(labeledMeans);
      if (!Double.isNaN(fc)) {
        labeledFractions.put(clusterId, fc);
      }

      processedData.put(clusterId, clusterMeans);
      progress.getAndIncrement();
    }

    computedFractions = labeledFractions;

    // Build color map: isotopologue → color (same order as dataset rows)
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    palette.resetColorCounter();
    Map<String, Color> isotopologueColors = new LinkedHashMap<>();
    for (int i = 0; i < dataset.getRowCount(); i++) {
      isotopologueColors.put((String) dataset.getRowKey(i), palette.getNextColorAWT());
    }

    chart = buildStackedBarChart(dataset, jitterValues, isotopologueColors, visualizationType,
        normalizationRank, selectedClusterIds, significanceAnnotations, allGroupNames, clusterInfo,
        labeledFractions);
  }

  // ── Grouped mode ─────────────────────────────────────────────────────────────────────────────

  /**
   * Grouped layout: series = groups ("Labeled", "Unlabeled"), categories = isotopologues ("M+0",
   * …). {@link GroupedBarJitterRenderer} draws dots at y = sample_value, x = bar centre.
   */
  private void processGrouped() {
    List<Integer> selectedClusterIds = model.getSelectedClusters();
    boolean multipleCluster = selectedClusterIds.size() > 1;

    // series = groups ("Labeled","Unlabeled"), categories = isotopologues ("M+0","M+1",…)
    DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
    // isotopologueKey(catKey) → groupKey → per-sample values  (for jitter dots)
    Map<String, Map<String, double[]>> jitterValues = new LinkedHashMap<>();
    List<Object[]> significanceAnnotations = new ArrayList<>();

    String visualizationType = model.getVisualizationType();
    boolean useRelative = "Relative intensities".equals(visualizationType);
    int normalizationRank = model.getNormalizationRank();
    int maxIsotopologues = model.getMaxIsotopologues();

    Set<String> allGroupNames = new LinkedHashSet<>();
    Map<Integer, double[]> clusterInfo = new LinkedHashMap<>();
    Map<Integer, Double> labeledFractions = new LinkedHashMap<>();

    // Fixed group colors assigned once so bars and dots share the same palette
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    palette.resetColorCounter();
    Map<String, Color> groupColorMap = new LinkedHashMap<>();
    for (String preferred : List.of("Unlabeled", "Labeled", "Unknown")) {
      groupColorMap.put(preferred, palette.getNextColorAWT());
    }

    for (Integer clusterId : selectedClusterIds) {
      if (isCanceled()) {
        return;
      }
      ClusterData cd = collectClusterData(clusterId, useRelative, normalizationRank,
          maxIsotopologues, multipleCluster);
      if (cd == null) {
        continue;
      }
      clusterInfo.put(clusterId, new double[]{cd.baseMZ, cd.baseRT});
      allGroupNames.addAll(cd.groupedFiles.keySet());

      // Populate dataset: series = group, category = isotopologue
      Map<String, double[]> clusterMeans = new LinkedHashMap<>();
      for (int i = 0; i < cd.rows.size(); i++) {
        Integer rank = IsotopeLabelingModel.getIsotopologueRank(cd.rows.get(i));
        String isoKey = multipleCluster ? ("C" + clusterId + " M+" + (rank != null ? rank : i))
            : "M+" + (rank != null ? rank : i);
        Map<String, double[]> groupValues = cd.rawGroupValues.get(i);
        for (Map.Entry<String, double[]> entry : groupValues.entrySet()) {
          String groupName = entry.getKey();
          double[] vals = entry.getValue();
          DescriptiveStatistics stats = new DescriptiveStatistics(vals);
          double mean = Double.isNaN(stats.getMean()) ? 0.0 : stats.getMean();
          double sd = Double.isNaN(stats.getStandardDeviation()) ? 0.0
              : stats.getStandardDeviation();
          dataset.add(mean, sd, groupName, isoKey);
          clusterMeans.computeIfAbsent(groupName, k -> new double[cd.rows.size()])[i] = mean;
          jitterValues.computeIfAbsent(isoKey, k -> new LinkedHashMap<>()).put(groupName, vals);
        }
      }

      // t-tests: significance annotation stored as {categoryKey, yPos, sigLabel}
      if (model.getShowSignificanceMarkers()) {
        TTest tTest = new TTest();
        for (int i = 0; i < cd.rows.size(); i++) {
          Integer rank = IsotopeLabelingModel.getIsotopologueRank(cd.rows.get(i));
          String isoKey = multipleCluster ? ("C" + clusterId + " M+" + (rank != null ? rank : i))
              : "M+" + (rank != null ? rank : i);
          Map<String, double[]> groupValues = cd.rawGroupValues.get(i);
          double[] labeled = groupValues.get("Labeled");
          double[] unlabeled = groupValues.get("Unlabeled");
          if (labeled == null || unlabeled == null || labeled.length < 2
              || unlabeled.length < 2) {
            continue;
          }
          try {
            String sig = significanceLabel(tTest.tTest(labeled, unlabeled));
            if (!sig.isEmpty()) {
              double yPos = 0;
              for (double[] vals : groupValues.values()) {
                DescriptiveStatistics st = new DescriptiveStatistics(vals);
                double top = st.getMean() + (Double.isNaN(st.getStandardDeviation()) ? 0.0
                    : st.getStandardDeviation());
                if (top > yPos) {
                  yPos = top;
                }
              }
              significanceAnnotations.add(new Object[]{isoKey, yPos, sig});
            }
          } catch (Exception e) {
            logger.log(Level.FINE,
                "t-test failed for isotopologue " + i + " cluster " + clusterId, e);
          }
        }
      }

      double[] labeledMeans = clusterMeans.getOrDefault("Labeled", new double[0]);
      double fc = fractionalContribution(labeledMeans);
      if (!Double.isNaN(fc)) {
        labeledFractions.put(clusterId, fc);
      }

      processedData.put(clusterId, clusterMeans);
      progress.getAndIncrement();
    }

    computedFractions = labeledFractions;

    chart = buildGroupedBarChart(dataset, jitterValues, groupColorMap, visualizationType,
        normalizationRank, selectedClusterIds, significanceAnnotations, allGroupNames, clusterInfo,
        labeledFractions);
  }

  // ── Shared data collection ───────────────────────────────────────────────────────────────────

  /** All data for one cluster needed by both stacked and grouped paths. */
  private static final class ClusterData {

    final List<FeatureListRow> rows;
    final Map<String, List<RawDataFile>> groupedFiles;
    final List<Map<String, double[]>> rawGroupValues; // index = isotopologue index
    final double baseMZ;
    final double baseRT;

    ClusterData(List<FeatureListRow> rows, Map<String, List<RawDataFile>> groupedFiles,
        List<Map<String, double[]>> rawGroupValues, double baseMZ, double baseRT) {
      this.rows = rows;
      this.groupedFiles = groupedFiles;
      this.rawGroupValues = rawGroupValues;
      this.baseMZ = baseMZ;
      this.baseRT = baseRT;
    }
  }

  private @Nullable ClusterData collectClusterData(int clusterId, boolean useRelative,
      int normalizationRank, int maxIsotopologues, boolean multipleCluster) {
    List<FeatureListRow> rawRows = allClusters.get(clusterId);
    if (rawRows == null || rawRows.isEmpty()) {
      return null;
    }

    List<FeatureListRow> sorted = new ArrayList<>(rawRows);
    sorted.sort((r1, r2) -> {
      Integer rk1 = IsotopeLabelingModel.getIsotopologueRank(r1);
      Integer rk2 = IsotopeLabelingModel.getIsotopologueRank(r2);
      return Integer.compare(rk1 != null ? rk1 : 0, rk2 != null ? rk2 : 0);
    });
    final List<FeatureListRow> rows =
        sorted.size() > maxIsotopologues ? sorted.subList(0, maxIsotopologues) : sorted;

    FeatureListRow baseRow = IsotopeLabelingModel.findBasePeak(rows);
    if (baseRow == null) {
      return null;
    }

    List<RawDataFile> rawDataFiles = baseRow.getRawDataFiles();
    Map<String, String> sampleGroups = model.determineSampleGroups(rawDataFiles);

    Map<String, List<RawDataFile>> groupedFiles = new LinkedHashMap<>();
    for (RawDataFile file : rawDataFiles) {
      groupedFiles.computeIfAbsent(sampleGroups.getOrDefault(file.getName(), "Unknown"),
          k -> new ArrayList<>()).add(file);
    }

    FeatureListRow normRow = null;
    if (normalizationRank >= 0) {
      final int targetRank = normalizationRank;
      normRow = rows.stream().filter(r -> {
        Integer rk = IsotopeLabelingModel.getIsotopologueRank(r);
        return rk != null && rk == targetRank;
      }).findFirst().orElse(null);
    }

    List<Map<String, double[]>> rawGroupValues = new ArrayList<>();
    for (FeatureListRow row : rows) {
      Map<String, double[]> groupValues = new HashMap<>();
      for (Map.Entry<String, List<RawDataFile>> entry : groupedFiles.entrySet()) {
        groupValues.put(entry.getKey(),
            collectIntensities(row, rows, entry.getValue(), useRelative, normRow));
      }
      rawGroupValues.add(groupValues);
    }

    return new ClusterData(rows, groupedFiles, rawGroupValues, baseRow.getAverageMZ(),
        baseRow.getAverageRT());
  }

  private void collectSignificance(ClusterData cd, boolean multipleCluster, int clusterId,
      String labeledCatKey, List<Object[]> out) {
    if (!model.getShowSignificanceMarkers()) {
      return;
    }
    TTest tTest = new TTest();
    for (int i = 0; i < cd.rows.size(); i++) {
      Integer rank = IsotopeLabelingModel.getIsotopologueRank(cd.rows.get(i));
      String isoKey = "M+" + (rank != null ? rank : i);
      double[] labeled = cd.rawGroupValues.get(i).get("Labeled");
      double[] unlabeled = cd.rawGroupValues.get(i).get("Unlabeled");
      if (labeled == null || unlabeled == null || labeled.length < 2 || unlabeled.length < 2) {
        continue;
      }
      try {
        String sig = significanceLabel(tTest.tTest(labeled, unlabeled));
        if (!sig.isEmpty()) {
          out.add(new Object[]{labeledCatKey, isoKey, sig});
        }
      } catch (Exception e) {
        logger.log(Level.FINE, "t-test failed for isotopologue " + i + " cluster " + clusterId, e);
      }
    }
  }

  private static double safeMean(double[] vals) {
    if (vals == null || vals.length == 0) {
      return 0.0;
    }
    double mean = new DescriptiveStatistics(vals).getMean();
    return Double.isNaN(mean) ? 0.0 : mean;
  }

  private static double fractionalContribution(double[] labeledMeans) {
    int n = labeledMeans.length;
    if (n <= 1) {
      return Double.NaN;
    }
    double total = 0;
    double weighted = 0;
    for (int k = 0; k < n; k++) {
      total += labeledMeans[k];
      weighted += k * labeledMeans[k];
    }
    return total > 0 ? (weighted / total) / (n - 1) : Double.NaN;
  }

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
          Feature baseFeature = normRow.getFeature(file);
          if (baseFeature == null || baseFeature.getHeight() <= 0) {
            continue;
          }
          values.add(intensity / baseFeature.getHeight());
        } else {
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

  private static String significanceLabel(double pValue) {
    if (pValue < 0.001) {
      return "***";
    }
    if (pValue < 0.01) {
      return "**";
    }
    if (pValue < 0.05) {
      return "*";
    }
    return "";
  }

  // ── Chart builders ───────────────────────────────────────────────────────────────────────────

  private JFreeChart buildStackedBarChart(DefaultCategoryDataset dataset,
      Map<String, Map<String, double[]>> jitterValues, Map<String, Color> isotopologueColors,
      String visualizationType, int normalizationRank, List<Integer> selectedClusterIds,
      List<Object[]> significanceAnnotations, Set<String> groupNames,
      Map<Integer, double[]> clusterInfo, Map<Integer, Double> labeledFractions) {

    String yAxisLabel = yAxisLabel(visualizationType, normalizationRank);

    JFreeChart jfchart = ChartFactory.createStackedBarChart(
        chartTitle(selectedClusterIds, clusterInfo), "Group", yAxisLabel, dataset,
        PlotOrientation.VERTICAL, true, true, false);

    CategoryPlot plot = jfchart.getCategoryPlot();

    BarJitterRenderer renderer = new BarJitterRenderer(jitterValues);
    plot.setRenderer(renderer);
    for (int i = 0; i < dataset.getRowCount(); i++) {
      renderer.setSeriesPaint(i, isotopologueColors.get(dataset.getRowKey(i)));
    }

    styleAxes(plot, selectedClusterIds);

    addStackedSignificanceAnnotations(plot, dataset, significanceAnnotations);

    applyChartStyle(jfchart, groupNames, selectedClusterIds, clusterInfo, labeledFractions);
    return jfchart;
  }

  private JFreeChart buildGroupedBarChart(DefaultStatisticalCategoryDataset dataset,
      Map<String, Map<String, double[]>> jitterValues, Map<String, Color> groupColorMap,
      String visualizationType, int normalizationRank, List<Integer> selectedClusterIds,
      List<Object[]> significanceAnnotations, Set<String> groupNames,
      Map<Integer, double[]> clusterInfo, Map<Integer, Double> labeledFractions) {

    String yAxisLabel = yAxisLabel(visualizationType, normalizationRank);

    JFreeChart jfchart = ChartFactory.createBarChart(
        chartTitle(selectedClusterIds, clusterInfo), "Isotopologue", yAxisLabel, dataset,
        PlotOrientation.VERTICAL, true, true, false);

    CategoryPlot plot = jfchart.getCategoryPlot();

    GroupedBarJitterRenderer renderer = new GroupedBarJitterRenderer(jitterValues);
    plot.setRenderer(renderer);

    // Color by group (series): Labeled, Unlabeled, Unknown → consistent palette colors
    for (int i = 0; i < dataset.getRowCount(); i++) {
      String groupKey = (String) dataset.getRowKey(i);
      Color c = groupColorMap.getOrDefault(groupKey, Color.GRAY);
      renderer.setSeriesPaint(i, c);
    }

    styleAxes(plot, selectedClusterIds);

    // Significance annotations: {categoryKey(isoKey), yPos, sigLabel}
    if (!significanceAnnotations.isEmpty()) {
      double globalMax = significanceAnnotations.stream()
          .mapToDouble(a -> (double) a[1]).max().orElse(0);
      ((NumberAxis) plot.getRangeAxis()).setUpperBound(globalMax * 1.25);
      Font sigFont = new Font("SansSerif", Font.BOLD, 14);
      for (Object[] ann : significanceAnnotations) {
        String catKey = (String) ann[0];
        double yPos = (double) ann[1] * 1.05;
        String sigLabel = (String) ann[2];
        CategoryTextAnnotation a = new CategoryTextAnnotation(sigLabel, catKey, yPos);
        a.setFont(sigFont);
        a.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        plot.addAnnotation(a);
      }
    }

    applyChartStyle(jfchart, groupNames, selectedClusterIds, clusterInfo, labeledFractions);
    return jfchart;
  }

  // ── Chart helpers ────────────────────────────────────────────────────────────────────────────

  private static String chartTitle(List<Integer> selectedClusterIds,
      Map<Integer, double[]> clusterInfo) {
    if (selectedClusterIds.size() == 1) {
      int id = selectedClusterIds.get(0);
      double[] info = clusterInfo.get(id);
      return info != null
          ? "Cluster " + id + "  |  m/z " + MZ_FORMAT.format(info[0]) + "  |  RT "
          + RT_FORMAT.format(info[1]) + " min"
          : "Isotopologue Distribution — Cluster " + id;
    }
    return "Isotopologue Distributions — " + selectedClusterIds.size() + " Clusters";
  }

  private static String yAxisLabel(String visualizationType, int normalizationRank) {
    if ("Relative intensities".equals(visualizationType)) {
      return normalizationRank >= 0
          ? "Relative Intensity (normalized to M+" + normalizationRank + ")"
          : "Relative Intensity (fraction of total)";
    }
    return "Intensity";
  }

  private static void styleAxes(CategoryPlot plot, List<Integer> selectedClusterIds) {
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setLowerMargin(0.05);
    domainAxis.setUpperMargin(0.05);
    if (selectedClusterIds.size() > 1) {
      domainAxis.setCategoryLabelPositions(
          CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4.0));
    }
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setAutoRange(true);
    rangeAxis.setAutoRangeIncludesZero(true);
  }

  private static void addStackedSignificanceAnnotations(CategoryPlot plot,
      DefaultCategoryDataset dataset, List<Object[]> significanceAnnotations) {
    if (significanceAnnotations.isEmpty()) {
      return;
    }
    double globalStackMax = 0;
    for (int col = 0; col < dataset.getColumnCount(); col++) {
      double colSum = 0;
      for (int row = 0; row < dataset.getRowCount(); row++) {
        Number v = dataset.getValue(row, col);
        if (v != null) {
          colSum += v.doubleValue();
        }
      }
      if (colSum > globalStackMax) {
        globalStackMax = colSum;
      }
    }
    ((NumberAxis) plot.getRangeAxis()).setUpperBound(globalStackMax * 1.25);

    Font sigFont = new Font("SansSerif", Font.BOLD, 14);
    for (Object[] ann : significanceAnnotations) {
      String catKey = (String) ann[0];
      String isoKey = (String) ann[1];
      String sigLabel = (String) ann[2];

      int colIdx = dataset.getColumnIndex(catKey);
      if (colIdx < 0) {
        continue;
      }
      double yPos = 0;
      for (int s = 0; s < dataset.getRowCount(); s++) {
        Number v = dataset.getValue(s, colIdx);
        if (v != null) {
          yPos += v.doubleValue();
        }
        if (dataset.getRowKey(s).equals(isoKey)) {
          break;
        }
      }
      CategoryTextAnnotation a = new CategoryTextAnnotation(sigLabel, catKey, yPos * 1.05);
      a.setFont(sigFont);
      a.setTextAnchor(TextAnchor.BOTTOM_CENTER);
      plot.addAnnotation(a);
    }
  }

  private static void applyChartStyle(JFreeChart jfchart, Set<String> groupNames,
      List<Integer> selectedClusterIds, Map<Integer, double[]> clusterInfo,
      Map<Integer, Double> labeledFractions) {

    jfchart.setBackgroundPaint(Color.WHITE);

    TextTitle groupSubtitle = new TextTitle("Groups: " + String.join(" vs ", groupNames));
    groupSubtitle.setFont(new Font("SansSerif", Font.ITALIC, 11));
    jfchart.addSubtitle(groupSubtitle);

    if (selectedClusterIds.size() == 1) {
      int id = selectedClusterIds.get(0);
      Double fraction = labeledFractions.get(id);
      if (fraction != null && fraction > 0) {
        TextTitle ft = new TextTitle(
            String.format("Fractional contribution: %.1f%%", fraction * 100));
        ft.setFont(new Font("SansSerif", Font.BOLD, 11));
        jfchart.addSubtitle(ft);
      }
    } else if (!clusterInfo.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Integer id : selectedClusterIds) {
        double[] info = clusterInfo.get(id);
        if (info != null) {
          if (sb.length() > 0) {
            sb.append("   |   ");
          }
          sb.append("C").append(id).append(": m/z ").append(MZ_FORMAT.format(info[0]))
              .append(", RT ").append(RT_FORMAT.format(info[1])).append(" min");
          Double fraction = labeledFractions.get(id);
          if (fraction != null && fraction > 0) {
            sb.append(String.format(", FC %.1f%%", fraction * 100));
          }
        }
      }
      if (sb.length() > 0) {
        TextTitle clusterSubtitle = new TextTitle(sb.toString());
        clusterSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
        jfchart.addSubtitle(clusterSubtitle);
      }
    }
  }

  @Override
  protected void updateGuiModel() {
    if (chart == null) {
      return;
    }
    model.setChart(chart);
    model.setProcessedData(processedData);
    model.setLabeledFractions(computedFractions);
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
