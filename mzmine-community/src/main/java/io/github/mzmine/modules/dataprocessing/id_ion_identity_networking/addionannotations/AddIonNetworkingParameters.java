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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations;


import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class AddIonNetworkingParameters extends SimpleParameterSet {

  // different depth of settings
  public enum Setup {
    FULL, SUB
  }

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      ToleranceType.INTRA_SAMPLE);

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height of feature shape (not used for average mode)",
      MZmineCore.getConfiguration().getIntensityFormat());


  public static final IonLibraryParameter fullIonLibrary = new IonLibraryParameter("Ion library",
      """
          The full ion library contains all adducts, in source fragments, and other ions to be searched.
          See ion network refinement to require main adducts in networks.""",
      IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_FULL);

  // MS MS
  // check for truth MS/MS
  // public static final OptionalModuleParameter<IonNetworkMSMSCheckParameters> MSMS_CHECK =
  // new OptionalModuleParameter<IonNetworkMSMSCheckParameters>("Check MS/MS",
  // "Check MS/MS for truth of multimers", new IonNetworkMSMSCheckParameters(true));

  public static final OptionalModuleParameter<IonNetworkRefinementParameters> ANNOTATION_REFINEMENTS = new OptionalModuleParameter<IonNetworkRefinementParameters>(
      "Annotation refinement", "", new IonNetworkRefinementParameters(true), true);

  // setup
  private final Setup setup;

  // Constructor
  public AddIonNetworkingParameters() {
    this(Setup.FULL);
  }

  public AddIonNetworkingParameters(Setup setup) {
    super(createParam(setup));
    this.setup = setup;
  }

  private static Parameter[] createParam(Setup setup) {
    switch (setup) {
      case FULL:
        return new Parameter[]{PEAK_LISTS, MZ_TOLERANCE, MIN_HEIGHT, ANNOTATION_REFINEMENTS,
            fullIonLibrary};
      case SUB:
        return new Parameter[]{MZ_TOLERANCE, ANNOTATION_REFINEMENTS};
    }
    return new Parameter[0];
  }

  /**
   * Create full set of parameters
   */
  public static AddIonNetworkingParameters createFullParamSet(AddIonNetworkingParameters param,
      double minHeight) {
    return createFullParamSet(param, null, minHeight);
  }

  /**
   * Create full set of parameters
   */
  public static AddIonNetworkingParameters createFullParamSet(AddIonNetworkingParameters param,
      MZTolerance mzTol, double minHeight) {
    AddIonNetworkingParameters full = new AddIonNetworkingParameters();
    for (Parameter p : param.getParameters()) {
      full.getParameter(p).setValue(p.getValue());
    }
    if (mzTol != null) {
      full.getParameter(AddIonNetworkingParameters.MZ_TOLERANCE).setValue(mzTol);
    }

    full.getParameter(AddIonNetworkingParameters.MIN_HEIGHT).setValue(minHeight);
    return full;
  }

  /**
   * The setup mode
   *
   * @return
   */
  public Setup getSetup() {
    return setup;
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
    nameParameterMap.put("m/z tolerance", getParameter(MZ_TOLERANCE));
    // {@link LegacyIonLibraryParameterSet} is loaded directly by the new library parameter
    nameParameterMap.put("Ion identity library", fullIonLibrary);
    return nameParameterMap;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    if (loadedVersion == 1) {
      getParameter(ANNOTATION_REFINEMENTS).getEmbeddedParameters()
          .getParameter(IonNetworkRefinementParameters.mainIonLibrary).getEmbeddedParameter()
          .setValue(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_MAIN);
    }
  }

  @Override
  public int getVersion() {
    return 2;
  }

}
