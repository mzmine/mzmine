/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;

public class BatchWizardParameters extends SimpleParameterSet {

  public static final ParameterSetParameter msParams = new ParameterSetParameter("MS parameters",
      "", new BatchWizardMassSpectrometerParameters());

  public static final ParameterSetParameter hplcParams = new ParameterSetParameter(
      "(U)HPLC parameters", "", new BatchWizardHPLCParameters());

  public static final ParameterSetParameter dataInputParams = new ParameterSetParameter(
      "Data input", "Data files and spectral library files", new BatchWizardDataInputParameters());

  public static final OptionalParameter<FileNameParameter> exportPath = new OptionalParameter<>(
      new FileNameParameter("Export path",
          "If checked, export results for different tools, e.g., GNPS IIMN, SIRIUS, ...",
          FileSelectionType.SAVE, false), false);

  public BatchWizardParameters() {
    super(new Parameter[]{msParams, hplcParams, dataInputParams, exportPath});
  }

}
