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
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeMassCalculatorModule;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionComponent;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.project.impl.ProjectChangeEvent;
import io.github.mzmine.project.impl.ProjectChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;

public class MZRangeComponent extends DoubleRangeComponent {

  private final Button setAutoButton, fromMassButton, fromFormulaButton;

  public MZRangeComponent() {

    super(MZmineCore.getConfiguration().getMZFormat());

    setAutoButton = new Button("Auto range");
    setAutoButton.setMinWidth(100.0);
    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    setAutoButton.setOnAction(e -> {
      RawDataFile currentFiles[] = project.getDataFiles();
      ScanSelection scanSelection = new ScanSelection();

      try {
        ParameterSetupDialog setupDialog = (ParameterSetupDialog) this.getScene().getWindow();
        RawDataFilesComponent rdc =
            setupDialog.getComponentForParameter(new RawDataFilesParameter());
        if (rdc != null) {
          RawDataFile matchingFiles[] = rdc.getValue().getMatchingRawDataFiles();
          if (matchingFiles.length > 0)
            currentFiles = matchingFiles;
        }
        ScanSelectionComponent ssc =
            setupDialog.getComponentForParameter(new ScanSelectionParameter());
        if (ssc != null)
          scanSelection = ssc.getValue();
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      Range<Double> mzRange = null;
      for (RawDataFile file : currentFiles) {
        Scan scans[] = scanSelection.getMatchingScans(file);
        for (Scan s : scans) {
          Range<Double> scanRange = s.getDataPointMZRange();
          if (scanRange == null) {
            continue;
          }
          if (mzRange == null) {
            mzRange = scanRange;
          } else {
            mzRange = mzRange.span(scanRange);
          }
        }
      }
      if (mzRange != null) {
        setValue(mzRange);
      }
    });

    project.addProjectListener(new ProjectChangeListener() {
      @Override
      public void dataFilesChanged(ProjectChangeEvent<RawDataFile> event) {
        setAutoButton.setDisable(project.getNumberOfDataFiles() == 0);
      }
    });

    fromMassButton = new Button("From mass");
    fromMassButton.setMinWidth(100.0);
    fromMassButton.setOnAction(e -> {
      Range<Double> mzRange = MzRangeMassCalculatorModule.showRangeCalculationDialog();
      if (mzRange != null) {
        setValue(mzRange);
      }
    });

    fromFormulaButton = new Button("From formula");
    fromFormulaButton.setMinWidth(100.0);
    fromFormulaButton.setOnAction(e -> {
      Range<Double> mzRange = MzRangeFormulaCalculatorModule.showRangeCalculationDialog();
      if (mzRange != null)
        setValue(mzRange);
    });

    // fromFormulaButton.setMinWidth(fromFormulaButton.getPrefWidth());
    getChildren().addAll(setAutoButton, fromMassButton, fromFormulaButton);
    super.setAlignment(Pos.BASELINE_LEFT);
  }

}
