/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDeconvolution;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import java.io.File;

public final class WorkflowGcElectronImpactWizardParameters extends WorkflowWizardParameters {


  public static final IntegerParameter MIN_NUMBER_OF_SIGNALS_IN_DECON_SPECTRA = new IntegerParameter(
      "Min number of signals in deconvoluted spectrum",
      "Min number of signals in deconvoluted spectrum", 8, true);

  public static final BooleanParameter applySpectralNetworking = new BooleanParameter(
      "Apply spectral / molecular networking", """
      Applies feature-based molecular networking by comparing all pseudo MS2 spectra.
      Adds graphml export and enables use of interactive network visualizer in mzmine.""", true);

  public static final BooleanParameter exportGnps = new BooleanParameter(
      "Export for GNPS GC-EI FBMN", "Export to Feature-based Molecular Networking (FBMN) on GNPS",
      true);

  public static final BooleanParameter exportMsp = new BooleanParameter("Export for MSP",
      "Export to MSP", true);

  public static final BooleanParameter exportAnnotationGraphics = new BooleanParameter(
      "Export annotation graphics", "Exports annotations to png and pdf images.", false);


  public static final OptionalParameter<FileNameSuffixExportParameter> exportPath = new OptionalParameter<>(
      new FileNameSuffixExportParameter("Export path",
          "If checked, export results for different tools, e.g., GNPS, SIRIUS, ...", null, false),
      false);


  public WorkflowGcElectronImpactWizardParameters() {
    super(new WorkflowDeconvolution(),
        // actual parameters
        MIN_NUMBER_OF_SIGNALS_IN_DECON_SPECTRA, applySpectralNetworking, exportPath, exportGnps,
        exportMsp, exportAnnotationGraphics);
  }


  public WorkflowGcElectronImpactWizardParameters(final int numberOfSignals,
      final boolean applyNetworking, final boolean exportActive, final File exportBasePath,
      final boolean exportGnpsActive, final boolean exportMspActive,
      final boolean exportAnnotationGraphicsActive) {
    this();
    setParameter(exportPath, exportActive, exportBasePath);
    setParameter(MIN_NUMBER_OF_SIGNALS_IN_DECON_SPECTRA, numberOfSignals);
    setParameter(applySpectralNetworking, applyNetworking);
    setParameter(exportGnps, exportGnpsActive);
    setParameter(exportMsp, exportMspActive);
    setParameter(exportAnnotationGraphics, exportAnnotationGraphicsActive);
  }

}
