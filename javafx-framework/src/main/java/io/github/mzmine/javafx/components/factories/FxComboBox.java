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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.javafx.components.converters.OptionsStringConverter;
import io.github.mzmine.javafx.components.skins.DynamicWidthComboBoxSkin;
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
import javafx.util.converter.DefaultStringConverter;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  public static <T> ComboBox<T> createComboBox(@Nullable String tooltip, Collection<T> values,
      @Nullable Property<T> selectedItem) {
    return addContent(tooltip, values, selectedItem, new ComboBox<>());
  }

  private static <T, COMBO extends ComboBox<T>> @NotNull COMBO addContent(
      @Nullable final String tooltip, final Collection<T> values,
      @Nullable final Property<T> selectedItem, final COMBO combo) {
    if (values instanceof ObservableList<T> ov) {
      combo.setItems(ov);
    } else if (values instanceof List<T> list) {
      combo.setItems(FXCollections.observableList(list));
    } else if (values != null) {
      combo.setItems(FXCollections.observableList(List.copyOf(values)));
    }
    if (selectedItem != null) {
      combo.valueProperty().bindBidirectional(selectedItem);
    }
    if (tooltip != null) {
      combo.setTooltip(new Tooltip(tooltip));
    }
    return combo;
  }

  public static <T> SearchableComboBox<T> newSearchableComboBox(@Nullable String tooltip,
      @NotNull List<T> values, @NotNull Property<T> selectedItem) {
    return addContent(tooltip, values, selectedItem, new SearchableComboBox<>());
  }

  public static <T> ComboBox<T> createComboBox(@Nullable String tooltip,
      @Nullable Collection<T> values) {
    return addContent(tooltip, values, null, new ComboBox<>());
  }

  public static <T> ComboBox<T> createComboBox(@Nullable String tooltip, T[] values,
      Property<T> selectedItem) {
    return createComboBox(tooltip, List.of(values), selectedItem);
  }

  /**
   * A combobox with auto complete for its items. This is useful for parameters when the input text
   * may be used although items do not define this option. This may be different from
   * SearchableComboBox but not testet.
   */
  public static <T> ComboBox<T> newAutoCompleteComboBox(@Nullable String tooltip) {
    return newAutoCompleteComboBox(tooltip, null);
  }

  /**
   * A combobox with auto complete for its items. This is useful for parameters when the input text
   * may be used although items do not define this option. This may be different from
   * SearchableComboBox but not testet.
   */
  public static <T> ComboBox<T> newAutoCompleteComboBox(@Nullable String tooltip,
      @Nullable Collection<T> items) {
    return newAutoCompleteComboBox(tooltip, items, null, 4, -1);
  }

  public static <T> ComboBox<T> newAutoCompleteComboBox(@Nullable String tooltip,
      @Nullable Collection<T> items, @Nullable final Property<T> selectedItem, int minColumnCount,
      int maxColumnCount) {
    final ComboBox<T> combo = createComboBox(tooltip, items, selectedItem);
    // auto complete for items
    FxComboBox.bindAutoCompletion(combo, true, minColumnCount, maxColumnCount);
    return combo;
  }

  /**
   * @param minColumnCount -1 to deactivate
   * @param maxColumnCount -1 to deactivate
   */
  public static <T> ComboBox<T> applyAutoGrow(ComboBox<T> combo, final int minColumnCount,
      final int maxColumnCount) {
    combo.setEditable(true);
    combo.setSkin(new DynamicWidthComboBoxSkin<>(combo, minColumnCount, maxColumnCount));
    return combo;
  }

  /**
   * Automatically bind auto-completion to a combobox
   *
   * @param combo the target for auto-completion. Only works if set to editable
   * @return AutoCompletionBinding of the string representation
   */
  public static <T> AutoCompletionBinding<T> bindAutoCompletion(ComboBox<T> combo,
      boolean autoGrow) {
    return bindAutoCompletion(combo, autoGrow, 4, -1);
  }

  /**
   * Automatically bind auto-completion to a combobox
   *
   * @param minColumnCount -1 to deactivate
   * @param maxColumnCount -1 to deactivate
   * @param combo          the target for auto-completion. Only works if set to editable
   * @return AutoCompletionBinding of the string representation
   */
  public static <T> AutoCompletionBinding<T> bindAutoCompletion(ComboBox<T> combo, boolean autoGrow,
      int minColumnCount, int maxColumnCount) {
    if (autoGrow) {
      // auto grow with input
      applyAutoGrow(combo, minColumnCount, maxColumnCount);
    }
    setEditable(combo);
    return FxTextFields.bindAutoCompletion(combo.getEditor(), false, combo.getItems());
  }

  /**
   * Also sets a {@link OptionsStringConverter} to be sure that values are converted properly. This
   * is only needed for combobox of type different from String but works also for String ComboBoxes.
   * At this point we do not know the type of T.
   *
   * <p>
   * IMPORTANT: ComboBox of String do not need this converter. But can help to reduce values to
   * actually available values. For ComboBoxes that can also take free text values like the metadata
   * group, where values are not known ahead of time, use a different component or use the regular
   * {@link DefaultStringConverter}. Or even better use TextField with auto complete.
   */
  public static <T> void setEditable(ComboBox<T> combo) {
    combo.setEditable(true);
    combo.setConverter(new OptionsStringConverter<>(combo));
  }

}
