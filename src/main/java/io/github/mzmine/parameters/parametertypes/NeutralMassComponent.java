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


import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.main.MZmineCore;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class NeutralMassComponent extends GridPane {

  private ComboBox<IonizationType> ionTypeCombo;
  private TextField ionMassField, chargeField, neutralMassField;

  public NeutralMassComponent() {

    add(new Label("m/z:"), 0, 0);

    ionMassField = new TextField();
    ionMassField.textProperty().addListener((observable, oldValue, newValue) -> {
      updateNeutralMass();
    });
    ionMassField.setPrefColumnCount(8);
    add(ionMassField, 1, 0);

    add(new Label("Charge:"), 2, 0);

    chargeField = new TextField();
    chargeField.textProperty().addListener((observable, oldValue, newValue) -> {
      updateNeutralMass();
    });
    chargeField.setPrefColumnCount(2);
    add(chargeField, 3, 0);

    add(new Label("Ionization type:"), 0, 1, 2, 1);
    ionTypeCombo =
        new ComboBox<IonizationType>(FXCollections.observableArrayList(IonizationType.values()));
    ionTypeCombo.setOnAction(e -> {
      updateNeutralMass();
    });
    add(ionTypeCombo, 2, 1, 2, 1);

    add(new Label("Calculated mass:"), 0, 2, 2, 1);

    neutralMassField = new TextField();
    neutralMassField.setPrefColumnCount(8);
    neutralMassField.setStyle("-fx-background-color: rgb(192, 224, 240);");
    neutralMassField.setEditable(false);
    add(neutralMassField, 2, 2, 2, 1);

  }

  public void setIonMass(double ionMass) {
    ionMassField.setText(MZmineCore.getConfiguration().getMZFormat().format(ionMass));
    updateNeutralMass();
  }

  public void setCharge(int charge) {
    chargeField.setText(String.valueOf(charge));
    updateNeutralMass();
  }

  public void setIonType(IonizationType ionType) {
    ionTypeCombo.getSelectionModel().select(ionType);
    updateNeutralMass();
  }

  public Double getValue() {
    String stringValue = neutralMassField.getText();
    try {
      double doubleValue = Double.parseDouble(stringValue);
      return doubleValue;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public Double getIonMass() {
    String stringValue = ionMassField.getText();
    try {
      double doubleValue = Double.parseDouble(stringValue);
      return doubleValue;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public Integer getCharge() {
    String stringValue = chargeField.getText();
    try {
      int intValue = Integer.parseInt(stringValue);
      return intValue;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public IonizationType getIonType() {
    return ionTypeCombo.getSelectionModel().getSelectedItem();
  }

  private void updateNeutralMass() {

    Integer charge = getCharge();
    if (charge == null)
      return;

    Double ionMass = getIonMass();
    if (ionMass == null)
      return;

    IonizationType ionType = getIonType();
    if (ionType == null)
      return;

    double neutral = ionMass.doubleValue() * charge.intValue() - ionType.getAddedMass();

    neutralMassField.setText(MZmineCore.getConfiguration().getMZFormat().format(neutral));
  }

  public void setToolTipText(String toolTip) {
    ionMassField.setTooltip(new Tooltip(toolTip));
  }
}
