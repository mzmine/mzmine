/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.javafx.groupabletreeview;

import io.github.mzmine.datamodel.features.FeatureList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups feature lists by their raw data file. Aligned feature lists (with multiple raw data files)
 * are grouped under "Aligned feature lists". Single-raw-data feature lists are grouped by the raw
 * data file name.
 */
public final class FeatureListByRawDataFileStrategy implements GroupingStrategy<FeatureList> {

  @Override
  public @NotNull String displayName() {
    return "By raw data file";
  }

  @Override
  public @Nullable String getGroupName(@NotNull final FeatureList item) {
    if (item.isAligned()) {
      return "Aligned feature lists";
    }
    return item.getRawDataFile(0).getName();
  }

  @Override
  public boolean isCustom() {
    return false;
  }
}
