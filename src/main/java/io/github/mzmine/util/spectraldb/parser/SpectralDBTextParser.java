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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class SpectralDBTextParser extends SpectralDBParser {

  private static final Logger logger = Logger.getLogger(SpectralDBTextParser.class.getName());

  protected long totalLines = 0L;
  protected AtomicLong processedLines = new AtomicLong(0l);

  public SpectralDBTextParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile)
      throws IOException {
    if (totalLines == 0L) {
      try {
        logger
            .fine(() -> "Reading the number of lines for file: " + dataBaseFile.getAbsolutePath());
        totalLines = FileAndPathUtil.countLines(dataBaseFile);
      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Could not count lines in data base file: " + dataBaseFile.getAbsolutePath(), ex);
        totalLines = 0L;
      }
    }
    return false;
  }

  @Override
  public double getProgress() {
    return totalLines == 0 ? 0 : processedLines.get() / (double) totalLines;
  }

  public void setTotalLines(long totalLines) {
    this.totalLines = totalLines;
  }
}
