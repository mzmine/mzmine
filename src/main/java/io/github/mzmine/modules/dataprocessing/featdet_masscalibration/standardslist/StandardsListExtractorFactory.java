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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist;

import com.google.common.io.Files;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static factory for StandardsListExtractor implementations picked by filename
 * with thread-safe caching of extractor objects and also their underlying extracted list data
 */
public class StandardsListExtractorFactory {

  private static final ConcurrentHashMap<String, StandardsListExtractor> cache = new ConcurrentHashMap<>();
  private static final Object lock = new Object();

  /**
   * Static factory method for returning appropriate implementation
   * based on filename extension,
   * caches the created extractors by their filename
   *
   * @param filename standards list filename
   * @param useCache when true, the created extractor cache by filename is used
   * @return cached and instantiated extractor object
   * @throws IOException              thrown by concrete extractor while opening the file
   * @throws IllegalArgumentException thrown when file with unsupported extension is given
   */
  public static StandardsListExtractor createFromFilename(String filename, boolean useCache) throws IOException {

    if (useCache == false) {
      return createExtractor(filename);
    }

    StandardsListExtractor cachedExtractor = cache.get(filename);
    if (cachedExtractor != null) {
      return cachedExtractor;
    }

    synchronized (lock) {
      if (cache.containsKey(filename)) {
        return cache.get(filename);
      }

      StandardsListExtractor extractor = createExtractor(filename);

      // use synchronization to extract the standards list
      // extractor caches the list, so subsequent method calls should just read from cached list data
      extractor.extractStandardsList();
      cache.put(filename, extractor);
      return extractor;
    }

  }

  protected static StandardsListExtractor createExtractor(String filename) throws IOException {
    String extension = Files.getFileExtension(filename);
    StandardsListExtractor extractor;

    if (extension.equals("xls") || extension.equals("xlsx")) {
      extractor = new StandardsListSpreadsheetExtractor(filename);
    } else if (extension.equals("csv")) {
      extractor = new StandardsListCsvExtractor(filename);
    } else {
      throw new IllegalArgumentException("Unsupported extension " + extension +
              " in spreadsheet file " + filename);
    }

    return extractor;
  }
}
