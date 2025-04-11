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

import com.google.common.collect.Range;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowLibraryGenerationWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderLcLibraryGen extends BaseWizardBatchBuilder {

  private final Range<Double> cropRtRange;
  private final RTTolerance intraSampleRtTol;
  private final RTTolerance interSampleRtTol;
  private final Integer minRtDataPoints;
  private final Integer maxIsomersInRt;
  private final RTTolerance rtFwhm;
  private final Boolean stableIonizationAcrossSamples;
  private final Boolean exportGnps;
  private final Boolean exportSirius;
  private final File exportPath;
  private final Boolean rtSmoothing;
  private final LibraryBatchMetadataParameters libGenMetadata;
  private final Boolean applySpectralNetworking;
  private final Boolean exportUnknownScansFile;

  public WizardBatchBuilderLcLibraryGen(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    // special workflow parameter are extracted here
    // chromatography
    rtSmoothing = getValue(params, IonInterfaceHplcWizardParameters.smoothing);
    cropRtRange = getValue(params, IonInterfaceHplcWizardParameters.cropRtRange);
    intraSampleRtTol = getValue(params, IonInterfaceHplcWizardParameters.intraSampleRTTolerance);
    interSampleRtTol = getValue(params, IonInterfaceHplcWizardParameters.interSampleRTTolerance);
    minRtDataPoints = getValue(params, IonInterfaceHplcWizardParameters.minNumberOfDataPoints);
    maxIsomersInRt = getValue(params,
        IonInterfaceHplcWizardParameters.maximumIsomersInChromatogram);
    rtFwhm = getValue(params, IonInterfaceHplcWizardParameters.approximateChromatographicFWHM);
    stableIonizationAcrossSamples = getValue(params,
        IonInterfaceHplcWizardParameters.stableIonizationAcrossSamples);

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
    makeAndAddAdapChromatogramStep(q, minFeatureHeight, mzTolScans, massDetectorOption,
        minRtDataPoints, cropRtRange, polarity);
    makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, false);

    var groupMs2Params = createMs2GrouperParameters();
    makeAndAddRtLocalMinResolver(q, groupMs2Params, minRtDataPoints, cropRtRange, rtFwhm,
        maxIsomersInRt);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minRtDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q, groupMs2Params);
      makeAndAddSmoothingStep(q, rtSmoothing, minRtDataPoints, imsSmoothing);
    }
    // NO FILTERING FOR ISOTOPES
    makeAndAddIsotopeFinderStep(q);

    // annotation
    makeAndAddLocalCsvDatabaseSearchStep(q, interSampleRtTol);
    makeAndAddLipidAnnotationStep(q);

    // library generation, reload library
    makeAndAddBatchLibraryGeneration(q, exportPath, libGenMetadata);

    // join after the generation but just concat all lists together
    makeAndAddJoinAlignmentStep(q, interSampleRtTol);

    // ions annotation and feature grouping
    makeAndAddMetaCorrStep(q);
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

  protected ParameterSet createMs2GrouperParameters() {
    return super.createMs2GrouperParameters(minRtDataPoints, minRtDataPoints >= 4, rtFwhm);
  }


  protected void makeAndAddMetaCorrStep(final BatchQueue q) {
    final boolean useCorrGrouping = minRtDataPoints > 3;
    RTTolerance rtTol = new RTTolerance(rtFwhm.getTolerance() * (useCorrGrouping ? 1.1f : 0.7f),
        rtFwhm.getUnit());
    makeAndAddMetaCorrStep(q, minRtDataPoints, rtTol, stableIonizationAcrossSamples);
  }

}
