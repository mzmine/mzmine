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
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MassDetectionConditionTest {

  @Test
  void standaloneMassDetectionBeforeConsumerSatisfiesCondition() {
    final BatchQueue queue = queue(step(new MassDetectionModule(), null),
        step(massListConsumer(), null));

    Assertions.assertFalse(BatchModuleOrderValidator.validate(queue).hasIssues());
  }

  @Test
  void standaloneMassDetectionAfterConsumerViolatesCondition() {
    final BatchQueue queue = queue(step(massListConsumer(), null),
        step(new MassDetectionModule(), null));

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(TestSubjectModule.class,
        queue.get(result.issues().getFirst().stepIndex()).getModule().getClass());
  }

  @Test
  void advancedImportMassDetectionSatisfiesCondition() {
    final BatchQueue ms1Queue = importThenMassListConsumer(true, false);
    final BatchQueue msnQueue = importThenMassListConsumer(false, true);

    Assertions.assertFalse(BatchModuleOrderValidator.validate(ms1Queue).hasIssues());
    Assertions.assertFalse(BatchModuleOrderValidator.validate(msnQueue).hasIssues());
  }

  @Test
  void advancedImportWithoutMassDetectionViolatesCondition() {
    final BatchQueue queue = importThenMassListConsumer(false, false);

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertTrue(result.issues().getFirst().message().contains("advanced data import"));
  }

  @Test
  void massDetectionInPreviousConcatenatedPipelineDoesNotSatisfyCondition() {
    final BatchQueue queue = queue(
        step(new AllSpectralDataImportModule(), importParameters(true, false)),
        step(new AllSpectralDataImportModule(), importParameters(false, false)),
        step(massListConsumer(), null));

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(1, result.issues().getFirst().segmentIndex());
  }

  private static @NotNull BatchQueue importThenMassListConsumer(final boolean ms1MassDetection,
      final boolean msnMassDetection) {
    return queue(step(new AllSpectralDataImportModule(),
        importParameters(ms1MassDetection, msnMassDetection)), step(massListConsumer(), null));
  }

  private static @NotNull TestSubjectModule massListConsumer() {
    return new TestSubjectModule(
        new ModuleOrderRecommendation("The test module requires mass lists",
            ModuleOrderRule.mustSatisfy(MassDetectionCondition.INSTANCE)));
  }

  private static @NotNull ParameterSet importParameters(final boolean ms1MassDetection,
      final boolean msnMassDetection) {
    final AdvancedSpectraImportParameters advanced = new AdvancedSpectraImportParameters();
    advanced.setParameter(AdvancedSpectraImportParameters.msMassDetection, ms1MassDetection);
    advanced.setParameter(AdvancedSpectraImportParameters.ms2MassDetection, msnMassDetection);

    final AllSpectralDataImportParameters parameters = new AllSpectralDataImportParameters();
    parameters.setParameter(AllSpectralDataImportParameters.advancedImport, true);
    parameters.getParameter(AllSpectralDataImportParameters.advancedImport)
        .setEmbeddedParameters(advanced);
    return parameters;
  }

  private static @NotNull MZmineProcessingStepImpl<MZmineProcessingModule> step(
      @NotNull final MZmineProcessingModule module, @Nullable final ParameterSet parameters) {
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
