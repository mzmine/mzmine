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
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectedComposition;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ElementAutoDetector;
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
import org.jetbrains.annotations.NotNull;
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
    return new CarbonAveragineEnvelopeModel(CarbonAveragineEnvelopeParameters.createDefault(),
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
   * Same engine as {@link #engine} but with opt-in heavy-element auto-detection enabled over the
   * default candidate set. Charge selection is unchanged; detection only annotates the result.
   */
  @NotNull
  private static IsotopeFinderEngine engineAutoDetect(@NotNull final List<Element> elements,
      final int maxCharge) {
    return new IsotopeFinderEngine(elements, maxCharge, TOL, signalModel(elements), "test", false,
        ElementDetectionMode.AUTO_DETECT, ElementAutoDetector.DEFAULT_CANDIDATES);
  }

  /**
   * Same engine as {@link #engine} but with opt-in heavy-element auto-detection enabled over the
   * default candidate set. Charge selection is unchanged; detection only annotates the result.
   */
  @NotNull
  private static IsotopeFinderEngine engineAutoDetectRequireC13(
      @NotNull final List<Element> elements, final int maxCharge) {
    return new IsotopeFinderEngine(elements, maxCharge, TOL, signalModel(elements), "test", true,
        ElementDetectionMode.AUTO_DETECT, ElementAutoDetector.DEFAULT_CANDIDATES);
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
        new CarbonAveragineEnvelopeModel(CarbonAveragineEnvelopeParameters.createDefault(),
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
  void requireC13TruncatesPatternAtHole() {
    // mono + 13C M+1, then a genuine hole at M+2 (no signal), then on-grid peaks at M+3/M+4 that the
    // model still supports (~72 C at m/z 1000). Without the gate the base engine bridges the gap and
    // keeps M+3/M+4; with require-13C the pattern must terminate at the hole and drop everything past it.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final double mono = 1000.0;
    final SimpleMassSpectrum spectrum = spec(
        new double[]{mono, mono + C13, mono + 3 * C13, mono + 4 * C13},
        new double[]{100d, 78d, 8d, 2d});

    // control: without the gate, M+3 is bridged across the M+2 gap and retained
    final DetectionResult open = engine(elements, 2).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    assertNotNull(open);
    assertTrue(containsMz(open.patterns().get(0), mono + 3 * C13),
        "without require-13C the M+3 is bridged across the gap");

    final DetectionResult gated = engineRequireC13(elements, 2).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    assertNotNull(gated, "the contiguous mono + M+1 part must still be detected");
    assertEquals(1, gated.bestCharge());
    final IsotopePattern gp = gated.patterns().get(0);
    assertEquals(2, gp.getNumberOfDataPoints(), "pattern must be truncated at the M+2 hole");
    assertTrue(!containsMz(gp, mono + 3 * C13), "peaks beyond the hole must be dropped");
  }

  @Test
  void requireC13ToleratesMergedHeavyIsotopeShiftedPeak() {
    // the M+2 signal is a 13C2/37Cl merge whose centroid is pulled ~11 mDa below the exact 13C grid
    // (beyond the 1x tolerance of 5 mDa, within the 3x gap tolerance of 15 mDa). It must NOT be read
    // as a hole, so the pattern stays contiguous and the merged peak is retained rather than dropped.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("Cl"));
    final double mono = 500.0;
    final double mergedM2 = 501.996; // exact 13C grid at offset 2 is 502.00671 -> ~10.7 mDa off
    final SimpleMassSpectrum spectrum = spec(new double[]{mono, mono + C13, mergedM2},
        new double[]{100d, 38d, 33d});
    final DetectionResult r = engineRequireC13(elements, 2).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    assertNotNull(r);
    assertEquals(1, r.bestCharge());
    // without the 3x relaxation the shifted peak would be read as a hole at M+2 and dropped
    assertTrue(containsMz(r.patterns().get(0), mergedM2),
        "the merged/shifted M+2 must count as present, not a hole");
  }

  @Test
  void requireC13AcceptsProteinHumpWithoutMonoisotopic() {
    // a mono-less protein-like hump (base is mid-envelope): require-13C must NOT reject it, because
    // the 13C ladder is gap-free through the hump even though there is no visible monoisotopic.
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
    final DetectionResult r = engineRequireC13(elements, 3).detect(hump, hump.getMzValue(3),
        hump.getIntensityValue(3), PolarityType.POSITIVE);
    assertNotNull(r, "a mono-less hump must still pass require-13C via its gap-free 13C ladder");
    assertEquals(1, r.bestCharge());
  }

  @Test
  void requireC13AcceptsEverySecond13CLadder() {
    // pattern present only on every second 13C position (base + 13C2 + 13C4), the odd positions
    // swallowed - as for a molecule dominated by an intense +2 heavy comb. The every-13C (step 1)
    // ladder has an immediate hole at +1/-1, so require-13C must fall back to the every-second
    // (step 2) ladder and still accept the pattern rather than truncating it to a single peak.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("O"));
    final double mono = 500.0;
    final SimpleMassSpectrum spectrum = spec(new double[]{mono, mono + 2 * C13, mono + 4 * C13},
        new double[]{40d, 100d, 60d});
    final DetectionResult r = engineRequireC13(elements, 2).detect(spectrum, mono + 2 * C13, 100d,
        PolarityType.POSITIVE);
    assertNotNull(r, "an every-second 13C ladder must be accepted via the step-2 fallback");
    assertEquals(1, r.bestCharge());
    final IsotopePattern p = r.patterns().get(0);
    assertTrue(containsMz(p, mono) && containsMz(p, mono + 4 * C13),
        "the full every-second ladder must be retained");
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
  void ftRingingAroundSinglyChargedSignalIsNotReadAsHighCharge() {
    // real measured spectrum: m/z 734.4683 (1.17e9) is the monoisotopic of a singly charged compound
    // with a clean 13C ladder (735.47/736.47/737.48/738.48). FT ringing around the strong base peak
    // (734.27/734.32/734.35/734.59/734.62/734.67/735.32/735.63, all <1% of the base) previously fit a
    // z=5 grid and won. The ringing "M+1" is orders of magnitude too small to be a real 13C peak at the
    // ~3670 Da a z=5 ion implies -> must be detected as charge 1 with the 5-peak 13C ladder.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));
    final double[] mz = {150.0245667, 158.1175232, 158.9615631, 177.5477142, 178.049408,
        179.5753632, 183.6121521, 183.6225281, 183.8628387, 183.8734283, 205.0708313, 211.1441345,
        223.0636597, 224.0640106, 233.1644897, 245.1287231, 261.1272583, 261.159668, 262.1632996,
        263.1661987, 268.1082458, 330.1274414, 331.1536255, 340.1652222, 341.1695862, 354.0880127,
        355.0910645, 356.0828247, 361.0848694, 521.3115845, 540.3512573, 558.3635864, 559.3671875,
        560.3695679, 576.3742065, 577.378418, 716.4584351, 717.4613647, 718.4633179, 734.2695313,
        734.3152466, 734.3459473, 734.4682617, 734.5924683, 734.6229248, 734.6674194, 735.3190918,
        735.4714355, 735.6273804, 736.4740601, 737.4765625, 738.4807739, 748.4472656, 749.4506226,
        750.4542847, 756.4498901, 757.4528809, 1467.943481, 1468.922852, 1489.913452, 1490.917725,
        1491.917969, 1492.924316};
    final double[] intensity = {4864261.5, 2966466, 1955386.5, 1.59E7, 3244166.75, 1718261, 1.12E7,
        5980043, 4433066, 2226075.25, 1.80E7, 3160431.75, 1.34E7, 2583190.25, 1818151.625,
        1857023.75, 1554571.5, 2.08E8, 3.35E7, 3306679, 1480045.5, 1502891, 5644717.5, 9033896,
        1379463.375, 5.64E7, 1.11E7, 2232489, 1603300.25, 3203032, 1951881.625, 4.73E7, 1.38E7,
        3788569.75, 1.07E7, 3681628.25, 1.67E7, 7785902.5, 1838674, 2712168.25, 8430638, 7931655.5,
        1.17E9, 3966412, 8360896.5, 2754215.25, 3145022.5, 4.77E8, 3413058.25, 1.27E8, 2.50E7,
        4797151, 2.96E7, 1.25E7, 3256954.75, 1.16E7, 4922072.5, 2117249.25, 1911482.75, 1.06E7,
        9150988, 4641724.5, 1766348.125};
    final SimpleMassSpectrum spectrum = spec(mz, intensity);
    final DetectionResult r = engine(elements, 6).detect(spectrum, 734.4682617, 1.17E9,
        PolarityType.POSITIVE);
    assertNotNull(r, "the singly charged pattern must be detected");
    assertEquals(1, r.bestCharge(), "FT ringing must not be read as a high charge state");
    final IsotopePattern p = r.patterns().get(0);
    // the charge-1 13C ladder: mono + M+1..M+4
    for (final double peak : new double[]{734.4682617, 735.4714355, 736.4740601, 737.4765625,
        738.4807739}) {
      assertTrue(containsMz(p, peak), "charge-1 13C ladder peak missing: " + peak);
    }
    // the FT ringing peaks must not leak into the pattern
    assertTrue(!containsMz(p, 734.6674194), "FT ringing peak must not be kept");
  }

  @NotNull
  private static SimpleMassSpectrum spec271() {
    final double[] mz = {156.011322, 157.0148926, 158.0019379, 158.9614105, 185.1151581,
        186.9558868, 197.128067, 211.1442566, 223.0638275, 224.0632782, 270.9491577, 270.9867249,
        270.9968872, 271.0317383, 271.0447388, 271.05896, 271.0658569, 271.0754395, 271.0856323,
        271.0945129, 272.0345764, 273.0270081, 274.0304565, 275.0219727, 279.091095, 280.0950012,
        281.0850525, 292.9992981, 293.0137024, 294.0164795, 295.0093384, 297.081604, 301.0736389,
        308.9878235, 310.1071167, 310.9842224, 312.1229553, 314.9947815, 316.0888977, 344.1204834,
        351.0692139, 541.0565796, 542.0587769, 543.0533447, 544.0545654, 545.0509033, 560.0308838,
        563.0385132, 564.0407104, 565.0351563, 566.0376587, 567.031311, 571.0975952, 572.1005249,
        573.0957642, 579.0115967, 580.0142822, 581.006958, 585.0205688, 833.0628662, 834.0648193,
        835.059082, 836.0587769, 855.0447388, 864.9968262, 866.9995728};
    final double[] intensity = {1.48E7, 789644.4375, 1156482.25, 3.42E6, 866317.25, 657250.0625,
        6.53E5, 1734776.875, 5057024, 944177.75, 8.42E5, 1039276.875, 2.41E6, 5.67E8, 9487441,
        3945793.5, 5804221.5, 1.71E6, 1.01E6, 681984.25, 6.08E7, 4.91E7, 5070476.5, 1179668.625,
        1.51E7, 2.14E6, 7.86E5, 666107, 3.60E7, 3266284.75, 2527421, 1.10E6, 1.29E6, 6230270.5,
        6.62E5, 895285.8125, 1.15E6, 992463.375, 928929.5625, 624650.6875, 2201560.5, 4.77E7,
        1.13E7, 8947802, 2136723.25, 787367.3125, 842144.0625, 7.37E7, 1.84E7, 1.51E7, 3.60E6,
        1274502.125, 4.55E6, 1.30E6, 784020.0625, 3.77E6, 855760.125, 986559.375, 951464.1875,
        6.30E6, 2624961.25, 2350929, 779325.3125, 1055457.125, 865979.8125, 716015.625};
    return spec(mz, intensity);
  }

  @NotNull
  private static SimpleMassSpectrum spec330() {
    final double[] mz = {150.0912628, 151.0850067, 151.0876312, 151.0991211, 151.1042938,
        151.1136475, 152.102356, 153.105957, 165.011673, 165.5671234, 166.068924, 166.5649261,
        180.0167847, 180.0201569, 180.0350494, 180.049408, 180.0540009, 180.0590973, 181.0387421,
        182.0308533, 182.0400391, 182.0631561, 183.0344696, 213.1603241, 223.0637054, 245.1288757,
        279.1156311, 311.1386414, 312.1430359, 330.0666199, 330.0810242, 330.0899963, 330.1266785,
        330.1740112, 330.1868286, 331.1296692, 332.1206665, 333.1244507, 334.12854, 352.1084595,
        353.1124878, 681.2282104, 682.2317505, 683.2293091};
    final double[] intensity = {6.82E7, 9021375, 9275947, 1.51E9, 3.05E7, 1.07E7, 1.49E8, 8824702,
        9.86E7, 1.20E8, 1.95E7, 6707682.5, 1.49E7, 2.02E7, 2.59E9, 1.12E7, 1.82E7, 7.66E6, 2.07E8,
        1.15E8, 1.06E7, 2.97E7, 1.06E7, 8809208, 9.88E6, 1.88E7, 6.57E6, 4.37E7, 8.20E6, 9360763,
        3.08E7, 3.46E7, 4.82E9, 2.85E7, 1.35E7, 9.47E8, 1.98E8, 3.81E7, 6522436.5, 4.08E7, 7454330,
        1.26E8, 5.08E7, 1.94E7};
    return spec(mz, intensity);
  }

  /**
   * Build an engine matching the user's reported settings: elements H,C,N,O,S (no Cl), m/z
   * tolerance 0.009 Da / 25 ppm, max charge 10, signal (carbon-averagine) mode with default carbon
   * params, element auto-detection on. {@code requireC13} toggles the "require 13C isotope peak"
   * option.
   */
  @NotNull
  private static IsotopeFinderEngine userConfigEngine(final boolean requireC13) {
    final List<Element> els = List.of(new Element("H"), new Element("C"), new Element("N"),
        new Element("O"), new Element("S"));
    final MZTolerance tol = new MZTolerance(0.009, 25);
    final EnvelopeModel model = new CarbonAveragineEnvelopeModel(
        CarbonAveragineEnvelopeParameters.createDefault(), new EnvelopeContext(els, tol));
    return new IsotopeFinderEngine(els, 10, tol, model, "test", requireC13,
        ElementDetectionMode.AUTO_DETECT, ElementAutoDetector.DEFAULT_CANDIDATES);
  }

  @Test
  void userConfigDetectsChargeOneForMolecule271() {
    // exact user config (elements H,C,N,O,S; tol 0.009/25ppm; maxCharge 10; auto-detect on): the
    // singly charged m/z 271.0317 must be charge 1 both with and without "require 13C".
    assertEquals(1,
        userConfigEngine(false).detect(spec271(), 271.0317383, 5.67E8, PolarityType.POSITIVE)
            .bestCharge(), "271 without require-13C");
    final DetectionResult withGate = userConfigEngine(true).detect(spec271(), 271.0317383, 5.67E8,
        PolarityType.POSITIVE);
    assertNotNull(withGate, "271 must still be detected with require-13C");
    assertEquals(1, withGate.bestCharge(), "271 with require-13C");
  }

  @Test
  void userConfigDetectsChargeOneForMolecule330() {
    assertEquals(1,
        userConfigEngine(false).detect(spec330(), 330.1266785, 4.82E9, PolarityType.POSITIVE)
            .bestCharge(), "330 without require-13C");
    final DetectionResult withGate = userConfigEngine(true).detect(spec330(), 330.1266785, 4.82E9,
        PolarityType.POSITIVE);
    assertNotNull(withGate, "330 must still be detected with require-13C");
    assertEquals(1, withGate.bestCharge(), "330 with require-13C");
  }

  @Test
  void requireC13AcceptsCarbonPoorHeteroatomRichPattern() {
    // a carbon-poor, heteroatom-rich molecule (~11 C at 400 Da = 1 C per ~36 Da, below the 1/20-per-Da
    // averagine floor): mono 400, 13C M+1 at 401.003 with ratio ~0.13, and a 37Cl M+2 at 401.997. The
    // "require 13C" gate must NOT reject it just because its M+1 is below the (too high) carbon-min
    // prediction - the 13C M+1 is clearly present and plausible.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("Cl"));
    final double mono = 400.0;
    final SimpleMassSpectrum spectrum = spec(
        new double[]{mono, mono + C13, mono + 1.99705, mono + 2 * C13},
        new double[]{100d, 13d, 32d, 1.5d});
    final DetectionResult r = engineRequireC13(elements, 3).detect(spectrum, mono, 100d,
        PolarityType.POSITIVE);
    assertNotNull(r, "require-13C must accept a carbon-poor but valid 13C pattern");
    assertEquals(1, r.bestCharge(), "should be charge 1");
  }

  @Test
  void requireC13DetectsChargeOneForHeteroatomRichMolecule271() {
    // real measured spectrum: m/z 271.0317 (5.67e8) is the monoisotopic of a singly charged,
    // heteroatom-rich (Cl/S) compound: 13C M+1 at 272.0346, heavy M+2 at 273.027. With "require 13C"
    // enabled this must still be detected as charge 1 (the 13C M+1 is present and plausible).
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"), new Element("S"), new Element("Cl"));
    final DetectionResult r = engineRequireC13(elements, 6).detect(spec271(), 271.0317383, 5.67E8,
        PolarityType.POSITIVE);
    assertNotNull(r, "require-13C must not reject this valid singly charged 13C pattern");
    assertEquals(1, r.bestCharge(), "should be charge 1");
  }

  @Test
  void requireC13DetectsChargeOneForHeteroatomRichMolecule330() {
    // real measured spectrum: m/z 330.1267 (4.82e9) monoisotopic of a singly charged compound with a
    // clean 13C M+1 at 331.1297 (ratio ~0.20) and heavy M+2 at 332.1207. Must be charge 1 with
    // "require 13C" enabled.
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"), new Element("S"), new Element("Cl"));
    final DetectionResult r = engineRequireC13(elements, 6).detect(spec330(), 330.1266785, 4.82E9,
        PolarityType.POSITIVE);
    assertNotNull(r, "require-13C must not reject this valid singly charged 13C pattern");
    assertEquals(1, r.bestCharge(), "should be charge 1");
  }

  @Test
  void rejectsHighChargeStateWithoutEnoughSignals() {
    // misdetection guard: a high charge is only accepted with a charge-scaled minimum number of
    // distinct 13C-grid signals (z>=10 requires 5), so a couple of noise peaks that happen to fall on
    // the fine 1.00336/z grid are not read as a high charge state. Tight FT tolerance so neighbouring
    // high charges are otherwise distinguishable.
    final MZTolerance tightTol = new MZTolerance(0.0005, 2);
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("N"),
        new Element("O"));

    // a full, clean z=10 envelope has plenty of signals -> still detected as charge 10
    final SimpleMassSpectrum full = ladder(500.0, 10, 450, 8);
    final DetectionResult ok = engineTol(elements, 12, tightTol).detect(full, 500.0,
        full.getIntensityValue(0), PolarityType.POSITIVE);
    assertNotNull(ok, "a full high-charge envelope should still be detected");
    assertEquals(10, ok.bestCharge(), "a full 8-peak z=10 envelope must still be detected as z=10");

    // only the first 3 peaks of that same envelope -> below the z>=10 minimum of 5 -> not z=10
    final double[] mz = new double[3];
    final double[] in = new double[3];
    for (int i = 0; i < 3; i++) {
      mz[i] = full.getMzValue(i);
      in[i] = full.getIntensityValue(i);
    }
    final DetectionResult few = engineTol(elements, 12, tightTol).detect(spec(mz, in), 500.0, in[0],
        PolarityType.POSITIVE);
    assertTrue(few == null || few.bestCharge() != 10,
        "3 signals must not be reported as charge 10 (evidence floor guards against noise)");
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
        new Halo("C10Cl6", List.of(new Element("C"), br, cl)),
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
          DetectionResult r = engineAutoDetect(h.elements(), 6).detect(complex,
              target.getMzValue(s),
              target.getIntensityValue(s), PolarityType.POSITIVE);
          assertNotNull(r,
              "%s seed %d: no detection for charge %d, require 13C false".formatted(h.formula(), s,
                  expCharge));
          assertEquals(expCharge, r.bestCharge(), h.formula() + " seed " + s + ": wrong charge");

          final DetectionResult r13c = engineAutoDetectRequireC13(h.elements(), 6).detect(complex,
              target.getMzValue(s), target.getIntensityValue(s), PolarityType.POSITIVE);
          assertNotNull(r13c,
              "%s seed %d: no detection for charge %d, require 13C true".formatted(h.formula(), s,
                  expCharge));
          assertEquals(expCharge, r13c.bestCharge(), h.formula() + " seed " + s + ": wrong charge");
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

  /**
   * A real CDK tetrachloro pattern: a clear 37Cl M+2/M+4/M+6/M+8 comb (~1.997 Da spacing) that the
   * auto-detector should fire on.
   */
  @NotNull
  private static SimpleMassSpectrum chlorineComb() {
    return fromFormula("C10H6Cl4", 1);
  }

  @Test
  void autoDetectPopulatesChlorineComposition() {
    // opt-in auto-detection on a chlorinated pattern must report Cl in the detected composition
    final List<Element> elements = List.of(new Element("C"), new Element("H"));
    final SimpleMassSpectrum spectrum = chlorineComb();
    final double mono = spectrum.getMzValue(0);
    final DetectionResult r = engineAutoDetect(elements, 4).detect(spectrum, mono,
        spectrum.getIntensityValue(0), PolarityType.POSITIVE);
    assertNotNull(r);
    final DetectedComposition composition = r.detectedComposition();
    assertNotNull(composition, "auto-detect must populate the detected composition");
    assertTrue(composition.elements().contains("Cl"),
        "the 37Cl M+2 comb must be detected as chlorine but was " + composition.elements());
  }

  @Test
  void autoDetectDoesNotChangeCharge() {
    // enabling auto-detection must not alter charge selection (the heavy bound does not feed it)
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("Cl"));
    final SimpleMassSpectrum spectrum = chlorineComb();
    final double mono = spectrum.getMzValue(0);
    final DetectionResult userDefined = engine(elements, 2).detect(spectrum, mono,
        spectrum.getIntensityValue(0), PolarityType.POSITIVE);
    final DetectionResult autoDetect = engineAutoDetect(elements, 4).detect(spectrum, mono,
        spectrum.getIntensityValue(0), PolarityType.POSITIVE);
    assertNotNull(userDefined);
    assertNotNull(autoDetect);
    assertEquals(userDefined.bestCharge(), autoDetect.bestCharge(),
        "auto-detection must not change the winning charge");
  }

  @Test
  void elementDetectionIsOffByDefault() {
    // the default (USER_DEFINED) engine leaves the detected composition null
    final List<Element> elements = List.of(new Element("C"), new Element("H"), new Element("Cl"));
    final SimpleMassSpectrum spectrum = chlorineComb();
    final double mono = spectrum.getMzValue(0);
    final DetectionResult r = engine(elements, 2).detect(spectrum, mono,
        spectrum.getIntensityValue(0), PolarityType.POSITIVE);
    assertNotNull(r);
    org.junit.jupiter.api.Assertions.assertNull(r.detectedComposition(),
        "element detection must be off by default (USER_DEFINED)");
  }
}
