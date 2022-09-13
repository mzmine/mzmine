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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class IMSRawDataOverviewParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter(1, 1);

  public static final DoubleParameter summedFrameNoiseLevel = new DoubleParameter("Frame noise"
      + " level", IMSRawDataOverviewControlPanel.TOOLTIP_FRAME_NL,
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final DoubleParameter mobilityScanNoiseLevel = new DoubleParameter("Mobility scan "
      + "noise level", IMSRawDataOverviewControlPanel.TOOLTIP_MOBILITYSCAN_NL,
      MZmineCore.getConfiguration().getIntensityFormat(), 5E2);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance"
      , IMSRawDataOverviewControlPanel.TOOLTIP_MZTOL, 0.008, 10);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter("Scan "
      + "selection", IMSRawDataOverviewControlPanel.TOOLTIP_SCANSEL, new ScanSelection(1));

  public static final DoubleParameter rtWidth = new DoubleParameter("Retention time width",
      IMSRawDataOverviewControlPanel.TOOLTIP_RTRANGE, MZmineCore.getConfiguration().getRTFormat()
      , 2d);

  public static final IntegerParameter binWidth = new IntegerParameter("Mobilogram bin width (scans)",
      IMSRawDataOverviewControlPanel.TOOLTIP_BINWIDTH, 1);

  public IMSRawDataOverviewParameters() {
    super(new Parameter[]{rawDataFiles, summedFrameNoiseLevel, mobilityScanNoiseLevel,
        mzTolerance, scanSelection, rtWidth, binWidth},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/ims_raw_data_overview/IM-data-visualisation.html");
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
