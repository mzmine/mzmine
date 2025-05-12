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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
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
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * Task for untargeted isotope labeling analysis. Analyzes feature lists from unlabeled samples to
 * find isotopically enriched patterns in corresponding labeled samples. Modified to work with
 * feature lists instead of raw data files.
 */
public class UntargetedLabelingTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(UntargetedLabelingTask.class.getName());

  // ----- TASK VARIABLES -----
  private final MZmineProject project;
  private final FeatureList unlabeledFeatureList;
  private final FeatureList labeledFeatureList;
  private ModularFeatureList resultFeatureList;
  private final ParameterSet parameters;
  private final MemoryMapStorage storage;
  private Map<String, Map<String, double[]>> dataIsotopes;

  // Progress tracking
  private int processedRows;
  private int totalRows;

  // ----- ANALYSIS PARAMETERS -----
  // Basic parameters
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
   *
   * @param project              The MZmine project
   * @param unlabeledFeatureList The feature list containing unlabeled samples
   * @param labeledFeatureList   The feature list containing labeled samples
   * @param parameters           Task parameters
   * @param storage              Storage for results
   * @param moduleCallDate       Module call date
   */
  public UntargetedLabelingTask(MZmineProject project, FeatureList unlabeledFeatureList,
      FeatureList labeledFeatureList, ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.unlabeledFeatureList = unlabeledFeatureList;
    this.labeledFeatureList = labeledFeatureList;
    this.parameters = parameters;
    this.storage = storage;

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

    // Now calculate the values that would have been direct parameters before
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
    return "Analyzing isotope labeling patterns in " + unlabeledFeatureList.getName() + " vs "
        + labeledFeatureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info(
        "Starting untargeted isotope labeling analysis between " + unlabeledFeatureList.getName()
            + " and " + labeledFeatureList.getName());

    // Validate inputs
    if (!validateInputs()) {
      return;
    }

    // Initialize the result feature list (based on labeled feature list)
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
   * Validate task inputs
   *
   * @return true if inputs are valid, false otherwise
   */
  private boolean validateInputs() {
    if (unlabeledFeatureList == null) {
      setErrorMessage("No unlabeled feature list found");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (labeledFeatureList == null) {
      setErrorMessage("No labeled feature list found");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (unlabeledFeatureList.getNumberOfRows() == 0) {
      setErrorMessage("Unlabeled feature list is empty");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (labeledFeatureList.getNumberOfRows() == 0) {
      setErrorMessage("Labeled feature list is empty");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    return true;
  }

  /**
   * Initialize the result feature list based on the labeled feature list
   */
  private void initializeResultFeatureList() {
    // Create a new feature list based on the labeled feature list
    resultFeatureList = new ModularFeatureList(labeledFeatureList.getName() + " " + suffix, storage,
        labeledFeatureList.getRawDataFiles());

    logger.info("Created result feature list based on " + labeledFeatureList.getName() + " with "
        + labeledFeatureList.getNumberOfRows() + " rows");

    // Add required data types to the feature list immediately
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopeClusterType);
    }
    if (!resultFeatureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
      resultFeatureList.addRowType(UntargetedLabelingParameters.isotopologueRankType);
    }

    // Add all labeled features to the result feature list
    for (FeatureListRow row : labeledFeatureList.getRows()) {
      ModularFeatureListRow newRow = new ModularFeatureListRow(resultFeatureList, row.getID(),
          (ModularFeatureListRow) row, true);
      resultFeatureList.addRow(newRow);
    }

    logger.info("Copied " + resultFeatureList.getNumberOfRows() + " rows to result feature list");
  }

  /**
   * Process features to find isotope patterns using the comprehensive approach
   */
  private void processFeatures() {
    logger.info("Using comprehensive search approach for isotopologue detection");

    // Find all potential isotopologue groups
    List<List<IsotopeCandidate>> allGroups = findIsotopologueGroups();

    logger.info("Found " + allGroups.size() + " potential isotopologue groups");

    // Set up progress tracking
    totalRows = allGroups.size();
    processedRows = 0;

    // Process each group
    for (List<IsotopeCandidate> group : allGroups) {
      if (isCanceled()) {
        return;
      }

      // Get base peak information
      IsotopeCandidate baseCandidate = group.get(0);
      double baseMz = baseCandidate.row.getAverageMZ();
      double baseRt = baseCandidate.row.getAverageRT();

      // Analyze and annotate the pattern
      analyzeAndAnnotateIsotopePattern(group, baseMz, baseRt);

      processedRows++;
    }

    logger.info("Completed analysis of " + allGroups.size() + " isotopologue groups, found "
        + isotopeLabelResults.size() + " significant patterns");
  }

  /**
   * Find all potential isotopologue relationships in the feature lists using a comprehensive search
   * approach.
   *
   * @return List of potential isotopologue groups
   */
  private List<List<IsotopeCandidate>> findIsotopologueGroups() {
    List<List<IsotopeCandidate>> allGroups = new ArrayList<>();
    Map<FeatureListRow, List<IsotopePair>> basePeakToPairs = new HashMap<>();

    // Step 1: Find all potential isotope pairs within RT window
    List<IsotopePair> isoPairs = new ArrayList<>();

    for (FeatureListRow rowA : labeledFeatureList.getRows()) {
      double mzA = rowA.getAverageMZ();
      double rtA = rowA.getAverageRT();

      // Skip if below noise level
      if (getAverageIntensity(rowA) < noiseLevel) {
        continue;
      }

      // Find all peaks co-eluting with rowA
      for (FeatureListRow rowB : labeledFeatureList.getRows()) {
        // Skip same peak
        if (rowA.getID() == rowB.getID()) {
          continue;
        }

        double mzB = rowB.getAverageMZ();
        double rtB = rowB.getAverageRT();

        // Skip if not co-eluting
        if (!rtTolerance.checkWithinTolerance((float) rtA, (float) rtB)) {
          continue;
        }

        // Calculate mass difference in isotope units
        double mzDiff = mzB - mzA;
        double isoUnits = mzDiff / isotopeMassDifference;
        int roundedIsoUnits = (int) Math.round(isoUnits);

        // Check if difference is a multiple of isotope mass within tolerance
        // and ensure it doesn't exceed maximum isotopologues parameter
        double error = Math.abs(mzDiff - (roundedIsoUnits * isotopeMassDifference));
        double ppmError = (error / mzA) * 1e6;

        if (ppmError <= mzTolerance.getPpmTolerance() && roundedIsoUnits > 0
            && roundedIsoUnits <= maximumIsotopologues) {

          IsotopePair pair = new IsotopePair(rowA, rowB, roundedIsoUnits);
          isoPairs.add(pair);

          // Also maintain a mapping of base peaks to their pairs for more efficient clustering
          basePeakToPairs.computeIfAbsent(rowA, k -> new ArrayList<>()).add(pair);
        }
      }
    }

    logger.info("Found " + isoPairs.size() + " potential isotopologue pairs");

    // Step 2: Group pairs into isotopologue clusters more intelligently
    Set<FeatureListRow> processedBasePeaks = new HashSet<>();

    for (FeatureListRow basePeak : basePeakToPairs.keySet()) {
      if (processedBasePeaks.contains(basePeak)) {
        continue;
      }

      List<IsotopeCandidate> cluster = new ArrayList<>();
      // Add the base peak
      cluster.add(new IsotopeCandidate(basePeak, 0));

      // Add all direct isotopologues
      for (IsotopePair pair : basePeakToPairs.get(basePeak)) {
        // Check if already added to avoid duplicates
        boolean alreadyAdded = false;
        for (IsotopeCandidate existing : cluster) {
          if (existing.row.getID() == pair.labeledRow.getID()) {
            alreadyAdded = true;
            break;
          }
        }

        if (!alreadyAdded) {
          cluster.add(new IsotopeCandidate(pair.labeledRow, pair.massShift));
        }
      }

      // Mark this base peak as processed
      processedBasePeaks.add(basePeak);

      // Recursively expand the cluster to find potential multi-step isotopologues
      // (e.g., M+0 → M+2 → M+4 or other gaps)
      expandCluster(cluster, basePeakToPairs, processedBasePeaks, 0);

      // Sort by mass shift
      Collections.sort(cluster, Comparator.comparingInt(c -> c.massShift));

      // Apply filtering for valid patterns
      if (filterIsotopologueCluster(cluster)) {
        allGroups.add(cluster);
      }
    }

    // Log some diagnostic information
    int totalIsotopologues = allGroups.stream().mapToInt(List::size).sum();
    logger.info("Created " + allGroups.size() + " isotopologue clusters with " + totalIsotopologues
        + " total isotopologues");

    // Log cluster size distribution
    Map<Integer, Integer> sizeDistribution = new HashMap<>();
    for (List<IsotopeCandidate> group : allGroups) {
      int size = group.size();
      sizeDistribution.put(size, sizeDistribution.getOrDefault(size, 0) + 1);
    }

    StringBuilder sizeInfo = new StringBuilder("Cluster size distribution: ");
    for (int size = 2; size <= maximumIsotopologues + 1; size++) {
      if (sizeDistribution.containsKey(size)) {
        sizeInfo.append(size).append("=").append(sizeDistribution.get(size)).append(", ");
      }
    }
    logger.info(sizeInfo.toString());

    return allGroups;
  }

  /**
   * Recursively expand a cluster by finding isotopologues of isotopologues. This helps identify
   * non-continuous patterns like M+0, M+2, M+6.
   *
   * @param cluster            Current cluster of isotopologues
   * @param basePeakToPairs    Map of base peaks to their isotope pairs
   * @param processedBasePeaks Set of already processed base peaks
   * @param depth              Current recursion depth (to prevent infinite recursion)
   */
  private void expandCluster(List<IsotopeCandidate> cluster,
      Map<FeatureListRow, List<IsotopePair>> basePeakToPairs,
      Set<FeatureListRow> processedBasePeaks, int depth) {
    // Prevent infinite recursion
    if (depth >= 3) { // Limit recursion depth
      return;
    }

    // Copy current cluster to avoid concurrent modification issues
    List<IsotopeCandidate> currentCluster = new ArrayList<>(cluster);

    // Look for isotopologues of each isotopologue in the cluster
    for (IsotopeCandidate candidate : currentCluster) {
      FeatureListRow row = candidate.row;

      // Skip if already processed as a base peak
      if (processedBasePeaks.contains(row)) {
        continue;
      }

      // Skip if this row doesn't have any pairs
      if (!basePeakToPairs.containsKey(row)) {
        continue;
      }

      // Mark as processed to avoid cycles
      processedBasePeaks.add(row);

      // Add its isotopologues
      for (IsotopePair pair : basePeakToPairs.get(row)) {
        // Calculate correct mass shift relative to original base peak
        int combinedMassShift = candidate.massShift + pair.massShift;

        // Skip if exceeds maximum isotopologues
        if (combinedMassShift > maximumIsotopologues) {
          continue;
        }

        // Check if already added to avoid duplicates
        boolean alreadyAdded = false;
        for (IsotopeCandidate existing : cluster) {
          if (existing.row.getID() == pair.labeledRow.getID()) {
            alreadyAdded = true;
            break;
          }
        }

        if (!alreadyAdded) {
          cluster.add(new IsotopeCandidate(pair.labeledRow, combinedMassShift));
        }
      }

      // Recursively expand
      expandCluster(cluster, basePeakToPairs, processedBasePeaks, depth + 1);
    }
  }

  /**
   * Apply additional filtering to an isotopologue cluster to ensure it's a valid pattern
   *
   * @param cluster The cluster to filter
   * @return true if the cluster passes all filters
   */
  private boolean filterIsotopologueCluster(List<IsotopeCandidate> cluster) {
    // Basic validity check - make sure we have enough isotopologues
    if (cluster.size() < minIsotopePatternSize) {
      return false;
    }

    // Sort by mass shift to ensure proper order
    Collections.sort(cluster, Comparator.comparingInt(c -> c.massShift));

    // Check for unreasonable gaps in the pattern
    int lastMassShift = 0;
    int gapCount = 0;
    int maxConsecutiveGaps = 3;  // Allow up to 3 consecutive gaps

    for (int i = 1; i < cluster.size(); i++) {
      int currentMassShift = cluster.get(i).massShift;
      int expectedMassShift = lastMassShift + 1;

      if (currentMassShift > expectedMassShift) {
        // Found a gap
        gapCount += (currentMassShift - expectedMassShift);

        // If too many consecutive gaps, reject the pattern
        // (but be more lenient here than before)
        if (gapCount > maxConsecutiveGaps && !allowIncompletePatterns) {
          return false;
        }
      } else {
        // Reset gap count when we find a consecutive isotopologue
        gapCount = 0;
      }

      lastMassShift = currentMassShift;
    }

    // Check intensity patterns to avoid random associations
    FeatureListRow baseRow = cluster.get(0).row;
    double baseIntensity = getAverageIntensity(baseRow);

    // Count isotopologues with very low intensity compared to base
    int lowIntensityCount = 0;
    double minIntensityThreshold = baseIntensity * 0.005;  // 0.5% of base intensity (more lenient)

    // Check for unreasonably low intensities
    for (int i = 1; i < cluster.size(); i++) {
      FeatureListRow currentRow = cluster.get(i).row;
      double currentIntensity = getAverageIntensity(currentRow);

      if (currentIntensity < minIntensityThreshold) {
        lowIntensityCount++;
      }
    }

    // If more than 70% of isotopologues have very low intensity, reject the pattern
    if (lowIntensityCount > cluster.size() * 0.7) {
      return false;
    }

    // Ensure the pattern doesn't exceed maximum isotopologues
    if (cluster.size() > maximumIsotopologues + 1) { // +1 for M+0
      cluster.subList(0, maximumIsotopologues + 1);
    }

    // If we've passed all filters, accept the pattern
    return true;
  }

  /**
   * Helper class for isotope pair detection
   */
  private class IsotopePair {

    FeatureListRow baseRow;
    FeatureListRow labeledRow;
    int massShift;

    public IsotopePair(FeatureListRow baseRow, FeatureListRow labeledRow, int massShift) {
      this.baseRow = baseRow;
      this.labeledRow = labeledRow;
      this.massShift = massShift;
    }
  }

  /**
   * Get the intensity of a feature based on the selected measure
   *
   * @param feature The feature to get intensity from
   * @return The intensity value
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
   *
   * @param row The feature row
   * @return The average intensity
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
   * Find a matching feature in the unlabeled feature list
   *
   * @param mz The target m/z
   * @param rt The target RT
   * @return The matching feature row, or null if not found
   */
  private FeatureListRow findMatchingUnlabeledFeature(double mz, double rt) {
    // Look through the unlabeled feature list to find a matching feature
    for (FeatureListRow row : unlabeledFeatureList.getRows()) {
      if (mzTolerance.checkWithinTolerance(mz, row.getAverageMZ())
          && rtTolerance.checkWithinTolerance((float) rt, row.getAverageRT())) {
        return row;
      }
    }
    return null;
  }

  /**
   * Analyze an isotope pattern and annotate the feature list
   *
   * @param isotopologues The list of isotopologue candidates
   * @param baseMz        The base m/z
   * @param baseRt        The base RT
   */
  private void analyzeAndAnnotateIsotopePattern(List<IsotopeCandidate> isotopologues, double baseMz,
      double baseRt) {
    IsotopeGroupResult result = analyzeIsotopePattern(isotopologues);

    if (result == null && isotopologues.size() >= minIsotopePatternSize) {
      logger.fine(
          "Found " + isotopologues.size() + " isotopologues for feature at m/z=" + baseMz + ", RT="
              + baseRt + " but pattern analysis returned null");

      // Log possible rejection reasons
      if (monotonicityTolerance > 0) {
        logger.fine("Monotonicity constraint may have failed");
      }
      if (enrichmentTolerance > 0) {
        logger.fine("Enrichment constraint may have failed");
      }
    }

    if (result != null) {
      // Calculate the number of isotope patterns found so far plus one for this new pattern
      int clusterID = isotopeLabelResults.size() + 1;
      result.clusterId = clusterID;

      isotopeLabelResults.add(result);

      // Annotate all isotopologue rows in the result feature list
      annotateIsotopologueRows(isotopologues, result, clusterID, baseMz, baseRt);

      logger.info("Created isotope cluster #" + clusterID + " with " + isotopologues.size()
          + " isotopologues at m/z=" + baseMz + ", RT=" + baseRt);
    } else {
      logger.finest("Feature at m/z=" + baseMz + " did not show significant labeling");
    }
  }

  /**
   * Analyze an isotope pattern to determine if it represents true labeling
   *
   * @param isotopologues List of isotopologue candidates
   * @return Analysis result, or null if not a valid pattern
   */
  private IsotopeGroupResult analyzeIsotopePattern(List<IsotopeCandidate> isotopologues) {
    if (isCanceled()) {
      return null;
    }

    logger.fine("Analyzing isotope pattern with " + isotopologues.size() + " isotopologues");

    // Make sure the list is sorted by mass shift
    Collections.sort(isotopologues, Comparator.comparingInt(c -> c.massShift));

    // Basic validation: ensure we have at least minIsotopePatternSize isotopologues
    if (isotopologues.size() < minIsotopePatternSize) {
      return null;
    }

    // Extract intensity data for all isotopologues from unlabeled and labeled samples
    IsotopeIntensityData intensityData = extractIsotopologueIntensities(isotopologues);

    // Check pattern monotonicity and enrichment
    if (!validateIsotopePattern(intensityData)) {
      return null;
    }

    // Calculate statistics and enrichment ratios
    IsotopeGroupResult result = calculateEnrichmentStatistics(isotopologues, intensityData);

    // Check statistical significance if not in single sample mode
    if (!isSingleSampleSignificant(result)) {
      return null;
    }

    if (result != null) {
      logger.fine(
          "Isotope pattern analysis successful: Found significant labeling at m/z=" + result.baseMz
              + ", RT=" + result.baseRt);

      // Log the enrichment ratios
      logEnrichmentRatios(result.enrichmentRatios);
    } else {
      logger.fine("Isotope pattern analysis rejected pattern");
    }

    return result;
  }

  /**
   * Extract isotopologue intensities from samples
   *
   * @param isotopologues List of isotopologue candidates
   * @return The extracted intensity data
   */
  private IsotopeIntensityData extractIsotopologueIntensities(
      List<IsotopeCandidate> isotopologues) {
    int numIsotopologues = isotopologues.size();

    // Get raw data files from both feature lists
    List<RawDataFile> unlabeledFiles = unlabeledFeatureList.getRawDataFiles();
    List<RawDataFile> labeledFiles = labeledFeatureList.getRawDataFiles();

    int numUnlabeledFiles = unlabeledFiles.size();
    int numLabeledFiles = labeledFiles.size();

    // Arrays to store intensity data
    double[][] unlabeledIntensities = new double[numIsotopologues][numUnlabeledFiles];
    double[][] labeledIntensities = new double[numIsotopologues][numLabeledFiles];

    // Extract intensity values
    for (int i = 0; i < numIsotopologues; i++) {
      FeatureListRow row = isotopologues.get(i).row;
      int massShift = isotopologues.get(i).massShift;

      // For M+0 (base peak), get intensities from unlabeled feature list
      if (massShift == 0) {
        FeatureListRow unlabeledBaseRow = findMatchingUnlabeledFeature(row.getAverageMZ(),
            row.getAverageRT());

        if (unlabeledBaseRow != null) {
          // Extract intensities from the unlabeled feature list
          for (int j = 0; j < numUnlabeledFiles; j++) {
            RawDataFile file = unlabeledFiles.get(j);
            Feature feature = unlabeledBaseRow.getFeature(file);
            if (feature != null) {
              unlabeledIntensities[i][j] = getFeatureIntensity(feature);
            }
          }
        }
      } else {
        // For higher isotopologues (M+1, M+2, etc.), we don't expect them to be in the unlabeled data
        // So set unlabeled intensities to low values (natural abundance)
        for (int j = 0; j < numUnlabeledFiles; j++) {
          // Use a simple formula to estimate natural abundance (decreasing with mass shift)
          unlabeledIntensities[i][j] = unlabeledIntensities[0][j] * Math.pow(0.01, massShift);
        }
      }

      // Get labeled intensities directly from the labeled feature
      for (int j = 0; j < numLabeledFiles; j++) {
        RawDataFile file = labeledFiles.get(j);
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
    if (monotonicityTolerance > 0) {
      double prevMean = data.meanUnlabeledRelIntensities[0];
      for (int i = 1; i < data.meanUnlabeledRelIntensities.length; i++) {
        if (data.meanUnlabeledRelIntensities[i] > (1 + monotonicityTolerance) * prevMean) {
          // Non-monotonic pattern in unlabeled samples, likely not a true isotope pattern
          logger.fine("Monotonicity check failed at isotopologue " + i);

          // Exception for incomplete patterns if allowed
          if (allowIncompletePatterns) {
            // Special case for hexose patterns (glucose): M+0, M+6
            if (i == 1 && data.meanUnlabeledRelIntensities.length >= 7) {
              // Check if actual M+6 is much higher in labeled samples - indicating glucose labeling
              if (data.meanLabeledRelIntensities[6] > 5 * data.meanUnlabeledRelIntensities[6]) {
                // Likely glucose pattern, allow it despite monotonicity failure
                logger.fine("Making monotonicity exception for potential glucose pattern");
                return true;
              }
            }
          }

          return false;
        }
        prevMean = data.meanUnlabeledRelIntensities[i];
      }
    }

    // Check enrichment in labeled samples
    if (enrichmentTolerance > 0) {
      double baseRatio = data.meanLabeledRelIntensities[0] / data.meanUnlabeledRelIntensities[0];
      if (baseRatio > (1 + enrichmentTolerance)) {
        // Base peak is enriched, which typically shouldn't happen - this indicates a problem
        logger.fine("Enrichment check failed: base peak is enriched too much in labeled samples");
        return false;
      }

      // For incomplete patterns, ensure at least one peak is significantly enriched
      if (allowIncompletePatterns) {
        boolean anyEnriched = false;
        for (int i = 1; i < data.meanLabeledRelIntensities.length; i++) {
          double ratio = data.meanLabeledRelIntensities[i] / data.meanUnlabeledRelIntensities[i];
          if (ratio > 2.0) { // At least 2-fold enrichment
            anyEnriched = true;
            break;
          }
        }

        if (!anyEnriched) {
          logger.fine("No isotopologues show significant enrichment in labeled samples");
          return false;
        }
      }
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
   * Log enrichment ratios for debugging
   */
  private void logEnrichmentRatios(double[] enrichmentRatios) {
    StringBuilder ratiosStr = new StringBuilder("Enrichment ratios: ");
    for (int i = 0; i < Math.min(5, enrichmentRatios.length); i++) {
      ratiosStr.append("M+").append(i).append("=")
          .append(String.format("%.2f", enrichmentRatios[i])).append(" ");
    }
    logger.fine(ratiosStr.toString());
  }

  /**
   * Annotate isotopologue rows with cluster ID and other information
   *
   * @param isotopologues The list of isotopologue candidates
   * @param result        The isotope pattern analysis result
   * @param clusterID     The cluster ID to assign
   * @param baseMz        The base m/z
   * @param baseRt        The base RT
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
   *
   * @param featureId The feature ID to look for
   * @return The matching row, or null if not found
   */
  private ModularFeatureListRow findResultRowByFeatureId(int featureId) {
    return (ModularFeatureListRow) resultFeatureList.findRowByID(featureId);
  }

  /**
   * Annotate a row's comment with isotope information
   *
   * @param row       The row to annotate
   * @param candidate The isotope candidate
   * @param result    The isotope pattern analysis result
   * @param clusterID The cluster ID
   * @param baseMz    The base m/z
   * @param baseRt    The base RT
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
   * Consolidate overlapping isotope clusters
   */
  private void consolidateIsotopeClusters() {
    if (isotopeLabelResults.isEmpty()) {
      return;
    }

    logger.info("Consolidating overlapping isotope clusters...");

    // Sort clusters by m/z
    Collections.sort(isotopeLabelResults, Comparator.comparingDouble(r -> r.baseMz));

    // Initialize cluster IDs if not already set
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      isotopeLabelResults.get(i).clusterId = i + 1;
    }

    // Track which clusters have been merged into others
    Set<Integer> mergedClusterIndices = new HashSet<>();

    // For efficient lookup of existing clusters by base peak
    Map<Integer, List<Integer>> featureIdToClusterIndices = new HashMap<>();
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      IsotopeGroupResult cluster = isotopeLabelResults.get(i);
      for (IsotopeCandidate candidate : cluster.isotopologues) {
        int featureId = candidate.row.getID();
        featureIdToClusterIndices.computeIfAbsent(featureId, k -> new ArrayList<>()).add(i);
      }
    }

    // First pass: identify clusters that should be merged
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      if (mergedClusterIndices.contains(i)) {
        continue; // Skip if this cluster has already been merged into another
      }

      IsotopeGroupResult currentCluster = isotopeLabelResults.get(i);
      Set<Integer> clustersToMerge = new HashSet<>();

      // Try to find clusters to merge with this one
      // First check for overlapping features
      for (IsotopeCandidate candidate : currentCluster.isotopologues) {
        int featureId = candidate.row.getID();
        List<Integer> overlappingClusterIndices = featureIdToClusterIndices.getOrDefault(featureId,
            Collections.emptyList());

        for (int overlappingIndex : overlappingClusterIndices) {
          if (overlappingIndex != i && !mergedClusterIndices.contains(overlappingIndex)) {
            IsotopeGroupResult otherCluster = isotopeLabelResults.get(overlappingIndex);

            // Check if within RT tolerance
            boolean sameRT = rtTolerance.checkWithinTolerance((float) currentCluster.baseRt,
                (float) otherCluster.baseRt);

            if (!sameRT) {
              continue;
            }

            // Calculate mass difference in isotope units
            double massRatio =
                (otherCluster.baseMz - currentCluster.baseMz) / isotopeMassDifference;
            int massShiftDiff = (int) Math.round(massRatio);

            // Case 1: Direct isotope relationship
            if (Math.abs(massRatio - massShiftDiff) <= 0.2
                && Math.abs(massShiftDiff) <= maximumIsotopologues / 2) {
              clustersToMerge.add(overlappingIndex);
              continue;
            }

            // Case 2: Check for shared features
            Set<Integer> featureIds1 = currentCluster.isotopologues.stream().map(c -> c.row.getID())
                .collect(java.util.stream.Collectors.toSet());

            Set<Integer> featureIds2 = otherCluster.isotopologues.stream().map(c -> c.row.getID())
                .collect(java.util.stream.Collectors.toSet());

            Set<Integer> intersection = new HashSet<>(featureIds1);
            intersection.retainAll(featureIds2);

            if (intersection.size() >= 2) {
              clustersToMerge.add(overlappingIndex);
              continue;
            }

            // Case 3: Intensity consistency
            double mzDiff = Math.abs(otherCluster.baseMz - currentCluster.baseMz);
            if (mzDiff < 10 * isotopeMassDifference && checkIntensityConsistency(currentCluster,
                otherCluster)) {
              clustersToMerge.add(overlappingIndex);
            }
          }
        }
      }

      // Merge all identified clusters into the current one
      for (int clusterIndex : clustersToMerge) {
        IsotopeGroupResult clusterToMerge = isotopeLabelResults.get(clusterIndex);
        mergeIsotopeClusters(currentCluster, clusterToMerge);
        mergedClusterIndices.add(clusterIndex);

        logger.fine("Merged cluster " + clusterToMerge.clusterId + " (m/z=" + clusterToMerge.baseMz
            + ") into cluster " + currentCluster.clusterId + " (m/z=" + currentCluster.baseMz
            + ")");
      }
    }

    // Create a new list with only the non-merged clusters
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

      // Update cluster IDs in the result feature list - pass empty map since we'll reassign IDs
      updateClusterIds(new HashMap<>());
    } else {
      logger.info("No clusters were consolidated");
    }
  }

  /**
   * Check if intensity patterns between two clusters are consistent
   */
  private boolean checkIntensityConsistency(IsotopeGroupResult cluster1,
      IsotopeGroupResult cluster2) {
    // Simple check 1: total intensity ratio shouldn't differ too much
    double intensityRatio1 = cluster1.labeledTotalIntensity / cluster1.unlabeledTotalIntensity;
    double intensityRatio2 = cluster2.labeledTotalIntensity / cluster2.unlabeledTotalIntensity;

    // Check if their ratio is within a reasonable range
    double ratioOfRatios =
        Math.max(intensityRatio1, intensityRatio2) / Math.min(intensityRatio1, intensityRatio2);

    if (ratioOfRatios > 10.0) {
      return false;
    }

    // Simple check 2: look for similar patterns in enrichment profiles
    double patternSimilarity = 0.0;
    int comparedPoints = 0;

    // Find overlapping mass shifts
    for (IsotopeCandidate c1 : cluster1.isotopologues) {
      for (IsotopeCandidate c2 : cluster2.isotopologues) {
        if (c1.massShift == c2.massShift) {
          // Get enrichment ratios for this mass shift
          double ratio1 = Double.NaN;
          double ratio2 = Double.NaN;

          if (cluster1.enrichmentRatios.length > c1.massShift) {
            ratio1 = cluster1.enrichmentRatios[c1.massShift];
          }

          if (cluster2.enrichmentRatios.length > c2.massShift) {
            ratio2 = cluster2.enrichmentRatios[c2.massShift];
          }

          if (!Double.isNaN(ratio1) && !Double.isNaN(ratio2)) {
            // Calculate similarity
            double diff = Math.abs(ratio1 - ratio2) / Math.max(ratio1, ratio2);
            patternSimilarity += (1.0 - diff);
            comparedPoints++;
          }
        }
      }
    }

    // Calculate average similarity
    if (comparedPoints > 0) {
      patternSimilarity /= comparedPoints;
      if (patternSimilarity > 0.6) { // If more than 60% similar
        return true;
      }
    }

    return false;
  }

  /**
   * Merge two isotope clusters
   */
  private void mergeIsotopeClusters(IsotopeGroupResult mainCluster,
      IsotopeGroupResult secondaryCluster) {
    // Calculate exact number of isotope shifts between the two clusters
    double mzDifference = secondaryCluster.baseMz - mainCluster.baseMz;
    int isotopeDifference = (int) Math.round(mzDifference / isotopeMassDifference);

    // If isotope difference is beyond limits, treat as separate patterns
    if (Math.abs(isotopeDifference) > maximumIsotopologues) {
      logger.fine("Skipping merge due to excessive isotope difference: " + isotopeDifference);
      return;
    }

    // Copy isotopologues from the secondary cluster to the main cluster
    for (IsotopeCandidate candidate : secondaryCluster.isotopologues) {
      // Calculate adjusted mass shift based on the relative position of the clusters
      int adjustedMassShift = candidate.massShift;

      // If the secondary cluster has a different base peak, adjust the mass shifts
      if (Math.abs(isotopeDifference) > 0) {
        adjustedMassShift = candidate.massShift + isotopeDifference;
      }

      // Skip if exceeds maximum allowed isotopologues
      if (adjustedMassShift > maximumIsotopologues || adjustedMassShift < 0) {
        continue;
      }

      // Add to main cluster if this is a new isotopologue
      boolean exists = false;
      for (IsotopeCandidate existing : mainCluster.isotopologues) {
        if (existing.row.getID() == candidate.row.getID()) {
          exists = true;
          break;
        }
      }

      if (!exists) {
        mainCluster.isotopologues.add(new IsotopeCandidate(candidate.row, adjustedMassShift));
      }
    }

    // Re-sort isotopologues by mass shift
    Collections.sort(mainCluster.isotopologues, Comparator.comparingInt(c -> c.massShift));

    // Limit to maximum allowed isotopologues
    if (mainCluster.isotopologues.size() > maximumIsotopologues + 1) { // +1 because we include M+0
      List<IsotopeCandidate> limitedIsotopologues = new ArrayList<>(
          mainCluster.isotopologues.subList(0, maximumIsotopologues + 1));
      mainCluster.isotopologues.clear();
      mainCluster.isotopologues.addAll(limitedIsotopologues);
    }
  }

  /**
   * Update cluster IDs in the result feature list after consolidation
   */
  private void updateClusterIds(Map<Integer, Integer> clusterIdMap) {
    // First, clear all existing cluster assignments to avoid conflicts
    for (FeatureListRow row : resultFeatureList.getRows()) {
      ModularFeatureListRow modRow = (ModularFeatureListRow) row;
      modRow.set(UntargetedLabelingParameters.isotopeClusterType, null);
      // Don't clear the isotopologue rank as that's still valid
    }

    // Assign new unique cluster IDs sequentially to the merged results
    for (int i = 0; i < isotopeLabelResults.size(); i++) {
      IsotopeGroupResult cluster = isotopeLabelResults.get(i);
      int newClusterId = i + 1;  // Cluster IDs start from 1

      // Store the cluster ID in the result object for reference
      cluster.clusterId = newClusterId;

      // Assign the new cluster ID to all isotopologues in this cluster
      for (IsotopeCandidate candidate : cluster.isotopologues) {
        ModularFeatureListRow resultRow = findResultRowByFeatureId(candidate.row.getID());
        if (resultRow != null) {
          // Set the new cluster ID
          resultRow.set(UntargetedLabelingParameters.isotopeClusterType, newClusterId);
          resultRow.set(UntargetedLabelingParameters.isotopologueRankType, candidate.massShift);

          // Update the row comment with the new information
          updateRowComment(resultRow, candidate, cluster, newClusterId);

          logger.fine("Assigned cluster ID " + newClusterId + " to row " + resultRow.getID()
              + " with isotopologue rank " + candidate.massShift);
        } else {
          logger.warning("Could not find row for feature ID " + candidate.row.getID());
        }
      }
    }

    // Verify uniqueness for debugging
    Map<Integer, Integer> clusterCounts = new HashMap<>();
    for (FeatureListRow row : resultFeatureList.getRows()) {
      ModularFeatureListRow modRow = (ModularFeatureListRow) row;
      Integer clusterId = modRow.get(UntargetedLabelingParameters.isotopeClusterType);
      if (clusterId != null) {
        clusterCounts.put(clusterId, clusterCounts.getOrDefault(clusterId, 0) + 1);
      }
    }

    // Log cluster statistics
    for (Map.Entry<Integer, Integer> entry : clusterCounts.entrySet()) {
      logger.fine("Cluster ID " + entry.getKey() + " has " + entry.getValue() + " features");
    }

    logger.info("Updated cluster IDs for " + clusterCounts.size() + " clusters");
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