package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Corrector for high-resolution data.
 */
public class HighResMetaboliteCorrector extends LowResMetaboliteCorrector {

  private final double resolution;
  private final double mzOfResolution;
  private final int charge;
  private final String resolutionFormulaCode;
  private final double correctionLimit;
  private final double thresholdP; // Probability threshold for peaks

  // Interface for resolution calculation formulas
  @FunctionalInterface
  private interface ResolutionFormula {

    double calculate(double mw, double res, double atMz);
  }

  // Resolution formulas mapping
  private static final Map<String, ResolutionFormula> RESOLUTION_FORMULAS = new HashMap<>();

  static {
    RESOLUTION_FORMULAS.put("orbitrap",
        (mw, res, atMz) -> 1.66 * Math.pow(mw, 1.5) / (res * Math.sqrt(atMz)));
    RESOLUTION_FORMULAS.put("ft-icr", (mw, res, atMz) -> 1.66 * Math.pow(mw, 2) / (res * atMz));
    RESOLUTION_FORMULAS.put("constant", (mw, res, atMz) -> 1.66 * mw / res);
    RESOLUTION_FORMULAS.put("datafile", (mw, res, atMz) -> 1.66 * mw / res);
  }

  public HighResMetaboliteCorrector(String formula, String tracer, double resolution,
      double mzOfResolution, int charge, String resolutionFormulaCode,
      Map<String, Object> options) {
    super(formula, tracer, options);
    this.resolution = resolution;
    this.mzOfResolution = mzOfResolution;
    this.charge = charge;
    this.resolutionFormulaCode = resolutionFormulaCode;

    // Set threshold based on molecular weight (like Python code)
    this.thresholdP = getMolecularWeight() < 500 ? 0.0 : 1e-10;

    this.correctionLimit = computeCorrectionLimit();

    logger.info("Initialized HighResMetaboliteCorrector with resolution: " + resolution
        + ", mzOfResolution: " + mzOfResolution + ", charge: " + charge);
  }

  private double computeCorrectionLimit() {
    logger.info("Computing correction limit.");

    double mw = getMolecularWeight();
    double res = resolution;
    double atMz = mzOfResolution;

    // Get the appropriate resolution formula
    ResolutionFormula formula = RESOLUTION_FORMULAS.getOrDefault(resolutionFormulaCode,
        RESOLUTION_FORMULAS.get("orbitrap"));

    if (formula == null) {
      logger.warning("Unknown resolution formula code: " + resolutionFormulaCode
          + ". Using 'orbitrap' as default.");
      formula = RESOLUTION_FORMULAS.get("orbitrap");
    }

    // Calculate the correction limit and adjust for charge
    double limit = formula.calculate(mw / Math.abs(charge), res, atMz) * Math.abs(charge);

    // Validate the correction limit
    double precisionMachine = 1000 * Math.ulp(1.0);
    if (limit >= 0.5) {
      throw new IllegalArgumentException("The correction limit is expected to be sufficient"
          + " to distinguish peaks with a delta-mass of 1 amu (>" + limit + ">0.5)");
    } else if (limit < precisionMachine) {
      logger.warning("Correction limit is close to machine limits for floating point"
          + " operations. Correction limit reset to: " + precisionMachine);
      limit = precisionMachine;
    }

    logger.info("Computed correction limit: " + limit);
    return limit;
  }

  /**
   * Count the number of isotopomers for a given isoblock (permutations) Equivalent to
   * _count_isotopomers python function
   */
  private int countIsotopomers(List<Integer> isoblock) {
    // Calculate denominator (product of factorials of counts)
    Map<Integer, Integer> counts = new HashMap<>();
    for (int x : isoblock) {
      counts.put(x, counts.getOrDefault(x, 0) + 1);
    }

    int denominator = 1;
    for (int count : counts.values()) {
      denominator *= factorial(count);
    }

    // Return n! / (a! * b! * c! * ...)
    return factorial(isoblock.size()) / denominator;
  }

  /**
   * Calculate factorial of n
   */
  private int factorial(int n) {
    if (n <= 1) {
      return 1;
    }
    int result = 1;
    for (int i = 2; i <= n; i++) {
      result *= i;
    }
    return result;
  }

  /**
   * Get block for element with 1 isotope Equivalent _get_block_1 python function
   */
  private List<Pair<Double, Double>> getBlock1(double mass, int nAtoms) {
    List<Pair<Double, Double>> result = new ArrayList<>();
    result.add(new Pair<>(mass * nAtoms, 1.0));
    return result;
  }

  /**
   * Get block for element with 2 isotopes Equivalent to _get_block_2 python function
   */
  private List<Pair<Double, Double>> getBlock2(double[] masses, double[] abundances, int nAtoms) {
    // Initialize with [1.0]
    double[] probabilities = {1.0};

    // Convolve n times with abundances
    for (int i = 0; i < nAtoms; i++) {
      probabilities = convolve(probabilities, abundances);
    }

    // Create result pairs
    List<Pair<Double, Double>> result = new ArrayList<>();
    for (int i = 0; i <= nAtoms; i++) {
      double mass = masses[0] * (nAtoms - i) + masses[1] * i;
      result.add(new Pair<>(mass, probabilities[i]));
    }

    return result;
  }

  /**
   * Get block for element with n isotopes Equivalent to _get_block_n pytohn function
   */
  private List<Pair<Double, Double>> getBlockN(double[] masses, double[] abundances, int nAtoms,
      int nIsotopes) {
    List<Pair<Double, Double>> results = new ArrayList<>();

    // Generate all combinations with replacement
    List<List<Integer>> allIsoblocks = generateCombinationsWithReplacement(nIsotopes, nAtoms);

    for (List<Integer> isoblock : allIsoblocks) {
      // Calculate mass of this block
      double blockMass = 0;
      for (int i : isoblock) {
        blockMass += masses[i];
      }

      // Calculate probability for this block
      double blockAbundance = 1.0;
      for (int i : isoblock) {
        blockAbundance *= abundances[i];
      }

      // Correct for number of isotopomeres
      int nIsotopomeres = countIsotopomers(isoblock);
      blockAbundance *= nIsotopomeres;

      results.add(new Pair<>(blockMass, blockAbundance));
    }

    return results;
  }

  /**
   * Generate all combinations with replacement Equivalent to
   * itertools.combinations_with_replacement python function
   */
  private List<List<Integer>> generateCombinationsWithReplacement(int n, int r) {
    if (n <= 0 || r <= 0) {
      return new ArrayList<>();
    }

    // Pre-allocate expected size
    List<List<Integer>> result = new ArrayList<>(
        (int) Math.min(Math.pow(n, r),  // Upper bound for all combinations with replacement
            10000));         // Practical limit to avoid very large allocations

    // Initialize first combination
    int[] current = new int[r];

    // Generate all combinations iteratively rather than recursively
    boolean finished = false;
    while (!finished) {
      // Add current combination to result
      List<Integer> combination = new ArrayList<>(r);
      for (int i = 0; i < r; i++) {
        combination.add(current[i]);
      }
      result.add(combination);

      // Generate next combination
      int pos = r - 1;
      while (pos >= 0 && current[pos] == n - 1) {
        pos--;
      }

      if (pos < 0) {
        finished = true;
      } else {
        current[pos]++;
        for (int i = pos + 1; i < r; i++) {
          current[i] = current[pos];
        }
      }
    }

    return result;
  }

  /**
   * Get all isotopic blocks for all elements, loading missing elements on demand. Equivalent to
   * _get_isotopic_blocks python function
   */
  private Map<String, List<Pair<Double, Double>>> getIsotopicBlocks() {
    Map<String, List<Pair<Double, Double>>> data = new HashMap<>();

    // Work element by element to make elemental blocks
    for (Map.Entry<String, Integer> entry : correctionFormula.entrySet()) {
      String element = entry.getKey();
      int nAtoms = entry.getValue();

      // Check if element is in our database
      if (!dataIsotopes.containsKey(element)) {
        logger.info(
            "Element " + element + " not found in current isotope data. Attempting to load it.");

        // Try to load data specifically for this element
        Map<String, Map<String, double[]>> specificData = loadSpecificElementIsotopeData(element);

        // If we got data for this element, add it to our existing data
        if (specificData.containsKey(element)) {
          dataIsotopes.put(element, specificData.get(element));
          logger.info("Successfully loaded isotope data for " + element + " in getIsotopicBlocks");
        } else {
          logger.warning(
              "Element " + element + " not found in isotope data. Skipping in isotopic blocks.");
          continue;
        }
      }

      double[] masses = dataIsotopes.get(element).get("mass");
      double[] abundances = dataIsotopes.get(element).get("abundance");
      int nIsotopes = masses.length;

      // Select appropriate method based on number of isotopes
      List<Pair<Double, Double>> elementBlocks;
      if (nIsotopes == 1) {
        // Element has only 1 isotope, probability is 1.0 by definition
        elementBlocks = getBlock1(masses[0], nAtoms);
      } else if (nIsotopes == 2) {
        // Element has 2 isotopes, use convolution method
        elementBlocks = getBlock2(masses, abundances, nAtoms);
      } else {
        // Element has 3+ isotopes, use full computation
        elementBlocks = getBlockN(masses, abundances, nAtoms, nIsotopes);
      }

      // Filter by threshold
      if (thresholdP > 0) {
        elementBlocks.removeIf(pair -> pair.getSecond() <= thresholdP);
      }

      data.put(element, elementBlocks);
    }

    return data;
  }

  /**
   * Combine blocks of isotopes to compute the isotopic cluster Equivalent to _combine_blocks python
   * function
   */
  private Map<Double, Double> combineBlocks(Map<String, List<Pair<Double, Double>>> groups) {
    // If no groups, return empty result
    if (groups.isEmpty()) {
      return new HashMap<>();
    }

    // Create list of element groups to process
    List<String> elements = new ArrayList<>(groups.keySet());

    // Start with the first element's blocks
    Map<Double, Double> result = new HashMap<>();
    for (Pair<Double, Double> pair : groups.get(elements.get(0))) {
      result.put(pair.getFirst(), pair.getSecond());
    }

    // Combine with remaining elements
    for (int i = 1; i < elements.size(); i++) {
      List<Pair<Double, Double>> elementBlocks = groups.get(elements.get(i));
      Map<Double, Double> newResult = new HashMap<>();

      // Combine all pairs
      for (Map.Entry<Double, Double> existingEntry : result.entrySet()) {
        for (Pair<Double, Double> newPair : elementBlocks) {
          double newMass = existingEntry.getKey() + newPair.getFirst();
          double newProba = existingEntry.getValue() * newPair.getSecond();

          // Add or merge
          newResult.put(newMass, newResult.getOrDefault(newMass, 0.0) + newProba);
        }
      }

      result = newResult;
    }

    // Trim out peaks below probability threshold
    if (thresholdP > 0) {
      result.entrySet().removeIf(entry -> entry.getValue() <= thresholdP);
    }

    return result;
  }

  /**
   * Get the full isotopic cluster for non-tracer elements Equivalent to get_isotopic_cluster python
   * function
   */
  private Map<Double, Double> getIsotopicCluster() {
    logger.info("Computing isotopic cluster.");

    if (correctionFormula.isEmpty()) {
      // No atoms, return single peak at mass 0
      Map<Double, Double> emptyCluster = new HashMap<>();
      emptyCluster.put(0.0, 1.0);
      return emptyCluster;
    }

    // Get isotopic blocks for all elements
    Map<String, List<Pair<Double, Double>>> groups = getIsotopicBlocks();

    // Combine all blocks
    Map<Double, Double> isoClust = combineBlocks(groups);

    logger.info("Isotopic cluster computed with " + isoClust.size() + " peaks.");
    return isoClust;
  }

  /**
   * Get masses of peaks that originate from the tracer Equivalent to
   * get_tracershifted_peaks_between python function
   */
  private double[] getTracerShiftedPeaksBetween(double mzMin, double mzMax) {
    if (mzMax <= mzMin) {
      throw new IllegalArgumentException("mzMax must be greater than mzMin");
    }

    // Calculate mass shift
    double[] masses = dataIsotopes.get(tracerElement).get("mass");
    double mzShift = masses[tracerIsotopeIndex] - masses[0];

    // Calculate number of peaks
    int nPeaks = (int) Math.floor((mzMax - mzMin) / mzShift) + 1;
    double[] idMass = new double[nPeaks];

    // Generate peak masses
    for (int n = 0; n < nPeaks; n++) {
      idMass[n] = mzMin + n * mzShift;
    }

    return idMass;
  }

  /**
   * Sum unresolved peaks according to MS resolution Equivalent to get_peaks_around python function
   */
  private double[] getPeaksAround(Map<Double, Double> isotopicCluster, double[] masses) {
    double[] pooled = new double[masses.length];

    // For each requested mass position
    for (int i = 0; i < masses.length; i++) {
      double peak = masses[i];
      double sum = 0.0;

      // Sum all peaks within correction limit
      for (Map.Entry<Double, Double> entry : isotopicCluster.entrySet()) {
        if (Math.abs(entry.getKey() - peak) <= correctionLimit) {
          sum += entry.getValue();
        }
      }
      pooled[i] = sum;
    }
    return pooled;
  }

  /**
   * Compute the correction matrix for high-resolution data.
   *
   * @return The correction matrix as a RealMatrix object.
   */
  private RealMatrix computeCorrectionMatrixCombination() {
    logger.info("Computing high-resolution correction matrix with optimized matrix operations.");

    int nTracers = formula.getOrDefault(tracerElement, 0);
    Map<String, double[]> dataTracer = dataIsotopes.get(tracerElement);
    int nTracerIsotopes = dataTracer.get("mass").length;
    int nIsotopologues = nTracers + 1;

    // Get isotopic cluster for non-tracer elements - cache this result
    Map<Double, Double> isoclustNotracer = getIsotopicCluster();

    // Prepare correction matrix with proper dimensions
    RealMatrix correctionMatrix = MatrixUtils.createRealMatrix(nIsotopologues, nIsotopologues);

    // Use efficient array-based operations where possible
    double mzShift = dataTracer.get("mass")[tracerIsotopeIndex] - dataTracer.get("mass")[0];
    double minimumMass = calculateMinimumMass();
    double maxMass = minimumMass + mzShift * nTracers + 0.5;

    // Calculate main peak positions once
    double[] mainPeaks = getTracerShiftedPeaksBetween(minimumMass, maxMass);

    // Process each column of the correction matrix in parallel if possible
    for (int nTracedAtoms = 0; nTracedAtoms < nIsotopologues; nTracedAtoms++) {
      // Create final variable for lambda use
      final int finalNTracedAtoms = nTracedAtoms;

      // Prepare data for this column
      Map<String, List<Pair<Double, Double>>> data = new HashMap<>();

      // Add non-tracer cluster if available
      if (isoclustNotracer != null && !isoclustNotracer.isEmpty()) {
        List<Pair<Double, Double>> clusterNotracer = new ArrayList<>(isoclustNotracer.size());
        for (Map.Entry<Double, Double> entry : isoclustNotracer.entrySet()) {
          clusterNotracer.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        data.put("cluster_notracer", clusterNotracer);
      }

      // Add tracer data with pre-allocated lists for better memory efficiency
      addTracerData(data, nTracedAtoms, nTracers, nTracerIsotopes, dataTracer);

      // Combine to get the isotopic cluster for this column
      Map<Double, Double> isoClust = combineBlocks(data);

      // Pool together unresolved peaks
      double[] column = getPeaksAround(isoClust, mainPeaks);

      // Fill the column directly
      for (int j = 0; j < Math.min(nIsotopologues, column.length); j++) {
        correctionMatrix.setEntry(j, finalNTracedAtoms, column[j]);
      }
    }

    normalizeColumns(correctionMatrix);
    return correctionMatrix;
  }

  // Add this helper method for calculating minimum mass
  private double calculateMinimumMass() {
    double minimumMass = 0.0;
    for (Map.Entry<String, Integer> entry : correctionFormula.entrySet()) {
      String element = entry.getKey();
      int nElements = entry.getValue();

      // Check if element is in our database and load if needed
      if (!dataIsotopes.containsKey(element)) {
        Map<String, Map<String, double[]>> specificData = loadSpecificElementIsotopeData(element);
        if (specificData.containsKey(element)) {
          dataIsotopes.put(element, specificData.get(element));
        } else {
          logger.warning("Element " + element + " not found. Using 0 for mass contribution.");
          continue;
        }
      }

      minimumMass += nElements * dataIsotopes.get(element).get("mass")[0];
    }
    return minimumMass;
  }

  // Add this helper method for adding tracer data to the map
  private void addTracerData(Map<String, List<Pair<Double, Double>>> data, int nTracedAtoms,
      int nTracers, int nTracerIsotopes, Map<String, double[]> dataTracer) {
    // Handle labeled atoms - tracer purity
    if (nTracedAtoms > 0) {
      data.put("purity_tracer",
          getBlockN(dataTracer.get("mass"), tracerPurity, nTracedAtoms, nTracerIsotopes));
    }

    // Handle unlabeled tracer atoms
    if (nTracers - nTracedAtoms > 0) {
      if (correctNATracer) {
        // Apply natural abundance correction
        data.put("NA_tracer",
            getBlockN(dataTracer.get("mass"), dataTracer.get("abundance"), nTracers - nTracedAtoms,
                nTracerIsotopes));
      } else {
        // Just use the first isotope
        data.put("NA_tracer", getBlock1(dataTracer.get("mass")[0], nTracers - nTracedAtoms));
      }
    }
  }

  // Add this helper method for normalizing columns
  private void normalizeColumns(RealMatrix matrix) {
    int cols = matrix.getColumnDimension();
    int rows = matrix.getRowDimension();

    for (int col = 0; col < cols; col++) {
      double sum = 0.0;
      for (int row = 0; row < rows; row++) {
        sum += matrix.getEntry(row, col);
      }

      if (sum > 0) {
        for (int row = 0; row < rows; row++) {
          matrix.setEntry(row, col, matrix.getEntry(row, col) / sum);
        }
      }
    }
  }

  /**
   * Computes the correction matrix taking into account all parameters. The correction matrix is
   * used to convert between isotopologue fractions and measured peak intensities, accounting for
   * natural abundance and tracer purity. Each column of the matrix represents the expected
   * measurement pattern for a specific isotopologue.
   *
   * @return A square matrix where rows and columns represent isotopologues
   */
  @Override
  protected RealMatrix computeCorrectionMatrix() {
    logger.info("Computing high-resolution correction matrix.");

    // Get number of isotopes for tracer
    int nTracerIsotopes = dataIsotopes.get(tracerElement).get("abundance").length;

    // Check that we have at least 2 isotopes for tracer
    if (nTracerIsotopes <= 1) {
      throw new IllegalArgumentException(
          "Unexpected number of isotopes for tracer: " + nTracerIsotopes);
    }

    // Select method based on number of tracer isotopes
    RealMatrix matrix;
    if (nTracerIsotopes == 2) {
      // Use convolution method for tracer with 2 isotopes
      matrix = super.computeCorrectionMatrix();
    } else {
      // Use combination method for tracer with 3+ isotopes
      matrix = computeCorrectionMatrixCombination();
    }

    // Ensure columns sum to 1
    int cols = matrix.getColumnDimension();
    int rows = matrix.getRowDimension();

    for (int col = 0; col < cols; col++) {
      double sum = 0.0;
      for (int row = 0; row < rows; row++) {
        sum += matrix.getEntry(row, col);
      }

      if (sum > 0) {
        for (int row = 0; row < rows; row++) {
          matrix.setEntry(row, col, matrix.getEntry(row, col) / sum);
        }
      }
    }

    logger.info("Correction matrix computed with dimensions: " + rows + "x" + cols);
    return matrix;
  }

  /**
   * Utility class for holding mass-abundance pairs
   */
  private static class Pair<K, V> {

    private final K first;
    private final V second;

    public Pair(K first, V second) {
      this.first = first;
      this.second = second;
    }

    public K getFirst() {
      return first;
    }

    public V getSecond() {
      return second;
    }

    @Override
    public String toString() {
      return "(" + first + ", " + second + ")";
    }
  }
}
