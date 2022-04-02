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

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.CCSCalibrant;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.FeatureListUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;

/**
 * @see CCSCalcModule
 */
public class CCSUtils {

  // If K0 can be determined, we could also calculate the CCS directly via the formula shown in
  // https://analyticalsciencejournals.onlinelibrary.wiley.com/doi/pdf/10.1002/mas.21585 Fig 2.
  // could be an option for TIMS

  private static final Logger logger = Logger.getLogger(CCSUtils.class.getName());
  private static final TDFUtils tdfUtils = new TDFUtils();

  private CCSUtils() {
  }

  /**
   * @return The calculated CCS value or null if no calibration information is available.
   */
  public static Float calcCCS(double mz, @NotNull Float mobility,
      @NotNull MobilityType mobilityType, int charge, @NotNull IMSRawDataFile file) {
    return switch (mobilityType) {
      case DRIFT_TUBE, TRAVELING_WAVE -> file.getCCSCalibration() != null ? file.getCCSCalibration()
          .getCCS(mz, charge, mobility) : null;
      case TIMS -> file.getCCSCalibration() != null ? file.getCCSCalibration()
          .getCCS(mz, charge, mobility)
          : calcCCSFromTimsMobility(mobility.doubleValue(), charge, mz);
      case NONE, FAIMS, MIXED -> logUnsupportedMobilityUnit();
    };
  }

  public static boolean hasValidMobilityType(@NotNull IMSRawDataFile file) {
    //Valid Mobility Check for CCS calculation in the function
    if (file.getMobilityType() == MobilityType.TIMS
        || file.getMobilityType() == MobilityType.DRIFT_TUBE
        || file.getMobilityType() == MobilityType.TRAVELING_WAVE) {
      return true;
    }
    return false;
  }

  /**
   * Uses Bruker's library to calculate the ccs for a given tims mobility.
   *
   * @author https://github.com/SteffenHeu
   */
  public static Float calcCCSFromTimsMobility(double mobility, int charge, double mz) {
    return tdfUtils.calculateCCS(mobility, charge, mz).floatValue();
  }

  public static Float logUnsupportedMobilityUnit() {
    logger.fine("Unsupported mobility unit");
    return null;
  }

  /**
   * @param file A file containing colums titled "mz", "mobility", "ccs", "charge" for given library
   *             compounds. Delimiter must be ";"
   * @return A list of {@link CCSCalibrant}s or null.
   * @throws IOException
   */
  public static List<CCSCalibrant> getCalibrantsFromCSV(final File file) throws IOException {
    final FileReader fileReader = new FileReader(file);
    final String[][] content = CSVParser.parse(fileReader, ';');
    fileReader.close();

    List<ImportType> importTypes = CSVParsingUtils.findLineIds(
        List.of(new ImportType(true, "mz", DataTypes.get(MZType.class)), //
            new ImportType(true, "mobility", DataTypes.get(
                io.github.mzmine.datamodel.features.types.numbers.MobilityType.class)), //
            new ImportType(true, "ccs", DataTypes.get(CCSType.class)), //
            new ImportType(true, "charge", DataTypes.get(ChargeType.class))), content[0]);

    if (importTypes == null) {
      return null;
    }

    final int mzIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("mz"))
        .findFirst().get().getColumnIndex();
    final int mobilityIndex = importTypes.stream()
        .filter(t -> t.getCsvColumnName().equals("mobility")).findFirst().get().getColumnIndex();
    final int ccsIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("ccs"))
        .findFirst().get().getColumnIndex();
    final int chargeIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("charge"))
        .findFirst().get().getColumnIndex();

    final int numCalibrants = content.length - 1; // first line is headers
    List<CCSCalibrant> calibrants = new ArrayList<>();

    for (int i = 0; i < numCalibrants; i++) {
      try {
        calibrants.add(new CCSCalibrant(null, null, //
            Double.parseDouble(content[i + 1][mzIndex]), // mz
            Float.parseFloat(content[i + 1][mobilityIndex]), // mobility
            Float.parseFloat(content[i + 1][ccsIndex]), // ccs
            Integer.parseInt(content[i + 1][chargeIndex]))); // charge
      } catch (NumberFormatException e) {
        final int finalI = i;
        logger.warning(
            () -> String.format("Cannot parse line %d in calibrant library file.", finalI + 1));
      }
    }

    return calibrants;
  }

  public static List<CCSCalibrant> findCalibrants(FeatureList flist, List<CCSCalibrant> calibrants,
      MZTolerance mzTol, Range<Float> rtRange, MobilityTolerance mobTol, double minHeight) {

    final List<FeatureListRow> rowsByMz = flist.stream()
        .filter(r -> rtRange.contains(r.getAverageRT()))
        .sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList();

    List<CCSCalibrant> detectedCalibrants = new ArrayList<>();
    for (int i = 0; i < calibrants.size(); i++) {
      final CCSCalibrant potentialCalibrant = calibrants.get(i);
      final Range<Float> mobRange = mobTol.getToleranceRange(potentialCalibrant.libraryMobility());
      final Range<Double> mzRange = mzTol.getToleranceRange(potentialCalibrant.libraryMz());

      final List<FeatureListRow> rows = FeatureListUtils.getRows(rowsByMz, rtRange, mzRange,
              mobRange, true).stream().filter(
              r -> r.getAverageHeight() > minHeight && r.getBestFeature().getRepresentativeScan()
                  .getPolarity().equals(PolarityType.fromInt(potentialCalibrant.libraryCharge())))
          .toList();
      final FeatureListRow calibrantRow = FeatureListUtils.getBestRow(rows, mzRange, null,
          mobRange);

      if (calibrantRow != null) {
        var calibrant = potentialCalibrant;
        calibrant.setFoundMobility(calibrantRow.getAverageMobility());
        calibrant.setFoundMz(calibrantRow.getAverageMZ());
        detectedCalibrants.add(calibrant);
        int finalI = i;
        logger.finest(() -> String.format("Found calibrant %d: %s", finalI, calibrant));
      }
    }

    return detectedCalibrants;
  }

  public static SimpleRegression getDriftTimeMzRegression(List<CCSCalibrant> calibrants) {
    if (calibrants.size() < 2) {
      return null;
    }

    // Calculation according to: Analyst, 2015,140, 6834-6844
    // https://pubs.rsc.org/en/content/articlelanding/2015/AN/C5AN00991J
    // t_D = ß * gamma * ccs + t_fix
    // gamma = 1/charge * sqrt(ionmass /(ionmass + m_driftgas))

    final SimpleRegression dtMzRegression = new SimpleRegression();
    for (CCSCalibrant calibrant : calibrants) {
      dtMzRegression.addData(calibrant.getN2Gamma() * calibrant.libraryCCS(),
          calibrant.foundMobility());
    }

    return dtMzRegression;
  }
}
