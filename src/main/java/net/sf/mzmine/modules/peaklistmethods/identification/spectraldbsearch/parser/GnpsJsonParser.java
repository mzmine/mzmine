/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.DBEntryField;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.dbentry.SpectralDBEntry;
import net.sf.mzmine.taskcontrol.AbstractTask;

public class GnpsJsonParser extends SpectralDBParser {
  private static Logger logger = Logger.getLogger(GnpsJsonParser.class.getName());

  public GnpsJsonParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile) throws IOException {
    logger.info("Parsing GNPS spectral library " + dataBaseFile.getAbsolutePath());

    int correct = 0;
    int error = 0;
    // create db
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null;) {
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }

        JsonReader reader = null;
        try {
          reader = Json.createReader(new StringReader(l));
          JsonObject json = reader.readObject();
          SpectralDBEntry entry = getDBEntry(json);
          if (entry != null && entry.getPrecursorMZ() != null) {
            correct++;
            // add entry and process
            addLibraryEntry(entry);
          } else
            error++;
        } catch (Exception ex) {
          error++;
          logger.log(Level.WARNING, "Error for entry", ex);
        } finally {
          if (reader != null)
            reader.close();
        }
        // to many errors? wrong data format?
        if (error > 5 && correct < 5) {
          logger.log(Level.WARNING, "This file was no GNPS spectral json library");
          return false;
        }
      }
    }
    // finish and process last entries
    finish();

    return true;
  }

  public SpectralDBEntry getDBEntry(JsonObject main) {
    // extract dps
    DataPoint[] dps = getDataPoints(main);
    if (dps == null)
      return null;

    // extract meta data
    Map<DBEntryField, Object> map = new EnumMap<>(DBEntryField.class);
    for (DBEntryField f : DBEntryField.values()) {
      String id = f.getGnpsJsonID();
      if (id != null && !id.isEmpty()) {

        Object o = null;
        if (f.getObjectClass() == Double.class || f.getObjectClass() == Integer.class
            || f.getObjectClass() == Float.class) {
          o = main.getJsonNumber(id);
        } else {
          o = main.getString(id, null);
          if (o != null && o.equals("N/A"))
            o = null;
        }
        // add value
        if (o != null) {
          if (o instanceof JsonNumber) {
            if (f.getObjectClass().equals(Integer.class)) {
              o = ((JsonNumber) o).intValue();
            } else {
              o = ((JsonNumber) o).doubleValue();
            }
          }
          // add
          map.put(f, o);
        }
      }
    }

    return new SpectralDBEntry(map, dps);
  }


  /**
   * Data points or null
   * 
   * @param main
   * @return
   */
  public DataPoint[] getDataPoints(JsonObject main) {
    JsonArray data = main.getJsonArray("peaks");
    if (data == null)
      return null;

    DataPoint[] dps = new DataPoint[data.size()];
    for (int i = 0; i < data.size(); i++) {
      double mz = data.getJsonArray(i).getJsonNumber(0).doubleValue();
      double intensity = data.getJsonArray(i).getJsonNumber(1).doubleValue();
      dps[i] = new SimpleDataPoint(mz, intensity);
    }
    return dps;
  }
}
