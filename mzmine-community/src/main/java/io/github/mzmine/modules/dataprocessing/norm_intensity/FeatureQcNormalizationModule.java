package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.parameters.ParameterSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import org.jetbrains.annotations.NotNull;

/** Fits feature-specific QC corrections from the native task's prepared batch inputs. */
public final class FeatureQcNormalizationModule implements NormalizationTypeModule {

  @Override public @NotNull String getName() { return "QC drift correction"; }
  @Override public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FeatureQcNormalizationParameters.class;
  }

  @Override
  public void createAllNormalizationFunctionsToSummary(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final ModularFeatureList featureList, @NotNull final SamplesBatch batch,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleParameters) {
    createAllNormalizationFunctionsToSummary(summary, featureList, List.of(batch), metadata,
        mainParameters, moduleParameters, () -> false, ignored -> { });
  }

  public void createAllNormalizationFunctionsToSummary(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final ModularFeatureList featureList, @NotNull final List<SamplesBatch> batches,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleParameters, @NotNull final BooleanSupplier canceled,
      @NotNull final IntConsumer completedFiles) {
    final var settings = ((FeatureQcNormalizationParameters) moduleParameters).settings();
    final var abundance = mainParameters.getValue(IntensityNormalizerParameters.featureMeasurementType);
    if (abundance == null || abundance.isNormalized()) {
      throw new IllegalArgumentException("Feature-QC requires raw area or height as its modeled measure.");
    }
    final List<RawDataFile> files = List.copyOf(featureList.getRawDataFiles());
    final Map<RawDataFile, String> batchIds = new LinkedHashMap<>();
    for (int index = 0; index < batches.size(); index++) {
      final SamplesBatch batch = batches.get(index);
      final String batchId = "Batch " + (index + 1) + " (" + batch.getGroupMetadataValueStr() + ")";
      for (final RawDataFile raw : batch.getRaws()) {
        if (batchIds.put(raw, batchId) != null) {
          throw new IllegalArgumentException("Each file must belong to exactly one QC batch.");
        }
      }
    }
    final var referenceFilter = moduleParameters.getValue(FeatureQcNormalizationParameters.sampleTypes);
    final var references = files.stream().filter(file -> referenceFilter.matches(metadata, file)).toList();
    final List<LocalDateTime> dates = new ArrayList<>();
    for (final RawDataFile file : files) {
      checkCanceled(canceled);
      final LocalDateTime date = MetadataTableUtils.getRunDate(metadata, file);
      if (date == null) throw new IllegalArgumentException("Acquisition timestamp missing for " + file.getName());
      dates.add(date);
    }
    final LocalDateTime origin = dates.stream().min(LocalDateTime::compareTo).orElseThrow();
    final List<FeatureQcDriftNormalization.Sample> samples = new ArrayList<>();
    for (int index = 0; index < files.size(); index++) {
      final RawDataFile raw = files.get(index);
      final Duration elapsed = Duration.between(origin, dates.get(index));
      samples.add(new FeatureQcDriftNormalization.Sample("s" + (index + 1), batchIds.get(raw),
          elapsed.getSeconds() + elapsed.getNano() / 1_000_000_000d, references.contains(raw)));
    }
    final Map<Integer, Map<String, Double>> values = new LinkedHashMap<>();
    for (final var row : featureList.getRows()) {
      checkCanceled(canceled);
      final Map<String, Double> rowValues = new LinkedHashMap<>();
      if (row.getID() <= 0 || values.put(row.getID(), rowValues) != null) {
        throw new IllegalArgumentException("Feature-QC requires unique positive row IDs.");
      }
      for (int index = 0; index < files.size(); index++) {
        checkCanceled(canceled);
        final var raw = files.get(index);
        final var feature = row.getFeature(raw);
        if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) continue;
        final Float measured = abundance == io.github.mzmine.datamodel.AbundanceMeasure.Area
            ? feature.getArea() : feature.getHeight();
        if (measured == null || !Float.isFinite(measured) || measured <= 0f) continue;
        final var preceding = summary.get(raw);
        final var factor = preceding == null ? java.util.OptionalDouble.of(1d)
            : preceding.getNormalizationFactor(row, feature.getMZ(), feature.getRT());
        if (factor.isEmpty()) continue;
        final double effective = measured.doubleValue() * factor.getAsDouble();
        if (Double.isFinite(effective) && effective > 0d) rowValues.put(samples.get(index).id(), effective);
      }
    }
    final FeatureQcResult result = FeatureQcDriftNormalization.calculate(samples, values, settings, canceled);
    for (int index = 0; index < files.size(); index++) {
      checkCanceled(canceled);
      final String sampleId = samples.get(index).id();
      final Map<Integer, Double> factors = new LinkedHashMap<>();
      final Map<Integer, String> unavailable = new LinkedHashMap<>();
      for (final int rowId : values.keySet()) {
        checkCanceled(canceled);
        final Double factor = result.rowFactors().getOrDefault(rowId, Map.of()).get(sampleId);
        if (factor != null) factors.put(rowId, factor);
        else {
          final var reason = result.unavailableReason(rowId, sampleId);
          if (reason == null) throw new IllegalStateException("Incomplete feature-QC result.");
          unavailable.put(rowId, reason.name());
        }
      }
      summary.addMergeFunction(files.get(index), new FeatureRowNormalizationFunction(factors, unavailable));
      completedFiles.accept(1);
    }
    // decision: never populate SamplesBatch scalar reference levels for feature-QC.
    summary.featureQc().add(new FeatureQcProvenance(abundance.name(), result.diagnostics()));
    if (result.availability().warning() != null) {
      summary.messages().add("QC warning: " + result.availability().warning());
    }
    final long skippedBatches = result.diagnostics().rows().values().stream()
        .flatMap(row -> row.batches().values().stream())
        .filter(batch -> batch.unavailableReason() != null).count();
    if (skippedBatches > 0) {
      summary.messages().add("QC warning: " + skippedBatches
          + " feature/batch combinations are unavailable; supported batches remain corrected. "
          + "Review QC diagnostics for coverage and reasons.");
    }
    summary.messages().add(settings.description() + ": " + result.availability());
  }

  private static void checkCanceled(@NotNull final BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
  }
}
