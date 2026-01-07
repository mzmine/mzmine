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
package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.RIColumn;
import java.text.NumberFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.converter.NumberStringConverter;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class RIToleranceComponent extends HBox {

  private final ObservableList<RIColumn> toleranceTypes;
  private final NumberFormat format = ConfigService.getGuiFormats().rtFormat();
  private final TextFormatter<Number> textFormatter = new TextFormatter<>(
      new NumberStringConverter(format));
  private final TextField toleranceField;
  private final ComboBox<RIColumn> toleranceType;
  private final CheckBox allowMatchesWithoutRi;

  public RIToleranceComponent(ObservableList<RIColumn> riColumnTypes) {
    this.toleranceTypes = FXCollections.observableArrayList(riColumnTypes);

    setSpacing(5);
    this.toleranceField = new TextField();
    this.toleranceField.setTextFormatter(textFormatter);

    this.toleranceType = new ComboBox<>(toleranceTypes);
    this.toleranceType.getSelectionModel().select(0);

    this.allowMatchesWithoutRi = new CheckBox("Allow matches without RI");

    getChildren().addAll(this.toleranceField, this.toleranceType, allowMatchesWithoutRi);
  }

  public RITolerance getValue() {
    RIColumn selectedColumnType = toleranceType.getValue();
    String valueString = toleranceField.getText();
    final boolean allowWithoutRi = allowMatchesWithoutRi.isSelected();

    Float tolerance = null;
    try {
      tolerance = Float.parseFloat(valueString);
    } catch (Exception e) {
      return null;
    }

    return new RITolerance(tolerance, selectedColumnType, allowWithoutRi);
  }

  public void setValue(@Nullable RITolerance value) {
    if (value == null) {
      toleranceField.setText("");
      toleranceType.getSelectionModel().select(0);
      return;
    }

    float tolerance = value.getTolerance();
    RIColumn selectedColumnType = value.getRIType();

    toleranceType.setValue(selectedColumnType);
    toleranceField.setText(format.format(tolerance));
    allowMatchesWithoutRi.setSelected(value.isMatchOnNull());
  }

  public void setToolTipText(String toolTip) {
    toleranceField.setTooltip(new Tooltip(toolTip));
  }
}
