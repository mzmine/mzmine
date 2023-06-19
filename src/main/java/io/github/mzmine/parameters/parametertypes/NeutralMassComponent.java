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

    double neutral = (ionMass.doubleValue() - ionType.getAddedMass()) * charge.intValue();

    neutralMassField.setText(MZmineCore.getConfiguration().getMZFormat().format(neutral));
  }

  public void setToolTipText(String toolTip) {
    ionMassField.setTooltip(new Tooltip(toolTip));
  }
}
