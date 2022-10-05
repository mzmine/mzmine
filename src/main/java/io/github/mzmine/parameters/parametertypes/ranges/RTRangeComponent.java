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
