/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.javafx.components.factories;

import java.util.Collection;
import java.util.List;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.controlsfx.control.SearchableComboBox;
import org.jetbrains.annotations.NotNull;

public class FxComboBox {

  public static <T> HBox createLabeledComboBox(String label, ObservableList<T> values,
      Property<T> modelProperty) {
    final Label lab = new Label(label);
    final ComboBox<T> comboBox = new ComboBox<>(values);
    comboBox.valueProperty().bindBidirectional(modelProperty);
    lab.setLabelFor(comboBox);
    final HBox hBox = new HBox(5, lab, comboBox);
    hBox.setAlignment(Pos.CENTER_LEFT);
    return hBox;
  }

  public static <T> ComboBox<T> createComboBox(String tooltip, Collection<T> values,
      Property<T> selectedItem) {
    return addContent(tooltip, values, selectedItem, new ComboBox<T>());
  }

  private static <T, COMBO extends ComboBox<T>> @NotNull COMBO addContent(final String tooltip,
      final Collection<T> values, final Property<T> selectedItem, final COMBO combo) {
    if (values instanceof ObservableList<T> ov) {
      combo.setItems(ov);
    } else if (values instanceof List<T> list) {
      combo.setItems(FXCollections.observableList(list));
    } else {
      combo.setItems(FXCollections.observableList(List.copyOf(values)));
    }
    combo.valueProperty().bindBidirectional(selectedItem);
    combo.setTooltip(new Tooltip(tooltip));
    return combo;
  }

  public static <T> SearchableComboBox<T> newSearchableComboBox(@NotNull String tooltip,
      @NotNull List<T> values, @NotNull Property<T> selectedItem) {
    return addContent(tooltip, values, selectedItem, new SearchableComboBox<>());
  }

  public static <T> ComboBox<T> createComboBox(String tooltip, Collection<T> values) {
    final ComboBox<T> combo = new ComboBox<>();
    if (values instanceof ObservableList<T> ov) {
      combo.setItems(ov);
    } else if (values instanceof List<T> list) {
      combo.setItems(FXCollections.observableList(list));
    } else {
      combo.setItems(FXCollections.observableList(List.copyOf(values)));
    }
    combo.setTooltip(new Tooltip(tooltip));
    return combo;
  }

  public static <T> ComboBox<T> createComboBox(String tooltip, T[] values,
      Property<T> selectedItem) {
    return createComboBox(tooltip, List.of(values), selectedItem);
  }
}
