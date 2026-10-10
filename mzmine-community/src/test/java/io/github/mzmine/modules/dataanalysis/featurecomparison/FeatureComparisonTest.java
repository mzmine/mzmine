/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package io.github.mzmine.modules.dataanalysis.featurecomparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.FeatureStatus;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FeatureComparisonTest {

  @Test
  void summarizeKeepsStatusAndMissingEvidenceSeparate() {
    final GroupSummary summary = FeatureComparison.summarize(List.of(
        observation(FeatureStatus.DETECTED, 10d), observation(FeatureStatus.MANUAL, 20d),
        observation(FeatureStatus.ESTIMATED, 30d), observation(FeatureStatus.COMPOUND_AGGREGATED,
            40d), observation(FeatureStatus.UNKNOWN, 99d), observation(null, 50d),
        observation(FeatureStatus.DETECTED, null)));

    assertEquals(7, summary.sampleCount());
    assertEquals(3, summary.detectedCount());
    assertEquals(1, summary.estimatedCount());
    assertEquals(1, summary.aggregatedCount());
    assertEquals(3, summary.missingCount());
    assertEquals(4, summary.quantifiedCount());
    assertEquals(25d, summary.mean());
    assertEquals(Math.sqrt(500d / 3d) / 25d, summary.cv(), 1e-12);
  }

  @Test
  void summarizeDoesNotExposeOverflowedMeanOrCv() {
    final GroupSummary summary = FeatureComparison.summarize(List.of(
        observation(FeatureStatus.DETECTED, Double.MAX_VALUE),
        observation(FeatureStatus.DETECTED, Double.MAX_VALUE)));

    assertEquals(2, summary.quantifiedCount());
    assertNull(summary.mean());
    assertNull(summary.cv());
  }

  @Test
  void compareCalculatesFiniteRatiosAndRejectsUndefinedBlankEvidence() {
    final FeatureComparisonSpec spec = new FeatureComparisonSpec(List.of(0, 1), List.of(2, 3),
        List.of(4, 5), 1d, 2d, 5d, true);
    final FeatureComparisonResult result = FeatureComparison.compare(new RowInput(7, List.of(
        observation(FeatureStatus.DETECTED, 20d), observation(FeatureStatus.DETECTED, 10d),
        observation(FeatureStatus.DETECTED, 5d), observation(FeatureStatus.DETECTED, 5d),
        observation(FeatureStatus.DETECTED, 1d), observation(FeatureStatus.DETECTED, 2d))), spec);

    assertTrue(result.passes());
    assertEquals("increased", result.differenceStatus());
    assertEquals("blank_ratio_passed", result.blankStatus());
    assertEquals(3d, result.foldChange());
    assertEquals(10d, result.blankRatio());
    assertTrue(result.reasons().isEmpty());

    final FeatureComparisonResult undefinedBlank = FeatureComparison.compare(new RowInput(8, List.of(
        observation(FeatureStatus.DETECTED, 20d), observation(FeatureStatus.DETECTED, 10d),
        observation(FeatureStatus.DETECTED, 5d), observation(FeatureStatus.DETECTED, 5d),
        observation(FeatureStatus.UNKNOWN, 1d), observation(FeatureStatus.UNKNOWN, 2d))), spec);
    assertFalse(undefinedBlank.passes());
    assertEquals("unassessed", undefinedBlank.blankStatus());
    assertNull(undefinedBlank.blankRatio());
    assertTrue(undefinedBlank.reasons().contains("blank_ratio_unavailable"));
  }

  @Test
  void detectedZeroReferenceIsNotTreatedAsSampleOnlyOrInfiniteFoldChange() {
    final FeatureComparisonSpec spec = new FeatureComparisonSpec(List.of(0), List.of(1), List.of(),
        1d, 2d, null, true);
    final FeatureComparisonResult result = FeatureComparison.compare(new RowInput(9, List.of(
        observation(FeatureStatus.DETECTED, 10d), observation(FeatureStatus.DETECTED, 0d))), spec);

    assertFalse(result.passes());
    assertEquals("comparison_undefined", result.differenceStatus());
    assertNull(result.foldChange());
    assertTrue(result.reasons().contains("fold_change_unavailable"));
  }

  @Test
  void sampleOnlyRequiresTargetDetectionAndCanBeExcluded() {
    final RowInput row = new RowInput(10, List.of(observation(FeatureStatus.DETECTED, 10d),
        observation(FeatureStatus.UNKNOWN, null)));
    final FeatureComparisonSpec included = new FeatureComparisonSpec(List.of(0), List.of(1),
        List.of(), 1d, 2d, null, true);
    final FeatureComparisonSpec excluded = new FeatureComparisonSpec(List.of(0), List.of(1),
        List.of(), 1d, 2d, null, false);

    assertTrue(FeatureComparison.compare(row, included).passes());
    final FeatureComparisonResult excludedResult = FeatureComparison.compare(row, excluded);
    assertFalse(excludedResult.passes());
    assertEquals("sample_only", excludedResult.differenceStatus());
    assertTrue(excludedResult.reasons().contains("sample_only_excluded"));
  }

  @Test
  void sampleOnlyRequiresPositiveQuantifiedTargetSignalAndRatiosStayFinite() {
    final FeatureComparisonSpec spec = new FeatureComparisonSpec(List.of(0), List.of(1), List.of(),
        1d, 2d, null, true);
    final FeatureComparisonResult noTargetQuantification = FeatureComparison.compare(new RowInput(11,
        List.of(observation(FeatureStatus.DETECTED, null), observation(FeatureStatus.UNKNOWN, null))), spec);
    assertFalse(noTargetQuantification.passes());
    assertEquals("comparison_undefined", noTargetQuantification.differenceStatus());
    assertTrue(noTargetQuantification.reasons().contains("sample_only_target_signal_unavailable"));

    final FeatureComparisonResult overflow = FeatureComparison.compare(new RowInput(12, List.of(
        observation(FeatureStatus.DETECTED, Double.MAX_VALUE),
        observation(FeatureStatus.DETECTED, Double.MIN_VALUE))), spec);
    assertFalse(overflow.passes());
    assertNull(overflow.foldChange());
    assertTrue(overflow.reasons().contains("fold_change_unavailable"));
  }

  @Test
  void compareAllRetainsExcludedRowsInDeterministicRanking() {
    final FeatureComparisonSpec spec = new FeatureComparisonSpec(List.of(0), List.of(1), List.of(),
        1d, 2d, null, true);
    final List<FeatureComparisonResult> results = FeatureComparison.compareAll(List.of(
        row(3, 40d, 10d), row(12, 10d, null), row(2, 20d, 10d), row(1, 5d, 10d)), spec);

    assertEquals(List.of(12, 3, 2, 1),
        results.stream().map(FeatureComparisonResult::rowId).toList());
    assertFalse(results.getLast().passes());
    assertTrue(results.getLast().reasons().contains("fold_change_below_minimum"));
  }

  @Test
  void compareAllRespondsToCallerCancellationBetweenRows() {
    final FeatureComparisonSpec spec = new FeatureComparisonSpec(List.of(0), List.of(1), List.of(),
        1d, 2d, null, true);
    final AtomicInteger checks = new AtomicInteger();
    final Iterable<RowInput> lazyRows = () -> List.of(row(1, 20d, 10d), row(2, 40d, 10d)).iterator();

    assertThrows(CancellationException.class,
        () -> FeatureComparison.compareAll(lazyRows, spec, () -> checks.incrementAndGet() >= 2));
  }

  @Test
  void specificationRejectsAmbiguousGroupsAndInvalidThresholds() {
    assertThrows(IllegalArgumentException.class,
        () -> new FeatureComparisonSpec(List.of(0), List.of(0), List.of(), 1d, 2d, null, true));
    assertThrows(IllegalArgumentException.class,
        () -> new FeatureComparisonSpec(List.of(0), List.of(1), List.of(), 1.1d, 2d, null, true));
    assertThrows(IllegalArgumentException.class,
        () -> new FeatureComparisonSpec(List.of(0), List.of(1), List.of(), 1d, .9d, null, true));
  }

  private static Observation observation(FeatureStatus status, Double abundance) {
    return new Observation(status, abundance);
  }

  private static RowInput row(int id, Double target, Double reference) {
    return new RowInput(id, List.of(observation(FeatureStatus.DETECTED, target),
        observation(reference == null ? FeatureStatus.UNKNOWN : FeatureStatus.DETECTED, reference)));
  }
}
