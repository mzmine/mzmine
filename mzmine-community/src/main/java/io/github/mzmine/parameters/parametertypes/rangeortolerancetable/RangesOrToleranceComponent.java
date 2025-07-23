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

public abstract class RangesOrToleranceComponent<T extends Number & Comparable<T>, C extends Node> extends
    BorderPane {

  @NotNull
  protected final UserParameter<Tolerance<T>, C> toleranceParameter;
  protected final ListProperty<RangeOrValue<T>> ranges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  protected final TableView<RangeOrValue<T>> table = new TableView<>(ranges.getValue());
  protected final C toleranceComponent;

  protected final ObjectProperty<RangeOrValueResult<T>> value = new SimpleObjectProperty<>();

  public RangesOrToleranceComponent(UserParameter<? extends Tolerance<T>, C> toleranceParameter,
      String unit, NumberFormat numberFormat) {
    this.toleranceParameter = (UserParameter<Tolerance<T>, C>) toleranceParameter;
    toleranceComponent = toleranceParameter.createEditingComponent();
    final Label tolLabel = FxLabels.newBoldLabel(toleranceParameter.getName());

    setTop(FxLayout.newHBox(tolLabel, toleranceComponent));

    final TableColumn<RangeOrValue<T>, T> lowerCol = createLowerEditableFormattedColumn(
        "lower " + unit, numberFormat);
    table.getColumns().add(lowerCol);

    final TableColumn<RangeOrValue<T>, T> upperCol = createUpperEditableFormattedColumn(
        "upper " + unit, numberFormat);
    table.getColumns().add(upperCol);

    table.setEditable(true);
    setCenter(table);
    BorderPane.setAlignment(table, Pos.TOP_LEFT);
    table.setMinHeight(150);
    table.setPrefHeight(USE_COMPUTED_SIZE);
    table.setMaxHeight(600);
    TableColumns.autoFitLastColumn(table);

    ranges.subscribe(list -> updateValue(toleranceParameter, list));

    final Button add = FxButtons.createButton("Add", () -> {
      if (!ranges.isEmpty()) {
        final RangeOrValue<T> last = ranges.getLast();
        ranges.add(new RangeOrValue<>(last.getLower(), last.getUpper()));
      } else {
        ranges.add(createNewDefaultValue());
      }
    });

    final Button remove = FxButtons.createButton("Remove", () -> {
      final RangeOrValue<T> selected = table.getSelectionModel().getSelectedItem();
      if (selected != null) {
        ranges.remove(selected);
      }
    });

    final VBox buttons = FxLayout.newVBox(Pos.TOP_LEFT, add, remove);
    add.minWidthProperty().bind(remove.widthProperty());
    add.maxWidthProperty().bind(remove.widthProperty());

    setRight(buttons);
  }

  @NotNull
  protected abstract RangeOrValue<T> createNewDefaultValue();

  protected abstract @NotNull TableColumn<RangeOrValue<T>, T> createLowerEditableFormattedColumn(
      String name, NumberFormat numberFormat);

  protected abstract @NotNull TableColumn<RangeOrValue<T>, T> createUpperEditableFormattedColumn(
      String name, NumberFormat numberFormat);

  private void updateValue(UserParameter<? extends Tolerance<T>, C> toleranceParameter,
      ObservableList<RangeOrValue<T>> list) {
    toleranceParameter.setValueFromComponent(toleranceComponent);
    final Tolerance<T> tolerance = toleranceParameter.getValue();
    final List<RangeOrValue<T>> rangesOrValues = list.stream().filter(RangeOrValue::isValid)
        .toList();
    value.set(new RangeOrValueResult<>(rangesOrValues, tolerance));
  }

  public RangeOrValueResult<T> getValue() {
    return value.get();
  }

  public void setValue(RangeOrValueResult<T> values) {
    toleranceParameter.setValue(values.tolerance());
    toleranceParameter.setValueToComponent(toleranceComponent, values.tolerance());
    table.getItems().setAll(values.ranges());
  }
}
