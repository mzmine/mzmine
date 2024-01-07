/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import java.io.File;

public final class WorkflowGcElectronImpactWizardParameters extends WorkflowWizardParameters {

  public static final BooleanParameter exportGnps = new BooleanParameter(
      "Export for GNPS GC-EI FBMN", "Export to Feature-based Molecular Networking (FBMN) on GNPS",
      true);

  public static final BooleanParameter exportMsp = new BooleanParameter("Export for MSP",
      "Export to MSP", true);

  public static final BooleanParameter exportAnnotationGraphics = new BooleanParameter(
      "Export annotation graphics", "Exports annotations to png and pdf images.", false);


  public static final OptionalParameter<FileNameParameter> exportPath = new OptionalParameter<>(
      new FileNameParameter("Export path",
          "If checked, export results for different tools, e.g., GNPS, SIRIUS, ...",
          FileSelectionType.SAVE, false), false);


  public WorkflowGcElectronImpactWizardParameters() {
    super(WorkflowWizardParameterFactory.DECONVOLUTION,
        // actual parameters
        exportPath, exportGnps, exportMsp, exportAnnotationGraphics);
  }


  public WorkflowGcElectronImpactWizardParameters(final boolean exportActive,
      final File exportBasePath, final boolean exportGnpsActive, final boolean exportMspActive, final boolean exportAnnotationGraphicsActive) {
    this();
    setParameter(exportPath, exportActive);
    getParameter(exportPath).getEmbeddedParameter().setValue(exportBasePath);
    setParameter(exportGnps, exportGnpsActive);
    setParameter(exportMsp, exportMspActive);
    setParameter(exportAnnotationGraphics, exportAnnotationGraphicsActive);
  }

}
