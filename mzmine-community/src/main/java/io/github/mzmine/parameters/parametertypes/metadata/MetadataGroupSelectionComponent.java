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

package io.github.mzmine.parameters.parametertypes.metadata;

import static io.github.mzmine.javafx.components.factories.FxLabels.newLabelNoWrap;

import io.github.mzmine.javafx.components.util.FxLayout;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataGroupSelectionComponent extends GridPane {

  // auto completion is automatically bound to both fields
  private final MetadataGroupingComponent columnField = new MetadataGroupingComponent();
  private final ComboBox<String> groupField = columnField.createLinkedGroupCombo(
      "Select group from column.");

  public MetadataGroupSelectionComponent() {
    super();
    setHgap(FxLayout.DEFAULT_SPACE);
    setVgap(FxLayout.DEFAULT_SPACE);

    FxLayout.applyGrid2Col(this,
        // children
        newLabelNoWrap("Metadata column"), columnField, //
        newLabelNoWrap("Metadata group"), groupField //
    );
  }

  @NotNull
  public MetadataGroupSelection getValue() {
    final String column = columnField.getValue();
    final String group = groupField.getValue();

    if (column == null || group == null) {
      return MetadataGroupSelection.NONE;
    }

    return new MetadataGroupSelection(column.trim(), group.trim());
  }

  public void setValue(@Nullable MetadataGroupSelection value) {
    if (value == null) {
      groupField.setValue("");
      columnField.setValue("");
      return;
    }

    groupField.setValue(value.groupStr());
    columnField.setValue(value.columnName());
  }
}
