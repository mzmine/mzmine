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
