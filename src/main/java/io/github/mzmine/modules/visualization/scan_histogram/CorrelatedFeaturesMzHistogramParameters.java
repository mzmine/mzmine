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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.DecimalFormat;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class CorrelatedFeaturesMzHistogramParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv") //
  );

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final MZRangeParameter mzRange = new MZRangeParameter(true);
  public static final OptionalParameter<RTRangeParameter> rtRange = new OptionalParameter<>(
      new RTRangeParameter(false));
  public static final DoubleParameter binWidth = new DoubleParameter("m/z bin width",
      "Binning of m/z values for feature picking ", MZmineCore.getConfiguration().getMZFormat(),
      0.001);

  public static final DoubleParameter minCorr = new DoubleParameter("Minimum Pearson correlation",
      "Minimum Pearson correlation of feature shapes ", new DecimalFormat("0.000"), 0.85);

  public static final BooleanParameter limitToDoubleMz = new BooleanParameter("Limit delta to m/z",
      "Maximum m/z delta is the m/z of the smaller ion (feature list row)", true);

  public static final OptionalParameter<FileNameParameter> saveToFile = new OptionalParameter<>(
      new FileNameParameter("Append to file",
          "Append the correlated features delta m/z to a csv file", extensions,
          FileSelectionType.SAVE), false);

  public CorrelatedFeaturesMzHistogramParameters() {
    super(new Parameter[]{featureLists, mzRange, rtRange, minCorr, limitToDoubleMz, binWidth,
        saveToFile});
  }

}
