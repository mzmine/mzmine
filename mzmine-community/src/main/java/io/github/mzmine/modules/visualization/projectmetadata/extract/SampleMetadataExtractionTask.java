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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractRawDataFileTask;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Extracts sample metadata columns from the file names or paths of the selected raw data files
 * using user-defined regular expressions and writes them into the project {@link MetadataTable}.
 */
public class SampleMetadataExtractionTask extends AbstractRawDataFileTask {

  private static final Logger logger = Logger.getLogger(
      SampleMetadataExtractionTask.class.getName());

  private final List<RawDataFile> raws;
  private final List<MetadataRegexMapping> mappings;
  private final boolean overwrite;

  public SampleMetadataExtractionTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass, @NotNull final RawDataFile[] raws) {
    this(moduleCallDate, parameters, moduleClass, raws,
        parameters.getValue(SampleMetadataExtractionParameters.mappings),
        parameters.getValue(SampleMetadataExtractionParameters.overwrite));
  }

  /**
   * Explicit-values constructor for callers that provide the mappings and files directly (e.g. the
   * embedded use in raw data import via {@link SampleMetadataExtractionEmbeddedParameters}).
   */
  public SampleMetadataExtractionTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass, @NotNull final RawDataFile[] raws,
      @NotNull final List<MetadataRegexMapping> mappings, final boolean overwrite) {
    super(null, moduleCallDate, parameters, moduleClass);
    this.raws = List.of(raws);
    this.mappings = mappings;
    this.overwrite = overwrite;
  }

  @Override
  protected void process() {
    final MetadataTable metadata = ProjectService.getMetadata();
    totalItems = mappings.size();

    for (final MetadataRegexMapping mapping : mappings) {
      if (isCanceled()) {
        return;
      }
      applyMapping(metadata, mapping);
      incrementFinishedItems();
    }
  }

  private void applyMapping(@NotNull final MetadataTable metadata,
      @NotNull final MetadataRegexMapping mapping) {
    final String columnName = mapping.columnName();
    if (columnName.isBlank()) {
      return;
    }

    // 1. extract one string value per file
    final String[] values = new String[raws.size()];
    for (int i = 0; i < raws.size(); i++) {
      final String input = SampleMetadataExtractionUtils.inputString(raws.get(i), mapping);
      values[i] = SampleMetadataExtractionUtils.extractValue(mapping, input);
    }

    // 2. resolve the target column and convert the extracted strings to its type
    final MetadataColumn<?> existing = metadata.getColumnByName(columnName);
    final Object[] typed = new Object[raws.size()];
    final MetadataColumn<?> column;

    // decision: reuse an existing column (and its type); never write to the synthetic filename column
    if (existing != null && !MetadataColumn.FILENAME_HEADER.equalsIgnoreCase(columnName)) {
      column = existing;
      for (int i = 0; i < values.length; i++) {
        typed[i] = values[i] == null ? null : existing.convertOrElse(values[i], null);
      }
    } else {
      AvailableTypes type = mapping.type().toAvailableType();
      if (type == null) {
        // AUTO: detect the type from all extracted values of the whole column
        final String[] detect = Arrays.stream(values).map(v -> v == null ? "" : v)
            .toArray(String[]::new);
        type = AvailableTypes.castToMostAppropriateType(detect, new Object[detect.length]);
      }
      final MetadataColumn<?> created = MetadataColumn.forType(type, columnName);
      for (int i = 0; i < values.length; i++) {
        typed[i] = values[i] == null ? null : created.convertOrElse(values[i], null);
      }
      // do not create an empty column if nothing matched
      if (Arrays.stream(typed).allMatch(Objects::isNull)) {
        logger.info(
            () -> "Sample metadata extraction: no values extracted for column '" + columnName
                + "'. Skipping column.");
        return;
      }
      column = created;
    }

    // 3. write the values, respecting the overwrite option
    for (int i = 0; i < raws.size(); i++) {
      final Object value = typed[i];
      if (value == null) {
        continue; // no match and no default - never clear existing values
      }
      final RawDataFile raw = raws.get(i);
      if (!overwrite && metadata.getValue(column, raw) != null) {
        continue; // keep existing value
      }
      setValueUnchecked(metadata, column, raw, value);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void setValueUnchecked(@NotNull final MetadataTable metadata,
      @NotNull final MetadataColumn column, @NotNull final RawDataFile raw,
      @NotNull final Object value) {
    metadata.setValue(column, raw, value);
  }

  @Override
  public String getTaskDescription() {
    return "Extracting sample metadata from file names of " + raws.size() + " raw data files";
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return raws;
  }
}
