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

import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowLibraryGeneration;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import java.io.File;

public final class WorkflowLibraryGenerationWizardParameters extends WorkflowWizardParameters {

  public static final BooleanParameter exportUnknownScansFile = new BooleanParameter(
      "Export scans file for 'unknowns'",
      "Library generation only exports scans of annotated features. This option exports another file for unannotated features.",
      false);

  public static final BooleanParameter exportSirius = new BooleanParameter("Export for SIRIUS", "",
      true);
  public static final BooleanParameter exportGnps = new BooleanParameter(
      "Export for molecular networking (e.g., GNPS, FBMN, IIMN, MetGem)",
      "Export to Feature-based Molecular Networking (FBMN) and Ion Identity Molecular Networking (IIMN) on GNPS",
      true);

  public static final FileNameSuffixExportParameter exportPath = new FileNameSuffixExportParameter(
      "Export path", "If checked, export results for different tools, e.g., GNPS IIMN, SIRIUS, ...",
      null, false);


  public static final ParameterSetParameter<LibraryBatchMetadataParameters> metadata = new ParameterSetParameter<>(
      "Metadata", "Metadata for all entries", new LibraryBatchMetadataParameters());


  public static final BooleanParameter applySpectralNetworking = new BooleanParameter(
      "Apply spectral networking (FBMN/IIMN)", """
      Applies feature-based molecular networking/ion identity molecular networking
      by comparing all MS2 spectra. Adds graphml export and enables use of visualizer. 
      """, false);


  public WorkflowLibraryGenerationWizardParameters() {
    super(new WorkflowLibraryGeneration(),
        // actual parameters
        metadata, exportPath, exportUnknownScansFile, exportGnps, exportSirius,
        applySpectralNetworking);
  }


  public WorkflowLibraryGenerationWizardParameters(final File exportBasePath,
      final boolean exportUnknownScansFileActive, final boolean exportGnpsActive,
      boolean exportSiriusActive, boolean applySpectralNetworking) {
    this();
    setParameter(exportUnknownScansFile, exportUnknownScansFileActive);
    setParameter(exportPath, exportBasePath);
    setParameter(exportGnps, exportGnpsActive);
    setParameter(exportSirius, exportSiriusActive);
    setParameter(this.applySpectralNetworking, applySpectralNetworking);
  }

}
