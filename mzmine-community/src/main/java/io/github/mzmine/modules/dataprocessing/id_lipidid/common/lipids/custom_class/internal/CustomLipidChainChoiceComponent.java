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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

class CustomLipidChainChoiceComponent extends BorderPane {

  private final ListView<LipidChainType> checkList = new ListView<>();
  private final FlowPane buttonsPane = new FlowPane(Orientation.HORIZONTAL);
  private final Button addButton = new Button("Add...");
  private final Button removeButton = new Button("Clear");

  public CustomLipidChainChoiceComponent(LipidChainType[] choices) {

    ObservableList<LipidChainType> choicesList = FXCollections.observableArrayList(
        Arrays.asList(choices));
    checkList.setItems(choicesList);
    setCenter(checkList);
    setPrefSize(300, 200);
    setMinWidth(200);
    setMaxHeight(200);
    addButton.setOnAction(e -> {
      final ParameterSet parameters = new AddLipidChainTypeParameters();
      if (parameters.showSetupDialog(true) != ExitCode.OK) {
        return;
      }

      LipidChainType lipidChainType = parameters.getParameter(
          AddLipidChainTypeParameters.lipidChainType).getValue();

      // Add to list of choices
      checkList.getItems().add(lipidChainType);
    });

    removeButton.setTooltip(new Tooltip("Remove all Lipid Chains"));
    removeButton.setOnAction(e -> {
      checkList.getItems().clear();
    });

    buttonsPane.getChildren().addAll(addButton, removeButton);
    setTop(buttonsPane);

  }

  void setValue(List<LipidChainType> checkedItems) {
    checkList.getItems().clear();
    for (LipidChainType chain : checkedItems) {
      checkList.getItems().add(chain);
    }
  }

  public List<LipidChainType> getChoices() {
    return checkList.getItems();
  }

  public List<LipidChainType> getValue() {
    return checkList.getItems();
  }

  /**
   * Represents a fragmentation rule of a custom lipid class. Not intended to be used as part of a
   * module, does not support saving.
   */
  private static class AddLipidChainTypeParameters extends SimpleParameterSet {

    private static final ComboParameter<LipidChainType> lipidChainType = new ComboParameter<>(
        "Select lipid chain type", "Select lipid chain type",
        new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_MONO_HYDROXY_CHAIN,
            LipidChainType.ALKYL_CHAIN, LipidChainType.AMID_CHAIN,
            LipidChainType.AMID_MONO_HYDROXY_CHAIN,
            LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN,
            LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
            LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN});

    public AddLipidChainTypeParameters() {
      super(lipidChainType);
    }
  }
}
