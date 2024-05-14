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
import org.jetbrains.annotations.Nullable;

public class NistMspParser extends SpectralDBTextParser {

  private static final Logger logger = Logger.getLogger(NistMspParser.class.getName());

  public NistMspParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }


  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile, SpectralLibrary library)
      throws IOException {
    super.parse(mainTask, dataBaseFile, library);
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
      for (String l; (l = br.readLine()) != null; ) {
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
              } else {
                isData = false;
              }
            }
          } else {
            // empty row
            if (isData) {
              // empty row after data
              // add entry and reset
              SpectralLibraryEntry entry = SpectralLibraryEntry.create(library.getStorage(), fields,
                  dps.toArray(new DataPoint[dps.size()]));
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
        processedLines.incrementAndGet();
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

    data = dataAndComment[0].split("\t");
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
   * @param line   String with metadata
   * @param sep    index of the separation char ':'
   */
  private void extractMetaData(Map<DBEntryField, Object> fields, String line, int sep) {
    String key = line.substring(0, sep);
    DBEntryField field = DBEntryField.forMspID(key);
    if (field != null) {
      // spe +2 for colon and space
      String content = line.substring(sep + 2);
      if (content.length() > 0) {
        try {
          // convert into value type
          Object value = field.convertValue(content);
          fields.put(field, value);
        } catch (Exception e) {
          logger.log(Level.WARNING,
              "Cannot convert value type of " + content + " to " + field.getObjectClass()
                  .toString(), e);
        }
      }
    }
  }

}
