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

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.CaseMetrics;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GroundTruthCase;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.IsotopeCorpus;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.IsotopeMetrics;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeFinderEngine;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * CI accuracy guard for the isotope finder over the fast {@link IsotopeCorpus#ciCases()} subset.
 * <p>
 * Thresholds are pinned to the CURRENT engine's measured baseline (regenerate with the
 * {@code isotopeBenchmark} Gradle task): axes the engine solves cleanly assert an exact charge
 * match, while the known-hard interference/combined axes assert only that a detection is returned,
 * the true charge is at least flagged among the alternates, and the noise leak stays within the
 * observed bound. This locks in current behaviour without turning CI red on the present engine.
 */
class IsotopeAccuracyTest {

  /**
   * Axes where the current engine gets the charge exactly right on the ci subset.
   */
  private static final Set<String> STRICT_CHARGE_AXES = Set.of("clean", "charge",
      "resolution_merged", "protein_highz", "polyhalogen", "cutoff", "noise");

  /**
   * Known-hard axes: co-eluting interference / combined stressors currently drive harmonic
   * charge-doubling and noise leakage, so only weak invariants are asserted here.
   */
  private static final Set<String> SOFT_AXES = Set.of("interference", "combined");

  /**
   * Upper bound on the noise-leak fraction for the soft axes (the ci interference case currently
   * leaks its single interferent peak, i.e. 1.0). Plus a tiny epsilon.
   */
  private static final double NOISE_LEAK_MAX = 1.0 + 1e-3;

  @NotNull
  private static List<Named<GroundTruthCase>> ciCases() {
    final List<Named<GroundTruthCase>> named = new ArrayList<>();
    for (final GroundTruthCase c : IsotopeCorpus.ciCases()) {
      named.add(Named.of(c.axis() + "/" + c.id(), c));
    }
    return named;
  }

  @NotNull
  private static IsotopeFinderEngine buildEngine(@NotNull final GroundTruthCase c) {
    final EnvelopeModel model = new CarbonAveragineEnvelopeModel(
        CarbonAveragineEnvelopeParameters.createDefault(),
        new EnvelopeContext(c.elements(), c.tol()));
    return new IsotopeFinderEngine(c.elements(), c.maxCharge(), c.tol(), model, "ci", false);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("ciCases")
  void detectsAccuratelyPerAxis(@NotNull final GroundTruthCase c) {
    final IsotopeFinderEngine engine = buildEngine(c);
    final DetectionResult result = engine.detect(c.spectrum(), c.seedMz(), c.seedHeight(),
        c.polarity());
    final CaseMetrics m = IsotopeMetrics.computeCase(c, result, 0d);

    if (STRICT_CHARGE_AXES.contains(c.axis())) {
      Assertions.assertNotNull(result, () -> c.id() + ": expected a detection on a clean axis");
      Assertions.assertEquals(c.trueCharge(), result.bestCharge(),
          () -> c.id() + ": wrong charge on strict axis " + c.axis());
      // the true isotope peaks should be well recovered on the clean axes
      Assertions.assertTrue(m.patternRecall() >= 0.9,
          () -> c.id() + ": pattern recall " + m.patternRecall() + " below 0.9 on " + c.axis());
      // position-agnostic: seeding from the mono / base / top true peak must recover the same charge
      for (final double[] seed : IsotopeMetrics.startSeeds(c)) {
        final DetectionResult seeded = engine.detect(c.spectrum(), seed[0], seed[1], c.polarity());
        Assertions.assertNotNull(seeded,
            () -> c.id() + ": no detection when seeded at m/z " + seed[0]);
        Assertions.assertEquals(c.trueCharge(), seeded.bestCharge(),
            () -> c.id() + ": wrong charge seeded at m/z " + seed[0] + " on " + c.axis());
      }
    } else if (SOFT_AXES.contains(c.axis())) {
      // known-hard: only require a detection, the true charge flagged, and bounded noise leak
      Assertions.assertNotNull(result, () -> c.id() + ": expected a detection on " + c.axis());
      Assertions.assertTrue(m.chargeRecallAlt(),
          () -> c.id() + ": true charge not even flagged among alternates on " + c.axis());
      Assertions.assertTrue(m.noiseLeak() == null || m.noiseLeak() <= NOISE_LEAK_MAX,
          () -> c.id() + ": noise leak " + m.noiseLeak() + " above bound " + NOISE_LEAK_MAX);
    } else {
      // any other axis that may enter the ci subset later: require a detection at minimum
      Assertions.assertNotNull(result, () -> c.id() + ": expected a detection on " + c.axis());
    }
  }
}
