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

package io.github.mzmine.datamodel.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataTableTest {

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
  void testFunctions() {
    assertEquals(6, table.getNumberOfFeatures());
    assertEquals(6, table.getSampleData(1).length);

    assertEquals(5, table.getNumberOfSamples());
    assertEquals(5, table.getFeatureData(0, false).length);
    assertEquals(5, table.getFeatureData(0, true).length);

    assertEquals(8d, table.getValue(1, 3), 1E-6);
    assertEquals(10d, table.getValue(1, 4), 1E-6);

    assertEquals(10d, table.getSampleData(4)[1], 1E-6);
    assertEquals(10d, table.getFeatureData(1, false)[4], 1E-6);
    assertEquals(10d, table.getFeatureData(1, true)[4], 1E-6);


  }

  @Test
  void testCopy() {
    final SimpleArrayDataTable copy = table.copy();
    assertNotNull(copy);
    assertNotEquals(table, copy);
    assertEqualContent(table, copy);
  }


  public static void assertEqualContent(DataTable expected, DataTable actual) {
    assertEquals(expected.getNumberOfSamples(), actual.getNumberOfSamples());
    assertEquals(expected.getNumberOfFeatures(), actual.getNumberOfFeatures());
    for (int featureIndex = 0; featureIndex < expected.getNumberOfFeatures(); featureIndex++) {
      for (int sampleIndex = 0; sampleIndex < expected.getNumberOfSamples(); sampleIndex++) {
        final double exValue = expected.getValue(featureIndex, sampleIndex);
        final double acValue = actual.getValue(featureIndex, sampleIndex);
        assertEquals(exValue, acValue, 1E-6,
            "Wrong value feature=%d sample=%d (%f!=%f)".formatted(featureIndex, sampleIndex,
                exValue, acValue));
      }
    }
  }
}