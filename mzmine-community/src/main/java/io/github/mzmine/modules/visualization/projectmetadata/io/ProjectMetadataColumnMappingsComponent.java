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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingComponent;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataColumnMappingsComponent extends VBox {

  private static final String TARGET_TOOLTIP = """
      Export column name that receives values from the selected metadata column.""";
  private static final String DEFAULT_VALUE_TOOLTIP = """
      Value written when the selected source metadata column has no value for a raw data file.""";

  private final List<MappingRow> rows = new ArrayList<>();
  private final VBox rowsPane = FxLayout.newVBox(Insets.EMPTY);

  public ProjectMetadataColumnMappingsComponent() {
    super(FxLayout.DEFAULT_SPACE);
    setPadding(Insets.EMPTY);

    final ButtonBase addButton = FxIconUtil.newIconButton(FxIcons.PLUS_CIRCLE,
        "Add metadata column mapping", this::addEmptyRow);
    getChildren().addAll(rowsPane, addButton);
    addEmptyRow();
  }

  public @NotNull List<ProjectMetadataColumnMapping> getValue() {
    return rows.stream().map(MappingRow::getValue).filter(mapping -> !mapping.isEmpty()).toList();
  }

  public void setValue(@Nullable final List<ProjectMetadataColumnMapping> mappings) {
    rows.clear();
    rowsPane.getChildren().clear();

    if (mappings != null) {
      mappings.forEach(this::addRow);
    }
    if (rows.isEmpty()) {
      addEmptyRow();
    }
  }

  public boolean hasActiveMappings() {
    return getValue().stream().anyMatch(ProjectMetadataColumnMapping::isActive);
  }

  private void addEmptyRow() {
    addRow(new ProjectMetadataColumnMapping("", "", ""));
  }

  private void addRow(@NotNull final ProjectMetadataColumnMapping mapping) {
    final MappingRow row = new MappingRow(mapping);
    rows.add(row);
    rowsPane.getChildren().add(row.pane());
  }

  private void removeRow(@NotNull final MappingRow row) {
    final ProjectMetadataColumnMapping mapping = row.getValue();
    final String title = mapping.targetColumn().isBlank() ? "Remove metadata mapping?"
        : "Remove mapping to " + mapping.targetColumn() + "?";
    if (!DialogLoggerUtil.showDialogYesNo(title, "Remove this export metadata mapping row?")) {
      return;
    }

    rows.remove(row);
    rowsPane.getChildren().remove(row.pane());
    if (rows.isEmpty()) {
      addEmptyRow();
    }
  }

  private final class MappingRow {

    private final MetadataGroupingComponent sourceColumn = new MetadataGroupingComponent();
    private final TextField targetColumn = FxTextFields.newAutoGrowTextField(null, "column name",
        TARGET_TOOLTIP, 8, 30);
    private final TextField defaultValue = FxTextFields.newAutoGrowTextField(null, "default",
        DEFAULT_VALUE_TOOLTIP, 6, 30);
    private final HBox pane;

    private MappingRow(@NotNull final ProjectMetadataColumnMapping mapping) {
      sourceColumn.setValue(mapping.sourceColumn());
      targetColumn.setText(mapping.targetColumn());
      defaultValue.setText(mapping.defaultValue());

      final ButtonBase removeButton = FxIconUtil.newIconButton(FxIcons.X_CIRCLE,
          "Remove metadata column mapping", () -> removeRow(this));
      pane = FxLayout.newHBox(Insets.EMPTY, sourceColumn, new Label("map to"), targetColumn,
          defaultValue, removeButton);
    }

    private @NotNull ProjectMetadataColumnMapping getValue() {
      return new ProjectMetadataColumnMapping(sourceColumn.getValue(), targetColumn.getText(),
          defaultValue.getText());
    }

    private @NotNull HBox pane() {
      return pane;
    }
  }
}
