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

import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLIsolationWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorActivation;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorSelectedIonList;
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

  public static MassSpectrumType msdkToMZmineSpectrumType(MsSpectrumType msdk) {
    return switch (msdk) {
      case PROFILE -> MassSpectrumType.PROFILE;
      case CENTROIDED -> MassSpectrumType.CENTROIDED;
      case THRESHOLDED -> MassSpectrumType.THRESHOLDED;
    };
  }

  public static PolarityType msdkToMZminePolarityType(io.github.msdk.datamodel.PolarityType msdk) {
    return switch (msdk) {
      case POSITIVE -> PolarityType.POSITIVE;
      case NEGATIVE -> PolarityType.NEGATIVE;
      default -> PolarityType.UNKNOWN;
    };
  }

  /**
   * Creates a {@link SimpleScan} from an MSDK scan from MzML import
   *
   * @param rawDataFile
   * @param scan        the scan
   * @return a {@link SimpleScan}
   */
  public static Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan) {
    return msdkScanToSimpleScan(rawDataFile, scan, scan.getMzValues(),
        convertFloatsToDoubles(scan.getIntensityValues()));
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
  public static Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan, double[] mzs,
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
  public static Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MzMLMsScan scan, double[] mzs,
      double[] intensities, MassSpectrumType spectrumType) {
    DDAMsMsInfo info = null;
    if (scan.getPrecursorList() != null) {
      final var precursorElements = scan.getPrecursorList().getPrecursorElements();
      if (precursorElements.size() == 1) {
        info = DDAMsMsInfoImpl.fromMzML(precursorElements.get(0), scan.getMsLevel());
      } else if (precursorElements.size() > 1) {
        info = MSnInfoImpl.fromMzML(precursorElements, scan.getMsLevel());
      }
    }

    final SimpleScan newScan = new SimpleScan(rawDataFile, scan.getScanNumber(), scan.getMsLevel(),
        scan.getRetentionTime() / 60, info, mzs, intensities, spectrumType,
        ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
        scan.getScanningRange());

    return newScan;
  }

  public static BuildingMobilityScan msdkScanToMobilityScan(int scannum, MsScan scan) {
    return new BuildingMobilityScan(scannum, scan.getMzValues(),
        convertFloatsToDoubles(scan.getIntensityValues()));
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
      Optional<MzMLPrecursorSelectedIonList> selectedIonList = precursorElement.getSelectedIonList();
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
              Objects.requireNonNullElse(colissionEnergy, PasefMsMsInfo.UNKNOWN_COLISSIONENERGY)
                  .floatValue(), Objects.requireNonNullElse(charge, PasefMsMsInfo.UNKNOWN_CHARGE),
              currentFrameNumber, currentScanNumber);
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
