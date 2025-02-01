/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper to map samples to names and descriptions defined in project metadata columns.
 */
public class ProjectMetadataToLibraryEntryMapper {

  private final @NotNull Map<RawDataFile, Object> nameColumn;
  private final @NotNull Map<RawDataFile, Object> descriptionColumn;

  public ProjectMetadataToLibraryEntryMapper(final @Nullable MetadataColumn<?> nameColumn,
      final @Nullable MetadataColumn<?> descriptionColumn) {

    final var metadata = ProjectService.getMetadata();
    this.nameColumn = nameColumn != null ? metadata.getColumnData(nameColumn) : Map.of();
    this.descriptionColumn =
        descriptionColumn != null ? metadata.getColumnData(descriptionColumn) : Map.of();

    // column was available before - so this should usually be available
    assert this.nameColumn != null;
    assert this.descriptionColumn != null;
  }

  public @Nullable Object getName(final @NotNull RawDataFile raw) {
    return nameColumn.get(raw);
  }

  public @Nullable Object getDescription(final @NotNull RawDataFile raw) {
    return descriptionColumn.get(raw);
  }

  public void addMetadataToEntry(final @NotNull SpectralLibraryEntry entry,
      final @Nullable Scan scan) {
    if (scan == null) {
      return;
    }
    var raws = ScanUtils.streamSourceScans(scan).map(ScanUtils::getDataFile)
        .filter(Objects::nonNull).distinct().sorted().toList();

    putConcatMetadataValues(entry, raws, nameColumn, DBEntryField.NAME);
    putConcatMetadataValues(entry, raws, descriptionColumn, DBEntryField.DESCRIPTION);
  }

  /**
   * Concat all non-null metadata values for raw data files and put as {@link SpectralLibraryEntry}
   * {@link DBEntryField}
   *
   * @param entry          target library entry
   * @param raws           raw files to use
   * @param metadataColumn metadata to search in
   * @param field          the target field to put the resulting concat value in
   */
  private void putConcatMetadataValues(final @NotNull SpectralLibraryEntry entry,
      final List<RawDataFile> raws, final @Nullable Map<RawDataFile, Object> metadataColumn,
      final DBEntryField field) {
    if (metadataColumn == null) {
      return;
    }
    final String concat = raws.stream().map(metadataColumn::get).filter(Objects::nonNull).distinct()
        .map(Object::toString).map(String::trim).sorted().collect(Collectors.joining("; "));
    if (!concat.isBlank()) {
      String originalValue = entry.getAsString(field).orElse(null);

      var combined = Stream.of(concat, originalValue).filter(Objects::nonNull)
          .collect(Collectors.joining("; "));

      entry.putIfNotNull(field, combined);
    }
  }
}
