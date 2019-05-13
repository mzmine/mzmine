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

    int correct = 0;
    boolean isData = false;
    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    List<DataPoint> dps = new ArrayList<>();
    // create db
    int sep = -1;
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
            if (sep != -1) {
              DBEntryField field = DBEntryField.forMspID(l.substring(0, sep));
              if (field != null) {
                String content = l.substring(sep + 2, l.length());
                if (content.length() > 0) {
                  try {
                    Object value = field.convertValue(content);
                    fields.put(field, value);
                  } catch (Exception e) {
                    logger.log(Level.WARNING, "Cannot convert value type of " + content + " to "
                        + field.getObjectClass().toString(), e);
                  }
                }
              }
            } else {
              // data?
              String[] dataAndComment = l.split("\"");
              String[] data = dataAndComment[0].split(" ");
              if (data.length == 2) {
                try {
                  dps.add(new SimpleDataPoint(Double.parseDouble(data[0]),
                      Double.parseDouble(data[1])));
                  isData = true;
                } catch (Exception e) {
                  isData = false;
                }
              }
            }
          }
          if (isData) {
            // empty row after data
            // add entry and reset
            SpectralDBEntry entry =
                new SpectralDBEntry(fields, dps.toArray(new DataPoint[dps.size()]));
            fields = new EnumMap<>(fields);
            dps.clear();
            // add and push
            addLibraryEntry(entry);
            correct++;
          }
          // reset
          isData = false;
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error for entry", ex);
        }
      }
      // finish and process all entries
      finish();
      return true;
    }
  }

}
