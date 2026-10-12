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
 * Every neutral-mass deviation from the exact 13C grid the candidate elements' isotopes can produce,
 * for any COMBINATION of up to N substitutions. Backs the emitted-signal filter in
 * {@link ExplainableSignalFilter}.
 * <p>
 * decision: combinations, not just multiples of one isotope. A mixed 37Cl+34S signal sits at the SUM
 * of two defects, which is no multiple of either, and restricting the table to multiples measurably
 * truncated real patterns (polyhalogen {@code patternRecall} 0.9934 &rarr; 0.9860).
 * <p>
 * Built once per element set and searched per candidate signal of every charge hypothesis, so the
 * sums are pre-expanded and sorted and a lookup is a binary search plus a short walk.
 */
public final class IsotopeDefectTable {

  // two sums closer than this are one entry: far below the attribution window, so dedup cannot merge
  // distinguishable defects, yet coarse enough to keep each level a few hundred entries
  private static final double DEDUP_GRID = 1e-5;

  /**
   * decision: a plain {@link ConcurrentHashMap}, not a Caffeine cache. The key space is bounded by
   * the user's configuration - a handful of element sets per session at a few tens of kB each - so an
   * eviction policy would have nothing to do, and {@code computeIfAbsent} already guarantees the
   * expensive expansion runs once per key. Revisit only if callers start deriving element sets per
   * feature.
   */
  private static final Map<String, IsotopeDefectTable> CACHE = new ConcurrentHashMap<>();

  // sorted ascending; the parallel array holds the FEWEST substitutions reaching each sum
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
    // the content is order-independent, so keying on the SORTED symbols lets two searches that
    // declare the same elements in a different order share one table
    final List<String> keySymbols = new ArrayList<>(candidates);
    Collections.sort(keySymbols);
    final String key = String.join(",", keySymbols) + "/" + maxSubstitutions;
    return CACHE.computeIfAbsent(key, _ -> expand(candidates, maxSubstitutions));
  }

  private static @NotNull IsotopeDefectTable expand(@NotNull final List<String> candidates,
      final int maxSubstitutions) {
    final double[] single = ElementAutoDetector.isotopeGridDeviations(candidates);
    final int levels = Math.max(1, maxSubstitutions);
    // level n holds every sum reachable with exactly n substitutions and level n+1 extends each by
    // one isotope. Keeping both sorted and deduplicated bounds the size by the reachable RANGE rather
    // than letting it grow combinatorially, and leaves the accumulated table needing no final sort.
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
          // must keep the SMALLEST count, or a defect reachable with one atom inherits the count of
          // a near-identical multi-atom sum and is then wrongly rejected at low offsets
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
   * @param deviation        signed neutral-mass deviation from the nearest exact 13C grid position.
   * @param window           maximum accepted |deviation - table entry| (Da).
   * @param maxSubstitutions highest multiplicity this signal's offset can hold.
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
