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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.parser;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
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
  public boolean parse(AbstractTask mainTask, File dataBaseFile) throws IOException {
    FileTypeFilter json = new FileTypeFilter("json", "");
    FileTypeFilter msp = new FileTypeFilter("msp", "");
    if (json.accept(dataBaseFile)) {
      // test Gnps and MONA json parser
      SpectralDBParser[] parser =
          new SpectralDBParser[] {new GnpsJsonParser(bufferEntries, processor),
              new MonaJsonParser(bufferEntries, processor)};
      for (SpectralDBParser p : parser) {
        try {
          boolean state = p.parse(mainTask, dataBaseFile);
          if (state)
            return state;
          else
            continue;
        } catch (Exception ex) {
        }
      }
    } else if (msp.accept(dataBaseFile)) {
      // load NIST msp format
      NistMspParser parser = new NistMspParser(bufferEntries, processor);
      try {
        boolean state = parser.parse(mainTask, dataBaseFile);
        if (state)
          return state;
      } catch (Exception ex) {
      }
    }
    logger.log(Level.WARNING, "Unsupported file format: " + dataBaseFile.getAbsolutePath());
    return false;
  }

}
