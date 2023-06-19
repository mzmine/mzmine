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

package io.github.mzmine.util.spectraldb.parser;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class MZmineJsonParser extends SpectralDBTextParser {

  private final static Logger logger = Logger.getLogger(MZmineJsonParser.class.getName());

  public MZmineJsonParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile, SpectralLibrary library)
      throws IOException {
    super.parse(mainTask, dataBaseFile, library);

    logger.info("Parsing MZmine spectral library " + dataBaseFile.getAbsolutePath());

    int correct = 0;
    int error = 0;
    // create db
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null; ) {
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }

        JsonReader reader = null;
        try {
          reader = Json.createReader(new StringReader(l));
          JsonObject json = reader.readObject();
          SpectralLibraryEntry entry = getDBEntry(library, json);
          if (entry != null) {
            correct++;
            // add entry and process
            addLibraryEntry(entry);
          } else {
            error++;
          }
        } catch (Exception ex) {
          error++;
          logger.log(Level.WARNING, "Error for entry", ex);
        } finally {
          if (reader != null) {
            reader.close();
          }
        }
        // to many errors? wrong data format?
        if (error > 5 && correct < 5) {
          logger.log(Level.WARNING, "This file was no MZmine spectral json library");
          return false;
        }
        processedLines.incrementAndGet();
      }
    }
    // finish and process last entries
    finish();

    return true;
  }

  @Nullable
  private static Object getValue(final JsonObject main, final DBEntryField f, final String id) {
    Object o = null;
    JsonValue value = main.get(id);
    switch (value.getValueType()) {
      case STRING, OBJECT -> {
        if (f.getObjectClass() == Double.class) {
          o = Double.parseDouble(main.getString(id));
        } else if (f.getObjectClass() == Integer.class) {
          o = Integer.parseInt(main.getString(id));
        } else if (f.getObjectClass() == Float.class) {
          o = Float.parseFloat(main.getString(id));
        } else {
          o = main.getString(id, null);
        }
      }
      case NUMBER -> {
        o = main.getJsonNumber(id);
      }
      case TRUE -> {
        o = true;
      }
      case FALSE -> {
        o = false;
      }
      case NULL -> {
        o = null;
      }
    }
    if (o != null && o.equals("N/A")) {
      return null;
    }
    return o;
  }

  private static JsonNumber getDoubleValue(final JsonObject main, final String id) {
    return main.getJsonNumber(id);
  }

  public SpectralLibraryEntry getDBEntry(SpectralLibrary library, JsonObject main) {
    // extract dps
    DataPoint[] dps = getDataPoints(main);
    if (dps == null) {
      return null;
    }

    // extract meta data
    Map<DBEntryField, Object> map = new EnumMap<>(DBEntryField.class);
    for (DBEntryField f : DBEntryField.values()) {
      String id = f.getMZmineJsonID();
      if (id != null && !id.isEmpty() && main.containsKey(id)) {

        try {
          Object o = getValue(main, f, id);
          // add value
          if (o != null) {
            if (o instanceof JsonNumber) {
              if (f.getObjectClass().equals(Integer.class)) {
                o = ((JsonNumber) o).intValue();
              } else if (f.getObjectClass().equals(Float.class)) {
                o = (float) ((JsonNumber) o).doubleValue();
              } else {
                o = ((JsonNumber) o).doubleValue();
              }
            }
            // add
            map.put(f, o);
          }
        } catch (Exception e) {
          logger.log(Level.WARNING,
              String.format("Cannot convert value %s to its type %s", f.getMZmineJsonID(),
                  f.getObjectClass().toString()), e);
        }
      }
    }

    return SpectralLibraryEntry.create(library.getStorage(), map, dps);
  }

  public static DataPoint[] getDataPointsFromJsonArray(JsonArray data) {
    if (data == null) {
      return null;
    }

    DataPoint[] dps = new DataPoint[data.size()];
    try {
      for (int i = 0; i < data.size(); i++) {
        final JsonArray dataPoint = data.getJsonArray(i);
        double mz = dataPoint.getJsonNumber(0).doubleValue();
        double intensity = dataPoint.getJsonNumber(1).doubleValue();
        dps[i] = new SimpleDataPoint(mz, intensity);
      }
      return dps;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Cannot convert DP values to doubles", e);
      return null;
    }
  }

  /**
   * Data points or null
   *
   * @param main
   * @return
   */
  public DataPoint[] getDataPoints(JsonObject main) {
    JsonArray data = main.getJsonArray("peaks");
    return getDataPointsFromJsonArray(data);
  }
}
