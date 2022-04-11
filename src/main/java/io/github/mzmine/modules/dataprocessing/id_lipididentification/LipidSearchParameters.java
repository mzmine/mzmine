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

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.AllLipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClassParameter;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClassChoiceParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

/**
 * Parameters for lipid annotation module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final LipidClassParameter<Object> lipidClasses = new LipidClassParameter<>(
      "Lipid classes", "Selection of lipid backbones", AllLipidClasses.getList().toArray());

  public static final IntRangeParameter chainLength =
      new IntRangeParameter("Number of carbon atoms in chains", "Number of carbon atoms in chains");

  public static final IntRangeParameter doubleBonds =
      new IntRangeParameter("Number of double bonds in chains", "Number of double bonds in chains");

  public static final MZToleranceParameter mzTolerance =
      new MZToleranceParameter("m/z tolerance MS1 level:",
          "Enter m/z tolerance for exact mass database matching on MS1 level");

  public static final OptionalModuleParameter<LipidSearchMSMSParameters> searchForMSMSFragments =
      new OptionalModuleParameter<>("Search for lipid class specific fragments in MS/MS spectra",
          "Search for lipid class specific fragments in MS/MS spectra",
          new LipidSearchMSMSParameters());

  public static final OptionalParameter<CustomLipidClassChoiceParameter> customLipidClasses =
      new OptionalParameter<>(new CustomLipidClassChoiceParameter("Search for custom lipid class",
          "If checked the algorithm searches for custom, by the user defined lipid classes",
          new CustomLipidClass[0]));

  public LipidSearchParameters() {
    super(new Parameter[] {featureLists, lipidClasses, chainLength, doubleBonds, mzTolerance,
        searchForMSMSFragments, customLipidClasses});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    LipidSearchParameterSetupDialog dialog =
        new LipidSearchParameterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
