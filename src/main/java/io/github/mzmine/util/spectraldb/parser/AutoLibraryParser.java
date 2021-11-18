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
      SpectralDBParser[] parser =
          new SpectralDBParser[]{new MonaJsonParser(bufferEntries, processor),
              new GnpsJsonParser(bufferEntries, processor)};
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
