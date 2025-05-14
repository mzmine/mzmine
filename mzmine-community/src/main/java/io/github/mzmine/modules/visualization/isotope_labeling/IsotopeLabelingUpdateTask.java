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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Update task for isotope labeling visualization, creating stacked bar charts
 */
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

    // Set progress total based on the number of selected clusters
    List<Integer> selectedClusters = model.getSelectedClusters();
    progress.setTotal(selectedClusters != null ? selectedClusters.size() : 0);
  }

  @Override
  public boolean checkPreConditions() {
    // Check if we have feature lists and clusters
    if (model.getFeatureLists() == null || model.getFeatureLists().isEmpty()) {
      logger.warning("No feature lists available");
      return false;
    }

    if (allClusters == null || allClusters.isEmpty()) {
      logger.warning("No isotope clusters available");
      return false;
    }

    // Check if any clusters are selected
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

    // Get selected clusters
    List<Integer> selectedClusterIds = model.getSelectedClusters();
    Map<Integer, List<FeatureListRow>> selectedClusters = new HashMap<>();

    for (Integer clusterId : selectedClusterIds) {
      if (allClusters.containsKey(clusterId)) {
        selectedClusters.put(clusterId, allClusters.get(clusterId));
      }
    }

    if (selectedClusters.isEmpty()) {
      logger.warning("No valid clusters selected");
      return;
    }

    // Set up color palette
    final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    colors.resetColorCounter();

    // Get visualization type
    String visualizationType = model.getVisualizationType();

    // We'll need to create a category dataset for the bar chart
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    // Process each selected cluster
    for (Map.Entry<Integer, List<FeatureListRow>> entry : selectedClusters.entrySet()) {
      if (isCanceled()) {
        return;
      }

      Integer clusterId = entry.getKey();
      List<FeatureListRow> rows = entry.getValue();

      // Skip empty clusters
      if (rows == null || rows.isEmpty()) {
        continue;
      }

      // Sort rows by isotopologue rank
      rows.sort((r1, r2) -> {
        Integer rank1 = IsotopeLabelingModel.getIsotopologueRank(r1);
        Integer rank2 = IsotopeLabelingModel.getIsotopologueRank(r2);
        return Integer.compare(rank1 != null ? rank1 : 0, rank2 != null ? rank2 : 0);
      });

      // Limit to max isotopologues
      int maxIsotopologues = model.getMaxIsotopologues();
      List<FeatureListRow> limitedRows =
          rows.size() > maxIsotopologues ? rows.subList(0, maxIsotopologues) : rows;

      // Get base feature info for the dataset name
      FeatureListRow baseRow = IsotopeLabelingModel.findBasePeak(rows);
      if (baseRow == null) {
        continue;
      }

      double baseMz = baseRow.getAverageMZ();
      double baseRt = baseRow.getAverageRT();

      // Get raw data files from the feature list
      List<RawDataFile> rawDataFiles = baseRow.getRawDataFiles();

      // Determine sample groups
      Map<String, String> sampleGroups = IsotopeLabelingModel.determineSampleGroups(rawDataFiles);

      // Group files by type
      Map<String, List<RawDataFile>> groupedFiles = new HashMap<>();
      for (RawDataFile file : rawDataFiles) {
        String group = sampleGroups.getOrDefault(file.getName(), "Unknown");
        groupedFiles.computeIfAbsent(group, k -> new ArrayList<>()).add(file);
      }

      // Create datasets based on visualization type
      if ("Relative intensities".equals(visualizationType)) {
        processRelativeIntensities(clusterId, limitedRows, groupedFiles, dataset, baseMz);
      } else if ("Absolute intensities".equals(visualizationType)) {
        processAbsoluteIntensities(clusterId, limitedRows, groupedFiles, dataset, baseMz);
      }

      progress.getAndIncrement();
    }

    // Create the chart with the dataset
    createStackedBarChart(dataset, visualizationType, selectedClusterIds);
  }

  /**
   * Process relative intensities for stacked bar chart
   */
  private void processRelativeIntensities(Integer clusterId, List<FeatureListRow> rows,
      Map<String, List<RawDataFile>> groupedFiles, DefaultCategoryDataset dataset, double baseMz) {

    FeatureListRow baseRow = IsotopeLabelingModel.findBasePeak(rows);
    boolean normalizeToBase = model.getNormalizeToBaseIsotopologue();
    Map<String, double[]> groupIntensities = new HashMap<>();

    // Create a unique category label for this cluster with m/z
    String clusterLabel =
        "Cluster " + clusterId + " (m/z " + MZ_FORMAT.format(baseMz) + ") at " + RT_FORMAT.format(
            baseRow.getAverageRT()) + " min";

    // For each sample group
    for (Map.Entry<String, List<RawDataFile>> entry : groupedFiles.entrySet()) {
      String groupName = entry.getKey();
      List<RawDataFile> groupFiles = entry.getValue();

      if (groupFiles.isEmpty()) {
        continue;
      }

      // Calculate relative intensities for this group
      double[][] relativeIntensities = calculateRelativeIntensities(rows, groupFiles,
          normalizeToBase);
      double[] meanValues = relativeIntensities[0];

      // Store mean values for this group
      groupIntensities.put(groupName, meanValues);

      // Add to dataset for each isotopologue
      for (int i = 0; i < meanValues.length; i++) {
        // For stacked bars, the isotopologue is the series (row key)
        // and the cluster is the category (column key)
        String isotopologueLabel = "M+" + i;

        // For each sample group, add a separate segment to the stacked bar
        if (groupName.equals("Unlabeled")) {
          dataset.addValue(meanValues[i], isotopologueLabel + " (Unlabeled)", clusterLabel);
        } else if (groupName.equals("Labeled")) {
          dataset.addValue(meanValues[i], isotopologueLabel + " (Labeled)", clusterLabel);
        } else {
          dataset.addValue(meanValues[i], isotopologueLabel + " (" + groupName + ")", clusterLabel);
        }
      }
    }

    // Store processed data for this cluster
    processedData.put(clusterId, groupIntensities);
  }

  /**
   * Process absolute intensities for stacked bar chart
   */
  private void processAbsoluteIntensities(Integer clusterId, List<FeatureListRow> rows,
      Map<String, List<RawDataFile>> groupedFiles, DefaultCategoryDataset dataset, double baseMz) {

    Map<String, double[]> groupIntensities = new HashMap<>();

    // Create a unique category label for this cluster with m/z
    String clusterLabel = "Cluster " + clusterId + " (m/z " + MZ_FORMAT.format(baseMz) + ")";

    // For each sample group
    for (Map.Entry<String, List<RawDataFile>> entry : groupedFiles.entrySet()) {
      String groupName = entry.getKey();
      List<RawDataFile> groupFiles = entry.getValue();

      if (groupFiles.isEmpty()) {
        continue;
      }

      // Calculate absolute intensities for this group
      double[][] absoluteIntensities = calculateAbsoluteIntensities(rows, groupFiles);
      double[] meanValues = absoluteIntensities[0];

      // Store mean values for this group
      groupIntensities.put(groupName, meanValues);

      // Add to dataset for each isotopologue
      for (int i = 0; i < meanValues.length; i++) {
        // For stacked bars, the isotopologue is the series (row key)
        // and the cluster is the category (column key)
        String isotopologueLabel = "M+" + i;

        // For each sample group, add a separate segment to the stacked bar
        if (groupName.equals("Unlabeled")) {
          dataset.addValue(meanValues[i], isotopologueLabel + " (Unlabeled)", clusterLabel);
        } else if (groupName.equals("Labeled")) {
          dataset.addValue(meanValues[i], isotopologueLabel + " (Labeled)", clusterLabel);
        } else {
          dataset.addValue(meanValues[i], isotopologueLabel + " (" + groupName + ")", clusterLabel);
        }
      }
    }

    // Store processed data for this cluster
    processedData.put(clusterId, groupIntensities);
  }

  /**
   * Calculate relative intensities for a list of rows and files
   */
  private double[][] calculateRelativeIntensities(List<FeatureListRow> rows,
      List<RawDataFile> files, boolean normalizeToBase) {

    int numIsotopologues = rows.size();
    FeatureListRow baseRow = normalizeToBase ? IsotopeLabelingModel.findBasePeak(rows) : null;

    // Arrays to store mean and std dev values
    double[] meanValues = new double[numIsotopologues];
    double[] stdDevValues = new double[numIsotopologues];

    // Process each isotopologue
    for (int i = 0; i < numIsotopologues; i++) {
      FeatureListRow row = rows.get(i);
      DescriptiveStatistics stats = new DescriptiveStatistics();

      // Calculate total intensity for normalization if not normalizing to base peak
      if (!normalizeToBase) {
        for (RawDataFile file : files) {
          double rowTotal = 0.0;

          // Calculate total for this sample
          for (FeatureListRow r : rows) {
            Feature feature = r.getFeature(file);
            if (feature != null) {
              rowTotal += feature.getHeight();
            }
          }

          // Calculate relative intensity
          Feature feature = row.getFeature(file);
          if (feature != null && rowTotal > 0) {
            stats.addValue(feature.getHeight() / rowTotal);
          }
        }
      } else {
        // Normalize to base peak
        for (RawDataFile file : files) {
          Feature baseFeature = baseRow.getFeature(file);
          Feature feature = row.getFeature(file);

          if (baseFeature != null && feature != null && baseFeature.getHeight() > 0) {
            stats.addValue(feature.getHeight() / baseFeature.getHeight());
          }
        }
      }

      // Store statistics
      meanValues[i] = stats.getMean();
      stdDevValues[i] = stats.getStandardDeviation();
    }

    return new double[][]{meanValues, stdDevValues};
  }

  /**
   * Calculate absolute intensities for a list of rows and files
   */
  private double[][] calculateAbsoluteIntensities(List<FeatureListRow> rows,
      List<RawDataFile> files) {
    int numIsotopologues = rows.size();

    // Arrays to store mean and std dev values
    double[] meanValues = new double[numIsotopologues];
    double[] stdDevValues = new double[numIsotopologues];

    // Process each isotopologue
    for (int i = 0; i < numIsotopologues; i++) {
      FeatureListRow row = rows.get(i);
      DescriptiveStatistics stats = new DescriptiveStatistics();

      // Calculate intensity for each file
      for (RawDataFile file : files) {
        Feature feature = row.getFeature(file);
        if (feature != null) {
          stats.addValue(feature.getHeight());
        }
      }

      // Store statistics
      meanValues[i] = stats.getMean();
      stdDevValues[i] = stats.getStandardDeviation();
    }

    return new double[][]{meanValues, stdDevValues};
  }

  /**
   * Create a stacked bar chart with the provided dataset
   */
  private void createStackedBarChart(DefaultCategoryDataset dataset, String visualizationType,
      List<Integer> selectedClusterIds) {

    // Determine chart title
    String title;
    if (selectedClusterIds.size() == 1) {
      title = "Isotope Labeling - Cluster " + selectedClusterIds.get(0);
    } else {
      title = "Isotope Labeling - Multiple clusters (" + selectedClusterIds.size() + ")";
    }

    // Determine Y axis label
    String yAxisLabel;
    if ("Relative intensities".equals(visualizationType)) {
      yAxisLabel = "Relative Intensity";
    } else {
      yAxisLabel = "Intensity";
    }

    // Create the chart
    chart = ChartFactory.createStackedBarChart(title,                      // chart title
        "Isotope Cluster",          // domain axis label
        yAxisLabel,                 // range axis label
        dataset,                    // data
        PlotOrientation.VERTICAL,   // orientation
        true,                       // include legend
        true,                       // tooltips
        false                       // URLs
    );

    // Customize the chart
    CategoryPlot plot = chart.getCategoryPlot();

    // Customize domain axis
    CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setCategoryMargin(0.2);  // increase gap between clusters
    domainAxis.setCategoryLabelPositions(
        CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

    // Customize bars
    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter());  // solid fill, not gradient
    renderer.setShadowVisible(false);
    renderer.setDrawBarOutline(true);

    // Customize colors based on isotopologues (M+0, M+1, etc.)
    SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    colors.resetColorCounter();

    // Group series by isotopologues (extract M+X from "M+X (group)")
    Map<String, List<Integer>> isotopologueGroups = new LinkedHashMap<>();
    for (int i = 0; i < dataset.getRowCount(); i++) {
      String seriesKey = (String) dataset.getRowKey(i);
      String baseIsotopologue = seriesKey.split(" \\(")[0]; // Extract M+X part

      isotopologueGroups.computeIfAbsent(baseIsotopologue, k -> new ArrayList<>()).add(i);
    }

    // Assign colors to each isotopologue group
    for (Map.Entry<String, List<Integer>> entry : isotopologueGroups.entrySet()) {
      Color baseColor = colors.getNextColorAWT();

      // For each occurrence of this isotopologue (different groups)
      for (int i = 0; i < entry.getValue().size(); i++) {
        int seriesIndex = entry.getValue().get(i);

        // Adjust color brightness based on whether it's labeled or unlabeled
        String seriesKey = (String) dataset.getRowKey(seriesIndex);
        if (seriesKey.contains("(Unlabeled)")) {
          // Make unlabeled slightly darker
          renderer.setSeriesPaint(seriesIndex, baseColor.darker());
        } else if (seriesKey.contains("(Labeled)")) {
          // Keep labeled as is
          renderer.setSeriesPaint(seriesIndex, baseColor);
        } else {
          // Make other groups slightly brighter
          renderer.setSeriesPaint(seriesIndex, baseColor.brighter());
        }
      }
    }

    // Set basic chart properties
    chart.setBackgroundPaint(java.awt.Color.WHITE);

    // Try to customize the legend - using try/catch to handle version differences
    try {
      if (chart.getLegend() != null) {
        // Just customize the font to be slightly smaller
        java.awt.Font smallerFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10);
        chart.getLegend().setItemFont(smallerFont);
      }
    } catch (Exception e) {
      // If there's any issue with customizing the legend, log and continue
      logger.log(Level.WARNING, "Could not customize chart legend", e);
    }

    // Add subtitle with information about normalization
    StringBuilder subtitle = new StringBuilder(visualizationType);
    if ("Relative intensities".equals(visualizationType)) {
      subtitle.append(model.getNormalizeToBaseIsotopologue() ? " (normalized to M+0)"
          : " (normalized to total)");
    }
    TextTitle subtitleText = new TextTitle(subtitle.toString());
    subtitleText.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 12));
    chart.addSubtitle(subtitleText);
  }

  @Override
  protected void updateGuiModel() {
    if (chart == null && !isFinished()) {
      return;
    }

    // Update the model with the chart
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