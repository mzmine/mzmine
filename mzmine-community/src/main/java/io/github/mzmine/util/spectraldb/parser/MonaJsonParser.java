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

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.io.CountingInputStream;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// top level json objects/arrays

/**
 * Mass bank of North America (MONA) json database files
 *
 * @author Robin Schmid
 */
public class MonaJsonParser extends SpectralDBTextParser {

  private static final String COMPOUND = "compound", MONA_ID = "id", META_DATA = "metaData", SPECTRUM = "spectrum", SPLASH = "splash", SUBMITTER = "submitter";

  private static final Logger logger = Logger.getLogger(MonaJsonParser.class.getName());

  private static final int READ_BUFFER = 1 << 16;

  /**
   * Lines parsed in parallel at a time. Keeps memory bounded on large libraries while still giving
   * the worker threads enough to import.
   */
  private static final int PARALLEL_BATCH = 512;

  public MonaJsonParser(int bufferEntries, LibraryEntryProcessor processor,
      boolean extensiveErrorLogging) {
    super(bufferEntries, processor, extensiveErrorLogging);
  }

  @Override
  public boolean parse(@Nullable AbstractTask mainTask, @NotNull File dataBaseFile,
      @NotNull SpectralLibrary library) throws IOException {
    // progress from the bytes consumed instead of the line counting pass of the super
    // implementation, which would read the whole file a second time
    initByteProgress(dataBaseFile);
    logger.info("Parsing MONA spectral json library " + dataBaseFile.getAbsolutePath());

    AtomicInteger correct = new AtomicInteger(0);
    AtomicInteger error = new AtomicInteger(0);

    List<SpectralLibraryEntry> results = new ArrayList<>();

    final LibraryParsingErrors errors = new LibraryParsingErrors(library.getName());

    // create db
    try (CountingInputStream counting = new CountingInputStream(
        new BufferedInputStream(new FileInputStream(dataBaseFile),
            READ_BUFFER)); BufferedReader br = new BufferedReader(
        new InputStreamReader(counting, StandardCharsets.UTF_8))) {
      // test on first ten if it is really a MoNA file
      String l = br.readLine();
      while (l != null) {
        if (l.length() > 2) {
          final SpectralLibraryEntry entry = parseLineToEntry(errors, library, correct, error, l);
          if (entry != null) {
            results.add(entry);
          }
        }
        processedLines.incrementAndGet();
        processedBytes.set(counting.getCount());

        if ((correct.get() + error.get()) >= 4) {
          break;
        }

        l = br.readLine();
      }

      if (error.get() > correct.get()) {
        logger.warning("Stopping to parse file " + dataBaseFile.getName() + " as MoNA library, "
            + "there were too many entries with mismatching format. This is usually the case when "
            + "reading GNPS json libraries and just to determine the file type.");
        return false;
      }

      // the format check passed, hand over what it already parsed
      for (final SpectralLibraryEntry entry : results) {
        addLibraryEntry(library.getStorage(), errors, entry);
      }

      // read the rest in batches rather than parsing the whole multi GB library all in memory
      // send entries to addLibraryEntry so zero intensity signals and profile spectra are filtered
      final List<String> batch = new ArrayList<>(PARALLEL_BATCH);
      for (String line = br.readLine(); line != null; line = br.readLine()) {
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }
        processedLines.incrementAndGet();
        // counts what the reader pulled from the file, so it runs slightly ahead of the line
        // being handled here. Good enough for a progress bar and capped at 1
        processedBytes.set(counting.getCount());

        if (line.length() > 2) {
          batch.add(line);
        }
        if (batch.size() >= PARALLEL_BATCH) {
          parseBatch(errors, library, correct, error, batch);
          batch.clear();
        }
      }
      parseBatch(errors, library, correct, error, batch);

      if (error.get() > 0) {
        logger.warning(
            String.format("MoNA spectral library %s was imported with %d entries failing.",
                dataBaseFile.getName(), error.get()));
      }
      finishByteProgress();
      // finish and process last entries
      finish();

      // log errors
      logger.info(isExtensiveErrorLogging() ? errors.toString() : errors.toStringShort());

      return true;
    }
  }

  /**
   * Parses a batch of lines in parallel and adds the entries in file order.
   */
  private void parseBatch(@NotNull final LibraryParsingErrors errors,
      @NotNull final SpectralLibrary library, @NotNull final AtomicInteger correct,
      @NotNull final AtomicInteger error, @NotNull final List<String> batch) {
    if (batch.isEmpty()) {
      return;
    }
    final List<SpectralLibraryEntry> entries = batch.parallelStream()
        .map(line -> parseLineToEntry(errors, library, correct, error, line))
        .filter(Objects::nonNull).toList();
    for (final SpectralLibraryEntry entry : entries) {
      addLibraryEntry(library.getStorage(), errors, entry);
    }
  }

  private SpectralLibraryEntry parseLineToEntry(LibraryParsingErrors errors,
      SpectralLibrary library, AtomicInteger correct, AtomicInteger error, String l) {
    try {
      final SpectralLibraryEntry entry = parseToEntry(errors, library, l);
      if (entry != null) {
        correct.getAndIncrement();
        return entry;
      } else {
        error.getAndIncrement();
      }
    } catch (Exception ex) {
//      logger.log(Level.FINEST, "During mona parser read: " + ex.getMessage());
      error.getAndIncrement();
    }
    return null;
  }

  @Nullable
  private SpectralLibraryEntry parseToEntry(LibraryParsingErrors errors, SpectralLibrary library,
      String line) throws IOException {
    return getDBEntry(errors, library, JsonUtils.MAPPER.readTree(line));
  }

  private SpectralLibraryEntry getDBEntry(LibraryParsingErrors errors, SpectralLibrary library,
      JsonNode main) {
    // extract dps
    DataPoint[] dps = getDataPoints(errors, main);
    if (dps == null || dps.length == 0) {
      return null;
    }
    // metadata
    Map<DBEntryField, Object> map = new EnumMap<>(DBEntryField.class);
    extractAllFields(errors, main, map);
    return SpectralLibraryEntryFactory.create(library.getStorage(), map, dps);
  }

  private void extractAllFields(LibraryParsingErrors errors, JsonNode main,
      Map<DBEntryField, Object> map) {
    for (DBEntryField f : DBEntryField.values()) {
      Object value = null;
      JsonNode j = null;

      switch (f) {
        case INCHI:
          value = readCompound(main, "inchi");
          if (value == null) {
            value = readCompoundMetaData(main, "InChI");
          }
          break;
        case INCHIKEY:
          value = readCompound(main, "inchiKey");
          if (value == null) {
            value = readCompoundMetaData(main, "InChIKey");
          }
          break;
        case ACQUISITION:
          break;
        case MONA_ID:
          value = readMetaData(main, "accession");
          break;
        case CAS:
          // TODO check real id (cas CAS ?)
          value = readCompoundMetaData(main, "cas");
          break;
        case CHARGE:
          break;
        case COLLISION_ENERGY:
          value = readMetaData(main, "collision energy");
          break;
        case COMMENT:
          break;
        case DATA_COLLECTOR:
          value = readMetaData(main, "author");
          break;
        case INSTRUMENT:
          value = readMetaData(main, "instrument");
          break;
        case INSTRUMENT_TYPE:
          value = readMetaData(main, "instrument type");
          break;
        case MS_LEVEL:
          value = readMetaData(main, "ms level");
          break;
        case RESOLUTION:
          value = readMetaData(main, "resolution");
          if (value != null) {
            value = value.toString();
          }
          break;
        case ION_TYPE:
          value = readMetaData(main, "precursor type");
          break;
        case POLARITY:
          value = readMetaData(main, "ionization mode");
          break;
        case ION_SOURCE:
          value = readMetaData(main, "ionization");
          break;
        case EXACT_MASS:
          value = readMetaDataDouble(main, "exact mass");
          break;
        case MOLWEIGHT:
          value = readMetaDataDouble(main, "exact mass");
          break;
        case PRECURSOR_MZ:
          value = readMetaDataDouble(main, "precursor m/z");
          break;
        case NAME:
          // can have multiple names
          value = readCompoundNames(main);
          break;
        case NUM_PEAKS:
          break;
        case PRINCIPAL_INVESTIGATOR:
          value = readMetaData(main, "author");
          break;
        case CHEMSPIDER:
          j = readCompoundMetaDataJson(main, "chemspider");
          if (j != null) {
            if (j.isTextual()) {
              value = j.textValue();
            }
            if (j.isNumber()) {
              value = j.intValue();
            }
          }
          break;
        case PUBCHEM:
          j = readCompoundMetaDataJson(main, "pubchem cid");
          if (j != null) {
            if (j.isTextual()) {
              value = j.textValue();
            }
            if (j.isNumber()) {
              value = j.intValue();
            }
          }
          break;
        case FORMULA:
          value = readCompoundMetaData(main, "molecular formula");
          break;
        case PUBMED:
          break;
        case RT:
          // MoNA writes the unit next to the value, like "13.601 min" or "42 sec".
          final Object rt = readMetaData(main, "retention time");
          if (rt instanceof String text && text.toLowerCase().contains("sec")) {
            try {
              value = Float.parseFloat(text.replaceAll("[^0-9.]", "")) / 60f;
            } catch (NumberFormatException ex) {
              errors.addValueParsingError(f, "retention time", text);
            }
          } else {
            value = rt;
          }
          break;
        case SMILES:
          value = readCompoundMetaData(main, "SMILES");
          break;
        case SOFTWARE:
          break;
        case SYNONYMS:
          break;
        default:
          break;
      }

      putConverted(errors, map, f, value);
    }
  }

  /**
   * MoNA stores its values as plain json text and numbers, so each one goes through
   * {@link DBEntryField#convertValue(String)} like in the other parsers. Without it the value keeps
   * whatever shape the file had: an ms level stays the text "MS2", a polarity stays "positive"
   * instead of being harmonized, and a collision energy stays text instead of the
   * {@link FloatArrayList} its field is declared as.
   */
  private void putConverted(@NotNull final LibraryParsingErrors errors,
      @NotNull final Map<DBEntryField, Object> map, @NotNull final DBEntryField f,
      @Nullable final Object value) {
    if (value == null) {
      return;
    }
    final String content = value.toString().trim();
    if (content.isEmpty() || "n/a".equalsIgnoreCase(content)) {
      return;
    }

    try {
      final Object converted = f.convertValue(content);
      if (converted != null) {
        map.put(f, converted);
      }
    } catch (Exception ex) {
      // a single unparsable value must not drop the whole entry, so keep it without this field
      errors.addValueParsingError(f, f.toString(), content);
    }
  }

  /**
   * read from META_DATA array
   *
   * @param main
   * @param id
   * @return String or Number or null
   */
  private Object readMetaData(JsonNode main, String id) {
    final JsonNode j = findMetaDataValue(main.path(META_DATA), id);
    if (j != null) {
      if (j.isTextual()) {
        return j.textValue();
      }
      if (j.isNumber()) {
        return j.numberValue();
      }
    }
    return null;
  }

  private Double readMetaDataDouble(JsonNode main, String id) {
    final JsonNode value = findMetaDataValue(main.path(META_DATA), id);
    if (value == null) {
      return null;
    }
    return value.isNumber() ? value.doubleValue() : Double.parseDouble(value.asText());
  }

  private JsonNode readCompoundMetaDataJson(JsonNode main, String id) {
    return findMetaDataValue(firstCompound(main).path(META_DATA), id);
  }

  /**
   * read from COMPOUND...META_DATA array
   *
   * @param main
   * @param id
   * @return
   */
  private String readCompoundMetaData(JsonNode main, String id) {
    return text(findMetaDataValue(firstCompound(main).path(META_DATA), id));
  }

  /**
   * Read from COMPOUND object
   *
   * @param main
   * @param id
   * @return
   */
  private String readCompound(JsonNode main, String id) {
    return text(firstCompound(main).get(id));
  }

  /**
   * @return all names of the first compound joined by ", " or null if it carries none
   */
  @Nullable
  private static String readCompoundNames(JsonNode main) {
    final StringJoiner joiner = new StringJoiner(", ");
    int found = 0;
    for (final JsonNode entry : firstCompound(main).path("names")) {
      final String name = text(entry.get("name"));
      if (name != null) {
        joiner.add(name);
        found++;
      }
    }
    return found == 0 ? null : joiner.toString();
  }

  /**
   * The first entry of the compound array, or a missing node so callers can keep chaining.
   */
  private static JsonNode firstCompound(JsonNode main) {
    return main.path(COMPOUND).path(0);
  }

  /**
   * Value of the first {name, value} pair in a metaData array that carries this name.
   *
   * @return the value node or null if no pair matches
   */
  @Nullable
  private static JsonNode findMetaDataValue(JsonNode metaData, String name) {
    for (final JsonNode pair : metaData) {
      if (name.equals(text(pair.get("name")))) {
        return pair.get("value");
      }
    }
    return null;
  }

  /**
   * @return the text of a json string, null for anything else including missing values
   */
  @Nullable
  private static String text(@Nullable JsonNode node) {
    return node != null && node.isTextual() ? node.textValue() : null;
  }

  private DataPoint[] getDataPoints(LibraryParsingErrors errors, JsonNode main) {
    String spec = text(main.get("spectrum"));
    if (spec == null) {
      errors.addUnknownException("'spectrum' key for data points not found");
      return null;
    }

    try {
      String[] data = spec.split(" ");
      DataPoint[] dps = new DataPoint[data.length];
      for (int i = 0; i < dps.length; i++) {
        String[] dp = data[i].split(":");
        double mz = Double.parseDouble(dp[0]);
        double intensity = Double.parseDouble(dp[1]);
        dps[i] = new SimpleDataPoint(mz, intensity);
      }
      return dps;
    } catch (Exception e) {
      errors.addUnknownException("Error parsing data points");
      throw e; // was thrown before to count errors
    }
  }

}
