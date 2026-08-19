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

package import_data.speed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.JsonUtils;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Appends {@link SpeedMeasurement}s to a csv and a jsonlines file next to each other. Both files
 * are kept open for the whole run so that all iterations end up in one file even if a single
 * iteration fails.
 * <p>
 * The csv header is only written for a new file. When an existing file was written with a different
 * set of columns this logs a warning instead of silently appending rows that do not match the
 * header - use a new output file in that case.
 */
public class SpeedMeasurementWriter implements Closeable {

  private static final Logger logger = Logger.getLogger(SpeedMeasurementWriter.class.getName());

  private final ObjectMapper jsonMapper = JsonUtils.MAPPER;
  private final CsvMapper csvMapper = CsvMapper.builder().addModule(new JavaTimeModule()).build();
  private final CsvSchema schema = schemaInRecordOrder();
  private final ObjectWriter csvObjectWriter = csvMapper.writer(schema.withUseHeader(false));

  private final File csvFile;
  private final File jsonFile;
  private final BufferedWriter csvWriter;
  private final BufferedWriter jsonWriter;
  /**
   * The header is written together with the first row of a new file.
   */
  private boolean headerPending;

  public SpeedMeasurementWriter(@NotNull final File outFile) throws IOException {
    csvFile = FileAndPathUtil.getRealFilePath(outFile, ".csv");
    jsonFile = FileAndPathUtil.getRealFilePath(outFile, ".jsonlines");
    FileAndPathUtil.createDirectory(csvFile.getParentFile());

    headerPending = !csvFile.exists() || csvFile.length() == 0;
    if (!headerPending) {
      checkHeaderMatches();
    }

    csvWriter = Files.newBufferedWriter(csvFile.toPath(), StandardCharsets.UTF_8,
        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    jsonWriter = Files.newBufferedWriter(jsonFile.toPath(), StandardCharsets.UTF_8,
        StandardOpenOption.APPEND, StandardOpenOption.CREATE);

    logger.info("Appending speed measurements to " + csvFile.getAbsolutePath() + " and "
        + jsonFile.getAbsolutePath());
  }

  /**
   * {@link CsvMapper#schemaFor(Class)} sorts the columns alphabetically, the record components are
   * the more useful order because they group the identity, the measurements and the environment.
   */
  @NotNull
  private static CsvSchema schemaInRecordOrder() {
    final CsvSchema.Builder builder = CsvSchema.builder();
    for (final RecordComponent component : SpeedMeasurement.class.getRecordComponents()) {
      builder.addColumn(component.getName());
    }
    return builder.build();
  }

  public void append(@NotNull final List<SpeedMeasurement> measurements) throws IOException {
    for (final SpeedMeasurement sm : measurements) {
      final ObjectWriter writer =
          headerPending ? csvMapper.writer(schema.withUseHeader(true)) : csvObjectWriter;
      headerPending = false;
      csvWriter.append(writer.writeValueAsString(sm));
      jsonWriter.append(jsonMapper.writeValueAsString(sm)).append('\n');
    }
    csvWriter.flush();
    jsonWriter.flush();
  }

  /**
   * Warns when the existing csv was written with a different schema, the rows would not match its
   * header.
   */
  private void checkHeaderMatches() throws IOException {
    final List<String> names = new ArrayList<>();
    for (final CsvSchema.Column column : schema) {
      names.add(column.getName());
    }
    final String expected = String.join(",", names);
    try (var lines = Files.lines(csvFile.toPath(), StandardCharsets.UTF_8)) {
      final String header = lines.findFirst().orElse("");
      if (!header.isBlank() && !header.strip().equals(expected)) {
        logger.warning("""
            The existing csv %s was written with different columns, the appended rows will not \
            match its header. Please export to a new file.
            existing: %s
            current : %s""".formatted(csvFile.getAbsolutePath(), header.strip(), expected));
      }
    }
  }

  @Override
  public void close() throws IOException {
    try (csvWriter; jsonWriter) {
      csvWriter.flush();
      jsonWriter.flush();
    }
  }

  @NotNull
  public File getCsvFile() {
    return csvFile;
  }
}
