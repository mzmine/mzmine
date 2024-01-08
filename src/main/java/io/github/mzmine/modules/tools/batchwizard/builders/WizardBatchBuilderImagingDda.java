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

package io.github.mzmine.modules.tools.batchwizard.builders;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceImagingWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalValue;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.io.File;
import java.util.Optional;

public class WizardBatchBuilderImagingDda extends BaseWizardBatchBuilder {

  private final Integer minNumberOfPixels;
  private final Boolean enableDeisotoping;
  private final File exportPath;
  private final boolean isExportActive;
  private final Boolean applySpectralNetworking;

  public WizardBatchBuilderImagingDda(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    // special workflow parameter are extracted here
    minNumberOfPixels = getValue(params, IonInterfaceImagingWizardParameters.minNumberOfDataPoints);
    enableDeisotoping = getValue(params, IonInterfaceImagingWizardParameters.enableDeisotoping);

//    // DDA workflow parameters
    params = steps.get(WizardPart.WORKFLOW);
    applySpectralNetworking = getValue(params, WorkflowDdaWizardParameters.applySpectralNetworking);
    OptionalValue<File> optional = getOptional(params, WorkflowDdaWizardParameters.exportPath);
    isExportActive = optional.active();
    exportPath = optional.value();

//    exportGnps = getValue(params, WorkflowDdaWizardParameters.exportGnps);
//    exportSirius = getValue(params, WorkflowDdaWizardParameters.exportSirius);
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);

    // TODO make image builder
    makeAndImageBuilderStep(q);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minImsDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q, null);
      makeAndAddSmoothingStep(q, false, minImsDataPoints, imsSmoothing);
    }

    if (enableDeisotoping) {
      makeAndAddDeisotopingStep(q, null);
    }

    makeAndAddIsotopeFinderStep(q);
    makeAndAddAlignmentStep(q);
    makeAndAddRowFilterStep(q);

    // networking
    if (applySpectralNetworking) {
      makeAndAddSpectralNetworkingSteps(q, isExportActive, exportPath);
    }

    // annotation
    makeAndAddLibrarySearchStep(q, false);
    return q;
  }

  @Override
  protected void makeAndAddImportTask(final BatchQueue q) {
    // todo make auto mass detector work, so we can use it here.

    if (isImsActive && imsInstrumentType == MobilityType.TIMS) {
      final ParameterSet param = MZmineCore.getConfiguration()
          .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
      final AdvancedSpectraImportParameters advancedParam = (AdvancedSpectraImportParameters) new AdvancedSpectraImportParameters().cloneParameterSet();

      final CentroidMassDetector massDetector = MassDetectionParameters.centroid;
      final ParameterSet massDetectorParam = MZmineCore.getConfiguration()
          .getModuleParameters(CentroidMassDetector.class).cloneParameterSet();
      massDetectorParam.setParameter(CentroidMassDetectorParameters.noiseLevel,
          massDetectorOption.getMs1NoiseLevel());
      massDetectorParam.setParameter(CentroidMassDetectorParameters.detectIsotopes, false);
      MZmineProcessingStep<MassDetector> massDetectorStep = new MZmineProcessingStepImpl<>(
          massDetector, massDetectorParam);
      advancedParam.setParameter(AdvancedSpectraImportParameters.msMassDetection, true,
          massDetectorStep);

      param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(true);
      param.getParameter(AllSpectralDataImportParameters.advancedImport)
          .setEmbeddedParameters(advancedParam);
      param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(dataFiles);
      param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(libraries);

      q.add(new MZmineProcessingStepImpl<>(
          MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param));
    } else {
      super.makeAndAddImportTask(q);
    }
  }

  @Override
  protected void makeAndAddImsExpanderStep(final BatchQueue q) {
    ParameterSet param = MZmineCore.getConfiguration().getModuleParameters(ImsExpanderModule.class)
        .cloneParameterSet();

    param.setParameter(ImsExpanderParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(ImsExpanderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ImsExpanderParameters.useRawData, false);
    param.getParameter(ImsExpanderParameters.useRawData).getEmbeddedParameter()
        .setValue(massDetectorOption.getMs1NoiseLevel());
    param.setParameter(ImsExpanderParameters.mzTolerance, true);
    param.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .setValue(mzTolScans);
    param.setParameter(ImsExpanderParameters.mobilogramBinWidth, false);
    param.setParameter(ImsExpanderParameters.maxNumTraces, true, 5);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImsExpanderModule.class),
        param));
  }


  protected void makeAndImageBuilderStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ImageBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // crop rt range
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection, new ScanSelection(1));
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolScans);
    param.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minFeatureHeight);
    param.setParameter(ImageBuilderParameters.minimumConsecutiveScans, 5);
    param.setParameter(ImageBuilderParameters.minTotalSignals, minNumberOfPixels);
    param.setParameter(ImageBuilderParameters.suffix, "images");

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImageBuilderModule.class),
        param));
  }


  protected void makeAndAddAlignmentStep(final BatchQueue q) {
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

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param));
  }

  protected void makeAndAddMassDetectorSteps(final BatchQueue q) {
    if (isImsActive && imsInstrumentType == MobilityType.TIMS) {
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.FRAMES);
      makeAndAddMassDetectionStep(q, 2, SelectedScanTypes.MOBLITY_SCANS);
    }
  }

}
