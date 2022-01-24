/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class AddIonNetworkingParameters extends SimpleParameterSet {

  // different depth of settings
  public enum Setup {
    FULL, SUB, SIMPLE;
  }

  // NOT INCLUDED in sub
  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between peaks");

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height of feature shape (not used for average mode)",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final SubModuleParameter<IonLibraryParameterSet> LIBRARY =
      new SubModuleParameter<>("Ion identity library", "Adducts, in-source fragments and multimers",
          new IonLibraryParameterSet());

  // MS MS
  // check for truth MS/MS
  // public static final OptionalModuleParameter<IonNetworkMSMSCheckParameters> MSMS_CHECK =
  // new OptionalModuleParameter<IonNetworkMSMSCheckParameters>("Check MS/MS",
  // "Check MS/MS for truth of multimers", new IonNetworkMSMSCheckParameters(true));

  public static final OptionalModuleParameter<IonNetworkRefinementParameters> ANNOTATION_REFINEMENTS =
      new OptionalModuleParameter<IonNetworkRefinementParameters>("Annotation refinement", "",
          new IonNetworkRefinementParameters(true), true);

  // setup
  private Setup setup;

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
            LIBRARY};
      case SUB:
        return new Parameter[]{MZ_TOLERANCE, ANNOTATION_REFINEMENTS};
      case SIMPLE:
        return new Parameter[]{LIBRARY};
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
}
