/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.util.spectraldb.parser;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public abstract class SpectralDBParser {

  private static final Logger logger = Logger.getLogger(SpectralDBParser.class.getName());

  protected final int bufferEntries;
  // process entries
  protected final LibraryEntryProcessor processor;
  protected final Object LOCK = new Object();
  private List<SpectralDBEntry> list;
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
  public abstract boolean parse(AbstractTask mainTask, File dataBaseFile)
      throws UnsupportedFormatException, IOException;

  /**
   * Add DB entry and push every 1000 entries. Does not allow 0 intensity values.
   *
   * @param entry handle parsed library entry
   */
  protected boolean addLibraryEntry(SpectralDBEntry entry) {
    // no 0 values allowed in entry
    if (Arrays.stream(entry.getDataPoints()).mapToDouble(DataPoint::getIntensity)
        .anyMatch(v -> Double.compare(v, 0) == 0)) {
      return false;
    }
    synchronized (LOCK) {
      // need double lock as list changes inside
      synchronized (list) {
        list.add(entry);
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

}
