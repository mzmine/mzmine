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
