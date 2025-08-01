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

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import com.google.common.collect.Range;
import com.opencsv.exceptions.CsvException;
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
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
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
      case DRIFT_TUBE, TRAVELING_WAVE ->
          file.getCCSCalibration() != null ? file.getCCSCalibration().getCCS(mz, charge, mobility)
              : null;
      case TIMS ->
          file.getCCSCalibration() != null ? file.getCCSCalibration().getCCS(mz, charge, mobility)
              : calcCCSFromTimsMobility(mobility.doubleValue(), charge, mz);
      case NONE, FAIMS, MIXED, OTHER, SLIM -> logUnsupportedMobilityUnit();
    };
  }

  /**
   * @param file Represents a raw data file @return The {@link IMSRawDataFile#getMobilityType()} and
   *             {@link IMSRawDataFile#getCCSCalibration()} of this data file.
   * @return true if it has the valid mobility type and the calibration.
   * @author https://github.com/Tarush-Singh35
   */
  public static boolean hasValidMobilityType(@NotNull IMSRawDataFile file) {
    //Valid Mobility Check for CCS calculation in the function
    if (file.getMobilityType() == MobilityType.TIMS) {
      return true;
    } else {
      return (file.getMobilityType() == MobilityType.DRIFT_TUBE
          || file.getMobilityType() == MobilityType.TRAVELING_WAVE)
          && file.getCCSCalibration() != null;
    }
  }

  /**
   * Uses Bruker's library to calculate the ccs for a given tims mobility.
   *
   * @author https://github.com/SteffenHeu
   */
  public static Float calcCCSFromTimsMobility(double mobility, int charge, double mz) {
    return tdfUtils.calculateCCS(mobility, charge, mz);
  }

  public static Float logUnsupportedMobilityUnit() {
    logger.fine("Unsupported mobility unit");
    return null;
  }

  /**
   * @param file A file containing colums titled "mz", "mobility", "ccs", "charge" for given library
   *             compounds. Delimiter must be ";"
   * @return A list of {@link CCSCalibrant}s or null.
   * @throws IOException while trying to read file
   */
  public static List<CCSCalibrant> getCalibrantsFromCSV(final File file)
      throws IOException, CsvException {
    final FileReader fileReader = new FileReader(file);
    final List<String[]> content = CSVParsingUtils.readData(file, ";");
    fileReader.close();

    List<ImportType<?>> importTypes = CSVParsingUtils.findLineIds(
        List.of(new ImportType<>(true, "mz", DataTypes.get(MZType.class)), //
            new ImportType<>(true, "mobility", DataTypes.get(
                io.github.mzmine.datamodel.features.types.numbers.MobilityType.class)), //
            new ImportType<>(true, "ccs", DataTypes.get(CCSType.class)), //
            new ImportType<>(true, "charge", DataTypes.get(ChargeType.class))), content.getFirst(),
        new SimpleStringProperty());

    if (importTypes == null) {
      return null;
    }

    final int mzIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("mz"))
        .findFirst().orElseThrow().getColumnIndex();
    final int mobilityIndex = importTypes.stream()
        .filter(t -> t.getCsvColumnName().equals("mobility")).findFirst().orElseThrow()
        .getColumnIndex();
    final int ccsIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("ccs"))
        .findFirst().orElseThrow().getColumnIndex();
    final int chargeIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("charge"))
        .findFirst().orElseThrow().getColumnIndex();

    final int numCalibrants = content.size() - 1; // first line is headers
    List<CCSCalibrant> calibrants = new ArrayList<>();

    for (int i = 0; i < numCalibrants; i++) {
      try {
        calibrants.add(new CCSCalibrant(null, null, //
            Double.parseDouble(content.get(i + 1)[mzIndex]), // mz
            Float.parseFloat(content.get(i + 1)[mobilityIndex]), // mobility
            Float.parseFloat(content.get(i + 1)[ccsIndex]), // ccs
            Integer.parseInt(content.get(i + 1)[chargeIndex]))); // charge
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

      final List<FeatureListRow> candidates = FeatureListUtils.getCandidatesWithinRanges(mzRange,
          rtRange, mobRange, rowsByMz, true).stream().filter(
          r -> r.getMaxHeight() > minHeight && Objects.equals(
              r.getBestFeature().getRepresentativeScan().getPolarity(),
              PolarityType.fromInt(potentialCalibrant.libraryCharge()))).toList();
      final FeatureListRow calibrantRow = FeatureListUtils.getBestRow(candidates, mzRange, null,
          mobRange, null, 1, 1, 1, 1).orElse(null);

      if (calibrantRow != null) {
        potentialCalibrant.setFoundMobility(calibrantRow.getAverageMobility());
        potentialCalibrant.setFoundMz(calibrantRow.getAverageMZ());
        detectedCalibrants.add(potentialCalibrant);
        int finalI = i;
        logger.finest(() -> String.format("Found calibrant %d: %s", finalI, potentialCalibrant));
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
