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

package io.github.mzmine.modules.dataanalysis.heatmaps;

import java.util.ArrayList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

public class HeatmapSetupDialog extends ParameterSetupDialog {

  private ComboBox<UserParameter<?, ?>> selDataCombo;
  private ComboBox<Object> refGroupCombo;
  private UserParameter<?, ?> previousParameterSelection;

  public HeatmapSetupDialog(boolean valueCheckRequired, HeatMapParameters parameters) {
    super(valueCheckRequired, parameters);

    // Get a reference to the combo boxes
    selDataCombo = this.getComponentForParameter(HeatMapParameters.selectionData);
    refGroupCombo = this.getComponentForParameter(HeatMapParameters.referenceGroup);

    // Save a reference to current "Sample parameter" value
    previousParameterSelection = selDataCombo.getSelectionModel().getSelectedItem();

    // Call parametersChanged() to rebuild the reference group combo
    parametersChanged();

  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void parametersChanged() {

    // Get the current value of the "Sample parameter" combo
    UserParameter<?, ?> currentParameterSelection =
        selDataCombo.getSelectionModel().getSelectedItem();
    if (currentParameterSelection == null)
      return;

    // If the value has changed, update the "Reference group" combo
    if (currentParameterSelection != previousParameterSelection) {
      ArrayList<Object> values = new ArrayList<Object>();

      // Obtain all possible values
      for (RawDataFile dataFile : MZmineCore.getProjectManager().getCurrentProject()
          .getDataFiles()) {
        Object paramValue = MZmineCore.getProjectManager().getCurrentProject()
            .getParameterValue(currentParameterSelection, dataFile);
        if (paramValue == null)
          continue;
        if (!values.contains(paramValue))
          values.add(paramValue);
      }

      // Update the parameter and combo model
      Object newValues[] = values.toArray();
      super.parameterSet.getParameter(HeatMapParameters.referenceGroup).setChoices(newValues);

      ObservableList<Object> newItems = FXCollections.observableArrayList(newValues);
      refGroupCombo.setItems(newItems);


      previousParameterSelection = currentParameterSelection;
    }

    this.updateParameterSetFromComponents();

  }

}
