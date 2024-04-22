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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SpectrumTensorizerTest {

  private static Scan testSpectrum;
  private static SettingsMS2Deepscore settingsMS2Deepscore;

  @BeforeAll
  static void setUp() {
    RawDataFile dummyFile = new RawDataFileImpl("testfile", null, null,
        javafx.scene.paint.Color.BLACK);
    testSpectrum = new SimpleScan(dummyFile, -1, 2, 0.1F, null,
        new double[]{5, 12, 12.1, 14., 14.3}, new double[]{100, 200, 400, 200, 100},
        MassSpectrumType.ANY, PolarityType.ANY, "Pseudo", null);

    settingsMS2Deepscore = new SettingsMS2Deepscore(null, "positive", 10, 15, 1.0, null);
  }

  @Test
  void testTensorizeSpectrum() {
    SpectrumTensorizer spectrumTensorizer = new SpectrumTensorizer(settingsMS2Deepscore);
    double[] results = spectrumTensorizer.tensorizeFragments(testSpectrum);
    Assertions.assertArrayEquals(new double[]{0.0, 0.0, 400.0, 0.0, 200.0}, results, 0.0001);
  }

}