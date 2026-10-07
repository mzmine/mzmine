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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.factories.FxListViews;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.parameters.Parameter;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListSummaryViewBuilder extends FxViewBuilder<FeatureListSummaryModel> {

  private final Runnable onSetPreferences;
  private final Runnable onOpenInBatchQueue;
  private final Runnable onExport;

  protected FeatureListSummaryViewBuilder(@NotNull FeatureListSummaryModel model,
      @NotNull Runnable onSetPreferences, @NotNull Runnable onOpenInBatchQueue,
      @NotNull Runnable onExport) {
    super(model);
    this.onSetPreferences = onSetPreferences;
    this.onOpenInBatchQueue = onOpenInBatchQueue;
    this.onExport = onExport;
  }

  private static @NotNull TextField newReadOnlyField(
      final @NotNull javafx.beans.property.StringProperty textProperty) {
    final TextField field = FxTextFields.newTextField(textProperty, null);
    field.setEditable(false);
    return field;
  }

  private static @NotNull Label newTitleLabel(@NotNull String text) {
    return FxLabels.styled(text, "title-label");
  }

  private static @NotNull Label newTitleLabel(
      @NotNull javafx.beans.value.ObservableValue<String> textBinding) {
    return FxLabels.bindText(FxLabels.styled("", "title-label"), textBinding);
  }

  /**
   * Builds the multi line parameter description shown for the selected applied method.
   */
  private static @NotNull String methodToText(@Nullable FeatureListAppliedMethod method) {
    if (method == null) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(method).append("\n");
    sb.append(method.getDescription()).append("\n");
    for (final Parameter<?> parameter : method.getParameters().getParameters()) {
      sb.append(FeatureListSummaryController.parameterToString(parameter, null)).append("\n");
    }
    return sb.toString();
  }

  @Override
  public Region build() {
    final Label title = FxLabels.newLabel(Styles.BOLD_TITLE, model.titleProperty());

    // set preferences moved next to the title, only shown while a feature list is displayed
    final Button btnSetPreferences = FxButtons.createButton("Set list preferences", null,
        onSetPreferences);
    btnSetPreferences.visibleProperty().bind(model.featureListProperty().isNotNull());
    FxLayout.bindManagedToVisible(btnSetPreferences);

    // title stays centered, the button is placed at the right edge
    final BorderPane titleBox = new BorderPane();
    title.setMaxWidth(Double.MAX_VALUE);
    title.setAlignment(Pos.CENTER);
    titleBox.setCenter(title);
    titleBox.setRight(btnSetPreferences);
    BorderPane.setAlignment(btnSetPreferences, Pos.CENTER);

    final TextField tfCreated = newReadOnlyField(model.createdProperty());
    final TextField tfNumRows = newReadOnlyField(model.numRowsProperty());
    final TextField tfNumAnnotated = newReadOnlyField(model.numAnnotatedProperty());

    final ListView<FeatureListAppliedMethod> lvAppliedMethods = FxListViews.newListView(
        model.getAppliedMethods(), false, SelectionMode.SINGLE);

    final TextArea tvParameterValues = new TextArea();
    tvParameterValues.setEditable(false);
    // reflect the selected applied method as formatted parameter text
    tvParameterValues.textProperty().bind(
        lvAppliedMethods.getSelectionModel().selectedItemProperty()
            .map(FeatureListSummaryViewBuilder::methodToText).orElse(""));

    final Button btnOpenInBatchQueue = FxButtons.createButton("Open in batch queue", null,
        onOpenInBatchQueue);
    final Button exportButton = FxButtons.createButton("Export", null, onExport);
    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().addAll(btnOpenInBatchQueue, exportButton);

    final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    grid.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    // keep the original default window size
    grid.setPrefSize(600.0, 400.0);

    final ColumnConstraints col0 = new ColumnConstraints();
    col0.setHgrow(Priority.NEVER);
    col0.setMinWidth(10.0);
    final ColumnConstraints col1 = new ColumnConstraints();
    col1.setHgrow(Priority.ALWAYS);
    col1.setMinWidth(10.0);
    grid.getColumnConstraints().addAll(col0, col1);

    // the last row holds the growing applied-methods / parameter panes
    final RowConstraints growRow = FxLayout.newFillHeightRow();

    int row = 0;
    grid.add(titleBox, 0, row, 2, 1);
    GridPane.setHgrow(titleBox, Priority.ALWAYS);
    row++;

    grid.add(newTitleLabel(model.createdLabelProperty()), 0, row);
    grid.add(tfCreated, 1, row);
    row++;

    grid.add(newTitleLabel(model.numRowsLabelProperty()), 0, row);
    grid.add(tfNumRows, 1, row);
    row++;

    grid.add(newTitleLabel(model.numAnnotatedLabelProperty()), 0, row);
    grid.add(tfNumAnnotated, 1, row);
    row++;

    grid.add(newTitleLabel("Applied methods log:"), 0, row);
    grid.add(buttonBar, 1, row);
    row++;

    grid.getRowConstraints()
        .setAll(new RowConstraints(), new RowConstraints(), new RowConstraints(),
            new RowConstraints(), new RowConstraints(), growRow);
    grid.add(lvAppliedMethods, 0, row);
    grid.add(tvParameterValues, 1, row);

    return grid;
  }
}
