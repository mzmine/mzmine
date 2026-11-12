package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import java.util.Arrays;
import java.util.Map;
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
 * Corrector for low-resolution data.
 */
public class LowResMetaboliteCorrector extends MetaboliteCorrector {

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

  /**
   * Computes the mass distribution vector for non-tracer elements based on the elemental
   * compositions of both metabolite's and derivative's moieties.
   *
   * @return An array representing the mass distribution vector where each element is the
   * probability of finding a molecule with a specific mass increase due to natural isotope
   * abundance.
   */
  private double[] getMassDistributionVector() {
    logger.info("Computing mass distribution vector.");

    double[] result = {1.0};  // Start with probability 1.0 for no isotope shift

    // For each element in the correction formula
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
          logger.info(
              "Successfully loaded isotope data for " + element + " in getMassDistributionVector");
        } else {
          logger.warning("Element " + element + " not found in isotope data. Skipping.");
          continue;
        }
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
    MultivariateFunction costFunction = isotopologueFractions -> {
      RealVector predicted = correctionMatrix.operate(new ArrayRealVector(isotopologueFractions));
      RealVector residuals = new ArrayRealVector(measurements).subtract(predicted);
      return residuals.dotProduct(residuals);  // Sum of squared residuals
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
   * Data class to store the result of corrections.
   */
  public class CorrectedResult {

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
