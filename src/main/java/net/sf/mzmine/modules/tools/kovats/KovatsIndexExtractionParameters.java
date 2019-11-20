/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.tools.kovats;

import java.awt.Window;
import java.text.DecimalFormat;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.kovats.KovatsValues.KovatsIndex;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.util.DialogLoggerUtil;
import net.sf.mzmine.util.ExitCode;

/**
 * Calc Kovats retention idex and save to file (also for GNPS GC-MS workflow)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class KovatsIndexExtractionParameters extends SimpleParameterSet {

  // last saved file
  public static final FileNameParameter lastSavedFile =
      new FileNameParameter("Last file", "Last saved file", "csv");

  public static final StringParameter pickedKovatsValues =
      new StringParameter("Picked Kovats values", "The picked values as C10:time,C12:time ... ");
  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1, 2);
  public static final DoubleParameter noiseLevel =
      new DoubleParameter("Min intensity", "Minimum intensity to recognice a peak",
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
        // picking of peaks
        dataFiles, mzRange, rtRange, noiseLevel, ratioTopEdge,
        // kovats selection
        minKovats, maxKovats, kovats //
    });
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;


    // at least one raw data file in project
    RawDataFile[] raw = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    if (raw == null || raw.length <= 0) {
      DialogLoggerUtil.showMessageDialogForTime(MZmineCore.getDesktop().getMainWindow(),
          "No RAW data files",
          "Cannot use Kovats extraction without raw data files in this project", 3500);
      return ExitCode.ERROR;
    }

    // set choices of kovats to min max
    int min = getParameter(minKovats).getValue();
    int max = getParameter(maxKovats).getValue();
    getParameter(kovats).setChoices(KovatsIndex.getRange(min, max));

    ParameterSetupDialog dialog = new KovatsIndexExtractionDialog(parent, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
