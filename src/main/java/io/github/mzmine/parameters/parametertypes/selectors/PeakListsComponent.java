/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.PeakList;
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

public class PeakListsComponent extends BorderPane {


  private final ComboBox<PeakListsSelectionType> typeCombo;
  private final Button detailsButton;
  private final Label numPeakListsLabel;
  private PeakListsSelection currentValue = new PeakListsSelection();

  public PeakListsComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

    numPeakListsLabel = new Label();
    setLeft(numPeakListsLabel);

    detailsButton = new Button("...");
    detailsButton.setDisable(true);
    setRight(detailsButton);

    typeCombo = new ComboBox<>(FXCollections.observableArrayList(PeakListsSelectionType.values()));
    typeCombo.setOnAction(e -> {
      PeakListsSelectionType type = typeCombo.getSelectionModel().getSelectedItem();
      currentValue.setSelectionType(type);
      detailsButton.setDisable((type != PeakListsSelectionType.NAME_PATTERN)
          && (type != PeakListsSelectionType.SPECIFIC_PEAKLISTS));
      updateNumPeakLists();
    });
    setCenter(typeCombo);

    detailsButton.setOnAction(e -> {
      PeakListsSelectionType type = typeCombo.getSelectionModel().getSelectedItem();

      if (type == PeakListsSelectionType.SPECIFIC_PEAKLISTS) {
        final MultiChoiceParameter<PeakList> plsParameter =
            new MultiChoiceParameter<PeakList>("Select feature lists", "Select feature lists",
                MZmineCore.getProjectManager().getCurrentProject().getPeakLists(),
                currentValue.getSpecificPeakLists());
        final SimpleParameterSet paramSet = new SimpleParameterSet(new Parameter[] {plsParameter});
        final ExitCode exitCode = paramSet.showSetupDialog(true);
        if (exitCode == ExitCode.OK) {
          PeakList pls[] = paramSet.getParameter(plsParameter).getValue();
          currentValue.setSpecificPeakLists(pls);
        }

      }

      if (type == PeakListsSelectionType.NAME_PATTERN) {
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

  void setValue(PeakListsSelection newValue) {
    currentValue = newValue.clone();
    PeakListsSelectionType type = newValue.getSelectionType();
    if (type != null)
      typeCombo.getSelectionModel().select(type);
    updateNumPeakLists();
  }

  PeakListsSelection getValue() {
    return currentValue;
  }



  public void setToolTipText(String toolTip) {
    typeCombo.setTooltip(new Tooltip(toolTip));
  }

  private void updateNumPeakLists() {
    if (currentValue.getSelectionType() == PeakListsSelectionType.BATCH_LAST_PEAKLISTS) {
      numPeakListsLabel.setText("");
      numPeakListsLabel.setTooltip(null);
    } else {
      PeakList pls[] = currentValue.getMatchingPeakLists();
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
