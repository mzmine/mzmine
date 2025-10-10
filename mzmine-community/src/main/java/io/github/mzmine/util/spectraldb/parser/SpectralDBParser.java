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
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class SpectralDBParser {

  private static final Logger logger = Logger.getLogger(SpectralDBParser.class.getName());

  protected final int bufferEntries;
  // process entries
  protected final LibraryEntryProcessor processor;
  protected final Object LOCK = new Object();
  private List<SpectralLibraryEntry> list;
  private int processedEntries = 0;

  public SpectralDBParser(int bufferEntries, LibraryEntryProcessor processor) {
    list = new ArrayList<>();
    this.bufferEntries = bufferEntries;
    this.processor = processor;
  }

  /**
   * Parses the file and creates spectral db entries
   *
   * @param dataBaseFile file to parse
   * @return the list or an empty list if something went wrong (e.g., wrong format)
   * @throws IOException exception while reading file
   */
  public abstract boolean parse(AbstractTask mainTask, File dataBaseFile, SpectralLibrary library)
      throws UnsupportedFormatException, IOException;

  /**
   * Add DB entry and push every 1000 entries. Does not allow 0 intensity values.
   * <p>
   * Caller should check for profile mode spectra with zero intensity signals
   *
   * @param storage
   * @param errors
   * @param entry   handle parsed library entry
   */
  protected boolean addLibraryEntry(@Nullable MemoryMapStorage storage, LibraryParsingErrors errors,
      SpectralLibraryEntry entry) {
    // zero intensity may be due to normalization and then number formatting
    // or too many (50%) zero values may be profile spectrum
    final List<DataPoint> zeroFiltered = Arrays.stream(entry.getDataPoints())
        .filter(dp -> dp.getIntensity() > 0).toList();
    if (zeroFiltered.isEmpty()) {
      errors.addUnknownException("Skipped entry with empty spectrum");
      return false;
    } else if (zeroFiltered.size() * 2 < entry.getNumberOfDataPoints()) {
      // 50% zero values so filtered out as profile
      errors.addUnknownException(
          "Skipped entry with >50% zero-intensity values (maybe profile spectrum).");
      return false;
    } else {
      // only a few zero values so maybe just bad formatting
      entry = SpectralLibraryEntryFactory.create(storage, entry.getFields(),
          zeroFiltered.toArray(new DataPoint[0]));
    }

    synchronized (LOCK) {
      // need double lock as list changes inside
      synchronized (list) {
        list.add(entry);
        if (bufferEntries > 0) {
          if (list.size() % bufferEntries == 0) {
            // start new task for every 1000 entries
            // push entries
            processor.processNextEntries(list, processedEntries);
            processedEntries += list.size();
            // new list
            list = new ArrayList<>();
          }
        }
      }
    }
    return true;
  }

  /**
   * Finish and push last entries
   */
  protected void finish() {
    // push entries
    synchronized (LOCK) {
      synchronized (list) {
        if (!list.isEmpty()) {
          logger.info("Imported last " + list.size() + " library entries");
          processor.processNextEntries(list, processedEntries);
          processedEntries += list.size();
          list = new ArrayList<>();
        }
      }
    }
    logger.info(processedEntries + "  library entries imported");
  }

  public int getProcessedEntries() {
    return processedEntries + list.size();
  }

  public abstract double getProgress();

  public String getDescription() {
    return "Importing library. Loaded entries:" + getProcessedEntries();
  }

}
