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
