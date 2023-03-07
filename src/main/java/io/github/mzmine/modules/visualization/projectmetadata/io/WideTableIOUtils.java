/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.FILENAME_HEADER;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class WideTableIOUtils implements TableIOUtils {

  private static final Logger logger = Logger.getLogger(MetadataTable.class.getName());

  private final MetadataTable metadataTable;

  // define the header fields names of the file with imported metadata
  private enum HeaderFields {
    TITLE, DESC, TYPE
  }

  public WideTableIOUtils(MetadataTable metadataTable) {
    this.metadataTable = metadataTable;
  }

  /**
   * File header would be: ============================================ Title         |  Datafile  |
   * Path        | Description   |  file name |  file path  | ... Type          |  TEXT      | TEXT
   * | -------------------------------------------- a.mzML        ../../a.mzML b.mzML ../../b.mzML
   * ... ============================================
   *
   * @param file the file in which exported metadata will be stored
   * @return true if the export was successful, false otherwise
   */
  @Override
  public boolean exportTo(File file) {
    try (FileWriter fw = new FileWriter(file,
        false); BufferedWriter bufferedWriter = new BufferedWriter(fw)) {

      var data = metadataTable.getData();

      // in case if there's no metadata to export
      if (data.isEmpty()) {
        logger.warning("There's no metadata to export");
        return false;
      }

      // create the .tsv file header
      StringMetadataColumn dataFileCol = new StringMetadataColumn(FILENAME_HEADER, "");
      List<String> parametersTitles = new ArrayList<>(
          List.of(HeaderFields.TITLE.toString(), dataFileCol.getTitle()));
      List<String> parametersDescriptions = new ArrayList<>(
          List.of(HeaderFields.DESC.toString(), dataFileCol.getDescription()));
      List<String> parametersTypes = new ArrayList<>(
          List.of(HeaderFields.TYPE.toString(), dataFileCol.getType().toString()));
      for (var column : data.keySet()) {
        parametersTitles.add(column.getTitle());
        parametersDescriptions.add(column.getDescription());
        parametersTypes.add(column.getType().toString());
      }

      // write the header down
      bufferedWriter.write(String.join("\t", parametersDescriptions));
      bufferedWriter.newLine();
      bufferedWriter.write(String.join("\t", parametersTypes));
      bufferedWriter.newLine();
      bufferedWriter.write(String.join("\t", parametersTitles));
      bufferedWriter.newLine();
      logger.info("Header was successfully written down");

      // write the parameters value down
      RawDataFile[] files = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
      for (var rawDataFile : files) {
        List<String> lineFieldsValues = new ArrayList<>(List.of("", rawDataFile.getName()));
        for (var column : data.entrySet()) {
          // get the parameter value
          // [IMPORTANT] "" will be returned in case if it's unset
          Object value = metadataTable.getValue(column.getKey(), rawDataFile);
          lineFieldsValues.add(value == null ? "" : value.toString());
        }
        String line = String.join("\t", lineFieldsValues);
        line += System.lineSeparator();

        bufferedWriter.write(line);
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

  private static boolean anyConversionError(final String[] titles, final AvailableTypes[] dataTypes,
      final Object[][] convertedData) {
    boolean anyError = false;
    for (int i = 0; i < convertedData.length; i++) {
      if (convertedData[i] == null) {
        String type = Objects.toString(dataTypes[i], "null");
        logger.warning("Data for column " + titles[i] + " could not be converted to type " + type);
        anyError = true;
      }
    }
    return anyError;
  }

  @Override
  public boolean importFrom(File file, final boolean skipColOnError) {
    // different file formats are supported.
    // see test/resources/metadata
    // first two lines are optional (description / type) otherwise try to cast to type
    final String sep = file.getName().endsWith(".csv") ? "," : "\t";

    // titles is always considered the last row before data
    String[] titles = null;
    String[] descriptions = null;
    AvailableTypes[] dataTypes = null;

    try (FileReader fr = new FileReader(file); BufferedReader reader = new BufferedReader(fr)) {
      // read the header
      String line;
      while (titles == null && (line = reader.readLine()) != null) {
        // split with trailing empty strings removed
        var cells = line.split(sep, 0);
        if (cells.length == 0) {
          continue;
        }
        if (FILENAME_HEADER.equalsIgnoreCase(cells[0])) {
          titles = cells;
        } else if (dataTypes == null) {
          // try to map to types otherwise use as description
          dataTypes = AvailableTypes.tryMap(cells);
          if (dataTypes == null && descriptions == null) {
            descriptions = cells;
          }
        }
      }

      // found header?
      if (titles == null) {
        return false;
      }

      // empty header entry?
      if (Arrays.stream(titles).anyMatch(String::isEmpty)) {
        logger.severe("Could not load wide format table. A header title was empty.");
        return false;
      }

      //
      if (dataTypes != null && dataTypes.length != titles.length) {
        logger.severe(
            "Could not load wide format table. The type definition and titles need to have the same number of entries.");
        return false;
      }

      logger.info("The header size & format correspond, OK");
      // represents the names of the RawDataFiles
      var columnData = CSVParsingUtils.readDataMapToColumns(reader, sep);
      final Object[][] convertedData;

      if (dataTypes != null) {
        // map data to data types
        convertedData = mapDataToTypes(dataTypes, columnData);
      } else {
        // find the best data type and map values
        // if all values are numbers -> parse as double
        // if all are dates -> parse as dates
        // otherwise use string
        dataTypes = new AvailableTypes[columnData.length];
        convertedData = findAndMapDataTypes(dataTypes, columnData);
      }

      // check if col data is null
      if (skipColOnError) {
        // with skipColOnError the converted column is null and we use the original string column and data type
        for (int col = 1; col < convertedData.length; col++) {
          if (convertedData[col] == null) {
            convertedData[col] = columnData[col];
            dataTypes[col] = AvailableTypes.TEXT;
          }
        }
      } else if (anyConversionError(titles, dataTypes, convertedData)) {
        // end because of conversion error
        logger.severe("Stopped metadata import because of data type incompatibility");
        return false;
      }

      //
      descriptions = Objects.requireNonNullElse(descriptions, new String[titles.length]);
      // the matched parameters columns
      MetadataColumn[] columns = getMetadataColumns(titles, descriptions, dataTypes);

      // first column is raw data file names
      String[] fileNames = columnData[0];
      Map<String, RawDataFile> raws = new HashMap<>(fileNames.length);
      for (String name : fileNames) {
        raws.put(name, MZmineCore.getProject().getDataFileByName(name));
      }

      // finally add data to the columns: start at 1 after data files column
      for (int col = 1; col < columns.length; col++) {
        MetadataColumn column = columns[col];
        Object[] colData = convertedData[col];

        for (int row = 0; row < colData.length; row++) {
          var rawFile = raws.get(fileNames[row]);
          // not all raw data files might be imported - but more covered in the metadata sheet
          if (rawFile != null) {
            metadataTable.setValue(column, rawFile, colData[row]);
          }
        }
      }

      logger.info("Metadata table: ");
      metadataTable.getData().forEach((par, value) -> logger.info(par.getTitle() + ":" + value));
    } catch (FileNotFoundException fileNotFoundException) {
      logger.severe(
          "Couldn't open file for metadata import: " + fileNotFoundException.getMessage());
      return false;
    } catch (IOException ioException) {
      logger.severe("Error while reading the metadata: " + ioException.getMessage());
      return false;
    }
    return true;
  }

  @NotNull
  private MetadataColumn[] getMetadataColumns(final String[] titles, final String[] descriptions,
      final AvailableTypes[] dataTypes) {
    var metadataColumns = new MetadataColumn[titles.length];
    // try to find existing column first otherwise
    // create a column instance according to the parameter type
    for (int i = 0; i < titles.length; i++) {
      AvailableTypes type = dataTypes[i];
      String title = titles[i];
      String description = descriptions[i];
      metadataColumns[i] = metadataTable.getColumnByName(title);
      if (metadataColumns[i] == null) {
        metadataColumns[i] = MetadataColumn.forType(type, title, description);
      }
    }
    return metadataColumns;
  }

  /**
   * Try to map String data to dates and numbers, if successful change the column type and data.
   * Otherwise, keep the string type.
   *
   * @param dataTypes  an empty array of column data types - will be filled by this method
   * @param columnData the data in columns
   * @return the data mapped to their new types
   */
  private Object[][] findAndMapDataTypes(final AvailableTypes[] dataTypes,
      final String[][] columnData) {
    Object[][] data = new Object[dataTypes.length][];
    for (int col = 0; col < dataTypes.length; col++) {
      String[] column = columnData[col];
      Object[] converted = new Object[column.length];
      // set datatype to input array
      dataTypes[col] = AvailableTypes.castToMostAppropriateType(column, converted);
      data[col] = converted;
    }
    return data;
  }

  /**
   * Map columns data to their types
   *
   * @param dataTypes  column types
   * @param columnData data
   * @return same size arrays with the types
   */
  private Object[][] mapDataToTypes(final AvailableTypes[] dataTypes, final String[][] columnData) {
    Object[][] data = new Object[dataTypes.length][];
    for (int col = 0; col < dataTypes.length; col++) {
      String[] column = columnData[col];
      AvailableTypes dataType = dataTypes[col];
      // might be null on error in this column - this needs to be checked in the caller
      data[col] = dataType.tryCastType(column);
    }
    return data;
  }
}
