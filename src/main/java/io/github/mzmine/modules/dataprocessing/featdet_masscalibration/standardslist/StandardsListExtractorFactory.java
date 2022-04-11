/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
