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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.standardslist;

import com.google.common.io.Files;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static factory for StandardsListExtractor implementations picked by filename with caching
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
   * @return cached and instantiated extractor object
   * @throws IOException thrown by concrete extractor while opening the file
   */
  public static StandardsListExtractor createFromFilename(String filename) throws IOException {

    /*if(cache.containsKey(filename)){
      return cache.get(filename);
    }*/

    StandardsListExtractor cachedExtractor = cache.get(filename);
    if(cachedExtractor != null){
      return cachedExtractor;
    }

    synchronized (lock) {
      if(cache.containsKey(filename)){
        return cache.get(filename);
      }

      String extension = Files.getFileExtension(filename);

      if (extension.equals("xls") || extension.equals("xlsx")) {
        StandardsListExtractor extractor = new StandardsListSpreadsheetExtractor(filename);
        extractor.extractStandardsList();
        cache.put(filename, extractor);
        return extractor;
      }

      throw new IllegalArgumentException("Unsupported extension " + extension +
              " in spreadsheet file " + filename);
    }

  }
}
