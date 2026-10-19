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

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GenerationConfig;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GenerationConfig.SweepVariant;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GroundTruthCase;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.IsotopeCorpus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Guards the append-only sweep contract of the benchmark corpus.
 * <p>
 * Each sweep variant's INDEX is baked into every case id, and the id seeds every degradation applied
 * to that case. Deleting or reordering a variant therefore silently changes cases that already exist
 * in the committed corpus and invalidates the committed baselines - a failure that would not show up
 * as a compile error and is easy to introduce while tidying up. These assertions make it show up as
 * a red test instead.
 */
class BenchmarkSweepContractTest {

  /**
   * Slots of the two retired adversarial interference variants (see {@link GenerationConfig}). They
   * must stay in place and stay retired.
   */
  private static final int[] RETIRED_SLOTS = {5, 6};

  /**
   * Sweep index the realistic co-elution axis was generated at; it appears in every such case id.
   */
  private static final int REALISTIC_INTERFERENCE_SLOT = 7;

  @Test
  void retiredSweepSlotsAreKeptInPlace() {
    final List<SweepVariant> sweep = GenerationConfig.sweep();
    for (final int slot : RETIRED_SLOTS) {
      Assertions.assertTrue(sweep.size() > slot,
          () -> "sweep slot " + slot + " was deleted; retired slots must be kept so later variants "
                + "keep their index (and therefore the ids and seeds of their committed cases)");
      Assertions.assertTrue(sweep.get(slot).isRetired(),
          () -> "sweep slot " + slot + " must stay retired; reusing a retired slot changes the "
                + "seeds of cases that already exist in the committed corpus");
    }
  }

  @Test
  void realisticInterferenceKeepsItsSweepIndex() {
    final List<SweepVariant> sweep = GenerationConfig.sweep();
    Assertions.assertTrue(sweep.size() > REALISTIC_INTERFERENCE_SLOT,
        "the realistic co-elution variant is missing from the sweep");
    Assertions.assertEquals(GenerationConfig.REALISTIC_INTERFERENCE_AXIS,
        sweep.get(REALISTIC_INTERFERENCE_SLOT).axisHint(),
        "the realistic co-elution axis moved off sweep index " + REALISTIC_INTERFERENCE_SLOT
        + "; its committed case ids end in _" + REALISTIC_INTERFERENCE_SLOT
        + " and would no longer be reproducible");
  }

  @Test
  void committedCorpusContainsNoRetiredAxis() {
    final Set<String> liveAxes = new HashSet<>();
    for (final SweepVariant v : GenerationConfig.sweep()) {
      if (!v.isRetired() && v.axisHint() != null) {
        liveAxes.add(v.axisHint());
      }
    }
    // the adversarial axes were removed from the corpus; nothing may reintroduce them
    for (final GroundTruthCase c : IsotopeCorpus.all()) {
      Assertions.assertNotEquals("interference", c.axis(),
          () -> "adversarial axis 'interference' is back in the corpus: " + c.id());
      Assertions.assertNotEquals("combined", c.axis(),
          () -> "adversarial axis 'combined' is back in the corpus: " + c.id());
      Assertions.assertNotEquals(GenerationConfig.RETIRED_AXIS, c.axis(),
          () -> "a retired sweep slot produced a case: " + c.id());
    }
    Assertions.assertTrue(liveAxes.contains(GenerationConfig.REALISTIC_INTERFERENCE_AXIS),
        "the realistic co-elution axis must remain the corpus' co-elution coverage");
  }
}
