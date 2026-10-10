package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
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
    return new ScopedNormalizationTask(project, source, parameters, storage, moduleCallDate, current);
  }
}
