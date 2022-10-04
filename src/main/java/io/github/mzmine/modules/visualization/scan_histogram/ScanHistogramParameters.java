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

package io.github.mzmine.modules.visualization.scan_histogram;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class ScanHistogramParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));
  public static final MZRangeParameter mzRange = new MZRangeParameter(true);

  public static final OptionalParameter<DoubleRangeParameter> heightRange = new OptionalParameter<>(
      new DoubleRangeParameter("Signal intensity range",
          "Limits signal intensities for better performance.",
          MZmineCore.getConfiguration().getIntensityFormat(), Range.closed(1d, Double.MAX_VALUE)));


  public static final OptionalParameter<MassDefectParameter> massDefect = new OptionalParameter<>(
      new MassDefectParameter("Mass defect",
          "Filters for mass defects of signals. \nValid inputs: 0.314-0.5 or 0.90-0.15",
          MZmineCore.getConfiguration().getMZFormat()));
  public static final BooleanParameter useMobilityScans = new BooleanParameter("Use mobility scans",
      "If the file contains an ion mobility dimension, the data from "
          + "mobility scans will be used instead of the data from summed frames.", false);
  public static final DoubleParameter binWidth = new DoubleParameter("Bin width",
      "Binning of values");

  public static final ComboParameter<ScanHistogramType> type = new ComboParameter<>("Type",
      "Create histogram of this type", ScanHistogramType.values(), ScanHistogramType.MZ);

  public ScanHistogramParameters() {
    super(
        new Parameter[]{dataFiles, scanSelection, mzRange, heightRange, massDefect, type, binWidth,
            useMobilityScans},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/raw_data_overview/raw_data_additional.html#scan-histogram");
  }

  public enum Weight {
    None, Linear, log10
  }

}
