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
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor for a single {@link MetadataRegexMapping}. Shows the column configuration in one row and
 * an optional, auto-growing list of case-insensitive value mappings below it.
 */
public class MetadataRegexMappingRow extends VBox {

  private final ComboBox<RegexInputSource> sourceCombo;
  private final TextField columnField;
  private final ComboBox<ExtractColumnType> typeCombo;
  private final TextField regexField;
  private final TextField defaultField;
  private final ToggleButton mappingToggle;
  private final CheckBox dropUnmappedCheck;
  private final VBox valueMapBox;
  private final List<RawDataFile> rawFiles;

  private @Nullable Consumer<MetadataRegexMappingRow> onActivate;
  private @Nullable Runnable onChange;
  private @Nullable Consumer<MetadataRegexMappingRow> onRemove;

  public MetadataRegexMappingRow(@NotNull final MetadataRegexMapping mapping,
      @NotNull final List<String> columnSuggestions, @NotNull final List<RawDataFile> rawFiles) {
    super(FxLayout.DEFAULT_SPACE);
    setPadding(new Insets(FxLayout.DEFAULT_SPACE));
    this.rawFiles = rawFiles;

    sourceCombo = FxComboBox.createComboBox("String of the file used as regex input",
        List.of(RegexInputSource.values()));
    sourceCombo.setValue(mapping.inputSource());

    columnField = new TextField(mapping.columnName());
    columnField.setPromptText("column name");
    FxTextFields.autoGrowFitText(columnField, 10, 30);
    // suggest existing metadata columns (e.g. mzmine_sample_type) while still allowing new names
    FxTextFields.bindAutoCompletion(columnField, columnSuggestions);

    typeCombo = FxComboBox.createComboBox(
        "Target column type. Auto detects the type from all extracted values.",
        List.of(ExtractColumnType.values()));
    typeCombo.setValue(mapping.type());

    regexField = new TextField(mapping.regex());
    regexField.setPromptText("regex, e.g. _([A-Za-z]+)_  (case-insensitive)");
    FxTextFields.autoGrowFitText(regexField, 16, 60);

    defaultField = new TextField(mapping.defaultValue());
    defaultField.setPromptText("default");
    FxTextFields.autoGrowFitText(defaultField, 6, 20);
    defaultField.setTooltip(new Tooltip(
        "Value used when the regex does not match this file. Leave empty to keep the cell blank."));

    mappingToggle = new ToggleButton("Value map");
    mappingToggle.setTooltip(new Tooltip(
        "Map extracted values to other values, e.g. media → blank (case-insensitive)."));

    final var removeButton = FxButtons.createButton("✕", "Remove this mapping", () -> fireRemove());

    final HBox controls = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, FxLabels.newLabel("from"),
        sourceCombo, FxLabels.newLabel("to column"), columnField, typeCombo,
        FxLabels.newLabel("regex"), regexField, FxLabels.newLabel("default"), defaultField,
        mappingToggle, removeButton);

    dropUnmappedCheck = new CheckBox("Drop values not listed below");
    dropUnmappedCheck.setSelected(mapping.dropUnmapped());
    dropUnmappedCheck.setTooltip(new Tooltip(
        "If checked, extracted values that are not in the mapping list below are left empty."));

    final var fillButton = FxButtons.createButton("Fill values from files",
        "Add all values matched in the loaded raw data files to the list below.",
        this::autoFillFromFiles);

    valueMapBox = FxLayout.newVBox(Insets.EMPTY);
    final HBox optionsRow = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, fillButton,
        dropUnmappedCheck);
    final VBox mappingArea = FxLayout.newVBox(Insets.EMPTY,
        FxLabels.newItalicLabel("Value mappings (extracted value → stored value)"), optionsRow,
        valueMapBox);
    // hidden until the toggle is selected, but keeps its values either way
    mappingArea.managedProperty().bind(mappingArea.visibleProperty());
    mappingArea.visibleProperty().bind(mappingToggle.selectedProperty());

    getChildren().addAll(controls, mappingArea);

    // build the value mapping rows (plus a trailing empty one to type into)
    for (final MetadataValueMapping vm : mapping.activeValueMappings()) {
      addValueMapRow(vm.from(), vm.to());
    }
    ensureTrailingEmptyRow();
    mappingToggle.setSelected(!mapping.activeValueMappings().isEmpty());

    registerListeners();
  }

  private void registerListeners() {
    registerEditable(sourceCombo);
    registerEditable(typeCombo);
    sourceCombo.valueProperty().addListener((_, _, _) -> fireChangeActivated());
    typeCombo.valueProperty().addListener((_, _, _) -> fireChange());

    registerEditable(columnField);
    registerEditable(regexField);
    registerEditable(defaultField);
    columnField.textProperty().addListener((_, _, _) -> fireChange());
    regexField.textProperty().addListener((_, _, _) -> fireChangeActivated());
    defaultField.textProperty().addListener((_, _, _) -> fireChange());
    dropUnmappedCheck.selectedProperty().addListener((_, _, _) -> fireChange());
    mappingToggle.selectedProperty().addListener((_, _, _) -> fireActivate());
  }

  // focusing any control makes this the active row for the preview
  private void registerEditable(final Control control) {
    control.focusedProperty().addListener((_, _, focused) -> {
      if (focused) {
        fireActivate();
      }
    });
  }

  // -------------------------------------------------------------------------------------- value map

  private void addValueMapRow(@NotNull final String from, @NotNull final String to) {
    final TextField fromField = new TextField(from);
    fromField.setPromptText("extracted value");
    FxTextFields.autoGrowFitText(fromField, 10, 25);
    final TextField toField = new TextField(to);
    toField.setPromptText("stored value");
    FxTextFields.autoGrowFitText(toField, 10, 25);

    // action is set below once the row HBox exists
    final var removeButton = FxButtons.createButton("✕", "Remove this value mapping", () -> {
    });

    // layout is fixed: [fromField, arrow, toField, removeButton] - see fromField()/toField()
    final HBox row = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, fromField,
        FxLabels.newLabel("→"), toField, removeButton);

    removeButton.setOnAction(_ -> {
      valueMapBox.getChildren().remove(row);
      ensureTrailingEmptyRow();
      fireChange();
    });

    fromField.textProperty().addListener((_, _, _) -> {
      maybeAppendRow();
      fireChange();
    });
    toField.textProperty().addListener((_, _, _) -> fireChange());
    fromField.focusedProperty().addListener((_, _, focused) -> {
      if (focused) {
        fireActivate();
      }
    });

    valueMapBox.getChildren().add(row);
  }

  // append a new empty row once the user starts typing into the last one
  private void maybeAppendRow() {
    final List<Node> rows = valueMapBox.getChildren();
    if (rows.isEmpty()) {
      return;
    }
    final HBox last = (HBox) rows.getLast();
    if (!fromField(last).getText().isBlank()) {
      addValueMapRow("", "");
    }
  }

  private void ensureTrailingEmptyRow() {
    final List<Node> rows = valueMapBox.getChildren();
    if (rows.isEmpty() || !fromField((HBox) rows.getLast()).getText().isBlank()) {
      addValueMapRow("", "");
    }
  }

  // pre-fill the left (extracted value) side with all values matched across the loaded files
  private void autoFillFromFiles() {
    final List<String> values = SampleMetadataExtractionUtils.distinctMatchedValues(toMapping(),
        rawFiles);
    final Set<String> existing = new HashSet<>();
    for (final Node node : valueMapBox.getChildren()) {
      final String from = fromField((HBox) node).getText();
      if (from != null && !from.isBlank()) {
        existing.add(from.trim().toLowerCase());
      }
    }
    // drop the empty/trailing rows so the new values append in matching order
    valueMapBox.getChildren().removeIf(node -> fromField((HBox) node).getText().isBlank());
    for (final String value : values) {
      if (existing.add(value.trim().toLowerCase())) {
        addValueMapRow(value, "");
      }
    }
    ensureTrailingEmptyRow();
    fireChange();
  }

  private static TextField fromField(@NotNull final HBox row) {
    return (TextField) row.getChildren().get(0);
  }

  private static TextField toField(@NotNull final HBox row) {
    return (TextField) row.getChildren().get(2);
  }

  private @NotNull List<MetadataValueMapping> collectValueMappings() {
    final List<MetadataValueMapping> result = new ArrayList<>();
    for (final Node node : valueMapBox.getChildren()) {
      final HBox row = (HBox) node;
      final String from = fromField(row).getText();
      if (from != null && !from.isBlank()) {
        result.add(new MetadataValueMapping(from.trim(), toField(row).getText()));
      }
    }
    return result;
  }

  // ------------------------------------------------------------------------------------------ value

  /**
   * @return the mapping represented by the current state of this row.
   */
  public @NotNull MetadataRegexMapping toMapping() {
    return new MetadataRegexMapping(sourceCombo.getValue(), columnField.getText().trim(),
        typeCombo.getValue(), regexField.getText(), defaultField.getText(),
        dropUnmappedCheck.isSelected(), collectValueMappings());
  }

  // --------------------------------------------------------------------------------------- callbacks

  public void setOnActivate(@Nullable final Consumer<MetadataRegexMappingRow> onActivate) {
    this.onActivate = onActivate;
  }

  public void setOnChange(@Nullable final Runnable onChange) {
    this.onChange = onChange;
  }

  public void setOnRemove(@Nullable final Consumer<MetadataRegexMappingRow> onRemove) {
    this.onRemove = onRemove;
  }

  private void fireActivate() {
    if (onActivate != null) {
      onActivate.accept(this);
    }
  }

  private void fireChange() {
    if (onChange != null) {
      onChange.run();
    }
  }

  private void fireChangeActivated() {
    fireActivate();
    fireChange();
  }

  private void fireRemove() {
    if (onRemove != null) {
      onRemove.accept(this);
    }
  }
}
