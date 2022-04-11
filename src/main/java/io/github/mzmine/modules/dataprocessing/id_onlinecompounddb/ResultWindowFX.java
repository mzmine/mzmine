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


package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;


import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.taskcontrol.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class ResultWindowFX extends Stage {

  public ResultWindowController controller;

  public ResultWindowFX(FeatureListRow peakListRow, double searchedMass, Task searchTask) {

    try {

      FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultWindow.fxml"));
      Scene rootScene = loader.load();
      setScene(rootScene);
      setMinWidth(700);
      setMinHeight(550);
      controller = loader.getController();
      controller.initValues(peakListRow, searchTask, searchedMass);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void addNewListItem(final CompoundDBAnnotation compound) {
    controller.addNewListItem(compound);
  }

}
