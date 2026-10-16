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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class SpectralDeconvolutionUtilsTest {

  private static final MZTolerance MZ_TOLERANCE = new MZTolerance(0.005, 10);

  @Test
  void consensusSignalsRankDetectionsBeforeIntensity() {
    final RawDataFile a = new RawDataFileImpl("a", null, null);
    final RawDataFile b = new RawDataFileImpl("b", null, null);
    final ModularFeatureList flist = new ModularFeatureList("aligned", null, a, b);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    // m/z 300 is the most intense signal, but only found in one sample
    addFeature(row, flist, a, List.of(pseudoSpectrum(a, new double[]{100, 150, 300},
        new double[]{1E5, 8E4, 9E5})));
    addFeature(row, flist, b, List.of(pseudoSpectrum(b, new double[]{100, 150},
        new double[]{1E5, 8E4})));

    final List<PseudoSpectrumConsensusSignal> signals = SpectralDeconvolutionUtils.getConsensusSignals(
        row, MZ_TOLERANCE);

    assertEquals(List.of(100d, 150d, 300d),
        signals.stream().map(PseudoSpectrumConsensusSignal::mz).toList());
    assertEquals(2, signals.getFirst().numDetections());
    assertEquals(2E5, signals.getFirst().sumIntensity(), 1E-6);
  }

  @Test
  void onlyPseudoSpectraAreMerged() {
    final RawDataFile a = new RawDataFileImpl("a", null, null);
    final ModularFeatureList flist = new ModularFeatureList("list", null, a);
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    final Scan ms2 = new SimpleScan(a, 1, 2, 1f, null, new double[]{50d}, new double[]{1E6},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 500d));
    addFeature(row, flist, a,
        List.of(ms2, pseudoSpectrum(a, new double[]{100}, new double[]{1E5})));

    final double[][] merged = SpectralDeconvolutionUtils.mergePseudoSpectra(row, MZ_TOLERANCE);

    assertEquals(1, merged[0].length, "the MS2 scan is not a pseudo spectrum");
    assertEquals(100d, merged[0][0], 1E-6);
  }

  private static void addFeature(@NotNull final ModularFeatureListRow row,
      @NotNull final ModularFeatureList flist, @NotNull final RawDataFile file,
      @NotNull final List<Scan> fragmentScans) {
    final ModularFeature feature = new ModularFeature(flist, file, FeatureStatus.DETECTED);
    feature.setAllMS2FragmentScans(fragmentScans);
    row.addFeature(file, feature, false);
  }

  private static @NotNull SimplePseudoSpectrum pseudoSpectrum(@NotNull final RawDataFile file,
      final double @NotNull [] mzs, final double @NotNull [] intensities) {
    return new SimplePseudoSpectrum(file, 1, 1f, null, mzs, intensities, PolarityType.POSITIVE,
        "Pseudo spectrum", PseudoSpectrumType.GC_EI);
  }
}
