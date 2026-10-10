/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataanalysis.study;

import io.github.mzmine.modules.dataanalysis.significance.SignificanceTests;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure calculations for explicitly described study designs. This class deliberately does not
 * infer conditions, subjects, or technical replicates from file names.
 */
public final class StudyAnalysis {

  private StudyAnalysis() {
  }

  /** A sample's immutable, user-supplied study assignment. */
  public record Sample(@NotNull String sampleId, @NotNull String condition, @Nullable Double time,
                       @NotNull String independentUnit, @Nullable String pairId) {
    public Sample {
      requireText(sampleId, "sample_id");
      requireText(condition, "condition");
      requireText(independentUnit, "independent_unit");
      if (time != null && !Double.isFinite(time)) throw new IllegalArgumentException("time must be finite");
    }
  }

  /** One frozen numerical measurement. A null value is missing, never a zero. */
  public record Measurement(@NotNull String sampleId, @Nullable Double value) {
    public Measurement {
      requireText(sampleId, "sample_id");
      if (value != null && !Double.isFinite(value)) throw new IllegalArgumentException("measurement must be finite");
    }
  }

  /** Mean value at one condition/time point after aggregating technical repeats by independent unit. */
  public record TrajectoryPoint(@NotNull String condition, @Nullable Double time, int unitCount,
                                int missingSampleCount, @Nullable Double mean, @Nullable Double standardDeviation) {
  }

  /** Raw test result. The caller applies multiple-testing correction over its complete feature family. */
  public record TestResult(@NotNull String status, @Nullable Double pValue, @Nullable Double effect,
                           int leftUnits, int rightUnits, @NotNull List<String> warnings) {
  }

  /** Aggregates technical repeat samples by condition/time/independent unit and rejects ambiguous mappings. */
  public static @NotNull List<TrajectoryPoint> trajectories(@NotNull List<Sample> samples,
      @NotNull List<Measurement> measurements) {
    final Map<String, Sample> sampleMap = sampleMap(samples);
    final Map<String, List<Double>> values = new LinkedHashMap<>();
    final Map<String, Integer> missing = new LinkedHashMap<>();
    final Map<String, PointKey> keys = new LinkedHashMap<>();
    for (final Measurement measurement : measurements) {
      final Sample sample = requireSample(sampleMap, measurement.sampleId());
      final PointKey key = new PointKey(sample.condition(), sample.time(), sample.independentUnit());
      final String encoded = key.encoded();
      keys.putIfAbsent(encoded, key);
      if (measurement.value() == null) missing.merge(encoded, 1, Integer::sum);
      else values.computeIfAbsent(encoded, ignored -> new ArrayList<>()).add(measurement.value());
    }
    final Map<PointKey, List<Double>> unitValues = new TreeMap<>(PointKey.ORDER);
    final Map<PointKey, Integer> unitMissing = new TreeMap<>(PointKey.ORDER);
    for (final Map.Entry<String, PointKey> entry : keys.entrySet()) {
      final List<Double> repeated = values.getOrDefault(entry.getKey(), List.of());
      if (!repeated.isEmpty()) unitValues.put(entry.getValue(), List.of(mean(repeated)));
      unitMissing.put(entry.getValue(), missing.getOrDefault(entry.getKey(), 0));
    }
    final Map<GroupKey, List<Double>> grouped = new TreeMap<>(GroupKey.ORDER);
    final Map<GroupKey, Integer> groupedMissing = new TreeMap<>(GroupKey.ORDER);
    for (final Map.Entry<PointKey, List<Double>> entry : unitValues.entrySet()) {
      grouped.computeIfAbsent(new GroupKey(entry.getKey().condition, entry.getKey().time), ignored -> new ArrayList<>())
          .addAll(entry.getValue());
    }
    for (final Map.Entry<PointKey, Integer> entry : unitMissing.entrySet()) {
      final GroupKey group = new GroupKey(entry.getKey().condition, entry.getKey().time);
      grouped.computeIfAbsent(group, ignored -> new ArrayList<>());
      groupedMissing.merge(group, entry.getValue(), Integer::sum);
    }
    final List<TrajectoryPoint> output = new ArrayList<>();
    for (final GroupKey key : grouped.keySet()) {
      final List<Double> observations = grouped.get(key);
      output.add(new TrajectoryPoint(key.condition, key.time, observations.size(),
          groupedMissing.getOrDefault(key, 0), observations.isEmpty() ? null : mean(observations), sd(observations)));
    }
    return List.copyOf(output);
  }

  /**
   * Tests independent units only. Paired tests require every pair id exactly once in each group;
   * any incomplete pair returns an unavailable result instead of silently dropping a subject.
   */
  public static @NotNull TestResult compare(@NotNull List<Sample> samples,
      @NotNull List<Measurement> measurements, @NotNull String leftCondition,
      @NotNull String rightCondition, @NotNull SignificanceTests test) {
    requireText(leftCondition, "left_condition");
    requireText(rightCondition, "right_condition");
    if (leftCondition.equals(rightCondition)) throw new IllegalArgumentException("comparison conditions must differ");
    final Map<String, Sample> sampleMap = sampleMap(samples);
    final Map<String, Double> unitValues = aggregateUnits(samples, measurements, sampleMap);
    final List<String> warnings = new ArrayList<>();
    final Map<String, Double> left = unitsForCondition(samples, unitValues, leftCondition);
    final Map<String, Double> right = unitsForCondition(samples, unitValues, rightCondition);
    if (test == SignificanceTests.PAIRED_T_TEST) {
      return paired(samples, left, right, warnings);
    }
    final double[] leftValues = values(left);
    final double[] rightValues = values(right);
    if (leftValues.length < 2 || rightValues.length < 2) {
      return new TestResult("insufficient_independent_units", null, null, leftValues.length, rightValues.length,
          List.of("At least two measured independent units are required in each condition."));
    }
    final double p;
    try { p = test.test(List.of(leftValues, rightValues)); }
    catch (RuntimeException e) { return new TestResult("test_unavailable", null, null, leftValues.length,
        rightValues.length, List.of(e.getMessage())); }
    if (!Double.isFinite(p)) return new TestResult("test_unavailable", null, null, leftValues.length,
        rightValues.length, List.of("The selected test returned a non-finite p value."));
    return new TestResult("available", p, mean(leftValues) - mean(rightValues), leftValues.length,
        rightValues.length, List.copyOf(warnings));
  }

  /** One-way ANOVA over all listed conditions, after technical-repeat aggregation. */
  public static @NotNull TestResult anova(@NotNull List<Sample> samples,
      @NotNull List<Measurement> measurements, @NotNull List<String> conditions) {
    if (conditions.size() < 3) throw new IllegalArgumentException("ANOVA requires at least three conditions");
    final Map<String, Sample> sampleMap = sampleMap(samples);
    final Map<String, Double> unitValues = aggregateUnits(samples, measurements, sampleMap);
    final List<double[]> groups = new ArrayList<>();
    for (final String condition : conditions) {
      final double[] group = values(unitsForCondition(samples, unitValues, condition));
      if (group.length < 2) return new TestResult("insufficient_independent_units", null, null, group.length, 0,
          List.of("At least two measured independent units are required in every ANOVA condition."));
      groups.add(group);
    }
    try {
      final double p = SignificanceTests.ONE_WAY_ANOVA.test(groups);
      if (!Double.isFinite(p)) return new TestResult("test_unavailable", null, null, 0, 0,
          List.of("The selected test returned a non-finite p value."));
      return new TestResult("available", p, null, groups.stream().mapToInt(group -> group.length).sum(), 0, List.of());
    } catch (RuntimeException e) {
      return new TestResult("test_unavailable", null, null, 0, 0, List.of(e.getMessage()));
    }
  }

  /** Benjamini-Hochberg correction over the complete supplied family, preserving null/unavailable entries. */
  public static @NotNull List<@Nullable Double> benjaminiHochberg(@NotNull List<@Nullable Double> pValues) {
    final List<IndexedP> finite = new ArrayList<>();
    for (int i = 0; i < pValues.size(); i++) {
      final Double p = pValues.get(i);
      if (p != null && Double.isFinite(p) && p >= 0d && p <= 1d) finite.add(new IndexedP(i, p));
    }
    final List<Double> adjusted = new ArrayList<>(java.util.Collections.nCopies(pValues.size(), null));
    finite.sort(Comparator.comparingDouble(IndexedP::value));
    double previous = 1d;
    for (int rank = finite.size(); rank >= 1; rank--) {
      final IndexedP point = finite.get(rank - 1);
      previous = Math.min(previous, point.value * finite.size() / rank);
      adjusted.set(point.index, previous);
    }
    // Null preserves an untestable feature in its complete multiple-testing family.
    return java.util.Collections.unmodifiableList(adjusted);
  }

  /** Pearson or Spearman correlation based only on matched, finite independent-unit means. */
  public static @NotNull Association associate(@NotNull Map<String, Double> featureByUnit,
      @NotNull Map<String, Double> activityByUnit, boolean spearman) {
    final List<Double> left = new ArrayList<>(), right = new ArrayList<>();
    for (final Map.Entry<String, Double> entry : featureByUnit.entrySet()) {
      final Double activity = activityByUnit.get(entry.getKey());
      if (finite(entry.getValue()) && finite(activity)) { left.add(entry.getValue()); right.add(activity); }
    }
    if (left.size() < 3) return new Association("insufficient_matched_independent_units", null, null, null, left.size());
    final double[] x = left.stream().mapToDouble(Double::doubleValue).toArray();
    final double[] y = right.stream().mapToDouble(Double::doubleValue).toArray();
    final double correlation = spearman ? new org.apache.commons.math3.stat.correlation.SpearmansCorrelation().correlation(x, y)
        : new org.apache.commons.math3.stat.correlation.PearsonsCorrelation().correlation(x, y);
    if (!Double.isFinite(correlation)) return new Association("constant_or_undefined_values", null, null, null, left.size());
    // Fisher's interval is only reported where its large-sample approximation is defined.
    Double lower = null, upper = null;
    if (!spearman && left.size() > 3 && Math.abs(correlation) < 1d) {
      final double z = 0.5d * (Math.log1p(correlation) - Math.log1p(-correlation));
      final double margin = 1.96d / Math.sqrt(left.size() - 3d);
      lower = Math.tanh(z - margin); upper = Math.tanh(z + margin);
    }
    // This is a descriptive association. Do not invent a p value when assumptions are not checked.
    return new Association("available", correlation, lower, upper, left.size());
  }

  public record Association(@NotNull String status, @Nullable Double correlation,
                            @Nullable Double confidenceIntervalLower,
                            @Nullable Double confidenceIntervalUpper, int matchedIndependentUnits) { }

  private static @NotNull TestResult paired(List<Sample> samples, Map<String, Double> left,
      Map<String, Double> right, List<String> warnings) {
    final Map<String, String> leftPairs = pairs(samples, left.keySet());
    final Map<String, String> rightPairs = pairs(samples, right.keySet());
    if (!leftPairs.keySet().equals(rightPairs.keySet()) || leftPairs.size() < 2) {
      return new TestResult("incomplete_pair_mapping", null, null, left.size(), right.size(),
          List.of("Every measured independent unit must have one explicit pair_id in each comparison condition."));
    }
    final double[] x = new double[leftPairs.size()], y = new double[rightPairs.size()]; int i = 0;
    for (final String pair : new TreeMap<>(leftPairs).keySet()) { x[i] = left.get(leftPairs.get(pair)); y[i++] = right.get(rightPairs.get(pair)); }
    try { final double p = SignificanceTests.PAIRED_T_TEST.test(List.of(x, y));
      if (!Double.isFinite(p)) return new TestResult("test_unavailable", null, null, x.length, y.length,
          List.of("The selected test returned a non-finite p value."));
      return new TestResult("available", p, mean(x) - mean(y), x.length, y.length, List.copyOf(warnings)); }
    catch (RuntimeException e) { return new TestResult("test_unavailable", null, null, x.length, y.length, List.of(e.getMessage())); }
  }

  private static @NotNull Map<String, String> pairs(List<Sample> samples, java.util.Set<String> unitIds) {
    final Map<String, String> result = new LinkedHashMap<>();
    for (final Sample sample : samples) if (unitIds.contains(sample.independentUnit())) {
      if (sample.pairId() == null || sample.pairId().isBlank()) return Map.of();
      final String existing = result.putIfAbsent(sample.pairId(), sample.independentUnit());
      if (existing != null && !existing.equals(sample.independentUnit())) return Map.of();
    }
    return result;
  }

  private static @NotNull Map<String, Double> aggregateUnits(List<Sample> samples, List<Measurement> measurements,
      Map<String, Sample> sampleMap) {
    final Map<String, List<Double>> values = new LinkedHashMap<>();
    final Map<String, GroupKey> unitGroup = new LinkedHashMap<>();
    for (final Measurement measurement : measurements) {
      final Sample sample = requireSample(sampleMap, measurement.sampleId());
      final GroupKey group = new GroupKey(sample.condition(), sample.time());
      final GroupKey old = unitGroup.putIfAbsent(sample.independentUnit(), group);
      if (old != null && !old.equals(group)) throw new IllegalArgumentException(
          "independent_unit " + sample.independentUnit()
              + " occurs across conditions or time points; use distinct units and explicit pair_id for paired designs");
      if (measurement.value() != null) values.computeIfAbsent(sample.independentUnit(), ignored -> new ArrayList<>()).add(measurement.value());
    }
    final Map<String, Double> result = new LinkedHashMap<>();
    values.forEach((unit, repeats) -> result.put(unit, mean(repeats)));
    return result;
  }

  private static @NotNull Map<String, Double> unitsForCondition(List<Sample> samples, Map<String, Double> units,
      String condition) {
    final Map<String, Double> result = new LinkedHashMap<>();
    for (final Sample sample : samples) if (condition.equals(sample.condition()) && units.containsKey(sample.independentUnit()))
      result.put(sample.independentUnit(), units.get(sample.independentUnit()));
    return result;
  }

  private static @NotNull Map<String, Sample> sampleMap(List<Sample> samples) {
    final Map<String, Sample> map = new LinkedHashMap<>();
    for (final Sample sample : samples) if (map.put(sample.sampleId(), sample) != null)
      throw new IllegalArgumentException("duplicate sample_id " + sample.sampleId());
    return map;
  }
  private static Sample requireSample(Map<String, Sample> samples, String id) { final Sample sample = samples.get(id); if (sample == null) throw new IllegalArgumentException("measurement refers to undeclared sample_id " + id); return sample; }
  private static double[] values(Map<String, Double> values) { return values.values().stream().mapToDouble(Double::doubleValue).toArray(); }
  private static Double finiteOrNull(double value) { return Double.isFinite(value) ? value : null; }
  private static boolean finite(Double value) { return value != null && Double.isFinite(value); }
  private static double mean(List<Double> values) { return mean(values.stream().mapToDouble(Double::doubleValue).toArray()); }
  private static double mean(double[] values) { return new DescriptiveStatistics(values).getMean(); }
  private static Double sd(List<Double> values) { return values.size() < 2 ? null : finiteOrNull(new DescriptiveStatistics(values.stream().mapToDouble(Double::doubleValue).toArray()).getStandardDeviation()); }
  private static void requireText(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); }
  private record IndexedP(int index, double value) { }
  private record PointKey(String condition, Double time, String unit) { String encoded() { return condition + "\u0000" + time + "\u0000" + unit; } static final Comparator<PointKey> ORDER = Comparator.comparing(PointKey::condition).thenComparing(PointKey::time, Comparator.nullsFirst(Double::compare)).thenComparing(PointKey::unit); }
  private record GroupKey(String condition, Double time) { static final Comparator<GroupKey> ORDER = Comparator.comparing(GroupKey::condition).thenComparing(GroupKey::time, Comparator.nullsFirst(Double::compare)); }
}
