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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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

