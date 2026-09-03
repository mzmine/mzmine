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

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.filter_diams2.DiaMs2CorrModule;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Module;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Ms2ScanPairingConditionTest {

  @Test
  void standalonePairingModulesBeforeConsumerSatisfyCondition() {
    final BatchQueue groupMs2Queue = queue(step(new GroupMS2Module()), step(ms2Consumer()));
    final BatchQueue diaMs2Queue = queue(step(new DiaMs2CorrModule()), step(ms2Consumer()));

    Assertions.assertFalse(BatchModuleOrderValidator.validate(groupMs2Queue).hasIssues());
    Assertions.assertFalse(BatchModuleOrderValidator.validate(diaMs2Queue).hasIssues());
  }

  @Test
  void resolverWithMs2ScanPairingEnabledSatisfiesCondition() {
    final BatchQueue queue = resolverThenMs2Consumer(true);

    Assertions.assertFalse(BatchModuleOrderValidator.validate(queue).hasIssues());
  }

  @Test
  void resolverWithoutMs2ScanPairingViolatesCondition() {
    final BatchQueue queue = resolverThenMs2Consumer(false);

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertTrue(result.issues().getFirst().message().contains("MS/MS scan pairing"));
  }

  @Test
  void pairingAfterConsumerViolatesCondition() {
    final BatchQueue queue = queue(step(ms2Consumer()), step(new GroupMS2Module()));

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(TestSubjectModule.class,
        queue.get(result.issues().getFirst().stepIndex()).getModule().getClass());
  }

  private static @NotNull BatchQueue resolverThenMs2Consumer(final boolean pairMs2Scans) {
    final MinimumSearchFeatureResolverParameters parameters =
        new MinimumSearchFeatureResolverParameters();
    parameters.setParameter(GeneralResolverParameters.groupMS2Parameters, pairMs2Scans);
    return queue(step(new MinimumSearchFeatureResolverModule(), parameters), step(ms2Consumer()));
  }

  private static @NotNull TestSubjectModule ms2Consumer() {
    return new TestSubjectModule(
        new ModuleOrderRecommendation("The test module requires paired MS2 scans",
            ModuleOrderRule.mustRunAfter(Ms2ScanPairingCondition.INSTANCE)));
  }

  private static @NotNull MZmineProcessingStepImpl<MZmineProcessingModule> step(
      @NotNull final MZmineProcessingModule module) {
    return step(module, new SimpleParameterSet());
  }

  private static @NotNull MZmineProcessingStepImpl<MZmineProcessingModule> step(
      @NotNull final MZmineProcessingModule module, @NotNull final ParameterSet parameters) {
    return new MZmineProcessingStepImpl<>(module, parameters);
  }

  @SafeVarargs
  private static @NotNull BatchQueue queue(
      @NotNull final MZmineProcessingStepImpl<MZmineProcessingModule>... steps) {
    final BatchQueue queue = new BatchQueue();
    queue.addAll(steps);
    return queue;
  }
}
