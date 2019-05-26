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

package net.sf.mzmine.util.spectraldb.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

public class NistMspParser extends SpectralDBParser {

  public NistMspParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  private static Logger logger = Logger.getLogger(NistMspParser.class.getName());

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile) throws IOException {
    logger.info("Parsing NIST msp spectral library " + dataBaseFile.getAbsolutePath());

    // metadata fields and data points
    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    List<DataPoint> dps = new ArrayList<>();
    // separation index (metadata is separated by ': '
    int sep = -1;
    // currently loading data?
    boolean isData = false;

    // read DB file
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null;) {
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }
        try {
          if (l.length() > 1) {
            // meta data?
            sep = isData ? -1 : l.indexOf(": ");
            if (sep != -1 && sep < l.length() - 2) {
              extractMetaData(fields, l, sep);
            } else {
              // data?
              DataPoint dp = extractDataPoint(l);
              if (dp != null) {
                dps.add(dp);
                isData = true;
              } else
                isData = false;
            }
          } else {
            // empty row
            if (isData) {
              // empty row after data
              // add entry and reset
              SpectralDBEntry entry =
                  new SpectralDBEntry(fields, dps.toArray(new DataPoint[dps.size()]));
              // add and push
              addLibraryEntry(entry);
              // reset
              fields = new EnumMap<>(fields);
              dps.clear();
              isData = false;
            }
          }
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error for entry", ex);
          // reset on error
          isData = false;
          fields = new EnumMap<>(fields);
          dps.clear();
        }
      }
      // finish and process all entries
      finish();
      return true;
    }
  }

  /**
   * Extract data point
   * 
   * @param line
   * @return DataPoint or null
   */
  @Nullable
  private DataPoint extractDataPoint(String line) {
    // comment possible as mz intensity"
    String[] dataAndComment = line.split("\"");
    // split by space
    String[] data = dataAndComment[0].split(" ");
    if (data.length == 2) {
      try {
        return new SimpleDataPoint(Double.parseDouble(data[0]), Double.parseDouble(data[1]));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Cannot parse data point", e);
      }
    }
    return null;
  }

  /**
   * Extracts metadata from a line which is separated by ': ' and inserts the metadata inta a map
   * 
   * @param fields The map of metadata fields
   * @param line String with metadata
   * @param sep index of the separation char ':'
   */
  private void extractMetaData(Map<DBEntryField, Object> fields, String line, int sep) {
    String key = line.substring(0, sep);
    DBEntryField field = DBEntryField.forMspID(key);
    if (field != null) {
      // spe +2 for colon and space
      String content = line.substring(sep + 2, line.length());
      if (content.length() > 0) {
        try {
          // convert into value type
          Object value = field.convertValue(content);
          fields.put(field, value);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Cannot convert value type of " + content + " to "
              + field.getObjectClass().toString(), e);
        }
      }
    }
  }

}
