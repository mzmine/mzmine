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

package io.github.mzmine.gui.preferences;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 */
public class NumOfThreadsEditor extends BorderPane {

  private static final ObservableList<String> options = FXCollections.observableArrayList(
      "Set to the number of CPU cores (" + Runtime.getRuntime().availableProcessors() + ")",
      "Set manually");

  private ComboBox<String> optionCombo;
  private TextField numField;

  public NumOfThreadsEditor() {

    optionCombo = new ComboBox<>(options);
    optionCombo.setOnAction(e -> {
      numField.setDisable(isAutomatic());
    });
    setLeft(optionCombo);

    numField = new TextField();
    numField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.matches("\\d+")) {
        numField.setText(oldValue);
      }
    });
    numField.setPrefColumnCount(3);
    setCenter(numField);

  }

  public void setValue(boolean automatic, int numOfThreads) {
    if (automatic) {
      optionCombo.getSelectionModel().select(0);
    } else {
      optionCombo.getSelectionModel().select(1);
    }
    numField.setText(String.valueOf(numOfThreads));
    numField.setDisable(automatic);
  }

  public boolean isAutomatic() {
    int index = optionCombo.getSelectionModel().getSelectedIndex();
    return index <= 0;
  }

  public Number getNumOfThreads() {
    return Integer.valueOf(numField.getText());
  }


}
