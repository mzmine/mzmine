package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

/** Applies exactly one validated frozen factor to every feature of each raw file in a copy. */
final class ScopedNormalizationTask extends AbstractSimpleTask {
  private final @NotNull MZmineProject project;
  private final @NotNull ModularFeatureList source;
  private final @NotNull String suffix;
  private final @NotNull BooleanSupplier current;
  private @NotNull List<FeatureList> processed = List.of();

  ScopedNormalizationTask(final @NotNull MZmineProject project, final @NotNull FeatureList source,
      final @NotNull ParameterSet parameters, final @NotNull MemoryMapStorage storage,
      final @NotNull Instant moduleCallDate, final @NotNull BooleanSupplier current) {
    super(storage, moduleCallDate, parameters, ScopedNormalizationModule.class);
    if (!(source instanceof ModularFeatureList modular))
      throw new IllegalArgumentException("Scoped normalization requires a modular feature list.");
    this.project = project;
    this.source = modular;
    suffix = parameters.getValue(ScopedNormalizationParameters.suffix);
    this.current = current;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Applying reviewed frozen normalization factors to " + source.getName();
  }

  @Override
  protected void process() {
    requireCurrent();
    final Map<String, Double> factors = ScopedNormalizationParameters.decodeFactors(
        parameters.getValue(ScopedNormalizationParameters.factors));
    final List<RawDataFile> files = source.getRawDataFiles();
    final Map<RawDataFile, RawFileNormalizationFunction> functions = functions(files, factors);
    totalItems = (long) files.size() * source.getNumberOfRows();
    final ModularFeatureList copy = FeatureListUtils.createCopy(source, suffix, storage, true);
    prepareCopy(copy);
    for (final Map.Entry<RawDataFile, RawFileNormalizationFunction> entry : functions.entrySet()) {
      for (final FeatureListRow row : copy.getRows()) {
        if (isCanceled()) return;
        final Feature feature = row.getFeature(entry.getKey());
        if (feature instanceof ModularFeature modular) FeatureDataUtils.normalizeAbundances(modular, entry.getValue().function());
        incrementFinishedItems();
      }
    }
    if (isCanceled()) return;
    requireCurrent();
    parameters.setParameter(ScopedNormalizationParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(List.copyOf(functions.values()), List.of(
            "Frozen factors applied to raw area and height; source revision " + parameters.getValue(ScopedNormalizationParameters.sourceRevision))));
    processed = List.of(copy);
    project.addFeatureList(copy);
  }

  private static @NotNull Map<RawDataFile, RawFileNormalizationFunction> functions(
      final @NotNull List<RawDataFile> files, final @NotNull Map<String, Double> factors) {
    final Map<RawDataFile, RawFileNormalizationFunction> output = new LinkedHashMap<>();
    if (factors.size() != files.size()) throw new IllegalArgumentException("Exactly one reviewed factor is required for every source file.");
    for (int index = 0; index < files.size(); index++) {
      final String id = "s" + (index + 1);
      final Double factor = factors.get(id);
      if (factor == null || !Double.isFinite(factor) || factor <= 0d)
        throw new IllegalArgumentException("Missing valid reviewed factor for " + id);
      output.put(files.get(index), new RawFileNormalizationFunction(files.get(index), new FactorNormalizationFunction(factor)));
    }
    return Map.copyOf(output);
  }

  private static void prepareCopy(final @NotNull ModularFeatureList copy) {
    final NormalizedAreaType area = DataTypes.get(NormalizedAreaType.class);
    final NormalizedHeightType height = DataTypes.get(NormalizedHeightType.class);
    // decision: source raw values remain untouched; the copied normalized values always start clean.
    FeatureDataUtils.clearIntensityNormalization(copy);
    if (!copy.hasFeatureType(area)) {
      copy.addFeatureType(height);
      copy.addFeatureType(area);
    }
  }

  private void requireCurrent() {
    if (!current.getAsBoolean()) throw new IllegalStateException(
        "The reviewed feature-table snapshot changed; normalization copy was not added.");
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return processed;
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }
}
