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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
      final ModuleOrderRecommendationEvaluation evaluation = evaluateRecommendation(batchQueue,
          segment, stepIndex, recommendation);
      if (evaluation.status() == ModuleOrderRuleStatus.VIOLATION) {
        violations.add(evaluation);
      }
    }

    // decision: A passing alternative suppresses only its own recommendation, never a violation in
    // another recommendation. Only failed recommendations are ranked to select the least severe
    // user-facing problem, ranked by their most severe violated alternative.
    final ModuleOrderRecommendationEvaluation selectedViolation = violations.stream().min(
            Comparator.comparingInt(evaluation -> severityRank(
                ModuleOrderRules.level(representativeViolation(evaluation).ruleEvaluation().rule()))))
        .orElse(null);
    if (selectedViolation == null) {
      return;
    }

    final ModuleOrderRecommendation recommendation = selectedViolation.recommendation();
    final ModuleOrderRule selectedRule = representativeViolation(selectedViolation).ruleEvaluation()
        .rule();
    final String pipelineSuffix =
        showPipelineIndex ? ", pipeline %d".formatted(segmentIndex + 1) : "";
    final String message = formatIssueMessage(stepIndex + 1, pipelineSuffix,
        selectedViolation.violations());
    issues.add(
        new BatchModuleOrderIssue(ModuleOrderRules.level(selectedRule), segmentIndex, stepIndex,
            module.getName(), recommendation, selectedRule, message));
  }

  /**
   * Formats the user-facing issue message. A single violation is reported inline. Several
   * alternatives of a combined recommendation are listed as {@code step.index} bullets separated by
   * "or", because satisfying any one of them resolves the warning.
   */
  private static @NotNull String formatIssueMessage(final int stepNumber,
      @NotNull final String pipelineSuffix,
      @NotNull final List<@NotNull ModuleOrderRuleViolation> violations) {
    if (violations.size() == 1) {
      final ModuleOrderRuleViolation violation = violations.getFirst();
      return "Step %d%s, %s. %s".formatted(stepNumber, pipelineSuffix,
          violation.ruleEvaluation().ruleDescription(), asSentence(violation.rationale()));
    }
    final String bullets = IntStream.range(0, violations.size()).mapToObj(index -> {
      final ModuleOrderRuleViolation violation = violations.get(index);
      return "\u2022 %d.%d - %s. %s".formatted(stepNumber, index + 1,
          violation.ruleEvaluation().ruleDescription(), asSentence(violation.rationale()));
    }).collect(Collectors.joining("\n   or\n"));
    return "Step %d%s:\n%s".formatted(stepNumber, pipelineSuffix, bullets);
  }

  /**
   * Evaluates a recommendation. {@link AnyOfModuleOrderRecommendation} uses OR semantics: any
   * passing alternative satisfies it, an alternative violation only counts when none pass, and it
   * is not applicable when every alternative is not applicable.
   */
  private static @NotNull ModuleOrderRecommendationEvaluation evaluateRecommendation(
      @NotNull final BatchQueue batchQueue, @NotNull final IndexRange segment, final int stepIndex,
      @NotNull final ModuleOrderRecommendation recommendation) {
    return switch (recommendation) {
      case SingleModuleOrderRecommendation single -> {
        final ModuleOrderRuleEvaluation ruleEvaluation = evaluateRule(batchQueue, segment,
            stepIndex, single.rule());
        final List<ModuleOrderRuleViolation> violations =
            ruleEvaluation.status() == ModuleOrderRuleStatus.VIOLATION ? List.of(
                new ModuleOrderRuleViolation(single.rationale(), ruleEvaluation)) : List.of();
        yield new ModuleOrderRecommendationEvaluation(single, ruleEvaluation.status(), violations);
      }
      case AnyOfModuleOrderRecommendation any -> {
        final List<ModuleOrderRecommendationEvaluation> alternatives = any.alternatives().stream()
            .map(alternative -> evaluateRecommendation(batchQueue, segment, stepIndex, alternative))
            .toList();
        if (alternatives.stream()
            .anyMatch(evaluation -> evaluation.status() == ModuleOrderRuleStatus.PASS)) {
          yield new ModuleOrderRecommendationEvaluation(any, ModuleOrderRuleStatus.PASS, List.of());
        }
        final List<ModuleOrderRuleViolation> violations = alternatives.stream()
            .flatMap(evaluation -> evaluation.violations().stream()).toList();
        final ModuleOrderRuleStatus status =
            violations.isEmpty() ? ModuleOrderRuleStatus.NOT_APPLICABLE
                : ModuleOrderRuleStatus.VIOLATION;
        yield new ModuleOrderRecommendationEvaluation(any, status, violations);
      }
    };
  }

  /**
   * The most severe violated alternative, which drives the reported importance level of a combined
   * recommendation.
   */
  private static @NotNull ModuleOrderRuleViolation representativeViolation(
      @NotNull final ModuleOrderRecommendationEvaluation evaluation) {
    return evaluation.violations().stream().max(Comparator.comparingInt(
            violation -> severityRank(ModuleOrderRules.level(violation.ruleEvaluation().rule()))))
        .orElseThrow();
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
