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

package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class RTRangeComponent extends DoubleRangeComponent {

  private final Button setAutoButton;

  public RTRangeComponent() {

    super(MZmineCore.getConfiguration().getRTFormat());

    setAutoButton = new Button("Auto range");
    setAutoButton.setOnAction(e -> {
      RawDataFile currentFiles[] =
          MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

      try {
        ParameterSetupDialog setupDialog = (ParameterSetupDialog) this.getScene().getWindow();

        RawDataFilesComponent rdc =
            setupDialog.getComponentForParameter(new RawDataFilesParameter());

        // If the current setup dialog has no raw data file selector, it
        // is probably in the parent dialog, so let's check it
        /*
         * if (rdc == null) { setupDialog = (ParameterSetupDialog) setupDialog.getParent(); if
         * (setupDialog != null) { rdc = (RawDataFilesComponent) setupDialog
         * .getComponentForParameter(new RawDataFilesParameter()); } } if (rdc != null) {
         * RawDataFile matchingFiles[] = rdc.getValue().getMatchingRawDataFiles(); if
         * (matchingFiles.length > 0) currentFiles = matchingFiles; }
         */
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      Range<Double> rtRange = null;
      for (RawDataFile file : currentFiles) {
        // TODO: FloatRangeComponent
        Range<Double> fileRange = Range.closed(file.getDataRTRange().lowerEndpoint().doubleValue(), file.getDataRTRange().upperEndpoint().doubleValue());
        if (rtRange == null)
          rtRange = fileRange;
        else
          rtRange = rtRange.span(fileRange);
      }
      setValue(rtRange);
    });
    RawDataFile currentFiles[] = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    setAutoButton.setDisable(currentFiles.length == 0);

    getChildren().addAll(new Label("min."), setAutoButton);
    super.setAlignment(Pos.BASELINE_LEFT);
  }

}
