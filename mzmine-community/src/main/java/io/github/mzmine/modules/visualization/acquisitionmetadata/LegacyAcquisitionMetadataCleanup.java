/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.visualization.acquisitionmetadata;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/** Removes only legacy columns that are provably unchanged duplicates of acquisition metadata. */
public final class LegacyAcquisitionMetadataCleanup {

  private static final String IMPORTED_DESCRIPTION =
      "Declared source metadata; may contain sample/study information";
  private static final String ACQUISITION_DESCRIPTION =
      "Declared in imported file header; normalized PSI-MS controlled terms";

  private LegacyAcquisitionMetadataCleanup() {
  }

  public static void cleanupCurrentProject() {
    cleanup(ProjectService.getMetadata(), ProjectService.getProject().getCurrentRawDataFiles());
  }

  static void cleanup(final @NotNull MetadataTable table, final @NotNull List<RawDataFile> files) {
    if (files.isEmpty()) {
      return;
    }
    final Map<String, LegacyColumn> expected = expectedColumns(files);
    final List<MetadataColumn<?>> removable = new ArrayList<>();
    for (final MetadataColumn<?> column : table.getColumns()) {
      final LegacyColumn candidate = expected.get(column.getTitle());
      if (candidate != null && column instanceof StringMetadataColumn
          && candidate.description().equals(column.getDescription())
          && exactlyMatches(table.getColumnData(column), files, candidate.values())) {
        removable.add(column);
      }
    }
    if (!removable.isEmpty()) {
      table.removeColumns(removable);
    }
  }

  private static @NotNull Map<String, LegacyColumn> expectedColumns(
      final @NotNull Collection<RawDataFile> files) {
    final Map<String, LegacyColumn> expected = new HashMap<>();
    final List<String> headerFields = files.stream().flatMap(
        file -> file.getAcquisitionMetadata().localFields().keySet().stream()).distinct().toList();
    for (final String field : headerFields) {
      expected.put("Imported: " + field, new LegacyColumn(IMPORTED_DESCRIPTION,
          file -> file.getAcquisitionMetadata().localFields().get(field)));
    }
    for (final AcquisitionMetadata.Field field : AcquisitionMetadata.Field.values()) {
      final String title = "Acquisition: " + field.name().toLowerCase(java.util.Locale.ROOT)
          .replace('_', ' ');
      expected.put(title, new LegacyColumn(ACQUISITION_DESCRIPTION, file -> file
          .getAcquisitionMetadata().terms().stream().filter(term -> term.field() == field)
          .map(term -> term.label() + " [" + term.accession() + "]").distinct().sorted()
          .collect(java.util.stream.Collectors.joining("; "))));
    }
    expected.put("Measured: MS levels", measured("Derived from imported scans",
        file -> Arrays.toString(file.getMSLevels())));
    expected.put("Measured: polarity", measured("Derived from imported scans",
        file -> String.valueOf(file.getDataPolarity())));
    expected.put("Measured: spectrum type", measured("Derived from imported scans",
        file -> String.valueOf(file.getSpectraType())));
    expected.put("Measured: scan count", measured("Number of imported scans",
        file -> Integer.toString(file.getNumOfScans())));
    expected.put("Measured: RT range (min)", measured("Derived from imported scans", file ->
        file.getNumOfScans() > 0 ? file.getDataRTRange().toString() : null));
    expected.put("Measured: m/z range", measured("Derived from imported scans", file ->
        file.getNumOfScans() > 0 ? file.getDataMZRange().toString() : null));
    return expected;
  }

  private static @NotNull LegacyColumn measured(final @NotNull String description,
      final @NotNull Function<RawDataFile, String> values) {
    return new LegacyColumn(description, values);
  }

  private static boolean exactlyMatches(final Map<RawDataFile, Object> actual,
      final List<RawDataFile> files, final Function<RawDataFile, String> expected) {
    if (actual == null || actual.size() != files.size() || !files.containsAll(actual.keySet())) {
      return false;
    }
    return files.stream().allMatch(file -> expected.apply(file) != null
        && expected.apply(file).equals(actual.get(file)));
  }

  private record LegacyColumn(@NotNull String description,
                              @NotNull Function<RawDataFile, String> values) {
  }
}
