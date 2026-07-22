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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectedComposition;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ElementAutoDetector;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ElementAutoDetector}. Test spectra are generated with CDK via
 * {@link IsotopePatternCalculator} at high (resolved) resolution and fed to the detector as
 * {@link DataPoint}s, mirroring how the benchmark metric calls the detector. Besides the clear
 * single-element cases, the suite exercises the robustness requirements: mono-independence (leading
 * peaks dropped), multiple charge states, m/z shift within tolerance, and a custom candidate list.
 */
class ElementAutoDetectorTest {

  private static final MZTolerance TOL = new MZTolerance(0.005, 10);

  /**
   * Resolved isotope pattern for {@code formula} at {@code charge} as a signal list (sorted by
   * m/z).
   */
  @NotNull
  private static List<DataPoint> signalsOf(@NotNull final String formula, final int charge) {
    final IsotopePattern p = IsotopePatternCalculator.calculateIsotopePattern(formula, 0.001,
        0.00005, charge, PolarityType.POSITIVE, false);
    final List<DataPoint> signals = new ArrayList<>(p.getNumberOfDataPoints());
    for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
      signals.add(new SimpleDataPoint(p.getMzValue(i), p.getIntensityValue(i)));
    }
    signals.sort(Comparator.comparingDouble(DataPoint::getMZ));
    return signals;
  }

  /**
   * Detect from a signal list, seeding the neutral mass as {@code lowestMz * charge} exactly as the
   * benchmark metric does.
   */
  @NotNull
  private static Set<String> elementsOf(@NotNull final List<DataPoint> signals, final int charge) {
    double lowestMz = Double.POSITIVE_INFINITY;
    for (final DataPoint dp : signals) {
      lowestMz = Math.min(lowestMz, dp.getMZ());
    }
    return ElementAutoDetector.detect(signals, charge, lowestMz * charge, TOL).elements();
  }

  @NotNull
  private static Set<String> detect(@NotNull final String formula, final int charge) {
    return elementsOf(signalsOf(formula, charge), charge);
  }

  /**
   * Remove the {@code n} lowest-m/z signals (simulate a monoisotopic + first isotopes below the
   * detection threshold).
   */
  @NotNull
  private static List<DataPoint> dropLowest(@NotNull final List<DataPoint> signals, final int n) {
    return new ArrayList<>(signals.subList(Math.min(n, signals.size()), signals.size()));
  }

  /**
   * Shift every m/z by a seeded amount uniformly in [-maxShift, +maxShift] (jitter within
   * tolerance).
   */
  @NotNull
  private static List<DataPoint> jitter(@NotNull final List<DataPoint> signals,
      final double maxShift, final long seed) {
    final Random rnd = new Random(seed);
    final List<DataPoint> out = new ArrayList<>(signals.size());
    for (final DataPoint dp : signals) {
      out.add(new SimpleDataPoint(dp.getMZ() + (rnd.nextDouble() * 2d - 1d) * maxShift,
          dp.getIntensity()));
    }
    return out;
  }

  // ---- clear single-element cases -----------------------------------------

  @Test
  void detectsTetrachloroAsClNotBr() {
    final Set<String> e = detect("C20H30Cl4", 1);
    Assertions.assertTrue(e.contains("Cl"), () -> "expected Cl in " + e);
    Assertions.assertFalse(e.contains("Br"), () -> "Cl4 must not be read as Br: " + e);
  }

  @Test
  void detectsDibromoAsBr() {
    final Set<String> e = detect("C20H30Br2", 1);
    Assertions.assertTrue(e.contains("Br"), () -> "expected Br in " + e);
  }

  @Test
  void detectsWeakM2HeavyForSulfur() {
    // S is a weak M+2 element; at a 5 mDa tolerance its 34S defect (1.9958) is degenerate with Si
    // (1.99684, ~1 mDa) and buried under 13C/15N combinations, so we require a weak M+2 heavy element
    // to be found, not a strict S-vs-Si separation (a documented resolution limit).
    final Set<String> e = detect("C30H50N4O6S2", 1);
    Assertions.assertTrue(e.contains("S") || e.contains("Si"),
        () -> "expected a weak M+2 heavy element (S/Si) for a sulfur compound, got " + e);
  }

  @Test
  void detectsSiloxaneAsSi() {
    // Si carries a strong, distinctive 29Si M+1 fingerprint (absent for S/Cl), so it is identifiable
    final Set<String> e = detect("C8H24O4Si4", 1);
    Assertions.assertTrue(e.contains("Si"), () -> "expected Si in " + e);
  }

  @Test
  void detectsNoHeavyInPlainChno() {
    final Set<String> e = detect("C30H50N4O6", 1);
    Assertions.assertTrue(e.isEmpty(), () -> "expected no heavy elements, got " + e);
  }

  @Test
  void detectsHeavyElementInChlorineSulfurMix() {
    // Cl and S sit ~1.3 mDa apart at M+2; at a 5 mDa tolerance a mixture is not cleanly separable, so
    // we only require that a heavy element is detected (mixture separation needs higher resolution).
    final Set<String> e = detect("C20H28Cl2S2", 1);
    Assertions.assertFalse(e.isEmpty(), () -> "expected a heavy element in the Cl/S mix, got " + e);
  }

  // ---- R2: different charge states ----------------------------------------

  @Test
  void detectsChlorineAcrossCharges() {
    for (final int z : new int[]{1, 2, 3}) {
      final Set<String> e = detect("C40H60Cl4", z);
      Assertions.assertTrue(e.contains("Cl"), () -> "expected Cl at charge " + z + ": " + e);
      Assertions.assertFalse(e.contains("Br"),
          () -> "Cl4 at charge " + z + " must not be read as Br: " + e);
    }
  }

  @Test
  void detectAcrossChargesFindsChlorineAtTrueCharge() {
    final List<DataPoint> signals = signalsOf("C40H60Cl4", 2);
    final Map<Integer, DetectedComposition> byCharge = ElementAutoDetector.detectAcrossCharges(
        signals, 4, TOL, ElementAutoDetector.DEFAULT_CANDIDATES);
    final DetectedComposition atTwo = byCharge.get(2);
    Assertions.assertNotNull(atTwo, () -> "expected a detection at the true charge 2: " + byCharge);
    Assertions.assertTrue(atTwo.elements().contains("Cl"),
        () -> "expected Cl at charge 2: " + atTwo.elements());
  }

  // ---- R1: mono-independent (leading signals missing) ----------------------

  @Test
  void detectsChlorineWithLeadingPeaksMissing() {
    // drop the lowest 3 signals (monoisotopic + first isotopes below threshold); the +2 comb remains
    final List<DataPoint> trimmed = dropLowest(signalsOf("C30H40Cl6", 1), 3);
    final Set<String> e = elementsOf(trimmed, 1);
    Assertions.assertTrue(e.contains("Cl"),
        () -> "expected Cl even with the monoisotopic region removed: " + e);
  }

  @Test
  void detectsBromineWithLeadingPeaksMissing() {
    final List<DataPoint> trimmed = dropLowest(signalsOf("C30H40Br4", 1), 3);
    final Set<String> e = elementsOf(trimmed, 1);
    Assertions.assertTrue(e.contains("Br"),
        () -> "expected Br even with the monoisotopic region removed: " + e);
  }

  // ---- R3: m/z shift within tolerance -------------------------------------

  @Test
  void detectsHalogenUnderMzJitter() {
    // Under +/-tolerance jitter on every peak, the Cl(1.997)/Br(1.998) defects (0.9 mDa apart) are
    // physically indistinguishable - the shift exceeds their separation. What must remain robust is
    // that a strong HALOGEN-class M+2 element is still detected (not lost, not read as weak S/Si).
    for (final String formula : new String[]{"C40H60Cl4", "C30H40Br4"}) {
      final List<DataPoint> jittered = jitter(signalsOf(formula, 1), 0.005, 42L);
      final Set<String> e = elementsOf(jittered, 1);
      Assertions.assertTrue(e.contains("Cl") || e.contains("Br"),
          () -> "expected a halogen (Cl/Br) under +/-tolerance jitter for " + formula + ": " + e);
    }
  }

  // ---- R4: candidate ("potential") element handling ------------------------

  @Test
  void honoursCustomCandidateListAndTolerastesUnknownElements() {
    final List<DataPoint> signals = signalsOf("C20H30Br2", 1);
    // an extended candidate list including a non-default element must not crash and still find Br
    final DetectedComposition extended = ElementAutoDetector.detect(signals, 1,
        signals.get(0).getMZ(), TOL, List.of("Cl", "Br", "S", "Si", "Se"));
    Assertions.assertTrue(extended.elements().contains("Br"),
        () -> "expected Br with an extended candidate list: " + extended.elements());

    // restricting the candidates to just {Cl} must not report Br for a Cl compound
    final List<DataPoint> clSignals = signalsOf("C20H30Cl4", 1);
    final DetectedComposition onlyCl = ElementAutoDetector.detect(clSignals, 1,
        clSignals.get(0).getMZ(), TOL, List.of("Cl"));
    Assertions.assertEquals(Set.of("Cl"), onlyCl.elements(),
        () -> "expected only Cl with a Cl-only candidate list: " + onlyCl.elements());
  }
}
