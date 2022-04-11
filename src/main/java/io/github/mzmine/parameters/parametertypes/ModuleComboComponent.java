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

import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class ModuleComboComponent extends FlowPane {

  private ComboBox<MZmineProcessingStep<?>> comboBox;
  private Button setButton;

  public ModuleComboComponent(MZmineProcessingStep<?> modules[]) {

    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    assert modules != null;
    assert modules.length > 0;

    var modulesList = FXCollections.observableArrayList(Arrays.asList(modules));

    comboBox = new ComboBox<MZmineProcessingStep<?>>(modulesList);
    comboBox.setOnAction(e -> {
      MZmineProcessingStep<?> selected = comboBox.getSelectionModel().getSelectedItem();
      if (selected == null) {
        setButton.setDisable(true);
        return;
      }
      ParameterSet parameterSet = selected.getParameterSet();
      int numOfParameters = parameterSet.getParameters().length;
      setButton.setDisable(numOfParameters == 0);
    });

    setButton = new Button("Setup");
    setButton.setOnAction(e -> {
      MZmineProcessingStep<?> selected = comboBox.getSelectionModel().getSelectedItem();
      if (selected == null)
        return;
      ParameterSetupDialog dialog = (ParameterSetupDialog) getScene().getWindow();
      if (dialog == null)
        return;
      ParameterSet parameterSet = selected.getParameterSet();
      parameterSet.showSetupDialog(dialog.isValueCheckRequired());
    });
    boolean buttonEnabled = (modules[0].getParameterSet() != null);
    setButton.setDisable(!buttonEnabled);

    super.setHgap(7d);
    super.getChildren().addAll(comboBox, setButton);
  }

  public int getSelectedIndex() {
    return comboBox.getSelectionModel().getSelectedIndex();
  }

  public void setSelectedItem(MZmineProcessingStep<?> selected) {
    comboBox.getSelectionModel().select(selected);
  }

  public void setToolTipText(String toolTip) {
    comboBox.setTooltip(new Tooltip(toolTip));
  }
}
