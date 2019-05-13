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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

public abstract class SpectralDBParser {
  private static Logger logger = Logger.getLogger(SpectralDBParser.class.getName());

  protected int bufferEntries = 1000;
  private List<SpectralDBEntry> list;
  private int processedEntries = 0;
  // process entries
  protected LibraryEntryProcessor processor;

  public SpectralDBParser(int bufferEntries, LibraryEntryProcessor processor) {
    list = new ArrayList<>();
    this.bufferEntries = bufferEntries;
    this.processor = processor;
  }

  /**
   * Parses the file and creates spectral db entries
   * 
   * @param task
   * 
   * @param dataBaseFile
   * @return the list or an empty list if something went wrong (e.g., wrong format)
   * @throws IOException
   */
  public abstract boolean parse(AbstractTask mainTask, File dataBaseFile) throws IOException;

  /**
   * Add DB entry and push every 1000 entries
   * 
   * @param entry
   */
  protected void addLibraryEntry(SpectralDBEntry entry) {
    list.add(entry);
    if (list.size() % bufferEntries == 0) {
      // start new task for every 1000 entries
      logger.info("Imported " + list.size() + " library entries");
      // push entries
      processor.processNextEntries(list, processedEntries);
      processedEntries += list.size();
      // new list
      list = new ArrayList<>();
    }
  }

  /**
   * Finish and push last entries
   */
  protected void finish() {
    // push entries
    if (!list.isEmpty()) {
      logger.info("Imported last " + list.size() + " library entries");
      processor.processNextEntries(list, processedEntries);
      processedEntries += list.size();
      list = null;
    }

    logger.info(processedEntries + "  library entries imported");
  }

}
