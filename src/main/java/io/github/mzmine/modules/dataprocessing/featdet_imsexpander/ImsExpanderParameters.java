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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class ImsExpanderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final OptionalParameter<MZToleranceParameter> mzTolerance = new OptionalParameter<>(
      new MZToleranceParameter("m/z tolerance",
          "m/z tolerance for peaks in the mobility dimension. If enabled, the given "
              + "tolerance will be applied to the feature m/z. If disabled, the m/z range of the "
              + "feature's data points will be used as a tolerance range."));

  public static final OptionalParameter<DoubleParameter> useRawData = new OptionalParameter<>(
      new DoubleParameter("Raw data instead of thresholded",
          "If checked, the raw data can be used to expand the chromatograms into mobility dimension.\n"
              + "This can increase sensitivity but will also increase RAM demands and computation time.\n"
              + "A new noise level can be given or every data point can be used (0E0)",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E1, 0d, Double.POSITIVE_INFINITY),
      true);

  public static final OptionalParameter<IntegerParameter> mobilogramBinWidth = new OptionalParameter<>(
      new IntegerParameter("Override default mobility bin witdh (scans)",
          "If checked, the default recommended bin width for the raw data file will be overridden with the given value.\n"
              + "The mobility binning width in scans. (high mobility resolutions "
              + "in TIMS might require a higher bin width to achieve a constant ion current for a "
              + "mobilogram.", 1, true), false);


  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public ImsExpanderParameters() {
    super(
        new Parameter[]{featureLists, mzTolerance, useRawData, mobilogramBinWidth, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ims_expander/ims-expander.html");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    final boolean superCheck = super.checkParameterValues(errorMessages);
    if (!superCheck) {
      return false;
    }

    ModularFeatureList[] matchingFeatureLists = getParameter(featureLists).getValue()
        .getMatchingFeatureLists();

    for (ModularFeatureList flist : matchingFeatureLists) {
      if (flist.getNumberOfRawDataFiles() > 1) {
        errorMessages.add("Feature list " + flist.getName()
            + " is an aligned feature list. Please expand before alignment.");
      }

      if (((IMSRawDataFile) flist.getRawDataFile(0)).getFrame(0).getMobilityScan(0)
          .getSpectrumType() != MassSpectrumType.CENTROIDED
          && getParameter(useRawData).getValue() == true) {
        errorMessages.add(
            "Feature list " + flist.getName() + " contains raw data file " + flist.getRawDataFile(0)
                + " which has profile raw data.\nCannot use profile raw data to expand in mobility dimension. Please disable the \""
                + useRawData.getName() + "\" parameter.");
      }
    }

    return errorMessages.isEmpty();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
