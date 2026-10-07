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
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.util.FeatureUtils;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * View builder for the Kendrick outlier review popup. Creates a compact panel with a title label
 * and a single-selection {@link ListView} of outlier {@link FeatureListRow}s. The ListView
 * selection is forwarded to the model's {@code selectedRowsProperty} as a single-element list.
 */
class KendrickOutlierPopupViewBuilder extends FxViewBuilder<KendrickOutlierPopupModel> {

  KendrickOutlierPopupViewBuilder(final @NotNull KendrickOutlierPopupModel model) {
    super(model);
  }

  @Override
  public @NotNull Region build() {
    final Label titleLabel = new Label();
    titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 4 6 4 6;");
    // decision: bind the title text to reviewMode so it updates automatically when mode changes
    titleLabel.textProperty().bind(model.reviewModeProperty().map(KendrickReviewMode::toString));

    final ListView<FeatureListRow> listView = new ListView<>(model.getOutlierRows());
    listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    listView.setCellFactory(_ -> new ListCell<>() {
      @Override
      protected void updateItem(final @Nullable FeatureListRow row, final boolean empty) {
        super.updateItem(row, empty);
        setText(empty || row == null ? null : FeatureUtils.rowToString(row));
      }
    });

    listView.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
      // decision: wrap single selection in a list for SelectedRowsBinding; clear on deselect
      model.setSelectedRows(selected != null ? List.of(selected) : null);
    });

    final BorderPane pane = new BorderPane(listView);
    pane.setTop(titleLabel);
    pane.setPrefWidth(280);
    pane.setMaxHeight(300);
    return pane;
  }
}
