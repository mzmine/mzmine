/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.external.ExternalCCSCalibrationModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.ReferenceCCSCalcModule;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import java.util.Collection;

public class CCSCalibrationParameters extends SimpleParameterSet {

  public static final MZmineModule referenceCalcModule = MZmineCore.getModuleInstance(
      ReferenceCCSCalcModule.class);

  public static final MZmineModule externalCalcModule = MZmineCore.getModuleInstance(
      ExternalCCSCalibrationModule.class);

  public static final MZmineModule[] calibrationModules = new MZmineModule[]{referenceCalcModule,
      externalCalcModule};
  public static final ModuleComboParameter calibrationType = new ModuleComboParameter(
      "Calibration type",
      "Select how mobility values shall be calibrated to calculate CCS values.\n"
          + "For timsTOF files, internal calibration can be used if mobility values have already been calibrated.\n"
          + "Other vendors require calibration files with reference compounds.\n"
          + "The files should contain the columns mz, charge, mobility, and CCS.",
      calibrationModules, referenceCalcModule);

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final OptionalParameter<RawDataFilesParameter> files = new OptionalParameter<>(
      new RawDataFilesParameter("Set to additional raw files", 0, Integer.MAX_VALUE), false);

  public CCSCalibrationParameters() {
    super(new Parameter[]{files, flists, calibrationType});
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    if (!super.checkParameterValues(errorMessages)) {
      return false;
    }

    boolean check = true;

    if (getValue(CCSCalibrationParameters.files)) {
      final RawDataFilesSelection value = getParameter(
          CCSCalibrationParameters.files).getEmbeddedParameter().getValue();
      final RawDataFilesSelection clone = value.clone();
      final RawDataFile[] files = clone.getMatchingRawDataFiles(); // dont evaluate the real parameter, otherwise we have to reset it.
      ModularFeatureList[] flists = getParameter(CCSCalibrationParameters.flists).getValue()
          .getMatchingFeatureLists();

      if (getValue(CCSCalibrationParameters.files) && files.length != 0 && flists.length > 1) {
        errorMessages.add(
            "Invalid parameter selection. Either select one feature list and >= 1 raw data file or no raw data files.");
        check = false;
      }
    }

    return check;
  }
}
