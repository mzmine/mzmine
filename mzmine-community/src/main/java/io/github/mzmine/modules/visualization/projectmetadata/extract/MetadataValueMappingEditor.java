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

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

  private final Function<MetadataRegexMapping, List<String>> inputSupplier;
  private final Runnable onChange;

  private final ComboBox<ValueHandlingMode> modeCombo;
  private final Label forColumnLabel = FxLabels.newBoldLabel("");
  private final ComboBox<DropUnmappedMode> dropUnmappedCombo;
  private final VBox fieldsBox = FxLayout.newVBox(Insets.EMPTY);
  // holds the value-mapping controls; only shown in the VALUE_MAPPINGS mode
  private final VBox valueSection;
  // distinct resulting metadata groups across previewed files (mapping/default/drop applied)
  private final FlowPane resultsFlow = FxLayout.newFlowPane();

  private @Nullable MetadataRegexMappingRow target;
  // guards against writing back into the target while the controls are being (re)populated
  private boolean loading;

  public MetadataValueMappingEditor(
      @NotNull final Function<MetadataRegexMapping, List<String>> inputSupplier,
      @NotNull final Runnable onChange) {
    super(FxLayout.DEFAULT_SPACE);
    setPadding(new Insets(FxLayout.DEFAULT_SPACE));
    this.inputSupplier = inputSupplier;
    this.onChange = onChange;

    modeCombo = FxComboBox.createComboBox(
        "Extract values: store the extracted value directly. Value mappings: map extracted values "
            + "to other values (e.g. media → blank).", List.of(ValueHandlingMode.values()));
    modeCombo.setValue(ValueHandlingMode.EXTRACT_VALUES);

    dropUnmappedCombo = FxComboBox.createComboBox("""
            Keep unmapped values: extracted values not in the mapping list are passed through unchanged.
            Drop unmapped values: extracted values not in the mapping list are left empty.""",
        List.of(DropUnmappedMode.values()));
    dropUnmappedCombo.setValue(DropUnmappedMode.KEEP_UNMAPPED);
    dropUnmappedCombo.valueProperty().addListener((_, _, mode) -> {
      if (!loading && target != null) {
        target.setDropUnmapped(mode);
        refreshResults();
        onChange.run();
      }
    });

    final Button fillButton = FxButtons.createButton("Fill values from files",
        "Add all values matched in the previewed files to the list below.",
        this::autoFillFromFiles);

    final HBox options = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, fillButton,
        dropUnmappedCombo);
    valueSection = FxLayout.newVBox(Insets.EMPTY, FxLabels.newItalicLabel(
        "Map extracted values (left) to stored values (right), e.g. media → blank "
            + "(case-insensitive)"), options, fieldsBox);
    valueSection.managedProperty().bind(valueSection.visibleProperty());

    final HBox headerRow = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, modeCombo,
        forColumnLabel);
    getChildren().addAll(headerRow, resultsFlow, valueSection);

    modeCombo.valueProperty().addListener((_, _, mode) -> {
      final boolean useMaps = mode == ValueHandlingMode.VALUE_MAPPINGS;
      valueSection.setVisible(useMaps);
      if (!loading && target != null) {
        target.setUseValueMappings(useMaps);
        refreshResults();
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
      refreshResults();
      loading = false;
      return;
    }
    setDisable(false);
    updateHeader();
    final boolean useMaps = row.isUseValueMappings();
    modeCombo.setValue(
        useMaps ? ValueHandlingMode.VALUE_MAPPINGS : ValueHandlingMode.EXTRACT_VALUES);
    valueSection.setVisible(useMaps);
    dropUnmappedCombo.setValue(row.getDropUnmapped());
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
    // the regex/source/default may have changed, so the resulting groups can change too
    refreshResults();
  }

  // shows the distinct resulting metadata groups across all previewed files, with the row's mapping,
  // default value and drop-unmapped option applied; alternating regular and positive text color
  public void refreshResults() {
    resultsFlow.getChildren().setAll(FxLabels.newBoldLabel("Results for previewed samples:"));
    if (target == null) {
      return;
    }
    final MetadataRegexMapping mapping = target.toMapping();
    final List<String> groups = inputSupplier.apply(mapping).stream()
        .map(input -> SampleMetadataExtractionUtils.extractValue(mapping, input))
        .map(value -> value == null ? "(empty)" : value).distinct()
        .sorted(String.CASE_INSENSITIVE_ORDER).toList();

    if (groups.isEmpty()) {
      resultsFlow.getChildren().add(FxLabels.newItalicLabel("(no files loaded or selected)"));
      return;
    }
    final Color positive = ConfigService.getDefaultColorPalette().getPositiveColor();
    for (int i = 0; i < groups.size(); i++) {
      final Label chip = new Label(groups.get(i));
      // alternate between the regular text color and the positive color
      if (i % 2 == 1) {
        FxLabels.colored(chip, positive);
      }
      resultsFlow.getChildren().add(chip);
    }
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

  // pre-fill the left (extracted value) side with all values matched across the previewed files
  private void autoFillFromFiles() {
    if (target == null) {
      return;
    }
    final MetadataRegexMapping mapping = target.toMapping();
    final List<String> values = SampleMetadataExtractionUtils.distinctMatchedValuesFromInputs(
        mapping, inputSupplier.apply(mapping));
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
    refreshResults();
    onChange.run();
  }

  private static TextField fromField(@NotNull final HBox row) {
    return (TextField) row.getChildren().get(0);
  }

  private static TextField toField(@NotNull final HBox row) {
    return (TextField) row.getChildren().get(2);
  }
}
