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

package io.github.mzmine.modules.dataprocessing.id_untargetedLabeling;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * Task for untargeted isotope labeling analysis. Analyzes aligned feature lists containing both
 * labeled and unlabeled samples to find isotopically enriched patterns.
 */
public class UntargetedLabelingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(UntargetedLabelingTask.class.getName());

  // ----- TASK VARIABLES -----
  private final MZmineProject project;
  private final FeatureList alignedFeatureList;
  private final MetadataColumn<?> metadataColumn;
  private final String unlabeledValue;
  private final String labeledValue;
  private ModularFeatureList resultFeatureList;
  private final ParameterSet parameters;
  private Map<String, Map<String, double[]>> dataIsotopes;

  // Progress tracking
  private int processedRows;
  private int totalRows;

  // ----- ANALYSIS PARAMETERS -----
  private String tracerElement;
  private int tracerIsotopeIndex;
  private double isotopeMassDifference;
  private RTTolerance rtTolerance;
  private MZTolerance mzTolerance;
  private int maximumIsotopologues;
  private int minIsotopePatternSize;
  private double noiseLevel;
  private String intensityMeasure;
  private double pValueCutoff;
  private boolean singleSample;
  private double monotonicityTolerance;
  private double enrichmentTolerance;
  private String suffix;
  private boolean allowIncompletePatterns;

  // Results storage
  private List<IsotopeGroupResult> isotopeLabelResults;

  /**
   * Constructor for the untargeted labeling task.
   */
  public UntargetedLabelingTask(MZmineProject project, FeatureList alignedFeatureList,
      String metadataColumnName, String unlabeledValue, String labeledValue,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.alignedFeatureList = alignedFeatureList;
    this.unlabeledValue = unlabeledValue;
    this.labeledValue = labeledValue;
    this.parameters = parameters;

    // Get metadata column
    MetadataTable metadata = MZmineCore.getProjectMetadata();
    this.metadataColumn = metadata.getColumnByName(metadataColumnName);

    // Initialize isotope data first
    this.dataIsotopes = getDefaultIsotopicData();

    // Initialize parameters from the parameter set
    initializeParameters();

    // Initialize empty result lists
    isotopeLabelResults = new ArrayList<>();
  }

  /**
   * Initialize all task parameters from the parameter set
   */
  private void initializeParameters() {
    // Get tracer information - parse the tracer string
    String tracerStr = parameters.getParameter(UntargetedLabelingParameters.tracerType).getValue();
    parseTracer(tracerStr);

    // Now calculate the isotope mass difference
    isotopeMassDifference = calculateIsotopeMassDifference();

    // Get the rest of the parameters
    rtTolerance = parameters.getParameter(UntargetedLabelingParameters.rtTolerance).getValue();
    mzTolerance = parameters.getParameter(UntargetedLabelingParameters.mzTolerance).getValue();
    maximumIsotopologues = parameters.getParameter(
        UntargetedLabelingParameters.maximumIsotopologues).getValue();
    minIsotopePatternSize = parameters.getParameter(
        UntargetedLabelingParameters.minimumIsotopePatternSize).getValue();
    noiseLevel = parameters.getParameter(UntargetedLabelingParameters.noiseLevel).getValue();
    intensityMeasure = parameters.getParameter(UntargetedLabelingParameters.intensityMeasure)
        .getValue();
    pValueCutoff = parameters.getParameter(UntargetedLabelingParameters.pValueCutoff).getValue();
    singleSample = parameters.getParameter(UntargetedLabelingParameters.singleSample).getValue();
    monotonicityTolerance = parameters.getParameter(
        UntargetedLabelingParameters.monotonicityTolerance).getValue();
    enrichmentTolerance = parameters.getParameter(UntargetedLabelingParameters.enrichmentTolerance)
        .getValue();
    suffix = parameters.getParameter(UntargetedLabelingParameters.suffix).getValue();
    allowIncompletePatterns = parameters.getParameter(
        UntargetedLabelingParameters.allowIncompletePatterns).getValue();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting untargeted isotope labeling analysis on " + alignedFeatureList.getName());

    // Validate inputs
    if (!validateInputs()) {
      return;
    }

    // Initialize the result feature list
    initializeResultFeatureList();

    // Process features to find isotope patterns
    processFeatures();

    if (!isotopeLabelResults.isEmpty()) {
      consolidateIsotopeClusters();
    }

    // Finalize and add to project
    finalizeResults();

    logger.info("Finished untargeted isotope labeling analysis, found " + isotopeLabelResults.size()
        + " isotopically enriched groups");
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Process features to find isotope patterns using FeatureListUtils
   */
  private void processFeatures() {
    logger.info("Finding isotopologue groups using FeatureListUtils");

    // Get all rows sorted by m/z for efficiency
    List<FeatureListRow> allRows = new ArrayList<>(alignedFeatureList.getRows());
    allRows.sort(Comparator.comparingDouble(FeatureListRow::getAverageMZ));

    // Track which rows have been used as isotopologues
    Set<Integer> usedRowIds = new HashSet<>();

    // Find isotopologue groups
    List<List<IsotopeCandidate>> allGroups = new ArrayList<>();

    totalRows = allRows.size();
    processedRows = 0;

    for (FeatureListRow baseRow : allRows) {
      if (isCanceled()) {
        return;
      }

      processedRows++;

      // Skip if already used as an isotopologue
      if (usedRowIds.contains(baseRow.getID())) {
        continue;
      }

      // Skip if below noise level
      if (getAverageIntensity(baseRow) < noiseLevel) {
        continue;
      }

      // Find isotopologue pattern starting from this base peak
      List<IsotopeCandidate> pattern = findIsotopologuePattern(baseRow, allRows, usedRowIds);

      if (pattern.size() >= minIsotopePatternSize) {
        allGroups.add(pattern);

        // Mark all except base peak as used
        for (int i = 1; i < pattern.size(); i++) {
          usedRowIds.add(pattern.get(i).row.getID());
        }
      }
    }

    logger.info("Found " + allGroups.size() + " potential isotopologue groups");

    // Analyze each group
    for (List<IsotopeCandidate> group : allGroups) {
      if (isCanceled()) {
        return;
      }

      IsotopeCandidate baseCandidate = group.get(0);
      analyzeAndAnnotateIsotopePattern(group, baseCandidate.row.getAverageMZ(),
          baseCandidate.row.getAverageRT());
    }
  }

  /**
   * Find isotopologue pattern starting from a base peak using FeatureListUtils
   */
  private List<IsotopeCandidate> findIsotopologuePattern(FeatureListRow basePeak,
      List<FeatureListRow> allRows, Set<Integer> usedRowIds) {

    List<IsotopeCandidate> pattern = new ArrayList<>();
    pattern.add(new IsotopeCandidate(basePeak, 0));

    double baseMz = basePeak.getAverageMZ();
    double baseRt = basePeak.getAverageRT();

    // Define RT range for isotopologues
    Range<Float> rtRange = rtTolerance.getToleranceRange((float) baseRt);

    // Search for each potential isotopologue
    for (int massShift = 1; massShift <= maximumIsotopologues; massShift++) {
      double expectedMz = baseMz + (massShift * isotopeMassDifference);

      // Define m/z range for this isotopologue
      Range<Double> mzRange = mzTolerance.getToleranceRange(expectedMz);

      // Use FeatureListUtils to find candidates
      List<FeatureListRow> candidates = FeatureListUtils.getCandidatesWithinRanges(mzRange, rtRange,
          null, allRows, true);

      // Filter out already used rows and find best match
      FeatureListRow bestMatch = null;
      double bestScore = Double.MAX_VALUE;

      for (FeatureListRow candidate : candidates) {
        if (usedRowIds.contains(candidate.getID())) {
          continue;
        }

        // Calculate score based on m/z error and intensity
        double mzError = Math.abs(candidate.getAverageMZ() - expectedMz);
        double ppmError = (mzError / baseMz) * 1e6;

        // Basic intensity check if not allowing incomplete patterns
        if (!allowIncompletePatterns) {
          double candidateIntensity = getAverageIntensity(candidate);
          double baseIntensity = getAverageIntensity(basePeak);

          // Skip if isotopologue is more intense than base peak in unlabeled samples
          double unlabeledRatio = getIntensityRatio(candidate, basePeak, false);
          if (unlabeledRatio > 1.5) {
            continue;
          }
        }

        double score = ppmError;

        if (score < bestScore) {
          bestScore = score;
          bestMatch = candidate;
        }
      }

      if (bestMatch != null) {
        pattern.add(new IsotopeCandidate(bestMatch, massShift));
      } else if (!allowIncompletePatterns) {
        // Stop searching if we can't find a consecutive isotopologue
        break;
      }
    }

    return pattern;
  }

  /**
   * Get intensity ratio between two features for a specific sample group
   */
  private double getIntensityRatio(FeatureListRow row1, FeatureListRow row2, boolean labeled) {
    double[] intensities1 = getIntensitiesForGroup(row1, labeled);
    double[] intensities2 = getIntensitiesForGroup(row2, labeled);

    double avg1 = Arrays.stream(intensities1).average().orElse(0);
    double avg2 = Arrays.stream(intensities2).average().orElse(0);

    if (avg2 == 0) {
      return Double.POSITIVE_INFINITY;
    }

    return avg1 / avg2;
  }

  /**
   * Get intensities for a feature row in either unlabeled or labeled samples
   */
  private double[] getIntensitiesForGroup(FeatureListRow row, boolean labeled) {
    List<Double> intensities = new ArrayList<>();

    for (RawDataFile file : alignedFeatureList.getRawDataFiles()) {
      String metadataValue = getMetadataValue(file);

      if (metadataValue != null) {
        boolean isLabeledSample = labeledValue.equalsIgnoreCase(metadataValue.trim());

        if (isLabeledSample == labeled) {
          Feature feature = row.getFeature(file);
          if (feature != null) {
            intensities.add(getFeatureIntensity(feature));
          } else {
            intensities.add(0.0);
          }
        }
      }
    }

    return intensities.stream().mapToDouble(Double::doubleValue).toArray();
  }

  /**
   * Analyze an isotope pattern and annotate the feature list
   */
  private void analyzeAndAnnotateIsotopePattern(List<IsotopeCandidate> isotopologues, double baseMz,
      double baseRt) {

    IsotopeGroupResult result = analyzeIsotopePattern(isotopologues);

    if (result != null) {
      // Calculate the cluster ID
      int clusterID = isotopeLabelResults.size() + 1;
      result.clusterId = clusterID;

      isotopeLabelResults.add(result);

      // Annotate all isotopologue rows in the result feature list
      annotateIsotopologueRows(isotopologues, result, clusterID, baseMz, baseRt);

      logger.info("Created isotope cluster #" + clusterID + " with " + isotopologues.size()
          + " isotopologues at m/z=" + baseMz + ", RT=" + baseRt);
    }
  }

  /**
   * Analyze an isotope pattern to determine if it represents true labeling
   */
  private IsotopeGroupResult analyzeIsotopePattern(List<IsotopeCandidate> isotopologues) {
    if (isCanceled()) {
      return null;
    }

    logger.fine("Analyzing isotope pattern with " + isotopologues.size() + " isotopologues");

    // Make sure the list is sorted by mass shift
    Collections.sort(isotopologues, Comparator.comparingInt(c -> c.massShift));

    // Basic validation
    if (isotopologues.size() < minIsotopePatternSize) {
      return null;
    }

    // Extract intensity data
    IsotopeIntensityData intensityData = extractIsotopologueIntensities(isotopologues);

    // Apply quality filters
    if (!applyQualityFilters(isotopologues, intensityData)) {
      return null;
    }

    // Check pattern validity
    if (!validateIsotopePattern(intensityData)) {
      return null;
    }

    // Calculate statistics and enrichment ratios
    IsotopeGroupResult result = calculateEnrichmentStatistics(isotopologues, intensityData);

    // Check statistical significance
    if (!isSingleSampleSignificant(result)) {
      return null;
    }

    return result;
  }

  /**
   * Apply quality filters to isotopologue groups
   */
  private boolean applyQualityFilters(List<IsotopeCandidate> isotopologues,
      IsotopeIntensityData data) {

    // 1. Check if base peak is above noise threshold
    FeatureListRow basePeak = isotopologues.get(0).row;
    double baseIntensity = getAverageIntensity(basePeak);
    if (baseIntensity < noiseLevel) {
      logger.fine("Base peak below noise threshold");
      return false;
    }

    // 2. Check if we have sufficient samples with signal
    int unlabeledWithSignal = 0;
    int labeledWithSignal = 0;

    for (double total : data.unlabeledTotals) {
      if (total > noiseLevel) {
        unlabeledWithSignal++;
      }
    }

    for (double total : data.labeledTotals) {
      if (total > noiseLevel) {
        labeledWithSignal++;
      }
    }

    // Require at least half of samples to have signal
    double unlabeledRatio =
        data.unlabeledTotals.length > 0 ? (double) unlabeledWithSignal / data.unlabeledTotals.length
            : 0;
    double labeledRatio =
        data.labeledTotals.length > 0 ? (double) labeledWithSignal / data.labeledTotals.length : 0;

    if (unlabeledRatio < 0.5 || labeledRatio < 0.5) {
      logger.fine("Insufficient samples with signal");
      return false;
    }

    // 3. Check for valid relative intensities
    boolean hasValidRelativeIntensities = false;
    for (double relInt : data.meanUnlabeledRelIntensities) {
      if (relInt > 0 && !Double.isNaN(relInt) && !Double.isInfinite(relInt)) {
        hasValidRelativeIntensities = true;
        break;
      }
    }

    if (!hasValidRelativeIntensities) {
      logger.fine("No valid relative intensities");
      return false;
    }

    return true;
  }

  /**
   * Consolidate overlapping isotope clusters
   */
  private void consolidateIsotopeClusters() {
    if (isotopeLabelResults.isEmpty()) {
      return;
    }

    logger.info("Consolidating overlapping isotope clusters...");

    // Sort clusters by m/z
    Collections.sort(isotopeLabelResults, Comparator.comparingDouble(r -> r.baseMz));

    // Initialize cluster IDs
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      isotopeLabelResults.get(i).clusterId = i + 1;
    }

    // Track which clusters have been merged
    Set<Integer> mergedClusterIndices = new HashSet<>();

    // Build feature-to-cluster mapping
    Map<Integer, List<Integer>> featureIdToClusterIndices = new HashMap<>();
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      IsotopeGroupResult cluster = isotopeLabelResults.get(i);
      for (IsotopeCandidate candidate : cluster.isotopologues) {
        int featureId = candidate.row.getID();
        featureIdToClusterIndices.computeIfAbsent(featureId, k -> new ArrayList<>()).add(i);
      }
    }

    // Identify clusters that should be merged
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      if (mergedClusterIndices.contains(i)) {
        continue;
      }

      IsotopeGroupResult currentCluster = isotopeLabelResults.get(i);
      Set<Integer> clustersToMerge = new HashSet<>();

      // Check for overlapping features
      for (IsotopeCandidate candidate : currentCluster.isotopologues) {
        int featureId = candidate.row.getID();
        List<Integer> overlappingClusterIndices = featureIdToClusterIndices.getOrDefault(featureId,
            Collections.emptyList());

        for (int overlappingIndex : overlappingClusterIndices) {
          if (overlappingIndex != i && !mergedClusterIndices.contains(overlappingIndex)) {
            IsotopeGroupResult otherCluster = isotopeLabelResults.get(overlappingIndex);

            // Check if clusters should be merged
            if (shouldMergeClusters(currentCluster, otherCluster)) {
              clustersToMerge.add(overlappingIndex);
            }
          }
        }
      }

      // Merge all identified clusters
      for (int clusterIndex : clustersToMerge) {
        IsotopeGroupResult clusterToMerge = isotopeLabelResults.get(clusterIndex);
        mergeIsotopeClusters(currentCluster, clusterToMerge);
        mergedClusterIndices.add(clusterIndex);
      }
    }

    // Create new list with only non-merged clusters
    List<IsotopeGroupResult> mergedResults = new ArrayList<>();
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      if (!mergedClusterIndices.contains(i)) {
        mergedResults.add(isotopeLabelResults.get(i));
      }
    }

    // Replace original results with merged results
    if (mergedResults.size() < isotopeLabelResults.size()) {
      logger.info(
          "Consolidated " + isotopeLabelResults.size() + " clusters into " + mergedResults.size()
              + " clusters");
      isotopeLabelResults = mergedResults;
      updateClusterIds();
    }
  }

  /**
   * Determine if two clusters should be merged
   */
  private boolean shouldMergeClusters(IsotopeGroupResult cluster1, IsotopeGroupResult cluster2) {
    // Check RT tolerance
    if (!rtTolerance.checkWithinTolerance((float) cluster1.baseRt, (float) cluster2.baseRt)) {
      return false;
    }

    // Calculate mass difference in isotope units
    double massRatio = (cluster2.baseMz - cluster1.baseMz) / isotopeMassDifference;
    int massShiftDiff = (int) Math.round(massRatio);

    // Direct isotope relationship
    if (Math.abs(massRatio - massShiftDiff) <= 0.2
        && Math.abs(massShiftDiff) <= maximumIsotopologues / 2) {
      return true;
    }

    // Check for shared features
    Set<Integer> featureIds1 = cluster1.isotopologues.stream().map(c -> c.row.getID())
        .collect(Collectors.toSet());

    Set<Integer> featureIds2 = cluster2.isotopologues.stream().map(c -> c.row.getID())
        .collect(Collectors.toSet());

    Set<Integer> intersection = new HashSet<>(featureIds1);
    intersection.retainAll(featureIds2);

    return intersection.size() >= 2;
  }

  /**
   * Merge two isotope clusters
   */
  private void mergeIsotopeClusters(IsotopeGroupResult mainCluster,
      IsotopeGroupResult secondaryCluster) {

    // Calculate isotope difference between clusters
    double mzDifference = secondaryCluster.baseMz - mainCluster.baseMz;
    int isotopeDifference = (int) Math.round(mzDifference / isotopeMassDifference);

    // Add isotopologues from secondary cluster
    for (IsotopeCandidate candidate : secondaryCluster.isotopologues) {
      int adjustedMassShift = candidate.massShift;

      // Adjust mass shift if necessary
      if (Math.abs(isotopeDifference) > 0) {
        adjustedMassShift = candidate.massShift + isotopeDifference;
      }

      // Skip if exceeds maximum
      if (adjustedMassShift > maximumIsotopologues || adjustedMassShift < 0) {
        continue;
      }

      // Add if not already present
      boolean exists = mainCluster.isotopologues.stream()
          .anyMatch(existing -> existing.row.getID() == candidate.row.getID());

      if (!exists) {
        mainCluster.isotopologues.add(new IsotopeCandidate(candidate.row, adjustedMassShift));
      }
    }

    // Re-sort by mass shift
    Collections.sort(mainCluster.isotopologues, Comparator.comparingInt(c -> c.massShift));
  }

  /**
   * Update cluster IDs after consolidation
   */
  private void updateClusterIds() {
    // Clear existing assignments
    for (FeatureListRow row : resultFeatureList.getRows()) {
      ModularFeatureListRow modRow = (ModularFeatureListRow) row;
      modRow.set(UntargetedLabelingParameters.isotopeClusterType, null);
    }

    // Assign new cluster IDs
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      IsotopeGroupResult cluster = isotopeLabelResults.get(i);
      int newClusterId = i + 1;
      cluster.clusterId = newClusterId;

      // Assign to all isotopologues in this cluster
      for (IsotopeCandidate candidate : cluster.isotopologues) {
        ModularFeatureListRow resultRow = findResultRowByFeatureId(candidate.row.getID());
        if (resultRow != null) {
          resultRow.set(UntargetedLabelingParameters.isotopeClusterType, newClusterId);
          resultRow.set(UntargetedLabelingParameters.isotopologueRankType, candidate.massShift);
          updateRowComment(resultRow, candidate, cluster, newClusterId);
        }
      }
    }

    logger.info("Updated cluster IDs for " + isotopeLabelResults.size() + " clusters");
  }

  /**
   * Get metadata value for a raw data file
   */
  private String getMetadataValue(RawDataFile file) {
    if (metadataColumn == null) {
      logger.warning("Metadata column is null. Cannot access metadata values.");
      return null;
    }

    // Get metadata table and retrieve value for this raw data file
    MetadataTable metadata = MZmineCore.getProjectMetadata();
    Object value = metadata.getValue(metadataColumn, file);
    return value != null ? value.toString() : null;
  }

  /**
   * Calculate isotope mass difference based on the loaded isotope data
   */
  private double calculateIsotopeMassDifference() {
    logger.info("Computing isotope mass difference for " + tracerElement);

    if (!dataIsotopes.containsKey(tracerElement)) {
      throw new IllegalArgumentException("Element " + tracerElement + " not found in isotope data");
    }

    double[] masses = dataIsotopes.get(tracerElement).get("mass");

    if (tracerIsotopeIndex >= masses.length || tracerIsotopeIndex < 0) {
      throw new IllegalArgumentException("Invalid tracer isotope index: " + tracerIsotopeIndex);
    }

    // Calculate the mass difference between the tracer isotope and the most abundant isotope
    double difference = masses[tracerIsotopeIndex] - masses[0];
    logger.info("Isotope mass difference calculated: " + difference);
    return difference;
  }

  /**
   * Parse the tracer string to extract the element and isotope index.
   */
  private void parseTracer(String tracerStr) {
    logger.info("Parsing tracer: " + tracerStr);

    try {
      int tracerMass;

      // Check if format is "13C" (number first)
      java.util.regex.Pattern numberFirstPattern = java.util.regex.Pattern.compile(
          "([0-9]+)([A-Z][a-z]*)");
      java.util.regex.Matcher numberFirstMatcher = numberFirstPattern.matcher(tracerStr);

      if (numberFirstMatcher.matches()) {
        tracerMass = Integer.parseInt(numberFirstMatcher.group(1));
        this.tracerElement = numberFirstMatcher.group(2);
        logger.info(
            "Matched 'number-first' format: element=" + tracerElement + ", mass=" + tracerMass);
      } else {
        // Check if format is "C13" (element first)
        java.util.regex.Pattern elementFirstPattern = java.util.regex.Pattern.compile(
            "([A-Z][a-z]*)([0-9]+)");
        java.util.regex.Matcher elementFirstMatcher = elementFirstPattern.matcher(tracerStr);

        if (elementFirstMatcher.matches()) {
          this.tracerElement = elementFirstMatcher.group(1);
          tracerMass = Integer.parseInt(elementFirstMatcher.group(2));
          logger.info(
              "Matched 'element-first' format: element=" + tracerElement + ", mass=" + tracerMass);
        } else {
          throw new IllegalArgumentException(
              "Invalid tracer format: " + tracerStr + ". Expected formats are '13C' or 'C13'.");
        }
      }

      // Check if we have isotope data for this element
      if (!dataIsotopes.containsKey(tracerElement)) {
        logger.info("Tracer element " + tracerElement
            + " not found in current isotope data. Attempting to load specific isotope data.");

        // Try to load data specifically for this element
        Map<String, Map<String, double[]>> specificData = loadSpecificElementIsotopeData(
            tracerElement);

        // If we got data for this element, add it to our existing data
        if (specificData.containsKey(tracerElement)) {
          dataIsotopes.put(tracerElement, specificData.get(tracerElement));
          logger.info("Successfully loaded isotope data for " + tracerElement);
        } else {
          throw new IllegalArgumentException(
              "Tracer element not found in isotope data: " + tracerElement);
        }
      }

      // Find the closest isotope by mass
      double[] masses = dataIsotopes.get(tracerElement).get("mass");
      double bestDiff = Double.POSITIVE_INFINITY;

      for (int i = 0; i < masses.length; i++) {
        double diff = Math.abs(masses[i] - tracerMass);
        if (diff < bestDiff) {
          bestDiff = diff;
          tracerIsotopeIndex = i;
        }
      }

      if (bestDiff > 0.5) {
        throw new IllegalArgumentException(
            "No matching isotope found for tracer: " + tracerStr + ". Closest mass difference was "
                + bestDiff);
      }

      logger.info("Parsed tracer: element=" + tracerElement + ", isotopeIndex=" + tracerIsotopeIndex
          + ", isotope mass=" + masses[tracerIsotopeIndex]);

    } catch (Exception e) {
      logger.severe("Error parsing tracer '" + tracerStr + "': " + e.getMessage());
      throw new IllegalArgumentException("Failed to parse tracer: " + tracerStr, e);
    }
  }

  /**
   * Load isotope data specifically for the given element using CDK's IsotopeFactory.
   *
   * @param element The chemical element to load data for
   * @return A map containing isotope data for the requested element
   */
  public Map<String, Map<String, double[]>> loadSpecificElementIsotopeData(String element) {
    logger.info("Loading specific isotope data for element: " + element);

    Map<String, Map<String, double[]>> isotopes = new HashMap<>();

    try {
      // Initialize CDK's IsotopeFactory
      IsotopeFactory isotopeFactory = Isotopes.getInstance();

      // Get all isotopes for the requested element
      IIsotope[] elementIsotopes = isotopeFactory.getIsotopes(element);

      if (elementIsotopes == null || elementIsotopes.length == 0) {
        logger.warning("No isotopes found for element: " + element);
        return isotopes; // Return empty map
      }

      // Filter and collect isotopes with abundance > 0
      List<IIsotope> validIsotopes = new ArrayList<>();
      for (IIsotope isotope : elementIsotopes) {
        if (isotope.getNaturalAbundance() != null && isotope.getNaturalAbundance() > 0.0) {
          validIsotopes.add(isotope);
        }
      }

      // Sort isotopes by mass number
      Collections.sort(validIsotopes, Comparator.comparingInt(IIsotope::getMassNumber));

      // Skip if no isotopes with abundance data were found
      if (validIsotopes.isEmpty()) {
        logger.warning("No isotopes with abundance data found for element: " + element);
        return isotopes; // Return empty map
      }

      // Create arrays for abundance and mass
      double[] abundances = new double[validIsotopes.size()];
      double[] masses = new double[validIsotopes.size()];

      // Fill arrays with data
      for (int i = 0; i < validIsotopes.size(); i++) {
        IIsotope isotope = validIsotopes.get(i);
        // CDK stores abundance as percentage (0-100), convert to fraction (0-1)
        abundances[i] = isotope.getNaturalAbundance() / 100.0;
        masses[i] = isotope.getExactMass();
      }

      // Store data in the map
      isotopes.put(element, new HashMap<>());
      isotopes.get(element).put("abundance", abundances);
      isotopes.get(element).put("mass", masses);

      logger.info("Loaded specific isotope data for " + element + ": " + validIsotopes.size()
          + " isotopes");

    } catch (java.io.IOException e) {
      logger.warning("Error loading specific isotope data for " + element + ": " + e.getMessage());
    } catch (Exception e) {
      logger.warning("Unexpected error when loading specific isotope data for " + element + ": "
          + e.getMessage());
    }

    return isotopes;
  }

  /**
   * Retrieves isotopic data for common elements using CDK's IsotopeFactory. If CDK data retrieval
   * fails, falls back to hardcoded values.
   *
   * @return A map containing isotopic data (abundance and mass) for each element
   */
  private Map<String, Map<String, double[]>> getDefaultIsotopicData() {
    logger.info("Loading isotopic data from CDK.");

    Map<String, Map<String, double[]>> isotopes = new HashMap<>();

    try {
      // Initialize CDK's IsotopeFactory
      IsotopeFactory isotopeFactory = Isotopes.getInstance();

      // Define elements of interest
      String[] elements = {"C", "H", "N", "P", "O", "S", "Si"};

      for (String element : elements) {
        // Get all isotopes for the current element
        IIsotope[] elementIsotopes = isotopeFactory.getIsotopes(element);

        // Filter and collect isotopes with abundance > 0
        List<IIsotope> validIsotopes = new ArrayList<>();
        for (IIsotope isotope : elementIsotopes) {
          if (isotope.getNaturalAbundance() != null && isotope.getNaturalAbundance() > 0.0) {
            validIsotopes.add(isotope);
          }
        }

        // Sort isotopes by mass number
        Collections.sort(validIsotopes, Comparator.comparingInt(IIsotope::getMassNumber));

        // Skip if no isotopes with abundance data were found
        if (validIsotopes.isEmpty()) {
          logger.warning("No isotopes with abundance data found for element: " + element);
          continue;
        }

        // Create arrays for abundance and mass
        double[] abundances = new double[validIsotopes.size()];
        double[] masses = new double[validIsotopes.size()];

        // Fill arrays with data
        for (int i = 0; i < validIsotopes.size(); i++) {
          IIsotope isotope = validIsotopes.get(i);
          // CDK stores abundance as percentage (0-100), convert to fraction (0-1)
          abundances[i] = isotope.getNaturalAbundance() / 100.0;
          masses[i] = isotope.getExactMass();
        }

        // Store data in the map
        isotopes.put(element, new HashMap<>());
        isotopes.get(element).put("abundance", abundances);
        isotopes.get(element).put("mass", masses);

        logger.info(
            "Loaded isotopic data for " + element + ": " + validIsotopes.size() + " isotopes");
      }

      logger.info("Successfully loaded isotopic data from CDK.");
      return isotopes;

    } catch (java.io.IOException e) {
      logger.warning("Error loading isotope data from CDK: " + e.getMessage());
      // Fallback to hardcoded data if CDK fails
      return getFallbackIsotopicData();
    } catch (Exception e) {
      logger.warning("Unexpected error when loading isotope data from CDK: " + e.getMessage());
      // Fallback if any other error occurs
      return getFallbackIsotopicData();
    }
  }

  /**
   * Provides hardcoded isotopic data as a fallback when CDK is unavailable.
   */
  private Map<String, Map<String, double[]>> getFallbackIsotopicData() {
    logger.info("Loading fallback isotopic data.");

    Map<String, Map<String, double[]>> isotopes = new HashMap<>();

    // Carbon isotopes
    isotopes.put("C", new HashMap<>());
    isotopes.get("C").put("abundance", new double[]{0.9893, 0.0107});
    isotopes.get("C").put("mass", new double[]{12.0, 13.003354835});

    // Hydrogen isotopes
    isotopes.put("H", new HashMap<>());
    isotopes.get("H").put("abundance", new double[]{0.999885, 0.000115});
    isotopes.get("H").put("mass", new double[]{1.0078250322, 2.0141017781});

    // Nitrogen isotopes
    isotopes.put("N", new HashMap<>());
    isotopes.get("N").put("abundance", new double[]{0.99636, 0.00364});
    isotopes.get("N").put("mass", new double[]{14.003074004, 15.000108899});

    // Phosphorus isotopes
    isotopes.put("P", new HashMap<>());
    isotopes.get("P").put("abundance", new double[]{1.0});
    isotopes.get("P").put("mass", new double[]{30.973761998});

    // Oxygen isotopes
    isotopes.put("O", new HashMap<>());
    isotopes.get("O").put("abundance", new double[]{0.99757, 0.00038, 0.00205});
    isotopes.get("O").put("mass", new double[]{15.99491462, 16.999131757, 17.999159613});

    // Sulfur isotopes
    isotopes.put("S", new HashMap<>());
    isotopes.get("S").put("abundance", new double[]{0.9499, 0.0075, 0.0425, 0.0, 0.0001});
    isotopes.get("S")
        .put("mass", new double[]{31.972071174, 32.971458910, 33.9678670, 35.0, 35.967081});

    // Silicon isotopes
    isotopes.put("Si", new HashMap<>());
    isotopes.get("Si").put("abundance", new double[]{0.92223, 0.04685, 0.03092});
    isotopes.get("Si").put("mass", new double[]{27.976926535, 28.976494665, 29.9737701});

    logger.info("Loaded fallback isotopic data.");
    return isotopes;
  }

  @Override
  public String getTaskDescription() {
    return "Analyzing isotope labeling patterns in " + alignedFeatureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / totalRows;
  }

  /**
   * Validate task inputs
   *
   * @return true if inputs are valid, false otherwise
   */
  private boolean validateInputs() {
    if (alignedFeatureList == null) {
      setErrorMessage("No aligned feature list found");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (alignedFeatureList.getNumberOfRows() == 0) {
      setErrorMessage("Aligned feature list is empty");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (metadataColumn == null) {
      setErrorMessage("Metadata column not found or not selected");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    // Check that we have samples from both groups
    int unlabeledCount = 0;
    int labeledCount = 0;

    for (RawDataFile file : alignedFeatureList.getRawDataFiles()) {
      String metadataValue = getMetadataValue(file);
      if (metadataValue != null) {
        if (unlabeledValue.equalsIgnoreCase(metadataValue.trim())) {
          unlabeledCount++;
        } else if (labeledValue.equalsIgnoreCase(metadataValue.trim())) {
          labeledCount++;
        }
      }
    }

    if (unlabeledCount == 0) {
      setErrorMessage(
          "No unlabeled samples found with value '" + unlabeledValue + "' in metadata column '"
              + metadataColumn.getTitle() + "'");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (labeledCount == 0) {
      setErrorMessage(
          "No labeled samples found with value '" + labeledValue + "' in metadata column '"
              + metadataColumn.getTitle() + "'");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    logger.info("Validation successful: " + unlabeledCount + " unlabeled and " + labeledCount
        + " labeled samples found");
    return true;
  }

  /**
   * Initialize the result feature list based on the aligned feature list
   */
  private void initializeResultFeatureList() {
    // Create a new feature list based on the aligned feature list
    resultFeatureList = new ModularFeatureList(alignedFeatureList.getName() + " " + suffix, storage,
        alignedFeatureList.getRawDataFiles());

    logger.info("Created result feature list based on " + alignedFeatureList.getName() + " with "
        + alignedFeatureList.getNumberOfRows() + " rows");

    // Add required data types to the feature list immediately
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopeClusterType);
    }
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopologueRankType);
    }

    // Add all features to the result feature list
    for (FeatureListRow row : alignedFeatureList.getRows()) {
      ModularFeatureListRow newRow = new ModularFeatureListRow(resultFeatureList, row.getID(),
          (ModularFeatureListRow) row, true);
      resultFeatureList.addRow(newRow);
    }

    logger.info("Copied " + resultFeatureList.getNumberOfRows() + " rows to result feature list");
  }

  /**
   * Get the intensity of a feature based on the selected measure
   */
  private double getFeatureIntensity(Feature feature) {
    switch (intensityMeasure) {
      case "Height":
        return feature.getHeight();
      case "Area":
      case "Area (including background)":
        return feature.getArea();
      default:
        return feature.getHeight();
    }
  }

  /**
   * Get average intensity across all features in a row
   */
  private double getAverageIntensity(FeatureListRow row) {
    double totalIntensity = 0;
    int count = 0;

    for (RawDataFile file : row.getRawDataFiles()) {
      Feature feature = row.getFeature(file);
      if (feature != null) {
        totalIntensity += getFeatureIntensity(feature);
        count++;
      }
    }

    return count > 0 ? totalIntensity / count : 0;
  }

  /**
   * Extract isotopologue intensities from samples based on metadata
   */
  private IsotopeIntensityData extractIsotopologueIntensities(
      List<IsotopeCandidate> isotopologues) {
    int numIsotopologues = isotopologues.size();

    // Get all raw data files and categorize them by metadata
    List<RawDataFile> unlabeledDataFiles = new ArrayList<>();
    List<RawDataFile> labeledDataFiles = new ArrayList<>();

    for (RawDataFile file : alignedFeatureList.getRawDataFiles()) {
      String metadataValue = getMetadataValue(file);

      if (metadataValue != null) {
        if (unlabeledValue.equalsIgnoreCase(metadataValue.trim())) {
          unlabeledDataFiles.add(file);
          logger.fine(
              "Categorized file '" + file.getName() + "' as unlabeled (metadata: " + metadataValue
                  + ")");
        } else if (labeledValue.equalsIgnoreCase(metadataValue.trim())) {
          labeledDataFiles.add(file);
          logger.fine(
              "Categorized file '" + file.getName() + "' as labeled (metadata: " + metadataValue
                  + ")");
        } else {
          logger.fine("File '" + file.getName() + "' has metadata value '" + metadataValue
              + "' which matches neither unlabeled nor labeled values. Skipping.");
        }
      } else {
        logger.warning("No metadata value found for file '" + file.getName() + "'. Skipping.");
      }
    }

    logger.info(
        "Categorized " + unlabeledDataFiles.size() + " unlabeled and " + labeledDataFiles.size()
            + " labeled files");

    if (unlabeledDataFiles.isEmpty() || labeledDataFiles.isEmpty()) {
      logger.warning(
          "Missing samples in one or both groups. Unlabeled: " + unlabeledDataFiles.size()
              + ", Labeled: " + labeledDataFiles.size());
    }

    int numUnlabeledFiles = unlabeledDataFiles.size();
    int numLabeledFiles = labeledDataFiles.size();

    // Arrays to store intensity data
    double[][] unlabeledIntensities = new double[numIsotopologues][numUnlabeledFiles];
    double[][] labeledIntensities = new double[numIsotopologues][numLabeledFiles];

    // Extract intensity values
    for (int i = 0; i < numIsotopologues; i++) {
      FeatureListRow row = isotopologues.get(i).row;

      // Extract intensities from unlabeled samples
      for (int j = 0; j < numUnlabeledFiles; j++) {
        RawDataFile file = unlabeledDataFiles.get(j);
        Feature feature = row.getFeature(file);
        if (feature != null) {
          unlabeledIntensities[i][j] = getFeatureIntensity(feature);
        }
      }

      // Extract intensities from labeled samples
      for (int j = 0; j < numLabeledFiles; j++) {
        RawDataFile file = labeledDataFiles.get(j);
        Feature feature = row.getFeature(file);
        if (feature != null) {
          labeledIntensities[i][j] = getFeatureIntensity(feature);
        }
      }
    }

    // Calculate total intensities per sample
    double[] unlabeledTotals = calculateTotalIntensities(unlabeledIntensities);
    double[] labeledTotals = calculateTotalIntensities(labeledIntensities);

    // Calculate relative intensities
    double[][] unlabeledRelativeIntensities = calculateRelativeIntensities(unlabeledIntensities,
        unlabeledTotals);
    double[][] labeledRelativeIntensities = calculateRelativeIntensities(labeledIntensities,
        labeledTotals);

    // Calculate mean and sd of relative intensities
    double[] meanUnlabeledRelIntensities = new double[numIsotopologues];
    double[] meanLabeledRelIntensities = new double[numIsotopologues];
    double[] sdUnlabeledRelIntensities = new double[numIsotopologues];
    double[] sdLabeledRelIntensities = new double[numIsotopologues];

    for (int i = 0; i < numIsotopologues; i++) {
      DescriptiveStatistics statsUnlabeled = new DescriptiveStatistics();
      DescriptiveStatistics statsLabeled = new DescriptiveStatistics();

      for (int j = 0; j < numUnlabeledFiles; j++) {
        statsUnlabeled.addValue(unlabeledRelativeIntensities[i][j]);
      }

      for (int j = 0; j < numLabeledFiles; j++) {
        statsLabeled.addValue(labeledRelativeIntensities[i][j]);
      }

      meanUnlabeledRelIntensities[i] = statsUnlabeled.getMean();
      meanLabeledRelIntensities[i] = statsLabeled.getMean();
      sdUnlabeledRelIntensities[i] = statsUnlabeled.getStandardDeviation();
      sdLabeledRelIntensities[i] = statsLabeled.getStandardDeviation();
    }

    // Create and return the data object
    IsotopeIntensityData data = new IsotopeIntensityData();
    data.unlabeledIntensities = unlabeledIntensities;
    data.labeledIntensities = labeledIntensities;
    data.unlabeledRelativeIntensities = unlabeledRelativeIntensities;
    data.labeledRelativeIntensities = labeledRelativeIntensities;
    data.unlabeledTotals = unlabeledTotals;
    data.labeledTotals = labeledTotals;
    data.meanUnlabeledRelIntensities = meanUnlabeledRelIntensities;
    data.meanLabeledRelIntensities = meanLabeledRelIntensities;
    data.sdUnlabeledRelIntensities = sdUnlabeledRelIntensities;
    data.sdLabeledRelIntensities = sdLabeledRelIntensities;

    return data;
  }

  /**
   * Calculate total intensities across all isotopologues for each sample
   */
  private double[] calculateTotalIntensities(double[][] intensities) {
    int numSamples = intensities[0].length;
    double[] totals = new double[numSamples];

    for (int j = 0; j < numSamples; j++) {
      for (int i = 0; i < intensities.length; i++) {
        totals[j] += intensities[i][j];
      }
    }

    return totals;
  }

  /**
   * Calculate relative intensities by dividing by total intensities
   */
  private double[][] calculateRelativeIntensities(double[][] intensities, double[] totals) {
    int numIsotopologues = intensities.length;
    int numSamples = intensities[0].length;
    double[][] relativeIntensities = new double[numIsotopologues][numSamples];

    for (int j = 0; j < numSamples; j++) {
      if (totals[j] > 0) {
        for (int i = 0; i < numIsotopologues; i++) {
          relativeIntensities[i][j] = intensities[i][j] / totals[j];
        }
      }
    }

    return relativeIntensities;
  }

  /**
   * Validate the isotope pattern according to monotonicity and enrichment rules
   */
  private boolean validateIsotopePattern(IsotopeIntensityData data) {
    // Check monotonicity in unlabeled samples if configured
    if (!allowIncompletePatterns && monotonicityTolerance >= 0) {
      double prevMean = data.meanUnlabeledRelIntensities[0];

      for (int i = 1; i < data.meanUnlabeledRelIntensities.length; i++) {
        // Check if there's a gap in the isotopologue pattern
        boolean hasGap = false;
        // You would need to track the actual mass shifts to determine gaps

        if (data.meanUnlabeledRelIntensities[i] > (1 + monotonicityTolerance) * prevMean) {
          if (!hasGap) {
            // Non-monotonic pattern without a gap - reject
            logger.fine("Monotonicity check failed at isotopologue " + i);
            return false;
          }
        } else {
          prevMean = data.meanUnlabeledRelIntensities[i];
        }
      }
    }

    // Check enrichment in labeled samples - base peak should NOT be enriched
    if (enrichmentTolerance >= 0) {
      double baseUnlabeled = data.meanUnlabeledRelIntensities[0];
      double baseLabeled = data.meanLabeledRelIntensities[0];

      if (baseUnlabeled > 0) {
        double baseRatio = baseLabeled / baseUnlabeled;
        if (baseRatio > (1 + enrichmentTolerance)) {
          // Base peak is enriched, which typically shouldn't happen
          logger.fine("Enrichment check failed: base peak is enriched in labeled samples");
          return false;
        }
      }

      // For incomplete patterns, ensure at least one peak is significantly enriched
      boolean anyEnriched = false;
      for (int i = 1; i < data.meanLabeledRelIntensities.length; i++) {
        if (data.meanUnlabeledRelIntensities[i] > 0) {
          double ratio = data.meanLabeledRelIntensities[i] / data.meanUnlabeledRelIntensities[i];
          if (ratio > (1 + enrichmentTolerance)) {
            anyEnriched = true;
            break;
          }
        }
      }

      if (!anyEnriched) {
        logger.fine("No isotopologues show significant enrichment in labeled samples");
        return false;
      }
    }

    // Additional validation: check that not all intensities are zero
    boolean allZeroUnlabeled = Arrays.stream(data.meanUnlabeledRelIntensities)
        .allMatch(v -> v == 0);
    boolean allZeroLabeled = Arrays.stream(data.meanLabeledRelIntensities).allMatch(v -> v == 0);

    if (allZeroUnlabeled && allZeroLabeled) {
      return false;
    }

    return true;
  }

  /**
   * Calculate enrichment statistics and create result object
   */
  private IsotopeGroupResult calculateEnrichmentStatistics(List<IsotopeCandidate> isotopologues,
      IsotopeIntensityData data) {

    int numIsotopologues = isotopologues.size();

    // Compute enrichment ratios
    double[] enrichmentRatios = new double[numIsotopologues];
    double[] pValues = null;

    for (int i = 0; i < numIsotopologues; i++) {
      if (data.meanUnlabeledRelIntensities[i] > 0) {
        enrichmentRatios[i] =
            data.meanLabeledRelIntensities[i] / data.meanUnlabeledRelIntensities[i];
      } else {
        enrichmentRatios[i] = Double.NaN;
      }
    }

    // Perform statistical tests if not in single sample mode
    if (!singleSample) {
      pValues = calculatePValues(data);
    }

    // Create result object
    IsotopeGroupResult result = new IsotopeGroupResult();
    result.baseMz = isotopologues.get(0).row.getAverageMZ();
    result.baseRt = isotopologues.get(0).row.getAverageRT();
    result.isotopologues = isotopologues;
    result.meanUnlabeledRelIntensities = data.meanUnlabeledRelIntensities;
    result.meanLabeledRelIntensities = data.meanLabeledRelIntensities;
    result.sdUnlabeledRelIntensities = data.sdUnlabeledRelIntensities;
    result.sdLabeledRelIntensities = data.sdLabeledRelIntensities;
    result.enrichmentRatios = enrichmentRatios;
    result.pValues = pValues;
    result.unlabeledTotalIntensity = Arrays.stream(data.unlabeledTotals).average().orElse(0);
    result.labeledTotalIntensity = Arrays.stream(data.labeledTotals).average().orElse(0);
    result.rawUnlabeledRelIntensities = data.unlabeledRelativeIntensities;
    result.rawLabeledRelIntensities = data.labeledRelativeIntensities;

    return result;
  }

  /**
   * Calculate p-values for enrichment significance
   */
  private double[] calculatePValues(IsotopeIntensityData data) {
    TTest tTest = new TTest();
    int numIsotopologues = data.meanUnlabeledRelIntensities.length;
    double[] pValues = new double[numIsotopologues];

    for (int i = 0; i < numIsotopologues; i++) {
      try {
        // Extract the non-zero values for the test
        List<Double> unlabeledValues = new ArrayList<>();
        List<Double> labeledValues = new ArrayList<>();

        for (int j = 0; j < data.unlabeledTotals.length; j++) {
          if (data.unlabeledTotals[j] > 0) {
            unlabeledValues.add(data.unlabeledRelativeIntensities[i][j]);
          }
        }

        for (int j = 0; j < data.labeledTotals.length; j++) {
          if (data.labeledTotals[j] > 0) {
            labeledValues.add(data.labeledRelativeIntensities[i][j]);
          }
        }

        // Convert to arrays for the t-test
        double[] unlabeledArray = unlabeledValues.stream().mapToDouble(Double::doubleValue)
            .toArray();
        double[] labeledArray = labeledValues.stream().mapToDouble(Double::doubleValue).toArray();

        if (unlabeledArray.length > 0 && labeledArray.length > 0) {
          pValues[i] = tTest.tTest(unlabeledArray, labeledArray);
        } else {
          pValues[i] = 1.0;
        }
      } catch (Exception e) {
        logger.warning("Error performing t-test for isotopologue " + i + ": " + e.getMessage());
        pValues[i] = 1.0;
      }
    }

    return pValues;
  }

  /**
   * Check if the isotope pattern shows significant enrichment
   */
  private boolean isSingleSampleSignificant(IsotopeGroupResult result) {
    if (result == null) {
      return false;
    }

    if (!singleSample) {
      // Check for any significant p-values
      boolean anySignificant = false;
      for (double pValue : result.pValues) {
        if (pValue < pValueCutoff) {
          anySignificant = true;
          break;
        }
      }

      if (!anySignificant) {
        return false;
      }
    } else {
      // In single sample mode, use a simple difference in distribution
      double totalDelta = 0;
      for (int i = 0; i < result.meanLabeledRelIntensities.length; i++) {
        totalDelta += Math.abs(
            result.meanLabeledRelIntensities[i] - result.meanUnlabeledRelIntensities[i]);
      }

      if (totalDelta < 0.1) {  // Arbitrary threshold
        return false;
      }
    }

    return true;
  }

  /**
   * Annotate isotopologue rows with cluster ID and other information
   */
  private void annotateIsotopologueRows(List<IsotopeCandidate> isotopologues,
      IsotopeGroupResult result, int clusterID, double baseMz, double baseRt) {

    logger.info(
        "Annotating isotopologue rows for cluster ID " + clusterID + " with " + isotopologues.size()
            + " isotopologues");

    // First ensure the required column types exist in the result feature list
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopeClusterType);
      logger.info("Added isotopeClusterType to result feature list");
    }

    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopologueRankType);
      logger.info("Added isotopologueRankType to result feature list");
    }

    // Verify feature list has the required columns
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)
        || !resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      logger.severe("Failed to add required column types to result feature list");
      return;
    }

    // Count how many rows were successfully annotated
    int successCount = 0;

    for (IsotopeCandidate candidate : isotopologues) {
      // Find the corresponding row in the result feature list by matching ID
      ModularFeatureListRow resultRow = findResultRowByFeatureId(candidate.row.getID());

      if (resultRow != null) {
        // Set the cluster ID and isotopologue rank values
        try {
          resultRow.set(UntargetedLabelingParameters.isotopeClusterType, clusterID);
          resultRow.set(UntargetedLabelingParameters.isotopologueRankType, candidate.massShift);

          // Log the stored values for debugging
          Integer storedClusterId = resultRow.get(UntargetedLabelingParameters.isotopeClusterType);
          Integer storedRank = resultRow.get(UntargetedLabelingParameters.isotopologueRankType);

          if (storedClusterId == null || storedRank == null) {
            logger.warning("Failed to set values for row ID " + resultRow.getID() + ": clusterId="
                + storedClusterId + ", rank=" + storedRank);
          } else {
            logger.fine(
                "Row ID " + resultRow.getID() + ": Set cluster ID " + clusterID + " (stored: "
                    + storedClusterId + "), rank " + candidate.massShift + " (stored: " + storedRank
                    + ")");
            successCount++;
          }

          // Add comprehensive annotation to the row comment
          annotateRowComment(resultRow, candidate, result, clusterID, baseMz, baseRt);
        } catch (Exception e) {
          logger.severe("Error annotating row " + resultRow.getID() + ": " + e.getMessage());
          e.printStackTrace();
        }
      } else {
        logger.warning(
            "Could not find matching row ID " + candidate.row.getID() + " in result feature list");
      }
    }

    logger.info("Successfully annotated " + successCount + " out of " + isotopologues.size()
        + " rows for cluster ID " + clusterID);
  }

  /**
   * Find a row in the result feature list that matches a specific ID
   */
  private ModularFeatureListRow findResultRowByFeatureId(int featureId) {
    return (ModularFeatureListRow) resultFeatureList.findRowByID(featureId);
  }

  /**
   * Annotate a row's comment with isotope information
   */
  private void annotateRowComment(ModularFeatureListRow row, IsotopeCandidate candidate,
      IsotopeGroupResult result, int clusterID, double baseMz, double baseRt) {

    // Create a map with all our isotope properties for the comment
    Map<String, String> isotopeProps = new HashMap<>();
    isotopeProps.put("ISOTOPE_CLUSTER_ID", String.valueOf(clusterID));
    isotopeProps.put("ISOTOPOLOGUE_RANK", String.valueOf(candidate.massShift));
    isotopeProps.put("Isotope group base m/z", String.format("%.4f", baseMz));
    isotopeProps.put("Isotope group base RT", String.format("%.2f", baseRt));

    // Add enrichment ratio information if available
    int massShift = candidate.massShift;
    if (result.enrichmentRatios != null && massShift < result.enrichmentRatios.length) {
      isotopeProps.put("Enrichment ratio",
          String.format("%.2f", result.enrichmentRatios[massShift]));
    }

    // Add p-value information if available
    if (result.pValues != null && massShift < result.pValues.length) {
      isotopeProps.put("Enrichment p-value", String.format("%.5f", result.pValues[massShift]));
    }

    // Build the new comment
    String comment = row.getComment();
    StringBuilder newComment = new StringBuilder();
    if (comment != null && !comment.isEmpty()) {
      newComment.append(comment).append("\n\n");
    }

    // Add all isotope properties to the comment
    newComment.append("ISOTOPE LABELING PATTERN:\n");
    for (Map.Entry<String, String> entry : isotopeProps.entrySet()) {
      newComment.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }

    // Set the updated comment
    row.setComment(newComment.toString());
  }

  /**
   * Update a row's comment after cluster consolidation
   */
  private void updateRowComment(ModularFeatureListRow row, IsotopeCandidate candidate,
      IsotopeGroupResult result, int clusterID) {
    // Create a map with all our isotope properties for the comment
    Map<String, String> isotopeProps = new HashMap<>();
    isotopeProps.put("ISOTOPE_CLUSTER_ID", String.valueOf(clusterID));
    isotopeProps.put("ISOTOPOLOGUE_RANK", String.valueOf(candidate.massShift));
    isotopeProps.put("Isotope group base m/z", String.format("%.4f", result.baseMz));
    isotopeProps.put("Isotope group base RT", String.format("%.2f", result.baseRt));

    // Add enrichment ratio information if available
    int massShift = candidate.massShift;
    if (result.enrichmentRatios != null && massShift < result.enrichmentRatios.length) {
      isotopeProps.put("Enrichment ratio",
          String.format("%.2f", result.enrichmentRatios[massShift]));
    }

    // Add p-value information if available
    if (result.pValues != null && massShift < result.pValues.length) {
      isotopeProps.put("Enrichment p-value", String.format("%.5f", result.pValues[massShift]));
    }

    // Build the new comment
    StringBuilder newComment = new StringBuilder();
    newComment.append("ISOTOPE LABELING PATTERN:\n");

    // Add all isotope properties to the comment
    for (Map.Entry<String, String> entry : isotopeProps.entrySet()) {
      newComment.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }

    // Set the updated comment
    row.setComment(newComment.toString());
  }

  /**
   * Finalize the results and add the feature list to the project
   */
  private void finalizeResults() {
    if (isCanceled() || resultFeatureList == null) {
      return;
    }

    logger.info("Finalizing results and adding feature list to project");

    // Ensure column types are added to the feature list
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopeClusterType);
      logger.info("Added isotopeClusterType to result feature list");
    }

    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopologueRankType);
      logger.info("Added isotopologueRankType to result feature list");
    }

    // Verify that the required column types exist
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)
        || !resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      logger.severe("Failed to add required column types to result feature list");
    }

    // Count the number of isotope clusters found
    int clustersFound = 0;
    Set<Integer> uniqueClusterIds = new HashSet<>();
    int annotatedRows = 0;
    int totalRows = resultFeatureList.getNumberOfRows();

    for (FeatureListRow row : resultFeatureList.getRows()) {
      ModularFeatureListRow modRow = (ModularFeatureListRow) row;
      Integer clusterId = modRow.get(UntargetedLabelingParameters.isotopeClusterType);
      if (clusterId != null) {
        uniqueClusterIds.add(clusterId);
        annotatedRows++;
      }
    }
    clustersFound = uniqueClusterIds.size();

    logger.info("Found " + clustersFound + " isotope clusters with " + annotatedRows
        + " annotated rows out of " + totalRows + " total rows");

    // Perform one final check to ensure all fields are correctly set
    verifyResultFeatureList();

    // Add task description to feature list
    resultFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Untargeted isotope labeling analysis",
            UntargetedLabelingModule.class, parameters, getModuleCallDate()));

    // Add the new feature list to the project
    project.addFeatureList(resultFeatureList);

    logger.info("Added new feature list " + resultFeatureList.getName() + " to the project");
  }

  /**
   * Verify that the result feature list is properly annotated
   */
  private void verifyResultFeatureList() {
    logger.info("Verifying result feature list annotations");

    // Check if the feature list has the required column types
    boolean hasClusterType = resultFeatureList.hasRowType(
        UntargetedLabelingParameters.isotopeClusterType);
    boolean hasRankType = resultFeatureList.hasRowType(
        UntargetedLabelingParameters.isotopologueRankType);

    if (!hasClusterType || !hasRankType) {
      logger.severe("Result feature list is missing required column types: " + "isotopeClusterType="
          + hasClusterType + ", isotopologueRankType=" + hasRankType);

      // Try to add them again
      if (!hasClusterType) {
        resultFeatureList.addRowType(UntargetedLabelingParameters.isotopeClusterType);
      }
      if (!hasRankType) {
        resultFeatureList.addRowType(UntargetedLabelingParameters.isotopologueRankType);
      }
    }

    // Verify that our previously annotated rows still have their annotations
    int totalWithCluster = 0;
    int totalWithRank = 0;

    for (FeatureListRow row : resultFeatureList.getRows()) {
      if (row instanceof ModularFeatureListRow) {
        ModularFeatureListRow modRow = (ModularFeatureListRow) row;

        // Check if clusterId is stored
        Integer clusterId = modRow.get(UntargetedLabelingParameters.isotopeClusterType);
        if (clusterId != null) {
          totalWithCluster++;
        }

        // Check if rank is stored
        Integer rank = modRow.get(UntargetedLabelingParameters.isotopologueRankType);
        if (rank != null) {
          totalWithRank++;
        }
      }
    }

    logger.info(
        "Verification complete: " + totalWithCluster + " rows have cluster ID, " + totalWithRank
            + " rows have isotopologue rank");

    // Final sanity check - if we identified clusters but no rows have cluster IDs, something is wrong
    if (isotopeLabelResults.size() > 0 && totalWithCluster == 0) {
      logger.severe("Critical error: Found " + isotopeLabelResults.size()
          + " isotope clusters but no rows have cluster IDs assigned");
    }
  }

  /**
   * Class to represent an isotopologue candidate
   */
  public static class IsotopeCandidate {

    FeatureListRow row;
    int massShift;

    public IsotopeCandidate(FeatureListRow row, int massShift) {
      this.row = row;
      this.massShift = massShift;
    }
  }

  /**
   * Class to store isotope intensity data during analysis
   */
  private class IsotopeIntensityData {

    double[][] unlabeledIntensities;
    double[][] labeledIntensities;
    double[][] unlabeledRelativeIntensities;
    double[][] labeledRelativeIntensities;
    double[] unlabeledTotals;
    double[] labeledTotals;
    double[] meanUnlabeledRelIntensities;
    double[] meanLabeledRelIntensities;
    double[] sdUnlabeledRelIntensities;
    double[] sdLabeledRelIntensities;
  }

  /**
   * Class to store isotope pattern analysis results
   */
  private class IsotopeGroupResult {

    double baseMz;
    double baseRt;
    List<IsotopeCandidate> isotopologues;
    double[] meanUnlabeledRelIntensities;
    double[] meanLabeledRelIntensities;
    double[] sdUnlabeledRelIntensities;
    double[] sdLabeledRelIntensities;
    double[] enrichmentRatios;
    double[] pValues;
    double unlabeledTotalIntensity;
    double labeledTotalIntensity;
    double[][] rawUnlabeledRelIntensities;
    double[][] rawLabeledRelIntensities;
    int clusterId; // Added field to track the current cluster ID
  }
}