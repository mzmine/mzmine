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
import javafx.scene.layout.BorderPane;

public class FeatureListsComponent extends BorderPane {


  private final ComboBox<FeatureListsSelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numPeakListsLabel;
  private FeatureListsSelection currentValue = new FeatureListsSelection();

  public FeatureListsComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

    numPeakListsLabel = new Label();
    setLeft(numPeakListsLabel);

    detailsButton = new Button("Select");
    detailsButton.setDisable(true);
    setRight(detailsButton);

    typeCombo = new ComboBox<>(FXCollections.observableArrayList(FeatureListsSelectionType.values()));
    typeCombo.getSelectionModel().selectFirst();

    typeCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
      currentValue.setSelectionType(newValue);
      detailsButton.setDisable((newValue != FeatureListsSelectionType.NAME_PATTERN)
          && (newValue != FeatureListsSelectionType.SPECIFIC_FEATURELISTS));
      updateNumPeakLists();
    });

    setCenter(typeCombo);

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
          FeatureList pls[] = paramSet.getParameter(plsParameter).getValue();
          currentValue.setSpecificFeatureLists(pls);
        }

      }

      if (type == FeatureListsSelectionType.NAME_PATTERN) {
        final StringParameter nameParameter = new StringParameter("Name pattern",
            "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse",
            currentValue.getNamePattern());
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[] {nameParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          String namePattern = paramSet.getParameter(nameParameter).getValue();
          currentValue.setNamePattern(namePattern);
        }

      }
      updateNumPeakLists();
    });


  }

  void setValue(FeatureListsSelection newValue) {
    currentValue = newValue.clone();
    FeatureListsSelectionType type = newValue.getSelectionType();
    if (type != null)
      typeCombo.getSelectionModel().select(type);
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
      FeatureList pls[] = currentValue.getMatchingFeatureLists();
      if (pls.length == 1) {
        String plName = pls[0].getName();
        if (plName.length() > 22)
          plName = plName.substring(0, 20) + "...";
        numPeakListsLabel.setText(plName);
      } else {
        numPeakListsLabel.setText(pls.length + " selected");
      }
      numPeakListsLabel.setTooltip(new Tooltip(currentValue.toString()));
    }
  }
}
