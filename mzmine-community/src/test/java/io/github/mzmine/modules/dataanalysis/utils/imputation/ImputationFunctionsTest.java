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

package io.github.mzmine.modules.dataanalysis.utils.imputation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mzmine.datamodel.statistics.DataTableTest;
import io.github.mzmine.datamodel.statistics.DataTableUtils;
import io.github.mzmine.datamodel.statistics.SimpleArrayDataTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImputationFunctionsTest {

  private SimpleArrayDataTable table;

  @BeforeEach
  void init() {
    // 5 samples
    // 6 features
    double[][] data = new double[][]{ //
        // 0 will also be imputed
        {0.0, 2.0, 3.0, 4.0, 5.0}, //
        {2.0, Double.NaN, 6.0, 8.0, 10.0}, //
        {3.0, 6.0, Double.NaN, 12.0, 15.0}, //
        {4.0, 8.0, 12.0, Double.NaN, 20.0}, //
        {5.0, 10.0, 15.0, 20.0, Double.NaN}, //
        {6.0, 12.0, 18.0, 24.0, 30.0} //
    };
    table = new SimpleArrayDataTable(data);
  }

  @Test
  void test0Value() {
    final ImputationFunctions function = ImputationFunctions.Zero;
    final SimpleArrayDataTable result = function.getImputer().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    for (int sampleIndex = 0; sampleIndex < table.getNumberOfSamples(); sampleIndex++) {
      final int featureIndex = sampleIndex; // just check the same index
      final double value = result.getValue(featureIndex, sampleIndex);
      assertEquals(0d, value, 1E-6, "Error in feature=%d".formatted(featureIndex));
    }

    final SimpleArrayDataTable resultInplace = function.getImputer().processInPlace(table);
    assertEquals(table, resultInplace);
    DataTableTest.assertEqualContent(result, resultInplace);
  }

  @Test
  void testLimitOfDetection() {
    final ImputationFunctions function = ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION;
    final SimpleArrayDataTable result = function.getImputer().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    final double globalMinimum =
        DataTableUtils.getMinimum(table, true).orElse(1d) / GlobalLimitOfDetectionImputer.DEVISOR;

    for (int sampleIndex = 0; sampleIndex < table.getNumberOfSamples(); sampleIndex++) {
      final int featureIndex = sampleIndex; // just check the same index
      final double value = result.getValue(featureIndex, sampleIndex);
      assertEquals(globalMinimum, value, 1E-6, "Error in feature=%d".formatted(featureIndex));
    }

    final SimpleArrayDataTable resultInplace = function.getImputer().processInPlace(table);
    assertEquals(table, resultInplace);
    DataTableTest.assertEqualContent(result, resultInplace);
  }

  @Test
  void testOneFifthOfMinimum() {
    final ImputationFunctions function = ImputationFunctions.OneFifthOfMinimum;
    final SimpleArrayDataTable result = function.getImputer().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    for (int sampleIndex = 0; sampleIndex < table.getNumberOfSamples(); sampleIndex++) {
      final int featureIndex = sampleIndex; // just check the same index
      // calculate minimum from original data
      final double minValue =
          DataTableUtils.getMinimum(table.getFeatureData(featureIndex, false), true).orElse(1d)
              / OneFifthOfMinimumImputer.DEVISOR;

      final double value = result.getValue(featureIndex, sampleIndex);
      assertEquals(minValue, value, 1E-6, "Error in feature=%d".formatted(featureIndex));
    }

    final SimpleArrayDataTable resultInplace = function.getImputer().processInPlace(table);
    assertEquals(table, resultInplace);
    DataTableTest.assertEqualContent(result, resultInplace);
  }
}