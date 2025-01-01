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

package io.github.mzmine.util.scans.similarity.impl.DreaMS;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.scans.ScanUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class DreaMSSpectrumTensorizerTest {

  private static Scan testSpectrum;
  private static DreaMSSettings settings;

  @BeforeAll
  static void setUp() {
    RawDataFile dummyFile = new RawDataFileImpl("testfile", null, null,
        javafx.scene.paint.Color.BLACK);
    testSpectrum = new SimpleScan(dummyFile, -1, 2, 0.1F,
        new DDAMsMsInfoImpl(519.7, 1, 2),
        new double[]{2.2, 50.1, 120.11, 120.12, 521.1, 1111.1}, new double[]{100.3, 200.2, 400.1, 1100.5, 500.123, 333.33},
        MassSpectrumType.ANY, PolarityType.POSITIVE, "Pseudo", null);

    settings = new DreaMSSettings(1024, 1.1, 100);
  }

  @Test
  void testTensorizeFragments() {
    DreaMSSpectrumTensorizer spectrumTensorizer = new DreaMSSpectrumTensorizer(settings);
    float[][] results = spectrumTensorizer.tensorizeFragments(testSpectrum, ScanUtils.getPrecursorMz(testSpectrum));
    int nPeaks = settings.nHighestPeaks() + 1;  // + 1 for the artificial precursor peak

    // Assert that the shape is (settings.nHighestPeaks() + 1, 2)
    Assertions.assertEquals(nPeaks, results.length,
            "The outer dimension should be settings.nHighestPeaks() + 1.");
    for (float[] row : results) {
      Assertions.assertEquals(2, row.length,
              "Each inner dimension should have size 2 (m/z, intensity).");
    }

    // Assert that the first elements match the expected values
    float[][] expectedInitialValues = {
            {519.7000f, 1.1000f},
            {2.2000f, 0.0911f},
            {50.1000f, 0.1819f},
            {120.1100f, 0.3636f},
            {120.1200f, 1.0000f},
            {521.1000f, 0.4545f},
            {1111.1000f, 0.3029f}
    };

    for (int i = 0; i < expectedInitialValues.length; i++) {
      Assertions.assertArrayEquals(
              expectedInitialValues[i],
              results[i],
              0.0001f, // Tolerance for floating-point comparisons
              "Mismatch in the expected values at row " + i
      );
    }

    // Assert that the rest of the array contains only zeros (padding)
    for (int i = expectedInitialValues.length; i < results.length; i++) {
      Assertions.assertArrayEquals(
              new float[]{0.0f, 0.0f},
              results[i],
              0.0001f,
              "Expected zeros at row " + i
      );
    }

    // Optional: Print results in a human-readable way (for debugging purposes)
    // for (float[] row : results) {
    //   System.out.println(java.util.Arrays.toString(row));
    // }
  }
}