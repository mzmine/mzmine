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

package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class IsotopeNaturalAbundanceParameters extends SimpleParameterSet {

  public static final String ChooseTopIntensity = "Most intense";
  public static final String ChooseLowestMZ = "Lowest m/z";

  public static final String[] representativeIsotopeValues = {ChooseTopIntensity, ChooseLowestMZ};

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "deisotoped");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      ToleranceType.INTRA_SAMPLE);

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final OptionalParameter<MobilityToleranceParameter> mobilityTolerace = new OptionalParameter<>(
      new MobilityToleranceParameter("Mobility tolerance",
          "If enabled (and mobility dimension was recorded), "
              + "isotopic peaks will only be grouped if they fit within the given tolerance."));

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public IsotopeNaturalAbundanceParameters() {
    super(new Parameter[]{peakLists, suffix, mzTolerance, rtTolerance, mobilityTolerace,
            handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_isotope_filter/isotope_filter.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", mzTolerance);
    return nameParameterMap;
  }
}
