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

package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import io.github.mzmine.modules.dataprocessing.isolab_natabundance.LowResMetaboliteCorrector.CorrectedResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

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
    Double resolution = (Double) options.getOrDefault("resolution", 0.0);
    Double mzOfResolution = (Double) options.getOrDefault("mzOfResolution", null);
    Integer charge = (Integer) options.getOrDefault("charge", null);

    if (resolution > 0.0 && mzOfResolution != null && charge != null) {
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

  /**
   * Creates a metabolite corrector based on the provided parameters.
   *
   * @param formula The elemental formula of the metabolite moiety (e.g. "C3H7O6P")
   * @param tracer  The isotopic tracer (e.g. "13C")
   * @param options Additional parameters for the corrector: - label: metabolite abbreviation (e.g.
   *                "G6P") - data_isotopes: isotopic data with mass and abundance (defaults to
   *                built-in data) - derivative_formula: elemental formula of the derivative moiety
   *                - tracer_purity: proportion of each isotope of the tracer element (defaults to
   *                perfect purity) - correct_NA_tracer: flag to correct tracer natural abundance
   *                (defaults to false) - resolution: resolution of the mass spectrometer (e.g. 1e4)
   *                - mzOfResolution: m/z at which the resolution was measured (e.g. 400) - charge:
   *                charge state of the metabolite (e.g. -2) - resolutionFormulaCode: code for
   *                resolution formula ("orbitrap", "ft-icr", "constant")
   * @return A configured instance of MetaboliteCorrector.
   * @throws IllegalArgumentException if the input parameters are invalid
   */
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
    validateIsotopicData(this.dataIsotopes);
  }

  /**
   * Parse the formula string into a map of elements and their counts. Handles both standard
   * molecular formulas (e.g., "C6H12O6") and formulas with isotope notations (e.g., both "13C" and
   * "C13").
   */
  private Map<String, Integer> parseFormula(String formula) {
    logger.info("Parsing formula: " + formula);

    Map<String, Integer> formulaMap = new HashMap<>();
    if (formula == null || formula.isEmpty()) {
      throw new IllegalArgumentException("Formula cannot be null or empty.");
    }

    // Standard molecular formula pattern: C6H12O6
    String elementPattern = "([A-Z][a-z]*)([0-9]*)";
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(elementPattern);
    java.util.regex.Matcher matcher = pattern.matcher(formula);

    while (matcher.find()) {
      String element = matcher.group(1);
      String countStr = matcher.group(2);
      int count = countStr.isEmpty() ? 1 : Integer.parseInt(countStr);
      formulaMap.put(element, formulaMap.getOrDefault(element, 0) + count);
    }

    logger.info("Parsed formula: " + formulaMap);
    return formulaMap;
  }

  /**
   * Parse the tracer string to extract the element and isotope index. Handles both formats: "13C"
   * and "C13". Nevertheless, right now only the "13C" format is supported by the rest of the code,
   * so it's a bit useless, but it was fun to figure out. If the tracer element is not in isotope
   * data, attempts to load it specifically.
   */
  private void parseTracer(String tracerStr) {
    logger.info("Parsing tracer: " + tracerStr);

    try {
      int tracerMass;

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
   * Load isotope data specifically for the given element. This method attempts to load isotope data
   * from CDK focusing only on the requested element.
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

    } catch (IOException e) {
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

        // Sort isotopes by mass number (full list sort)
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

    } catch (IOException e) {
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
   * Compute the molecular weight of the compound. For elements not found in existing isotope data,
   * tries to load them on demand.
   */
  protected double getMolecularWeight() {
    logger.info("Computing molecular weight.");

    double mw = 0.0;
    for (Map.Entry<String, Integer> entry : formula.entrySet()) {
      String element = entry.getKey();
      int count = entry.getValue();

      // Check if element is in our database
      if (!dataIsotopes.containsKey(element)) {
        logger.info(
            "Element " + element + " not found in current isotope data. Attempting to load it.");

        // Try to load data specifically for this element
        Map<String, Map<String, double[]>> specificData = loadSpecificElementIsotopeData(element);

        // If we got data for this element, add it to our existing data
        if (specificData.containsKey(element)) {
          dataIsotopes.put(element, specificData.get(element));
          logger.info("Successfully loaded isotope data for " + element);
        } else {
          logger.warning("Element " + element
              + " not found in isotope data. Skipping in molecular weight calculation.");
          continue;
        }
      }

      // Now add to molecular weight using the first (most abundant) isotope mass
      mw += count * dataIsotopes.get(element).get("mass")[0];
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

  /**
   * Corrects the measurement vector using optimization techniques.
   *
   * @param measurements An array of measured areas for each isotopologue peak
   * @return A CorrectedResult object containing: - correctedArea: Corrected area for each peak -
   * isotopologueFraction: The abundance of each tracer isotopologue (normalized to 1) - residuum:
   * Normalized residuals between measured and expected values - meanEnrichment: Average isotope
   * enrichment
   * @throws IllegalArgumentException if the measurements array length does not match the expected
   *                                  number of isotopologues or contains negative values
   */
  public abstract CorrectedResult correct(double[] measurements);

  /**
   * Add this method to the MetaboliteCorrector base class
   */
  protected void validateIsotopicData(Map<String, Map<String, double[]>> dataIsotopes) {
    logger.info("Validating isotopic data...");

    // Different tolerance values for different elements
    // Sulfur needs a larger tolerance due to the gap between isotopes
    Map<String, Double> elementTolerances = new HashMap<>();
    elementTolerances.put("S", 2.5); // Special case for sulfur - larger gap allowed
    double defaultTolerance = 1.2;   // Default tolerance for other elements

    for (Map.Entry<String, Map<String, double[]>> elementEntry : dataIsotopes.entrySet()) {
      String element = elementEntry.getKey();
      Map<String, double[]> data = elementEntry.getValue();

      // Get element-specific tolerance or use default
      double toleranceIsomass = elementTolerances.getOrDefault(element, defaultTolerance);

      // Check required fields
      if (!data.containsKey("mass") || !data.containsKey("abundance")) {
        throw new IllegalArgumentException(
            "Invalid data_isotopes for element " + element + ". Missing mass or abundance.");
      }

      double[] masses = data.get("mass");
      double[] abundances = data.get("abundance");

      // Check lengths match
      if (masses.length != abundances.length) {
        throw new IllegalArgumentException(
            "There should ALWAYS be the same number of isotopes mass and abundance in data_isotopes. "
                + "This is not the case for " + element);
      }

      // Check masses are in increasing order
      for (int i = 1; i < masses.length; i++) {
        if (masses[i] <= masses[i - 1]) {
          throw new IllegalArgumentException(
              "Isotopes masses in data_isotopes should ALWAYS be in increasing order. "
                  + "This is not the case for " + element);
        }
      }

      // Check for missing isotopes, with element-specific tolerance
      for (int i = 1; i < masses.length; i++) {
        if (masses[i] - masses[i - 1] > toleranceIsomass) {
          // Special case for sulfur: skip the check between 34S and 36S (approximately masses 34 and 36)
          if (element.equals("S") && Math.abs(masses[i - 1] - 34.0) < 0.5
              && Math.abs(masses[i] - 36.0) < 0.5) {
            logger.info(
                "Allowing gap between " + masses[i - 1] + " and " + masses[i] + " for element "
                    + element + " (intentional exception)");
            continue;
          }

          throw new IllegalArgumentException(
              "It seems that data_isotopes is incomplete and that we are missing data for an isotope "
                  + "between masses " + masses[i - 1] + " Da and " + masses[i] + " Da for "
                  + element);
        }
      }

      // Check for negative masses
      for (double mass : masses) {
        if (mass <= 0) {
          throw new IllegalArgumentException(
              "One or several masses are negative in data_isotopes for element " + element);
        }
      }

      // Check abundance sum is 1.0
      double sum = 0.0;
      for (double abundance : abundances) {
        sum += abundance;
      }

      if (Math.abs(sum - 1.0) > 1e-6) {
        throw new IllegalArgumentException(
            "The sum of the natural abundance of each isotope should ALWAYS equal 1. "
                + "This is not the case for " + element + ". Sum = " + sum);
      }

      // Check abundances are valid probabilities
      for (double abundance : abundances) {
        if (abundance < 0.0 || abundance > 1.0) {
          throw new IllegalArgumentException(
              "One or several natural abundance are invalid probabilities for element " + element);
        }
      }
    }

    logger.info("Isotopic data validation passed.");
  }
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

/**
 * Corrector for high-resolution data.
 */
class HighResMetaboliteCorrector extends LowResMetaboliteCorrector {

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