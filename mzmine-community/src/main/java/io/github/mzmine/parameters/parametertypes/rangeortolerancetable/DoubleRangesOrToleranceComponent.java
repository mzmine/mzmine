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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.rangeortolerancetable;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.factories.TableColumns.ColumnAlignment;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.parameters.UserParameter;
import java.text.NumberFormat;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jmol.util.C;

public class DoubleRangesOrToleranceComponent<C extends Node> extends RangesOrToleranceComponent<Double, C> {

  public DoubleRangesOrToleranceComponent(UserParameter<? extends Tolerance<Double>, C> toleranceParameter,
      String unit, NumberFormat numberFormat) {
    super(toleranceParameter, unit, numberFormat);
  }

  @Override
  protected @NotNull RangeOrValue<Double> createNewDefaultValue() {
    return new RangeOrValue<>(300d, 301d);
  }

  @Override
  protected @NotNull TableColumn<RangeOrValue<Double>, Double> createLowerEditableFormattedColumn(
      String name, NumberFormat numberFormat) {
    final TableColumn<RangeOrValue<Double>, Double> lowerCol = TableColumns.createColumn(name, 120,
        numberFormat, ColumnAlignment.RIGHT, RangeOrValue::lowerProperty);
    TableColumns.setFormattedEditableCellFactory(lowerCol, numberFormat);
    lowerCol.setEditable(true);
    return lowerCol;
  }

  @Override
  protected @NotNull TableColumn<RangeOrValue<Double>, Double> createUpperEditableFormattedColumn(
      String name, NumberFormat numberFormat) {
    final TableColumn<RangeOrValue<Double>, Double> lowerCol = TableColumns.createColumn(name, 120,
        numberFormat, ColumnAlignment.RIGHT, RangeOrValue::upperProperty);
    TableColumns.setFormattedEditableCellFactory(lowerCol, numberFormat);
    lowerCol.setEditable(true);
    return lowerCol;
  }
}
