/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_mzml;

import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLIsolationWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLPrecursorActivation;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLPrecursorSelectedIonList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ConversionUtils {

  private static Logger logger = Logger.getLogger(ConversionUtils.class.getName());

  public static double[] convertFloatsToDoubles(float[] input, int length) {
    if (input == null) {
      return null; // Or throw an exception - your choice
    }
    double[] output = new double[length];
    for (int i = 0; i < length; i++) {
      output[i] = input[i];
    }
    return output;
  }

  public static double[] convertFloatsToDoubles(float[] input) {
    return convertFloatsToDoubles(input, input.length);
  }

  public static double[] convertIntsToDoubles(int[] input) {
    return convertIntsToDoubles(input, input.length);
  }

  public static double[] convertIntsToDoubles(int[] input, int length) {
    if (input == null) {
      return null; // Or throw an exception - your choice
    }
    double[] output = new double[length];
    for (int i = 0; i < length; i++) {
      output[i] = input[i];
    }
    return output;
  }

  public static double[] convertLongsToDoubles(long[] input, int length) {
    if (input == null) {
      return null; // Or throw an exception - your choice
    }
    double[] output = new double[length];
    for (int i = 0; i < length; i++) {
      output[i] = (double) input[i];
    }
    return output;
  }

  public static MassSpectrumType msdkToMZmineSpectrumType(MassSpectrumType msdk) {
    return switch (msdk) {
      case PROFILE -> MassSpectrumType.PROFILE;
      case CENTROIDED -> MassSpectrumType.CENTROIDED;
      case THRESHOLDED -> MassSpectrumType.THRESHOLDED;
    };
  }

  /**
   * Creates a {@link SimpleScan} from an MSDK scan from MzML import
   *
   * @param rawDataFile
   * @param scan        the scan
   * @return a {@link SimpleScan}
   */
  public static io.github.mzmine.datamodel.Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan) {
    return msdkScanToSimpleScan(rawDataFile, scan, scan.getMzValues(null),
        scan.getIntensityValues(null));
  }

  /**
   * Creates a {@link SimpleScan} from an MSDK scan from MzML import
   *
   * @param rawDataFile
   * @param scan        the scan
   * @param mzs         use these mz values instead of the scan data
   * @param intensities use these intensity values instead of the scan data
   * @return a {@link SimpleScan}
   */
  public static io.github.mzmine.datamodel.Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan, double[] mzs,
      double[] intensities) {
    return msdkScanToSimpleScan(rawDataFile, scan, mzs, intensities,
        ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()));
  }

  /**
   * Creates a {@link SimpleScan} from an MSDK scan from MzML import
   *
   * @param rawDataFile
   * @param scan         the scan
   * @param mzs          use these mz values instead of the scan data
   * @param intensities  use these intensity values instead of the scan data
   * @param spectrumType override spectrum type
   * @return a {@link SimpleScan}
   */
  public static io.github.mzmine.datamodel.Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan, double[] mzs,
      double[] intensities, MassSpectrumType spectrumType) {
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
        scan.getRetentionTime() / 60, precursorMz, precursorCharge, mzs, intensities,
        spectrumType, scan.getPolarity(),
        scan.getScanDefinition(), scan.getScanningRange());

    return newScan;
  }

  public static BuildingMobilityScan msdkScanToMobilityScan(int scannum,
      MzMLMsScan scan) {
    return new BuildingMobilityScan(scannum, scan.getMzValues(null),
        scan.getIntensityValues(null));
  }

  /**
   * Builds precursor info based on the current scan. If a new Precursors was detected, a new
   * element is added to the list parameter.
   *
   * @param scan
   * @param buildingInfos      Altered during this method. New Infos are added if not part of this
   *                           list
   * @param currentFrameNumber
   * @param currentScanNumber
   */
  public static void extractImsMsMsInfo(final MzMLMsScan scan,
      @NotNull List<BuildingImsMsMsInfo> buildingInfos, final int currentFrameNumber,
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
