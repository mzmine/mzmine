/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TargetedFeatureDetectionMergeTest {

  private static final MZTolerance MZ_TOL = new MZTolerance(0.005, 5);
  private static final RTTolerance RT_TOL = new RTTolerance(0.2f, Unit.MINUTES);

  private static CompoundDBAnnotation annotation(String name, double mz, float rt) {
    final var annotation = new SimpleCompoundDBAnnotation();
    annotation.put(CompoundNameType.class, name);
    annotation.put(PrecursorMZType.class, mz);
    annotation.put(RTType.class, rt);
    return annotation;
  }

  private static List<String> names(OverlappingCompoundAnnotation merged) {
    return merged.getAnnotations().stream().map(CompoundDBAnnotation::getCompoundName)
        .collect(Collectors.toList());
  }

  /**
   * Regression test for issue #3479: a CSV with a single target was dropped during merging, so the
   * targeted feature detection reported "no annotations remaining" instead of detecting the target.
   */
  @Test
  void singleTargetIsNotDropped() {
    final List<CompoundDBAnnotation> input = List.of(annotation("ID1", 418.2238, 0.518f));

    final var merged = TargetedFeatureDetectionModuleTask.findAndMergeOverlaps(input, MZ_TOL, RT_TOL,
        null);

    assertEquals(1, merged.size(), "The single target must be kept.");
    assertEquals(List.of("ID1"), names(merged.get(0)));
  }

  /**
   * The last distinct target of any input list must survive the merge (this was the general form of
   * the #3479 bug, masked by duplicating the row).
   */
  @Test
  void lastDistinctTargetIsNotDropped() {
    final List<CompoundDBAnnotation> input = List.of(annotation("low", 200.1000, 1.0f),
        annotation("mid", 300.1000, 1.0f), annotation("high", 418.2238, 0.518f));

    final var merged = TargetedFeatureDetectionModuleTask.findAndMergeOverlaps(input, MZ_TOL, RT_TOL,
        null);

    assertEquals(3, merged.size(), "All distinct targets must be kept.");
    final List<String> keptNames = merged.stream().flatMap(m -> m.getAnnotations().stream())
        .map(CompoundDBAnnotation::getCompoundName).collect(Collectors.toList());
    assertTrue(keptNames.containsAll(List.of("low", "mid", "high")));
  }

  /**
   * Two targets at the same m/z and RT (the "duplicate" work-around from the bug report) collapse
   * into a single merged annotation carrying both.
   */
  @Test
  void duplicateTargetsAreMergedIntoOne() {
    final List<CompoundDBAnnotation> input = List.of(annotation("ID1", 418.2238, 0.518f),
        annotation("ID1_duplicate", 418.2238, 0.518f));

    final var merged = TargetedFeatureDetectionModuleTask.findAndMergeOverlaps(input, MZ_TOL, RT_TOL,
        null);

    assertEquals(1, merged.size(), "Overlapping targets must merge into one.");
    assertEquals(2, merged.get(0).getAnnotations().size(),
        "Both annotations must be retained in the merged entry.");
  }

  /**
   * Targets that share an m/z but are separated in RT beyond the tolerance must stay separate.
   */
  @Test
  void sameMzDifferentRtStaySeparate() {
    final List<CompoundDBAnnotation> input = List.of(annotation("early", 418.2238, 0.5f),
        annotation("late", 418.2238, 5.0f));

    final var merged = TargetedFeatureDetectionModuleTask.findAndMergeOverlaps(input, MZ_TOL, RT_TOL,
        null);

    assertEquals(2, merged.size(), "Targets separated in RT must not be merged.");
  }

  @Test
  void emptyInputYieldsEmptyResult() {
    final var merged = TargetedFeatureDetectionModuleTask.findAndMergeOverlaps(List.of(), MZ_TOL,
        RT_TOL, null);

    assertTrue(merged.isEmpty());
  }
}
