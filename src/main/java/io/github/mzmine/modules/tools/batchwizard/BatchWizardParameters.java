/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
