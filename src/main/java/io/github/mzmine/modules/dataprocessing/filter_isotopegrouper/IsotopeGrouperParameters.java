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

package io.github.mzmine.modules.dataprocessing.filter_isotopegrouper;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
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

public class IsotopeGrouperParameters extends SimpleParameterSet {

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

  public static final BooleanParameter monotonicShape = new BooleanParameter("Monotonic shape",
      "If true, then monotonically decreasing height of isotope pattern is required");


  public static final BooleanParameter keepAllMS2 = new BooleanParameter(
      "Never remove feature with MS2",
      "If checked, all rows with MS2 are retained without applying any further filters on them.",
      true);

  public static final IntegerParameter maximumCharge = new IntegerParameter("Maximum charge",
      "Maximum charge to consider for detecting the isotope patterns");

  public static final ComboParameter<String> representativeIsotope = new ComboParameter<String>(
      "Representative isotope",
      "Which peak should represent the whole isotope pattern. For small molecular weight\n"
          + "compounds with monotonically decreasing isotope pattern, the most intense isotope\n"
          + "should be representative. For high molecular weight peptides, the lowest m/z\n"
          + "peptides, the lowest m/z isotope may be the representative.",
      representativeIsotopeValues);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public IsotopeGrouperParameters() {
    super(new Parameter[]{peakLists, suffix, mzTolerance, rtTolerance, mobilityTolerace,
            monotonicShape, maximumCharge, representativeIsotope, keepAllMS2, handleOriginal},
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
