/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.project.parameterssetup;

import static io.github.mzmine.project.parameterssetup.ProjectMetadataParameters.AvailableTypes.DATETIME;
import static io.github.mzmine.project.parameterssetup.ProjectMetadataParameters.AvailableTypes.DOUBLE;
import static io.github.mzmine.project.parameterssetup.ProjectMetadataParameters.AvailableTypes.TEXT;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.parameterssetup.ProjectMetadataParameters.AvailableTypes;
import io.github.mzmine.project.parameterssetup.columns.DateMetadataColumn;
import io.github.mzmine.project.parameterssetup.columns.DoubleMetadataColumn;
import io.github.mzmine.project.parameterssetup.columns.MetadataColumn;
import io.github.mzmine.project.parameterssetup.columns.StringMetadataColumn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Holds the metadata of a project and represents it as a table (parameters are columns).
 */
public class MetadataTable {

  private final Map<MetadataColumn<?>, Map<RawDataFile, Object>> data;

  // define the header fields names of the file with imported metadata
  private enum HeaderFields {
    NAME, DESC, TYPE, FILE, VALUE
  }

  // we will need HeaderFields enum converted into array
  private final String[] HeaderFieldsArr = Stream.of(HeaderFields.values()).map(Enum::toString)
      .toArray(String[]::new);

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  public MetadataTable() {
    this.data = new HashMap<>();
  }

  public MetadataTable(Map<MetadataColumn<?>, Map<RawDataFile, Object>> data) {
    this.data = data;
  }

  public Map<MetadataColumn<?>, Map<RawDataFile, Object>> getData() {
    return data;
  }

  /**
   * Clear the metadata table up.
   */
  public void clearData() {
    data.clear();
  }

  /**
   * Add new parameter column to the metadata table.
   *
   * @param column new parameter column
   */
  public void addColumn(MetadataColumn<?> column) {
    data.putIfAbsent(column, new HashMap<>());
  }

  /**
   * Remove parameter column from the metadata table.
   *
   * @param column parameter column
   */
  public void removeColumn(MetadataColumn<?> column) {
    data.remove(column);
  }

  /**
   * Remove the all parameters values for a passed file.
   *
   * @param file file for which parameters values should be deleted.
   */
  public void removeFile(RawDataFile file) {
    // iterate through the all parameters and try to delete the parameters
    // values mapped to the passed file
    for (var param : data.keySet()) {
      data.get(param).remove(file);
    }
  }

  /**
   * Is the specified metadata column obtained in the metadata table?
   *
   * @param column project parameter column
   * @return true if it's contained, false otherwise
   */
  public boolean hasColumn(MetadataColumn<?> column) {
    return data.containsKey(column);
  }

  /**
   * Return parameters columns of the metadata table.
   *
   * @return set with the parameters columns
   */
  public Set<MetadataColumn<?>> getColumns() {
    return data.keySet();
  }

  /**
   * Return parameter column with the corresponding parameter name.
   *
   * @param name name of the parameter
   * @return parameterColumn or null in case if the parameter with the passed name isn't obtained in
   * the metadata table
   */
  public MetadataColumn<?> getColumnByName(String name) {
    for (MetadataColumn<?> column : getColumns()) {
      if (column.getTitle().equals(name)) {
        return column;
      }
    }

    return null;
  }

  /**
   * Return parameter value of the corresponding RawData file.
   *
   * @param column      project parameter column
   * @param rawDataFile RawData file
   * @param <T>         type of the project parameter
   * @return parameter value
   */
  public <T> T getValue(MetadataColumn<T> column, RawDataFile rawDataFile) {
    var row = data.get(column);
    if (row != null) {
      return (T) row.get(rawDataFile);
    }

    return null;
  }

  /**
   * Try to set particular value of the parameter of the RawData file. The parameter column will be
   * added in case if it wasn't previously obtained in the table.
   *
   * @param column project parameter column
   * @param file   RawData file
   * @param value  value to be set
   * @param <T>    type of the parameter
   */
  public <T> void setValue(MetadataColumn<T> column, RawDataFile file, T value) {
    if (!data.containsKey(column)) {
      addColumn(column);
    }

    data.get(column).put(file, value);
  }

  /**
   * Export the metadata table using .tsv format.
   * todo: add extra argument defining the format of the exported data (e.g. GNPS or .tsv)
   * File format would be:
   * ====================================================================
   * NAME TYPE DESC FILE VALUE
   * ====================================================================
   * NAME  - parameter name
   * TYPE  - type of the parameter
   * DESC  - description of the parameter
   * FILE  - name of the file to which the parameter belong to
   * VALUE - value of the parameter
   *
   * @param file the file in which exported metadata will be stored
   * @return true if the export was successful, false otherwise
   */
  public boolean exportMetadata(File file) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false))) {
      // in case if there's no metadata to export
      if (data.isEmpty()) {
        logger.info("There's no metadata to export");
        return false;
      }

      // create the .tsv file header
      String header = String.join("\t", HeaderFieldsArr);
      header += System.lineSeparator();
      // write the header down
      bufferedWriter.write(header);
      logger.info("Header was successfully written down");

      // write the parameters value down
      for (var param : data.keySet()) {
        var record = data.get(param);
        for (var rawDataFile : record.keySet()) {
          var paramVal = record.get(rawDataFile);
          // pattern match the parameter type
          AvailableTypes paramType = switch (param) {
            case (StringMetadataColumn stringMetadataColumn) -> TEXT;
            case (DoubleMetadataColumn doubleMetadataColumn) -> DOUBLE;
            case (DateMetadataColumn dateMetadataColumn) -> DATETIME;
          };

          // create the record line
          List<String> lineFieldsValues = new ArrayList<>();
          lineFieldsValues.add(param.getTitle());
          lineFieldsValues.add(param.getDescription());
          lineFieldsValues.add(paramType.toString());
          lineFieldsValues.add(rawDataFile.getName());
          lineFieldsValues.add(paramVal.toString());

          String line = String.join("\t", lineFieldsValues);
          line += System.lineSeparator();
          bufferedWriter.write(line);
        }
      }

      logger.info("The metadata table was successfully exported");
    } catch (FileNotFoundException fileNotFoundException) {
      logger.info("Couldn't open file for metadata export: " + fileNotFoundException.getMessage());
      return false;
    } catch (IOException ioException) {
      logger.info("Error while writing the exported metadata down: " + ioException.getMessage());
      return false;
    }

    return true;
  }

  /**
   * Import the metadata to the metadata table.
   * todo: add extra argument defining the format of the imported data (e.g. GNPS or .tsv)
   *
   * @param file       source of the metadata
   * @param appendMode whether the new metadata should be appended or they should replace the old
   *                   metadata
   * @return true if the metadata were successfully imported, false otherwise
   */
  public boolean importMetadata(File file, boolean appendMode) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
      // create the .tsv file header
      String header = String.join("\t", HeaderFieldsArr);
      // compare the headers
      if (!header.equals(bufferedReader.readLine())) {
        logger.info("Import failed: wrong format of the header");
        return false;
      }
      logger.info("The header corresponds, OK");

      // find the field position in the string
      // it's useful in case if you imagine that the position of a field could be changed,
      // with this implementation no code changes would be necessary
      int namePos = IntStream.range(0, HeaderFieldsArr.length)
          .filter(i -> HeaderFieldsArr[i].equals(HeaderFields.NAME.toString())).findFirst()
          .orElse(-1);
      int typePos = IntStream.range(0, HeaderFieldsArr.length)
          .filter(i -> HeaderFieldsArr[i].equals(HeaderFields.TYPE.toString())).findFirst()
          .orElse(-1);
      int descPos = IntStream.range(0, HeaderFieldsArr.length)
          .filter(i -> HeaderFieldsArr[i].equals(HeaderFields.DESC.toString())).findFirst()
          .orElse(-1);
      int filePos = IntStream.range(0, HeaderFieldsArr.length)
          .filter(i -> HeaderFieldsArr[i].equals(HeaderFields.FILE.toString())).findFirst()
          .orElse(-1);
      int valuePos = IntStream.range(0, HeaderFieldsArr.length)
          .filter(i -> HeaderFieldsArr[i].equals(HeaderFields.VALUE.toString())).findFirst()
          .orElse(-1);
      if (namePos == -1 || typePos == -1 || descPos == -1 || filePos == -1 || valuePos == -1) {
        logger.info("Import failed: bad fields to pos mapping");
        return false;
      }

      // we will need the info about the rawDataFiles to decide whether to import a parameter or not
      // if the parameter structure is normal but there's no such file for it to be mapped to, then
      // we will just skip this parameter
      RawDataFile[] files = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] splitLine = line.split("\t");

        if (splitLine.length != HeaderFieldsArr.length) {
          logger.info("Import failed: wrong number of the fields in line");
          return false;
        }

        // find if there's a rawDataFile corresponding to this parameter in the project
        int rawDataFileInd;
        logger.info(splitLine[filePos] + " " + files.length);
        if ((rawDataFileInd = IntStream.range(0, files.length)
            .filter(i -> files[i].getName().equals(splitLine[filePos])).findFirst().orElse(-1))
            != -1) {
          // match the parameter according to its type
          MetadataColumn parameterMatched = switch (AvailableTypes.valueOf(splitLine[typePos])) {
            case TEXT -> {
              yield new StringMetadataColumn(splitLine[namePos]);
            }
            case DOUBLE -> {
              yield new DoubleMetadataColumn(splitLine[namePos]);
            }
            case DATETIME -> {
              yield new DateMetadataColumn(splitLine[namePos]);
            }
          };
          logger.info("RawDataFile corresponding to this metadata parameter is found");

          // if the parameter value is in the right format then save it to the metadata table,
          // otherwise abort importing
          Object convertedParameterInput = parameterMatched.convert(splitLine[valuePos], null);
          if (parameterMatched.checkInput(convertedParameterInput)) {
            setValue(parameterMatched, files[rawDataFileInd], convertedParameterInput);
          } else {
            logger.info("Import failed: wrong parameter value format");
            return false;
          }
          // todo: need to update the metadata table when removing a RawDataFile
          //       the second option is to add equals() and hashCode() methods
          //       for RawDataFiles; it will work to, but imho the 1. option is better
        }
      }

      logger.info("Metadata table: ");
      getData().forEach((par, value) -> logger.info(par.getTitle() + ":" + value));
    } catch (FileNotFoundException fileNotFoundException) {
      logger.info("Couldn't open file for metadata import: " + fileNotFoundException.getMessage());
      return false;
    } catch (IOException ioException) {
      logger.info("Error while reading the metadata: " + ioException.getMessage());
      return false;
    }

    return true;
  }
}
