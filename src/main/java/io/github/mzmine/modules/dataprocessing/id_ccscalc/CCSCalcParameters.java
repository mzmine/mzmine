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

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.internal.InternalCCSCalcModule;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.mztochargeparameter.MzToChargeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import org.jetbrains.annotations.NotNull;

public class CCSCalcParameters extends SimpleParameterSet {

  public static final MZmineModule internalCalcModule = MZmineCore.getModuleInstance(
      InternalCCSCalcModule.class);

  public static final MZmineModule referenceCalcModule = MZmineCore.getModuleInstance(
      InternalCCSCalcModule.class);

  public static final MZmineModule[] calibrationModules = new MZmineModule[]{internalCalcModule,
      referenceCalcModule};

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final OptionalParameter<MzToChargeParameter> assumeChargeStage = new OptionalParameter<>(
      new MzToChargeParameter("Use fallback charge state\nfor unknown charges",
          "If enabled, a fallback charge state will be assumed, if no charge could be "
              + "determined during isotope pattern recognition.\nOtherwise, CCS values will only "
              + "be calculated for features with an assigned charge.\nOverlapping ranges are not allowed."),
      false);

  public static final ModuleComboParameter calibrationType = new ModuleComboParameter(
      "Calibration type",
      "Select how mobility values shall be calibrated to calculate CCS values.\n"
          + "For timsTOF files, internal calibration can be used if mobility values have already been calibrated.\n"
          + "Other vendors require calibration files with reference compounds.\n"
          + "The files should contain the columns mz, charge, mobility, and CCS.",
      calibrationModules, internalCalcModule);

  public CCSCalcParameters() {
    super(new UserParameter[]{featureLists, assumeChargeStage, calibrationType});
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
