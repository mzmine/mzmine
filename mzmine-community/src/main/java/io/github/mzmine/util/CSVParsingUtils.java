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

package io.github.mzmine.util;

import static io.github.mzmine.util.StringUtils.inQuotes;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.ICSVWriter;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.exceptions.CsvMultilineLimitBrokenException;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.JsonStringType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.ExtraColumnHandler;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.HandleExtraColumnsOptions;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithStringInputValue;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingColumnException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.io.CharsetUtils;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.io.WriterOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSVParsingUtils {

  private static final Logger logger = Logger.getLogger(CSVParsingUtils.class.getName());

  /**
   * Separators tested by {@link #autoDetermineSeparator(File)}. The order only breaks ties between
   * equally scored separators, see {@link #candidateSeparators(File)}.
   */
  private static final List<Character> POSSIBLE_SEPARATORS = List.of('\t', ',', ';', '|');

  /**
   * Number of non-empty lines inspected to determine the separator of a file.
   */
  private static final int SEPARATOR_DETECTION_LINES = 50;

  /**
   * Upper limit of characters read to determine the separator, in case of very long lines.
   */
  private static final int SEPARATOR_DETECTION_CHARS = 1 << 18; // 256 k characters

  /**
   * Excel writes an optional "sep=;" line in front of the header to declare the separator of the
   * file. Trailing whitespace is allowed, but the separator itself may be a tab.
   */
  private static final Pattern SEPARATOR_DIRECTIVE = Pattern.compile("sep=(.)\\s*",
      Pattern.CASE_INSENSITIVE);

  /**
   * Value of a separator parameter that triggers {@link #autoDetermineSeparator(File)} instead of
   * using a fixed separator.
   */
  public static final String AUTO_SEPARATOR = "auto";


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
  public static List<ImportType<?>> findLineIds(List<ImportType<?>> importTypes, String[] firstLine,
      @NotNull StringProperty errorMessage) {
    return findLineIds(importTypes, firstLine, errorMessage, false);
  }

  /**
   * Searches an array of strings for a specified list of import types. Returns all selected import
   * types or null if they were found.
   *
   * @param importTypes    A list of {@link ImportType}s. Only if a type
   *                       {@link ImportType#isSelected()}, it will be included in the output list.
   * @param firstLine      The column headers
   * @param errorMessage   A string property to place an error message on failure. Stored property
   *                       is null unless an error occurs.
   * @param keepUnselected if true, the return value will also include unselected columns. Only
   *                       selected columns must be found in the csv.
   * @return A new list of the selected import types with their line indices set, or null if a
   * selected column was not found.
   */
  @Nullable
  public static List<ImportType<?>> findLineIds(List<ImportType<?>> importTypes, String[] firstLine,
      @NotNull StringProperty errorMessage, boolean keepUnselected) {
    List<ImportType<?>> lines = new ArrayList<>();
    for (ImportType<?> importType : importTypes) {
      if (importType.isSelected() || keepUnselected) {
        ImportType<?> type = new ImportType<>(importType.isSelected(),
            importType.getCsvColumnName(), importType.getDataType());
        lines.add(type);
      }
    }

    for (ImportType<?> importType : lines) {
      for (int i = 0; i < firstLine.length; i++) {
        String columnName = firstLine[i];
        if (columnName.trim().equalsIgnoreCase(importType.getCsvColumnName().trim())) {
          if (importType.getColumnIndex() != -1) {
            logger.warning(
                () -> "Library file contains two columns called \"" + columnName + "\".");
          }
          importType.setColumnIndex(i);
          importType.setCsvColumnName(
              columnName); // need to set to the specific upper/lower case for getCompoundFromLine
        }
      }
    }

    // only need to check if the selected ones were found
    final List<ImportType<?>> nullMappings = lines.stream()
        .filter(val -> val.getColumnIndex() == -1 && val.isSelected()).toList();
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
   *                       IonLibrary)}.
   */
  public static CompoundDbLoadResult getAnnotationsFromCsvFile(final File peakListFile,
      String fieldSeparator, @NotNull List<ImportType<?>> types, @Nullable IonLibrary ionLibrary) {
    final List<CompoundDBAnnotation> list = new ArrayList<>();
    final ExtraColumnHandler extraColHandler = new ExtraColumnHandler(
        new ComboWithStringInputValue<>(HandleExtraColumnsOptions.IGNORE, null));
    final SimpleStringProperty errorMessage = new SimpleStringProperty();

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

    final List<ImportType<?>> lineIds = CSVParsingUtils.findLineIds(types,
        peakListValues.getFirst(), errorMessage);
    if (lineIds == null) {
      return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR, errorMessage.get());
    }

    for (int i = 1; i < peakListValues.size(); i++) {
      final CompoundDBAnnotation baseAnnotation = CSVParsingUtils.getCompoundFromLine(
          peakListValues.getFirst(), peakListValues.get(i), lineIds, extraColHandler);

      if (!CompoundDBAnnotation.isBaseAnnotationValid(baseAnnotation, ionLibrary != null)) {
        logger.info(String.format(
            "Invalid base annotation for compound %s in line %d. Skipping annotation.",
            baseAnnotation, i));
        continue;
      }
      baseAnnotation.put(DatabaseNameType.class, peakListFile.getName());

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
    final char sep = isAutoSeparator(separator) ? autoDetermineSeparatorDefaultFallback(file)
        : toSeparatorChar(separator);
    try (var reader = CharsetUtils.newBufferedReader(file)) {
      return readData(reader, sep, false);
    } catch (CsvMalformedLineException | CsvMultilineLimitBrokenException e) {
      // files with unbalanced quotes, e.g., an inch sign in an unquoted field, cannot be parsed
      // by the rules of RFC4180. Rather than failing the whole import, read them without quoting.
      logger.log(Level.WARNING,
          "Unbalanced quotes in %s, reading the file without quote handling. %s".formatted(file,
              e.getMessage()));
      try (var reader = CharsetUtils.newBufferedReader(file)) {
        return readData(reader, sep, true);
      }
    }
  }

  /**
   * Read data until end, using the separator determined by
   * {@link #autoDetermineSeparatorDefaultFallback(File)}.
   *
   * @return List of rows
   * @throws IOException if read is unsuccessful
   */
  public static List<String[]> readDataAutoSeparator(final File file)
      throws IOException, CsvException {
    return readData(file, AUTO_SEPARATOR);
  }

  /**
   * @return true if the separator parameter asks for {@link #autoDetermineSeparator(File)}, which is
   * the case for null, empty, and {@link #AUTO_SEPARATOR}. Whitespace is not empty, a tab or a
   * space are valid separators.
   */
  public static boolean isAutoSeparator(@Nullable final String separator) {
    return separator == null || separator.isEmpty() || AUTO_SEPARATOR.equalsIgnoreCase(
        separator.trim());
  }

  /**
   * @param separator a single character or the escaped tab "\\t"
   * @return the separator character
   */
  public static char toSeparatorChar(@Nullable final String separator) {
    if (isAutoSeparator(separator)) {
      throw new IllegalArgumentException(
          "Automatic separator detection needs a file, see readData(File, String).");
    }
    return "\\t".equals(separator) ? '\t' : separator.charAt(0);
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
   * Attempts to automatically determine the field separator of a tabular text file. An explicit
   * "sep=;" directive as written by excel wins, otherwise all {@link #POSSIBLE_SEPARATORS} are
   * tested on the first {@link #SEPARATOR_DETECTION_LINES} lines and the separator that splits the
   * most lines into the same number of columns wins. Quoted fields, quoted line breaks, and ragged
   * rows are handled, the file encoding is detected by {@link CharsetUtils}.
   *
   * @return the determined separator or null if no separator splits the file into more than one
   * column, e.g., for single column files
   */
  public static @Nullable Character autoDetermineSeparator(@NotNull File file) {
    final List<String> lines;
    try {
      lines = readSampleLines(file, SEPARATOR_DETECTION_LINES, SEPARATOR_DETECTION_CHARS);
    } catch (IOException e) {
      logger.log(Level.FINE,
          "Cannot auto determine the separator of %s, the file cannot be read.".formatted(file), e);
      return null;
    }
    if (lines.isEmpty()) {
      logger.finest(
          () -> "Cannot auto determine the separator of %s, the file is empty.".formatted(file));
      return null;
    }

    // excel and other tools may declare the separator in an optional first line: sep=;
    final Character declared = extractSeparatorDirective(lines.getFirst());
    if (declared != null) {
      logger.finest(() -> "File %s declares %s as separator.".formatted(file,
          inQuotes(declared.toString())));
      return declared;
    }

    final String sample = String.join("\n", lines);
    Character bestSeparator = null;
    SeparatorScore bestScore = null;
    for (Character sep : candidateSeparators(file)) {
      final SeparatorScore score = scoreSeparator(sample, sep);
      // a single column means this separator does not occur outside of quoted fields
      if (score.columns() < 2) {
        continue;
      }
      if (bestScore == null || score.isBetterThan(bestScore)) {
        bestSeparator = sep;
        bestScore = score;
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
    return autoDetermineSeparatorOrElse(file, CSVParsingUtils::defaultSeparatorForExtension);
  }

  /**
   * The default file ending for the excel export to tab-separated is .txt, so tab is also the
   * default for .txt files if the user did not rename them.
   *
   * @return tab for .tsv, .tab, and .txt files, comma otherwise
   */
  public static @NotNull Character defaultSeparatorForExtension(@NotNull final File file) {
    final String name = file.getName().toLowerCase();
    return name.endsWith(".tsv") || name.endsWith(".tab") || name.endsWith(".txt") ? '\t' : ',';
  }

  /**
   * @return all {@link #POSSIBLE_SEPARATORS} with the separator matching the file extension first,
   * so that equally scored separators are decided by the file extension
   */
  private static @NotNull List<Character> candidateSeparators(@NotNull final File file) {
    final Character preferred = defaultSeparatorForExtension(file);
    return Stream.concat(Stream.of(preferred),
        POSSIBLE_SEPARATORS.stream().filter(sep -> !sep.equals(preferred))).toList();
  }

  /**
   * Rates a separator by how many of the sampled lines are split into the same number of columns.
   */
  private static @NotNull SeparatorScore scoreSeparator(@NotNull final String sample,
      final char separator) {
    final List<String[]> rows;
    try (CSVReader reader = createCsvReader(new StringReader(sample), separator, false)) {
      rows = reader.readAll();
    } catch (Exception e) {
      // unbalanced quotes and the like simply disqualify this separator
      return SeparatorScore.NONE;
    }
    if (rows.isEmpty()) {
      return SeparatorScore.NONE;
    }

    final Map<Integer, Long> columnHistogram = rows.stream()
        .collect(Collectors.groupingBy(row -> row.length, Collectors.counting()));
    // most common number of columns, ties are won by the higher column count
    final Entry<Integer, Long> mode = columnHistogram.entrySet().stream().max(
            Comparator.<Entry<Integer, Long>>comparingLong(Entry::getValue).thenComparing(Entry::getKey))
        .orElseThrow();

    return new SeparatorScore(mode.getKey(), mode.getValue() / (double) rows.size());
  }

  /**
   * Reads the beginning of a text file of any encoding, skipping empty lines and an optional byte
   * order mark.
   */
  private static @NotNull List<String> readSampleLines(@NotNull final File file, final int maxLines,
      final int maxChars) throws IOException {
    final List<String> lines = new ArrayList<>(Math.min(maxLines, 64));
    int chars = 0;

    try (BufferedReader reader = CharsetUtils.newBufferedReader(file)) {
      String line;
      while (lines.size() < maxLines && chars < maxChars && (line = reader.readLine()) != null) {
        if (lines.isEmpty() && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
          line = line.substring(1);
        }
        if (line.isBlank()) {
          continue;
        }
        lines.add(line);
        chars += line.length() + 1;
      }
    }
    return lines;
  }

  /**
   * @return the separator declared by an excel style "sep=;" line or null if this is a regular
   * line
   */
  private static @Nullable Character extractSeparatorDirective(@NotNull final String line) {
    final Matcher matcher = SEPARATOR_DIRECTIVE.matcher(line);
    return matcher.matches() ? matcher.group(1).charAt(0) : null;
  }

  /**
   * @param row       the first parsed row of a file
   * @param separator the separator used to parse the row, needed to join the row back together in
   *                  case the declared separator was used for parsing
   * @return true if this row is an excel style "sep=;" directive and not data
   */
  private static boolean isSeparatorDirective(final String[] row, final char separator) {
    return SEPARATOR_DIRECTIVE.matcher(String.join(String.valueOf(separator), row)).matches();
  }

  /**
   * @param columns     the most common number of columns
   * @param consistency fraction of the sampled lines that are split into {@link #columns} columns
   */
  private record SeparatorScore(int columns, double consistency) {

    private static final SeparatorScore NONE = new SeparatorScore(0, 0);

    /**
     * Lines of equal length are the strongest indicator, the number of columns only breaks ties.
     * E.g., a european csv file (a;b;c) with decimal commas (1,5;2,5) is split into more columns by
     * comma, but only the semicolon gives every line the same number of columns.
     */
    boolean isBetterThan(@NotNull final SeparatorScore other) {
      return consistency > other.consistency || (consistency == other.consistency
                                                 && columns > other.columns);
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
    return readData(reader, toSeparatorChar(separator), false);
  }

  /**
   * @param ignoreQuotations quote characters are read as regular characters. Recovers files with
   *                         unbalanced quotes, but breaks fields that contain the separator.
   */
  private static List<String[]> readData(final BufferedReader reader, final char separator,
      final boolean ignoreQuotations) throws IOException, CsvException {
    skipOptionalBom(reader);

    try (CSVReader csvReader = createCsvReader(reader, separator, ignoreQuotations)) {
      List<String[]> result = new ArrayList<>(64);
      String[] row;
      boolean firstRow = true;
      while ((row = csvReader.readNext()) != null) {
        boolean empty = Arrays.stream(row).allMatch(s -> s == null || s.isBlank());
        if (empty) {
          continue;
        }
        if (firstRow) {
          firstRow = false;
          if (isSeparatorDirective(row, separator)) {
            continue; // "sep=;" line written by excel, not data
          }
        }
        result.add(row);
      }
      return result;
    }
  }

  private static @NotNull CSVReader createCsvReader(final Reader reader, final char separator,
      final boolean ignoreQuotations) {
    final ICSVParser parser = ignoreQuotations ? new CSVParserBuilder().withSeparator(separator)
        .withIgnoreQuotations(true).build()
        : new RFC4180ParserBuilder().withSeparator(separator).build();
    return new CSVReaderBuilder(reader).withCSVParser(parser).build();
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
    List<String[]> rows = readData(file, sep);
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

  /**
   * @param csvHeaders         The headers in the csv file in the same order as the lines (=values)
   * @param values             The column values
   * @param linesWithIndices   the loaded import types. only types that are
   *                           {@link ImportType#isSelected()} will be loaded with the associated
   *                           {@link ImportType#getMapper()}. If a type is in the list but not
   *                           selected and the {@link ExtraColumnHandler} specifies to import it,
   *                           it will be mapped by the default {@link DataType#getMapper()}.
   * @param extraColumnHandler handler for additional columns found in the csv file. If the column
   *                           name matches the {@link DataType#getUniqueID()}, it will be attempted
   *                           to convert the type using the {@link DataType#getMapper()} method.
   *                           Otherwise, it will be put into a {@link JsonStringType}.
   * @return A {@link CompoundDBAnnotation} built for the csv line.
   */
  @NotNull
  public static CompoundDBAnnotation getCompoundFromLine(@NotNull String[] csvHeaders,
      @NotNull String[] values, @NotNull List<ImportType<?>> linesWithIndices,
      @NotNull final ExtraColumnHandler extraColumnHandler) {

    final Map<@NotNull String, @Nullable String> data = HashMap.newHashMap(csvHeaders.length);
    for (int i = 0; i < csvHeaders.length; i++) {
      data.put(csvHeaders[i], values[i]);
    }

    final CompoundDBAnnotation a = new SimpleCompoundDBAnnotation();
    for (final ImportType<?> importType : linesWithIndices) {
      if (importType.isSelected()) {
        importType.applyRemoveAndPut(data, a);
      }
    }

    if (extraColumnHandler.isImportExtraColumns() && !data.isEmpty()) {
      final Iterator<Entry<@NotNull String, @Nullable String>> entryIterator = data.entrySet()
          .iterator();

      // check if the column name maybe matches one of our unique ids. If so, map them to the actual type
      // also remove all the types that shall not be imported, if only specific types are imported
      while (entryIterator.hasNext()) {
        final Entry<@NotNull String, @Nullable String> entry = entryIterator.next();
        if (!extraColumnHandler.isImportedColumn(entry.getKey())) {
          entryIterator.remove(); // remove type if a column is not meant to be imported
          continue;
        }
        final DataType<?> existingType = DataTypes.getTypeForId(entry.getKey().toLowerCase());
        if (existingType != null && existingType.getMapper() != null) {
          final Function<@Nullable String, ?> mapper = existingType.getMapper();
          final Object value = mapper.apply(entry.getValue());
          a.put((DataType) existingType, value);
          entryIterator.remove(); // only remove types we have successfully mapped
        }
      }

      // everything else can go into a JSON string
      if (!data.isEmpty()) {
        final String jsonRemaining = JsonUtils.writeStringOrElse(data, null);
        a.putIfNotNull(JsonStringType.class, jsonRemaining);
      }
    }

    a.enrichMetadata();
    return a;
  }

  public record CompoundDbLoadResult(@NotNull List<CompoundDBAnnotation> annotations,
                                     @NotNull TaskStatus status, @Nullable String errorMessage) {

  }


}
