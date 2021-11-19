/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

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

  public void setValue(List<Double> doublesList) {
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
