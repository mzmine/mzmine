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

package io.github.mzmine.modules.visualization.network_overview;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the immutable parameters used to initialise a network overview. State is read-only because
 * setup happens exactly once.
 */
public class NetworkOverviewModel {

  private final @NotNull ModularFeatureList featureList;
  private final @Nullable FeatureTableFX externalTable;
  private final @Nullable List<? extends FeatureListRow> focussedRows;
  private final @NotNull NetworkOverviewFlavor flavor;

  public NetworkOverviewModel(@NotNull ModularFeatureList featureList,
      @Nullable FeatureTableFX externalTable, @Nullable List<? extends FeatureListRow> focussedRows,
      @NotNull NetworkOverviewFlavor flavor) {
    this.featureList = featureList;
    this.externalTable = externalTable;
    this.focussedRows = focussedRows;
    this.flavor = flavor;
  }

  public @NotNull ModularFeatureList getFeatureList() {
    return featureList;
  }

  public @Nullable FeatureTableFX getExternalTable() {
    return externalTable;
  }

  public @Nullable List<? extends FeatureListRow> getFocussedRows() {
    return focussedRows;
  }

  public @NotNull NetworkOverviewFlavor getFlavor() {
    return flavor;
  }
}
