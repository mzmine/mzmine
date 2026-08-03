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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Every neutral-mass deviation from the exact 13C grid that the candidate elements' isotopes can
 * produce, for any COMBINATION of up to N substitutions. Used to decide whether an off-grid signal
 * is explainable by the configured chemistry at all - see the emitted-pattern filter in
 * {@link IsotopeFinderEngine}.
 * <p>
 * decision: combinations, not just multiples of one isotope. Each substituted atom adds its own
 * defect, so a Cl&#8322; comb sits at twice the 37Cl defect - but a mixed 37Cl+34S signal sits at
 * their sum, which is no multiple of either. Restricting the table to multiples of a single isotope
 * measurably truncated real patterns (polyhalogen {@code patternRecall} 0.9934 &rarr; 0.9860), so
 * the reachable sums are enumerated level by level, deduplicated on a fine grid to keep each level
 * small.
 * <p>
 * The table is built once per engine and searched per candidate signal of every charge hypothesis,
 * so the sums are pre-expanded and sorted: a lookup is a binary search plus a short walk over the
 * entries inside the window.
 */
public final class IsotopeDefectTable {

  // two sums closer than this are the same entry: far below the attribution window, so dedup cannot
  // merge distinguishable defects, but coarse enough to keep each level a few hundred entries.
  private static final double DEDUP_GRID = 1e-5;

  /**
   * The table depends only on the candidate elements and the level cap - both fixed chemistry - while
   * an engine is built per feature list (and, in the benchmark, per case). Caching it keeps the
   * combination expansion a one-off.
   * <p>
   * decision: a plain {@link ConcurrentHashMap} rather than a Caffeine cache. The key space is
   * bounded by the user's configuration - a handful of distinct element sets per session, with the
   * substitution cap fixed by the caller - and one entry is a few tens of kB, so there is nothing for
   * an eviction policy to do; {@code computeIfAbsent} additionally guarantees the (expensive)
   * expansion runs once per key. Switch to Caffeine with a {@code maximumSize} only if a caller ever
   * starts deriving element sets per feature.
   */
  private static final Map<String, IsotopeDefectTable> CACHE = new ConcurrentHashMap<>();

  // reachable deviation sums, sorted ascending, with the smallest number of substitutions that
  // reaches each one in the parallel array
  private final double[] deviations;
  private final int[] substitutions;

  private IsotopeDefectTable(final double[] deviations, final int[] substitutions) {
    this.deviations = deviations;
    this.substitutions = substitutions;
  }

  /**
   * @param candidates       element symbols whose isotopes may explain a signal.
   * @param maxSubstitutions highest number of combined substitutions to pre-expand (&ge; 1).
   * @return the searchable table, cached per {@code (candidates, maxSubstitutions)}.
   */
  public static @NotNull IsotopeDefectTable build(@NotNull final List<String> candidates,
      final int maxSubstitutions) {
    // the table content is independent of the candidate ORDER (the reachable sums are merged,
    // deduplicated and sorted), so the key is built from the SORTED symbols: two searches that
    // declare the same elements in a different order then share one table instead of expanding it
    // twice. Joined explicitly rather than via List#toString so the key stays a stable format.
    final List<String> keySymbols = new ArrayList<>(candidates);
    Collections.sort(keySymbols);
    final String key = String.join(",", keySymbols) + "/" + maxSubstitutions;
    return CACHE.computeIfAbsent(key, _ -> expand(candidates, maxSubstitutions));
  }

  private static @NotNull IsotopeDefectTable expand(@NotNull final List<String> candidates,
      final int maxSubstitutions) {
    final double[] single = ElementAutoDetector.isotopeGridDeviations(candidates);
    final int levels = Math.max(1, maxSubstitutions);
    // level n holds every sum reachable with exactly n substitutions; level n+1 extends each of them
    // by one more isotope. Both the level and the accumulated table are kept sorted and deduplicated,
    // so sizes stay bounded by the reachable range rather than growing combinatorially, and the
    // accumulated table needs no final sort.
    double[] level = dedup(single);
    double[] mergedDevs = new double[0];
    int[] mergedCounts = new int[0];
    for (int n = 1; n <= levels && level.length > 0; n++) {
      final double[] nextDevs = new double[mergedDevs.length + level.length];
      final int[] nextCounts = new int[nextDevs.length];
      int at = 0;
      int i = 0;
      int j = 0;
      while (i < mergedDevs.length || j < level.length) {
        final boolean takeMerged = j >= level.length || (i < mergedDevs.length
            && mergedDevs[i] <= level[j]);
        final double value = takeMerged ? mergedDevs[i] : level[j];
        final int count = takeMerged ? mergedCounts[i] : n;
        if (takeMerged) {
          i++;
        } else {
          j++;
        }
        if (at == 0 || value - nextDevs[at - 1] > DEDUP_GRID) {
          nextDevs[at] = value;
          nextCounts[at] = count;
          at++;
        } else {
          // same deviation (within the dedup grid) as the entry just kept: it must carry the SMALLEST
          // number of substitutions that reaches it, otherwise a defect reachable with one atom can
          // inherit the count of a near-identical multi-atom sum and be rejected at low offsets
          nextCounts[at - 1] = Math.min(nextCounts[at - 1], count);
        }
      }
      mergedDevs = Arrays.copyOf(nextDevs, at);
      mergedCounts = Arrays.copyOf(nextCounts, at);
      if (n == levels) {
        break;
      }
      final double[] next = new double[level.length * single.length];
      int k = 0;
      for (final double sum : level) {
        for (final double dev : single) {
          next[k++] = sum + dev;
        }
      }
      level = dedup(next);
    }
    return new IsotopeDefectTable(mergedDevs, mergedCounts);
  }

  /**
   * @return the values sorted ascending with near-duplicates (within {@link #DEDUP_GRID}) removed.
   */
  private static double @NotNull [] dedup(final double @NotNull [] values) {
    final double[] sorted = values.clone();
    Arrays.sort(sorted);
    final DoubleArrayList out = new DoubleArrayList(sorted.length);
    for (final double v : sorted) {
      if (out.isEmpty() || v - out.getDouble(out.size() - 1) > DEDUP_GRID) {
        out.add(v);
      }
    }
    return out.toDoubleArray();
  }

  /**
   * @param deviation        signed neutral-mass deviation of the signal from the nearest exact 13C
   *                         grid position (Da).
   * @param window           maximum accepted |deviation - table entry| (Da).
   * @param maxSubstitutions highest multiplicity the signal's offset can hold; entries needing more
   *                         substitutions than this are ignored.
   * @return whether some entry within the window explains the deviation.
   */
  public boolean explains(final double deviation, final double window, final int maxSubstitutions) {
    if (deviations.length == 0) {
      return false;
    }
    int from = Arrays.binarySearch(deviations, deviation - window);
    if (from < 0) {
      from = -from - 1; // first entry >= deviation - window
    }
    for (int i = from; i < deviations.length && deviations[i] <= deviation + window; i++) {
      if (substitutions[i] <= maxSubstitutions) {
        return true;
      }
    }
    return false;
  }
}
