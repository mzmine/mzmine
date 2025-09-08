/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.FilterableMenuPopup;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.parameters.parametertypes.StringParameterComponent;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.RowTypeFilter;
import io.github.mzmine.util.presets.PresetsButton;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

public class RowTypeFilterComponent extends HBox implements ValuePropertyComponent<RowTypeFilter> {

  private final ObjectProperty<RowTypeFilter> value = new SimpleObjectProperty<>();

  private final ComboComponent<RowTypeFilterOption> optionCombo;
  private final ComboComponent<MatchingMode> matchingModeCombo;
  private final StringParameterComponent queryField;
  private final StringProperty queryFormatErrorMessage = new SimpleStringProperty();

  public RowTypeFilterComponent(RowTypeFilter value,
      ComboComponent<RowTypeFilterOption> optionCombo,
      ComboComponent<MatchingMode> matchingModeCombo, StringParameterComponent queryField,
      boolean addPresetsMenuButton) {

    super(FxLayout.DEFAULT_SPACE, optionCombo, matchingModeCombo, queryField);

    FxLayout.applyDefaults(this, Insets.EMPTY);

    // manage presets
    if (addPresetsMenuButton) {
      addPresetsMenuButton();
    }

    this.optionCombo = optionCombo;
    this.matchingModeCombo = matchingModeCombo;
    this.queryField = queryField;

    setupValidation(queryField);

    FxComboBox.bindAutoCompletion(optionCombo, true, 4, -1);
    FxComboBox.bindAutoCompletion(matchingModeCombo, true, 1, -1);

    matchingModeCombo.tooltipProperty()
        .bind(matchingModeCombo.valueProperty().map(mode -> new Tooltip(mode.getDescription())));

    FxTextFields.autoGrowFitText(queryField, 4, 40);

    // needs delay to not update too early when all properties are set for a new filter
//    PropertyUtils.onChange(this::updateValue, optionCombo.valueProperty(),
//        matchingModeCombo.valueProperty(), queryField.textProperty());
    PropertyUtils.onChangeDelayedSubscription(this::updateValue, Duration.millis(100),
        optionCombo.valueProperty(), matchingModeCombo.valueProperty(), queryField.textProperty());

    // adjust matching modes to selected type
    optionCombo.valueProperty().subscribe((nv) -> {
      final MatchingMode old = matchingModeCombo.getValue();
      if (nv == null) {
        matchingModeCombo.getItems().clear();
        matchingModeCombo.getSelectionModel().clearSelection();
        queryField.setPromptText("");
        optionCombo.setTooltip(null);
        return;
      }

      optionCombo.setTooltip(new Tooltip(nv.getDescription()));
      queryField.setPromptText(nv.getQueryPromptText());

      matchingModeCombo.getItems().setAll(nv.getMatchingModes());
      if (old != null && nv.getMatchingModes().contains(old)) {
        matchingModeCombo.setValue(old);
      } else {
        // select first as it is the preferred
        matchingModeCombo.getSelectionModel().select(0);
      }
    });

    setValue(value);
    this.value.subscribe(nv -> setToComponents());
  }

  private void addPresetsMenuButton() {
    final PresetsButton<RowTypeFilterPreset> button = new PresetsButton<>(false,
        new RowTypeFilterPresetStore(),
        name -> value.get() == null ? null : new RowTypeFilterPreset(name, value.get()),
        preset -> value.set(preset.filter()));
    getChildren().add(button);

    final FilterableMenuPopup popup = button.getPopup();
    if (popup != null) {
      setListCellFactory(popup);
    }
  }

  private static void setListCellFactory(FilterableMenuPopup popup) {
    popup.getListView().setCellFactory(_ -> new RowTypeFilterPresetListCell());
  }

  private void setupValidation(StringParameterComponent queryField) {
    FxValidation.registerErrorValidator(queryField, queryFormatErrorMessage);
  }

  private void setToComponents() {
    final RowTypeFilter filter = value.get();
    if (filter == null) {
      // do not reset the combos, keep selection
      queryField.setText("");
      return;
    }
    optionCombo.getSelectionModel().select(filter.selectedType());
    matchingModeCombo.getSelectionModel().select(filter.matchingMode());
    final int caretPosition = queryField.getCaretPosition();
    queryField.setText(filter.query());
    if (caretPosition >= 0) {
      queryField.positionCaret(Math.min(filter.query().length(), caretPosition));
    }
  }

  private void updateValue() {
    final RowTypeFilterOption type = optionCombo.getValue();
    final MatchingMode mode = matchingModeCombo.getValue();
    final String query = queryField.getText().trim();
    if (type == null || mode == null) {
      value.set(null);
      return;
    }
    try {
      value.set(RowTypeFilter.create(type, mode, query));
      queryFormatErrorMessage.set(null);
    } catch (QueryFormatException e) {
      queryFormatErrorMessage.set(e.getMessage());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setValue(@Nullable RowTypeFilter value) {
    this.value.set(value);
  }

  public @Nullable RowTypeFilter getValue() {
    return value.getValue();
  }

  @Override
  public Property<RowTypeFilter> valueProperty() {
    return value;
  }

}
