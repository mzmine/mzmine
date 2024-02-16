/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingColumnException;
import io.github.mzmine.util.io.WriterOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSVParsingUtils {

  private static final Logger logger = Logger.getLogger(CSVParsingUtils.class.getName());


  /**
   * Find indices of all columns
   *
   * @param titleLine titleLine
   * @return map of column and index
   */
  @NotNull
  public static Map<String, Integer> extractColumnIndices(final String[] columns,
      final String[] titleLine) {
    List<String> lowerLine = Arrays.stream(titleLine).map(String::toLowerCase).toList();
    Map<String, Integer> map = Arrays.stream(columns).map(String::toLowerCase)
        .collect(Collectors.toMap(key -> key, lowerLine::indexOf));
    return map;
  }

  /**
   * Find indices of all columns or throw exception if some are missing
   *
   * @param titleLine titleLine
   * @return map of column and index
   */
  @NotNull
  public static Map<String, Integer> extractColumnIndicesStrict(final String[] columns,
      final String[] titleLine) throws MissingColumnException {
    Map<String, Integer> map = extractColumnIndices(columns, titleLine);

    List<String> missingColumns = map.entrySet().stream().filter(e -> e.getValue() == -1)
        .map(Entry::getKey).toList();
    if (!missingColumns.isEmpty()) {
      throw new MissingColumnException(missingColumns);
    }
    return map;
  }


  /**
   * Searches an array of strings for a specified list of import types. Returns all selected import
   * types or null if they were found.
   *
   * @param importTypes  A list of {@link ImportType}s. Only if a type
   *                     {@link ImportType#isSelected()}, it will be included in the output list.
   * @param firstLine    The column headers
   * @param errorMessage A string property to place an error message on failure. Stored property is
   *                     null unless an error occurs.
   * @return A new list of the selected import types with their line indices set, or null if a
   * selected column was not found.
   */
  @Nullable
  public static List<ImportType> findLineIds(List<ImportType> importTypes, String[] firstLine,
      @NotNull StringProperty errorMessage) {
    List<ImportType> lines = new ArrayList<>();
    for (ImportType importType : importTypes) {
      if (importType.isSelected()) {
        ImportType type = new ImportType(importType.isSelected(), importType.getCsvColumnName(),
            importType.getDataType());
        lines.add(type);
      }
    }

    for (ImportType importType : lines) {
      for (int i = 0; i < firstLine.length; i++) {
        String columnName = firstLine[i];
        if (columnName.trim().equalsIgnoreCase(importType.getCsvColumnName().trim())) {
          if (importType.getColumnIndex() != -1) {
            logger.warning(
                () -> "Library file contains two columns called \"" + columnName + "\".");
          }
          importType.setColumnIndex(i);
        }
      }
    }
    final List<ImportType> nullMappings = lines.stream().filter(val -> val.getColumnIndex() == -1)
        .toList();
    if (!nullMappings.isEmpty()) {
      final String error = "Did not find specified column " + Arrays.toString(
          nullMappings.stream().map(ImportType::getCsvColumnName).toArray()) + " in file.";
      logger.warning(() -> error);
      errorMessage.set(error);
      return null;
    }
    return lines;
  }

  public static CompoundDBAnnotation csvLineToCompoundDBAnnotation(final String[] line,
      final List<ImportType> types) {

    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    for (ImportType type : types) {
      final int columnIndex = type.getColumnIndex();

      if (columnIndex == -1 || line[columnIndex] == null || line[columnIndex].isBlank()) {
        continue;
      }

      try {
        switch (type.getDataType()) {
          case FloatType ft -> annotation.put(ft, Float.parseFloat(line[columnIndex]));
          case IntegerType it -> annotation.put(it, Integer.parseInt(line[columnIndex]));
          case DoubleType dt -> annotation.put(dt, Double.parseDouble(line[columnIndex]));
          case IonAdductType ignored -> {
            final IonType ionType = IonTypeParser.parse(line[columnIndex]);
            annotation.putIfNotNull(IonTypeType.class, ionType);

          }
          case IonTypeType ignored -> {
            final IonType ionType = IonTypeParser.parse(line[columnIndex]);
            annotation.putIfNotNull(IonTypeType.class, ionType);
          }
          case StringType st -> annotation.put(st, line[columnIndex]);
          default -> throw new RuntimeException(
              "Don't know how to parse data type " + type.getDataType().getClass().getName());
        }
      } catch (NumberFormatException e) {
        // silent - e.g. #NV from excel
      }
    }

    // try to calculate the neutral mass if it was not present.
    if (annotation.get(NeutralMassType.class) == null) {
      annotation.putIfNotNull(NeutralMassType.class,
          CompoundDBAnnotation.calcNeutralMass(annotation));
    }

    return annotation;
  }

  /**
   * Reads the given csv file and returns a {@link CompoundDbLoadResult} containing a list of valid
   * (see {@link CompoundDBAnnotation#isBaseAnnotationValid(CompoundDBAnnotation, boolean)}
   * annotations or an error message.
   *
   * @param peakListFile   The csv file.
   * @param fieldSeparator The field separator.
   * @param types          The list of possible import types. All types that are selected
   *                       ({@link ImportType#isSelected()} must be found in the csv file.
   * @param ionLibrary     An ion library or null. If the library is null, a precursor mz must be
   *                       given in the file. Otherwise, all formulas will be ionised by
   *                       {@link
   *                       CompoundDBAnnotation#buildCompoundsWithAdducts(CompoundDBAnnotation,
   *                       IonNetworkLibrary)}.
   */
  public static CompoundDbLoadResult getAnnotationsFromCsvFile(final File peakListFile,
      String fieldSeparator, @NotNull List<ImportType> types,
      @Nullable IonNetworkLibrary ionLibrary) {
    List<CompoundDBAnnotation> list = new ArrayList<>();

    List<String[]> peakListValues = null;
    try {
      peakListValues = readData(peakListFile, fieldSeparator);
    } catch (IOException | CsvException e) {

      throw new RuntimeException(e);
    }

    final SimpleStringProperty errorMessage = new SimpleStringProperty();
    final List<ImportType> lineIds = CSVParsingUtils.findLineIds(types, peakListValues.get(0),
        errorMessage);

    if (lineIds == null) {
      return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR, errorMessage.get());
    }

    for (int i = 1; i < peakListValues.size(); i++) {
      final CompoundDBAnnotation baseAnnotation = CSVParsingUtils.csvLineToCompoundDBAnnotation(
          peakListValues.get(i), lineIds);

      if (!CompoundDBAnnotation.isBaseAnnotationValid(baseAnnotation, ionLibrary != null)) {
        logger.info(String.format(
            "Invalid base annotation for compound %s in line %d. Skipping annotation.",
            baseAnnotation, i));
        continue;
      }

      if (ionLibrary != null) {
        final List<CompoundDBAnnotation> ionizedAnnotations = CompoundDBAnnotation.buildCompoundsWithAdducts(
            baseAnnotation, ionLibrary);
        list.addAll(ionizedAnnotations);
      } else {
        list.add(baseAnnotation);
      }
    }

    if (list.isEmpty()) {
      return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR,
          "Did not find any valid compounds in file.");
    }

    return new CompoundDbLoadResult(list, TaskStatus.FINISHED, null);
  }

  /**
   * Read data until end. Skips empty lines. The returned list is not trimmed to size. If you retain
   * the list than use {@link ArrayList#trimToSize()}.
   *
   * @param separator separator
   * @return List of rows
   * @throws IOException if read is unsuccessful
   */
  public static List<String[]> readData(final File file, final String separator)
      throws IOException, CsvException {
    try (var reader = Files.newBufferedReader(file.toPath())) {
      return readData(reader, separator);
    }
  }

  /**
   * Read data until end. Skips empty lines. The returned list is not trimmed to size. If you retain
   * the list than use {@link ArrayList#trimToSize()}.
   *
   * @param separator separator
   * @return List of rows
   * @throws IOException if read is unsuccessful
   */
  public static List<String[]> readData(final BufferedReader reader, final String separator)
      throws IOException, CsvException {
    char sep = "\\t".equals(separator) ? '\t' : separator.charAt(0);
    try (CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(
        new RFC4180ParserBuilder().withSeparator(sep).build()).build()) {
      List<String[]> result = new ArrayList<>(64);
      String[] row;
      while ((row = csvReader.readNext()) != null) {
        boolean empty = Arrays.stream(row).allMatch(s -> s == null || s.isBlank());
        if (!empty) {
          result.add(row);
        }
      }
      return result;
    }
  }

  public static String[][] readDataMapToColumns(final File file, final String sep)
      throws IOException, CsvException {
    return readDataMapToColumns(file, sep, 0);
  }

  /**
   * Read data until end - then map to columns
   *
   * @param sep          separator
   * @param mapStartLine the line in which to start the mapping. can be used to exclude the header.
   * @return array of [columns][rows]
   * @throws IOException if read is unsuccessful
   */
  public static String[][] readDataMapToColumns(final File file, final String sep, int mapStartLine)
      throws IOException, CsvException {
    try (var reader = Files.newBufferedReader(file.toPath())) {
      List<String[]> rows = readData(reader, sep);
      if(mapStartLine > 0) {
        rows.subList(0, mapStartLine).clear();
      }

      // max columns
      int cols = rows.stream().mapToInt(a -> a.length).max().orElse(0);

      String[][] data = new String[cols][rows.size()];
      for (int r = 0; r < rows.size(); r++) {
        String[] row = rows.get(r);
        for (int c = 0; c < row.length; c++) {
          String v = row[c];
          data[c][r] = v == null || v.isBlank() ? null : v;
        }
      }

      return data;
    }
  }

  public static ICSVWriter createDefaultWriter(File file, String separator, WriterOptions option)
      throws IOException {
    var writer = Files.newBufferedWriter(file.toPath(), option.toOpenOption());
    char sep = separator.equals("\t") ? '\t' : separator.charAt(0);
    return new CSVWriterBuilder(writer).withSeparator(sep).build();
  }

  public record CompoundDbLoadResult(@NotNull List<CompoundDBAnnotation> annotations,
                                     @NotNull TaskStatus status, @Nullable String errorMessage) {

  }


}
