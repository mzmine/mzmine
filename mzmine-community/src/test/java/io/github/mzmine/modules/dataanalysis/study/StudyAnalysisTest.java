package io.github.mzmine.modules.dataanalysis.study;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StudyAnalysisTest {
  private static final List<StudyAnalysis.Sample> SAMPLES = List.of(
      new StudyAnalysis.Sample("a1", "baseline", 0d, "a", "p1"),
      new StudyAnalysis.Sample("a1_repeat", "baseline", 0d, "a", "p1"),
      new StudyAnalysis.Sample("b1", "baseline", 0d, "b", "p2"),
      new StudyAnalysis.Sample("c1", "treated", 7d, "c", "p1"),
      new StudyAnalysis.Sample("d1", "treated", 7d, "d", "p2"));

  @Test
  void trajectoriesAggregateTechnicalRepeatsAndLeaveMissingValuesMissing() {
    final var values = List.of(new StudyAnalysis.Measurement("a1", 10d),
        new StudyAnalysis.Measurement("a1_repeat", 14d), new StudyAnalysis.Measurement("b1", null),
        new StudyAnalysis.Measurement("c1", 20d), new StudyAnalysis.Measurement("d1", 24d));
    final var trajectory = StudyAnalysis.trajectories(SAMPLES, values);
    assertEquals(2, trajectory.size());
    assertEquals(12d, trajectory.getFirst().mean());
    assertEquals(1, trajectory.getFirst().unitCount());
    assertEquals(1, trajectory.getFirst().missingSampleCount());
  }

  @Test
  void pairedComparisonRequiresCompleteExplicitPairMapping() {
    final var values = List.of(new StudyAnalysis.Measurement("a1", 1d),
        new StudyAnalysis.Measurement("b1", 2d), new StudyAnalysis.Measurement("c1", 3d));
    final var result = StudyAnalysis.compare(SAMPLES, values, "baseline", "treated", SignificanceTests.PAIRED_T_TEST);
    assertEquals("incomplete_pair_mapping", result.status());
  }

  @Test
  void bhCorrectionUsesEntireFamilyAndRetainsUnavailableEntries() {
    final var corrected = StudyAnalysis.benjaminiHochberg(java.util.Arrays.asList(0.01d, 0.04d, null, 0.03d));
    assertEquals(0.03d, corrected.get(0), 1e-12);
    assertEquals(0.04d, corrected.get(1), 1e-12);
    assertNull(corrected.get(2));
    assertEquals(0.04d, corrected.get(3), 1e-12);
  }

  @Test
  void associationHasNoCausalClaimAndReportsIntervalOnlyWhenDefined() {
    final var result = StudyAnalysis.associate(Map.of("a", 1d, "b", 2d, "c", 3d, "d", 4d),
        Map.of("a", 4d, "b", 3d, "c", 2d, "d", 1d), true);
    assertEquals("available", result.status());
    assertEquals(-1d, result.correlation());
    assertNull(result.confidenceIntervalLower());
  }

  @Test
  void repeatedUnitAcrossConditionsIsRejectedInsteadOfPseudoreplicated() {
    final var samples = List.of(new StudyAnalysis.Sample("a", "control", null, "subject", "pair"),
        new StudyAnalysis.Sample("b", "treated", null, "subject", "pair"));
    assertThrows(IllegalArgumentException.class, () -> StudyAnalysis.compare(samples,
        List.of(new StudyAnalysis.Measurement("a", 1d), new StudyAnalysis.Measurement("b", 2d)),
        "control", "treated", SignificanceTests.WELCHS_T_TEST));
  }

  @Test
  void repeatedUnitAcrossTimepointsIsRejectedInsteadOfTechnicallyAveraged() {
    final var samples = List.of(new StudyAnalysis.Sample("a", "treated", 0d, "subject", "pair"),
        new StudyAnalysis.Sample("b", "treated", 7d, "subject", "pair"));
    assertThrows(IllegalArgumentException.class, () -> StudyAnalysis.compare(samples,
        List.of(new StudyAnalysis.Measurement("a", 1d), new StudyAnalysis.Measurement("b", 2d)),
        "treated", "control", SignificanceTests.WELCHS_T_TEST));
  }

  @Test
  void technicalRepeatsWithSamePairAndUnitRemainOnePairedObservation() {
    final var samples = List.of(new StudyAnalysis.Sample("a1", "before", null, "before_subject1", "subject1"),
        new StudyAnalysis.Sample("a2", "before", null, "before_subject1", "subject1"),
        new StudyAnalysis.Sample("b1", "after", null, "after_subject1", "subject1"),
        new StudyAnalysis.Sample("b2", "after", null, "after_subject1", "subject1"),
        new StudyAnalysis.Sample("c", "before", null, "before_subject2", "subject2"),
        new StudyAnalysis.Sample("d", "after", null, "after_subject2", "subject2"));
    final var result = StudyAnalysis.compare(samples, List.of(new StudyAnalysis.Measurement("a1", 1d),
        new StudyAnalysis.Measurement("a2", 3d), new StudyAnalysis.Measurement("b1", 5d),
        new StudyAnalysis.Measurement("b2", 7d), new StudyAnalysis.Measurement("c", 2d),
        new StudyAnalysis.Measurement("d", 6d)), "before", "after", SignificanceTests.PAIRED_T_TEST);
    assertEquals("available", result.status());
    assertEquals(2, result.leftUnits());
  }
}
