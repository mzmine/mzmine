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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.CrossScanRefiner;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeEnvelope;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.RatioAggregation;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.formula.FormulaEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.formula.FormulaEnvelopeParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Element;

/**
 * Tests for the signal-based isotope finder: carbon-averagine envelope shape, charge-state
 * selection across element mixes / resolutions / charges, envelope-shape-aware termination, and
 * cross-scan refinement.
 */
class IsotopeFinderEngineTest {

  private static final double C13 = 1.0033548;
  private static final MZTolerance TOL = new MZTolerance(0.005, 10);

  private static EnvelopeModel signalModel(final List<Element> elements) {
    return new CarbonAveragineEnvelopeModel(new CarbonAveragineEnvelopeParameters(),
        new EnvelopeContext(elements, TOL));
  }

  private static IsotopeFinderEngine engine(final List<Element> elements, final int maxCharge) {
    return new IsotopeFinderEngine(elements, maxCharge, TOL, signalModel(elements), "test", false);
  }

  private static IsotopeFinderEngine engineRequireC13(final List<Element> elements,
      final int maxCharge) {
    return new IsotopeFinderEngine(elements, maxCharge, TOL, signalModel(elements), "test", true);
  }

  /**
   * Build a clean 13C ladder at the given charge (Poisson envelope, base peak = 100).
   */
  private static SimpleMassSpectrum ladder(final double monoMz, final int charge,
      final int nCarbons, final int nPeaks) {
    final double spacing = C13 / charge;
    final double lambda = nCarbons * 0.0107;
    final double[] mz = new double[nPeaks];
    final double[] in = new double[nPeaks];
    double p = Math.exp(-lambda);
    for (int k = 0; k < nPeaks; k++) {
      mz[k] = monoMz + k * spacing;
      in[k] = p * 100d;
      p = p * lambda / (k + 1);
    }
    return new SimpleMassSpectrum(mz, in);
  }

  private static SimpleMassSpectrum spec(final double[] mz, final double[] intensity) {
    return new SimpleMassSpectrum(mz, intensity);
  }

  /**
   * Merge several spectra into one, summing intensities of peaks within ~1 mDa (for overlaps).
   */
  private static SimpleMassSpectrum combine(final SimpleMassSpectrum... specs) {
    final TreeMap<Double, Double> map = new TreeMap<>();
    for (final SimpleMassSpectrum s : specs) {
      for (int i = 0; i < s.getNumberOfDataPoints(); i++) {
        final double mz = s.getMzValue(i);
        final Double floor = map.floorKey(mz);
        final Double ceil = map.ceilingKey(mz);
        Double key = null;
        if (floor != null && Math.abs(floor - mz) < 1e-3) {
          key = floor;
        } else if (ceil != null && Math.abs(ceil - mz) < 1e-3) {
          key = ceil;
        }
        if (key != null) {
          map.put(key, map.get(key) + s.getIntensityValue(i));
        } else {
          map.put(mz, s.getIntensityValue(i));
        }
      }
    }
    final double[] mz = new double[map.size()];
    final double[] in = new double[map.size()];
    int k = 0;
    for (final var e : map.entrySet()) {
      mz[k] = e.getKey();
      in[k] = e.getValue();
      k++;
    }
    return new SimpleMassSpectrum(mz, in);
  }

  /**
   * Real CDK-generated isotope distribution for a formula at the given charge, sorted by m/z.
   */
  private static SimpleMassSpectrum fromFormula(final String formula, final int charge) {
    return fromFormula(formula, charge, 0.00005); // default high resolution
  }

  /**
   * Real CDK isotope distribution at a given merge width: small width keeps fine structure
   * resolved, large width merges it into one peak per nominal mass.
   */
  private static SimpleMassSpectrum fromFormula(final String formula, final int charge,
      final double mergeWidth) {
    final IsotopePattern p = IsotopePatternCalculator.calculateIsotopePattern(formula, 0.001,
        mergeWidth, charge, PolarityType.POSITIVE, false);
    final TreeMap<Double, Double> map = new TreeMap<>();
    for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
      map.merge(p.getMzValue(i), p.getIntensityValue(i), Double::sum);
    }
    final double[] mz = new double[map.size()];
    final double[] in = new double[map.size()];
    int k = 0;
    for (final var e : map.entrySet()) {
      mz[k] = e.getKey();
      in[k] = e.getValue();
      k++;
    }
    return new SimpleMassSpectrum(mz, in);
  }

  /**
   * Real CDK isotope distribution merged to ~one peak per nominal isotope offset (fine structure
   * collapsed), sorted by m/z. {@code minAbundance} prunes the long tail to keep large molecules
   * fast.
   */
  private static SimpleMassSpectrum cdkSpectrum(final String formula, final int charge,
      final double minAbundance) {
    final double mergeWidth =
        0.01 * C13 / charge; // < isotope spacing -> one peak per nominal offset
    final IsotopePattern p = IsotopePatternCalculator.calculateIsotopePattern(formula, minAbundance,
        mergeWidth, charge, PolarityType.POSITIVE, false);
    final TreeMap<Double, Double> map = new TreeMap<>();
    for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
      map.merge(p.getMzValue(i), p.getIntensityValue(i), Double::sum);
    }
    final double[] mz = new double[map.size()];
    final double[] in = new double[map.size()];
    int k = 0;
    for (final var e : map.entrySet()) {
      mz[k] = e.getKey();
      in[k] = e.getValue();
      k++;
    }
    return new SimpleMassSpectrum(mz, in);
  }

  /**
   * Index of the most intense peak (the base peak / apex of the envelope).
   */
  private static int baseIndex(final MassSpectrum s) {
    int idx = 0;
    for (int i = 1; i < s.getNumberOfDataPoints(); i++) {
      if (s.getIntensityValue(i) > s.getIntensityValue(idx)) {
        idx = i;
      }
    }
    return idx;
  }

  /**
   * Shift every m/z by {@code dmz} (to place an interferent away from the target).
   */
  private static SimpleMassSpectrum shift(final SimpleMassSpectrum s, final double dmz) {
    final double[] mz = new double[s.getNumberOfDataPoints()];
    final double[] in = new double[mz.length];
    for (int i = 0; i < mz.length; i++) {
      mz[i] = s.getMzValue(i) + dmz;
      in[i] = s.getIntensityValue(i);
    }
    return new SimpleMassSpectrum(mz, in);
  }

  private static IsotopeFinderEngine engineTol(final List<Element> elements, final int maxCharge,
      final MZTolerance tol) {
    return new IsotopeFinderEngine(elements, maxCharge, tol,
        new CarbonAveragineEnvelopeModel(new CarbonAveragineEnvelopeParameters(),
            new EnvelopeContext(elements, tol)), "test", false);
  }

  private static boolean containsMz(final IsotopePattern p, final double mz) {
    for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
      if (Math.abs(p.getMzValue(i) - mz) < 0.02) {
        return true;
      }
    }
    return false;
  }

  /**
   * Br3C10 isotope pattern (mono is not the base peak; Br doublets).
   */
  private static SimpleMassSpectrum br3C10() {
    return spec(
        new double[]{356.7545, 357.7578, 358.7524, 358.7612, 359.7558, 360.7504, 360.7591, 361.7537,
            362.7483, 362.7571, 363.7517, 364.7550},
        new double[]{34.27, 3.71, 100.00, 0.18, 10.82, 97.28, 0.53, 10.52, 31.54, 0.51, 3.41,
            0.17});
  }

  @Test
  void carbonAveragineM1RatioMatchesCarbonCount() {
    final EnvelopeModel model = signalModel(
        List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O")));
    final IsotopeEnvelope env = model.buildEnvelope(500.0, 1, PolarityType.POSITIVE);
    // ~36 carbons at 500 Da -> M+1/M ≈ 36 * 0.0107 ≈ 0.39
    final double ratio = env.expectedAt(1) / env.expectedAt(0);
    assertTrue(ratio > 0.30 && ratio < 0.45, "Unexpected M+1/M ratio: " + ratio);
  }

  @Test
  void detectsChargeOneForSinglyChargedPattern() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final DetectionResult result = engine(elements, 3).detect(ladder(500.0, 1, 36, 6), 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void detectsChargeTwoForDoublyChargedPattern() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    // doubly charged: peaks spaced 0.5017, many carbons
    final DetectionResult result = engine(elements, 2).detect(ladder(400.0, 2, 60, 8), 400.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(2, result.bestCharge());
  }

  @Test
  void doesNotPromoteChargeTwoWhenHalfSpacingPeaksAbsent() {
    // a clean charge-1 pattern must not be read as charge 2 (no half-spacing peaks present)
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final DetectionResult result = engine(elements, 2).detect(ladder(700.0, 1, 50, 6), 700.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void terminationCrossesSmallM1GapToCaptureBrM2() {
    // few carbons -> tiny/absent M+1, but a large 81Br M+2 bump that must still be captured
    final List<Element> elements = List.of(new Element("C"), new Element("Br"));
    final double mono = 500.0;
    final SimpleMassSpectrum spectrum = spec(new double[]{mono, mono + 1.99795},
        new double[]{100d, 97d});
    final DetectionResult result = engine(elements, 2).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
    final IsotopePattern p = result.patterns().get(0);
    assertEquals(2, p.getNumberOfDataPoints(), "Br M+2 should be retained across the M+1 gap");
  }

  @Test
  void detectsChargeOneAcrossElementMixes() {
    final double mono = 500.0;
    // each case: allowed elements + a realistic singly-charged spectrum (mono, 13C M+1, heavy M+2)
    record Case(String name, List<Element> elements, double[] mz, double[] intensity) {

    }
    final List<Case> cases = List.of(new Case("CHNO",
            List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O")),
            new double[]{mono, mono + C13, mono + 2 * C13}, new double[]{100d, 38d, 8d}),
        new Case("CHNO+S", List.of(new Element("C"), new Element("S")),
            new double[]{mono, mono + C13, mono + 1.9958}, new double[]{100d, 38d, 4.5d}),
        new Case("CHNO+Cl", List.of(new Element("C"), new Element("Cl")),
            new double[]{mono, mono + C13, mono + 1.99705}, new double[]{100d, 38d, 32d}),
        new Case("CHNO+Br", List.of(new Element("C"), new Element("Br")),
            new double[]{mono, mono + C13, mono + 1.99795}, new double[]{100d, 38d, 97d}));

    for (final Case c : cases) {
      final DetectionResult result = engine(c.elements(), 3).detect(spec(c.mz(), c.intensity()),
          mono, 100d, PolarityType.POSITIVE);
      assertNotNull(result, c.name() + ": no pattern detected");
      assertEquals(1, result.bestCharge(), c.name() + ": wrong charge");
      assertTrue(result.patterns().get(0).getNumberOfDataPoints() >= 2,
          c.name() + ": pattern too small");
    }
  }

  @Test
  void detectsChargeOneAtLowResolutionMergedFineStructure() {
    // low resolution: fine structure merged into a single peak per nominal offset
    final List<Element> elements = List.of(new Element("C"), new Element("N"), new Element("O"));
    final DetectionResult result = engine(elements, 2).detect(ladder(600.0, 1, 40, 5), 600.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void formulaPredictionModeDetectsChargeOne() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final EnvelopeModel model = new FormulaEnvelopeModel(new FormulaEnvelopeParameters(),
        new EnvelopeContext(elements, TOL));
    final IsotopeFinderEngine engine = new IsotopeFinderEngine(elements, 2, TOL, model, "formula",
        false);
    // small molecule mass; formula enumeration is cheap here
    final DetectionResult result = engine.detect(ladder(195.0877, 1, 8, 4), 195.0877, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void crossScanRefinerRecoversResolvedSignalAndDropsTransient() {
    // detected on the top scan: only mono + M+1 (M+2 was merged/absent there)
    final IsotopePattern detected = new SimpleIsotopePattern(
        new DataPoint[]{new SimpleDataPoint(500.0, 100d), new SimpleDataPoint(500.0 + C13, 30d)}, 1,
        IsotopePatternStatus.DETECTED, "test");

    final double m2 = 500.0 + 2 * C13;
    final List<MassSpectrum> scans = List.of(
        spec(new double[]{500.0, 500.0 + C13, m2}, new double[]{100d, 30d, 10d}),
        spec(new double[]{500.0, 500.0 + C13, m2}, new double[]{100d, 30d, 10d}),
        spec(new double[]{500.0, 500.0 + C13, m2}, new double[]{100d, 30d, 10d}),
        // a transient noise peak present in only one scan, off the isotope grid
        spec(new double[]{500.0, 500.0 + C13, 503.5}, new double[]{100d, 30d, 50d}));

    final IsotopePattern refined = CrossScanRefiner.refine(detected, scans, TOL,
        RatioAggregation.MEDIAN, 2);

    boolean hasM2 = false;
    boolean hasTransient = false;
    for (int i = 0; i < refined.getNumberOfDataPoints(); i++) {
      final double mz = refined.getMzValue(i);
      if (Math.abs(mz - m2) < 0.01) {
        hasM2 = true;
      }
      if (Math.abs(mz - 503.5) < 0.05) {
        hasTransient = true;
      }
    }
    assertTrue(hasM2, "M+2 resolved in >= 2 scans should be recovered");
    assertTrue(!hasTransient, "transient peak present in < minScans should be dropped");
  }

  @Test
  void detectsChargeThree() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final DetectionResult result = engine(elements, 3).detect(ladder(300.0, 3, 90, 9), 300.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(3, result.bestCharge());
  }

  @Test
  void doesNotInflateChargeWithLargeMaxCharge() {
    // a clean charge-1 pattern must stay charge 1 even when many charge hypotheses are evaluated
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final DetectionResult result = engine(elements, 8).detect(ladder(700.0, 1, 50, 6), 700.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void detectsFromNonMonoisotopicStartSignal() {
    // start the search at the M+2 peak (a "random" signal in the pattern)
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final SimpleMassSpectrum spectrum = ladder(500.0, 1, 40, 5);
    final double startMz = spectrum.getMzValue(2); // M+2
    final DetectionResult result = engine(elements, 2).detect(spectrum, startMz,
        spectrum.getIntensityValue(2), PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
    boolean hasMono = false;
    final IsotopePattern p = result.patterns().get(0);
    for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
      if (Math.abs(p.getMzValue(i) - 500.0) < 0.01) {
        hasMono = true;
      }
    }
    assertTrue(hasMono, "downward search should recover the monoisotopic peak");
  }

  @Test
  void detectsRealBrominePatternAsChargeOne() {
    // mono (356.75) is only 34 %; the base peak is the M+2 (358.75). Start from mono.
    final List<Element> elements = List.of(new Element("C"), new Element("Br"));
    final DetectionResult result = engine(elements, 2).detect(br3C10(), 356.7545, 34.27d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
    assertTrue(result.patterns().get(0).getNumberOfDataPoints() >= 6,
        "should recover most of the bromine isotope pattern");
  }

  @Test
  void detectsChargeOneInNegativeMode() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("O"));
    final DetectionResult result = engine(elements, 2).detect(ladder(450.0, 1, 30, 5), 450.0, 100d,
        PolarityType.NEGATIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void flagsBothChargesForOverlappingMonomerAndDimer() {
    // [M+H]+ (charge 1) and [2M+2H]2+ (charge 2) share the same monoisotopic m/z (500)
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final SimpleMassSpectrum monomer = ladder(500.0, 1, 40, 5);
    final SimpleMassSpectrum dimer = ladder(500.0, 2, 80, 9);
    final DetectionResult result = engine(elements, 2).detect(combine(monomer, dimer), 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertTrue(result.patterns().size() >= 2, "both charge states should be reported");
    final boolean hasCharge1 = result.scores().stream().anyMatch(s -> s.charge() == 1);
    final boolean hasCharge2 = result.scores().stream().anyMatch(s -> s.charge() == 2);
    assertTrue(hasCharge1 && hasCharge2, "both charge 1 and charge 2 should be flagged");
  }

  @Test
  void singleSpuriousHalfSpacingPeakDoesNotWinChargeTwo() {
    // a clean charge-1 pattern with ONE spurious half-spacing peak must not be read as charge 2
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final SimpleMassSpectrum spectrum = spec(new double[]{500.0, 500.5017, 501.0033, 502.0067},
        new double[]{100d, 15d, 40d, 8d});
    final DetectionResult result = engine(elements, 2).detect(spectrum, 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void retainsResolvedFineStructure() {
    // 15N M+1 (500.99703) and 13C M+1 (501.00336) are resolved -> both retained in the pattern
    final List<Element> elements = List.of(new Element("C"), new Element("N"));
    final SimpleMassSpectrum spectrum = spec(new double[]{500.0, 500.99703, 501.00336},
        new double[]{100d, 20d, 38d});
    final DetectionResult result = engine(elements, 2).detect(spectrum, 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
    assertEquals(3, result.patterns().get(0).getNumberOfDataPoints(),
        "both resolved M+1 fine-structure peaks should be retained");
  }

  @Test
  void returnsNullForIsolatedPeak() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final SimpleMassSpectrum spectrum = spec(new double[]{500.0, 700.0}, new double[]{100d, 50d});
    final DetectionResult result = engine(elements, 2).detect(spectrum, 500.0, 100d,
        PolarityType.POSITIVE);
    org.junit.jupiter.api.Assertions.assertNull(result, "isolated peak has no isotope pattern");
  }

  @Test
  void resolvesCnoFineStructureAtHighResAndMergesAtLowRes() {
    // CNO-rich molecule: M+1 splits into 13C / 15N (~6 mDa), M+2 into 13C2 / 13C15N / 18O, etc.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final String formula = "C50H70N15O15";

    final SimpleMassSpectrum highRes = fromFormula(formula, 1, 0.0005); // fine structure resolved
    final SimpleMassSpectrum lowRes = fromFormula(formula, 1, 0.3);     // merged to nominal masses
    // sanity: the high-resolution spectrum actually contains more peaks than the low-resolution one
    assertTrue(highRes.getNumberOfDataPoints() > lowRes.getNumberOfDataPoints(),
        "high-res spectrum should have resolved fine structure");

    final DetectionResult hi = engine(elements, 2).detect(highRes, highRes.getMzValue(0),
        highRes.getIntensityValue(0), PolarityType.POSITIVE);
    final DetectionResult lo = engine(elements, 2).detect(lowRes, lowRes.getMzValue(0),
        lowRes.getIntensityValue(0), PolarityType.POSITIVE);

    assertNotNull(hi);
    assertNotNull(lo);
    // correct charge regardless of resolution
    assertEquals(1, hi.bestCharge());
    assertEquals(1, lo.bestCharge());
    // the detected pattern keeps the resolved fine structure at high resolution
    assertTrue(
        hi.patterns().get(0).getNumberOfDataPoints() > lo.patterns().get(0).getNumberOfDataPoints(),
        "high-resolution detection should retain more (fine-structure) peaks than low-resolution");
  }

  @Test
  void detectsChargeFromRealCdkIsotopePatterns() {
    record Case(String formula, int charge, List<Element> elements) {

    }
    final List<Case> cases = List.of(
        new Case("C30H50", 1, List.of(new Element("C"), new Element("H"))),
        new Case("C20H30Cl", 1, List.of(new Element("C"), new Element("H"), new Element("Cl"))),
        new Case("C15H20Br", 1, List.of(new Element("C"), new Element("H"), new Element("Br"))),
        new Case("C40H60N2O3S", 1,
            List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O"),
                new Element("S"))),
        new Case("C60H100", 2, List.of(new Element("C"), new Element("H"))),
        // doubly charged, CNO-rich: 15N/13C fine structure interleaves with the +0.5 half-spacing ladder
        new Case("C50H70N15O15", 2,
            List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O"))));

    for (final Case c : cases) {
      final SimpleMassSpectrum spectrum = fromFormula(c.formula(), c.charge());
      final double mono = spectrum.getMzValue(0);
      final DetectionResult result = engine(c.elements(), 3).detect(spectrum, mono,
          spectrum.getIntensityValue(0), PolarityType.POSITIVE);
      assertNotNull(result, c.formula() + ": no pattern detected");
      assertEquals(c.charge(), result.bestCharge(),
          c.formula() + ": wrong charge (z=" + c.charge() + ")");
      assertTrue(result.patterns().get(0).getNumberOfDataPoints() >= 3,
          c.formula() + ": pattern too small");
    }
  }

  @Test
  void detectsChargeInComplexCdkSpectrumFromAnyStartSignal() {
    // Build real CDK isotope patterns, embed each in a complex MS1 (a co-eluting decoy compound +
    // off-grid noise), then seed detection from EVERY isotope signal of the target. The correct
    // charge and the target's monoisotopic must be recovered regardless of the start signal, and
    // neither the decoy compound nor the noise may leak into the detected pattern.
    record Case(String formula, int charge, List<Element> elements) {

    }
    final List<Case> cases = List.of(
        new Case("C30H50", 1, List.of(new Element("C"), new Element("H"))),
        new Case("C20H30Cl", 1, List.of(new Element("C"), new Element("H"), new Element("Cl"))),
        new Case("C25H30Cl2", 1, List.of(new Element("C"), new Element("H"), new Element("Cl"))),
        new Case("C15H20Br", 1, List.of(new Element("C"), new Element("H"), new Element("Br"))),
        new Case("C40H60N2O3S", 1,
            List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O"),
                new Element("S"))),
        new Case("C60H100", 2, List.of(new Element("C"), new Element("H"))),
        new Case("C50H70N15O15", 2,
            List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O"))),
        // triply charged peptide-like envelope (consecutive isotopes spaced ~0.334 m/z)
        new Case("C80H120N20O25", 3,
            List.of(new Element("C"), new Element("H"), new Element("N"), new Element("O"))));

    for (final Case c : cases) {
      final SimpleMassSpectrum target = fromFormula(c.formula(), c.charge());
      final double mono = target.getMzValue(0);
      final double decoyMono = mono + 2.63; // co-eluting compound, off the target isotope grid
      final SimpleMassSpectrum decoy = ladder(decoyMono, 1, 28, 4);
      // fixed, deterministic off-grid noise within the search window (must be ignored)
      final SimpleMassSpectrum noise = spec(
          new double[]{mono - 1.53, mono + 0.37, mono + 1.62, mono + 3.49},
          new double[]{60d, 80d, 45d, 70d});
      final SimpleMassSpectrum complex = combine(target, decoy, noise);

      for (int i = 0; i < target.getNumberOfDataPoints(); i++) {
        final double startMz = target.getMzValue(i);
        final String where =
            c.formula() + " (z=" + c.charge() + ") seeded at peak " + i + " m/z=" + startMz;
        final DetectionResult r = engine(c.elements(), 3).detect(complex, startMz,
            target.getIntensityValue(i), PolarityType.POSITIVE);
        assertNotNull(r, "no detection: " + where);
        assertEquals(c.charge(), r.bestCharge(), "wrong charge: " + where);
        final IsotopePattern p = r.patterns().get(0);
        assertTrue(containsMz(p, mono), "target monoisotopic missing: " + where);
        assertTrue(!containsMz(p, decoyMono), "decoy compound leaked into pattern: " + where);
        assertTrue(!containsMz(p, mono + 0.37), "noise leaked into pattern: " + where);
      }
    }
  }

  @Test
  void keepsCoElutingCompoundsSeparate() {
    // two different compounds co-eluting in the same MS1 scan, monoisotopic masses 2.6 Da apart
    // (a non-integer offset, so B does not sit on A's 13C grid)
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("O"));
    final SimpleMassSpectrum a = ladder(500.0, 1, 36, 3); // 500.000, 501.003, 502.007
    final SimpleMassSpectrum b = ladder(502.6, 1, 36, 3); // 502.600, 503.603, 504.607
    final SimpleMassSpectrum combined = combine(a, b);

    // start from compound A -> must capture only A's isotopes, not B's
    final DetectionResult ra = engine(elements, 2).detect(combined, 500.0, a.getIntensityValue(0),
        PolarityType.POSITIVE);
    assertNotNull(ra);
    assertEquals(1, ra.bestCharge());
    final IsotopePattern pa = ra.patterns().get(0);
    assertTrue(containsMz(pa, 500.0), "A's monoisotopic peak");
    assertTrue(containsMz(pa, 501.00336), "A's M+1");
    assertTrue(!containsMz(pa, 502.6), "compound B's monoisotopic must not leak into A");
    assertTrue(!containsMz(pa, 503.60336), "compound B's M+1 must not leak into A");
    assertTrue(!containsMz(pa, 504.60671), "compound B's M+2 must not leak into A");

    // start from compound B -> symmetric: must capture only B's isotopes, not A's
    final DetectionResult rb = engine(elements, 2).detect(combined, 502.6, b.getIntensityValue(0),
        PolarityType.POSITIVE);
    assertNotNull(rb);
    assertEquals(1, rb.bestCharge());
    final IsotopePattern pb = rb.patterns().get(0);
    assertTrue(containsMz(pb, 502.6), "B's monoisotopic peak");
    assertTrue(containsMz(pb, 503.60336), "B's M+1");
    assertTrue(!containsMz(pb, 500.0), "compound A's monoisotopic must not leak into B");
    assertTrue(!containsMz(pb, 502.00671), "compound A's M+2 must not leak into B");
  }

  @Test
  void detectsMinimalTwoPeakPatternAsChargeOne() {
    final List<Element> elements = List.of(new Element("C"));
    final SimpleMassSpectrum spectrum = spec(new double[]{500.0, 501.00336},
        new double[]{100d, 40d});
    final DetectionResult result = engine(elements, 2).detect(spectrum, 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(result);
    assertEquals(1, result.bestCharge());
  }

  @Test
  void chargeAndScoreAreIndependentOfStartSignal() {
    // position-agnostic: seeding the search from ANY peak of the pattern must give the same charge
    // and the same carbon-fit score (scoring anchors on the intensity-max + a sliding template, not
    // on the seed)
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final SimpleMassSpectrum spectrum = ladder(800.0, 1, 55, 7);
    Integer charge = null;
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
      final DetectionResult r = engine(elements, 3).detect(spectrum, spectrum.getMzValue(i),
          spectrum.getIntensityValue(i), PolarityType.POSITIVE);
      assertNotNull(r, "no detection seeding from peak " + i);
      // the charge must be identical no matter which signal seeded the search (the position-agnostic
      // invariant), and a strong carbon fit must be found from every seed
      if (charge == null) {
        charge = r.bestCharge();
      } else {
        assertEquals(charge, r.bestCharge(),
            "charge must not depend on start signal (peak " + i + ")");
      }
      assertTrue(r.scores().get(0).carbonFit() > 0.9,
          "a strong carbon fit must be found from any start signal (peak " + i + ")");
    }
    assertEquals(1, charge);
  }

  @Test
  void heavyIsotopeDoesNotContaminateCarbonScore() {
    // a resolved 37Cl at M+2 (1.99705) is distinct from 13C2 (2.00671); it must not change the carbon
    // fit or the charge, yet must still be retained in the inclusive output pattern
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("Cl"));
    final double mono = 500.0;
    final DetectionResult noCl = engine(elements, 2).detect(
        spec(new double[]{mono, mono + C13, mono + 2 * C13}, new double[]{100d, 38d, 7d}), mono,
        100d, PolarityType.POSITIVE);
    // same spectrum + a large resolved 37Cl peak at M+1.99705 (between M+1 and 13C2)
    final DetectionResult withCl = engine(elements, 2).detect(
        spec(new double[]{mono, mono + C13, mono + 1.99705, mono + 2 * C13},
            new double[]{100d, 38d, 30d, 7d}), mono, 100d, PolarityType.POSITIVE);
    assertNotNull(noCl);
    assertNotNull(withCl);
    assertEquals(noCl.bestCharge(), withCl.bestCharge(), "37Cl must not change the charge");
    assertEquals(noCl.scores().get(0).carbonFit(), withCl.scores().get(0).carbonFit(), 1e-6,
        "37Cl must not contaminate the carbon fit");
    assertTrue(containsMz(withCl.patterns().get(0), mono + 1.99705),
        "37Cl must still be retained in the inclusive output pattern");
  }

  @Test
  void detectsProteinLikeHumpWithoutMonoisotopic() {
    // high-mass envelope whose base peak is well above the monoisotopic; drop the (low) mono so the
    // pattern is a rising hump, and seed mid-hump -> must still detect charge 1
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final SimpleMassSpectrum full = ladder(2000.0, 1, 140, 12);
    final double[] mz = new double[full.getNumberOfDataPoints() - 1];
    final double[] in = new double[mz.length];
    for (int i = 1; i < full.getNumberOfDataPoints(); i++) {
      mz[i - 1] = full.getMzValue(i);
      in[i - 1] = full.getIntensityValue(i);
    }
    final SimpleMassSpectrum hump = spec(mz, in);
    final DetectionResult r = engine(elements, 3).detect(hump, hump.getMzValue(3),
        hump.getIntensityValue(3), PolarityType.POSITIVE);
    assertNotNull(r);
    assertEquals(1, r.bestCharge());
  }

  @Test
  void requireC13AcceptsValidCarbonPattern() {
    // a normal CHNO pattern with an in-bounds 13C M+1 passes the gate
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final DetectionResult r = engineRequireC13(elements, 2).detect(ladder(500.0, 1, 36, 5), 500.0,
        100d, PolarityType.POSITIVE);
    assertNotNull(r, "valid 13C pattern should pass the require-13C gate");
    assertEquals(1, r.bestCharge());
  }

  @Test
  void requireC13RejectsMissingM1() {
    // only mono + a heavy M+2 (81Br), no 13C M+1 at all -> rejected when 13C is required
    final List<Element> elements = List.of(new Element("C"), new Element("Br"));
    final double mono = 500.0;
    final SimpleMassSpectrum spectrum = spec(new double[]{mono, mono + 1.99795},
        new double[]{100d, 97d});
    final DetectionResult r = engineRequireC13(elements, 2).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    org.junit.jupiter.api.Assertions.assertNull(r,
        "pattern without a 13C M+1 must be rejected when 13C is required");
    // sanity: without the gate the same pattern is still detected (heavy-spacing fallback)
    assertNotNull(engine(elements, 2).detect(spectrum, mono, 100d, PolarityType.POSITIVE));
  }

  @Test
  void requireC13RejectsOutOfBoundsM1() {
    // mono is clearly the base but its 13C M+1 is far too small for the mass (~36 C at 500 Da would
    // give M+1/M ≈ 0.39; here it is 0.02) -> below the lower carbon bound -> rejected
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final double mono = 500.0;
    final SimpleMassSpectrum spectrum = spec(new double[]{mono, mono + C13, mono + 2 * C13},
        new double[]{100d, 2d, 0.4d});
    final DetectionResult r = engineRequireC13(elements, 2).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    org.junit.jupiter.api.Assertions.assertNull(r,
        "M+1/M ratio far below the carbon bounds must be rejected when 13C is required");
  }

  @Test
  void detectsHighChargeProteinsFromAnyStartSignal() {
    // high-res FT tolerance so neighbouring high charge states are distinguishable
    final MZTolerance tightTol = new MZTolerance(0.0005, 2);
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"), new Element("S"));

    record Protein(String formula, int charge) {

    }
    final List<Protein> proteins = List.of(new Protein("C257H383N65O77S6", 6), // insulin ~5733 Da
        new Protein("C378H629N105O118S", 9),     // ubiquitin ~8565 Da
        new Protein("C436H682N110O125S2", 12),   // ~9.7 kDa
        new Protein("C600H900N150O180S3", 15));  // ~13 kDa, broad distributed envelope

    for (final Protein prot : proteins) {
      final SimpleMassSpectrum target = cdkSpectrum(prot.formula(), prot.charge(), 0.02);
      final int base = baseIndex(target);
      final double baseMz = target.getMzValue(base);
      // a co-eluting interferent (different compound + charge) shifted well away in m/z, plus a few
      // off-grid noise peaks inside the envelope range that must be ignored
      final SimpleMassSpectrum interferent = shift(
          cdkSpectrum("C300H450N80O90S2", prot.charge() == 6 ? 5 : prot.charge() - 1, 0.05), 40.0);
      final SimpleMassSpectrum noise = spec(
          new double[]{baseMz - 0.37, baseMz + 0.41, baseMz + 0.61},
          new double[]{baseHeight(target) * 0.7, baseHeight(target) * 0.9,
              baseHeight(target) * 0.6});
      final SimpleMassSpectrum complex = combine(target, interferent, noise);

      // seed from several signals of the target: lowest visible, base, highest tail, and mid-rising
      final int[] seeds = {0, base, target.getNumberOfDataPoints() - 1, base / 2};
      for (final int s : seeds) {
        final DetectionResult r = engineTol(elements, 16, tightTol).detect(complex,
            target.getMzValue(s), target.getIntensityValue(s), PolarityType.POSITIVE);
        assertNotNull(r, prot.formula() + " z=" + prot.charge() + " seed " + s + ": no detection");
        assertEquals(prot.charge(), r.bestCharge(),
            prot.formula() + " z=" + prot.charge() + " seed " + s + ": wrong charge");
      }
    }
  }

  @Test
  void detectsPolyhalogenatedSmallMoleculesFromAnyStartSignal() {
    record Halo(String formula, List<Element> elements) {

    }
    final Element br = new Element("Br");
    final Element cl = new Element("Cl");
    final List<Halo> cases = List.of(
        // Pigment Green 7 (perchloro copper phthalocyanine): mono is tiny, base is far up the envelope
        new Halo("C32Cl16CuN8",
            List.of(new Element("C"), br, cl, new Element("Cu"), new Element("N"))),
        // BDE-209 (decabromodiphenyl ether): 10 Br -> very broad, base near M+10
        new Halo("C12Br10O", List.of(new Element("C"), br, cl, new Element("O"))),
        new Halo("C6Cl6", List.of(new Element("C"), br, cl)),
        new Halo("C10H4Cl2Br2", List.of(new Element("C"), new Element("H"), br, cl, br)));

    for (int expCharge = 1; expCharge < 4; expCharge++) {

      for (final Halo h : cases) {
        final SimpleMassSpectrum target = cdkSpectrum(h.formula(), expCharge, 0.01);
        final int base = baseIndex(target);
        final SimpleMassSpectrum noise = spec(
            new double[]{target.getMzValue(0) - 0.37, target.getMzValue(base) + 0.41},
            new double[]{baseHeight(target) * 0.5, baseHeight(target) * 0.6});
        final SimpleMassSpectrum complex = combine(target, noise);

        final int[] seeds = {0, base, target.getNumberOfDataPoints() - 1};
        for (final int s : seeds) {
          final DetectionResult r = engine(h.elements(), 6).detect(complex, target.getMzValue(s),
              target.getIntensityValue(s), PolarityType.POSITIVE);
          assertNotNull(r, h.formula() + " seed " + s + ": no detection");
          assertEquals(expCharge, r.bestCharge(), h.formula() + " seed " + s + ": wrong charge");
        }
      }
    }
  }

  private static double baseHeight(final MassSpectrum s) {
    return s.getIntensityValue(baseIndex(s));
  }

  @Test
  void detectedPatternCarriesFiniteScore() {
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final DetectionResult r = engine(elements, 3).detect(ladder(500.0, 1, 36, 6), 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(r);
    final double score = r.patterns().get(0).getScore();
    assertTrue(!Double.isNaN(score) && score > 0d && score <= 1.0001,
        "pattern score should be a finite (0,1] value but was " + score);
    assertEquals(r.scores().get(0).score(), score, 1e-9,
        "ChargeScore.score should match the stored pattern score");
  }

  @Test
  void multiChargePatternSortedByScoreBestFirst() {
    // [M+H]+ and [2M+2H]2+ overlap at the same monoisotopic m/z -> both charges flagged
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final SimpleMassSpectrum monomer = ladder(500.0, 1, 40, 5);
    final SimpleMassSpectrum dimer = ladder(500.0, 2, 80, 9);
    final DetectionResult r = engine(elements, 2).detect(combine(monomer, dimer), 500.0, 100d,
        PolarityType.POSITIVE);
    assertNotNull(r);
    org.junit.jupiter.api.Assumptions.assumeTrue(r.patterns().size() >= 2);
    final IsotopePattern assembled = IsotopeFinderEngine.assemble(r.patterns());
    assertTrue(assembled instanceof MultiChargeStateIsotopePattern);
    final List<IsotopePattern> ordered = ((MultiChargeStateIsotopePattern) assembled).getPatterns();
    for (int i = 1; i < ordered.size(); i++) {
      assertTrue(ordered.get(i - 1).getScore() >= ordered.get(i).getScore(),
          "assembled patterns must be ordered by score, best first");
    }
    assertEquals(assembled.getScore(), ordered.get(0).getScore(), 1e-9,
        "the multi pattern exposes the best (preferred) pattern score");
  }

  @Test
  void insignificantBridgedPeakIsTrimmed() {
    // Br M+2 bridges the (absent) 13C M+1 gap. When significant it is retained; when it is only noise
    // (0.2 % of the base) it must be trimmed instead of widening the pattern.
    final List<Element> elements = List.of(new Element("C"), new Element("Br"));
    final double mono = 500.0;
    final double m2 = mono + 1.99795;

    final DetectionResult keep = engine(elements, 2).detect(
        spec(new double[]{mono, m2}, new double[]{100d, 97d}), mono, 100d, PolarityType.POSITIVE);
    assertNotNull(keep);
    assertTrue(containsMz(keep.patterns().get(0), m2),
        "a significant bridged Br M+2 should be retained");

    final DetectionResult drop = engine(elements, 2).detect(
        spec(new double[]{mono, m2}, new double[]{100d, 0.2d}), mono, 100d, PolarityType.POSITIVE);
    final boolean present = drop != null && containsMz(drop.patterns().get(0), m2);
    assertTrue(!present, "an insignificant bridged M+2 should be trimmed out of the pattern");
  }
}
