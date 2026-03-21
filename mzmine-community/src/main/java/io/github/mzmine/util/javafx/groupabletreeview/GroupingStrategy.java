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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy for grouping items in a {@link GroupableTreeView}. Determines how items are assigned to
 * groups based on their properties.
 *
 * @param <T> the type of items being grouped
 */
public sealed interface GroupingStrategy<T> permits NoGroupingStrategy, CustomGroupingStrategy,
    RawDataMetadataGroupingStrategy, FeatureListByRawDataFileStrategy,
    FeatureListByProcessingStepStrategy, FeatureListByMetadataStrategy {

  /**
   * @return display name shown in the grouping ComboBox
   */
  @NotNull String displayName();

  /**
   * @param item the item to determine the group for
   * @return the group name for the item, or null if the item should be at the top level
   */
  @Nullable String getGroupName(@NotNull T item);

  /**
   * @return true if the grouping allows manual editing (custom/no grouping), false for
   * auto-groupings
   */
  boolean isCustom();
}
