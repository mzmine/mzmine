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

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDIA;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import java.io.File;
import java.text.DecimalFormat;

public final class WorkflowDiaWizardParameters extends WorkflowWizardParameters {

  public static final DoubleParameter minPearson = new DoubleParameter(
      "Minimum DIA correlation coefficient",
      "Pearson correlation coefficient of the MS1 precursor shape and the MS2 fragment ion shape.",
      new DecimalFormat("0.00"), 0.8);

  public static final IntegerParameter minCorrelatedPoints = new IntegerParameter(
      "Minimum DIA correlated points",
      "Minimum number of correlated points between MS1 and MS2 ions for pearson correlation.", 5, 3,
      Integer.MAX_VALUE);

  public static final BooleanParameter applySpectralNetworking = WorkflowDdaWizardParameters.applySpectralNetworking.cloneParameter();

  public static final BooleanParameter exportSirius = new BooleanParameter("Export for SIRIUS", "",
      true);
  public static final BooleanParameter exportGnps = new BooleanParameter(
      "Export for molecular networking (e.g., GNPS, FBMN, IIMN, MetGem)",
      "Export to Feature-based Molecular Networking (FBMN) and Ion Identity Molecular Networking (IIMN) on GNPS and other tools",
      true);
  public static final BooleanParameter exportAnnotationGraphics = new BooleanParameter(
      "Export annotation graphics", "Exports annotations to png and pdf images.", false);

  public static final OptionalParameter<FileNameSuffixExportParameter> exportPath = new OptionalParameter<>(
      new FileNameSuffixExportParameter("Export path",
          "If checked, export results for different tools, e.g., GNPS IIMN, SIRIUS, ...", null,
          false), false);


  public WorkflowDiaWizardParameters() {
    super(new WorkflowDIA(),
        // actual parameters
        minPearson, minCorrelatedPoints, applySpectralNetworking, exportPath, exportGnps,
        exportSirius, exportAnnotationGraphics);
  }


  public WorkflowDiaWizardParameters(final double minPearsonCorrelation, final int minPoints,
      final boolean applyNetworking, final boolean exportActive, final File exportBasePath,
      final boolean exportGnpsActive, boolean exportSiriusActive,
      boolean exportAnnotationGraphicsActive) {
    this();
    setParameter(minPearson, minPearsonCorrelation);
    setParameter(minCorrelatedPoints, minPoints);
    setParameter(applySpectralNetworking, applyNetworking);
    setParameter(exportPath, exportActive);
    getParameter(exportPath).getEmbeddedParameter().setValue(exportBasePath);
    setParameter(exportGnps, exportGnpsActive);
    setParameter(exportSirius, exportSiriusActive);
    setParameter(exportAnnotationGraphics, exportAnnotationGraphicsActive);
  }

}
