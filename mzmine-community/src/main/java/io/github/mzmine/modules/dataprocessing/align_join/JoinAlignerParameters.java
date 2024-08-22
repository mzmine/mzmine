/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_join;

import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class JoinAlignerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter peakListName = new StringParameter("Feature list name",
      "Feature list name", "Aligned feature list");

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter(
      ToleranceType.SAMPLE_TO_SAMPLE);

  public static final DoubleParameter MZWeight = new DoubleParameter("Weight for m/z",
      "Score for perfectly matching m/z values");

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

  public static final DoubleParameter RTWeight = new DoubleParameter("Weight for RT",
      "Score for perfectly matching RT values");

  public static final OptionalParameter<MobilityToleranceParameter> mobilityTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter("Mobility tolerance",
          "If checked, mobility of features will be compared for alignment. "
              + "\nThis parameter then specifies the tolerance range for matching mobility values"),
      false);

  public static final DoubleParameter mobilityWeight = new DoubleParameter("Mobility weight",
      "Score for perfectly matching mobility values. Only taken into account if \"Mobility tolerance\" is activated.",
      new DecimalFormat("0.000"), 1d);

  public static final BooleanParameter SameChargeRequired = new BooleanParameter(
      "Require same charge state", "If checked, only rows having same charge state can be aligned",
      false);

  public static final BooleanParameter SameIDRequired = new BooleanParameter("Require same ID",
      "If checked, only rows having same compound identities (or no identities) can be aligned",
      false);

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> compareIsotopePattern = new OptionalModuleParameter<>(
      "Compare isotope pattern",
      "If both peaks represent an isotope pattern, add isotope pattern score to match score",
      new IsotopePatternScoreParameters(), false);

  public static final OptionalModuleParameter<JoinAlignerSpectraSimilarityScoreParameters> compareSpectraSimilarity = new OptionalModuleParameter<>(
      "Compare spectra similarity", "Compare MS1 or MS2 spectra similarity",
      new JoinAlignerSpectraSimilarityScoreParameters(), false);


  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      "Original feature list",
      "Defines the processing.\nKEEP is to keep the original feature list and create a new"
          + "processed list.\nREMOVE saves memory.", false);

  public JoinAlignerParameters() {
    super(new Parameter[]{peakLists, peakListName, MZTolerance, MZWeight, RTTolerance, RTWeight,
            mobilityTolerance, mobilityWeight, SameChargeRequired, SameIDRequired,
            compareIsotopePattern, compareSpectraSimilarity, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/align_join_aligner/join_aligner.html");
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    boolean state = super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);

    var mzWeight = getValue(MZWeight);
    var rtWeight = getValue(RTWeight);
    if(mzWeight==0 && rtWeight==0) {
      errorMessages.add("Cannot align with both rt and mz weight 0");
    }
    return state;
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", MZTolerance);
    return nameParameterMap;
  }
}
