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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.datamodel.Mobilogram;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.mobilogram.MobilogramVisualizerController;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public abstract class ParameterSetupDialogWithMobilogramPreview extends ParameterSetupDialog {

  protected final CheckBox cbShowPreview;
  protected MobilogramVisualizerController controller;
  protected AnchorPane visualiserPane;

  public ParameterSetupDialogWithMobilogramPreview(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    FXMLLoader loader =
        new FXMLLoader((getClass().getResource("../../visualization"
            + "/mobilogram/MobilogramVisualizerPane.fxml")));

    try {
      visualiserPane = loader.load();
      controller = loader.getController();
      controller.setRawDataFiles(MZmineCore.getProjectManager().getCurrentProject()
          .getRawDataFiles());
      mainPane.setRight(visualiserPane);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not load MobilogramVisualizerPane.fxml", e);
    }

    cbShowPreview = new CheckBox();
    cbShowPreview.selectedProperty().bindBidirectional(visualiserPane.visibleProperty());

    paramsPane.add(new Label("Show preview"), 0, getNumberOfParameters() + 1);
    paramsPane.add(cbShowPreview, 1, getNumberOfParameters() + 1);

    controller.addMobilogramSelectionListener(
        newMobilogram -> onMobilogramSelectionChanged(newMobilogram));
  }

  public abstract void onMobilogramSelectionChanged(Mobilogram newMobilogram);
}
