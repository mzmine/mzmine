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

package io.github.mzmine.modules.visualization.dash_lipidqc.kendrick;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data model for the Kendrick outlier review popup. Holds the list of outlier rows to display, the
 * current selection (as a list for {@link io.github.mzmine.gui.framework.fx.SelectedRowsBinding}),
 * and the active review mode used for the popup title.
 */
class KendrickOutlierPopupModel {

  private final ObservableList<FeatureListRow> outlierRows = FXCollections.observableArrayList();
  private final ObjectProperty<@Nullable List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>(
      null);
  private final ObjectProperty<@NotNull KendrickReviewMode> reviewMode = new SimpleObjectProperty<>(
      KendrickReviewMode.NONE);

  @NotNull ObservableList<FeatureListRow> getOutlierRows() {
    return outlierRows;
  }

  @Nullable List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  void setSelectedRows(final @Nullable List<FeatureListRow> rows) {
    selectedRows.set(rows);
  }

  ObjectProperty<@Nullable List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  @NotNull KendrickReviewMode getReviewMode() {
    return reviewMode.get();
  }

  void setReviewMode(final @NotNull KendrickReviewMode mode) {
    reviewMode.set(mode);
  }

  ObjectProperty<@NotNull KendrickReviewMode> reviewModeProperty() {
    return reviewMode;
  }
}
