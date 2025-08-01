/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowTargetPlate;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.util.List;

public final class WorkflowTargetPlateWizardParameters extends WorkflowWizardParameters {

  public static OptionalParameter<FileNameParameter> spotNamesFile = new OptionalParameter<>(
      new FileNameParameter("Spot-to-Sample name file (csv)", """
          A file containing spot names and the corresponding sample name.
          The file must contain labelled columns ('spot' and 'name') and be separated by ';'
          """, List.of(ExtensionFilters.CSV), FileSelectionType.OPEN), false);

  public WorkflowTargetPlateWizardParameters() {
    super(new WorkflowTargetPlate(), spotNamesFile);
  }

  public WorkflowTargetPlateWizardParameters(boolean enableSpotFileName, File spotFileName) {
    this();
    setParameter(spotNamesFile, enableSpotFileName, spotFileName);
  }
}
