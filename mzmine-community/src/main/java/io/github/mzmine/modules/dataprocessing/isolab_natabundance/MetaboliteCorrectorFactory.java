/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import java.util.Map;
import java.util.logging.Logger;

/**
 * Factory class to create MetaboliteCorrector instances based on parameters.
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

  protected String formula;
  protected String tracer;
  protected Map<String, Object> options;

  public MetaboliteCorrector(String formula, String tracer, Map<String, Object> options) {
    this.formula = formula;
    this.tracer = tracer;
    this.options = options;
  }

  public abstract CorrectedResult correct(double[] measurements);
}

/**
 * Corrector for low-resolution data.
 */
class LowResMetaboliteCorrector extends MetaboliteCorrector {

  public LowResMetaboliteCorrector(String formula, String tracer, Map<String, Object> options) {
    super(formula, tracer, options);
  }

  @Override
  public CorrectedResult correct(double[] measurements) {
    // Implement low-resolution correction logic here
    return new CorrectedResult(new double[]{measurements[0]}, new double[]{1.0}, new double[]{0.0});
  }
}

/**
 * Corrector for high-resolution data.
 */
class HighResMetaboliteCorrector extends MetaboliteCorrector {

  private final double resolution;
  private final double mzOfResolution;
  private final int charge;
  private final String resolutionFormulaCode;

  public HighResMetaboliteCorrector(String formula, String tracer, double resolution,
      double mzOfResolution, int charge, String resolutionFormulaCode,
      Map<String, Object> options) {
    super(formula, tracer, options);
    this.resolution = resolution;
    this.mzOfResolution = mzOfResolution;
    this.charge = charge;
    this.resolutionFormulaCode = resolutionFormulaCode;
  }

  @Override
  public CorrectedResult correct(double[] measurements) {
    // Implement high-resolution correction logic here
    return new CorrectedResult(new double[]{measurements[0] * 1.01}, new double[]{1.0},
        new double[]{0.0});
  }
}

/**
 * Data class to store the result of corrections.
 */
class CorrectedResult {

  private final double[] correctedArea;
  private final double[] isotopologueFraction;
  private final double[] residuum;

  public CorrectedResult(double[] correctedArea, double[] isotopologueFraction, double[] residuum) {
    this.correctedArea = correctedArea;
    this.isotopologueFraction = isotopologueFraction;
    this.residuum = residuum;
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
}
