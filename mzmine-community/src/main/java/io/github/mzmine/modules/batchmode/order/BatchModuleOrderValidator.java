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
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.util.collections.IndexRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates module order recommendations independently within inferred batch queue segments.
 */
public final class BatchModuleOrderValidator {

  private BatchModuleOrderValidator() {
  }

  public static @Nullable String validateAndFormat(@NotNull final BatchQueue batchQueue) {
    final BatchModuleOrderValidationResult result = validate(batchQueue);
    return result.hasIssues() ? result.formatMessage() : null;
  }

  /**
   * @return validation messages keyed by their zero-based batch step index
   */
  public static @NotNull Map<Integer, String> validateAndFormatByStep(
      @NotNull final BatchQueue batchQueue) {
    final Map<Integer, String> messages = new LinkedHashMap<>();
    for (final BatchModuleOrderIssue issue : validate(batchQueue).issues()) {
      messages.merge(issue.stepIndex(), issue.message(), (first, second) -> first + "\n" + second);
    }
    return Map.copyOf(messages);
  }

  static @NotNull BatchModuleOrderValidationResult validate(@NotNull final BatchQueue batchQueue) {
    final List<BatchModuleOrderIssue> issues = new ArrayList<>();
    final List<IndexRange> segments = BatchQueueSegmenter.split(batchQueue);
    final boolean showPipelineIndex = segments.size() > 1;
    for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
      final IndexRange segment = segments.get(segmentIndex);
      validateSegment(batchQueue, segment, segmentIndex, showPipelineIndex, issues);
    }
    return new BatchModuleOrderValidationResult(issues);
  }

  private static void validateSegment(@NotNull final BatchQueue batchQueue,
      @NotNull final IndexRange segment, final int segmentIndex, final boolean showPipelineIndex,
      @NotNull final List<BatchModuleOrderIssue> issues) {
    for (int stepIndex = segment.min(); stepIndex < segment.maxExclusive(); stepIndex++) {
      final MZmineProcessingModule module = batchQueue.get(stepIndex).getModule();
      validateRecommendations(batchQueue, segment, segmentIndex, showPipelineIndex, stepIndex,
          module,
          module.getModuleOrderRecommendations(), issues);
    }
  }

  private static void validateRecommendations(@NotNull final BatchQueue batchQueue,
      @NotNull final IndexRange segment, final int segmentIndex, final boolean showPipelineIndex,
      final int stepIndex, @NotNull final MZmineProcessingModule module,
      @NotNull final List<@NotNull ModuleOrderRecommendation> recommendations,
      @NotNull final List<BatchModuleOrderIssue> issues) {
    final List<ModuleOrderRecommendationEvaluation> violations = new ArrayList<>();
    for (final ModuleOrderRecommendation recommendation : recommendations) {
      final ModuleOrderRule rule = recommendation.rule();
      final ModuleOrderRuleEvaluation evaluation = evaluateRule(batchQueue, segment, stepIndex,
          rule);
      switch (evaluation.status()) {
        case VIOLATION ->
            violations.add(new ModuleOrderRecommendationEvaluation(recommendation, evaluation));
        case PASS, NOT_APPLICABLE -> {
        }
      }
    }

    // decision: A passing rule never suppresses another rule's violation. Only failed rules are
    // ranked to select the least severe user-facing problem.
    final ModuleOrderRecommendationEvaluation selectedViolation = violations.stream().min(
            Comparator.comparingInt(
                evaluation -> severityRank(ModuleOrderRules.level(evaluation.ruleEvaluation().rule()))))
        .orElse(null);
    if (selectedViolation == null) {
      return;
    }

    final ModuleOrderRecommendation recommendation = selectedViolation.recommendation();
    final ModuleOrderRuleEvaluation ruleEvaluation = selectedViolation.ruleEvaluation();
    final ModuleOrderRule selectedRule = ruleEvaluation.rule();
    final String ruleDescription = ruleEvaluation.ruleDescription();
    final String missingText = ruleEvaluation.requiredStepMissing() ? switch (selectedRule) {
      case RelativeModuleOrderRule relativeRule -> {
        final ModuleOrderEvaluationContext context = new ModuleOrderEvaluationContext(batchQueue,
            segment, stepIndex);
        final String anchorDescription = relativeRule.anchorCondition().description(context);
        yield " " + asSentence(capitalizeFirst(anchorDescription) + " needs to be added");
      }
    } : "";
    final String rationale = asSentence(recommendation.rationale());
    final String pipelineText =
        showPipelineIndex ? "pipeline %d, ".formatted(segmentIndex + 1) : "";
    final String message = "Step %d, %s%s. %s%s".formatted(stepIndex + 1, pipelineText,
        ruleDescription, rationale, missingText);
    issues.add(
        new BatchModuleOrderIssue(ModuleOrderRules.level(selectedRule), segmentIndex, stepIndex,
            module.getName(), recommendation, selectedRule, message));
  }

  private static @NotNull ModuleOrderRuleEvaluation evaluateRule(
      @NotNull final BatchQueue batchQueue, @NotNull final IndexRange segment, final int stepIndex,
      @NotNull final ModuleOrderRule rule) {
    return switch (rule) {
      case RelativeModuleOrderRule relativeRule ->
          evaluateRelativeRule(batchQueue, segment, stepIndex, relativeRule);
    };
  }

  private static @NotNull ModuleOrderRuleEvaluation evaluateRelativeRule(
      @NotNull final BatchQueue batchQueue, @NotNull final IndexRange segment, final int stepIndex,
      @NotNull final RelativeModuleOrderRule rule) {
    final ModuleOrderEvaluationContext context = new ModuleOrderEvaluationContext(batchQueue,
        segment, stepIndex);
    final String selfName = batchQueue.get(stepIndex).getModule().getName();
    final List<Integer> anchorIndices = new ArrayList<>();
    String anchorName = rule.anchorCondition().description(context);
    for (int i = segment.min(); i < segment.maxExclusive(); i++) {
      if (i == stepIndex) {
        continue;
      }
      final MZmineProcessingStep<MZmineProcessingModule> candidate = batchQueue.get(i);
      if (rule.anchorCondition().matches(candidate)) {
        anchorIndices.add(i);
        anchorName = candidate.getModule().getName();
      }
    }

    if (anchorIndices.isEmpty()) {
      final ModuleOrderRuleStatus status =
          rule.anchorRequirement() == ModuleOrderAnchorRequirement.REQUIRED
              ? ModuleOrderRuleStatus.VIOLATION : ModuleOrderRuleStatus.NOT_APPLICABLE;
      return new ModuleOrderRuleEvaluation(rule, status,
          ModuleOrderTextFormatter.describeRule(rule, selfName, anchorName), true);
    }

    // decision: Relative rules define a pipeline boundary. Every matching anchor must be on the
    // requested side so that sequences such as alignment -> resolving -> alignment are rejected.
    final Integer violatingAnchorIndex = anchorIndices.stream()
        .filter(anchorIndex -> !isCorrectlyOrdered(stepIndex, anchorIndex, rule.position()))
        .min(Comparator.comparingInt(anchorIndex -> Math.abs(anchorIndex - stepIndex)))
        .orElse(null);
    final boolean correctOrder = violatingAnchorIndex == null;
    if (violatingAnchorIndex != null) {
      final String violatingAnchorName = batchQueue.get(violatingAnchorIndex).getModule().getName();
      anchorName = "%s (step %d)".formatted(violatingAnchorName, violatingAnchorIndex + 1);
    }
    return new ModuleOrderRuleEvaluation(rule,
        correctOrder ? ModuleOrderRuleStatus.PASS : ModuleOrderRuleStatus.VIOLATION,
        ModuleOrderTextFormatter.describeRule(rule, selfName, anchorName), false);
  }

  private static boolean isCorrectlyOrdered(final int stepIndex, final int anchorIndex,
      @NotNull final ModuleOrderPosition position) {
    return switch (position) {
      case BEFORE -> stepIndex < anchorIndex;
      case AFTER -> stepIndex > anchorIndex;
    };
  }

  private static int severityRank(@NotNull final ModuleOrderLevel level) {
    return switch (level) {
      case SHOULD -> 0;
      case MUST -> 1;
    };
  }

  private static @NotNull String asSentence(@NotNull final String text) {
    return switch (text.charAt(text.length() - 1)) {
      case '.', '!', '?' -> text;
      default -> text + ".";
    };
  }

  private static @NotNull String capitalizeFirst(@NotNull final String text) {
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

}
