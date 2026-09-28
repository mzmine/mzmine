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

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Builders and seeded degradation ops for the benchmark corpus' synthetic spectra. The clean
 * builders mirror the private helpers in {@code IsotopeFinderEngineTest}; the degradation ops add
 * realistic stressors. All randomness comes from a caller-supplied {@link RandomGenerator} - never
 * {@code Math.random()}, which would make the corpus non-reproducible.
 */
public final class SyntheticSpectra {

  private static final double C13 = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE;

  /**
   * Injected noise stays this multiple of the tolerance away from real (and other injected) peaks,
   * so it can never land inside a true peak's window whatever resolution the case is scored at: ~10
   * mDa at high resolution, ~0.4 Da at unit resolution.
   */
  private static final double NOISE_MIN_SEPARATION_TOL_FACTOR = 2.0;

  private SyntheticSpectra() {
  }

  /** A clean 13C ladder: Poisson envelope, base peak 100. */
  @NotNull
  public static SimpleMassSpectrum ladder(final double monoMz, final int charge, final int nCarbons,
      final int nPeaks) {
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

  @NotNull
  public static SimpleMassSpectrum spec(@NotNull final double[] mz,
      @NotNull final double[] intensity) {
    return new SimpleMassSpectrum(mz, intensity);
  }

  /**
   * Rescale a charge-1 spectrum to {@code charge}, avoiding a second (expensive) CDK enumeration:
   * the isotopologue distribution is charge-invariant, as CDK merges it in neutral-mass space and
   * only the final m/z depends on charge. Matches {@link IsotopePatternCalculator}'s electron-loss
   * convention exactly; the transform is monotonic, so ascending m/z order survives.
   *
   * @param polaritySign +1 for positive, -1 for negative mode
   */
  @NotNull
  public static SimpleMassSpectrum atCharge(@NotNull final SimpleMassSpectrum charge1,
      final int charge, final int polaritySign) {
    if (charge <= 1) {
      return charge1;
    }
    final double shift =
        polaritySign * IsotopePatternCalculator.ELECTRON_MASS * (charge - 1) / (double) charge;
    final int n = charge1.getNumberOfDataPoints();
    final double[] mz = new double[n];
    final double[] in = new double[n];
    for (int i = 0; i < n; i++) {
      mz[i] = charge1.getMzValue(i) / charge - shift;
      in[i] = charge1.getIntensityValue(i);
    }
    return new SimpleMassSpectrum(mz, in);
  }

  /**
   * Real CDK distribution at the default minimum abundance (0.001). A small merge width keeps fine
   * structure resolved, a large one merges it into one peak per nominal mass.
   */
  @NotNull
  public static SimpleMassSpectrum fromFormula(@NotNull final String formula, final int charge,
      final double mergeWidth) {
    return fromFormula(formula, charge, mergeWidth, 0.001);
  }

  /**
   * As above with an explicit {@code minAbundance}, which prunes the tail to keep large molecules
   * fast.
   */
  @NotNull
  public static SimpleMassSpectrum fromFormula(@NotNull final String formula, final int charge,
      final double mergeWidth, final double minAbundance) {
    final IsotopePattern p = IsotopePatternCalculator.calculateIsotopePattern(formula, minAbundance,
        mergeWidth, charge, PolarityType.POSITIVE, false);
    return mergeToSpectrum(p);
  }

  /**
   * Real CDK distribution with the fine structure collapsed to one peak per nominal offset.
   */
  @NotNull
  public static SimpleMassSpectrum cdkSpectrum(@NotNull final String formula, final int charge,
      final double minAbundance) {
    // < isotope spacing -> one peak per nominal offset
    final double mergeWidth = 0.01 * C13 / charge;
    final IsotopePattern p = IsotopePatternCalculator.calculateIsotopePattern(formula, minAbundance,
        mergeWidth, charge, PolarityType.POSITIVE, false);
    return mergeToSpectrum(p);
  }

  @NotNull
  private static SimpleMassSpectrum mergeToSpectrum(@NotNull final IsotopePattern p) {
    final TreeMap<Double, Double> map = new TreeMap<>();
    for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
      map.merge(p.getMzValue(i), p.getIntensityValue(i), Double::sum);
    }
    return fromSortedMap(map);
  }

  /** Merge spectra, summing peaks within ~1 mDa so overlaps add up. */
  @NotNull
  public static SimpleMassSpectrum combine(@NotNull final SimpleMassSpectrum... specs) {
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
    return fromSortedMap(map);
  }

  @NotNull
  private static SimpleMassSpectrum fromSortedMap(@NotNull final TreeMap<Double, Double> map) {
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

  public static int baseIndex(@NotNull final MassSpectrum s) {
    int idx = 0;
    for (int i = 1; i < s.getNumberOfDataPoints(); i++) {
      if (s.getIntensityValue(i) > s.getIntensityValue(idx)) {
        idx = i;
      }
    }
    return idx;
  }

  public static double baseHeight(@NotNull final MassSpectrum s) {
    return s.getNumberOfDataPoints() == 0 ? 0d : s.getIntensityValue(baseIndex(s));
  }

  /**
   * Model a unit-resolution readout of an already-collapsed spectrum: jitter each centroid (the
   * instrument's poor mass accuracy) and round to one decimal (its coarse reported precision). The
   * result is re-sorted, since a large jitter can reorder adjacent peaks.
   *
   * @param s           a collapsed spectrum, one peak per nominal isotope offset
   * @param jitterMaxDa maximum absolute jitter per peak; 0 = coarse rounding only
   */
  @NotNull
  public static SimpleMassSpectrum quantizeUnitResolution(@NotNull final SimpleMassSpectrum s,
      final double jitterMaxDa, @NotNull final RandomGenerator rnd) {
    final int n = s.getNumberOfDataPoints();
    final double[] mz = new double[n];
    final double[] in = new double[n];
    for (int i = 0; i < n; i++) {
      final double jitter = jitterMaxDa <= 0d ? 0d : (2d * rnd.nextDouble() - 1d) * jitterMaxDa;
      // unit mass resolution reports at most one decimal figure
      mz[i] = Math.round((s.getMzValue(i) + jitter) * 10d) / 10d;
      in[i] = s.getIntensityValue(i);
    }
    final double[] reordered = reorderByMz(mz, in);
    return new SimpleMassSpectrum(sortedCopy(mz), reordered);
  }

  /** Scale intensities, to place an interferent at a realistic relative abundance. */
  @NotNull
  public static SimpleMassSpectrum scale(@NotNull final SimpleMassSpectrum s, final double factor) {
    final int n = s.getNumberOfDataPoints();
    final double[] mz = new double[n];
    final double[] in = new double[n];
    for (int i = 0; i < n; i++) {
      mz[i] = s.getMzValue(i);
      in[i] = s.getIntensityValue(i) * factor;
    }
    return new SimpleMassSpectrum(mz, in);
  }

  /** Shift every m/z, to place an interferent away from the target. */
  @NotNull
  public static SimpleMassSpectrum shift(@NotNull final SimpleMassSpectrum s, final double dmz) {
    final double[] mz = new double[s.getNumberOfDataPoints()];
    final double[] in = new double[mz.length];
    for (int i = 0; i < mz.length; i++) {
      mz[i] = s.getMzValue(i) + dmz;
      in[i] = s.getIntensityValue(i);
    }
    return new SimpleMassSpectrum(mz, in);
  }

  @NotNull
  public static double[] mzArray(@NotNull final MassSpectrum s) {
    final double[] a = new double[s.getNumberOfDataPoints()];
    for (int i = 0; i < a.length; i++) {
      a[i] = s.getMzValue(i);
    }
    return a;
  }

  @NotNull
  public static double[] intensityArray(@NotNull final MassSpectrum s) {
    final double[] a = new double[s.getNumberOfDataPoints()];
    for (int i = 0; i < a.length; i++) {
      a[i] = s.getIntensityValue(i);
    }
    return a;
  }

  /** Drop peaks below {@code cutoffFraction * maxIntensity}; 0.0 keeps every peak. */
  @NotNull
  public static SimpleMassSpectrum applyIntensityCutoff(@NotNull final SimpleMassSpectrum s,
      final double cutoffFraction) {
    final double threshold = cutoffFraction * baseHeight(s);
    int kept = 0;
    for (int i = 0; i < s.getNumberOfDataPoints(); i++) {
      if (s.getIntensityValue(i) >= threshold) {
        kept++;
      }
    }
    final double[] mz = new double[kept];
    final double[] in = new double[kept];
    int k = 0;
    for (int i = 0; i < s.getNumberOfDataPoints(); i++) {
      if (s.getIntensityValue(i) >= threshold) {
        mz[k] = s.getMzValue(i);
        in[k] = s.getIntensityValue(i);
        k++;
      }
    }
    return new SimpleMassSpectrum(mz, in);
  }

  /**
   * Add {@code nNoise} random peaks in {@code [mzWindowLo, mzWindowHi]}, each kept a
   * tolerance-scaled distance ({@link #NOISE_MIN_SEPARATION_TOL_FACTOR}) from existing and
   * previously injected peaks, so noise never lands inside a true peak's window at any resolution.
   *
   * @param tol the m/z tolerance the case will be scored with (drives the min separation)
   */
  @NotNull
  public static InjectionResult addRandomNoise(@NotNull final SimpleMassSpectrum s,
      final int nNoise, final double maxRelIntensity, final double mzWindowLo,
      final double mzWindowHi, @NotNull final MZTolerance tol, @NotNull final RandomGenerator rnd) {
    final double base = baseHeight(s);
    final double[] noiseMz = new double[nNoise];
    final double[] noiseIn = new double[nNoise];
    int added = 0;
    for (int n = 0; n < nNoise; n++) {
      double mz = 0d;
      boolean placed = false;
      // decision: resample a few times to avoid overlapping a real or already-added noise peak
      for (int attempt = 0; attempt < 40 && !placed; attempt++) {
        mz = mzWindowLo + rnd.nextDouble() * (mzWindowHi - mzWindowLo);
        placed = isFarEnough(s, noiseMz, added, mz, tol);
      }
      // decision: DROP an unplaceable peak instead of adding it anyway. Adding it would put a
      // "noise" peak inside a true peak's tolerance window, where it is recorded as a false peak -
      // so correctly detecting the REAL peak there would be scored as a noise leak, inflating the
      // metric with cases the engine cannot possibly win. A crowded window simply yields fewer
      // noise peaks, which the ground truth records faithfully.
      if (!placed) {
        continue;
      }
      noiseMz[added] = mz;
      noiseIn[added] = rnd.nextDouble() * (maxRelIntensity * base);
      added++;
    }
    // only the successfully placed prefix is real noise; the unused tail would otherwise inject
    // spurious peaks at m/z 0 and claim them as ground-truth false positives
    final double[] placedMz = Arrays.copyOf(noiseMz, added);
    final double[] placedIn = Arrays.copyOf(noiseIn, added);
    final SimpleMassSpectrum noise = new SimpleMassSpectrum(sortedCopy(placedMz),
        reorderByMz(placedMz, placedIn));
    final SimpleMassSpectrum combined = combine(s, noise);
    return new InjectionResult(combined, placedMz);
  }

  private static boolean isFarEnough(@NotNull final SimpleMassSpectrum s,
      @NotNull final double[] noiseMz, final int added, final double mz,
      @NotNull final MZTolerance tol) {
    final double minSeparation = NOISE_MIN_SEPARATION_TOL_FACTOR * tol.getMzToleranceForMass(mz);
    for (int i = 0; i < s.getNumberOfDataPoints(); i++) {
      if (Math.abs(s.getMzValue(i) - mz) < minSeparation) {
        return false;
      }
    }
    for (int i = 0; i < added; i++) {
      if (Math.abs(noiseMz[i] - mz) < minSeparation) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  private static double[] sortedCopy(@NotNull final double[] mz) {
    final double[] copy = mz.clone();
    Arrays.sort(copy);
    return copy;
  }

  @NotNull
  private static double[] reorderByMz(@NotNull final double[] mz,
      @NotNull final double[] intensity) {
    final Integer[] order = new Integer[mz.length];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    Arrays.sort(order, (a, b) -> Double.compare(mz[a], mz[b]));
    final double[] out = new double[intensity.length];
    for (int i = 0; i < out.length; i++) {
      out[i] = intensity[order[i]];
    }
    return out;
  }

  /**
   * Add an interferent (a co-eluting decoy) to the target spectrum. The decoy's peaks are the
   * interference. Returns the combined spectrum plus the injected (decoy) m/z values.
   */
  @NotNull
  public static InjectionResult addInterference(@NotNull final SimpleMassSpectrum target,
      @NotNull final SimpleMassSpectrum decoyShifted) {
    final double[] injected = mzArray(decoyShifted);
    final SimpleMassSpectrum combined = combine(target, decoyShifted);
    return new InjectionResult(combined, injected);
  }
}
