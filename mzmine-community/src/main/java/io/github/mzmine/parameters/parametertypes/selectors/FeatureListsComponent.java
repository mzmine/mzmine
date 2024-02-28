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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.Nullable;

public class FeatureListsComponent extends HBox {


  private final ComboBox<FeatureListsSelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numPeakListsLabel;
  private FeatureListsSelection currentValue = new FeatureListsSelection();

  public FeatureListsComponent() {
    setSpacing(5);

    numPeakListsLabel = new Label();

    detailsButton = new Button("Select");
    detailsButton.setDisable(true);

    typeCombo = new ComboBox<>(
        FXCollections.observableArrayList(FeatureListsSelectionType.values()));
    typeCombo.getSelectionModel().selectFirst();

    typeCombo.getSelectionModel().selectedItemProperty()
        .addListener((options, oldValue, newValue) -> {
          currentValue.setSelectionType(newValue);
          detailsButton.setDisable((newValue != FeatureListsSelectionType.NAME_PATTERN) && (newValue
              != FeatureListsSelectionType.SPECIFIC_FEATURELISTS));
          updateNumPeakLists();
        });

    getChildren().addAll(numPeakListsLabel, typeCombo, detailsButton);

    detailsButton.setOnAction(e -> {
      FeatureListsSelectionType type = typeCombo.getSelectionModel().getSelectedItem();

      if (type == FeatureListsSelectionType.SPECIFIC_FEATURELISTS) {
        final MultiChoiceParameter<FeatureList> plsParameter = new MultiChoiceParameter<FeatureList>(
            "Select feature lists", "Select feature lists",
            MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()
                .toArray(FeatureList[]::new), currentValue.getSpecificFeatureLists());
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[]{plsParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          FeatureList[] pls = paramSet.getParameter(plsParameter).getValue();
          currentValue.setSpecificFeatureLists(pls);
        }

      }

      if (type == FeatureListsSelectionType.NAME_PATTERN) {
        final StringParameter nameParameter = new StringParameter("Name pattern",
            "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse",
            currentValue.getNamePattern());
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[]{nameParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          String namePattern = paramSet.getParameter(nameParameter).getValue();
          currentValue.setNamePattern(namePattern);
        }

      }
      updateNumPeakLists();
    });

  }

  void setValue(@Nullable FeatureListsSelection newValue) {
    currentValue = newValue!=null ? newValue.clone() : null;
    if (newValue!=null && newValue.getSelectionType()!= null) {
      typeCombo.getSelectionModel().select(newValue.getSelectionType());
    }
    updateNumPeakLists();
  }

  FeatureListsSelection getValue() {
    return currentValue;
  }


  public void setToolTipText(String toolTip) {
    typeCombo.setTooltip(new Tooltip(toolTip));
  }

  private void updateNumPeakLists() {
    if (currentValue.getSelectionType() == FeatureListsSelectionType.BATCH_LAST_FEATURELISTS) {
      numPeakListsLabel.setText("");
      numPeakListsLabel.setTooltip(null);
    } else {
      FeatureList[] pls = currentValue.getMatchingFeatureLists();
      if (pls.length == 1) {
        String plName = pls[0].getName();
        if (plName.length() > 22) {
          plName = plName.substring(0, 20) + "...";
        }
        numPeakListsLabel.setText(plName);
      } else {
        numPeakListsLabel.setText(pls.length + " selected");
      }
      numPeakListsLabel.setTooltip(new Tooltip(currentValue.toString()));
    }
  }
}
