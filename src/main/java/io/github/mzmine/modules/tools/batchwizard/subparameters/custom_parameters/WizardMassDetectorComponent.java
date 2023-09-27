/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

/**
 * Combo is on the top and center is free for additional controls
 */
public class WizardMassDetectorComponent extends
    CustomComboComponent<MassDetectorWizardOptions, WizardMassDetectorNoiseLevels> {

  private final NumberFormat factorFormat = new DecimalFormat("0.00");

  private final TextField txtMs1 = new TextField("");
  private final TextField txtMsn = new TextField("");

  public WizardMassDetectorComponent(MassDetectorWizardOptions[] options,
      WizardMassDetectorNoiseLevels defaultValue) {
    super(options);

    txtMs1.setPrefColumnCount(6);
    txtMsn.setPrefColumnCount(6);

    GridPane pane = new GridPane();
    pane.setPadding(new Insets(10));
    pane.setVgap(5);
    pane.setHgap(5);

    pane.add(labelFor("MS1"), 0, 0);
    pane.add(labelFor("MS2..MSn"), 0, 1);
    pane.add(txtMs1, 1, 0);
    pane.add(txtMsn, 1, 1);

    setCenter(pane);
    comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {

      try {
        WizardMassDetectorNoiseLevels full = getValue();
        if (full != null) {
          setValue(full);
        }
      } catch (IllegalArgumentException e) {
        // wrong user input
      }
    });
    setValue(defaultValue);
  }

  @NotNull
  private static Label labelFor(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold");
    return label;
  }


  public WizardMassDetectorNoiseLevels getValue() throws IllegalArgumentException {
    MassDetectorWizardOptions value = comboBox.getValue();
    try {
      double ms1 = Double.parseDouble(txtMs1.getText());
      double msn = Double.parseDouble(txtMsn.getText());
      return new WizardMassDetectorNoiseLevels(value, ms1, msn);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid text input. Provide numbers.");
    }
  }

  public void setValue(WizardMassDetectorNoiseLevels value) {
    this.value = value;
    super.setValue(value);
    if (value == null) {
      txtMs1.setText("");
      txtMsn.setText("");
      return;
    }
    comboBox.setValue(value.getValueType());
    txtMs1.setText(format(value.getMs1NoiseLevel()));
    txtMsn.setText(format(value.getMsnNoiseLevel()));
  }

  private String format(double v) {
    NumberFormat form =
        comboBox.getValue() == MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL ? factorFormat
            : MZmineCore.getConfiguration().getGuiFormats().intensityFormat();
    return form.format(v);
  }


  public void setToolTipText(String toolTip) {
    txtMsn.setTooltip(new Tooltip(toolTip));
    txtMs1.setTooltip(new Tooltip(toolTip));
  }

}
