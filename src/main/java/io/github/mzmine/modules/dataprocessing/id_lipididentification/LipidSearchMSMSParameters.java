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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;

public class LipidSearchMSMSParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzToleranceMS2 =
      new MZToleranceParameter("m/z tolerance MS2 level:",
          "Enter m/z tolerance for exact mass database matching on MS2 level");

  public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction =
      new ModuleComboParameter<>("Compare spectra similarity",
          "Algorithm to calculate similarity and filter matches",
          SpectralSimilarityFunction.FUNCTIONS);

  public LipidSearchMSMSParameters() {
    super(new Parameter[]{mzToleranceMS2});
  }

}
