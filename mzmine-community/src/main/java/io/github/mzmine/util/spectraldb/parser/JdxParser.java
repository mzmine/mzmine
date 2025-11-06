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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parser for .jdx DB files for spectra database matching
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class JdxParser extends SpectralDBTextParser {

  public JdxParser(int bufferEntries, LibraryEntryProcessor processor,
      boolean extensiveErrorLogging) {
    super(bufferEntries, processor, extensiveErrorLogging);
  }

  private static final Logger logger = Logger.getLogger(NistMspParser.class.getName());

  @Override
  public boolean parse(@Nullable AbstractTask mainTask, @NotNull File dataBaseFile,
      @NotNull SpectralLibrary library) throws IOException {
    super.parse(mainTask, dataBaseFile, library);
    logger.info("Parsing jdx spectral library " + dataBaseFile.getAbsolutePath());

    final LibraryParsingErrors errors = new LibraryParsingErrors(library.getName());

    boolean isData = false;
    Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
    List<DataPoint> dps = new ArrayList<>();
    // create db
    int sep = -1;
    try (BufferedReader br = new BufferedReader(new FileReader(dataBaseFile))) {
      for (String l; (l = br.readLine()) != null; ) {
        // main task was canceled?
        if (mainTask.isCanceled()) {
          return false;
        }

        try {
          // meta data?
          sep = isData ? -1 : l.indexOf("=");
          if (sep != -1) {
            final String key = l.substring(0, sep);
            DBEntryField field = DBEntryField.forJdxID(key);

            if (field == null) {
              if (!key.isBlank()) {
                errors.addUnknownKey(key);
              }
            } else {
              String content = l.substring(sep + 1);
              if (content.length() > 0) {
                try {
                  Object value = field.convertValue(content);
                  fields.put(field, value);
                } catch (Exception e) {
                  errors.addValueParsingError(field, key, content);
                }
              }
            }
          } else {
            // data?
            String[] dataPairs = l.split(" ");
            for (String dataPair : dataPairs) {
              String[] data = dataPair.split(",");
              if (data.length == 2) {
                try {
                  dps.add(new SimpleDataPoint(Double.parseDouble(data[0]),
                      Double.parseDouble(data[1])));
                  isData = true;
                } catch (Exception e) {
                  // use generic message as exception will be unique for each value and will create too long error log
                  int dataPointErrors = errors.addUnknownException("Cannot parse data points");
                  // log the 2 first data point errors
                  if (dataPointErrors <= 2 && isExtensiveErrorLogging()) {
                    logger.log(Level.WARNING, "Cannot parse data point: " + e.getMessage(), e);
                  }
                }
              }
            }
          }
          if (l.trim().equalsIgnoreCase("end")) {
            // row with END
            // add entry and reset
            if (!fields.isEmpty() || !dps.isEmpty()) {
              // skipped some read information
              errors.addUnknownException("Skipped entry");
            } else {
              SpectralLibraryEntry entry = SpectralLibraryEntryFactory.create(library.getStorage(),
                  fields, dps.toArray(new DataPoint[dps.size()]));
              addLibraryEntry(library.getStorage(), errors, entry);
            }

            fields = new EnumMap<>(DBEntryField.class);
            dps.clear();
            // reset
            isData = false;
          }
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error for entry", ex);
        }
        processedLines.incrementAndGet();
      }
    }

    // finish and push last entries
    finish();

    // log errors
    logger.info(isExtensiveErrorLogging() ? errors.toString() : errors.toStringShort());

    return true;
  }

}
