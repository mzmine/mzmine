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

package io.github.mzmine.parameters.parametertypes.submodules;

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

  private final ComboBox<MZmineProcessingStep<?>> comboBox;
  private Button setButton;

  public ModuleComboComponent(MZmineProcessingStep<?>[] modules) {

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
      if (selected == null) {
        return;
      }
      ParameterSetupDialog dialog = (ParameterSetupDialog) getScene().getWindow();
      if (dialog == null) {
        return;
      }
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
    if (selected == null) {
      comboBox.getSelectionModel().clearSelection();
      return;
    }
    comboBox.getSelectionModel().select(selected);
  }

  public void setToolTipText(String toolTip) {
    comboBox.setTooltip(new Tooltip(toolTip));
  }
}
