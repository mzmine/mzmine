package io.github.mzmine.modules.io.rawdataimport.fileformats.mzml_msdk;

import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleScan;

public class ConversionUtils {

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

  public static Scan msdkScanToSimpleScan(RawDataFile rawDataFile, MsScan scan) {
    return new SimpleScan(rawDataFile, scan.getScanNumber(), scan.getMsLevel(),
        scan.getRetentionTime(), 0, 0, scan.getMzValues(),
        convertFloatsToDoubles(scan.getIntensityValues()),
        ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
        ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
        scan.getScanningRange());
  }

  public static MobilityScan msdkScanToMobilityScan(IMSRawDataFile rawDataFile, int scannum, MsScan scan,
      Frame frame) {
    return new SimpleMobilityScan(rawDataFile, scannum, frame, scan.getMzValues(),
        convertFloatsToDoubles(scan.getIntensityValues()));
  }

  /*@Nullable
  public MzMLMobility getMobility(MsScan scan) {
    if (!getScanList().getScans().isEmpty()) {
      for (MzMLCVParam param : getScanList().getScans().get(0).getCVParamsList()) {
        String accession = param.getAccession();
        if(param.getValue().isEmpty()) {
          continue;
        }
        switch (accession) {
          case MzMLCV.cvMobilityDriftTime -> {
            if (param.getUnitAccession().equals(MzMLCV.cvMobilityDriftTimeUnit)) {
              return new MzMLMobility(Double.parseDouble(param.getValue().get()),
                  MobilityType.DRIFT_TUBE);
            }
          }
          case MzMLCV.cvMobilityInverseReduced -> {
            if (param.getUnitAccession().equals(MzMLCV.cvMobilityInverseReducedUnit)) {
              return new MzMLMobility(Double.parseDouble(param.getValue().get()),
                  MobilityType.TIMS);
            }
          }
        }
      }
    }
    return null;
  }*/
}
