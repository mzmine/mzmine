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

package io.github.mzmine.modules.tools.kovats;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.kovats.KovatsValues.KovatsIndex;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Calc Kovats retention idex and save to file (also for GNPS GC-MS workflow)
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class KovatsIndexExtractionParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv") //
  );

  // last saved file
  public static final FileNameParameter lastSavedFile =
      new FileNameParameter("Last file", "Last saved file", extensions, FileSelectionType.SAVE);

  public static final StringParameter pickedKovatsValues =
      new StringParameter("Picked Kovats values", "The picked values as C10:time,C12:time ... ");
  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1, 2);
  public static final DoubleParameter noiseLevel =
      new DoubleParameter("Min intensity", "Minimum intensity to recognice a feature",
          MZmineCore.getConfiguration().getIntensityFormat(), 0d);
  public static final DoubleParameter ratioTopEdge = new DoubleParameter("Ratio top/edge",
      "Minimum ratio top/edge (left and right edge)", new DecimalFormat("0.0"), 3d);
  // limit ranges to show EIC
  public static final MZRangeParameter mzRange = new MZRangeParameter();
  public static final RTRangeParameter rtRange = new RTRangeParameter();
  // show min max kovats in list
  public static final IntegerParameter minKovats =
      new IntegerParameter("Min Kovats", "Show Kovats indexes from min", 8, 1, 49);
  public static final IntegerParameter maxKovats =
      new IntegerParameter("Max Kovats", "Show Kovats indexes until max (inclusive)", 24, 2, 50);
  public static final MultiChoiceParameter<KovatsIndex> kovats =
      new MultiChoiceParameter<KovatsIndex>("Kovats", "Choice of Kovats indexes",
          KovatsIndex.values(), null);

  public KovatsIndexExtractionParameters() {
    super(new Parameter[] {lastSavedFile, pickedKovatsValues,
        // picking of features
        dataFiles, mzRange, rtRange, noiseLevel, ratioTopEdge,
        // kovats selection
        minKovats, maxKovats, kovats //
    });
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;

    // at least one raw data file in project
    RawDataFile[] raw = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    if (raw == null || raw.length <= 0) {
      DialogLoggerUtil.showMessageDialogForTime("No RAW data files",
          "Cannot use Kovats extraction without raw data files in this project", 3500);
      return ExitCode.ERROR;
    }

    // set choices of kovats to min max
    int min = getParameter(minKovats).getValue();
    int max = getParameter(maxKovats).getValue();
    getParameter(kovats).setChoices(KovatsIndex.getRange(min, max));

    ParameterSetupDialog dialog = new KovatsIndexExtractionDialog(this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
