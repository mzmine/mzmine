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

package io.github.mzmine.modules.dataanalysis.utils.scaling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.statistics.DataTableTest;
import io.github.mzmine.datamodel.statistics.SimpleArrayDataTable;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScalingFunctionsTest {

  private final StandardDeviation dev = new StandardDeviation(true);
  private SimpleArrayDataTable table;

  @BeforeEach
  void init() {
    // 5 samples
    // 6 features
    double[][] data = new double[][]{ //
        {1.0, 2.0, 3.0, 4.0, 5.0}, //
        {2.0, 4.0, 6.0, 8.0, 10.0}, //
        {3.0, 6.0, 9.0, 12.0, 15.0}, //
        {4.0, 8.0, 12.0, 16.0, 20.0}, //
        {5.0, 10.0, 15.0, 20.0, 25.0}, //
        {6.0, 12.0, 18.0, 24.0, 30.0} //
    };
    table = new SimpleArrayDataTable(data);
  }

  @Test
  void testCenterScaling() {
    final ScalingFunctions function = ScalingFunctions.MeanCentering;
    final SimpleArrayDataTable result = function.getScalingFunction().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    for (double[] doubles : result) {
      assertEquals(0d, MathUtils.calcAvg(doubles), 1E-6);
    }

    final SimpleArrayDataTable resultInplace = function.getScalingFunction().processInPlace(table);
    assertEquals(table, resultInplace);
  }

  @Test
  void testPareto() {
    final ScalingFunctions function = ScalingFunctions.ParetoScaling;
    SimpleArrayDataTable result = function.getScalingFunction().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    for (int featureIndex = 0; featureIndex < table.getNumberOfFeatures(); featureIndex++) {
      final double sqrtSD = Math.sqrt(dev.evaluate(table.getFeatureData(featureIndex, false)));
      for (int sampleIndex = 0; sampleIndex < table.getNumberOfSamples(); sampleIndex++) {
        final double scaled = table.getValue(featureIndex, sampleIndex) / sqrtSD;
        assertEquals(scaled, result.getValue(featureIndex, sampleIndex), 1E-6);
      }
    }

    final SimpleArrayDataTable resultInplace = function.getScalingFunction().processInPlace(table);
    assertEquals(table, resultInplace);
    DataTableTest.assertEqualContent(result, resultInplace);
  }


  @Test
  void testAuto() {
    final ScalingFunctions function = ScalingFunctions.AutoScaling;
    SimpleArrayDataTable result = function.getScalingFunction().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    for (int featureIndex = 0; featureIndex < table.getNumberOfFeatures(); featureIndex++) {
      final double sd = dev.evaluate(table.getFeatureData(featureIndex, false));
      for (int sampleIndex = 0; sampleIndex < table.getNumberOfSamples(); sampleIndex++) {
        final double scaled = table.getValue(featureIndex, sampleIndex) / sd;
        assertEquals(scaled, result.getValue(featureIndex, sampleIndex), 1E-6);
      }
    }

    final SimpleArrayDataTable resultInplace = function.getScalingFunction().processInPlace(table);
    assertEquals(table, resultInplace);
    DataTableTest.assertEqualContent(result, resultInplace);
  }


  @Test
  void testNoScaling() {
    // always the same table
    final ScalingFunctions function = ScalingFunctions.None;
    SimpleArrayDataTable result = function.getScalingFunction().process(table, false);
    assertNotNull(result);

    final SimpleArrayDataTable resultInplace = function.getScalingFunction().processInPlace(table);
    assertEquals(table, resultInplace);
  }


  @Test
  void testRange() {
    final ScalingFunctions function = ScalingFunctions.RangeScaling;
    SimpleArrayDataTable result = function.getScalingFunction().process(table, false);
    assertNotNull(result);
    assertNotEquals(table, result);

    // check results directly
    for (int featureIndex = 0; featureIndex < result.getNumberOfFeatures(); featureIndex++) {
      final SimpleDoubleRange range = ArrayUtils.rangeOf(result.getFeatureData(featureIndex, false))
          .get();

      assertEquals(0d, range.lowerBound(), 1E-6);
      assertEquals(1d, range.upperBound(), 1E-6);
      assertEquals(1d, range.length(), 1E-6);
    }

    final SimpleArrayDataTable resultInplace = function.getScalingFunction().processInPlace(table);
    assertEquals(table, resultInplace);
    DataTableTest.assertEqualContent(result, resultInplace);
  }


}