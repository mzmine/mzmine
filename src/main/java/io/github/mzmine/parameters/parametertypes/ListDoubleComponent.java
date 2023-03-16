/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.jetbrains.annotations.Nullable;

public class ListDoubleComponent extends GridPane {

  List<TextField> inputFields = new ArrayList<>();
  private static final int INIT_NUM_OF_FIELDS = 3;

  public ListDoubleComponent() {

    // Add initial input fields
    for (int i = 0; i < INIT_NUM_OF_FIELDS; i++) {
      TextField newInputField = new TextField();
      newInputField.setPrefColumnCount(3);
      inputFields.add(newInputField);

      if (i != 0) {
        add(new Text(", "), i * 2 - 1, 0);
      }
      add(newInputField, i * 2, 0);
    }

    // Add button for the new fields addition
    Button newFieldButton = new Button("  +  ");
    add(new Text(", "), INIT_NUM_OF_FIELDS * 2 - 1, 0);
    add(newFieldButton, INIT_NUM_OF_FIELDS * 2, 0);
    newFieldButton.setOnAction(event -> {

      // Create new text field
      TextField newInputField = new TextField();
      newInputField.setPrefColumnCount(3);
      inputFields.add(newInputField);

      // Add new text field to the grid pane
      add(new Text(", "), inputFields.size() * 2 - 1, 0);
      add(newInputField, inputFields.size() * 2 - 2, 0);
      getChildren().remove(newFieldButton);
      add(newFieldButton, inputFields.size() * 2, 0);
    });
  }

  public List<Double> getValue() {
    List<Double> doublesList = new ArrayList<>();

    // Get the input from all input fields and collect them to the list
    for (TextField inputFields : inputFields) {
      try {
        if (!inputFields.getText().equals("")) {
          doublesList.add(Double.parseDouble(inputFields.getText()));
        }
      } catch (NumberFormatException exception) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Input error");
        alert.setHeaderText("List of doubles must contain only double values");
        alert.showAndWait();
      }
    }

    return doublesList;
  }

  public void setValue(@Nullable List<Double> doublesList) {
    if (doublesList == null) {
      return;
    }
    if (doublesList.size() > inputFields.size()) {
      throw new IllegalArgumentException("doublesList.size() > inputFields.size()");
    }

    for (int i = 0; i < doublesList.size(); i++) {
      inputFields.get(i).setText(doublesList.get(i).toString());
    }
  }

  public void setToolTipText(String toolTip) {
    for (TextField inputField : inputFields) {
      inputField.setTooltip(new Tooltip(toolTip));
    }
  }

}
