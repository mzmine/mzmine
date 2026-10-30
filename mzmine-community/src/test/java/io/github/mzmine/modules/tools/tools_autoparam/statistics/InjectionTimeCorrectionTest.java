/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InjectionTimeCorrectionTest {

  private static @NotNull Scan scan(float rt, @Nullable Float injectionTime) {
    final Scan scan = Mockito.mock(Scan.class);
    Mockito.when(scan.getRetentionTime()).thenReturn(rt);
    Mockito.when(scan.getInjectionTime()).thenReturn(injectionTime);
    Mockito.when(scan.hasInjectionTime()).thenCallRealMethod();
    return scan;
  }

  private static @NotNull FeatureStatistics envelopeWithLowestIsotope(float height,
      @NotNull Scan apex) {
    final ModularFeature main = Mockito.mock(ModularFeature.class);
    final ModularFeature lowest = Mockito.mock(ModularFeature.class);
    Mockito.when(lowest.getHeight()).thenReturn(height);
    Mockito.when(lowest.getRepresentativeScan()).thenReturn(apex);
    return new FeatureStatistics(List.of(
        new FeatureWithIsotopeTraces(200d, new MZTolerance(0.002, 5), main, List.of(lowest),
            new double[]{0.95})));
  }

  @Test
  void referenceIsMedianInjectionTimeWithinEffectiveRange() {
    final RawDataFile file = Mockito.mock(RawDataFile.class);
    // the 100 ms scans are in the wash phase and must not shift the reference
    final List<Scan> scans = List.of(scan(0.5f, 100f), scan(2f, 2f), scan(3f, 10f), scan(4f, 20f),
        scan(5f, null), scan(9f, 100f));
    final Double reference = RawDataParameterEstimation.estimateReferenceInjectionTime(file, scans,
        new SimpleFloatRange(1f, 8f));
    Assertions.assertEquals(10d, reference, 1E-9);
  }

  @Test
  void noReferenceWithoutInjectionTimesOrForIonMobility() {
    final RawDataFile tof = Mockito.mock(RawDataFile.class);
    Assertions.assertNull(RawDataParameterEstimation.estimateReferenceInjectionTime(tof,
        List.of(scan(1f, null), scan(2f, null)), null));

    final IMSRawDataFile tims = Mockito.mock(IMSRawDataFile.class);
    Assertions.assertNull(RawDataParameterEstimation.estimateReferenceInjectionTime(tims,
        List.of(scan(1f, 50f), scan(2f, 100f)), null));
  }

  @Test
  void heightsAreConvertedToTheReferenceInjectionTime() {
    final RawDataFile file = Mockito.mock(RawDataFile.class);
    // apex at a quarter of the reference injection time, so the noise floor there is 4x higher
    final List<FeatureStatistics> features = List.of(
        envelopeWithLowestIsotope(4E5f, scan(3f, 2.5f)),
        envelopeWithLowestIsotope(1E5f, scan(4f, 10f)),
        envelopeWithLowestIsotope(3E5f, scan(5f, null)));
    final DataFileStatistics stats = new DataFileStatistics(file, features, null, 10d);
    Assertions.assertArrayEquals(new double[]{1E5, 1E5},
        stats.getInjectionTimeCorrectedLowestIsotopeHeights(), 1E-3);

    final DataFileStatistics uncorrected = new DataFileStatistics(file, features, null, null);
    Assertions.assertArrayEquals(new double[]{4E5, 1E5, 3E5},
        uncorrected.getInjectionTimeCorrectedLowestIsotopeHeights(), 1E-3);
  }
}
