/*
 * Copyright 2006-2022 The MZmine Development Team
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
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/ms_raw_data_overview/raw_data_additional.md#scan-histogram");
  }

  public enum Weight {
    None, Linear, log10
  }

}
