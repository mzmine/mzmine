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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
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
  protected AtomicLong processedLines = new AtomicLong(0L);

  public SpectralDBTextParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile, SpectralLibrary library)
      throws IOException {
    if (totalLines == 0L) {
      try {
        logger.fine(
            () -> "Reading the number of lines for file: " + dataBaseFile.getAbsolutePath());
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
