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
import java.util.stream.Stream;

/**
 * According to the current implementation parameter would be imported only in case if
 * value for it is set at least for one of the RawDataFiles
 *
 * [IMPORTANT] "" would be treated as a valid value for the Text parameters, whilst for
 * the other types of parameters this value would be invalid
 */
public class LongTableExportUtility implements TableExportUtility {

  private static final Logger logger = Logger.getLogger(MetadataTable.class.getName());

  private final MetadataTable metadataTable;

  // define the header fields names of the file with imported metadata
  private enum HeaderFields {
    NAME, DESC, TYPE, FILE, VALUE
  }

  public LongTableExportUtility(MetadataTable metadataTable) {
    this.metadataTable = metadataTable;
  }

  /**
   * ====================================================================
   * NAME  - parameter name
   * DESC  - description of the parameter
   * FILE  - name of the file to which the parameter belong to
   * VALUE - value of the parameter
   *
   * @param file the file in which exported metadata will be stored
   * @return true if the export was successful, false otherwise
   */
  @Override
  public boolean exportTo(File file) {
    try (FileWriter fw = new FileWriter(file,
        false); BufferedWriter bufferedWriter = new BufferedWriter(fw)) {
      // we will need HeaderFields enum converted into array
      String[] HeaderFieldsArr = Stream.of(HeaderFields.values()).map(Enum::toString)
          .toArray(String[]::new);

      var data = metadataTable.getData();

      // in case if there's no metadata to export
      if (data.isEmpty()) {
        logger.warning("There's no metadata to export");
        return false;
      }

      // create the .tsv file header
      String header = String.join("\t", HeaderFieldsArr);
      header += System.lineSeparator();
      // write the header down
      bufferedWriter.write(header);
      logger.info("Header was successfully written down");

      // write the parameters value down
      for (var column : data.entrySet()) {
        for (var rawDataFile : column.getValue().keySet()) {
          var cellVal = column.getValue().get(rawDataFile);
          // get parameter type
          AvailableTypes paramType = column.getKey().getType();

          // create the record line
          List<String> lineFieldsValues = new ArrayList<>();
          lineFieldsValues.add(column.getKey().getTitle());
          lineFieldsValues.add(column.getKey().getDescription());
          lineFieldsValues.add(paramType.toString());
          lineFieldsValues.add(rawDataFile.getName());
          lineFieldsValues.add(cellVal.toString());

          String line = String.join("\t", lineFieldsValues);
          line += System.lineSeparator();
          bufferedWriter.write(line);
        }
      }

      logger.info("The metadata table was successfully exported");
    } catch (FileNotFoundException fileNotFoundException) {
      logger.severe("Couldn't open file for metadata export: " + fileNotFoundException.getMessage());
      return false;
    } catch (IOException ioException) {
      logger.severe("Error while writing the exported metadata down: " + ioException.getMessage());
      return false;
    }

    return true;
  }

  @Override
  public boolean importFrom(File file, boolean appendMode) {
    try  (FileReader fr = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(
        fr)) {
      // we will need HeaderFields enum converted into array
      String[] HeaderFieldsArr = Stream.of(HeaderFields.values()).map(Enum::toString)
          .toArray(String[]::new);

      // create the .tsv file header
      String header = String.join("\t", HeaderFieldsArr);
      // compare the headers
      if (!header.equals(bufferedReader.readLine())) {
        logger.severe("Import failed: wrong format of the header");
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
        logger.severe("Import failed: bad fields to pos mapping");
        return false;
      }

      // we will need the info about the rawDataFiles to decide whether to import a parameter or not
      // if the parameter structure is normal but there's no such file for it to be mapped to, then
      // we will just skip this parameter
      RawDataFile[] files = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] splitLine = line.split("\t", -1);

        if (splitLine.length != HeaderFieldsArr.length) {
          logger.severe("Import failed: wrong number of the fields in line");
          return false;
        }

        // find if there's a rawDataFile corresponding to this parameter in the project
        int rawDataFileInd;
        logger.info(splitLine[filePos] + " " + files.length);
        if ((rawDataFileInd = IntStream.range(0, files.length)
            .filter(i -> files[i].getName().equals(splitLine[filePos])).findFirst().orElse(-1))
            != -1) {
          // create a column instance according to the parameter type
          MetadataColumn parameterMatched = MetadataColumn.forType(
              AvailableTypes.valueOf(splitLine[typePos]), splitLine[namePos], splitLine[descPos]);
          logger.info("RawDataFile corresponding to this metadata parameter is found");

          // if the parameter value is in the right format then save it to the metadata table,
          // otherwise abort importing
          Object convertedParameterInput = parameterMatched.convert(splitLine[valuePos], null);
          if (parameterMatched.checkInput(convertedParameterInput)) {
            metadataTable.setValue(parameterMatched, files[rawDataFileInd], convertedParameterInput);
          } else {
            logger.severe("Import failed: wrong parameter value format");
            return false;
          }
          // todo: need to update the metadata table when removing a RawDataFile
          //       the second option is to add equals() and hashCode() methods
          //       for RawDataFiles; it will work to, but imho the 1. option is better
        }
      }

      logger.info("Metadata table: ");
      metadataTable.getData().forEach((par, value) -> logger.info(par.getTitle() + ":" + value));
    } catch (FileNotFoundException fileNotFoundException) {
      logger.severe("Couldn't open file for metadata import: " + fileNotFoundException.getMessage());
      return false;
    } catch (IOException ioException) {
      logger.severe("Error while reading the metadata: " + ioException.getMessage());
      return false;
    }

    return true;
  }
}
