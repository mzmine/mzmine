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

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset;
import io.github.mzmine.modules.tools.batchwizard.WizardWorkflow;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AbstractWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceImagingWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderImagingDda extends WizardBatchBuilder {

  private final Integer minDataPoints;
  private final Boolean isExportActive;
  private final Boolean exportGnps;
  private final Boolean exportSirius;
  private final File exportPath;

  public WizardBatchBuilderImagingDda(final WizardWorkflow steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends AbstractWizardParameters> params = steps.get(WizardPart.ION_INTERFACE)
        .map(WizardPreset::parameters);
    // special workflow parameter are extracted here
    minDataPoints = getValue(params, IonInterfaceImagingWizardParameters.minNumberOfDataPoints);

    // DDA workflow parameters
    params = steps.get(WizardPart.WORKFLOW).map(WizardPreset::parameters);
    OptionalValue<File> optional = getOptional(params, WorkflowDdaWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();
    exportGnps = getValue(params, WorkflowDdaWizardParameters.exportGnps);
    exportSirius = getValue(params, WorkflowDdaWizardParameters.exportSirius);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    q.add(makeImportTask());
    makeMassDetectorSteps(q);

    // TODO make image builder

    if (isImsActive) {
      q.add(makeImsExpanderStep());
      makeSmoothingStep(q, false, minDataPoints, imsSmoothing);
      q.add(makeMobilityResolvingStep());
      makeSmoothingStep(q, false, minDataPoints, imsSmoothing);
    }

    q.add(makeIsotopeFinderStep());
    q.add(makeAlignmentStep());
    makeRowFilterStep(q);

    // annotation
    makeLibrarySearchStep(q);
    // export
    makeDdaExportSteps(q, isExportActive, exportPath, exportGnps, exportSirius);
    return q;
  }


  protected MZmineProcessingStep<MZmineProcessingModule> makeAlignmentStep() {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(JoinAlignerModule.class).cloneParameterSet();
    param.setParameter(JoinAlignerParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(JoinAlignerParameters.peakListName, "Aligned feature list");
    param.setParameter(JoinAlignerParameters.MZTolerance, mzTolInterSample);
    param.setParameter(JoinAlignerParameters.MZWeight, 3d);
    param.setParameter(JoinAlignerParameters.RTTolerance, new RTTolerance(100000, Unit.MINUTES));
    param.setParameter(JoinAlignerParameters.RTWeight, 0d);
    param.setParameter(JoinAlignerParameters.mobilityTolerance, isImsActive);
    param.getParameter(JoinAlignerParameters.mobilityTolerance).getEmbeddedParameter().setValue(
        imsInstrumentType == MobilityType.TIMS ? new MobilityTolerance(0.01f)
            : new MobilityTolerance(1f));
    param.setParameter(JoinAlignerParameters.SameChargeRequired, false);
    param.setParameter(JoinAlignerParameters.SameIDRequired, false);
    param.setParameter(JoinAlignerParameters.compareIsotopePattern, false);
    param.setParameter(JoinAlignerParameters.compareSpectraSimilarity, false);
    param.setParameter(JoinAlignerParameters.handleOriginal, handleOriginalFeatureLists);

    return new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param);
  }

}
