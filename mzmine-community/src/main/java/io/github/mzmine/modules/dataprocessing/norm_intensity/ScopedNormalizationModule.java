package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

/** Applies explicit reviewed per-file factors to a copy of a feature list. */
public final class ScopedNormalizationModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "Scoped normalization";
  }

  @Override
  public @NotNull String getDescription() {
    return "Creates a normalized copy using reviewed frozen per-file factors.";
  }

  @Override
  public @NotNull ExitCode runModule(final @NotNull MZmineProject project,
      final @NotNull ParameterSet parameters, final @NotNull Collection<Task> tasks,
      final @NotNull Instant moduleCallDate) {
    final FeatureList[] lists = parameters.getValue(ScopedNormalizationParameters.featureLists)
        .getMatchingFeatureLists();
    if (lists.length != 1) return ExitCode.ERROR;
    tasks.add(createReviewedTask(project, lists[0], parameters, MemoryMapStorage.forFeatureList(),
        moduleCallDate, () -> true));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.NORMALIZATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ScopedNormalizationParameters.class;
  }

  /** Creates a task with a caller-supplied staleness guard for reviewed local result snapshots. */
  public static @NotNull Task createReviewedTask(final @NotNull MZmineProject project,
      final @NotNull FeatureList source, final @NotNull ParameterSet parameters,
      final @NotNull MemoryMapStorage storage, final @NotNull Instant moduleCallDate,
      final @NotNull BooleanSupplier current) {
    final List<RawDataFile> files = source.getRawDataFiles();
    final String rowPayload = parameters.getValue(ScopedNormalizationParameters.rowFactors);
    if (!rowPayload.isBlank()) {
      final Map<Integer, Map<String, Double>> rowFactors =
          ScopedNormalizationParameters.decodeRowFactors(rowPayload);
      final java.util.Set<Integer> unavailableRows =
          ScopedNormalizationParameters.decodeUnavailableRowIds(
              parameters.getValue(ScopedNormalizationParameters.unavailableRowIds));
      final java.util.Set<Integer> sourceRows = source.getRows().stream()
          .map(FeatureListRow::getID).collect(java.util.stream.Collectors.toSet());
      final java.util.Set<Integer> accountedRows = new java.util.LinkedHashSet<>(rowFactors.keySet());
      accountedRows.addAll(unavailableRows);
      if (!sourceRows.equals(accountedRows) || !java.util.Collections.disjoint(rowFactors.keySet(),
          unavailableRows)) {
        throw new IllegalArgumentException(
            "Reviewed row factors must account for every source feature row exactly once.");
      }
      final List<RawFileNormalizationFunction> functions = new ArrayList<>(files.size());
      for (int index = 0; index < files.size(); index++) {
        final String id = "s" + (index + 1);
        final Map<Integer, Double> factorsForFile = new java.util.LinkedHashMap<>();
        for (final Map.Entry<Integer, Map<String, Double>> entry : rowFactors.entrySet()) {
          final Double factor = entry.getValue().get(id);
          if (entry.getValue().size() != files.size() || factor == null || !Double.isFinite(factor)
              || factor <= 0d) {
            throw new IllegalArgumentException(
                "Each reviewed feature row needs one valid factor per source file.");
          }
          factorsForFile.put(entry.getKey(), factor);
        }
        functions.add(new RawFileNormalizationFunction(files.get(index),
            new FeatureRowNormalizationFunction(factorsForFile, unavailableRows)));
      }
      return reviewedTask(project, source, parameters, storage, moduleCallDate, current,
          new IntensityNormalizationSummary(List.copyOf(functions), List.of(
              "Frozen feature-row factors applied to raw area and height; source revision "
                  + parameters.getValue(ScopedNormalizationParameters.sourceRevision))));
    }
    final Map<String, Double> factors = ScopedNormalizationParameters.decodeFactors(
        parameters.getValue(ScopedNormalizationParameters.factors));
    if (factors.size() != files.size()) {
      throw new IllegalArgumentException("Exactly one reviewed factor is required for every source file.");
    }
    final List<RawFileNormalizationFunction> functions = new ArrayList<>(files.size());
    for (int index = 0; index < files.size(); index++) {
      final String id = "s" + (index + 1);
      final Double factor = factors.get(id);
      if (factor == null || !Double.isFinite(factor) || factor <= 0d) {
        throw new IllegalArgumentException("Missing valid reviewed factor for " + id);
      }
      functions.add(new RawFileNormalizationFunction(files.get(index),
          new FactorNormalizationFunction(factor)));
    }
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.copyOf(functions),
        List.of("Frozen factors applied to raw area and height; source revision "
            + parameters.getValue(ScopedNormalizationParameters.sourceRevision)));
    return reviewedTask(project, source, parameters, storage, moduleCallDate, current, summary);
  }

  private static @NotNull Task reviewedTask(final @NotNull MZmineProject project,
      final @NotNull FeatureList source, final @NotNull ParameterSet parameters,
      final @NotNull MemoryMapStorage storage, final @NotNull Instant moduleCallDate,
      final @NotNull BooleanSupplier current, final @NotNull IntensityNormalizationSummary summary) {
    parameters.setParameter(ScopedNormalizationParameters.hiddenNormalizationSummary, summary);
    return IntensityNormalizerTask.createReviewedTask(project, source,
        parameters.getValue(ScopedNormalizationParameters.suffix),
        parameters.getValue(ScopedNormalizationParameters.sourceRevision), summary, parameters,
        ScopedNormalizationModule.class, storage, moduleCallDate, current);
  }
}
