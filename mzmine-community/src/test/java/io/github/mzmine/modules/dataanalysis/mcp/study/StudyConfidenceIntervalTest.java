/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataanalysis.mcp.study;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudyConfidenceIntervalTest {

  /* Independent SciPy 1.16.2 reference: scipy.stats.ttest_ind for x=[1,2,4,5], y=[3,6,7,12]. */
  @Test
  void welchAndStudentIntervalsMatchSciPyReferences() {
    final List<StudyAnalysis.Sample> samples = List.of(
        sample("l1", "left", "l1", null), sample("l2", "left", "l2", null),
        sample("l3", "left", "l3", null), sample("l4", "left", "l4", null),
        sample("r1", "right", "r1", null), sample("r2", "right", "r2", null),
        sample("r3", "right", "r3", null), sample("r4", "right", "r4", null));
    final List<StudyAnalysis.Measurement> values = List.of(value("l1", 1), value("l2", 2), value("l3", 4),
        value("l4", 5), value("r1", 3), value("r2", 6), value("r3", 7), value("r4", 12));
    final var welch = StudyAnalysis.confidenceInterval(samples, values, "left", "right",
        SignificanceTests.WELCHS_T_TEST, .95d);
    assertEquals(-4d, welch.lower() + (welch.upper() - welch.lower()) / 2d, 1e-12d);
    assertEquals(2.0816659994661326d, welch.standardError(), 1e-12d);
    assertEquals(4.351931330472103d, welch.degreesOfFreedom(), 1e-12d);
    assertEquals(-9.599888614682941d, welch.lower(), 1e-9d);
    assertEquals(1.599888614682941d, welch.upper(), 1e-9d);
    final var student = StudyAnalysis.confidenceInterval(samples, values, "left", "right",
        SignificanceTests.STUDENTS_T_TEST, .95d);
    assertEquals(6d, student.degreesOfFreedom());
    assertEquals(-9.093653204219217d, student.lower(), 1e-9d);
    assertEquals(1.0936532042192173d, student.upper(), 1e-9d);
  }

  /* Independent SciPy 1.16.2 reference: ttest_rel([2,4,10], [5,8,11]). */
  @Test
  void pairedIntervalAggregatesTechnicalRepeatsBeforeUsingPairIds() {
    final List<StudyAnalysis.Sample> samples = List.of(
        sample("left-a-1", "left", "left-a", "p1"), sample("left-a-2", "left", "left-a", "p1"),
        sample("left-b", "left", "left-b", "p2"), sample("left-c", "left", "left-c", "p3"),
        sample("right-a", "right", "right-a", "p1"), sample("right-b", "right", "right-b", "p2"),
        sample("right-c", "right", "right-c", "p3"));
    final List<StudyAnalysis.Measurement> values = List.of(value("left-a-1", 1), value("left-a-2", 3),
        value("left-b", 4), value("left-c", 10), value("right-a", 5), value("right-b", 8), value("right-c", 11));
    final var interval = StudyAnalysis.confidenceInterval(samples, values, "left", "right",
        SignificanceTests.PAIRED_T_TEST, .95d);
    assertEquals(.8819171036881968d, interval.standardError(), 1e-12d);
    assertEquals(2d, interval.degreesOfFreedom());
    assertEquals(-6.461249700216402d, interval.lower(), 1e-9d);
    assertEquals(1.1279163668830687d, interval.upper(), 1e-9d);
  }

  @Test
  void pairedIntervalRejectsIncompletePairsAndNonTTests() {
    final List<StudyAnalysis.Sample> samples = List.of(sample("l", "left", "l", "p1"),
        sample("r", "right", "r", "p2"), sample("l2", "left", "l2", "p3"),
        sample("r2", "right", "r2", "p3"));
    final List<StudyAnalysis.Measurement> values = List.of(value("l", 1), value("r", 2), value("l2", 3), value("r2", 4));
    assertNull(StudyAnalysis.confidenceInterval(samples, values, "left", "right",
        SignificanceTests.PAIRED_T_TEST, .95d));
    assertNull(StudyAnalysis.confidenceInterval(samples, values, "left", "right",
        SignificanceTests.MANN_WHITNEY_U_TEST, .95d));
  }

  @Test
  void pairedIntervalRejectsAnIndependentUnitAssignedToMultiplePairs() {
    final List<StudyAnalysis.Sample> samples = List.of(
        sample("l1", "left", "left-subject1", "p1"), sample("l2", "left", "left-subject1", "p2"),
        sample("r1", "right", "right-subject1", "p1"), sample("r2", "right", "right-subject2", "p2"),
        sample("l3", "left", "left-subject2", "p3"), sample("r3", "right", "right-subject3", "p3"));
    final List<StudyAnalysis.Measurement> values = List.of(value("l1", 1), value("l2", 3), value("r1", 5),
        value("r2", 7), value("l3", 2), value("r3", 6));
    assertNull(StudyAnalysis.confidenceInterval(samples, values, "left", "right",
        SignificanceTests.PAIRED_T_TEST, .95d));
  }

  private static StudyAnalysis.Sample sample(final String id, final String condition, final String unit,
      final String pair) {
    return new StudyAnalysis.Sample(id, condition, null, unit, pair);
  }

  private static StudyAnalysis.Measurement value(final String id, final double value) {
    return new StudyAnalysis.Measurement(id, value);
  }
}
