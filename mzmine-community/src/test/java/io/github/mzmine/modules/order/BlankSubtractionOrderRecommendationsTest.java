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

package io.github.mzmine.modules.order;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.BatchModuleOrderValidator;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms.ChromatogramBlankSubtractionModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BlankSubtractionOrderRecommendationsTest {

  @Test
  void featureListBlankSubtractionMustRunAfterAlignment() {
    final BatchQueue correctOrder = queue(new JoinAlignerModule(),
        new FeatureListBlankSubtractionModule());
    final BatchQueue wrongOrder = queue(new FeatureListBlankSubtractionModule(),
        new JoinAlignerModule());

    Assertions.assertFalse(BatchModuleOrderValidator.validate(correctOrder).hasIssues());
    Assertions.assertEquals(1, BatchModuleOrderValidator.validate(wrongOrder).issues().size());
  }

  @Test
  void chromatogramBlankSubtractionMustRunBeforeResolving() {
    final BatchQueue correctOrder = queue(new ChromatogramBlankSubtractionModule(),
        new MinimumSearchFeatureResolverModule());
    final BatchQueue wrongOrder = queue(new MinimumSearchFeatureResolverModule(),
        new ChromatogramBlankSubtractionModule());

    Assertions.assertFalse(BatchModuleOrderValidator.validate(correctOrder).hasIssues());
    Assertions.assertEquals(1, BatchModuleOrderValidator.validate(wrongOrder).issues().size());
  }

  private static @NotNull BatchQueue queue(@NotNull final MZmineProcessingModule... modules) {
    final BatchQueue queue = new BatchQueue();
    for (final MZmineProcessingModule module : modules) {
      queue.add(new MZmineProcessingStepImpl<>(module, null));
    }
    return queue;
  }
}
