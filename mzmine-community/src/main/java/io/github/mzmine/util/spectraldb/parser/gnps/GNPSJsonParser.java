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

package io.github.mzmine.util.spectraldb.parser.gnps;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.LibraryEntryProcessor;
import io.github.mzmine.util.spectraldb.parser.SpectralDBParser;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

// top level json objects/arrays

/**
 * GNPS json from downloads https://gnps-external.ucsd.edu/gnpslibrary
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class GNPSJsonParser extends SpectralDBParser {

  private static final Logger logger = Logger.getLogger(GNPSJsonParser.class.getName());
  private boolean finished = false;

  public GNPSJsonParser(int bufferEntries, LibraryEntryProcessor processor) {
    super(bufferEntries, processor);
  }

  @Override
  public boolean parse(@Nullable AbstractTask mainTask, File dataBaseFile,
      @Nullable SpectralLibrary library) throws IOException {
    logger.info("Parsing GNPS spectral json library " + dataBaseFile.getAbsolutePath());
    int error = 0;
    ObjectMapper mapper = new ObjectMapper();
    // Create a JsonParser instance
    try (JsonParser jsonParser = mapper.getFactory().createParser(dataBaseFile)) {

      // Check the first token
      if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalStateException("Expected content to be an array");
      }

      // Iterate over the tokens until the end of the array
      while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
        try {
          SpectralLibraryEntry entry = mapper.readValue(jsonParser, GnpsLibraryEntry.class)
              .toSpectralLibraryEntry(library);
          addLibraryEntry(entry);
        } catch (Exception ex) {
          logger.log(Level.WARNING, ex.getMessage(), ex);
          error++;
        }
      }
    }
    finish();

    logger.info(String.format("GNPS library loaded with %d entries and %d failing entries",
        getProcessedEntries(), error));

    finished = true;
    return true;
  }

  @Override
  public double getProgress() {
    return finished ? 1 : (getProcessedEntries() % 10000) / 10000.0;
  }
}
