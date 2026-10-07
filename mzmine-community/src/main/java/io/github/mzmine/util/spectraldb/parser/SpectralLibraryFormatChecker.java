/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.util.spectraldb.parser.gnps.GnpsJsonFlavor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Get the right parser for the format. Checks for specific json formats.
 */
public class SpectralLibraryFormatChecker {

  private static final Logger logger = Logger.getLogger(
      SpectralLibraryFormatChecker.class.getName());

  /**
   * How much of a json library is scanned for the markers that identify its flavour. A GNPS entry
   * with long structure fields pushes peaks_json well past the first few kB, so this is generous.
   */
  private static final int DETECTION_CHARS = 1 << 20;

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
      final LibraryEntryProcessor processor, boolean extensiveErrorLogging) {
    final String content;
    try {
      content = readHead(dataBaseFile);
    } catch (Exception e) {
      // this may be triggered when the file is empty or unreadable
      // try mzmine parser as this might be a small library
      logger.log(Level.WARNING,
          "Could not read " + dataBaseFile.getAbsolutePath() + " to detect the json format", e);
      return new MZmineJsonParser(bufferEntries, processor, extensiveErrorLogging);
    }

    if (content.contains("peaks_json") || content.contains("library_membership")) {
      // classic GNPS export and the cleaned libraries, flat entries with a peaks_json string
      return new GNPSJsonParser(bufferEntries, processor, extensiveErrorLogging);
    } else if (content.contains("\"compound\"") && content.contains("\"computed\"")
        && content.contains("\"tags\"")) {
      return new MonaJsonParser(bufferEntries, processor, extensiveErrorLogging);
    } else if (content.contains("\"metadata\"") && content.contains("\"peaks\"")) {
      // GNPS2, a metadata object per entry next to the peaks array. The mzmine format also has
      // peaks but never a metadata object, so both keys have to be there
      return new GNPSJsonParser(bufferEntries, processor, extensiveErrorLogging,
          GnpsJsonFlavor.GNPS2);
    } else {
      return new MZmineJsonParser(bufferEntries, processor, extensiveErrorLogging);
    }
  }

  /**
   * Reads the beginning of the file for format detection. The markers sit inside the first entry,
   * which can be large when it carries structure or spectrum fields, so this reads much more than
   * one entry is ever expected to need.
   * <p>
   * Reader.read fills only what is currently buffered and returns short reads, so it is called in a
   * loop rather than once.
   *
   * @return up to {@link #DETECTION_CHARS} characters from the start of the file
   */
  private static String readHead(@NotNull final File dataBaseFile) throws IOException {
    final char[] chars = new char[DETECTION_CHARS];
    int total = 0;
    try (BufferedReader reader = Files.newBufferedReader(dataBaseFile.toPath(),
        StandardCharsets.UTF_8)) {
      while (total < chars.length) {
        final int read = reader.read(chars, total, chars.length - total);
        if (read < 0) {
          break;
        }
        total += read;
      }
    }
    return new String(chars, 0, total);
  }
}
