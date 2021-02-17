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

package io.github.mzmine.modules.visualization.mzhistogram;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class MZDistributionHistoParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(1));
  public static final MZRangeParameter mzRange = new MZRangeParameter(true);
  public static final OptionalParameter<RTRangeParameter> rtRange =
      new OptionalParameter<>(new RTRangeParameter(false));
  public static final BooleanParameter useMobilityScans = new BooleanParameter("Use mobility scans",
      "If the file contains an ion mobility dimension, the data from "
          + "mobility scans will be used instead of the data from summed frames.", false);
  public static final DoubleParameter binWidth = new DoubleParameter("m/z bin width",
      "Binning of m/z values for feature picking ", MZmineCore.getConfiguration().getMZFormat());

  public MZDistributionHistoParameters() {
    super(new Parameter[]{dataFiles, scanSelection, mzRange, rtRange, binWidth,
        useMobilityScans});
  }

  public enum Weight {
    None, Linear, log10;
  }

}
