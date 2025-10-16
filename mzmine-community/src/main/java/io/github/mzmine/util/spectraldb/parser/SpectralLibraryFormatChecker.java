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

import io.github.mzmine.util.files.FileTypeFilter;
import io.github.mzmine.util.spectraldb.parser.gnps.GNPSJsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Get the right parser for the format. Checks for specific json formats.
 */
public class SpectralLibraryFormatChecker {

  public static SpectralDBParser getParser(File dataBaseFile, int bufferEntries,
      final LibraryEntryProcessor processor, boolean extensiveErrorLogging)
      throws UnsupportedFormatException, IOException {

    FileTypeFilter json = new FileTypeFilter("json", "");
    FileTypeFilter msp = new FileTypeFilter("msp", "");
    FileTypeFilter mspRIKEN = new FileTypeFilter("msp_RIKEN", "");
    FileTypeFilter mspNIST = new FileTypeFilter("msp_NIST", "");
    FileTypeFilter mgf = new FileTypeFilter("mgf", "");
    FileTypeFilter jdx = new FileTypeFilter("jdx", "");

    if (json.accept(dataBaseFile)) {
      return getJsonParser(dataBaseFile, bufferEntries, processor, extensiveErrorLogging);
    }
    // msp, jdx or mgf
    if (msp.accept(dataBaseFile) || mspRIKEN.accept(dataBaseFile) || mspNIST.accept(dataBaseFile)) {
      // load NIST msp format
      return new NistMspParser(bufferEntries, processor, extensiveErrorLogging);
    } else if (jdx.accept(dataBaseFile)) {
      // load jdx format
      return new JdxParser(bufferEntries, processor, extensiveErrorLogging);
    } else if (mgf.accept(dataBaseFile)) {
      return new GnpsMgfParser(bufferEntries, processor, extensiveErrorLogging);
    } else {
      throw new UnsupportedFormatException(
          "Format not supported: " + dataBaseFile.getAbsolutePath());
    }
  }

  private static SpectralDBParser getJsonParser(final File dataBaseFile, final int bufferEntries,
      final LibraryEntryProcessor processor, boolean extensiveErrorLogging) throws IOException {
    try (FileReader reader = new FileReader(
        dataBaseFile); BufferedReader bufferedReader = new BufferedReader((reader))) {
      char[] chars = new char[4048];

      final String content;
      try {
        int read = bufferedReader.read(chars);
        content = new String(chars, 0, read);
        if (content.contains("peaks_json") || content.contains("library_membership")) {
          return new GNPSJsonParser(bufferEntries, processor, extensiveErrorLogging);
        } else if (content.contains("\"compound\"") && content.contains("\"computed\"")
            && content.contains("\"tags\"")) {
          return new MonaJsonParser(bufferEntries, processor, extensiveErrorLogging);
        } else {
          return new MZmineJsonParser(bufferEntries, processor, extensiveErrorLogging);
        }
      } catch (Exception e) {
        // this may be triggered when the file is empty or very small
        // try mzmine parser as this might be a small library
        return new MZmineJsonParser(bufferEntries, processor, extensiveErrorLogging);
      }
    }
  }
}
