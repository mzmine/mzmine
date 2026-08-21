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
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatchModuleOrderValidatorTest {

  @Test
  void requiredAnchorMustExistInTheSameSegment() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));
    final BatchQueue queue = queue(subject);

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(ModuleOrderLevel.MUST, result.issues().getFirst().level());
    Assertions.assertTrue(
        result.issues().getFirst().message().contains("required step is missing"));
  }

  @Test
  void requiredAnchorPassesInTheCorrectOrder() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));
    final BatchQueue queue = queue(new TestAnchorModule(), subject);

    Assertions.assertFalse(BatchModuleOrderValidator.validate(queue).hasIssues());
  }

  @Test
  void recommendedRuleCanRequireAnAnchor() {
    final TestSubjectModule beforeSubject = new TestSubjectModule(
        recommendation(ModuleOrderRule.shouldRunBefore(TestAnchorModule.class)));
    final TestSubjectModule afterSubject = new TestSubjectModule(
        recommendation(ModuleOrderRule.shouldRunAfter(TestAnchorModule.class)));

    Assertions.assertFalse(
        BatchModuleOrderValidator.validate(queue(beforeSubject, new TestAnchorModule()))
            .hasIssues());
    Assertions.assertFalse(
        BatchModuleOrderValidator.validate(queue(new TestAnchorModule(), afterSubject))
            .hasIssues());

    final BatchModuleOrderValidationResult missingAnchorResult = BatchModuleOrderValidator.validate(
        queue(beforeSubject));
    Assertions.assertEquals(1, missingAnchorResult.issues().size());
    Assertions.assertEquals(ModuleOrderLevel.SHOULD,
        missingAnchorResult.issues().getFirst().level());
    Assertions.assertTrue(
        missingAnchorResult.issues().getFirst().message().contains("required step is missing"));
  }

  @Test
  void conditionalRuleIsIgnoredWhenAnchorIsAbsent() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.ifPresentShouldRunAfter(TestAnchorModule.class)));

    Assertions.assertFalse(BatchModuleOrderValidator.validate(queue(subject)).hasIssues());
  }

  @Test
  void conditionalRuleWarnsWhenAnchorIsInTheWrongOrder() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.ifPresentShouldRunAfter(TestAnchorModule.class)));

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(
        queue(subject, new TestAnchorModule()));

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(ModuleOrderLevel.SHOULD, result.issues().getFirst().level());
  }

  @Test
  void onePassingRecommendationAcceptsThePlacement() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)),
        otherRecommendation(ModuleOrderRule.ifPresentShouldRunBefore(TestOtherAnchorModule.class)));
    final BatchQueue queue = queue(new TestAnchorModule(), subject, new TestOtherAnchorModule());

    Assertions.assertFalse(BatchModuleOrderValidator.validate(queue).hasIssues());
  }

  @Test
  void leastSevereViolationIsSelectedWhenAllAlternativesFail() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)),
        otherRecommendation(ModuleOrderRule.ifPresentShouldRunAfter(TestOtherAnchorModule.class)));
    final BatchQueue queue = queue(subject, new TestAnchorModule(), new TestOtherAnchorModule());

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(ModuleOrderLevel.SHOULD, result.issues().getFirst().level());
  }

  @Test
  void warningLevelsAreFormattedInSeparateGroups() {
    final TestSubjectModule mustSubject = new TestSubjectModule(
        recommendation(ModuleOrderRule.ifPresentMustRunAfter(TestAnchorModule.class)));
    final TestSubjectModule shouldSubject = new TestSubjectModule(
        otherRecommendation(ModuleOrderRule.ifPresentShouldRunAfter(TestOtherAnchorModule.class)));
    final BatchQueue queue = queue(mustSubject, new TestAnchorModule(), shouldSubject,
        new TestOtherAnchorModule());

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(2, result.issues().size());
    Assertions.assertTrue(result.formatMessage().contains("Required processing order"));
    Assertions.assertTrue(result.formatMessage().contains("Recommended processing order"));
  }

  @Test
  void recursivelySplitsImportSegmentsAtRepeatedChromatogramBuilders() {
    final BatchQueue twoImportsTwoBuilders = queue(new AllSpectralDataImportModule(),
        new ModularADAPChromatogramBuilderModule(), new TestOrderModule("First pipeline"),
        new AllSpectralDataImportModule(), new ModularADAPChromatogramBuilderModule(),
        new TestOrderModule("Second pipeline"));
    Assertions.assertEquals(List.of(IndexRange.ofExclusive(0, 3), IndexRange.ofExclusive(3, 6)),
        BatchQueueSegmenter.split(twoImportsTwoBuilders));

    final BatchQueue twoImportsThreeBuilders = queue(new AllSpectralDataImportModule(),
        new ModularADAPChromatogramBuilderModule(), new TestOrderModule("First pipeline"),
        new ModularADAPChromatogramBuilderModule(), new TestOrderModule("Second pipeline"),
        new AllSpectralDataImportModule(), new ModularADAPChromatogramBuilderModule(),
        new TestOrderModule("Third pipeline"));
    Assertions.assertEquals(List.of(IndexRange.ofExclusive(0, 3), IndexRange.ofExclusive(3, 5),
        IndexRange.ofExclusive(5, 8)), BatchQueueSegmenter.split(twoImportsThreeBuilders));
  }

  @Test
  void anAnchorInAnotherPipelineDoesNotSatisfyARule() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));
    final BatchQueue queue = queue(new AllSpectralDataImportModule(), new TestAnchorModule(),
        new AllSpectralDataImportModule(), subject);

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(1, result.issues().getFirst().segmentIndex());
  }

  private static ModuleOrderRecommendation recommendation(final ModuleOrderRule rule) {
    return new ModuleOrderRecommendation("Test use case", "Test rationale", rule);
  }

  private static ModuleOrderRecommendation otherRecommendation(final ModuleOrderRule rule) {
    return new ModuleOrderRecommendation("Other use case", "Other rationale", rule);
  }

  private static BatchQueue queue(final MZmineProcessingModule... modules) {
    final BatchQueue queue = new BatchQueue();
    for (final MZmineProcessingModule module : modules) {
      queue.add(new MZmineProcessingStepImpl<>(module, null));
    }
    return queue;
  }
}
