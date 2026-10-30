package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import io.github.mzmine.modules.dataprocessing.isolab_natabundance.LowResMetaboliteCorrector.CorrectedResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * Base abstract class for metabolite correctors.
 */
public abstract class MetaboliteCorrector {

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
