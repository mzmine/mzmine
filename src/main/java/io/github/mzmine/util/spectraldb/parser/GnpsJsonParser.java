/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.spectraldb.parser;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GnpsJsonParser extends SpectralDBTextParser {

  private final static Logger logger = Logger.getLogger(GnpsJsonParser.class.getName());

  public GnpsJsonParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile) throws IOException {
    super.parse(mainTask, dataBaseFile);

    logger.info("Parsing GNPS spectral library " + dataBaseFile.getAbsolutePath());

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
          SpectralDBEntry entry = getDBEntry(json);
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
          logger.log(Level.WARNING, "This file was no GNPS spectral json library");
          return false;
        }
        processedLines.incrementAndGet();
      }
    }
    // finish and process last entries
    finish();

    return true;
  }

  public SpectralDBEntry getDBEntry(JsonObject main) {
    // extract dps
    DataPoint[] dps = getDataPoints(main);
    if (dps == null) {
      return null;
    }

    // extract meta data
    Map<DBEntryField, Object> map = new EnumMap<>(DBEntryField.class);
    for (DBEntryField f : DBEntryField.values()) {
      String id = f.getGnpsJsonID();
      if (id != null && !id.isEmpty()) {

        try {
          Object o = null;
          if (f.getObjectClass() == Double.class || f.getObjectClass() == Integer.class
              || f.getObjectClass() == Float.class) {
            o = main.getJsonNumber(id);
          } else {
            o = main.getString(id, null);
            if (o != null && o.equals("N/A")) {
              o = null;
            }
          }
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
          logger.log(Level.WARNING, "Cannot convert value to its type", e);
        }
      }
    }

    return new SpectralDBEntry(map, dps);
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
