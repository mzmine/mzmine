/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import com.opencsv.ICSVWriter;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters.MetadataFileFormat;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataWriter {

  private static final Logger logger = Logger.getLogger(ProjectMetadataWriter.class.getName());
  private static final String GNPS_PREFIX = "ATTRIBUTE_";

  private final MetadataTable metadataTable;
  private final MetadataFileFormat format;
  private final List<ProjectMetadataColumnMapping> columnMappings;

  public ProjectMetadataWriter(@NotNull final MetadataTable metadata,
      @NotNull final MetadataFileFormat format) {
    this(metadata, format, List.of());
  }

  public ProjectMetadataWriter(@NotNull final MetadataTable metadata,
      @NotNull final MetadataFileFormat format,
      @Nullable final List<ProjectMetadataColumnMapping> columnMappings) {
    this.metadataTable = metadata;
    this.format = format;
    this.columnMappings = columnMappings == null ? List.of()
        : columnMappings.stream().filter(ProjectMetadataColumnMapping::isActive).toList();
  }


  /**
   * @return true if success. also if empty
   */
  public boolean exportTo(@NotNull final File file) {
    return exportTo(file, List.of(ProjectService.getProject().getDataFiles()));
  }

  /**
   * @return true if success. also if empty
   */
  public boolean exportTo(@NotNull final File file,
      @NotNull final List<? extends RawDataFile> rawDataFiles) {
    // resolved once so that the type descriptions, the header and all rows use the same columns
    final List<ExportColumn> columns = createExportColumns();
    // the data file column is always there
    if (columns.size() <= 1) {
      logger.info("Project metadata is empty, nothing to export");
      return true;
    }

    try (final ICSVWriter csvWriter = CSVParsingUtils.createDefaultWriterAutoDetect(file, "tsv",
        WriterOptions.REPLACE)) {
      // write the header down
      if (format == MetadataFileFormat.MZMINE_INTERNAL) {
        writeTypeDescriptions(csvWriter, columns);
      }

      final String[] parametersTitles = columns.stream().map(this::formatHeaderTitle)
          .toArray(String[]::new);
      csvWriter.writeNext(parametersTitles);
      logger.info("Header was successfully written");

      // write the parameters value down
      for (final RawDataFile rawDataFile : rawDataFiles) {
        final String[] lineFieldsValues = columns.stream()
            .map(column -> column.values().apply(rawDataFile)).toArray(String[]::new);
        csvWriter.writeNext(lineFieldsValues);
      }

      logger.info("The metadata table was successfully exported");
    } catch (FileNotFoundException fileNotFoundException) {
      logger.severe(
          "Couldn't open file for metadata export: " + fileNotFoundException.getMessage());
      return false;
    } catch (IOException ioException) {
      logger.severe("Error while writing the exported metadata down: " + ioException.getMessage());
      return false;
    }

    return true;
  }

  /**
   * The data file column first, then the metadata columns in the order of
   * {@link MetadataTable#getColumnsSorted()} and finally the mapped columns in the order of the
   * mappings. A mapping replaces the metadata column with the same title at its position.
   */
  private @NotNull List<ExportColumn> createExportColumns() {
    final StringMetadataColumn dataFileCol = metadataTable.createDataFileColumn();
    final List<MetadataColumn<?>> metadataColumns = metadataTable.getColumnsSorted();

    // later mappings to the same target win
    final Map<String, ProjectMetadataColumnMapping> mappingsByTarget = new LinkedHashMap<>();
    for (final ProjectMetadataColumnMapping mapping : columnMappings) {
      if (!mapping.targetColumn().equals(dataFileCol.getTitle())) {
        mappingsByTarget.put(mapping.targetColumn(), mapping);
      }
    }

    final List<ExportColumn> columns = new ArrayList<>();
    columns.add(new ExportColumn(dataFileCol, RawDataFile::getName));
    for (final MetadataColumn<?> column : metadataColumns) {
      if (column.getTitle().equals(dataFileCol.getTitle())) {
        continue;
      }
      final ProjectMetadataColumnMapping mapping = mappingsByTarget.remove(column.getTitle());
      columns.add(mapping != null ? createMappedColumn(mapping, dataFileCol, metadataColumns)
          : new ExportColumn(column, raw -> metadataValueAsString(column, raw)));
    }
    for (final ProjectMetadataColumnMapping mapping : mappingsByTarget.values()) {
      columns.add(createMappedColumn(mapping, dataFileCol, metadataColumns));
    }
    return columns;
  }

  private @NotNull ExportColumn createMappedColumn(
      @NotNull final ProjectMetadataColumnMapping mapping,
      @NotNull final StringMetadataColumn dataFileCol,
      @NotNull final List<MetadataColumn<?>> metadataColumns) {
    final StringMetadataColumn target = new StringMetadataColumn(mapping.targetColumn(),
        "Mapped from metadata column " + mapping.sourceColumn());

    if (mapping.sourceColumn().equals(dataFileCol.getTitle())) {
      return new ExportColumn(target, raw -> orDefault(raw.getName(), mapping));
    }

    final MetadataColumn<?> source = metadataColumns.stream()
        .filter(column -> column.getTitle().equals(mapping.sourceColumn())).findFirst()
        .orElse(null);
    return new ExportColumn(target,
        raw -> orDefault(source == null ? "" : metadataValueAsString(source, raw), mapping));
  }

  private static @NotNull String orDefault(@Nullable final String text,
      @NotNull final ProjectMetadataColumnMapping mapping) {
    return text == null || text.isBlank() ? mapping.defaultValue() : text;
  }

  private @NotNull String formatHeaderTitle(@NotNull final ExportColumn column) {
    final String title = column.definition().getTitle();
    if (format == MetadataFileFormat.GNPS && !title.equals(MetadataColumn.FILENAME_HEADER)
        && !title.toLowerCase().endsWith(GNPS_PREFIX.toLowerCase())) {
      return GNPS_PREFIX + title;
    }
    return title;
  }

  private @NotNull String metadataValueAsString(@NotNull final MetadataColumn<?> column,
      @NotNull final RawDataFile rawDataFile) {
    // [IMPORTANT] "" will be returned in case if it's unset
    final Object value = metadataTable.getValue(column, rawDataFile);
    return value == null ? "" : value.toString();
  }

  private void writeTypeDescriptions(final @NotNull ICSVWriter writer,
      final @NotNull List<ExportColumn> columns) {
    writer.writeNext(columns.stream().map(column -> column.definition().getDescription())
        .toArray(String[]::new));
    writer.writeNext(columns.stream().map(column -> column.definition().getType().toString())
        .toArray(String[]::new));
  }

  /**
   * A column of the exported file.
   *
   * @param definition title, description and type of the column
   * @param values     the exported text for a raw data file, empty if unset
   */
  private record ExportColumn(@NotNull MetadataColumn<?> definition,
                              @NotNull Function<RawDataFile, String> values) {

  }

}
