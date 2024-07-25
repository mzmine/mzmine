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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MS2DeepscoreSpectrumTensorizerTest {

  private static Scan testSpectrum;
  private static MS2DeepscoreSettings settingsMS2Deepscore;

  @BeforeAll
  static void setUp() {
    RawDataFile dummyFile = new RawDataFileImpl("testfile", null, null,
        javafx.scene.paint.Color.BLACK);
    testSpectrum = new SimpleScan(dummyFile, -1, 2, 0.1F, new DDAMsMsInfoImpl(200.0, 1, 2),
        new double[]{5, 12, 12.1, 14., 14.3}, new double[]{100, 200, 400, 900, 100},
        MassSpectrumType.ANY, PolarityType.POSITIVE, "Pseudo", null);

    settingsMS2Deepscore = new MS2DeepscoreSettings(50, "positive", 12, 15, 0.5, null, 0.5F);
  }

  @Test
  void testTensorizeFragments() {
    MS2DeepscoreSpectrumTensorizer spectrumTensorizer = new MS2DeepscoreSpectrumTensorizer(
        settingsMS2Deepscore);
    float[] results = spectrumTensorizer.tensorizeFragments(testSpectrum);
    Assertions.assertArrayEquals(new float[]{20.0F, 0.0F, 0.0F, 0.0F, 30.0F, 0.0F}, results,
        0.0001F);
  }

  @Test
  void testTensorizeSMetadata() {
    MS2DeepscoreSpectrumTensorizer spectrumTensorizer = new MS2DeepscoreSpectrumTensorizer(
        settingsMS2Deepscore);
    float[] results = spectrumTensorizer.tensorizeMetadata(testSpectrum);
    Assertions.assertArrayEquals(new float[]{0.2F, 1.0F}, results, 0.0001F);
  }
}