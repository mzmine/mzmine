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

package io.github.mzmine.util.spectraldb.parser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the mzmine spectral library json, a json lines format with one library entry per line.
 */
public class MZmineJsonParser extends SpectralDBTextParser {

  private final static Logger logger = Logger.getLogger(MZmineJsonParser.class.getName());

  private static final String PEAKS_KEY = "peaks";
  private static final int READ_BUFFER = 1 << 16;
  private static final int INITIAL_SIGNALS = 128;

  public MZmineJsonParser(int bufferEntries, LibraryEntryProcessor processor,
      boolean extensiveErrorLogging) {
    super(bufferEntries, processor, extensiveErrorLogging);
  }

  @Override
  public boolean parse(@Nullable AbstractTask mainTask, @NotNull File dataBaseFile,
      @NotNull SpectralLibrary library) throws IOException {
    // byte progress instead of the line counting pass of the super implementation, which would
    // read the whole file a second time
    initByteProgress(dataBaseFile);

    logger.info("Parsing MZmine spectral library " + dataBaseFile.getAbsolutePath());

    final LibraryParsingErrors errors = new LibraryParsingErrors(library.getName());

    int correct = 0;
    int error = 0;
    // create db
    // decision: one parser for the whole file instead of one per line. json lines is a sequence of
    // root level objects, which jackson reads natively, and feeding it bytes lets its utf-8 parser
    // decode inline instead of building a String for every line first.
    try (InputStream in = new BufferedInputStream(new FileInputStream(dataBaseFile),
        READ_BUFFER); JsonParser p = JsonUtils.FACTORY.createParser(in)) {
      // the format is json lines, but some libraries wrap the same entries in one big json array.
      // Stepping into that array makes both read as a sequence of entry objects
      JsonToken first = p.nextToken();
      if (first == JsonToken.START_ARRAY) {
        first = p.nextToken();
      }

      for (JsonToken token = first; token == JsonToken.START_OBJECT; token = p.nextToken()) {
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }

        try {
          SpectralLibraryEntry entry = getDBEntry(errors, library, p);
          if (entry != null) {
            correct++;
            // add entry and process
            addLibraryEntry(library.getStorage(), errors, entry);
          } else {
            error++;
          }
        } catch (Exception ex) {
          errors.addUnknownException(ex.getMessage());
          // add this to count unknown errors and log first 5 only
          int unknowns = errors.addUnknownException("unknown error");
          if (unknowns <= 5 && isExtensiveErrorLogging()) {
            logger.log(Level.WARNING, "Error for entry: " + ex.getMessage(), ex);
          }

          error++;
          // the entry may have failed part way through, drop the rest of it
          p.skipChildren();
        }
        // to many errors? wrong data format?
        if (error > 5 && correct < 5) {
          logger.log(Level.WARNING, "This file was no mzmine spectral json library");
          return false;
        }
        processedLines.incrementAndGet();
        processedBytes.set(p.currentLocation().getByteOffset());
      }
    }
    finishByteProgress();
    // finish and process last entries
    finish();

    // log errors
    logger.info(isExtensiveErrorLogging() ? errors.toString() : errors.toStringShort());

    return true;
  }

  /**
   * Reads one library entry. The parser is positioned on its opening brace and is left on the
   * matching closing brace.
   *
   * @return the entry or null if it carried no signals
   */
  @Nullable
  private SpectralLibraryEntry getDBEntry(@NotNull final LibraryParsingErrors errors,
      @NotNull final SpectralLibrary library, @NotNull final JsonParser p) throws IOException {
    final Map<DBEntryField, Object> map = new EnumMap<>(DBEntryField.class);
    double[] mzs = null;
    double[] intensities = null;

    while (p.nextToken() == JsonToken.FIELD_NAME) {
      final String id = p.currentName();
      final JsonToken value = p.nextToken();

      if (PEAKS_KEY.equals(id)) {
        final double[][] signals = readSignals(p);
        mzs = signals[0];
        intensities = signals[1];
        continue;
      }

      final DBEntryField f = DBEntryField.forMZmineJsonIDExact(id);
      if (f == null) {
        // nested values of unknown keys still need to be consumed
        p.skipChildren();
        continue;
      }

      Object o = null;
      try {
        o = getValue(p, value, f);
        // add value
        if (o != null) {
          map.put(f, o);
        }
      } catch (Exception e) {
        errors.addValueParsingError(f, id, o == null ? "null value" : o.toString());
        // pushed logging to later in the errors object to not overflow log
      }
    }

    if (mzs == null) {
      errors.addUnknownException("Error parsing data points");
      return null;
    }
    return SpectralLibraryEntryFactory.create(library.getStorage(), map, mzs, intensities);
  }

  /**
   * Reads the peaks array, an array of [mz, intensity] pairs. The parser is positioned on its
   * opening bracket.
   *
   * @return mz values in [0] and intensities in [1]
   */
  private static double[][] readSignals(@NotNull final JsonParser p) throws IOException {
    if (p.currentToken() != JsonToken.START_ARRAY) {
      throw new IOException("peaks is no json array");
    }

    double[] mzs = new double[INITIAL_SIGNALS];
    double[] intensities = new double[INITIAL_SIGNALS];
    int n = 0;

    while (p.nextToken() == JsonToken.START_ARRAY) {
      if (n == mzs.length) {
        mzs = Arrays.copyOf(mzs, n * 2);
        intensities = Arrays.copyOf(intensities, n * 2);
      }
      p.nextToken();
      mzs[n] = p.getDoubleValue();
      p.nextToken();
      intensities[n] = p.getDoubleValue();
      n++;

      // tolerate additional values in a signal, only mz and intensity are used
      for (JsonToken t = p.nextToken(); t != JsonToken.END_ARRAY; t = p.nextToken()) {
        if (t == null) {
          throw new IOException("peaks ended inside a signal");
        }
      }
    }
    return new double[][]{Arrays.copyOf(mzs, n), Arrays.copyOf(intensities, n)};
  }

  /**
   * @param value the token of the value, the parser is positioned on it
   */
  @Nullable
  private static Object getValue(@NotNull final JsonParser p, @NotNull final JsonToken value,
      @NotNull final DBEntryField f) throws IOException {
    final Object o = switch (value) {
      case VALUE_STRING -> f.convertValue(p.getText());
      case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> {
        final Class<?> clazz = f.getObjectClass();
        if (clazz.equals(Integer.class)) {
          yield p.getIntValue();
        } else if (clazz.equals(Float.class)) {
          yield (float) p.getDoubleValue();
        } else if (clazz.equals(Double.class)) {
          yield p.getDoubleValue();
        } else if (clazz.equals(Long.class)) {
          yield p.getLongValue();
        } else {
          // getText is the number exactly as written in the file
          yield f.convertValue(p.getText());
        }
      }
      case VALUE_TRUE -> Boolean.TRUE;
      case VALUE_FALSE -> Boolean.FALSE;
      case VALUE_NULL -> null;
      // objects and arrays are converted from their json text
      case START_OBJECT, START_ARRAY -> f.convertValue(copyRawJson(p));
      default -> null;
    };
    if (o != null && o.equals("N/A")) {
      return null;
    }
    return o;
  }

  /**
   * Nested arrays and objects are handed to {@link DBEntryField#convertValue(String)} as json text.
   * Copies the structure the parser sits on, writing numbers with the notation they have in the
   * file so nothing is reformatted on the way through a double. Leaves the parser on the closing
   * bracket.
   *
   * @return the json text of the structure the parser is positioned on
   */
  private static String copyRawJson(@NotNull final JsonParser p) throws IOException {
    final StringWriter out = new StringWriter(64);
    try (JsonGenerator gen = JsonUtils.FACTORY.createGenerator(out)) {
      int depth = 0;
      for (JsonToken t = p.currentToken(); ; t = p.nextToken()) {
        if (t == null) {
          throw new IOException("Json ended inside a nested value");
        }
        switch (t) {
          case START_OBJECT -> {
            gen.writeStartObject();
            depth++;
          }
          case END_OBJECT -> {
            gen.writeEndObject();
            depth--;
          }
          case START_ARRAY -> {
            gen.writeStartArray();
            depth++;
          }
          case END_ARRAY -> {
            gen.writeEndArray();
            depth--;
          }
          case FIELD_NAME -> gen.writeFieldName(p.currentName());
          case VALUE_STRING -> gen.writeString(p.getText());
          // as written in the file, jackson would otherwise render it back from a double
          case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> gen.writeNumber(p.getText());
          case VALUE_TRUE -> gen.writeBoolean(true);
          case VALUE_FALSE -> gen.writeBoolean(false);
          case VALUE_NULL -> gen.writeNull();
          default -> throw new IOException("Unexpected token in nested value: " + t);
        }
        if (depth == 0) {
          break;
        }
      }
    }
    return out.toString();
  }
}
