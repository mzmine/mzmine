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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ProjectMetadataWriter {

  private static final Logger logger = Logger.getLogger(ProjectMetadataWriter.class.getName());

  private final MetadataTable metadataTable;
  private final MetadataFileFormat format;

  public ProjectMetadataWriter(@NotNull final MetadataTable metadata,
      @NotNull final MetadataFileFormat format) {
    this.metadataTable = metadata;
    this.format = format;
  }


  /**
   * @return true if success. also if empty
   */
  public boolean exportTo(File file) {
    if (metadataTable.getData().isEmpty()) {
      logger.info("Project metadata is empty, nothing to export");
      return true;
    }

    try (final ICSVWriter csvWriter = CSVParsingUtils.createDefaultWriterAutoDetect(file, "tsv",
        WriterOptions.REPLACE)) {
      var data = metadataTable.getData();

      // create the file header
      StringMetadataColumn dataFileCol = metadataTable.createDataFileColumn();

      // write the header down
      if (format == MetadataFileFormat.MZMINE_INTERNAL) {
        writeTypeDescriptions(csvWriter, dataFileCol, data);
      }

      String GNPS_PREFIX = "ATTRIBUTE_";

      final List<String> parametersTitles = new ArrayList<>();
      parametersTitles.add(dataFileCol.getTitle());
      for (var column : data.keySet()) {
        var title = column.getTitle();
        if (format == MetadataFileFormat.GNPS && !title.toLowerCase()
            .endsWith(GNPS_PREFIX.toLowerCase())) {
          title = GNPS_PREFIX + title;
        }

        parametersTitles.add(title);
      }
      csvWriter.writeNext(parametersTitles.toArray(String[]::new));
      logger.info("Header was successfully written");

      // write the parameters value down
      RawDataFile[] files = ProjectService.getProject().getDataFiles();
      for (var rawDataFile : files) {
        List<String> lineFieldsValues = new ArrayList<>(List.of(rawDataFile.getName()));
        for (var column : data.entrySet()) {
          // get the parameter value
          // [IMPORTANT] "" will be returned in case if it's unset
          Object value = metadataTable.getValue(column.getKey(), rawDataFile);
          lineFieldsValues.add(value == null ? "" : value.toString());
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

  private void writeTypeDescriptions(final ICSVWriter writer,
      final StringMetadataColumn dataFileCol,
      final Map<MetadataColumn<?>, Map<RawDataFile, Object>> data) {

    final List<String> parametersDescriptions = new ArrayList<>();
    final List<String> parametersTypes = new ArrayList<>();
    parametersDescriptions.add(dataFileCol.getDescription());
    parametersTypes.add(dataFileCol.getType().toString());

    for (var column : data.keySet()) {
      parametersDescriptions.add(column.getDescription());
      parametersTypes.add(column.getType().toString());
    }

    writer.writeNext(parametersDescriptions.toArray(String[]::new));
    writer.writeNext(parametersTypes.toArray(String[]::new));
  }

}
