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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jfree.chart.JFreeChart;

/**
 * Model for isotope labeling visualization
 */
public class IsotopeLabelingModel {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingModel.class.getName());

  // Feature lists and selection
  private final ObjectProperty<List<FeatureList>> featureLists = new SimpleObjectProperty<>();
  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>(
      List.of());
  private final ObjectProperty<List<Integer>> selectedClusters = new SimpleObjectProperty<>(
      List.of());

  // Visualization properties
  private final StringProperty visualizationType = new SimpleStringProperty("Relative intensities");
  private final BooleanProperty normalizeToBaseIsotopologue = new SimpleBooleanProperty(false);
  private final IntegerProperty maxIsotopologues = new SimpleIntegerProperty(10);

  // Chart data
  private final ObjectProperty<JFreeChart> chart = new SimpleObjectProperty<>();
  private final ObjectProperty<Map<Integer, Map<String, double[]>>> processedData = new SimpleObjectProperty<>(
      new HashMap<>());

  /**
   * Extract isotope clusters from the feature list
   *
   * @param featureList Feature list containing isotope labeling results
   * @return Map of cluster IDs to lists of feature list rows
   */
  public Map<Integer, List<FeatureListRow>> extractIsotopeClusters(FeatureList featureList) {
    Map<Integer, List<FeatureListRow>> clusters = new HashMap<>();

    if (featureList == null) {
      return clusters;
    }

    // Check if feature list has the required types
    if (!featureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)
        || !featureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      logger.warning("Feature list does not have the required isotope labeling annotations");
      return clusters;
    }

    // Group rows by cluster ID
    for (FeatureListRow row : featureList.getRows()) {
      if (row instanceof ModularFeatureListRow modRow) {
        Integer clusterId = modRow.get(UntargetedLabelingParameters.isotopeClusterType);
        if (clusterId != null) {
          clusters.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(row);
        }
      }
    }

    // Sort rows within each cluster by isotopologue rank
    for (List<FeatureListRow> clusterRows : clusters.values()) {
      clusterRows.sort((r1, r2) -> {
        if (r1 instanceof ModularFeatureListRow modRow1
            && r2 instanceof ModularFeatureListRow modRow2) {
          Integer rank1 = modRow1.get(UntargetedLabelingParameters.isotopologueRankType);
          Integer rank2 = modRow2.get(UntargetedLabelingParameters.isotopologueRankType);

          if (rank1 != null && rank2 != null) {
            return Integer.compare(rank1, rank2);
          }
        }
        return 0;
      });
    }

    logger.info("Extracted " + clusters.size() + " isotope clusters from feature list");
    return clusters;
  }

  /**
   * Get clusters that have been selected
   *
   * @param allClusters Map of all isotope clusters
   * @return Map of selected clusters only
   */
  public Map<Integer, List<FeatureListRow>> getSelectedClusters(
      Map<Integer, List<FeatureListRow>> allClusters) {
    Map<Integer, List<FeatureListRow>> selected = new HashMap<>();

    if (selectedClusters.get() == null || selectedClusters.get().isEmpty()) {
      // No clusters selected, return an empty map
      return selected;
    }

    // Extract only the selected clusters
    for (Integer clusterId : selectedClusters.get()) {
      if (allClusters.containsKey(clusterId)) {
        selected.put(clusterId, allClusters.get(clusterId));
      }
    }

    return selected;
  }

  /**
   * Get isotope cluster ID for a row if available
   *
   * @param row Feature list row
   * @return Cluster ID or null if not part of a cluster
   */
  public static Integer getIsotopeClusterId(FeatureListRow row) {
    if (row instanceof ModularFeatureListRow modRow) {
      return modRow.get(UntargetedLabelingParameters.isotopeClusterType);
    }
    return null;
  }

  /**
   * Get isotopologue rank for a row if available
   *
   * @param row Feature list row
   * @return Isotopologue rank or null if not an isotopologue
   */
  public static Integer getIsotopologueRank(FeatureListRow row) {
    if (row instanceof ModularFeatureListRow modRow) {
      return modRow.get(UntargetedLabelingParameters.isotopologueRankType);
    }
    return null;
  }

  /**
   * Find the base peak (M+0) in a cluster
   *
   * @param clusterRows List of rows in a cluster
   * @return The base peak row or null if not found
   */
  public static FeatureListRow findBasePeak(List<FeatureListRow> clusterRows) {
    for (FeatureListRow row : clusterRows) {
      Integer rank = getIsotopologueRank(row);
      if (rank != null && rank == 0) {
        return row;
      }
    }

    // If no row with rank 0 is found, use the first row
    return clusterRows.isEmpty() ? null : clusterRows.get(0);
  }

  /**
   * Get sample groups based on file names
   *
   * @param files List of raw data files
   * @return Map of file names to group names
   */
  public static Map<String, String> determineSampleGroups(List<RawDataFile> files) {
    Map<String, String> groups = new HashMap<>();

    for (RawDataFile file : files) {
      String name = file.getName().toLowerCase();

      if (name.contains("unlabeled") || name.contains("control") || name.contains("c12")
          || name.contains("12c")) {
        groups.put(file.getName(), "Unlabeled");
      } else if (name.contains("labeled") || name.contains("c13") || name.contains("13c")
          || name.contains("treatment")) {
        groups.put(file.getName(), "Labeled");
      } else {
        groups.put(file.getName(), "Unknown");
      }
    }

    return groups;
  }

  // Getters and setters for the properties

  public List<FeatureList> getFeatureLists() {
    return featureLists.get();
  }

  public ObjectProperty<List<FeatureList>> featureListsProperty() {
    return featureLists;
  }

  public void setFeatureLists(List<FeatureList> featureLists) {
    this.featureLists.set(featureLists);
  }

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public void setSelectedRows(List<FeatureListRow> selectedRows) {
    this.selectedRows.set(selectedRows);

    // Update selected clusters based on selected rows
    if (selectedRows != null && !selectedRows.isEmpty()) {
      List<Integer> clusters = new ArrayList<>();

      for (FeatureListRow row : selectedRows) {
        Integer clusterId = getIsotopeClusterId(row);
        if (clusterId != null && !clusters.contains(clusterId)) {
          clusters.add(clusterId);
        }
      }

      if (!clusters.isEmpty()) {
        this.selectedClusters.set(clusters);
      }
    }
  }

  public List<Integer> getSelectedClusters() {
    return selectedClusters.get();
  }

  public ObjectProperty<List<Integer>> selectedClustersProperty() {
    return selectedClusters;
  }

  public void setSelectedClusters(List<Integer> selectedClusters) {
    this.selectedClusters.set(selectedClusters);
  }

  public String getVisualizationType() {
    return visualizationType.get();
  }

  public StringProperty visualizationTypeProperty() {
    return visualizationType;
  }

  public void setVisualizationType(String visualizationType) {
    this.visualizationType.set(visualizationType);
  }

  public boolean getNormalizeToBaseIsotopologue() {
    return normalizeToBaseIsotopologue.get();
  }

  public BooleanProperty normalizeToBaseIsotopologueProperty() {
    return normalizeToBaseIsotopologue;
  }

  public void setNormalizeToBaseIsotopologue(boolean normalizeToBaseIsotopologue) {
    this.normalizeToBaseIsotopologue.set(normalizeToBaseIsotopologue);
  }

  public int getMaxIsotopologues() {
    return maxIsotopologues.get();
  }

  public IntegerProperty maxIsotopologuesProperty() {
    return maxIsotopologues;
  }

  public void setMaxIsotopologues(int maxIsotopologues) {
    this.maxIsotopologues.set(maxIsotopologues);
  }

  public JFreeChart getChart() {
    return chart.get();
  }

  public ObjectProperty<JFreeChart> chartProperty() {
    return chart;
  }

  public void setChart(JFreeChart chart) {
    this.chart.set(chart);
  }

  public Map<Integer, Map<String, double[]>> getProcessedData() {
    return processedData.get();
  }

  public ObjectProperty<Map<Integer, Map<String, double[]>>> processedDataProperty() {
    return processedData;
  }

  public void setProcessedData(Map<Integer, Map<String, double[]>> processedData) {
    this.processedData.set(processedData);
  }
}