package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import io.github.mzmine.modules.dataprocessing.isolab_natabundance.LowResMetaboliteCorrector.CorrectedResult;
import io.github.mzmine.modules.dataprocessing.isolab_natabundance.LowResMetaboliteCorrector.HighResMetaboliteCorrector;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;

/**
 * Factory class to create MetaboliteCorrector instances.
 */
public class MetaboliteCorrectorFactory {

  private static final Logger logger = Logger.getLogger(MetaboliteCorrectorFactory.class.getName());

  /**
   * Creates a metabolite corrector based on the provided parameters.
   *
   * @param formula The elemental formula of the metabolite.
   * @param tracer  The isotopic tracer (e.g., "13C").
   * @param options Additional parameters for the corrector.
   * @return A configured instance of MetaboliteCorrector.
   */
  public static MetaboliteCorrector createCorrector(String formula, String tracer,
      Map<String, Object> options) {
    logger.info("Creating MetaboliteCorrector for formula: " + formula + " and tracer: " + tracer);

    if (options == null) {
      options = new HashMap<>();
    }
    double resolution = (double) options.getOrDefault("resolution", 0.0);
    Double mzOfResolution = (Double) options.getOrDefault("mzOfResolution", null);
    Integer charge = (Integer) options.getOrDefault("charge", null);

    if (resolution > 0 && mzOfResolution != null && charge != null) {
      logger.info("Creating HighResMetaboliteCorrector with resolution parameters.");
      return new HighResMetaboliteCorrector(formula, tracer, resolution, mzOfResolution, charge,
          (String) options.getOrDefault("resolutionFormulaCode", "orbitrap"), options);
    } else {
      logger.info("Creating LowResMetaboliteCorrector as default.");
      return new LowResMetaboliteCorrector(formula, tracer, options);
    }
  }
}

/**
 * Base abstract class for metabolite correctors.
 */
abstract class MetaboliteCorrector {

  protected Map<String, Integer> formula; // Elemental formula (e.g., {"C": 6, "H": 12, "O": 6})
  protected String tracer; // Isotopic tracer (e.g., "13C")
  protected Map<String, Object> options; // Additional parameters
  protected Map<String, Map<String, double[]>> dataIsotopes; // Isotopic data (mass and abundance)
  protected double[] tracerPurity; // Tracer purity (proportions of isotopes)
  protected boolean correctNATracer; // Flag to correct natural abundance of tracer
  protected Map<String, Integer> correctionFormula; // Formula for correction (excluding tracer)
  protected String tracerElement; // Element part of the tracer (e.g., "C" from "13C")
  protected int tracerIsotopeIndex; // Index of the tracer isotope in isotope data array

  public MetaboliteCorrector(String formula, String tracer, Map<String, Object> options) {
    logger.info(
        "Initializing MetaboliteCorrector with formula: " + formula + " and tracer: " + tracer);

    this.formula = parseFormula(formula);
    this.tracer = tracer;
    this.options = options != null ? options : new HashMap<>();
    this.dataIsotopes = getDefaultIsotopicData();

    // Parse tracer to extract element and isotope index
    parseTracer(tracer);

    this.tracerPurity = getTracerPurity(this.options);
    this.correctNATracer = (boolean) this.options.getOrDefault("correct_NA_tracer", true);
    this.correctionFormula = getCorrectionFormula(this.formula, this.tracerElement);
  }

  private Map<String, Integer> parseFormula(String formula) {
    logger.info("Parsing formula: " + formula);

    Map<String, Integer> formulaMap = new HashMap<>();
    if (formula == null || formula.isEmpty()) {
      throw new IllegalArgumentException("Formula cannot be null or empty.");
    }

    // Improved formula parsing with better regex
    String elementPattern = "([A-Z][a-z]*)([0-9]*)";
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(elementPattern);
    java.util.regex.Matcher matcher = pattern.matcher(formula);

    while (matcher.find()) {
      String element = matcher.group(1);
      String countStr = matcher.group(2);
      int count = countStr.isEmpty() ? 1 : Integer.parseInt(countStr);
      formulaMap.put(element, count);
    }

    if (formulaMap.isEmpty()) {
      throw new IllegalArgumentException("Failed to parse any elements from formula: " + formula);
    }

    logger.info("Parsed formula: " + formulaMap);
    return formulaMap;
  }

  /**
   * Parse the tracer string to extract the element and isotope index
   */
  private void parseTracer(String tracerStr) {
    logger.info("Parsing tracer: " + tracerStr);

    try {
      // Extract isotope mass number and element symbol
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([A-Z][a-z]*)");
      java.util.regex.Matcher matcher = pattern.matcher(tracerStr);

      if (!matcher.find()) {
        throw new IllegalArgumentException("Invalid tracer format: " + tracerStr);
      }

      int tracerMass = Integer.parseInt(matcher.group(1));
      this.tracerElement = matcher.group(2);

      if (!dataIsotopes.containsKey(tracerElement)) {
        throw new IllegalArgumentException(
            "Tracer element not found in isotope data: " + tracerElement);
      }

      // Find the closest isotope by mass
      double[] masses = dataIsotopes.get(tracerElement).get("mass");
      double bestDiff = Double.POSITIVE_INFINITY;
      this.tracerIsotopeIndex = 0;

      for (int i = 0; i < masses.length; i++) {
        double diff = Math.abs(masses[i] - tracerMass);
        if (diff < bestDiff) {
          bestDiff = diff;
          tracerIsotopeIndex = i;
        }
      }

      if (bestDiff > 0.5) {
        throw new IllegalArgumentException("No matching isotope found for tracer: " + tracerStr);
      }

    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse tracer: " + tracerStr, e);
    }

    logger.info("Parsed tracer: element=" + tracerElement + ", isotopeIndex=" + tracerIsotopeIndex);
  }

  private Map<String, Map<String, double[]>> getDefaultIsotopicData() {
    logger.info("Loading default isotopic data.");

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

    logger.info("Loaded default isotopic data.");
    return isotopes;
  }

  private double[] getTracerPurity(Map<String, Object> options) {
    logger.info("Getting tracer purity.");

    // Get tracer purity from options if available
    double[] purity = (double[]) options.getOrDefault("tracerPurity", null);

    // If not provided, create default purity array (perfect purity)
    if (purity == null) {
      double[] abundances = dataIsotopes.get(tracerElement).get("abundance");
      purity = new double[abundances.length];

      // Set all to 0 except the tracer isotope which gets 1.0 (perfect purity)
      for (int i = 0; i < purity.length; i++) {
        purity[i] = (i == tracerIsotopeIndex) ? 1.0 : 0.0;
      }
    }

    // Validate the length
    if (purity.length != dataIsotopes.get(tracerElement).get("abundance").length) {
      throw new IllegalArgumentException("Tracer purity array length doesn't match isotope data");
    }

    // Log the purity values
    StringBuilder logMsg = new StringBuilder("Tracer purity: ");
    for (double p : purity) {
      logMsg.append(p).append(" ");
    }
    logger.info(logMsg.toString());

    return purity;
  }

  private Map<String, Integer> getCorrectionFormula(Map<String, Integer> formula, String element) {
    logger.info("Computing correction formula for element: " + element);

    Map<String, Integer> correctionFormula = new HashMap<>(formula);
    Integer count = correctionFormula.remove(element);

    if (count == null) {
      logger.warning(
          "Element " + element + " not found in formula. Using full formula for correction.");
      return new HashMap<>(formula);  // Return a copy of the original formula
    }

    logger.info("Correction formula: " + correctionFormula);
    return correctionFormula;
  }

  /**
   * Compute the molecular weight of the compound
   */
  protected double getMolecularWeight() {
    logger.info("Computing molecular weight.");

    double mw = 0.0;
    for (Map.Entry<String, Integer> entry : formula.entrySet()) {
      String element = entry.getKey();
      int count = entry.getValue();

      if (!dataIsotopes.containsKey(element)) {
        logger.warning("Element " + element
            + " not found in isotope data. Skipping in molecular weight calculation.");
        continue;
      }

      mw += count * dataIsotopes.get(element).get("mass")[0]; // Use the first isotope mass
    }

    logger.info("Molecular weight computed: " + mw);
    return mw;
  }

  /**
   * Helper method to convolve two arrays.
   */
  protected double[] convolve(double[] a, double[] b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("Input arrays for convolution cannot be null.");
    }
    if (a.length == 0 || b.length == 0) {
      throw new IllegalArgumentException("Input arrays for convolution cannot be empty.");
    }

    double[] result = new double[a.length + b.length - 1];
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < b.length; j++) {
        result[i + j] += a[i] * b[j];
      }
    }
    return result;
  }

  public abstract CorrectedResult correct(double[] measurements);
}

/**
 * Corrector for low-resolution data.
 */
class LowResMetaboliteCorrector extends MetaboliteCorrector {

  public LowResMetaboliteCorrector(String formula, String tracer, Map<String, Object> options) {
    super(formula, tracer, options);
    logger.info("Initialized LowResMetaboliteCorrector.");
  }

  @Override
  public CorrectedResult correct(double[] measurements) {
    logger.info("Starting correction for low-resolution data.");

    // Ensure measurements are valid
    if (measurements == null || measurements.length == 0) {
      throw new IllegalArgumentException("Measurements array cannot be null or empty.");
    }
    for (double measurement : measurements) {
      if (measurement < 0) {
        throw new IllegalArgumentException("Measurements must be non-negative.");
      }
    }

    // Get the number of isotopologues
    int nIsotopologues = formula.getOrDefault(tracerElement, 0) + 1;
    logger.info("Number of isotopologues: " + nIsotopologues);

    // Check if measurement length matches number of isotopologues
    if (measurements.length != nIsotopologues) {
      throw new IllegalArgumentException(
          "The length of the measured isotopic cluster (" + measurements.length
              + ") is different than the required number of measurements: " + nIsotopologues
              + " (i.e. N + 1, where N is the number of atoms that could be traced)");
    }

    // If all measurements are zero, return zero
    if (Arrays.stream(measurements).allMatch(m -> m == 0)) {
      logger.warning("All measurements are zero. Returning default result.");
      double[] equalFractions = new double[nIsotopologues];
      Arrays.fill(equalFractions, 0.0); //1.0 / nIsotopologues);
      return new CorrectedResult(new double[nIsotopologues], equalFractions,
          new double[nIsotopologues], 0.0);
    }

    // Compute the correction matrix
    RealMatrix correctionMatrix = computeCorrectionMatrix();
    logger.info(
        "Correction matrix computed with dimensions: " + correctionMatrix.getRowDimension() + "x"
            + correctionMatrix.getColumnDimension());

    // Optimize isotopologue fractions
    double[] isotopologueFractions = optimizeIsotopologueFractions(measurements, correctionMatrix);
    logger.info("Isotopologue fractions optimized.");

    // Calculate corrected areas and residuals
    RealVector correctedAreas = correctionMatrix.operate(
        new ArrayRealVector(isotopologueFractions));
    RealVector residuals = new ArrayRealVector(measurements).subtract(correctedAreas);

    // Normalize the isotopologue fractions to sum to 1
    double sumFractions = Arrays.stream(isotopologueFractions).sum();
    if (sumFractions > 0) {
      for (int i = 0; i < isotopologueFractions.length; i++) {
        isotopologueFractions[i] /= sumFractions;
      }
    } else {
      logger.warning("Sum of isotopologue fractions is zero or negative. Using equal fractions.");
      Arrays.fill(isotopologueFractions, 1.0 / isotopologueFractions.length);
    }

    // Normalize residuals
    double sumMeasurements = Arrays.stream(measurements).sum();
    double[] normalizedResiduals = new double[residuals.getDimension()];
    for (int i = 0; i < residuals.getDimension(); i++) {
      normalizedResiduals[i] = (sumMeasurements > 0) ? residuals.getEntry(i) / sumMeasurements : 0;
    }

    // Calculate mean enrichment
    double meanEnrichment = 0.0;
    int totalTracerAtoms = formula.getOrDefault(tracerElement, 0);
    if (isotopologueFractions.length > 1 && totalTracerAtoms > 0) {
      for (int i = 0; i < isotopologueFractions.length; i++) {
        meanEnrichment += i * isotopologueFractions[i];
      }
      meanEnrichment /= totalTracerAtoms;
    }

    logger.info("Correction completed successfully.");
    return new CorrectedResult(correctedAreas.toArray(), isotopologueFractions, normalizedResiduals,
        meanEnrichment);
  }

  protected RealMatrix computeCorrectionMatrix() {
    logger.info("Computing correction matrix.");

    double[] correctionVector = getMassDistributionVector();
    int nIsotopologues = formula.getOrDefault(tracerElement, 0) + 1;
    RealMatrix correctionMatrix = MatrixUtils.createRealMatrix(nIsotopologues, nIsotopologues);

    for (int i = 0; i < nIsotopologues; i++) {
      double[] column = correctionVector.clone();

      // Convolve with tracer purity i times for the i-th labeled isotopologue
      for (int j = 0; j < i; j++) {
        column = convolve(column, tracerPurity);
      }

      // If correcting for natural abundance of tracer
      if (correctNATracer) {
        // Convolve with natural abundance of unlabeled element
        for (int j = 0; j < nIsotopologues - i - 1; j++) {
          column = convolve(column, dataIsotopes.get(tracerElement).get("abundance"));
        }
      }

      // Fill the column of the correction matrix
      int maxRows = Math.min(nIsotopologues, column.length);
      for (int j = 0; j < maxRows; j++) {
        correctionMatrix.setEntry(j, i, column[j]);
      }
    }

    // Ensure each column sums to 1.0 (proper probability distribution)
    for (int col = 0; col < nIsotopologues; col++) {
      double sum = 0.0;
      for (int row = 0; row < nIsotopologues; row++) {
        sum += correctionMatrix.getEntry(row, col);
      }
      if (sum > 0) {
        for (int row = 0; row < nIsotopologues; row++) {
          correctionMatrix.setEntry(row, col, correctionMatrix.getEntry(row, col) / sum);
        }
      }
    }

    return correctionMatrix;
  }

  private double[] getMassDistributionVector() {
    logger.info("Computing mass distribution vector.");

    double[] result = {1.0};  // Start with probability 1.0 for no isotope shift

    // For each element in the correction formula
    for (Map.Entry<String, Integer> entry : correctionFormula.entrySet()) {
      String element = entry.getKey();
      int nAtoms = entry.getValue();

      if (!dataIsotopes.containsKey(element)) {
        logger.warning("Element " + element + " not found in isotope data. Skipping.");
        continue;
      }

      double[] abundances = dataIsotopes.get(element).get("abundance");

      // Convolve the isotopic distribution for each atom of this element
      for (int i = 0; i < nAtoms; i++) {
        result = convolve(result, abundances);
      }
    }

    logger.info("Mass distribution vector computed. Length: " + result.length);
    return result;
  }

  private double[] optimizeIsotopologueFractions(double[] measurements,
      RealMatrix correctionMatrix) {
    logger.info("Starting optimization of isotopologue fractions.");

    int numVariables = correctionMatrix.getColumnDimension();

    // Define the cost function that calculates sum of squared residuals
    MultivariateFunction costFunction = new MultivariateFunction() {
      @Override
      public double value(double[] isotopologueFractions) {
        RealVector predicted = correctionMatrix.operate(new ArrayRealVector(isotopologueFractions));
        RealVector residuals = new ArrayRealVector(measurements).subtract(predicted);
        return residuals.dotProduct(residuals);  // Sum of squared residuals
      }
    };

    // Set up bounds for the solution (isotopologue fractions must be >= 0)
    double[] lowerBounds = new double[numVariables];
    double[] upperBounds = new double[numVariables];
    Arrays.fill(lowerBounds, 0.0);  // Non-negative constraint
    Arrays.fill(upperBounds, Double.POSITIVE_INFINITY);  // No upper bound

    // Initial guess starting with zeros (like Python np.zeros)
    double[] initialGuess = new double[numVariables];
    Arrays.fill(initialGuess, 0.0);

    try {
      // BOBYQA optimizer
      BOBYQAOptimizer optimizer = new BOBYQAOptimizer(2 * numVariables + 1);

      PointValuePair result = optimizer.optimize(new MaxEval(1000),
          // Comparable to Python's max iterations
          new ObjectiveFunction(costFunction), GoalType.MINIMIZE, new InitialGuess(initialGuess),
          new SimpleBounds(lowerBounds, upperBounds));

      logger.info("Optimization completed successfully.");
      return result.getPoint();
    } catch (Exception e) {
      logger.warning("Optimization failed: " + e.getMessage() + ". Using fallback.");

      // Fallback to a simple solution if optimization fails
      // Start with equal distribution
      double[] fallbackResult = new double[numVariables];
      Arrays.fill(fallbackResult, 0.0);

      return fallbackResult;
    }
  }

  /**
   * Corrector for high-resolution data.
   */
  static class HighResMetaboliteCorrector extends LowResMetaboliteCorrector {

    private final double resolution;
    private final double mzOfResolution;
    private final int charge;
    private final String resolutionFormulaCode;
    private final double correctionLimit;

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
    }

    public HighResMetaboliteCorrector(String formula, String tracer, double resolution,
        double mzOfResolution, int charge, String resolutionFormulaCode,
        Map<String, Object> options) {
      super(formula, tracer, options);
      this.resolution = resolution;
      this.mzOfResolution = mzOfResolution;
      this.charge = charge;
      this.resolutionFormulaCode = resolutionFormulaCode;
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

    private double[] getTracerShiftedPeaks(Map<Double, Double> isotopicCluster) {
      logger.info("Computing tracer-shifted peaks.");

      // Calculate mass shift between tracer isotope and first isotope
      double[] masses = dataIsotopes.get(tracerElement).get("mass");
      double mzShift = masses[tracerIsotopeIndex] - masses[0];

      // Get min mass from molecular weight or isotopic cluster
      double minMass = isotopicCluster.isEmpty() ? getMolecularWeight()
          : Collections.min(isotopicCluster.keySet());

      // Create array with exactly nIsotopologues peaks
      int nIsotopologues = formula.getOrDefault(tracerElement, 0) + 1;
      double[] mainPeaks = new double[nIsotopologues];

      for (int i = 0; i < nIsotopologues; i++) {
        mainPeaks[i] = minMass + i * mzShift;
      }

      return mainPeaks;
    }

    private Map<Double, Double> shiftIsotopicCluster(Map<Double, Double> cluster, int nLabeled) {
      // Calculate mass shift for labeled atoms
      double[] masses = dataIsotopes.get(tracerElement).get("mass");
      double massShift = masses[tracerIsotopeIndex] - masses[0];
      double totalShift = massShift * nLabeled;

      // Create new cluster with shifted masses
      Map<Double, Double> shiftedCluster = new HashMap<>();
      for (Map.Entry<Double, Double> peak : cluster.entrySet()) {
        shiftedCluster.put(peak.getKey() + totalShift, peak.getValue());
      }

      return shiftedCluster;
    }

    @Override
    protected RealMatrix computeCorrectionMatrix() {
      logger.info("Computing high-resolution correction matrix.");

      // Get isotopic cluster for all non-tracer elements
      Map<Double, Double> isotopicCluster = getIsotopicCluster();
      logger.info("Isotopic cluster computed with " + isotopicCluster.size() + " peaks.");

      // Determine main peak positions
      double[] mainPeaks = getTracerShiftedPeaks(isotopicCluster);
      logger.info("Tracer-shifted peaks computed. Number of peaks: " + mainPeaks.length);

      // Create matrix with dimensions that match the number of isotopologues
      int nIsotopologues = formula.getOrDefault(tracerElement, 0) + 1;
      RealMatrix correctionMatrix = MatrixUtils.createRealMatrix(nIsotopologues, nIsotopologues);

      // For each isotopologue (column in matrix)
      for (int i = 0; i < nIsotopologues; i++) {
        // Shift isotopic cluster for each isotopologue
        Map<Double, Double> shiftedCluster = shiftIsotopicCluster(isotopicCluster, i);

        // For each peak position (row in matrix)
        for (int j = 0; j < nIsotopologues; j++) {
          double intensity = 0.0;

          if (j < mainPeaks.length) {
            double peakMass = mainPeaks[j];

            // Sum all isotopic cluster peaks that fall within correction limit
            for (Map.Entry<Double, Double> peak : shiftedCluster.entrySet()) {
              if (Math.abs(peak.getKey() - peakMass) <= correctionLimit) {
                intensity += peak.getValue();
              }
            }
          }

          correctionMatrix.setEntry(j, i, intensity);
        }
      }

      logger.info("High-resolution correction matrix computed. Dimensions: "
          + correctionMatrix.getRowDimension() + "x" + correctionMatrix.getColumnDimension());
      return correctionMatrix;
    }

    private Map<Double, Double> getIsotopicCluster() {
      logger.info("Computing isotopic cluster.");

      // Start with a single peak at mass 0 with probability 1.0
      Map<Double, Double> isotopicCluster = new HashMap<>();
      isotopicCluster.put(0.0, 1.0);

      // If there's no correction formula, return this trivial cluster
      if (correctionFormula.isEmpty()) {
        return isotopicCluster;
      }

      // Probability threshold for peaks to keep
      double thresholdP = formula.get(tracerElement) > 20 ? 1e-10 : 0.0;

      // For each element in the correction formula
      for (Map.Entry<String, Integer> entry : correctionFormula.entrySet()) {
        String element = entry.getKey();
        int nAtoms = entry.getValue();

        if (!dataIsotopes.containsKey(element)) {
          logger.warning("Element " + element
              + " not found in isotope data. Skipping in isotopic cluster calculation.");
          continue;
        }

        double[] masses = dataIsotopes.get(element).get("mass");
        double[] abundances = dataIsotopes.get(element).get("abundance");

        // Adjust masses relative to the first isotope
        double[] relativeMasses = new double[masses.length];
        for (int i = 0; i < masses.length; i++) {
          relativeMasses[i] = masses[i] - masses[0];
        }

        // For each atom of this element, convolve the current cluster with its isotopic distribution
        for (int i = 0; i < nAtoms; i++) {
          Map<Double, Double> newCluster = new HashMap<>();

          // Combine each existing peak with each isotope of the current element
          for (Map.Entry<Double, Double> peak : isotopicCluster.entrySet()) {
            for (int j = 0; j < relativeMasses.length; j++) {
              double newMass = peak.getKey() + relativeMasses[j];
              double newAbundance = peak.getValue() * abundances[j];

              // Skip very low probability peaks
              if (thresholdP <= 0 || newAbundance > thresholdP) {
                // Add the new peak to the cluster or merge with existing peak
                newCluster.put(newMass, newCluster.getOrDefault(newMass, 0.0) + newAbundance);
              }
            }
          }

          isotopicCluster = newCluster;
        }
      }

      // Remove very low probability peaks from the final result
      if (thresholdP > 0) {
        isotopicCluster.entrySet().removeIf(entry -> entry.getValue() <= thresholdP);
      }

      logger.info("Isotopic cluster computed with " + isotopicCluster.size() + " peaks.");
      return isotopicCluster;
    }
  }

  /**
   * Data class to store the result of corrections.
   */
  class CorrectedResult {

    private final double[] correctedArea;
    private final double[] isotopologueFraction;
    private final double[] residuum;
    private final double meanEnrichment;

    public CorrectedResult(double[] correctedArea, double[] isotopologueFraction, double[] residuum,
        double meanEnrichment) {
      this.correctedArea = correctedArea;
      this.isotopologueFraction = isotopologueFraction;
      this.residuum = residuum;
      this.meanEnrichment = meanEnrichment;
    }

    public double[] getCorrectedArea() {
      return correctedArea;
    }

    public double[] getIsotopologueFraction() {
      return isotopologueFraction;
    }

    public double[] getResiduum() {
      return residuum;
    }

    public double getMeanEnrichment() {
      return meanEnrichment;
    }

    /**
     * Returns the corrected measurements.
     *
     * @return Corrected measurements as a double array.
     */
    public double[] getCorrectedMeasurements() {
      double[] correctedMeasurements = new double[correctedArea.length];
      for (int i = 0; i < correctedArea.length; i++) {
        correctedMeasurements[i] = correctedArea[i] * isotopologueFraction[i];
      }
      return correctedMeasurements;
    }
  }
}







