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

package io.github.mzmine.modules.dataprocessing.group_isotope_labeling_networking;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SimpleRowsRelationship;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingParameters;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Computes pairwise Isotopologue Compatibility Scores (ICS) between all isotope clusters in a
 * feature list and injects high-scoring pairs as network edges.
 */
public class IsotopeLabelingNetworkingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      IsotopeLabelingNetworkingTask.class.getName());

  public static final Type EDGE_TYPE = Type.ISOTOPE_LABELING_SIM;

  private final ModularFeatureList featureList;
  private final String metadataColumnName;
  private final String labeledGroupValue;
  private final double minICSScore;
  private final boolean useArea;

  private int processedPairs = 0;
  private int totalPairs = 0;

  public IsotopeLabelingNetworkingTask(@NotNull ParameterSet parameters,
      @NotNull ModularFeatureList featureList, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(null, moduleCallDate, parameters, moduleClass);
    this.featureList = featureList;
    this.metadataColumnName = parameters.getValue(
        IsotopeLabelingNetworkingParameters.metadataGrouping);
    this.labeledGroupValue = parameters.getValue(
        IsotopeLabelingNetworkingParameters.labeledGroupValue);
    this.minICSScore = parameters.getValue(IsotopeLabelingNetworkingParameters.minICSScore);
    String measure = parameters.getValue(IsotopeLabelingNetworkingParameters.intensityMeasure);
    this.useArea = "Area".equals(measure);
  }

  @Override
  public String getTaskDescription() {
    return "Isotope labeling networking on " + featureList.getName();
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  @Override
  protected void process() {
    List<RawDataFile> labeledFiles = getLabeledFiles();
    if (labeledFiles.isEmpty()) {
      logger.warning(
          "No labeled samples found for metadata column '" + metadataColumnName + "' with value '"
              + labeledGroupValue + "'. Aborting ICS networking.");
      return;
    }
    logger.info("Found %d labeled files for ICS networking.".formatted(labeledFiles.size()));

    // Group rows by cluster ID, recording each row's isotopologue rank
    Map<Integer, List<RankedRow>> clusterRows = new HashMap<>();
    for (FeatureListRow row : featureList.getRows()) {
      Integer clusterId = row.get(UntargetedLabelingParameters.isotopeClusterType);
      Integer rank = row.get(UntargetedLabelingParameters.isotopologueRankType);
      if (clusterId == null || rank == null) {
        continue;
      }
      clusterRows.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(new RankedRow(rank, row));
    }

    if (clusterRows.isEmpty()) {
      logger.info("No isotope cluster annotations found in " + featureList.getName()
          + ". Run Untargeted Isotope Labeling first.");
      return;
    }

    // Build normalized isotopologue distribution and representative (M+0) row per cluster
    Map<Integer, double[]> distributions = new HashMap<>();
    Map<Integer, Integer> maxRanks = new HashMap<>();
    Map<Integer, FeatureListRow> representatives = new HashMap<>();

    for (Map.Entry<Integer, List<RankedRow>> entry : clusterRows.entrySet()) {
      int clusterId = entry.getKey();
      List<RankedRow> ranked = entry.getValue();
      ranked.sort(Comparator.comparingInt(r -> r.rank));

      int maxRank = ranked.get(ranked.size() - 1).rank;
      maxRanks.put(clusterId, maxRank);
      representatives.put(clusterId, ranked.get(0).row);

      double[] dist = new double[maxRank + 1];
      for (RankedRow rr : ranked) {
        dist[rr.rank] = meanLabeledIntensity(rr.row, labeledFiles);
      }
      distributions.put(clusterId, dist);
    }

    // Compute pairwise ICS and collect edges above threshold
    List<Integer> clusterIds = new ArrayList<>(distributions.keySet());
    int n = clusterIds.size();
    totalPairs = n * (n - 1) / 2;

    R2RMap<RowsRelationship> edgeMap = new R2RMap<>();

    for (int i = 0; i < n - 1; i++) {
      if (isCanceled()) {
        return;
      }
      int idA = clusterIds.get(i);
      double[] distA = distributions.get(idA);
      int nA = maxRanks.get(idA);
      FeatureListRow repA = representatives.get(idA);

      for (int j = i + 1; j < n; j++) {
        int idB = clusterIds.get(j);
        double[] distB = distributions.get(idB);
        int nB = maxRanks.get(idB);
        FeatureListRow repB = representatives.get(idB);

        double ics = IsotopologueSimilarityCalculator.computeICS(distA, nA, distB, nB);

        if (ics >= minICSScore) {
          String annotation = buildAnnotation(idA, idB, nA, nB, ics);
          edgeMap.add(repA, repB,
              new SimpleRowsRelationship(repA, repB, ics, EDGE_TYPE.toString(), annotation));
        }
        processedPairs++;
      }
    }

    featureList.getRowMaps().addAllRowsRelationships(edgeMap, EDGE_TYPE);
    logger.info("Isotope labeling networking: added %d ICS edges (threshold %.2f) to %s".formatted(
        edgeMap.size(), minICSScore, featureList.getName()));
  }

  private List<RawDataFile> getLabeledFiles() {
    MetadataTable metadata = ProjectService.getMetadata();
    MetadataColumn<?> column = metadata.getColumnByName(metadataColumnName);
    if (column == null) {
      logger.warning("Metadata column '" + metadataColumnName + "' not found.");
      return List.of();
    }
    List<RawDataFile> labeled = new ArrayList<>();
    for (RawDataFile file : featureList.getRawDataFiles()) {
      Object val = metadata.getValue(column, file);
      if (val != null && labeledGroupValue.equalsIgnoreCase(val.toString().trim())) {
        labeled.add(file);
      }
    }
    return labeled;
  }

  private double meanLabeledIntensity(FeatureListRow row, List<RawDataFile> labeledFiles) {
    double sum = 0.0;
    int count = 0;
    for (RawDataFile file : labeledFiles) {
      Feature feature = row.getFeature(file);
      if (feature != null) {
        Float val = useArea ? feature.getArea() : feature.getHeight();
        if (val != null && val > 0) {
          sum += val;
          count++;
        }
      }
    }
    return count > 0 ? sum / count : 0.0;
  }

  private static String buildAnnotation(int idA, int idB, int nA, int nB, double ics) {
    int delta = Math.abs(nA - nB);
    String deltaStr = delta == 0 ? "same size" : "Δ" + delta + " atoms";
    return "Cluster %d (n=%d) ↔ Cluster %d (n=%d), %s, ICS=%.3f".formatted(idA, nA, idB, nB,
        deltaStr, ics);
  }

  @Override
  public double getFinishedPercentage() {
    return totalPairs > 0 ? (double) processedPairs / totalPairs : 0.0;
  }

  private record RankedRow(int rank, FeatureListRow row) {

  }
}
