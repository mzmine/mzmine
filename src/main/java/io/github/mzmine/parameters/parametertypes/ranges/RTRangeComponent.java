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

package io.github.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class RTRangeComponent extends DoubleRangeComponent {

  private final Button setAutoButton;

  public RTRangeComponent() {

    super(MZmineCore.getConfiguration().getRTFormat());

    // setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    add(new Label("min."), 3, 0);

    setAutoButton = new Button("Auto range");
    setAutoButton.setOnAction(e -> {
      RawDataFile currentFiles[] =
          MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

      try {
        ParameterSetupDialog setupDialog = (ParameterSetupDialog) this.getScene().getWindow();

        RawDataFilesComponent rdc = (RawDataFilesComponent) setupDialog
            .getComponentForParameter(new RawDataFilesParameter());

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
        Range<Double> fileRange = file.getDataRTRange();
        if (rtRange == null)
          rtRange = fileRange;
        else
          rtRange = rtRange.span(fileRange);
      }
      setValue(rtRange);
    });
    RawDataFile currentFiles[] = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    setAutoButton.setDisable(currentFiles.length == 0);
    add(setAutoButton, 4, 0);
  }

}
