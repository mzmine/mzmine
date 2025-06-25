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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceDirectAndFlowInjectWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderFlowInjectLibraryGen extends BaseWizardBatchBuilder {

  private final Integer minRtDataPoints;
  private final Boolean exportGnps;
  private final Boolean exportSirius;
  private final File exportPath;
  private final LibraryBatchMetadataParameters libGenMetadata;
  private final Boolean applySpectralNetworking;
  private final Boolean exportUnknownScansFile;

  public WizardBatchBuilderFlowInjectLibraryGen(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    // special workflow parameter are extracted here
    minRtDataPoints = getValue(params,
        IonInterfaceDirectAndFlowInjectWizardParameters.minNumberOfDataPoints);

    // library generation workflow parameters
    params = steps.get(WizardPart.WORKFLOW);
    applySpectralNetworking = getValue(params,
        WorkflowLibraryGenerationWizardParameters.applySpectralNetworking);
    exportPath = getValue(params, WorkflowLibraryGenerationWizardParameters.exportPath);
    exportGnps = getValue(params, WorkflowLibraryGenerationWizardParameters.exportGnps);
    exportSirius = getValue(params, WorkflowLibraryGenerationWizardParameters.exportSirius);
    exportUnknownScansFile = getValue(params,
        WorkflowLibraryGenerationWizardParameters.exportUnknownScansFile);
    libGenMetadata = getOptionalParameters(params,
        WorkflowLibraryGenerationWizardParameters.metadata).value();
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddMsNTreeBuilderStep(q);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
      var groupMs2Params = createMs2GrouperParameters(minRtDataPoints, false, null);
      makeAndAddMobilityResolvingStep(q, groupMs2Params);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
    }
    // NO FILTERING FOR ISOTOPES
    makeAndAddIsotopeFinderStep(q);

    // annotation
    makeAndAddLocalCsvDatabaseSearchStep(q, null);

    // library generation, reload library
    makeAndAddBatchLibraryGeneration(q, exportPath, libGenMetadata);

    // join after the generation but just concat all lists together
    makeAndAddJoinAlignmentStep(q, null);

    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q, minRtDataPoints, null, true);
    makeAndAddIinStep(q);

    // match against own library
    makeAndAddLibrarySearchStep(q, true);

    // export all unannotated scans - after alignment to merge duplicates
    if (exportUnknownScansFile) {
      makeAndAddExportScansStep(q, exportPath, libGenMetadata, true, "_unknown_scans");
    }

    // export
    makeAndAddDdaExportSteps(q, true, exportPath, exportGnps, exportSirius, false, mzTolScans);

    // convert library to feature list
    makeAndAddLibraryToFeatureListStep(q);

    // networking
    if (applySpectralNetworking) {
      makeAndAddSpectralNetworkingSteps(q, true, exportPath, false);
    }
    makeAndAddBatchExportStep(q, true, exportPath);
    return q;
  }

  protected void makeAndAddMsNTreeBuilderStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(MsnTreeFeatureDetectionModule.class).cloneParameterSet();

    param.setParameter(MsnTreeFeatureDetectionParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.setParameter(MsnTreeFeatureDetectionParameters.scanSelection, new ScanSelection(1));
    param.setParameter(MsnTreeFeatureDetectionParameters.mzTol, mzTolScans);
    param.setParameter(MsnTreeFeatureDetectionParameters.suffix, "msn trees");

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MsnTreeFeatureDetectionModule.class), param));
  }

}


