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

package io.github.mzmine.modules.tools.batchwizard.builders;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection.MaldiSpotFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection.MaldiSpotFeatureDetectionParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceImagingWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowTargetPlateWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderTargetPlate extends BaseWizardBatchBuilder {

  private final OptionalValue<File> spotNamesFile;
  private final Boolean enableDeisotoping;

  public WizardBatchBuilderTargetPlate(WizardSequence steps) {
    super(steps);

    final Optional<WizardStepParameters> driedDropletParam = steps.get(WizardPart.WORKFLOW);
    spotNamesFile = getOptional(driedDropletParam,
        WorkflowTargetPlateWizardParameters.spotNamesFile);
    final Optional<WizardStepParameters> maldiParam = steps.get(WizardPart.ION_INTERFACE);
    enableDeisotoping = getValue(maldiParam, IonInterfaceImagingWizardParameters.enableDeisotoping);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndAddSpotDetectionStep(q);

    if (isImsActive) {
      if (imsSmoothing) {
        makeAndAddSmoothingStep(q, false, 0, true);
      }
      makeAndAddMobilityResolvingStep(q, null);
    }
    if (enableDeisotoping) {
      makeAndAddDeisotopingStep(q, new RTTolerance(0.05f, Unit.SECONDS));
    }
    makeAndAddIsotopeFinderStep(q);
    makeAndAddLocalCsvDatabaseSearchStep(q, null);
    makeAndAddBatchExportStep(q, true, null);
    return q;
  }

  public void makeAndAddSpotDetectionStep(BatchQueue q) {
    final ParameterSet param = ConfigService.getConfiguration()
        .getModuleParameters(MaldiSpotFeatureDetectionModule.class).cloneParameterSet();

    param.setParameter(MaldiSpotFeatureDetectionParameters.files,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    param.setParameter(MaldiSpotFeatureDetectionParameters.spotNameFile, spotNamesFile.active(),
        spotNamesFile.value());
    param.setParameter(MaldiSpotFeatureDetectionParameters.minIntensity, minFeatureHeight);
    param.setParameter(MaldiSpotFeatureDetectionParameters.mzTolerance, mzTolScans);
    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(MaldiSpotFeatureDetectionModule.class), param));
  }
}
