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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataWriter {

  private static final Logger logger = Logger.getLogger(ProjectMetadataWriter.class.getName());
  private static final String GNPS_PREFIX = "ATTRIBUTE_";

  private final MetadataTable metadataTable;
  private final MetadataFileFormat format;
  private final List<ProjectMetadataColumnMapping> columnMappings;
  private Map<MetadataColumn<?>, Map<RawDataFile, Object>> data;
  private StringMetadataColumn dataFileCol;
  private List<String> exportTitles;
  private Map<String, ProjectMetadataColumnMapping> mappingsByTarget;

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
    if (metadataTable.getData().isEmpty() && columnMappings.isEmpty()) {
      logger.info("Project metadata is empty, nothing to export");
      return true;
    }

    try (final ICSVWriter csvWriter = CSVParsingUtils.createDefaultWriterAutoDetect(file, "tsv",
        WriterOptions.REPLACE)) {
      data = metadataTable.getData();

      // create the file header
      dataFileCol = metadataTable.createDataFileColumn();
      exportTitles = createExportTitles();
      mappingsByTarget = createMappingsByTarget();

      // write the header down
      if (format == MetadataFileFormat.MZMINE_INTERNAL) {
        writeTypeDescriptions(csvWriter);
      }

      final List<String> parametersTitles = new ArrayList<>();
      for (final String title : exportTitles) {
        parametersTitles.add(formatHeaderTitle(title));
      }
      csvWriter.writeNext(parametersTitles.toArray(String[]::new));
      logger.info("Header was successfully written");

      // write the parameters value down
      for (final RawDataFile rawDataFile : rawDataFiles) {
        final List<String> lineFieldsValues = new ArrayList<>(exportTitles.size());
        for (final String title : exportTitles) {
          lineFieldsValues.add(resolveExportValue(title, rawDataFile));
        }

        csvWriter.writeNext(lineFieldsValues.toArray(String[]::new));
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

  private @NotNull List<String> createExportTitles() {
    final LinkedHashSet<String> titles = new LinkedHashSet<>();
    titles.add(dataFileCol.getTitle());
    data.keySet().stream().map(MetadataColumn::getTitle).forEach(titles::add);
    columnMappings.stream().map(ProjectMetadataColumnMapping::targetColumn)
        .filter(title -> !title.equals(dataFileCol.getTitle())).forEach(titles::add);
    return List.copyOf(titles);
  }

  private @NotNull Map<String, ProjectMetadataColumnMapping> createMappingsByTarget() {
    final Map<String, ProjectMetadataColumnMapping> mappingsByTarget = new LinkedHashMap<>();
    for (final ProjectMetadataColumnMapping mapping : columnMappings) {
      mappingsByTarget.put(mapping.targetColumn(), mapping);
    }
    return mappingsByTarget;
  }

  private @NotNull String formatHeaderTitle(@NotNull final String title) {
    if (format == MetadataFileFormat.GNPS && !title.equals(dataFileCol.getTitle())
        && !title.toLowerCase().endsWith(GNPS_PREFIX.toLowerCase())) {
      return GNPS_PREFIX + title;
    }
    return title;
  }

  /**
   * @return ma
   */
  private @NotNull String resolveExportValue(@NotNull final String title,
      @NotNull final RawDataFile rawDataFile) {
    if (title.equals(dataFileCol.getTitle())) {
      return rawDataFile.getName();
    }

    final ProjectMetadataColumnMapping mapping = mappingsByTarget.get(title);
    if (mapping != null) {
      return resolveMappedValue(mapping, rawDataFile);
    }

    final MetadataColumn<?> column = findMetadataColumn(title);
    return column == null ? "" : metadataValueAsString(column, rawDataFile);
  }

  private @NotNull String resolveMappedValue(@NotNull final ProjectMetadataColumnMapping mapping,
      @NotNull final RawDataFile rawDataFile) {
    if (mapping.sourceColumn().equals(dataFileCol.getTitle())) {
      final String text = rawDataFile.getName();
      return text == null || text.isBlank() ? mapping.defaultValue() : text;
    }

    final MetadataColumn<?> sourceColumn = metadataTable.getColumnByName(mapping.sourceColumn());
    final String text =
        sourceColumn == null ? "" : metadataValueAsString(sourceColumn, rawDataFile);
    return text.isBlank() ? mapping.defaultValue() : text;
  }

  private <T> @Nullable T getMetadataValue(@NotNull final MetadataColumn<T> column,
      @NotNull final RawDataFile rawDataFile) {
    return metadataTable.getValue(column, rawDataFile);
  }

  private @NotNull String metadataValueAsString(@NotNull final MetadataColumn<?> column,
      @NotNull final RawDataFile rawDataFile) {
    final Object value = getMetadataValue(column, rawDataFile);
    return value == null ? "" : value.toString();
  }

  private void writeTypeDescriptions(final @NotNull ICSVWriter writer) {

    final List<String> parametersDescriptions = new ArrayList<>();
    final List<String> parametersTypes = new ArrayList<>();
    for (final String title : exportTitles) {
      final MetadataColumn<?> column =
          title.equals(dataFileCol.getTitle()) ? dataFileCol : findMetadataColumn(title);
      final ProjectMetadataColumnMapping mapping = mappingsByTarget.get(title);
      parametersDescriptions.add(column != null ? column.getDescription()
          : "Mapped from metadata column " + (mapping == null ? "" : mapping.sourceColumn()));
      parametersTypes.add(column != null ? column.getType().toString()
          : new StringMetadataColumn(title).getType().toString());
    }

    writer.writeNext(parametersDescriptions.toArray(String[]::new));
    writer.writeNext(parametersTypes.toArray(String[]::new));
  }

  private @Nullable MetadataColumn<?> findMetadataColumn(@NotNull final String title) {
    return data.keySet().stream().filter(column -> column.getTitle().equals(title)).findFirst()
        .orElse(null);
  }

}
