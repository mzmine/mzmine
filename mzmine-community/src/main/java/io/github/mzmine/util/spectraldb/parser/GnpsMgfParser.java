/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
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

/**
 * Main format for library entries in GNPS
 *
 * @author Robin Schmid
 */
public class GnpsMgfParser extends SpectralDBTextParser {

  public GnpsMgfParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  private final static Logger logger = Logger.getLogger(GnpsMgfParser.class.getName());

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile, SpectralLibrary library)
      throws IOException {
    super.parse(mainTask, dataBaseFile, library);
    logger.info("Parsing mgf spectral library " + dataBaseFile.getAbsolutePath());

    // BEGIN IONS
    // meta data
    // SCANS=1 .... n (the scan ID; could be used to put all spectra of the
    // same entry together)
    // data
    // END IONS

    int correct = 0;
    State state = State.WAIT_FOR_META;
    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    List<DataPoint> dps = new ArrayList<>();
    int sep = -1;
    // create db
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null; ) {
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
                  SpectralLibraryEntry entry = SpectralLibraryEntryFactory.create(
                      library.getStorage(), fields, dps.toArray(new DataPoint[dps.size()]));
                  // add and push
                  addLibraryEntry(entry);
                  correct++;
                }
                state = State.WAIT_FOR_META;
              } else {
                sep = l.indexOf('=');
                if (sep == -1) {
                  // data starts
                  state = State.DATA;
                }
                switch (state) {
                  case WAIT_FOR_META:
                    // wait for next entry
                    break;
                  case DATA:
                    // split for any white space (tab or space ...)
                    String[] data = l.split("\\s+");
                    dps.add(new SimpleDataPoint(Double.parseDouble(data[0]),
                        Double.parseDouble(data[1])));
                    break;
                  case META:
                    if (sep != -1 && sep < l.length() - 1) {
                      DBEntryField field = DBEntryField.forMgfID(l.substring(0, sep));
                      if (field != null) {
                        String content = l.substring(sep + 1);
                        if (!content.isBlank()) {
                          try {
                            // allow 1+ as 1 and 2- as -2
                            if (field.equals(DBEntryField.CHARGE)) {
                              content = parseCharge(content);
                            }

                            Object value = field.convertValue(content);

                            // only attempt parsing of adduct from name if there is no adduct already.
                            if (field.equals(DBEntryField.NAME)
                                && fields.get(DBEntryField.ION_TYPE) == null) {
                              String name = ((String) value);
                              int lastSpace = name.lastIndexOf(' ');
                              if (lastSpace != -1 && lastSpace < name.length() - 2) {
                                String adductCandidate = name.substring(lastSpace + 1);
                                // check for valid
                                // adduct with the
                                // adduct parser
                                // from export
                                // use as adduct
                                IonType adduct = IonTypeParser.parse(adductCandidate);
                                if (adduct != null && !adduct.isUndefinedAdduct()) {
                                  fields.put(DBEntryField.ION_TYPE, adduct.toString(false));
                                }
                              }
                            }
                            // retention time is in seconds, mzmine uses minutes
                            if (field.equals(DBEntryField.RT)) {
                              value = ((Float) value) / 60.f;
                            }

                            if (value != null) {
                              fields.put(field, value);
                            }
                          } catch (Exception e) {
                            logger.log(Level.WARNING,
                                "Cannot convert value type of " + content + " to "
                                    + field.getObjectClass().toString(), e);
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
        processedLines.incrementAndGet();
      }
      // finish and process all entries
      finish();
      return true;
    }
  }

  private String parseCharge(final String str) {
    var lastChar = str.charAt(str.length() - 1);
    if (lastChar == '+' || lastChar == '-') {
      return lastChar + str.substring(0, str.length() - 1);
    }
    return str;
  }

  private enum State {
    WAIT_FOR_META, META, DATA
  }

}
