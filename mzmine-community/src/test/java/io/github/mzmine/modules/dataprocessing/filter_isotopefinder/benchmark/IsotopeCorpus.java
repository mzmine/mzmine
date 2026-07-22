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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Thin accessor over the benchmark corpus for tests.
 */
public final class IsotopeCorpus {

  private IsotopeCorpus() {
  }

  /**
   * All benchmark cases from the committed corpus.
   */
  @NotNull
  public static List<GroundTruthCase> all() {
    return BenchmarkCorpusLoader.load();
  }

  /**
   * All cases whose dominant stressor equals {@code axis}.
   */
  @NotNull
  public static List<GroundTruthCase> byAxis(@NotNull final String axis) {
    final List<GroundTruthCase> out = new ArrayList<>();
    for (final GroundTruthCase c : all()) {
      if (c.axis().equals(axis)) {
        out.add(c);
      }
    }
    return out;
  }

  /**
   * A small, fast subset for CI: one clean case per molecule class (keyed by max charge, which is
   * unique per class) plus the first case of each of a few stressor axes.
   */
  @NotNull
  public static List<GroundTruthCase> ciCases() {
    final List<GroundTruthCase> all = all();
    final Map<String, GroundTruthCase> picked = new LinkedHashMap<>();

    // one clean/structural baseline per molecule class (maxCharge distinguishes SMALL/PEPTIDE/PROTEIN)
    final Set<String> structural = Set.of("clean", "charge", "polyhalogen", "protein_highz");
    final Map<Integer, GroundTruthCase> perClass = new LinkedHashMap<>();
    for (final GroundTruthCase c : all) {
      if (structural.contains(c.axis())) {
        perClass.putIfAbsent(c.maxCharge(), c);
      }
    }
    for (final GroundTruthCase c : perClass.values()) {
      picked.putIfAbsent(c.id(), c);
    }

    // a couple of stressors
    for (final String axis : List.of("resolution_merged", "cutoff", "noise", "interference",
        "unit_resolution")) {
      for (final GroundTruthCase c : all) {
        if (c.axis().equals(axis)) {
          picked.putIfAbsent(c.id(), c);
          break;
        }
      }
    }
    return new ArrayList<>(picked.values());
  }
}
