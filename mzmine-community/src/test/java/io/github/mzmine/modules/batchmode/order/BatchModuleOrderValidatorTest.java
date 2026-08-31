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
import java.util.Map;
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
        result.issues().getFirst().message().contains("Subject MUST run after TestAnchorModule"));
    Assertions.assertTrue(
        result.issues().getFirst().message().contains("TestAnchorModule needs to be added"));
  }

  @Test
  void requiredAnchorPassesInTheCorrectOrder() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));
    final BatchQueue queue = queue(new TestAnchorModule(), subject);

    Assertions.assertFalse(BatchModuleOrderValidator.validate(queue).hasIssues());
  }

  @Test
  void validationMessagesAreAvailableByStepIndex() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));
    final BatchQueue queue = queue(subject, new TestOrderModule("Unaffected"));

    final Map<Integer, String> messages = BatchModuleOrderValidator.validateAndFormatByStep(queue);

    Assertions.assertEquals(1, messages.size());
    Assertions.assertTrue(messages.get(0).contains("Subject MUST run after TestAnchorModule"));
    Assertions.assertFalse(messages.containsKey(1));
  }

  @Test
  void pipelineIndexIsOnlyShownForConcatenatedBatches() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));

    final String singlePipelineMessage = BatchModuleOrderValidator.validate(queue(subject)).issues()
        .getFirst().message();
    final String secondPipelineMessage = BatchModuleOrderValidator.validate(
        queue(new AllSpectralDataImportModule(), new TestOrderModule("First pipeline"),
            new AllSpectralDataImportModule(), subject)).issues().getFirst().message();

    Assertions.assertFalse(singlePipelineMessage.contains("pipeline 1"));
    Assertions.assertTrue(secondPipelineMessage.contains("pipeline 2"));
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
    Assertions.assertTrue(missingAnchorResult.issues().getFirst().message()
        .contains("TestAnchorModule needs to be added"));
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
  void passingRecommendationDoesNotSuppressAnotherViolation() {
    final TestSubjectModule subject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)),
        otherRecommendation(ModuleOrderRule.ifPresentShouldRunBefore(TestOtherAnchorModule.class)));
    final BatchQueue queue = queue(subject, new TestAnchorModule(), new TestOtherAnchorModule());

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(queue);

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(ModuleOrderLevel.MUST, result.issues().getFirst().level());
  }

  @Test
  void everyMatchingAnchorMustBeOnTheCorrectSide() {
    final TestSubjectModule beforeSubject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunBefore(TestAnchorModule.class)));
    final TestSubjectModule afterSubject = new TestSubjectModule(
        recommendation(ModuleOrderRule.mustRunAfter(TestAnchorModule.class)));

    final BatchModuleOrderValidationResult beforeBetweenAnchors = BatchModuleOrderValidator.validate(
        queue(new TestAnchorModule(), beforeSubject, new TestAnchorModule()));
    final BatchModuleOrderValidationResult afterBetweenAnchors = BatchModuleOrderValidator.validate(
        queue(new TestAnchorModule(), afterSubject, new TestAnchorModule()));

    Assertions.assertEquals(1, beforeBetweenAnchors.issues().size());
    Assertions.assertTrue(
        beforeBetweenAnchors.issues().getFirst().message().contains("Anchor (step 1)"));
    Assertions.assertEquals(1, afterBetweenAnchors.issues().size());
    Assertions.assertTrue(
        afterBetweenAnchors.issues().getFirst().message().contains("Anchor (step 3)"));

    Assertions.assertFalse(BatchModuleOrderValidator.validate(
        queue(beforeSubject, new TestAnchorModule(), new TestAnchorModule())).hasIssues());
    Assertions.assertFalse(BatchModuleOrderValidator.validate(
        queue(new TestAnchorModule(), new TestAnchorModule(), afterSubject)).hasIssues());
  }

  @Test
  void leastSevereViolationIsSelectedWhenMultipleRulesFail() {
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

  @Test
  void anyOfAnchorIsSatisfiedByEitherModule() {
    final TestSubjectModule subject = new TestSubjectModule(recommendation(
        ModuleOrderRule.mustRunAfter(
            ModuleOrderCondition.anyOf(TestAnchorModule.class, TestOtherAnchorModule.class))));

    Assertions.assertFalse(
        BatchModuleOrderValidator.validate(queue(new TestAnchorModule(), subject)).hasIssues());
    Assertions.assertFalse(
        BatchModuleOrderValidator.validate(queue(new TestOtherAnchorModule(), subject))
            .hasIssues());
  }

  @Test
  void anyOfAnchorReportsAllAlternativesWhenMissing() {
    final TestSubjectModule subject = new TestSubjectModule(recommendation(
        ModuleOrderRule.mustRunAfter(
            ModuleOrderCondition.anyOf(TestAnchorModule.class, TestOtherAnchorModule.class))));

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(
        queue(subject));

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertTrue(result.issues().getFirst().message()
        .contains("TestAnchorModule or TestOtherAnchorModule"));
  }

  @Test
  void anyOfAnchorEnforcesOrderAgainstEveryPresentAlternative() {
    final TestSubjectModule subject = new TestSubjectModule(recommendation(
        ModuleOrderRule.mustRunAfter(
            ModuleOrderCondition.anyOf(TestAnchorModule.class, TestOtherAnchorModule.class))));

    final BatchModuleOrderValidationResult result = BatchModuleOrderValidator.validate(
        queue(new TestAnchorModule(), subject, new TestOtherAnchorModule()));

    Assertions.assertEquals(1, result.issues().size());
    Assertions.assertEquals(ModuleOrderLevel.MUST, result.issues().getFirst().level());
  }

  private static ModuleOrderRecommendation recommendation(final ModuleOrderRule rule) {
    return new ModuleOrderRecommendation("Test rationale", rule);
  }

  private static ModuleOrderRecommendation otherRecommendation(final ModuleOrderRule rule) {
    return new ModuleOrderRecommendation("Other rationale", rule);
  }

  private static BatchQueue queue(final MZmineProcessingModule... modules) {
    final BatchQueue queue = new BatchQueue();
    for (final MZmineProcessingModule module : modules) {
      queue.add(new MZmineProcessingStepImpl<>(module, null));
    }
    return queue;
  }
}
