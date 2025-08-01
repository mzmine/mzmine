/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.forType;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.io.CSVUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ProjectMetadataReader {

  private static final Logger logger = Logger.getLogger(ProjectMetadataReader.class.getName());

  private final boolean skipColOnError;
  private final boolean removeAttributesPrefix;
  private final List<String> errors = new ArrayList<>();
  private final boolean usePlaceholderForMissingFiles;
  private String[] titles;
  private String[] descriptions;
  private AvailableTypes[] dataTypes;

  public ProjectMetadataReader(final boolean skipColOnError, final boolean removeAttributesPrefix) {
    this(skipColOnError, removeAttributesPrefix, false);
  }

  public ProjectMetadataReader(final boolean skipColOnError, final boolean removeAttributesPrefix,
      final boolean usePlaceholderForMissingFiles) {
    this.skipColOnError = skipColOnError;
    this.removeAttributesPrefix = removeAttributesPrefix;
    this.usePlaceholderForMissingFiles = usePlaceholderForMissingFiles;
  }

  @NotNull
  public List<String> getErrors() {
    return errors;
  }

  public MetadataTable readFile(File file) {
    errors.clear();
    // titles is always considered the last row before data
    titles = null;
    // optional rows before titles
    descriptions = null;
    dataTypes = null;

    if (!usePlaceholderForMissingFiles && ProjectService.getProject().getNumberOfDataFiles() == 0) {
      errors.add("Import data files before importing metadata.");
      return null;
    }

    // different file formats are supported.
    // see test/resources/metadata
    // first two lines are optional (description / type) otherwise try to cast to type
    char sep = CSVUtils.detectSeparatorFromName(file);

    try {
      List<String[]> lines = CSVParsingUtils.readData(file, String.valueOf(sep));

      lines = extractRemoveHeaderTitlesAndTypes(lines);
      if (!errors.isEmpty()) {
        return null;
      }

      // same length as data
      var fileNames = lines.stream().map(line -> line[0]).toArray(String[]::new);
      RawDataFile[] rawFiles = findProjectRawFiles(fileNames);

      if (!errors.isEmpty()) {
        return null;
      }

      logger.finest("The header size & format correspond, OK");
      MetadataTable metadata = new MetadataTable(false);

      // convert data
      // skip first as this is filenames
      for (int col = 1; col < titles.length; col++) {
        String title = titles[col].trim();
        if (removeAttributesPrefix && title.toLowerCase().startsWith("attribute_")) {
          title = title.substring(10);
        }

        AvailableTypes type = dataTypes != null ? dataTypes[col] : null;
        String description = descriptions != null ? descriptions[col] : "";

        final int colI = col;
        String[] columnData = lines.stream().map(line -> line[colI]).toArray(String[]::new);

        final Object[] data;
        if (type != null) {
          // map data to data types
          // might be null on error in this column - this needs to be checked in the caller
          data = type.tryCastType(columnData);
          if (data == null) {
            errors.add(
                "Column %s could not be converted to data type %s. Make sure numbers and dates follow the correct format: %s".formatted(
                    title, type, type.getInstance().exampleValue()));
            if (!skipColOnError) {
              logger.warning(
                  "Stopped metadata import because of incompatibility of provided data type and values for column "
                      + type);
              return null;
            } else {
              continue; // next column
            }
          }
        } else {
          // find the best data type and map values
          // if all values are numbers -> parse as double
          // if all are dates -> parse as dates
          // otherwise use string
          // write data into new array
          data = new Object[columnData.length];
          type = AvailableTypes.castToMostAppropriateType(columnData, data);
        }

        MetadataColumn metaCol = forType(type, title, description);
        metadata.addColumn(metaCol);

        // not all raw data files might be imported - but more covered in the metadata sheet
        for (int row = 0; row < data.length; row++) {
          var rawFile = rawFiles[row];
          if (rawFile != null) {
            metadata.setValue(metaCol, rawFile, data[row]);
          }
        }
      }

      logger.info("Metadata table: ");
      var info = metadata.getData().keySet().stream()
          .map(col -> "Column %s of type %s".formatted(col.getTitle(), col.getType()))
          .collect(Collectors.joining("\n"));

      long nRawFiles = Arrays.stream(rawFiles).filter(Objects::nonNull).count();
      logger.info("""
          Metadata info: %d columns for %d raw data files that are loaded into the project. Columns:
          %s""".formatted(metadata.getColumns().size(), nRawFiles, info));
      return metadata;
    } catch (FileNotFoundException fileNotFoundException) {
      errors.add("Could not open file");
      logger.severe(
          "Couldn't open file for metadata import: " + fileNotFoundException.getMessage());
      return null;
    } catch (IOException ioException) {
      errors.add("Error while reading metadata file");
      logger.severe("Error while reading the metadata: " + ioException.getMessage());
      return null;
    } catch (CsvException ioException) {
      errors.add("Error while parsing metadata file");
      logger.severe("Error while parsing the metadata: " + ioException.getMessage());
      return null;
    }
  }

  /**
   * Extract titles, types, descriptions from header and remove those lines so that the returned
   * list of lines contains only data
   *
   * @param lines input lines with header
   * @return output lines without header
   */
  private @NotNull List<String[]> extractRemoveHeaderTitlesAndTypes(List<String[]> lines) {
    // FILENAME_HEADER may duplicate one of the other options but better to check all of them
    // this makes it future-proof in case FILENAME_HEADER changes
    List<String> FILE_HEADERS = List.of(FILENAME_HEADER.toLowerCase(), "filename", "filenames");

    for (int i = 0; i < lines.size() && i < 4; i++) {
      String[] cells = lines.get(i);
      if (cells.length == 0) {
        continue;
      }

      String firstCellLower = cells[0].toLowerCase();
      if (FILE_HEADERS.stream().anyMatch(s -> s.equals(firstCellLower))) {
        titles = cells;
        // remove lines too retain only data
        lines = lines.subList(i + 1, lines.size());
        break;
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
      errors.add(
          "Did not find column headers. Make sure to add first column with filename or Filename header.");
      return lines;
    }

    // tiles might have empty field at the end - crop
    int lastHeaderIndex = titles.length - 1;
    for (; lastHeaderIndex >= 0; lastHeaderIndex--) {
      if (StringUtils.hasValue(titles[lastHeaderIndex])) {
        break;
      }
    }
    if (lastHeaderIndex < titles.length - 1) {
      titles = Arrays.copyOf(titles, lastHeaderIndex + 1);
      if (descriptions != null) {
        descriptions = Arrays.copyOf(descriptions, titles.length);
      }
      if (dataTypes != null) {
        dataTypes = Arrays.copyOf(dataTypes, titles.length);
      }
    }

    // empty header entry?
    if (Arrays.stream(titles).anyMatch(String::isEmpty)) {
      logger.severe("Could not load wide format table. A header title was empty.");
      errors.add("There were empty headers in title line");
      return lines;
    }

    //
    if (dataTypes != null && dataTypes.length != titles.length) {
      logger.severe(
          "Could not load wide format table. The type definition and titles need to have the same number of entries.");
      errors.add("The number of titles and type descriptors was different length");
      return lines;
    }
    return lines;
  }

  @NotNull
  private RawDataFile[] findProjectRawFiles(final String[] fileNames) {
    // match raw data files with or without file format
    var rawFiles = Arrays.stream(fileNames).map(name -> {
      final RawDataFile actualFile = ProjectService.getProject().getDataFileByName(name);
      // may use placeholders instead of real files
      // mostly if loading is done as a precheck
      return actualFile == null && usePlaceholderForMissingFiles ? new RawDataFilePlaceholder(name,
          null) : actualFile;
    }).toArray(RawDataFile[]::new);

    if (Arrays.stream(rawFiles).allMatch(Objects::isNull)) {
      errors.add(
          "Found no matching raw data files in metadata and imported project files. Make sure filenames in metadata are correct and in the first column: "
              + String.join(", ", fileNames));
    } else {
      long nProjectFiles = Arrays.stream(rawFiles).filter(Objects::nonNull).count();
      long nMetadataFiles = Arrays.stream(fileNames).filter(StringUtils::hasValue).count();
      logger.info(
          "Found %d raw data files in project from %d defined in metadata".formatted(nProjectFiles,
              nMetadataFiles));
    }
    return rawFiles;
  }

}
