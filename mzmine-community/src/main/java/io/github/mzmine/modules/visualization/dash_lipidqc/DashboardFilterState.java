/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import java.awt.Color;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable state object that tracks which rows and groups are currently selected in the lipid QC
 * dashboard summary bar chart, propagating selection changes to all dashboard panes via a shared
 * onChange callback.
 */
public final class DashboardFilterState {

  private @NotNull Set<Integer> barSelectedRowIds = Set.of();
  private @NotNull Set<String> barSelectedGroups = Set.of();
  private @NotNull Map<Integer, Color> barSelectedRowColors = Map.of();
  private @Nullable Runnable onChange;

  public @NotNull Set<Integer> getBarSelectedRowIds() {
    return barSelectedRowIds;
  }

  public void setBarSelectedRowIds(final @NotNull Set<Integer> barSelectedRowIds) {
    this.barSelectedRowIds = Objects.requireNonNullElse(barSelectedRowIds, Set.of());
  }

  public @NotNull Set<String> getBarSelectedGroups() {
    return barSelectedGroups;
  }

  public void setBarSelectedGroups(final @NotNull Set<String> barSelectedGroups) {
    this.barSelectedGroups = Objects.requireNonNullElse(barSelectedGroups, Set.of());
  }

  public @NotNull Map<Integer, Color> getBarSelectedRowColors() {
    return barSelectedRowColors;
  }

  public void setBarSelectedRowColors(final @NotNull Map<Integer, Color> barSelectedRowColors) {
    this.barSelectedRowColors = Objects.requireNonNullElse(barSelectedRowColors, Map.of());
  }

  public @Nullable Runnable getOnChange() {
    return onChange;
  }

  public void setOnChange(final @Nullable Runnable onChange) {
    this.onChange = onChange;
  }
}
