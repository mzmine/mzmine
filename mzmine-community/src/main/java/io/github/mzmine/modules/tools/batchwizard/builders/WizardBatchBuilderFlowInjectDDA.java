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

public class WizardBatchBuilderFlowInjectDDA extends BaseWizardBatchBuilder {

  private final Integer minRtDataPoints;
  private final Boolean applySpectralNetworking;
  private final boolean isExportActive;
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
    applySpectralNetworking = getValue(params, WorkflowDdaWizardParameters.applySpectralNetworking);
    OptionalValue<File> optional = getOptional(params, WorkflowDdaWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, massDetectorOption,
        minRtDataPoints, null, polarity);

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
    makeAndAddJoinAlignmentStep(q, null);
    makeAndAddRowFilterStep(q);
    makeAndAddGapFillStep(q, null, minRtDataPoints);
    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q, minRtDataPoints, null, true);
    makeAndAddIinStep(q);

    // annotation
    makeAndAddLibrarySearchStep(q, false);
    makeAndAddLocalCsvDatabaseSearchStep(q, null);
    makeAndAddLipidAnnotationStep(q);
    // networking
    if (applySpectralNetworking) {
      makeAndAddSpectralNetworkingSteps(q, isExportActive, exportPath, false);
    }

    // export
    makeAndAddDdaExportSteps(q, steps, mzTolScans);
    makeAndAddBatchExportStep(q, isExportActive, exportPath);
    return q;
  }

}


