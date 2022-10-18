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
import io.github.mzmine.util.files.FileTypeFilter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auto detects library format
 *
 * @author Robin Schmid
 */
public class AutoLibraryParser extends SpectralDBParser {

  private static final Logger logger = Logger.getLogger(AutoLibraryParser.class.getName());
  private SpectralDBParser subParser;

  public AutoLibraryParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile)
      throws UnsupportedFormatException, IOException {
    FileTypeFilter json = new FileTypeFilter("json", "");
    FileTypeFilter msp = new FileTypeFilter("msp", "");
    FileTypeFilter mgf = new FileTypeFilter("mgf", "");
    FileTypeFilter jdx = new FileTypeFilter("jdx", "");
    long totalLines;
    try {
      totalLines = FileAndPathUtil.countLines(dataBaseFile);
    } catch (Exception ex) {
      logger.log(Level.WARNING,
          "Could not count lines in data base file: " + dataBaseFile.getAbsolutePath(), ex);
      totalLines = 0L;
    }

    if (json.accept(dataBaseFile)) {
      // test Gnps and MONA json parser
      SpectralDBParser[] parser = new SpectralDBParser[]{
          new MonaJsonParser(bufferEntries, processor),
          new MZmineJsonParser(bufferEntries, processor)};
      for (SpectralDBParser p : parser) {
        if (mainTask.isCanceled()) {
          return false;
        }
        try {
          subParser = p;
          if (subParser instanceof SpectralDBTextParser txtParser) {
            txtParser.setTotalLines(totalLines);
          }
          boolean state = p.parse(mainTask, dataBaseFile);
          if (state) {
            return state;
          } else {
            continue;
          }
        } catch (Exception ex) {
          // do nothing and try next json format
        }
      }
    } else {
      // msp, jdx or mgf
      if (msp.accept(dataBaseFile)) {
        // load NIST msp format
        subParser = new NistMspParser(bufferEntries, processor);
      } else if (jdx.accept(dataBaseFile)) {
        // load jdx format
        subParser = new JdxParser(bufferEntries, processor);
      } else if (mgf.accept(dataBaseFile)) {
        subParser = new GnpsMgfParser(bufferEntries, processor);
      } else {
        throw (new UnsupportedFormatException(
            "Format not supported: " + dataBaseFile.getAbsolutePath()));
      }

      if (subParser instanceof SpectralDBTextParser txtParser) {
        txtParser.setTotalLines(totalLines);
      }
      // parse the file
      boolean state = subParser.parse(mainTask, dataBaseFile);
      if (state) {
        return state;
      }
    }
    if (mainTask.isCanceled()) {
      return false;
    } else {
      throw (new UnsupportedFormatException(
          "Format not supported: " + dataBaseFile.getAbsolutePath()));
    }
  }

  @Override
  public double getProgress() {
    return subParser == null ? 0 : subParser.getProgress();
  }
}
