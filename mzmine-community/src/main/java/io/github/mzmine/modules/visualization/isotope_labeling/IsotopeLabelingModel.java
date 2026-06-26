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
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingModule;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingParameters;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
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
  // -1 = no normalization (fraction of total in relative mode), 0 = M+0, 1 = M+1, etc.
  private final IntegerProperty normalizationRank = new SimpleIntegerProperty(-1);
  private final IntegerProperty maxIsotopologues = new SimpleIntegerProperty(10);
  private final BooleanProperty showSignificanceMarkers = new SimpleBooleanProperty(true);
  // true = stacked bars (isotopologue layers per group), false = grouped bars (groups side-by-side per isotopologue)
  private final BooleanProperty stackedBars = new SimpleBooleanProperty(true);

  // Chart data
  private final ObjectProperty<JFreeChart> chart = new SimpleObjectProperty<>();
  private final ObjectProperty<Map<Integer, Map<String, double[]>>> processedData = new SimpleObjectProperty<>(
      new HashMap<>());
  // Fractional contribution per cluster — updated after each task run
  private final ObjectProperty<Map<Integer, Double>> labeledFractions = new SimpleObjectProperty<>(
      new HashMap<>());

  // User-selectable grouping column (null/empty = use the one from the applied method)
  private final StringProperty groupingColumnName = new SimpleStringProperty(null);

  // Labeling group identifiers extracted from the feature list's applied methods
  private String metadataColumnName = null;
  private String labeledGroupValue = null;
  private String unlabeledGroupValue = null;
  // Tracer mass difference per isotopologue step (at charge 1), e.g. 1.003355 for 13C
  private double tracerMassDiff = 1.003355;

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
   * Reads the UntargetedLabelingModule parameters stored in the feature list's applied methods and
   * caches the metadata column name and group values so that {@link #determineSampleGroups} can
   * classify files correctly.
   */
  public void extractLabelingParamsFromFeatureList(FeatureList featureList) {
    if (featureList == null) {
      return;
    }
    for (FeatureListAppliedMethod method : featureList.getAppliedMethods()) {
      if (method.getModule() instanceof UntargetedLabelingModule) {
        ParameterSet params = method.getParameters();
        metadataColumnName = params.getParameter(UntargetedLabelingParameters.metadataGrouping)
            .getValue();
        labeledGroupValue = params.getParameter(UntargetedLabelingParameters.labeledGroupValue)
            .getValue();
        unlabeledGroupValue = params.getParameter(UntargetedLabelingParameters.unlabeledGroupValue)
            .getValue();
        String tracer = params.getParameter(UntargetedLabelingParameters.tracerType).getValue();
        tracerMassDiff = lookupTracerMassDiff(tracer);
        logger.info("Extracted labeling params from feature list: column=" + metadataColumnName
            + ", labeled=" + labeledGroupValue + ", unlabeled=" + unlabeledGroupValue
            + ", tracer=" + tracer + " (" + tracerMassDiff + " Da)");
        return;
      }
    }
    logger.warning("Could not find UntargetedLabelingModule in applied methods of feature list "
        + featureList.getName() + ". Sample groups will be shown as Unknown.");
  }

  /**
   * Determines sample groups (Labeled / Unlabeled / Unknown) for a list of raw data files by
   * consulting the project metadata table with the column and values extracted during processing.
   *
   * @param files raw data files to classify
   * @return map of file name → group label
   */
  public Map<String, String> determineSampleGroups(List<RawDataFile> files) {
    Map<String, String> groups = new HashMap<>();

    // User override takes precedence; fall back to the column extracted from applied methods
    String effectiveColumn = groupingColumnName.get();
    if (effectiveColumn == null || effectiveColumn.isBlank()) {
      effectiveColumn = metadataColumnName;
    }

    MetadataTable metadata = ProjectService.getMetadata();
    MetadataColumn<?> column = effectiveColumn != null
        ? metadata.getColumnByName(effectiveColumn) : null;

    if (column == null) {
      // No column available — use "Labeled"/"Unlabeled" from the applied-method values if known,
      // or mark everything Unknown.
      if (metadataColumnName == null || labeledGroupValue == null || unlabeledGroupValue == null) {
        for (RawDataFile file : files) {
          groups.put(file.getName(), "Unknown");
        }
        return groups;
      }
      logger.warning("Metadata column '" + effectiveColumn + "' not found in project metadata");
      for (RawDataFile file : files) {
        groups.put(file.getName(), "Unknown");
      }
      return groups;
    }

    for (RawDataFile file : files) {
      Object value = metadata.getValue(column, file);
      if (value == null) {
        groups.put(file.getName(), "Unknown");
      } else {
        String strValue = value.toString().trim();
        // When using the original column, apply the known labeled/unlabeled mapping.
        // When using a user-selected column, every distinct value becomes its own group.
        if (labeledGroupValue != null && labeledGroupValue.equalsIgnoreCase(strValue)
            && (effectiveColumn.equals(metadataColumnName))) {
          groups.put(file.getName(), "Labeled");
        } else if (unlabeledGroupValue != null && unlabeledGroupValue.equalsIgnoreCase(strValue)
            && (effectiveColumn.equals(metadataColumnName))) {
          groups.put(file.getName(), "Unlabeled");
        } else {
          // For a user-selected grouping column, use the raw metadata value as group name
          groups.put(file.getName(), strValue);
        }
      }
    }

    return groups;
  }

  public double getTracerMassDiff() {
    return tracerMassDiff;
  }

  private static double lookupTracerMassDiff(String tracer) {
    if (tracer == null) {
      return 1.003355;
    }
    return switch (tracer.trim().toUpperCase()) {
      case "D", "2H" -> 1.006277;
      case "15N" -> 0.997035;
      case "17O" -> 1.004217;
      case "18O" -> 2.004244;
      case "34S" -> 1.995796;
      default -> 1.003355; // 13C
    };
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

  public int getNormalizationRank() {
    return normalizationRank.get();
  }

  public IntegerProperty normalizationRankProperty() {
    return normalizationRank;
  }

  public void setNormalizationRank(int rank) {
    this.normalizationRank.set(rank);
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

  public boolean getShowSignificanceMarkers() {
    return showSignificanceMarkers.get();
  }

  public BooleanProperty showSignificanceMarkersProperty() {
    return showSignificanceMarkers;
  }

  public void setShowSignificanceMarkers(boolean show) {
    this.showSignificanceMarkers.set(show);
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

  public Map<Integer, Double> getLabeledFractions() {
    return labeledFractions.get();
  }

  public ObjectProperty<Map<Integer, Double>> labeledFractionsProperty() {
    return labeledFractions;
  }

  public void setLabeledFractions(Map<Integer, Double> fractions) {
    this.labeledFractions.set(fractions);
  }

  public String getGroupingColumnName() {
    return groupingColumnName.get();
  }

  public StringProperty groupingColumnNameProperty() {
    return groupingColumnName;
  }

  public void setGroupingColumnName(String name) {
    this.groupingColumnName.set(name);
  }

  public boolean isStackedBars() {
    return stackedBars.get();
  }

  public BooleanProperty stackedBarsProperty() {
    return stackedBars;
  }

  public void setStackedBars(boolean stacked) {
    this.stackedBars.set(stacked);
  }
}