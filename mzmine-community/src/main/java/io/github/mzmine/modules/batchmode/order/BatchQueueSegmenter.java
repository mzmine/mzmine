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

package io.github.mzmine.modules.batchmode.order;

import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder.ChromatogramBuilderModule;
import io.github.mzmine.util.collections.IndexRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Finds independently evaluable processing pipelines in concatenated batch queues.
 */
final class BatchQueueSegmenter {

  private static final Set<Class<? extends MZmineProcessingModule>> CHROMATOGRAM_BUILDERS = Set.of(
      ModularADAPChromatogramBuilderModule.class, ChromatogramBuilderModule.class);

  private static final List<Predicate<MZmineProcessingStep<MZmineProcessingModule>>> SPLIT_STEPS = List.of(
      step -> step.getModule().getModuleCategory() == MZmineModuleCategory.RAWDATAIMPORT,
      step -> CHROMATOGRAM_BUILDERS.contains(step.getModule().getClass()));

  private BatchQueueSegmenter() {
  }

  /**
   * Recursively splits first at repeated imports and then at repeated chromatogram builders inside
   * each import segment. A single occurrence of a split step does not split a queue.
   */
  static @NotNull List<@NotNull IndexRange> split(@NotNull final BatchQueue batchQueue) {
    if (batchQueue.isEmpty()) {
      return List.of();
    }

    final List<IndexRange> segments = new ArrayList<>();
    splitRecursively(batchQueue, IndexRange.ofExclusive(0, batchQueue.size()), 0, segments);
    return List.copyOf(segments);
  }

  private static void splitRecursively(@NotNull final BatchQueue batchQueue,
      @NotNull final IndexRange segment, final int splitStepIndex,
      @NotNull final List<IndexRange> results) {
    if (splitStepIndex >= SPLIT_STEPS.size()) {
      results.add(segment);
      return;
    }

    final Predicate<MZmineProcessingStep<MZmineProcessingModule>> isSplitStep = SPLIT_STEPS.get(
        splitStepIndex);
    final List<Integer> splitIndices = new ArrayList<>();
    for (int i = segment.min(); i < segment.maxExclusive(); i++) {
      if (isSplitStep.test(batchQueue.get(i))) {
        splitIndices.add(i);
      }
    }

    if (splitIndices.size() < 2) {
      splitRecursively(batchQueue, segment, splitStepIndex + 1, results);
      return;
    }

    int childStart = segment.min();
    for (int i = 1; i < splitIndices.size(); i++) {
      final int childEnd = splitIndices.get(i);
      splitRecursively(batchQueue, IndexRange.ofExclusive(childStart, childEnd), splitStepIndex + 1,
          results);
      childStart = childEnd;
    }
    splitRecursively(batchQueue, IndexRange.ofExclusive(childStart, segment.maxExclusive()),
        splitStepIndex + 1, results);
  }
}
