package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.BuildingMzMLMsScan;
import io.github.mzmine.parameters.ParameterSet;

public class SpectralProcessingUtils {

  private BuildingMzMLMsScan scan;

  private ParameterSet advancedParameters;
  private boolean denormalizeMSnScans;
  private MZmineProcessingStep<MassDetector> ms1Detector;
  private MZmineProcessingStep<MassDetector> ms2Detector;

  private boolean applyMassDetection;

  public SpectralProcessingUtils(BuildingMzMLMsScan scan, ParameterSet advancedParameters) {
    this.scan = scan;
    this.advancedParameters = advancedParameters;
  }

  public void processScan(double[] mzs, double[] intensities) {
    processAdvancedParameters(advancedParameters);
    // apply mass detection
    this.applyMassDetection = ms1Detector != null || ms2Detector != null;
    if (applyMassDetection) {
      double[][] values = null;
      if (ms1Detector != null) {
        values = applyMassDetection(ms1Detector, mzs, intensities);
      } else if (ms2Detector != null && scan.getMsLevel() >= 2) {
        values = applyMassDetection(ms2Detector, mzs, intensities);
        if (denormalizeMSnScans) {
          //todo add denormalization
          // denormalizeScan();
        }
      }
      scan.setDoubleBufferMzValues(values[0]);
      scan.setDoubleBufferIntensities(values[1]);
    } else {
      scan.setDoubleBufferMzValues(mzs);
      scan.setDoubleBufferIntensities(intensities);
    }
    scan.setTempValuesToNull();
  }

  private void processAdvancedParameters(ParameterSet advancedParameters) {
    if (advancedParameters != null) {
      if (advancedParameters.getParameter(AdvancedSpectraImportParameters.msMassDetection)
          .getValue()) {
        this.ms1Detector = advancedParameters.getParameter(
            AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter().getValue();
      }
      if (advancedParameters.getParameter(AdvancedSpectraImportParameters.ms2MassDetection)
          .getValue()) {
        this.ms2Detector = advancedParameters.getParameter(
            AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter().getValue();
      }
      denormalizeMSnScans = advancedParameters.getValue(
          AdvancedSpectraImportParameters.denormalizeMSnScans);
    }

    this.applyMassDetection = ms1Detector != null || ms2Detector != null;
  }

  //  todo move here sorting mzs
//  private double[][] sortByMzValues() {
//    return DataPointUtils.sort(this.mzValues, this.intensityValues,
//        DataPointSorter.DEFAULT_MZ_ASCENDING);
//  }

  private double[][] applyMassDetection(MZmineProcessingStep<MassDetector> msDetector, double[] mzs,
      double[] intensities) {
    // run mass detection on data object
    // [mzs, intensities]
    return msDetector.getModule().getMassValues(mzs, intensities, msDetector.getParameterSet());
  }

  //todo implemeny denormalization
  private void denormalizeScan() {
//        ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(values[1],
//            wrapper.getInjectionTime());
//        ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(values[1],
//            wrapper.getInjectionTime());
  }

  //todo add crop filter
  private void cropScan() {

  }

}
