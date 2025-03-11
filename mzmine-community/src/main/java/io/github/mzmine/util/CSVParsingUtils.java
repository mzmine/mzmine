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

package io.github.mzmine.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
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
import static io.github.mzmine.util.StringUtils.inQuotes;
import io.github.mzmine.util.exceptions.MissingColumnException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
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
import java.util.function.Function;
import java.util.logging.Level;
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
      // no header found at all. may indicate wrong separator
      boolean noHeaderFound = lines.size() == nullMappings.size();
      final String error = "Did not find specified column " + Arrays.toString(
          nullMappings.stream().map(ImportType::getCsvColumnName).toArray()) + " in file." + (
          noHeaderFound
              ? "\nNo column title was found. Did you specify the correct column separator?" : "");
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

    if (!peakListFile.exists()) {
      return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR,
          "Input file " + peakListFile.getAbsolutePath() + " does not exist.");
    }

    List<String[]> peakListValues = null;
    try {
      peakListValues = readData(peakListFile, fieldSeparator);

      if (peakListValues.isEmpty()) {
        return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR,
            "File " + peakListFile.getAbsolutePath() + " did not contain any content.");
      }
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

      skipOptionalBom(reader);

      return readData(reader, separator);
    }
  }

  public static List<String[]> readDataAutoSeparator(final File file)
      throws IOException, CsvException {
    final Character sep = autoDetermineSeparatorDefaultFallback(file);

    try (var reader = Files.newBufferedReader(file.toPath())) {
      skipOptionalBom(reader);

      return readData(reader, sep.toString());
    }
  }

  /**
   * some users/programs save csv files with an encoding prefix in the first few bytes. This prefix
   * is equal to the char code \uFEFF and means that the file is utf-8 encoded. However, most UTF-8
   * files don't come with this prefix (=BOM, byte order marker). If it is there, we want to skip
   * it, otherwise the first csv field may be mis-recognised as a string with a different encoding.
   * see: https://stackoverflow.com/questions/4897876/reading-utf-8-bom-marker
   */
  private static void skipOptionalBom(BufferedReader reader) throws IOException {
    reader.mark(1);
    final char[] possibleBom = new char[1];
    final int read = reader.read(possibleBom);
    if (read == 1 && possibleBom[0] != '\uFEFF') {
      reader.reset(); // no BOM found, don't skip
    }
  }

  /**
   * Attempts to automatically determine the file separator of a tabular text file by the first two
   * lines.
   * <p></p>
   * Fallback criteria:
   * <p></p>
   * .tsv or .txt files -> \t
   * <p></p>
   * .csv -> ,
   *
   * @return The determined separator.
   */
  public static @Nullable Character autoDetermineSeparator(@NotNull File file) {
    Character bestSeparator = null;
    int maxCols = 1;

    final List<Character> possibleSeparators = List.of('\t', ',', ';');
    for (Character sep : possibleSeparators) {
      try (var reader = Files.newBufferedReader(file.toPath())) {
        skipOptionalBom(reader);
        // the split line must have more than one entry to auto-determine,
        // bc otherwise we may think we found the separator, but we just have an array of length 1.
        // in that case, we default to the most likely option from the file ending

        try (CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(
            new RFC4180ParserBuilder().withSeparator(sep).build()).build()) {

          final String[] splitHeader = csvReader.readNext();
          final String[] splitLine = csvReader.readNext();

          // first and second line must be the same length if we auto-determine the separator
          if (splitHeader.length != splitLine.length) {
            logger.finest(
                "Line length mismatch for separator %s. header %d, first line %d in file %s.".formatted(
                    inQuotes(sep.toString()), splitHeader.length, splitLine.length,
                    file.getName()));
            continue;
          }

          if (splitHeader.length > maxCols) {
            maxCols = splitHeader.length;
            bestSeparator = sep;
          }
        } catch (CsvValidationException e) {
          throw new RuntimeException(e);
        }
      } catch (IOException | NullPointerException e) {
        // this may happen if the file has less than two lines.
        logger.log(Level.FINE,
            "Cannot auto determine file separator for %s. File may be empty or has less than two lines.".formatted(
                file));
      }
    }

    return bestSeparator;
  }

  public static @NotNull Character autoDetermineSeparatorOrElse(@NotNull File file,
      Function<File, Character> fallback) {
    final Character sep = autoDetermineSeparator(file);
    if (sep == null) {
      final Character fb = fallback.apply(file);
      logger.finest(
          "Could not automatically determine separator for file %s. Falling back to %s".formatted(
              file, fb.equals('\t') ? "tab" : fb));
      return fb;
    }

    logger.finest("Automatically determined separator for file %s to be %s".formatted(file,
        sep.equals('\t') ? "tab" : sep));
    return sep;
  }

  public static @NotNull Character autoDetermineSeparatorDefaultFallback(@NotNull File file) {
    // the default file ending for excel export to tab-separated is .txt, so we catch here if the
    // user did not rename.
    return autoDetermineSeparatorOrElse(file,
        f -> f.getName().endsWith(".tsv") || file.getName().endsWith(".txt") ? '\t' : ',');
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
      if (mapStartLine > 0) {
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

  /**
   * @param file          used to detect format and separator: tsv tab and csv comma
   * @param defaultFormat if format is not tsv or csv - then enforce default format
   */
  public static ICSVWriter createDefaultWriterAutoDetect(File file, String defaultFormat,
      WriterOptions option) throws IOException {
    file = CSVUtils.ensureTsvOrCsvFormat(file, defaultFormat);
    char sep = CSVUtils.detectSeparatorFromName(file);
    return createDefaultWriter(file, sep, option);
  }

  public static ICSVWriter createDefaultWriter(File file, String separator, WriterOptions option)
      throws IOException {
    char sep = separator.equals("\t") ? '\t' : separator.charAt(0);
    return createDefaultWriter(file, sep, option);
  }

  public static ICSVWriter createDefaultWriter(File file, char sep, WriterOptions option)
      throws IOException {
    FileAndPathUtil.createDirectory(file.getParentFile());
    var writer = Files.newBufferedWriter(file.toPath(), option.toOpenOption());
    return new CSVWriterBuilder(writer).withSeparator(sep).build();
  }

  public record CompoundDbLoadResult(@NotNull List<CompoundDBAnnotation> annotations,
                                     @NotNull TaskStatus status, @Nullable String errorMessage) {

  }


}
