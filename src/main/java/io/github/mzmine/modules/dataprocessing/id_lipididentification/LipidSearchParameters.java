/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.AllLipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClassParameter;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModificationChoiceParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;

/**
 * Parameters for lipid search module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameters extends SimpleParameterSet {

  public static final PeakListsParameter peakLists = new PeakListsParameter();

  public static final LipidClassParameter<Object> lipidClasses = new LipidClassParameter<Object>(
      "Lipid classes", "Selection of lipid backbones", AllLipidClasses.getList().toArray());

  public static final IntRangeParameter chainLength =
      new IntRangeParameter("Number of carbon atoms in chains", "Number of carbon atoms in chains");

  public static final IntRangeParameter doubleBonds =
      new IntRangeParameter("Number of double bonds in chains", "Number of double bonds in chains");

  public static final MZToleranceParameter mzTolerance =
      new MZToleranceParameter("m/z tolerance MS1 level:",
          "Enter m/z tolerance for exact mass database matching on MS1 level");

  public static final ComboParameter<IonizationType> ionizationMethod =
      new ComboParameter<IonizationType>("Ionization method",
          "Type of ion used to calculate the ionized mass", IonizationType.values());

  public static final OptionalModuleParameter<LipidSearchMSMSParameters> searchForMSMSFragments =
      new OptionalModuleParameter<LipidSearchMSMSParameters>(
          "Search for lipid class specific fragments in MS/MS spectra",
          "Search for lipid class specific fragments in MS/MS spectra",
          new LipidSearchMSMSParameters());

  public static final OptionalParameter<LipidModificationChoiceParameter> searchForModifications =
      new OptionalParameter<LipidModificationChoiceParameter>(new LipidModificationChoiceParameter(
          "Search for lipid modification",
          "If checked the algorithm searches for lipid modifications", new LipidModification[0]));

  public LipidSearchParameters() {
    super(new Parameter[] {peakLists, lipidClasses, chainLength, doubleBonds, ionizationMethod,
        mzTolerance, searchForMSMSFragments, searchForModifications});

  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    LipidSearchParameterSetupDialog dialog =
        new LipidSearchParameterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
