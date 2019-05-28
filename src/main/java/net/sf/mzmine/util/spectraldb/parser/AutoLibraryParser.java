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
import java.util.logging.Logger;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.util.files.FileTypeFilter;

/**
 * Auto detects library format
 * 
 * @author Robin Schmid
 *
 */
public class AutoLibraryParser extends SpectralDBParser {

  public AutoLibraryParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  private Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public boolean parse(AbstractTask mainTask, File dataBaseFile)
      throws UnsupportedFormatException, IOException {
    FileTypeFilter json = new FileTypeFilter("json", "");
    FileTypeFilter msp = new FileTypeFilter("msp", "");
    FileTypeFilter mgf = new FileTypeFilter("mgf", "");
    FileTypeFilter jdx = new FileTypeFilter("jdx", "");
    if (json.accept(dataBaseFile)) {
      // test Gnps and MONA json parser
      SpectralDBParser[] parser =
          new SpectralDBParser[] {new MonaJsonParser(bufferEntries, processor),
              new GnpsJsonParser(bufferEntries, processor)};
      for (SpectralDBParser p : parser) {
        if (mainTask.isCanceled())
          return false;
        try {
          boolean state = p.parse(mainTask, dataBaseFile);
          if (state)
            return state;
          else
            continue;
        } catch (Exception ex) {
          // do nothing and try next json format
        }
      }
    } else {
      final SpectralDBParser parser;
      // msp, jdx or mgf
      if (msp.accept(dataBaseFile)) {
        // load NIST msp format
        parser = new NistMspParser(bufferEntries, processor);
      } else if (jdx.accept(dataBaseFile)) {
        // load jdx format
        parser = new JdxParser(bufferEntries, processor);
      } else if (mgf.accept(dataBaseFile)) {
        parser = new GnpsMgfParser(bufferEntries, processor);
      } else {
        throw (new UnsupportedFormatException(
            "Format not supported: " + dataBaseFile.getAbsolutePath()));
      }

      // parse the file
      boolean state = parser.parse(mainTask, dataBaseFile);
      if (state)
        return state;
    }
    if (mainTask.isCanceled())
      return false;
    else
      throw (new UnsupportedFormatException(
          "Format not supported: " + dataBaseFile.getAbsolutePath()));
  }

}
