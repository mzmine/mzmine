/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main format for library entries in GNPS
 *
 * @author Robin Schmid
 */
public class GnpsMgfParser extends SpectralDBTextParser {

  /**
   * Gnps mgfs have the adduct after the name, e.g.
   * <p>
   * NAME=Umbelliferon M+H
   * <p>
   * NAME=Umbelliferon [2M-H2O+H]+
   * <p>
   * NAME=Gamma-aminobutyric_acid M-H
   * <p>
   * Looks like no brackets and charge indicator are present sometimes but sometimes they are (like
   * in IIMN library)
   * <p>
   * This predicate uses matcher.find to find any substring matching the regex
   */
  final Predicate<String> gnpsNameAdductPattern = Pattern.compile("(M[+\\-][\\d+\\-\\w]+)")
      .asPredicate();

  public GnpsMgfParser(int bufferEntries, LibraryEntryProcessor processor,
      boolean extensiveErrorLogging) {
    super(bufferEntries, processor, extensiveErrorLogging);
  }

  private final static Logger logger = Logger.getLogger(GnpsMgfParser.class.getName());

  @Override
  public boolean parse(@Nullable AbstractTask mainTask, @NotNull File dataBaseFile,
      @NotNull SpectralLibrary library) throws IOException {
    super.parse(mainTask, dataBaseFile, library);
    logger.info("Parsing mgf spectral library " + dataBaseFile.getAbsolutePath());

    final LibraryParsingErrors errors = new LibraryParsingErrors(library.getName());
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
    String[] sep = null;

    // flag that entry should be skipped
    boolean fatalEntryError = false;

    // create db
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null; ) {
        l = l.trim();
        // main task was canceled?
        if (mainTask != null && mainTask.isCanceled()) {
          return false;
        }
        try {
          if (l.length() > 1) {
            // meta data start?
            if (state.equals(State.WAIT_FOR_META)) {
              if (l.equalsIgnoreCase("BEGIN IONS")) {
                fields = new EnumMap<>(DBEntryField.class);
                dps.clear();
                state = State.META;
              }
            } else {
              if (l.equalsIgnoreCase("END IONS")) {
                // add entry and reset
                if (!fatalEntryError && fields.size() > 0 && dps.size() > 0) {
                  SpectralLibraryEntry entry = SpectralLibraryEntryFactory.create(
                      library.getStorage(), fields, dps.toArray(new DataPoint[dps.size()]));
                  // add and push
                  addLibraryEntry(library.getStorage(), errors, entry);
                  correct++;
                } else if (fatalEntryError) {
                  errors.addUnknownException("Skipped entry");
                }
                state = State.WAIT_FOR_META;
                fields = new EnumMap<>(DBEntryField.class);
                dps.clear();
                fatalEntryError = false;
              } else {
                // only 1 split into max of String[2]
                sep = l.split("=", 2);
                if (sep.length == 1) {
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
                    if (data.length < 2) {
                      // no data anymore
                      state = State.WAIT_FOR_META;
                    } else {
                      try {
                        dps.add(new SimpleDataPoint(Double.parseDouble(data[0]),
                            Double.parseDouble(data[1])));
                      } catch (Exception ex) {
                        fatalEntryError = true; // skip entry
                        // use generic message as exception will be unique for each value and will create too long error log
                        int dataPointErrors = errors.addUnknownException(
                            "Cannot parse data points");
                        // log the 2 first data point errors
                        if (dataPointErrors <= 2 && isExtensiveErrorLogging()) {
                          logger.log(Level.WARNING, "Cannot parse data point: " + ex.getMessage(),
                              ex);
                        }
                      }
                    }
                    break;
                  case META:
                    if (sep.length == 2) {
                      final String key = sep[0].trim();
                      String content = sep[1].trim();
                      // check many alternative names
                      DBEntryField field = DBEntryField.forID(key);

                      if (field == null) {
                        if (!key.isBlank()) {
                          errors.addUnknownKey(key);
                        }
                      } else {
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
                              tryExtractAdductFromName((String) value, fields);
                            }
                            // retention time is in seconds, mzmine uses minutes
                            if (field.equals(DBEntryField.RT)) {
                              value = ((Float) value) / 60.f;
                            }

                            if (value != null) {
                              fields.put(field, value);
                            }
                          } catch (Exception e) {
                            errors.addValueParsingError(field, key, content);
                            // pushed logging to later in the errors object to not overflow log
//                            logger.log(Level.WARNING,
//                                "Cannot convert value type of " + content + " to "
//                                    + field.getObjectClass().toString(), e);
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
          errors.addUnknownException(ex.getMessage());
          // add this to count unknown errors and log first 5 only
          int unknowns = errors.addUnknownException("unknown error");
          if (unknowns <= 5 && isExtensiveErrorLogging()) {
            logger.log(Level.WARNING, "Error for entry: " + ex.getMessage(), ex);
          }

          state = State.WAIT_FOR_META;
        }
        processedLines.incrementAndGet();
      }
      // finish and process all entries
      finish();

      // log errors
      logger.info(isExtensiveErrorLogging() ? errors.toString() : errors.toStringShort());

      return true;
    }
  }

  /**
   * Gnps mgfs have the adduct after the name, e.g.
   * <p></p>
   * NAME=Umbelliferon M+H
   * <p></p>
   * NAME=Gamma-aminobutyric_acid M-H
   *
   * @param value  the field value
   * @param fields the currently parsed fields
   */
  private void tryExtractAdductFromName(String value, Map<DBEntryField, Object> fields) {
    final String name = value;
    final int lastSpace = name.lastIndexOf(' ');
    if (lastSpace == -1 || lastSpace >= name.length() - 2) {
      return;
    }

    final String adductCandidate = name.substring(lastSpace + 1);
    // uses the Pattern.asPredicate() that internally uses matcher.find for substring match
    // matcher.match requires full match which does not work here
    if (!gnpsNameAdductPattern.test(adductCandidate)) {
      return;
    }
    // check for valid adduct with the adduct parser from export
    // use as adduct
    final IonType adduct = IonTypeParser.parse(adductCandidate);
    if (adduct != null && !adduct.isUndefinedAdduct()) {
      fields.put(DBEntryField.ION_TYPE, adduct.toString(false));
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
