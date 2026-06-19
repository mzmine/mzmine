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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single, shared editor for the value handling of the currently selected
 * {@link MetadataRegexMappingRow}. A mode combo box selects whether the extracted value is stored
 * directly ({@link ValueHandlingMode#EXTRACT_VALUES}, default) or mapped to other values
 * ({@link ValueHandlingMode#VALUE_MAPPINGS}). In the latter mode an auto-growing list of
 * case-insensitive value mappings is shown. Edits are written back into the selected row's data
 * live. When no row is selected the editor is disabled.
 */
public class MetadataValueMappingEditor extends VBox {

  private final List<RawDataFile> rawFiles;
  private final Runnable onChange;

  private final ComboBox<ValueHandlingMode> modeCombo;
  private final Label forColumnLabel = FxLabels.newBoldLabel("");
  private final CheckBox dropUnmappedCheck = new CheckBox("Drop values not listed below");
  private final VBox fieldsBox = FxLayout.newVBox(Insets.EMPTY);
  // holds the value-mapping controls; only shown in the VALUE_MAPPINGS mode
  private final VBox valueSection;

  private @Nullable MetadataRegexMappingRow target;
  // guards against writing back into the target while the controls are being (re)populated
  private boolean loading;

  public MetadataValueMappingEditor(@NotNull final List<RawDataFile> rawFiles,
      @NotNull final Runnable onChange) {
    super(FxLayout.DEFAULT_SPACE);
    setPadding(new Insets(FxLayout.DEFAULT_SPACE));
    this.rawFiles = rawFiles;
    this.onChange = onChange;

    modeCombo = FxComboBox.createComboBox(
        "Extract values: store the extracted value directly. Value mappings: map extracted values "
            + "to other values (e.g. media → blank).", List.of(ValueHandlingMode.values()));
    modeCombo.setValue(ValueHandlingMode.EXTRACT_VALUES);

    dropUnmappedCheck.setTooltip(new Tooltip(
        "If checked, extracted values that are not in the mapping list below are left empty."));
    dropUnmappedCheck.selectedProperty().addListener((_, _, selected) -> {
      if (!loading && target != null) {
        target.setDropUnmapped(selected);
        onChange.run();
      }
    });

    final Button fillButton = FxButtons.createButton("Fill values from files",
        "Add all values matched in the loaded raw data files to the list below.",
        this::autoFillFromFiles);

    final HBox options = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, fillButton,
        dropUnmappedCheck);
    valueSection = FxLayout.newVBox(Insets.EMPTY, FxLabels.newItalicLabel(
        "Map extracted values (left) to stored values (right), e.g. media → blank "
            + "(case-insensitive)"), options, fieldsBox);
    valueSection.managedProperty().bind(valueSection.visibleProperty());

    final HBox headerRow = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, modeCombo,
        forColumnLabel);
    getChildren().addAll(headerRow, valueSection);

    modeCombo.valueProperty().addListener((_, _, mode) -> {
      final boolean useMaps = mode == ValueHandlingMode.VALUE_MAPPINGS;
      valueSection.setVisible(useMaps);
      if (!loading && target != null) {
        target.setUseValueMappings(useMaps);
        onChange.run();
      }
    });

    setTarget(null);
  }

  /**
   * Binds the editor to a row (or {@code null}) and loads that row's value handling into the
   * controls.
   *
   * @param row the selected row, or null to disable the editor
   */
  public void setTarget(@Nullable final MetadataRegexMappingRow row) {
    loading = true;
    this.target = row;
    fieldsBox.getChildren().clear();
    if (row == null) {
      forColumnLabel.setText("(select a mapping above)");
      modeCombo.setValue(ValueHandlingMode.EXTRACT_VALUES);
      valueSection.setVisible(false);
      setDisable(true);
      loading = false;
      return;
    }
    setDisable(false);
    updateHeader();
    final boolean useMaps = row.isUseValueMappings();
    modeCombo.setValue(
        useMaps ? ValueHandlingMode.VALUE_MAPPINGS : ValueHandlingMode.EXTRACT_VALUES);
    valueSection.setVisible(useMaps);
    dropUnmappedCheck.setSelected(row.isDropUnmapped());
    for (final MetadataValueMapping vm : row.getValueMappings()) {
      addFieldRow(vm.from(), vm.to());
    }
    ensureTrailingEmptyRow();
    loading = false;
  }

  /**
   * Refreshes the header to reflect the target row's current column name without rebuilding the
   * value-mapping fields. Use this when only the row configuration changed.
   */
  public void updateHeader() {
    if (target == null) {
      return;
    }
    final String column = target.toMapping().columnName();
    forColumnLabel.setText("for column '" + (column.isBlank() ? "?" : column) + "'");
  }

  private void addFieldRow(@NotNull final String from, @NotNull final String to) {
    final TextField fromField = new TextField(from);
    fromField.setPromptText("extracted value");
    FxTextFields.autoGrowFitText(fromField, 10, 25);
    final TextField toField = new TextField(to);
    toField.setPromptText("stored value");
    FxTextFields.autoGrowFitText(toField, 10, 25);

    // action set below once the row HBox exists
    final Button remove = FxButtons.createButton("✕", "Remove this value mapping", () -> {
    });

    // layout is fixed: [fromField, arrow, toField, removeButton] - see fromField()/toField()
    final HBox row = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, fromField,
        FxLabels.newLabel("→"), toField, remove);

    remove.setOnAction(_ -> {
      fieldsBox.getChildren().remove(row);
      ensureTrailingEmptyRow();
      writeBack();
    });
    fromField.textProperty().addListener((_, _, _) -> {
      if (!loading) {
        maybeAppendRow();
        writeBack();
      }
    });
    toField.textProperty().addListener((_, _, _) -> {
      if (!loading) {
        writeBack();
      }
    });

    fieldsBox.getChildren().add(row);
  }

  // append a new empty row once the user starts typing into the last one
  private void maybeAppendRow() {
    final List<Node> rows = fieldsBox.getChildren();
    if (!rows.isEmpty() && !fromField((HBox) rows.getLast()).getText().isBlank()) {
      addFieldRow("", "");
    }
  }

  private void ensureTrailingEmptyRow() {
    final List<Node> rows = fieldsBox.getChildren();
    if (rows.isEmpty() || !fromField((HBox) rows.getLast()).getText().isBlank()) {
      addFieldRow("", "");
    }
  }

  // pre-fill the left (extracted value) side with all values matched across the loaded files
  private void autoFillFromFiles() {
    if (target == null) {
      return;
    }
    final List<String> values = SampleMetadataExtractionUtils.distinctMatchedValues(
        target.toMapping(), rawFiles);
    final Set<String> existing = new HashSet<>();
    for (final Node node : fieldsBox.getChildren()) {
      final String from = fromField((HBox) node).getText();
      if (from != null && !from.isBlank()) {
        existing.add(from.trim().toLowerCase());
      }
    }
    loading = true;
    // drop empty/trailing rows so the new values append in matching order
    fieldsBox.getChildren().removeIf(node -> fromField((HBox) node).getText().isBlank());
    for (final String value : values) {
      if (existing.add(value.trim().toLowerCase())) {
        addFieldRow(value, "");
      }
    }
    ensureTrailingEmptyRow();
    loading = false;
    writeBack();
  }

  private void writeBack() {
    if (target == null) {
      return;
    }
    final List<MetadataValueMapping> result = new ArrayList<>();
    for (final Node node : fieldsBox.getChildren()) {
      final HBox row = (HBox) node;
      final String from = fromField(row).getText();
      if (from != null && !from.isBlank()) {
        result.add(new MetadataValueMapping(from.trim(), toField(row).getText()));
      }
    }
    target.setValueMappings(result);
    onChange.run();
  }

  private static TextField fromField(@NotNull final HBox row) {
    return (TextField) row.getChildren().get(0);
  }

  private static TextField toField(@NotNull final HBox row) {
    return (TextField) row.getChildren().get(2);
  }
}
