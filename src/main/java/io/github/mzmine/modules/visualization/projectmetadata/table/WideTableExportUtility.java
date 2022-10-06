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

package io.github.mzmine.modules.visualization.projectmetadata.table;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class WideTableExportUtility implements TableExportUtility {

  private static final Logger logger = Logger.getLogger(MetadataTable.class.getName());

  private final MetadataTable metadataTable;

  // define the header fields names of the file with imported metadata
  private enum HeaderFields {
    TITLE, DESC, TYPE
  }

  public WideTableExportUtility(MetadataTable metadataTable) {
    this.metadataTable = metadataTable;
  }

  /**
   * File header would be:
   * ============================================
   * Title         |  Datafile  | Path        |
   * Description   |  file name |  file path  | ...
   * Type          |  TEXT      | TEXT        |
   * --------------------------------------------
   *                a.mzML        ../../a.mzML
   *                b.mzML        ../../b.mzML
   * ...
   * ============================================
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
      StringMetadataColumn dataFileCol = new StringMetadataColumn("Datafile", "File name");
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

  @Override
  public boolean importFrom(File file, boolean appendMode) {
    try (FileReader fr = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(
        fr)) {
      // read the header
      List<String> parametersDescriptions = new ArrayList<>(
          List.of(bufferedReader.readLine().split("\t", -1)));
      List<String> parametersTypes = new ArrayList<>(
          List.of(bufferedReader.readLine().split("\t", -1)));
      List<String> parametersTitles = new ArrayList<>(
          List.of(bufferedReader.readLine().split("\t", -1)));

      // represents the names of the RawDataFiles
      StringMetadataColumn dataFileCol = new StringMetadataColumn("Datafile", "File name");

      // compare the headers
      if (!parametersTitles.get(0).equals(HeaderFields.TITLE.toString())
          || !parametersDescriptions.get(0).equals(HeaderFields.DESC.toString())
          || !parametersTypes.get(0).equals(HeaderFields.TYPE.toString())
          || !parametersTitles.get(1).equals(dataFileCol.getTitle())
          || !parametersDescriptions.get(1).equals(dataFileCol.getDescription())
          || !parametersTypes.get(1).equals(dataFileCol.getType().toString())
          || parametersTitles.size() != parametersDescriptions.size()
          || parametersTitles.size() != parametersTypes.size()) {
        logger.severe("Import failed: wrong format of the header");
        return false;
      }
      logger.info("The header size & format correspond, OK");

      // the matched parameters columns
      MetadataColumn[] metadataColumns = new MetadataColumn[parametersTitles.size() - 2];
      // create a column instance according to the parameter type
      for (int i = 2; i < parametersTitles.size(); i++) {
        metadataColumns[i - 2] = MetadataColumn.forType(
            AvailableTypes.valueOf(parametersTypes.get(i)), parametersTitles.get(i),
            parametersDescriptions.get(i));
      }

      // we will need the info about the rawDataFiles to decide whether to import the parameters or not
      // if the parameter structure is normal but there's no such file for it to be mapped to, then
      // we will just skip this parameter
      RawDataFile[] files = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] splitLine = line.split("\t", -1);

        if (splitLine.length != parametersTitles.size()) {
          logger.severe("Import failed: wrong number of the fields in line");
          return false;
        }

        // find if there's a rawDataFile corresponding to this parameter in the project
        int rawDataFileInd;
        if ((rawDataFileInd = IntStream.range(0, files.length)
            .filter(i -> files[i].getName().equals(splitLine[1])).findFirst().orElse(-1)) != -1) {

          for (int i = 2; i < parametersTitles.size(); i++) {
            // if the parameter value is in the right format then save it to the metadata table,
            // otherwise abort importing
            Object convertedParameterInput = metadataColumns[i - 2].convert(splitLine[i],
                metadataColumns[i - 2].defaultValue());
            if (metadataColumns[i - 2].checkInput(convertedParameterInput)) {
              metadataTable.setValue(metadataColumns[i - 2], files[rawDataFileInd],
                  convertedParameterInput);
            } else if (!splitLine[i].isEmpty()) {
              // if neither parameter value is unset nor it has valid structure
              logger.severe("Parameter import failed: wrong parameter value format");
              return false;
            }
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
    } catch (NullPointerException nullPointerException) {
      logger.severe("The file for metadata import has bad format (not enough lines for a header).");
      return false;
    }

    return true;
  }
}
