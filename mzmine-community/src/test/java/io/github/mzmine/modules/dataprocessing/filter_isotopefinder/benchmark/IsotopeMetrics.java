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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ChargeScore;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectedComposition;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.DetectionResult;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ElementAutoDetector;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure (JUnit-free) scoring logic for the isotope-finder benchmark: turns a {@link GroundTruthCase}
 * plus its engine {@code DetectionResult} into {@link CaseMetrics}, and aggregates a list of cases
 * into a {@link MetricRow} per axis.
 * <p>
 * All m/z matching uses the case tolerance ({@link GroundTruthCase#tol()}).
 */
public final class IsotopeMetrics {

  /**
   * Heavy elements the element metric is restricted to (both detected and true sets), matching the
   * {@link ElementAutoDetector} candidate set so precision/recall stay comparable.
   */
  private static final Set<String> INFERABLE_HEAVY = new LinkedHashSet<>(
      ElementAutoDetector.DEFAULT_CANDIDATES);

  private IsotopeMetrics() {
  }

  /**
   * Compute the per-case primitives for one benchmark case, seeded only from the base peak (the
   * start-signal-invariance flag is then trivially true). Used by the fast CI test.
   *
   * @param c        the ground-truth case
   * @param result   the engine detection, or {@code null} when the engine returned nothing
   * @param detectMs wall time of the detect call in milliseconds
   */
  @NotNull
  public static CaseMetrics computeCase(@NotNull final GroundTruthCase c,
      @Nullable final DetectionResult result, final double detectMs) {
    return computeCase(c, result, new int[]{result == null ? 0 : result.bestCharge()}, detectMs);
  }

  /**
   * Compute the per-case primitives for one benchmark case. Every metric except start-signal
   * invariance is derived from {@code result} (the base-peak-seeded detection);
   * {@code seedBestCharges} holds the winning charge obtained when the finder is seeded from each
   * tested start signal (monoisotopic / base / top peak) and drives
   * {@link CaseMetrics#chargeStartInvariant()}.
   *
   * @param c               the ground-truth case
   * @param result          the base-peak-seeded detection, or {@code null} when nothing was found
   * @param seedBestCharges winning charge from each start signal (0 = no detection)
   * @param detectMs        wall time of the base-peak detect call in milliseconds
   */
  @NotNull
  public static CaseMetrics computeCase(@NotNull final GroundTruthCase c,
      @Nullable final DetectionResult result, @NotNull final int[] seedBestCharges,
      final double detectMs) {
    final MZTolerance tol = c.tol();
    final int predictedCharge = result == null ? 0 : result.bestCharge();
    final boolean chargeTop1 = predictedCharge == c.trueCharge();

    final boolean chargeRecallAlt =
        result != null && result.scores().stream().anyMatch(s -> s.charge() == c.trueCharge());

    // best detected pattern (best-first); empty when there is no detection
    final IsotopePattern best =
        (result == null || result.patterns().isEmpty()) ? null : result.patterns().get(0);
    final double[] detected = patternMz(best);

    // pattern precision / recall / F1
    final int matchedDetected = countMatched(detected, c.trueOffsetsMz(), tol);
    final int matchedTrue = countMatched(c.trueOffsetsMz(), detected, tol);
    final double precision = detected.length == 0 ? 0d : (double) matchedDetected / detected.length;
    final double recall =
        c.trueOffsetsMz().length == 0 ? 0d : (double) matchedTrue / c.trueOffsetsMz().length;
    final double f1 =
        (precision + recall) == 0d ? 0d : 2d * precision * recall / (precision + recall);

    // borderline recall (excluded when the case has no borderline peaks)
    final Double borderlineRecall = c.borderlineOffsetsMz().length == 0 ? null
        : (double) countMatched(c.borderlineOffsetsMz(), detected, tol)
          / c.borderlineOffsetsMz().length;

    // noise leak (excluded when the case has no injected false peaks)
    final Double noiseLeak = c.falseOffsetsMz().length == 0 ? null
        : (double) countMatched(c.falseOffsetsMz(), detected, tol) / c.falseOffsetsMz().length;

    // heavy-element precision/recall via the auto-detector
    final Set<String> detectedHeavy = restrictHeavy(detectHeavyElements(best, tol));
    final Set<String> trueHeavy = restrictHeavy(c.trueHeavyElements());
    final Double[] elementPr = elementPrecisionRecall(detectedHeavy, trueHeavy);

    // score margin (only meaningful when there is a detection)
    final Double scoreMargin = result == null ? null : scoreMargin(result.scores(), c.trueCharge());

    final double winningScore =
        (result == null || result.scores().isEmpty()) ? 0d : result.scores().get(0).score();

    // start-signal invariance (position-agnostic property): every tested start signal must yield the
    // same winning charge, regardless of whether that charge is correct
    final boolean chargeStartInvariant = allEqual(seedBestCharges);

    return new CaseMetrics(c.axis(), c.trueCharge(), predictedCharge, chargeTop1, chargeRecallAlt,
        chargeStartInvariant, precision, recall, f1, borderlineRecall, noiseLeak, elementPr[0],
        elementPr[1], scoreMargin, winningScore, detectMs);
  }

  /**
   * Start signals to seed the finder from for the invariance check: the monoisotopic (lowest-m/z),
   * the base (most intense), and the top (highest-m/z) TRUE isotope peak, de-duplicated. Each entry
   * is {@code {mz, height}} with the height read from the spectrum. Falls back to the case seed
   * when the case has no true peaks.
   */
  @NotNull
  public static List<double[]> startSeeds(@NotNull final GroundTruthCase c) {
    final double[] trueMz = c.trueOffsetsMz();
    if (trueMz.length == 0) {
      return List.of(new double[]{c.seedMz(), c.seedHeight()});
    }
    final SimpleMassSpectrum spectrum = c.spectrum();
    final MZTolerance tol = c.tol();
    double monoMz = trueMz[0];
    double topMz = trueMz[0];
    double baseMz = trueMz[0];
    double baseHeight = -1d;
    for (final double m : trueMz) {
      monoMz = Math.min(monoMz, m);
      topMz = Math.max(topMz, m);
      final double h = heightAt(spectrum, m, tol);
      if (h > baseHeight) {
        baseHeight = h;
        baseMz = m;
      }
    }
    final Set<Double> chosen = new LinkedHashSet<>(List.of(monoMz, baseMz, topMz));
    final List<double[]> seeds = new ArrayList<>(chosen.size());
    for (final double mz : chosen) {
      // decision: keep a positive height so the finder does not treat a seed as empty
      seeds.add(new double[]{mz, Math.max(heightAt(spectrum, mz, tol), 1d)});
    }
    return seeds;
  }

  /**
   * Intensity of the spectrum peak closest to {@code mz} within tolerance, or 0 if none.
   */
  private static double heightAt(@NotNull final SimpleMassSpectrum s, final double mz,
      @NotNull final MZTolerance tol) {
    double bestErr = Double.POSITIVE_INFINITY;
    double height = 0d;
    for (int i = 0; i < s.getNumberOfDataPoints(); i++) {
      final double err = Math.abs(s.getMzValue(i) - mz);
      if (tol.checkWithinTolerance(s.getMzValue(i), mz) && err < bestErr) {
        bestErr = err;
        height = s.getIntensityValue(i);
      }
    }
    return height;
  }

  /**
   * @return whether all entries are equal (an empty or single-element array is trivially
   * invariant).
   */
  private static boolean allEqual(@NotNull final int[] values) {
    for (int i = 1; i < values.length; i++) {
      if (values[i] != values[0]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Aggregate a list of per-case metrics (all belonging to {@code axisLabel}) into a single
   * {@link MetricRow}. Boolean rates are means of 0/1; excludable metrics average only their
   * defined cases; {@code aucCharge} and any all-undefined metric are {@link Double#NaN}.
   */
  @NotNull
  public static MetricRow aggregate(@NotNull final String axisLabel,
      @NotNull final List<CaseMetrics> cases) {
    final int n = cases.size();
    final double chargeTop1 = meanBool(cases, CaseMetrics::chargeTop1);
    final double chargeRecallAlt = meanBool(cases, CaseMetrics::chargeRecallAlt);
    final double chargeStartInvariance = meanBool(cases, CaseMetrics::chargeStartInvariant);

    double sumP = 0d;
    double sumR = 0d;
    double sumF1 = 0d;
    for (final CaseMetrics m : cases) {
      sumP += m.patternPrecision();
      sumR += m.patternRecall();
      sumF1 += m.patternF1();
    }
    final double patternPrecision = n == 0 ? Double.NaN : sumP / n;
    final double patternRecall = n == 0 ? Double.NaN : sumR / n;
    final double patternF1 = n == 0 ? Double.NaN : sumF1 / n;

    final double borderlineRecall = meanNullable(cases, CaseMetrics::borderlineRecall);
    final double noiseLeak = meanNullable(cases, CaseMetrics::noiseLeak);
    final double elementPrecision = meanNullable(cases, CaseMetrics::elementPrecision);
    final double elementRecall = meanNullable(cases, CaseMetrics::elementRecall);
    final double scoreMargin = meanNullable(cases, CaseMetrics::scoreMargin);
    final double aucCharge = auc(cases);
    final double medianDetectMs = median(cases);

    return new MetricRow(axisLabel, n, chargeTop1, chargeRecallAlt, chargeStartInvariance,
        patternPrecision, patternRecall, patternF1, borderlineRecall, noiseLeak, elementPrecision,
        elementRecall, scoreMargin, aucCharge, medianDetectMs);
  }

  /**
   * Group cases by axis, produce one {@link MetricRow} per axis (sorted by axis name) plus a final
   * overall {@code ALL} row.
   */
  @NotNull
  public static List<MetricRow> aggregateByAxis(@NotNull final List<CaseMetrics> cases) {
    final Map<String, List<CaseMetrics>> byAxis = new LinkedHashMap<>();
    for (final CaseMetrics m : cases) {
      byAxis.computeIfAbsent(m.axis(), k -> new ArrayList<>()).add(m);
    }
    final List<String> axes = new ArrayList<>(byAxis.keySet());
    axes.sort(String::compareTo);

    final List<MetricRow> rows = new ArrayList<>();
    for (final String axis : axes) {
      rows.add(aggregate(axis, byAxis.get(axis)));
    }
    rows.add(aggregate("ALL", cases));
    return rows;
  }

  // ---- per-case helpers ----------------------------------------------------

  /**
   * The m/z of every point of the pattern, or an empty array if the pattern is null/empty.
   */
  @NotNull
  private static double[] patternMz(@Nullable final IsotopePattern p) {
    if (p == null) {
      return new double[0];
    }
    final int n = p.getNumberOfDataPoints();
    final double[] mz = new double[n];
    for (int i = 0; i < n; i++) {
      mz[i] = p.getMzValue(i);
    }
    return mz;
  }

  /**
   * Count how many values in {@code query} have at least one value in {@code reference} within
   * tolerance.
   */
  private static int countMatched(@NotNull final double[] query, @NotNull final double[] reference,
      @NotNull final MZTolerance tol) {
    int matched = 0;
    for (final double q : query) {
      for (final double r : reference) {
        if (tol.checkWithinTolerance(q, r)) {
          matched++;
          break;
        }
      }
    }
    return matched;
  }

  /**
   * best correct-charge score − best incorrect-charge score (each defaulting to 0 when no such
   * charge was scored).
   */
  private static double scoreMargin(@NotNull final List<ChargeScore> scores, final int trueCharge) {
    double bestCorrect = 0d;
    double bestIncorrect = 0d;
    for (final ChargeScore s : scores) {
      if (s.charge() == trueCharge) {
        bestCorrect = Math.max(bestCorrect, s.score());
      } else {
        bestIncorrect = Math.max(bestIncorrect, s.score());
      }
    }
    return bestCorrect - bestIncorrect;
  }

  /**
   * Heavy-element detector for the element metric: rebuilds the detected pattern's signals as
   * {@link DataPoint}s and delegates to {@link ElementAutoDetector}, which infers {Cl,Br,S,Si} from
   * the M+2 / M+1 / M+4 exact-mass defects and relative intensities.
   * <p>
   * assumption: neutral mass is estimated as {@code lowestMz * charge} (the metric only carries the
   * detected pattern); this only seeds the detector's carbon estimate, so a rough value suffices.
   */
  @NotNull
  static Set<String> detectHeavyElements(@Nullable final IsotopePattern p,
      @NotNull final MZTolerance tol) {
    if (p == null || p.getNumberOfDataPoints() == 0) {
      return Set.of();
    }
    final int n = p.getNumberOfDataPoints();
    final List<DataPoint> signals = new ArrayList<>(n);
    double lowestMz = Double.POSITIVE_INFINITY;
    for (int i = 0; i < n; i++) {
      signals.add(new SimpleDataPoint(p.getMzValue(i), p.getIntensityValue(i)));
      lowestMz = Math.min(lowestMz, p.getMzValue(i));
    }
    final int charge = Math.max(1, p.getCharge());
    final DetectedComposition composition = ElementAutoDetector.detect(signals, charge,
        lowestMz * charge, tol);
    return composition.elements();
  }

  /**
   * Restrict a ground-truth heavy-element set to those the stub can infer.
   */
  @NotNull
  private static Set<String> restrictHeavy(@NotNull final Set<String> heavy) {
    final Set<String> out = new LinkedHashSet<>();
    for (final String h : heavy) {
      if (INFERABLE_HEAVY.contains(h)) {
        out.add(h);
      }
    }
    return out;
  }

  /**
   * Precision/recall of the detected heavy-element set vs. the (restricted) true set. Either value
   * is {@code null} when its denominator is 0 (undefined -> excluded from the axis mean).
   *
   * @return {@code [precision, recall]}
   */
  @NotNull
  private static Double[] elementPrecisionRecall(@NotNull final Set<String> detected,
      @NotNull final Set<String> trueSet) {
    int tp = 0;
    for (final String d : detected) {
      if (trueSet.contains(d)) {
        tp++;
      }
    }
    final int fp = detected.size() - tp;
    final int fn = trueSet.size() - tp;
    final Double precision = (tp + fp) == 0 ? null : (double) tp / (tp + fp);
    final Double recall = (tp + fn) == 0 ? null : (double) tp / (tp + fn);
    return new Double[]{precision, recall};
  }

  // ---- aggregation helpers -------------------------------------------------

  private static double meanBool(@NotNull final List<CaseMetrics> cases,
      @NotNull final java.util.function.Predicate<CaseMetrics> flag) {
    if (cases.isEmpty()) {
      return Double.NaN;
    }
    int hits = 0;
    for (final CaseMetrics m : cases) {
      if (flag.test(m)) {
        hits++;
      }
    }
    return (double) hits / cases.size();
  }

  private static double meanNullable(@NotNull final List<CaseMetrics> cases,
      @NotNull final java.util.function.Function<CaseMetrics, Double> extractor) {
    double sum = 0d;
    int count = 0;
    for (final CaseMetrics m : cases) {
      final Double v = extractor.apply(m);
      if (v != null) {
        sum += v;
        count++;
      }
    }
    return count == 0 ? Double.NaN : sum / count;
  }

  /**
   * Separation AUC (Mann-Whitney): fraction of (correct-charge, incorrect-charge) case pairs where
   * the correct case's winning score exceeds the incorrect case's (ties count 0.5). {@code NaN}
   * when either group is empty.
   */
  private static double auc(@NotNull final List<CaseMetrics> cases) {
    final List<Double> correct = new ArrayList<>();
    final List<Double> incorrect = new ArrayList<>();
    for (final CaseMetrics m : cases) {
      if (m.chargeTop1()) {
        correct.add(m.winningScore());
      } else {
        incorrect.add(m.winningScore());
      }
    }
    if (correct.isEmpty() || incorrect.isEmpty()) {
      return Double.NaN;
    }
    double wins = 0d;
    for (final double cScore : correct) {
      for (final double iScore : incorrect) {
        if (cScore > iScore) {
          wins += 1d;
        } else if (cScore == iScore) {
          wins += 0.5d;
        }
      }
    }
    return wins / ((double) correct.size() * incorrect.size());
  }

  private static double median(@NotNull final List<CaseMetrics> cases) {
    if (cases.isEmpty()) {
      return Double.NaN;
    }
    final double[] times = new double[cases.size()];
    for (int i = 0; i < cases.size(); i++) {
      times[i] = cases.get(i).detectMs();
    }
    java.util.Arrays.sort(times);
    final int mid = times.length / 2;
    return times.length % 2 == 1 ? times[mid] : (times[mid - 1] + times[mid]) / 2d;
  }
}
