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

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy that uses a manually maintained mapping of items to group names. Items not in the map
 * are placed at the top level.
 *
 * @param <T> the type of items being grouped
 */
public final class CustomGroupingStrategy<T> implements GroupingStrategy<T> {

  private final Map<T, String> assignments = new HashMap<>();

  @Override
  public @NotNull String displayName() {
    return "Custom grouping";
  }

  @Override
  public @Nullable String getGroupName(@NotNull final T item) {
    return assignments.get(item);
  }

  @Override
  public boolean isCustom() {
    return true;
  }

  /**
   * Assigns an item to a named group.
   */
  public void assignToGroup(@NotNull final T item, @NotNull final String groupName) {
    assignments.put(item, groupName);
  }

  /**
   * Removes an item from its group, placing it at the top level.
   */
  public void removeFromGroup(@NotNull final T item) {
    assignments.remove(item);
  }

  /**
   * @return the current assignments map (read-only view for snapshotting)
   */
  @NotNull
  public Map<T, String> getAssignments() {
    return Map.copyOf(assignments);
  }
}
