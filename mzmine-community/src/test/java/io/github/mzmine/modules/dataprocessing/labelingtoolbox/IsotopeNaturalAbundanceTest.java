/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.labelingtoolbox;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.dataprocessing.isolab_natabundance.LowResMetaboliteCorrector;
import io.github.mzmine.modules.dataprocessing.isolab_natabundance.MetaboliteCorrector;
import io.github.mzmine.modules.dataprocessing.isolab_natabundance.MetaboliteCorrectorFactory;
import org.junit.jupiter.api.Test;

class IsotopeNaturalAbundanceTest {

  /**
   * Test basic initialization of the corrector.
   */
  @Test
  void testBasicInitialization() throws Exception {

    String formula = "C3H7NO2"; // Alanine
    String tracer = "13C";

    java.util.HashMap<String, Object> options = new java.util.HashMap<>();
    options.put("charge", 1);
    options.put("correct_NA_tracer", true);

    // Create the corrector
    MetaboliteCorrector corrector = MetaboliteCorrectorFactory.createCorrector(formula, tracer,
        options);

    // Verify it's not null
    assertTrue(corrector != null, "Corrector should not be null");
  }

  /**
   * Test tracer parsing functionality.
   */
  @Test
  void testTracerParsing() throws Exception {
    // Test different tracer formats
    String formula = "C3H7NO2"; // Alanine
    String[] tracers = {"13C", "15N", "2H"};

    for (String tracer : tracers) {
      java.util.HashMap<String, Object> options = new java.util.HashMap<>();
      options.put("charge", 1);
      options.put("correct_NA_tracer", true);

      MetaboliteCorrector corrector = MetaboliteCorrectorFactory.createCorrector(formula, tracer,
          options);

      // Use reflection to check tracer element and index
      java.lang.reflect.Field elementField = MetaboliteCorrector.class.getDeclaredField(
          "tracerElement");
      elementField.setAccessible(true);
      String tracerElement = (String) elementField.get(corrector);
      assertTrue(
          tracerElement.equals("C") || tracerElement.equals("N") || tracerElement.equals("H"),
          "Tracer element should be C, N, or H: " + tracerElement);

      java.lang.reflect.Field indexField = MetaboliteCorrector.class.getDeclaredField(
          "tracerIsotopeIndex");
      indexField.setAccessible(true);
      int tracerIsotopeIndex = (int) indexField.get(corrector);
      assertTrue(tracerIsotopeIndex >= 0,
          "Tracer isotope index should be non-negative: " + tracerIsotopeIndex);
    }
  }

  /**
   * Test mass distribution vector calculation.
   */
  @Test
  void testMassDistributionVector() throws Exception {

    String formula = "C3H7NO2"; // Alanine
    String tracer = "13C";

    java.util.HashMap<String, Object> options = new java.util.HashMap<>();
    options.put("charge", 1);
    options.put("correct_NA_tracer", true);

    LowResMetaboliteCorrector corrector = (LowResMetaboliteCorrector) MetaboliteCorrectorFactory.createCorrector(
        formula, tracer, options);

    // Use reflection to access the mass distribution vector
    java.lang.reflect.Method method = LowResMetaboliteCorrector.class.getDeclaredMethod(
        "getMassDistributionVector");
    method.setAccessible(true);
    double[] vector = (double[]) method.invoke(corrector);

    assertTrue(vector != null && vector.length > 0,
        "Mass distribution vector should not be null or empty");

    // Check that it sums to approximately 1
    double sum = 0;
    for (double v : vector) {
      sum += v;
    }

    assertTrue(Math.abs(sum - 1.0) < 0.001,
        "Mass distribution vector should sum to approximately 1.0: " + sum);
  }

  /**
   * Test convolution functionality.
   */
  @Test
  void testConvolution() throws Exception {

    String formula = "C3H7NO2"; // Alanine
    String tracer = "13C";

    java.util.HashMap<String, Object> options = new java.util.HashMap<>();
    options.put("charge", 1);

    MetaboliteCorrector corrector = MetaboliteCorrectorFactory.createCorrector(formula, tracer,
        options);

    // Test arrays
    double[] a = {0.5, 0.5};
    double[] b = {0.8, 0.2};

    // Use reflection to access the convolve method
    java.lang.reflect.Method method = MetaboliteCorrector.class.getDeclaredMethod("convolve",
        double[].class, double[].class);
    method.setAccessible(true);
    double[] result = (double[]) method.invoke(corrector, a, b);

    assertTrue(result != null, "Convolution result should not be null");
    assertTrue(result.length == 3, "Convolution result should have length 3: " + result.length);

    // Check that result sums to 1
    double sum = 0;
    for (double v : result) {
      sum += v;
    }

    assertTrue(Math.abs(sum - 1.0) < 0.001,
        "Convolution result should sum to approximately 1.0: " + sum);
  }

  /**
   * Test full correction process for glucose.
   */
  @Test
  void testGlucoseFullCorrection() throws Exception {

    String formula = "C6H12O6"; // Glucose
    String tracer = "13C";

    java.util.HashMap<String, Object> options = new java.util.HashMap<>();
    options.put("charge", 1);
    options.put("correct_NA_tracer", true);

    // Explicit tracer purity for Carbon (2 isotopes in our dataset)
    double[] tracerPurity = {0.01, 0.99}; // 99% 13C purity
    options.put("tracerPurity", tracerPurity);

    // Create the corrector
    MetaboliteCorrector corrector = MetaboliteCorrectorFactory.createCorrector(formula, tracer,
        options);

    // Create reasonable test measurements
    double[] measurements = {10000.0, 650.0, 30.0, 5.0, 1.0, 0.5, 0.1};

    // Perform correction
    LowResMetaboliteCorrector.CorrectedResult result = corrector.correct(measurements);

    // Check results
    assertTrue(result != null, "Correction result should not be null");

    double[] fractions = result.getIsotopologueFraction();
    double[] areas = result.getCorrectedArea();
    double[] residuum = result.getResiduum();
    double meanEnrichment = result.getMeanEnrichment();

    // Check that fractions sum to 1
    double sum = 0;
    for (double v : fractions) {
      sum += v;
    }

    assertTrue(Math.abs(sum - 1.0) < 0.001,
        "Isotopologue fractions should sum to approximately 1.0: " + sum);

    // For glucose with natural abundance, M+0 should be dominant
    assertTrue(fractions[0] > 0.7, "M+0 fraction should be greater than 0.7: " + fractions[0]);
  }
}
