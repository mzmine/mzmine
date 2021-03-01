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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.io.import_all_data_files;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;

public class AdvancedSpectraImportParameters extends SimpleParameterSet {

  public static final OptionalModuleParameter<MassDetectionSubParameters> msMassDetection = new OptionalModuleParameter<>(
      "MS1 mass detection (ADVANCED)",
      "Caution: Advanced option that applies mass detection (centroiding+thresholding) directly to imported scans (see help). Positive: Lower memory consumption; Caution: All processing steps will directly change the underlying data, with no way of retrieving raw data or inial results apart from the current state.",
      new MassDetectionSubParameters(), true);

  public static final OptionalModuleParameter<MassDetectionSubParameters> ms2MassDetection = new OptionalModuleParameter<>(
      "MS2 mass detection (ADVANCED)",
      "Caution: Advanced option that applies mass detection (centroiding+thresholding) directly to imported scans (see help). Positive: Lower memory consumption; Caution: All processing steps will directly change the underlying data, with no way of retrieving raw data or inial results apart from the current state.",
      new MassDetectionSubParameters(), true);

  public AdvancedSpectraImportParameters() {
    super(new Parameter[]{msMassDetection, ms2MassDetection});
  }

}
