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

package io.github.mzmine.modules.tools.batchwizard.builders;

import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceDirectAndFlowInjectWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderFlowInjectDDA extends WizardBatchBuilder {

  private final Integer minRtDataPoints;
  private final Boolean isExportActive;
  private final Boolean exportGnps;
  private final Boolean exportSirius;
  private final File exportPath;

  public WizardBatchBuilderFlowInjectDDA(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    // special workflow parameter are extracted here
    minRtDataPoints = getValue(params,
        IonInterfaceDirectAndFlowInjectWizardParameters.minNumberOfDataPoints);
    // DDA workflow parameters
    params = steps.get(WizardPart.WORKFLOW);
    OptionalValue<File> optional = getOptional(params, WorkflowDdaWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();
    exportGnps = getValue(params, WorkflowDdaWizardParameters.exportGnps);
    exportSirius = getValue(params, WorkflowDdaWizardParameters.exportSirius);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, noiseLevelMs1, minRtDataPoints,
        null);

    var groupMs2Params = createMs2GrouperParameters(minRtDataPoints, false, null);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q, groupMs2Params);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
    } else {
      makeAndAddMs2GrouperStep(q, groupMs2Params);
    }

    makeAndAddDeisotopingStep(q, null);
    makeAndAddIsotopeFinderStep(q);
    makeAndAddAlignmentStep(q, null);
    makeAndAddRowFilterStep(q);
    makeAndAddGapFillStep(q, null);
    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q, minRtDataPoints, null, true);
    makeAndAddIinStep(q);

    // annotation
    makeAndAddLibrarySearchStep(q);
    // export
    makeAndAddDdaExportSteps(q, isExportActive, exportPath, exportGnps, exportSirius);
    return q;
  }

}


