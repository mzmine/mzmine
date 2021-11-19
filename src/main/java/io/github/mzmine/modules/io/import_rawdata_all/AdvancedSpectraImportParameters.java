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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class AdvancedSpectraImportParameters extends SimpleParameterSet {

  public static final OptionalParameter<ModuleComboParameter<MassDetector>> msMassDetection =
      new OptionalParameter<>(new ModuleComboParameter<MassDetector>("MS1 detector (Advanced)",
          "Algorithm to use on MS1 scans for mass detection and its parameters",
          MassDetectionParameters.massDetectors, MassDetectionParameters.massDetectors[0]));

  public static final OptionalParameter<ModuleComboParameter<MassDetector>> ms2MassDetection =
      new OptionalParameter<>(new ModuleComboParameter<MassDetector>("MS2 detector (Advanced)",
          "Algorithm to use on MS2 scans for mass detection and its parameters",
          MassDetectionParameters.massDetectors, MassDetectionParameters.massDetectors[0]));

  public AdvancedSpectraImportParameters() {
    super(new Parameter[]{msMassDetection, ms2MassDetection});
  }

}
