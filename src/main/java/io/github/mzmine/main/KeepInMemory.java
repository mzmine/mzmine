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

package io.github.mzmine.main;

import io.github.mzmine.util.MemoryMapStorage;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum KeepInMemory {

  NONE, ALL, FEATURES, MASS_LISTS, RAW_SCANS, MASSES_AND_FEATURES;

  public static KeepInMemory parse(String s) {
    s = s.toLowerCase();
    return switch (s) {
      case "all" -> ALL;
      case "none" -> NONE;
      case "features" -> FEATURES;
      case "centroids" -> MASS_LISTS;
      case "raw" -> RAW_SCANS;
      case "masses_features" -> MASSES_AND_FEATURES;
      default -> throw new IllegalStateException("Unexpected value: " + s);
    };
  }

  /**
   * Apply this option for memory mapping
   */
  public void enforceToMemoryMapping() {
    // reset
    MemoryMapStorage.setStoreAllInRam(false);
    // keep all in memory? (features, scans, ... in RAM instead of MemoryMapStorage
    switch (this) {
      case NONE -> {
        // nothing in RAM
      }
      case ALL -> MemoryMapStorage.setStoreAllInRam(true);
      case FEATURES -> MemoryMapStorage.setStoreFeaturesInRam(true);
      case MASS_LISTS -> MemoryMapStorage.setStoreMassListsInRam(true);
      case RAW_SCANS -> MemoryMapStorage.setStoreRawFilesInRam(true);
      case MASSES_AND_FEATURES -> {
        MemoryMapStorage.setStoreMassListsInRam(true);
        MemoryMapStorage.setStoreFeaturesInRam(true);
      }
    }
  }
}
