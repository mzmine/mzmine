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

/**
 * Main format for library entries in GNPS
 * 
 * @author Robin Schmid
 *
 */
public class GnpsMgfParser extends SpectralDBParser {

  public GnpsMgfParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  private static Logger logger = Logger.getLogger(GnpsMgfParser.class.getName());

  private enum State {
    WAIT_FOR_META, META, DATA;
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile) throws IOException {
    logger.info("Parsing mgf spectral library " + dataBaseFile.getAbsolutePath());

    // BEGIN IONS
    // meta data
    // SCANS=1 .... n (the scan ID; could be used to put all spectra of the same entry together)
    // data
    // END IONS

    int correct = 0;
    State state = State.WAIT_FOR_META;
    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    List<DataPoint> dps = new ArrayList<>();
    int sep = -1;
    // create db
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null;) {
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }
        try {
          if (l.length() > 1) {
            // meta data start?
            if (state.equals(State.WAIT_FOR_META)) {
              if (l.equalsIgnoreCase("BEGIN IONS")) {
                fields = new EnumMap<>(fields);
                dps.clear();
                state = State.META;
              }
            } else {
              if (l.equalsIgnoreCase("END IONS")) {
                // add entry and reset
                if (fields.size() > 1 && dps.size() > 1) {
                  SpectralDBEntry entry =
                      new SpectralDBEntry(fields, dps.toArray(new DataPoint[dps.size()]));
                  // add and push
                  addLibraryEntry(entry);
                  correct++;
                }
                state = State.WAIT_FOR_META;
              } else if (l.toLowerCase().startsWith("scans")) {
                // belongs to the previously created entry and is another spectrum

                // data starts
                state = State.DATA;
              } else {
                switch (state) {
                  case WAIT_FOR_META:
                    // wait for next entry
                    break;
                  case DATA:
                    String[] data = l.split("\t");
                    dps.add(new SimpleDataPoint(Double.parseDouble(data[0]),
                        Double.parseDouble(data[1])));
                    break;
                  case META:
                    sep = l.indexOf('=');
                    if (sep != -1 && sep < l.length() - 1) {
                      DBEntryField field = DBEntryField.forMgfID(l.substring(0, sep));
                      if (field != null) {
                        String content = l.substring(sep + 1, l.length());
                        if (!content.isEmpty()) {
                          try {
                            Object value = field.convertValue(content);
                            fields.put(field, value);
                          } catch (Exception e) {
                            logger.log(Level.WARNING, "Cannot convert value type of " + content
                                + " to " + field.getObjectClass().toString(), e);
                          }
                        }
                      }
                    }
                    break;
                }
              }
            }
          }
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error for entry", ex);
          state = State.WAIT_FOR_META;
        }
      }
      // finish and process all entries
      finish();
      return true;
    }
  }

}
