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

package io.github.mzmine.modules.visualization.combinedModule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.neutralloss.NeutralLossParameters;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CombinedModuleVisualizerWindow extends Stage {


  private RawDataFile dataFile;

  public CombinedModuleVisualizerWindow(ParameterSet parameters) {

    Range<Double> rtRange =
        parameters.getParameter(CombinedModuleParameters.retentionTimeRange).getValue();
    Range<Double> mzRange = parameters.getParameter(CombinedModuleParameters.mzRange).getValue();
    Object xAxisType = parameters.getParameter(NeutralLossParameters.xAxisType).getValue();

    dataFile = parameters.getParameter(CombinedModuleParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];

    try {
      FXMLLoader root = new FXMLLoader(
          getClass().getResource("CombinedModuleVisualizerWindow.fxml"));
      Parent rootPane = root.load();
      Scene scene = new Scene(rootPane);
      setScene(scene);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
