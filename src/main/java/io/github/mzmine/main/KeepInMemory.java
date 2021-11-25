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
