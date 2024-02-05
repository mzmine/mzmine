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

package io.github.mzmine.modules.io.import_rawdata_mzml;

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
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.BuildingMzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCVParam;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLIsolationWindow;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorActivation;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorElement;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLPrecursorSelectedIonList;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ConversionUtils {

  private static final Logger logger = Logger.getLogger(ConversionUtils.class.getName());

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


  public static Scan mzmlScanToSimpleScan(final RawDataFile dataFile,
      final BuildingMzMLMsScan scan) {
    return mzmlScanToSimpleScan(dataFile, scan, scan.getSpectrumType());
  }

  public static Scan mzmlScanToSimpleScan(final RawDataFile dataFile, final BuildingMzMLMsScan scan,
      final MassSpectrumType spectrumType) {
    return mzmlScanToSimpleScan(dataFile, scan, scan.getDoubleBufferMzValues(),
        scan.getDoubleBufferIntensityValues(), spectrumType);
  }

  /**
   * Creates a {@link SimpleScan} from an MSDK scan from MzML import
   *
   * @param scan         the scan
   * @param mzs          use these mz values instead of the scan data
   * @param intensities  use these intensity values instead of the scan data
   * @param spectrumType override spectrum type
   * @return a {@link SimpleScan}
   */
  public static Scan mzmlScanToSimpleScan(RawDataFile rawDataFile, BuildingMzMLMsScan scan,
      DoubleBuffer mzs, DoubleBuffer intensities, MassSpectrumType spectrumType) {
    DDAMsMsInfo info = null;
    if (scan.getPrecursorList() != null) {
      final var precursorElements = scan.getPrecursorList().getPrecursorElements();
      if (precursorElements.size() == 1) {
        info = DDAMsMsInfoImpl.fromMzML(precursorElements.get(0), scan.getMSLevel());
      } else if (precursorElements.size() > 1) {
        info = MSnInfoImpl.fromMzML(precursorElements, scan.getMSLevel());
      }
    }

    Float injTime = scan.getInjectionTime();

    final SimpleScan newScan = new SimpleScan(rawDataFile, scan.getScanNumber(), scan.getMSLevel(),
        scan.getRetentionTime(), info, mzs, intensities, spectrumType, scan.getPolarity(),
        scan.getScanDefinition(), scan.getScanningMZRange(), injTime);

    return newScan;
  }

  public static BuildingMobilityScan mzmlScanToMobilityScan(int scannum, BuildingMzMLMsScan scan) {
    SimpleSpectralArrays data = scan.getMobilityScanSimpleSpectralData();
    return new BuildingMobilityScan(scannum, data);
  }

  /**
   * Builds precursor info based on the current scan. If a new Precursors was detected, a new
   * element is added to the list parameter.
   *
   * @param buildingInfos      Altered during this method. New Infos are added if not part of this
   *                           list
   * @param currentFrameNumber the IMS frame
   * @param currentScanNumber  the IMS scan number
   */
  public static void extractImsMsMsInfo(final BuildingMzMLMsScan scan,
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
            charge = Integer.parseInt(param.getValue().orElse("0"));
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
        for (BuildingImsMsMsInfo buildingInfo : buildingInfos) {
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
