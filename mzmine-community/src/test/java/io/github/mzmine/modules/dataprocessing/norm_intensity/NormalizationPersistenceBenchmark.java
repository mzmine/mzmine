/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.io.projectload.version_3_0.FeatureListLoadTask;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;

/**
 * Reproducible persistence measurements for packed row factors and a bounded native feature-list
 * archive round trip. Invoke {@code --large} only on a machine provisioned for the 4 GiB heap cap.
 */
public final class NormalizationPersistenceBenchmark {

  private static final int ARCHIVE_ROWS = 10_000;
  private static final int ARCHIVE_FILES = 4;

  private NormalizationPersistenceBenchmark() {}

  public static void main(final @NotNull String[] args) throws Exception {
    final List<String> arguments = List.of(args);
    if (arguments.contains("--feature-list-archive")) {
      runFeatureListArchive(arguments.contains("--archive-normalized"));
      return;
    }

    final boolean large = arguments.contains("--large");
    final Availability availability = arguments.contains("--all-available")
        ? Availability.ALL_AVAILABLE : Availability.MIXED;
    runParameterRoundTrip(large ? 100_000 : 1_000, large ? 100 : 4, availability);
  }

  private static void runParameterRoundTrip(final int rows, final int files,
      final @NotNull Availability availability) throws Exception {
    resetPeakPools();
    final long constructionStart = System.nanoTime();
    final BenchmarkData data = createSummary(rows, files, availability);
    final long constructed = System.nanoTime();
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary, data.summary());
    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final long saved = System.nanoTime();
    final IntensityNormalizerParameters loaded = new IntensityNormalizerParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);
    final long loadedAt = System.nanoTime();
    if (!data.summary().equals(loaded.getValue(
        IntensityNormalizerParameters.hiddenNormalizationSummary))) {
      throw new IllegalStateException("Persistence roundtrip changed row factors or unavailable reasons.");
    }
    System.out.printf("mode=parameter availability=%s rows=%d files=%d factors=%d unavailable=%d "
            + "nodes=%d xmlBytes=%d gzipXmlBytes=%d constructMs=%.1f saveMs=%.1f loadMs=%.1f "
            + "heapPoolHighWaterSumMiB=%.1f%n", availability, rows, files, data.factors(), data.unavailable(),
        countElements(xml), xml.getBytes(StandardCharsets.UTF_8).length, gzipBytes(xml),
        (constructed - constructionStart) / 1_000_000d, (saved - constructed) / 1_000_000d,
        (loadedAt - saved) / 1_000_000d, heapPoolHighWaterSumBytes() / 1024d / 1024d);
  }

  private static void runFeatureListArchive(final boolean normalized) throws Exception {
    resetPeakPools();
    final long constructionStart = System.nanoTime();
    final MZmineProjectImpl sourceProject = new MZmineProjectImpl();
    final List<RawDataFileImpl> sourceFiles = createRawFiles(sourceProject, ARCHIVE_FILES);
    final ModularFeatureList sourceList = new ModularFeatureList("normalization archive", null,
        sourceFiles.toArray(RawDataFileImpl[]::new));
    for (final RawDataFileImpl sourceFile : sourceFiles) {
      sourceList.setSelectedScans(sourceFile, List.of(sourceFile.getScan(0)));
    }
    for (int rowId = 1; rowId <= ARCHIVE_ROWS; rowId++) {
      final ModularFeatureListRow row = new ModularFeatureListRow(sourceList, rowId);
      for (int fileIndex = 0; fileIndex < sourceFiles.size(); fileIndex++) {
        addFeature(row, sourceList, sourceFiles.get(fileIndex), rowId, fileIndex);
      }
      row.applyRowBindings();
      sourceList.addRow(row);
    }
    sourceList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(RowsFilterModule.class,
        new RowsFilterParameters(), Instant.parse("2026-01-01T00:00:00Z")));
    if (normalized) {
      sourceList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
          IntensityNormalizerModule.class, normalizationParameters(sourceFiles), Instant.now()));
    }
    sourceProject.addFeatureList(sourceList);
    final long constructed = System.nanoTime();

    final Path archive = Files.createTempFile("normalization-persistence-", ".mzmine");
    final long saveStart = System.nanoTime();
    try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive.toFile()))) {
      final FeatureListSaveTask saveTask = new FeatureListSaveTask(sourceList, output);
      saveTask.run();
      requireFinished("save", saveTask.getStatus());
    }
    final long saved = System.nanoTime();
    final long archiveBytes = Files.size(archive);

    final MZmineProjectImpl loadedProject = new MZmineProjectImpl();
    final List<RawDataFileImpl> loadedFiles = createRawFiles(loadedProject, ARCHIVE_FILES);
    final long loadStart = System.nanoTime();
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      final FeatureListLoadTask loadTask = new FeatureListLoadTask(null, loadedProject, zip);
      loadTask.run();
      requireFinished("load", loadTask.getStatus());
    } finally {
      Files.deleteIfExists(archive);
    }
    final long loaded = System.nanoTime();
    final ModularFeatureList loadedList = (ModularFeatureList) loadedProject.getCurrentFeatureLists()
        .getFirst();
    if (loadedList.getNumberOfRows() != ARCHIVE_ROWS) {
      throw new IllegalStateException("Feature-list archive roundtrip changed the row count.");
    }
    final boolean restoredNormalizer = loadedList.getAppliedMethods().stream()
        .anyMatch(method -> method.getModule() instanceof IntensityNormalizerModule);
    if (normalized != restoredNormalizer) {
      throw new IllegalStateException("Feature-list archive roundtrip changed normalization metadata.");
    }
    verifyIntensities(sourceList, sourceFiles);
    verifyIntensities(loadedList, loadedFiles);
    if (normalized) {
      verifyRestoredNormalization(loadedList, loadedFiles);
    }
    System.out.printf("mode=feature-list-archive archive=%s rows=%d files=%d sourceFeatureLists=%d "
            + "loadedFeatureLists=%d archiveBytes=%d constructMs=%.1f saveMs=%.1f loadMs=%.1f "
            + "heapPoolHighWaterSumMiB=%.1f%n", normalized ? "normalized" : "control", ARCHIVE_ROWS,
        ARCHIVE_FILES, sourceProject.getCurrentFeatureLists().size(),
        loadedProject.getCurrentFeatureLists().size(), archiveBytes,
        (constructed - constructionStart) / 1_000_000d, (saved - saveStart) / 1_000_000d,
        (loaded - loadStart) / 1_000_000d, heapPoolHighWaterSumBytes() / 1024d / 1024d);
  }

  private static @NotNull IntensityNormalizerParameters normalizationParameters(
      final @NotNull List<RawDataFileImpl> rawFiles) {
    final List<RawFileNormalizationFunction> functions = new ArrayList<>(rawFiles.size());
    for (int fileIndex = 0; fileIndex < rawFiles.size(); fileIndex++) {
      final Map<Integer, Double> factors = new LinkedHashMap<>(ARCHIVE_ROWS);
      for (int rowId = 1; rowId <= ARCHIVE_ROWS; rowId++) {
        factors.put(rowId, 1d + fileIndex * 1e-6 + rowId * 1e-12);
      }
      functions.add(new RawFileNormalizationFunction(rawFiles.get(fileIndex),
          new FeatureRowNormalizationFunction(factors, Map.of())));
    }
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(functions));
    return parameters;
  }

  private static @NotNull List<RawDataFileImpl> createRawFiles(final @NotNull MZmineProjectImpl project,
      final int fileCount) {
    final List<RawDataFileImpl> files = new ArrayList<>(fileCount);
    for (int fileIndex = 0; fileIndex < fileCount; fileIndex++) {
      final RawDataFileImpl raw = new RawDataFileImpl("sample-" + fileIndex + ".mzML", null, null);
      final SimpleScan scan = new SimpleScan(raw, 1, 1, 0f, null, new double[]{100d},
          new double[]{10d}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
          Range.closed(0d, 2000d));
      raw.addScan(scan);
      project.addFile(raw);
      files.add(raw);
    }
    return files;
  }

  private static void addFeature(final @NotNull ModularFeatureListRow row,
      final @NotNull ModularFeatureList list, final @NotNull RawDataFileImpl raw, final int rowId,
      final int fileIndex) {
    final float intensity = rowId + fileIndex / 10f;
    final ModularFeature feature = new ModularFeature(list, raw, FeatureStatus.DETECTED);
    feature.setArea(intensity);
    feature.setHeight(intensity);
    feature.setMZ(100d + fileIndex);
    feature.setRT(5f);
    row.addFeature(raw, feature, false);
  }

  private static void verifyIntensities(final @NotNull ModularFeatureList list,
      final @NotNull List<RawDataFileImpl> rawFiles) {
    for (final int rowId : List.of(1, ARCHIVE_ROWS)) {
      final ModularFeatureListRow row = (ModularFeatureListRow) list.getRows().get(rowId - 1);
      for (int fileIndex = 0; fileIndex < rawFiles.size(); fileIndex++) {
        final ModularFeature feature = (ModularFeature) row.getFeature(rawFiles.get(fileIndex));
        final float expected = rowId + fileIndex / 10f;
        if (feature == null || Float.compare(feature.getArea(), expected) != 0
            || Float.compare(feature.getHeight(), expected) != 0) {
          throw new IllegalStateException("Feature-list archive roundtrip changed raw intensities.");
        }
      }
    }
  }

  private static void verifyRestoredNormalization(final @NotNull ModularFeatureList list,
      final @NotNull List<RawDataFileImpl> rawFiles) {
    final IntensityNormalizerParameters parameters = (IntensityNormalizerParameters) list
        .getAppliedMethods().stream()
        .filter(method -> method.getModule() instanceof IntensityNormalizerModule)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Normalization metadata was not restored."))
        .getParameters();
    final IntensityNormalizationSummary summary = parameters.getValue(
        IntensityNormalizerParameters.hiddenNormalizationSummary);
    if (summary == null || summary.size() != ARCHIVE_FILES) {
      throw new IllegalStateException("Feature-list archive changed normalization raw-file bindings.");
    }
    for (int fileIndex = 0; fileIndex < ARCHIVE_FILES; fileIndex++) {
      final RawFileNormalizationFunction rawFunction = summary.getRawFileFunction(fileIndex);
      if (!(rawFunction.function() instanceof FeatureRowNormalizationFunction function)
          || !rawFunction.rawDataFilePlaceholder().getName().equals("sample-" + fileIndex + ".mzML")
          || !rawFunction.rawDataFilePlaceholder().matches(rawFiles.get(fileIndex))
          || function.factors().size() != ARCHIVE_ROWS) {
        throw new IllegalStateException("Feature-list archive changed normalization raw-file bindings.");
      }
      for (final int rowId : List.of(1, ARCHIVE_ROWS)) {
        final double expected = 1d + fileIndex * 1e-6 + rowId * 1e-12;
        if (Double.compare(function.factors().getOrDefault(rowId, Double.NaN), expected) != 0) {
          throw new IllegalStateException("Feature-list archive changed normalization row factors.");
        }
      }
    }
  }

  private static @NotNull BenchmarkData createSummary(final int rows, final int files,
      final @NotNull Availability availability) {
    final List<RawFileNormalizationFunction> functions = new ArrayList<>(files);
    long factorCount = 0;
    long unavailableCount = 0;
    for (int file = 0; file < files; file++) {
      final Map<Integer, Double> factors = new LinkedHashMap<>(rows);
      final Map<Integer, String> unavailableReasons = new LinkedHashMap<>();
      for (int row = 1; row <= rows; row++) {
        if (availability == Availability.MIXED && row % 10 == 0) {
          unavailableReasons.put(row, "missing QC");
        } else {
          factors.put(row, 1d + file * 1e-6 + row * 1e-12);
        }
      }
      factorCount += factors.size();
      unavailableCount += unavailableReasons.size();
      functions.add(new RawFileNormalizationFunction(new RawDataFilePlaceholder("file-" + file, null),
          LocalDateTime.of(2026, 1, 1, 0, 0), new FeatureRowNormalizationFunction(factors,
          unavailableReasons)));
    }
    return new BenchmarkData(new IntensityNormalizationSummary(functions), factorCount,
        unavailableCount);
  }

  private static void requireFinished(final @NotNull String operation,
      final @NotNull TaskStatus status) {
    if (status != TaskStatus.FINISHED) {
      throw new IllegalStateException("Feature-list archive " + operation + " did not finish: " + status);
    }
  }

  private static void resetPeakPools() {
    ManagementFactory.getMemoryPoolMXBeans().forEach(MemoryPoolMXBean::resetPeakUsage);
  }

  private static int countElements(final @NotNull String xml) {
    int count = 0;
    for (int index = 0; (index = xml.indexOf('<', index)) >= 0; index++) {
      if (index + 1 < xml.length() && Character.isLetter(xml.charAt(index + 1))) count++;
    }
    return count;
  }

  private static long gzipBytes(final @NotNull String xml) throws Exception {
    try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
      gzip.write(xml.getBytes(StandardCharsets.UTF_8));
      gzip.finish();
      return bytes.size();
    }
  }

  private static long heapPoolHighWaterSumBytes() {
    return ManagementFactory.getMemoryPoolMXBeans().stream()
        .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP)
        .mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
  }

  private enum Availability {
    MIXED, ALL_AVAILABLE
  }

  private record BenchmarkData(@NotNull IntensityNormalizationSummary summary, long factors,
                               long unavailable) {}
}
