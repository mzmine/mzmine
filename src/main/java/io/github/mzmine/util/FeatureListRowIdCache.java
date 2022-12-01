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
