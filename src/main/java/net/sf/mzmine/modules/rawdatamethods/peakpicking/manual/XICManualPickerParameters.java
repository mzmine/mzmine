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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.manual;

import java.awt.Window;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithChromatogramPreview;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.HiddenParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import net.sf.mzmine.util.ExitCode;

public class XICManualPickerParameters extends SimpleParameterSet {

  public static final HiddenParameter<RawDataFilesParameter, RawDataFilesSelection> rawDataFiles =
      new HiddenParameter<RawDataFilesParameter, RawDataFilesSelection>(
          new RawDataFilesParameter("Raw data file", 1, 100));

  public static final HiddenParameter<DoubleRangeParameter, Range<Double>> rtRange =
      new HiddenParameter<DoubleRangeParameter, Range<Double>>(new DoubleRangeParameter(
          "Retention time", "Retention time range", MZmineCore.getConfiguration().getRTFormat()));

  public static final HiddenParameter<DoubleRangeParameter, Range<Double>> mzRange =
      new HiddenParameter<DoubleRangeParameter, Range<Double>>(new DoubleRangeParameter("m/z range",
          "m/z range", MZmineCore.getConfiguration().getMZFormat()));

  public XICManualPickerParameters() {
    super(new Parameter[] {rawDataFiles, rtRange, mzRange});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    ParameterSetupDialog dialog =
        new XICManualPickerDialog(MZmineCore.getDesktop().getMainWindow(), true, this);
    dialog.setVisible(true);

    return dialog.getExitCode();
  }
}
