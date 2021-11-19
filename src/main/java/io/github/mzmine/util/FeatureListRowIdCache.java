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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This cache reduces the number of indexOf queries in feature lists when accessing {@link
 * FeatureListRow}s via their getID(). Use this method if rows might be accessed multiple times. In
 * cases where many features are accessed use the {@link #preCacheRowIds} method to initialize a map
 * of all rows and their IDs
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FeatureListRowIdCache {

  @NotNull
  private final FeatureList featureList;
  private final Int2ObjectOpenHashMap<FeatureListRow> idRowCache = new Int2ObjectOpenHashMap<>();

  public FeatureListRowIdCache(@NotNull FeatureList featureList) {
    this.featureList = featureList;
  }

  /**
   * Retrieves rows from a feature list in a cached way
   *
   * @param rowID the row id
   * @return the row or null if no row with this ID is present in the feature list
   */
  @Nullable
  public FeatureListRow get(int rowID) {
    return idRowCache.computeIfAbsent(rowID, featureList::findRowByID);
  }

  /**
   * Initialize a map of row -> rowID for all rows in the feature list to improve the speed of
   * accessing rows by their IDs
   */
  public void preCacheRowIds() {
    for (FeatureListRow row : featureList.getRows()) {
      idRowCache.put(row.getID(), row);
    }
  }

  /**
   * @return the underlying feature list
   */
  @NotNull
  public FeatureList getFeatureList() {
    return featureList;
  }
}
