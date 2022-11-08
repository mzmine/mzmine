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
 * Parser for .jdx DB files for spectra database matching
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class JdxParser extends SpectralDBTextParser {

  public JdxParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  private static final Logger logger = Logger.getLogger(NistMspParser.class.getName());

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile, SpectralLibrary library)
      throws IOException {
    super.parse(mainTask, dataBaseFile, library);
    logger.info("Parsing jdx spectral library " + dataBaseFile.getAbsolutePath());

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
            DBEntryField field = DBEntryField.forJdxID(l.substring(0, sep));
            if (field != null) {
              String content = l.substring(sep + 1);
              if (content.length() > 0) {
                try {
                  Object value = field.convertValue(content);
                  fields.put(field, value);
                } catch (Exception e) {
                  logger.log(Level.WARNING,
                      "Cannot convert value type of " + content + " to " + field.getObjectClass()
                          .toString(), e);
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
                }
              }
            }
          }
          if (l.contains("END")) {
            // row with END
            // add entry and reset
            SpectralLibraryEntry entry = SpectralLibraryEntry.create(library.getStorage(), fields,
                dps.toArray(new DataPoint[dps.size()]));
            fields = new EnumMap<>(fields);
            dps.clear();
            addLibraryEntry(entry);
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

    return true;
  }

}
