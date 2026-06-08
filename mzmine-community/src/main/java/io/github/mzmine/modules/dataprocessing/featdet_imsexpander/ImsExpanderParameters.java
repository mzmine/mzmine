/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImsExpanderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      """
          m/z tolerance for peaks in the mobility dimension. The given tolerance will be applied to the feature m/z.
          Default = 0.005 m/z and 20 ppm.
          """, 0.005, 20);

  public static final OptionalParameter<DoubleParameter> useRawData = new OptionalParameter<>(
      new DoubleParameter("Raw data instead of thresholded",
          "If checked, the raw data can be used to expand the chromatograms into mobility dimension.\n"
          + "This can increase sensitivity but will also increase RAM demands and computation time.\n"
          + "A new noise level can be given or every data point can be used (0E0)",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E1, 0d, Double.POSITIVE_INFINITY),
      true);

  public static final OptionalParameter<IntegerParameter> mobilogramBinWidth = new OptionalParameter<>(
      new IntegerParameter("Override default mobility bin width (scans)",
          "If checked, the default recommended bin width for the raw data file will be overridden with the given value.\n"
          + "The mobility binning width in scans. (high mobility resolutions "
          + "in TIMS might require a higher bin width to achieve a constant ion current for a "
          + "mobilogram.", 1, true), false);

  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public static final OptionalParameter<IntegerParameter> maxNumTraces = new OptionalParameter<>(
      new IntegerParameter("Maximum features per thread", """
          Sets the maximum number of features to be processed per thread.
          For LC-IMS-MS measurements, this is typically not required (deactivate).
          However, it can be beneficial for imaging experiments to reduce the memory consumption during this step.""",
          2_000), false);

  public ImsExpanderParameters() {
    super(new Parameter[]{featureLists, mzTolerance, useRawData, mobilogramBinWidth, maxNumTraces,
            handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ims-ms_featdet/featdet_ims_expander/ims-expander.html");
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

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {

    final Map<String, Parameter<?>> map = super.getNameParameterMap();
    map.put("Override default mobility bin witdh (scans)",
        getParameter(ImsExpanderParameters.mobilogramBinWidth));
    return map;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 ->
          "The previously optional m/z tolerance parameter in the IMS expander is not optional anymore and must be specified now.";
      default -> null;
    };
  }
}
