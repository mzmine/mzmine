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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import org.jetbrains.annotations.NotNull;

public class FeatureListBlankSubtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter alignedPeakList =
      new FeatureListsParameter("Aligned feature list", 1, 1);

  public static final RawDataFilesParameter blankRawDataFiles =
      new RawDataFilesParameter("Blank/Control raw data files", 1, 100);

  public static final IntegerParameter minBlanks =
      new IntegerParameter("Minimum # of detection in blanks",
          "Specifies in how many of the blank files a peak has to be detected.");

  public static final OptionalParameter<PercentParameter> foldChange =
      new OptionalParameter<>(new PercentParameter("Fold change increase",
          "Specifies a percentage of increase of the intensity of a feature. If the intensity in the list to be"
              + " filtered increases more than the given percentage to the blank, it will not be deleted from "
              + "the feature list.",
          3.0, 1.0, 1E5));
  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix for the new feature list.", "subtracted");

  public FeatureListBlankSubtractionParameters() {
    super(new Parameter[]{alignedPeakList, blankRawDataFiles, minBlanks, foldChange, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_blanksubtraction/filter_blanksubtraction.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
