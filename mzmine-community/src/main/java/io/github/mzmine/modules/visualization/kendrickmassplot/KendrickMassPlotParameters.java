/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

/**
 * parameters for Kendrick mass plots
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final ComboParameter<KendrickPlotDataTypes> xAxisValues = new ComboParameter<>(
      "X-Axis", "Select a parameter to be plotted on axis", KendrickPlotDataTypes.values(),
      KendrickPlotDataTypes.MZ);

  public static final StringParameter xAxisCustomKendrickMassBase = new StringParameter(
      "Repeating unit for x-Axis",
      "Enter a repeating molecular formula used as Kendrick mass base to calculate Kendrick mass defect",
      "CH2");

  public static final ComboParameter<KendrickPlotDataTypes> yAxisValues = new ComboParameter<>(
      "Y-Axis", "Select a parameter to be plotted on axis", KendrickPlotDataTypes.values(),
      KendrickPlotDataTypes.KENDRICK_MASS_DEFECT);

  public static final StringParameter yAxisCustomKendrickMassBase = new StringParameter(
      "Repeating unit for Y-Axis",
      "Enter a repeating molecular formula used as Kendrick mass base to calculate Kendrick mass defect",
      "H");

  public static final ComboParameter<KendrickPlotDataTypes> colorScaleValues = new ComboParameter<>(
      "Color scale", "Select a parameter to be plotted as color scale",
      KendrickPlotDataTypes.values(), KendrickPlotDataTypes.RETENTION_TIME);

  public static final StringParameter colorScaleCustomKendrickMassBase = new StringParameter(
      "Repeating unit for color scale",
      "Enter a repeating molecular formula used as Kendrick mass base to calculate Kendrick mass defect",
      "CH2");
  public static final ComboParameter<KendrickPlotDataTypes> bubbleSizeValues = new ComboParameter<>(
      "Bubble size", "Select a parameter to be plotted as bubble size",
      KendrickPlotDataTypes.values(), KendrickPlotDataTypes.INTENSITY);

  public static final StringParameter bubbleSizeCustomKendrickMassBase = new StringParameter(
      "Repeating unit for bubble size",
      "Enter a repeating molecular formula used as Kendrick mass base to calculate Kendrick mass defect",
      "CH2");

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public KendrickMassPlotParameters() {
    super(new Parameter[]{featureList, xAxisValues, xAxisCustomKendrickMassBase, yAxisValues,
            yAxisCustomKendrickMassBase, colorScaleValues, colorScaleCustomKendrickMassBase,
            bubbleSizeValues, bubbleSizeCustomKendrickMassBase, windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/kendrickmass/kendrick_mass_plot.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    KendrickMassPlotSetupDialog dialog = new KendrickMassPlotSetupDialog(valueCheckRequired, this,
        null);

    var xAxisValueComponent = dialog.getComponentForParameter(xAxisValues);
    var xAxisCustomKendrickMassBaseComponent = dialog.getComponentForParameter(
        xAxisCustomKendrickMassBase);

    xAxisCustomKendrickMassBaseComponent.setDisable(!xAxisValues.getValue().isKendrickType());
    xAxisValueComponent.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          try {
            boolean isKendrickType = newValue.isKendrickType();
            xAxisCustomKendrickMassBaseComponent.setDisable(!isKendrickType);
          } catch (Exception ex) {
            // do nothing user might be still typing
            xAxisCustomKendrickMassBaseComponent.setDisable(true);
          }
        });

    var yAxisValueComponent = dialog.getComponentForParameter(yAxisValues);
    var yAxisCustomKendrickMassBaseComponent = dialog.getComponentForParameter(
        yAxisCustomKendrickMassBase);

    yAxisCustomKendrickMassBaseComponent.setDisable(!yAxisValues.getValue().isKendrickType());
    yAxisValueComponent.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          try {
            boolean isKendrickType = newValue.isKendrickType();
            yAxisCustomKendrickMassBaseComponent.setDisable(!isKendrickType);
          } catch (Exception ex) {
            // do nothing user might be still typing
            yAxisCustomKendrickMassBaseComponent.setDisable(true);
          }
        });

    var colorScaleValueComponent = dialog.getComponentForParameter(colorScaleValues);
    var colorScaleCustomKendrickMassBaseComponent = dialog.getComponentForParameter(
        colorScaleCustomKendrickMassBase);

    colorScaleCustomKendrickMassBaseComponent.setDisable(
        !colorScaleValues.getValue().isKendrickType());
    colorScaleValueComponent.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          try {
            boolean isKendrickType = newValue.isKendrickType();
            colorScaleCustomKendrickMassBaseComponent.setDisable(!isKendrickType);
          } catch (Exception ex) {
            // do nothing user might be still typing
            colorScaleCustomKendrickMassBaseComponent.setDisable(true);
          }
        });

    var bubbleSizeValueComponent = dialog.getComponentForParameter(bubbleSizeValues);
    var bubbleSizeCustomKendrickMassBaseComponent = dialog.getComponentForParameter(
        bubbleSizeCustomKendrickMassBase);

    bubbleSizeCustomKendrickMassBaseComponent.setDisable(
        !colorScaleValues.getValue().isKendrickType());
    bubbleSizeValueComponent.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          try {
            boolean isKendrickType = newValue.isKendrickType();
            bubbleSizeCustomKendrickMassBaseComponent.setDisable(!isKendrickType);
          } catch (Exception ex) {
            // do nothing user might be still typing
            bubbleSizeCustomKendrickMassBaseComponent.setDisable(true);
          }
        });

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }

}
