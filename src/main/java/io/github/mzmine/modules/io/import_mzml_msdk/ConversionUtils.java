package io.github.mzmine.modules.io.import_mzml_msdk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLCV;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLIsolationWindow;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLMsScan;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLPrecursorActivation;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLPrecursorSelectedIonList;

public class ConversionUtils {

  private static Logger logger = Logger.getLogger(ConversionUtils.class.getName());

  public static double[] convertFloatsToDoubles(float[] input) {
    if (input == null) {
      return null; // Or throw an exception - your choice
    }
    double[] output = new double[input.length];
    for (int i = 0; i < input.length; i++) {
      output[i] = input[i];
    }
    return output;
  }

  public static double[] convertIntsToDoubles(int[] input) {
    if (input == null) {
      return null; // Or throw an exception - your choice
    }
    double[] output = new double[input.length];
    for (int i = 0; i < input.length; i++) {
      output[i] = input[i];
    }
    return output;
  }

  public static MassSpectrumType msdkToMZmineSpectrumType(MsSpectrumType msdk) {
    switch (msdk) {
      case PROFILE -> {
        return MassSpectrumType.PROFILE;
      }
      case CENTROIDED -> {
        return MassSpectrumType.PROFILE;
      }
      case THRESHOLDED -> {
        return MassSpectrumType.THRESHOLDED;
      }
      default -> {
        return MassSpectrumType.CENTROIDED;
      }
    }
  }

  public static PolarityType msdkToMZminePolarityType(io.github.msdk.datamodel.PolarityType msdk) {
    switch (msdk) {
      case POSITIVE -> {
        return PolarityType.POSITIVE;
      }
      case NEGATIVE -> {
        return PolarityType.NEGATIVE;
      }
      default -> {
        return PolarityType.UNKNOWN;
      }
    }
  }

  public static Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan) {
    double precursorMz = 0.0;
    int precursorCharge = -1;
    for (MzMLPrecursorElement precursorElement : scan.getPrecursorList().getPrecursorElements()) {
      Optional<MzMLPrecursorSelectedIonList> selectedIonList =
          precursorElement.getSelectedIonList();
      if (selectedIonList.isPresent()) {
        if (selectedIonList.get().getSelectedIonList().size() > 1) {
          logger.info("Selection of more than one ion in a single scan is not supported.");
        }
        for (MzMLCVParam param : selectedIonList.get().getSelectedIonList().get(0)
            .getCVParamsList()) {
          if (param.getAccession().equals(MzMLCV.cvPrecursorMz)) {
            precursorMz = Double.parseDouble(param.getValue().get());
          }
          if (param.getAccession().equals(MzMLCV.cvChargeState)) {
            precursorCharge = Integer.parseInt(param.getValue().get());
          }
        }
      }
    }

    final SimpleScan newScan = new SimpleScan(rawDataFile, scan.getScanNumber(), scan.getMsLevel(),
        scan.getRetentionTime() / 60, precursorMz, precursorCharge, scan.getMzValues(),
        convertFloatsToDoubles(scan.getIntensityValues()),
        ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
        ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
        scan.getScanningRange());


    return newScan;
  }

  public static MobilityScan msdkScanToMobilityScan(IMSRawDataFile rawDataFile, int scannum,
      MsScan scan, Frame frame) {
    return new SimpleMobilityScan(rawDataFile, scannum, frame, scan.getMzValues(),
        convertFloatsToDoubles(scan.getIntensityValues()));
  }

  /**
   * Builds precursor info based on the current scan. If a new Precursors was detected, a new
   * element is added to the list parameter.
   *
   * @param scan
   * @param buildingInfos Altered during this method. New Infos are added if not part of this list
   * @param currentFrameNumber
   * @param currentScanNumber
   */
  public static void extractImsMsMsInfo(final MzMLMsScan scan,
      @Nonnull List<BuildingImsMsMsInfo> buildingInfos, final int currentFrameNumber,
      final int currentScanNumber) {
    Double lowerWindow = null;
    Double upperWindow = null;
    Double isolationMz = null;
    Integer charge = null;
    Float colissionEnergy = null;
    for (MzMLPrecursorElement precursorElement : scan.getPrecursorList().getPrecursorElements()) {
      Optional<MzMLPrecursorSelectedIonList> selectedIonList =
          precursorElement.getSelectedIonList();
      if (selectedIonList.isPresent()) {
        if (selectedIonList.get().getSelectedIonList().size() > 1) {
          logger.info("Selection of more than one ion in a single scan is not supported.");
        }
        for (MzMLCVParam param : selectedIonList.get().getSelectedIonList().get(0)
            .getCVParamsList()) {
          if (param.getAccession().equals(MzMLCV.cvPrecursorMz)) {
            isolationMz = Double.parseDouble(param.getValue().get());
          }
          if (param.getAccession().equals(MzMLCV.cvChargeState)) {
            charge = Integer.parseInt(param.getValue().get());
          }
        }
      }
      Optional<MzMLIsolationWindow> iw = precursorElement.getIsolationWindow();
      if (iw.isPresent()) {
        for (MzMLCVParam param : iw.get().getCVParamsList()) {
          if (param.getAccession().equals(MzMLCV.cvIsolationWindowLowerOffset)) {
            lowerWindow = Double.parseDouble(param.getValue().get());
            continue;
          }
          if (param.getAccession().equals(MzMLCV.cvIsolationWindowUpperOffset)) {
            upperWindow = Double.parseDouble(param.getValue().get());
            continue;
          }
          if (isolationMz == null && param.getAccession().equals(MzMLCV.cvIsolationWindowTarget)) {
            isolationMz = Double.parseDouble(param.getValue().get());
          }
        }
      }
      MzMLPrecursorActivation activation = precursorElement.getActivation();
      if (activation != null) {
        for (MzMLCVParam param : activation.getCVParamsList()) {
          if (param.getAccession().equals(MzMLCV.cvActivationEnergy)) {
            colissionEnergy = Float.parseFloat(param.getValue().get());
          }
        }
      }
      if (lowerWindow != null && upperWindow != null && isolationMz != null
          && colissionEnergy != null) {
        boolean infoFound = false;
        for (int i = 0; i < buildingInfos.size(); i++) {
          BuildingImsMsMsInfo buildingInfo = buildingInfos.get(i);
          if (Double.compare(isolationMz, buildingInfo.getLargestPeakMz()) == 0
              && Float.compare(colissionEnergy, buildingInfo.getCollisionEnergy()) == 0) {
            buildingInfo.setLastSpectrumNumber(currentScanNumber);
            infoFound = true;
          }
        }
        if (!infoFound) {
          BuildingImsMsMsInfo info = new BuildingImsMsMsInfo(isolationMz,
              Objects.requireNonNullElse(colissionEnergy, ImsMsMsInfo.UNKNOWN_COLISSIONENERGY)
                  .floatValue(),
              Objects.requireNonNullElse(charge, ImsMsMsInfo.UNKNOWN_CHARGE), currentFrameNumber,
              currentScanNumber);
          buildingInfos.add(info);
        }
      }
    }
  }

  /*
   * @Nullable public MzMLMobility getMobility(MsScan scan) { if
   * (!getScanList().getScans().isEmpty()) { for (MzMLCVParam param :
   * getScanList().getScans().get(0).getCVParamsList()) { String accession = param.getAccession();
   * if(param.getValue().isEmpty()) { continue; } switch (accession) { case
   * MzMLCV.cvMobilityDriftTime -> { if
   * (param.getUnitAccession().equals(MzMLCV.cvMobilityDriftTimeUnit)) { return new
   * MzMLMobility(Double.parseDouble(param.getValue().get()), MobilityType.DRIFT_TUBE); } } case
   * MzMLCV.cvMobilityInverseReduced -> { if
   * (param.getUnitAccession().equals(MzMLCV.cvMobilityInverseReducedUnit)) { return new
   * MzMLMobility(Double.parseDouble(param.getValue().get()), MobilityType.TIMS); } } } } } return
   * null; }
   */
}
