/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable, complete accounting for feature-QC normalization. */
public final class FeatureQcResult {

  private final @NotNull List<FeatureQcDriftNormalization.Sample> samples;
  private final @NotNull Map<String, FeatureQcDriftNormalization.Sample> samplesById;
  private final @NotNull Map<Integer, Map<String, Double>> rowFactors;
  private final @NotNull Map<Integer, Set<String>> boundarySampleIds;
  private final @NotNull Map<Integer, FeatureQcUnavailableReason> unavailableRows;
  private final @NotNull FeatureQcSettings settings;
  private final @NotNull FeatureQcDiagnostics diagnostics;
  private final @NotNull FeatureQcAvailability availability;

  FeatureQcResult(@NotNull final List<FeatureQcDriftNormalization.Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> rowFactors,
      @NotNull final Map<Integer, Set<String>> boundarySampleIds,
      @NotNull final Map<Integer, FeatureQcUnavailableReason> unavailableRows,
      @NotNull final FeatureQcDiagnostics diagnostics) {
    this.samples = List.copyOf(samples);
    final Map<String, FeatureQcDriftNormalization.Sample> byId = new LinkedHashMap<>();
    this.samples.forEach(sample -> byId.put(sample.id(), sample));
    this.samplesById = Map.copyOf(byId);
    this.rowFactors = immutableNestedMap(rowFactors);
    this.boundarySampleIds = immutableSetMap(boundarySampleIds);
    this.unavailableRows = Map.copyOf(unavailableRows);
    this.settings = diagnostics.settings();
    this.diagnostics = diagnostics;
    this.availability = diagnostics.availability();
  }

  public @NotNull List<FeatureQcDriftNormalization.Sample> samples() { return samples; }
  public @NotNull Map<Integer, Map<String, Double>> rowFactors() { return rowFactors; }
  public @NotNull Map<Integer, Set<String>> boundarySampleIds() { return boundarySampleIds; }
  public @NotNull Map<Integer, FeatureQcUnavailableReason> unavailableRows() { return unavailableRows; }
  public @NotNull FeatureQcSettings settings() { return settings; }
  public @NotNull FeatureQcDiagnostics diagnostics() { return diagnostics; }
  public @NotNull FeatureQcAvailability availability() { return availability; }

  /** Resolves compact global or per-batch unavailable states without materializing a status matrix. */
  public @Nullable FeatureQcUnavailableReason unavailableReason(final int rowId,
      @NotNull final String sampleId) {
    final FeatureQcUnavailableReason global = unavailableRows.get(rowId);
    if (global != null) return global;
    final FeatureQcRowDiagnostic row = diagnostics.rows().get(rowId);
    if (row == null) return null;
    final FeatureQcDriftNormalization.Sample sample = samplesById.get(sampleId);
    if (sample == null) return null;
    final FeatureQcBatchDiagnostic batch = row.batches().get(sample.batch());
    if (batch == null) return null;
    if (batch.unavailableReason() != null) return batch.unavailableReason();
    return null;
  }

  private static @NotNull Map<Integer, Map<String, Double>> immutableNestedMap(
      @NotNull final Map<Integer, Map<String, Double>> input) {
    final Map<Integer, Map<String, Double>> output = new LinkedHashMap<>();
    input.forEach((key, value) -> output.put(key, Map.copyOf(value)));
    return Map.copyOf(output);
  }

  private static @NotNull Map<Integer, Set<String>> immutableSetMap(
      @NotNull final Map<Integer, Set<String>> input) {
    final Map<Integer, Set<String>> output = new LinkedHashMap<>();
    input.forEach((key, value) -> output.put(key, Set.copyOf(new LinkedHashSet<>(value))));
    return Map.copyOf(output);
  }
}
