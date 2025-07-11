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

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.textfield.AutoCompletionBinding;

public class Metadata2GroupsSelectionComponent extends GridPane {

  private static final int inputSize = 20;
  private final MetadataGroupingComponent columnField = new MetadataGroupingComponent();
  private final TextField groupFieldA = new TextField();
  private final TextField groupFieldB = new TextField();

  public Metadata2GroupsSelectionComponent() {
    super();
    groupFieldA.setPrefColumnCount(inputSize);
    groupFieldB.setPrefColumnCount(inputSize);

    // auto bind unique column values to group field
    final AutoCompletionBinding<String> newBinding = FxTextFields.bindAutoCompletion(groupFieldA,
        columnField.uniqueColumnValuesProperty());
    final AutoCompletionBinding<String> newBindingB = FxTextFields.bindAutoCompletion(groupFieldB,
        columnField.uniqueColumnValuesProperty());

    FxLayout.applyGrid2Col(this,
        // children
        newLabelNoWrap("Metadata column"), columnField, //
        newLabelNoWrap("Group A"), groupFieldA, //
        newLabelNoWrap("Group B"), groupFieldB //
    );
  }

  public Metadata2GroupsSelection getValue() {
    final String column = columnField.getValue();
    final String groupA = groupFieldA.getText();
    final String groupB = groupFieldB.getText();

    if (column == null || groupA == null || groupB == null) {
      return Metadata2GroupsSelection.NONE;
    }

    return new Metadata2GroupsSelection(column.trim(), groupA.trim(), groupB.trim());
  }

  public void setValue(Metadata2GroupsSelection value) {
    if (value == null) {
      groupFieldA.setText("");
      groupFieldB.setText("");
      columnField.setValue("");
      return;
    }

    groupFieldA.setText(value.groupA());
    groupFieldB.setText(value.groupB());
    columnField.setValue(value.columnName());
  }
}
