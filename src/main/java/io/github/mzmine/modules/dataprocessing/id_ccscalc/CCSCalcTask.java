/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.reference.ReferenceCCSCalcParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see CCSCalcModule
 */
public class CCSCalcTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(CCSCalcTask.class.getName());

  private final boolean assumeChargeState;
  private final RangeMap<Double, Integer> rangeChargeMap;
  private final ModularFeatureList[] featureLists;
  private final MZmineProject project;
  private final ParameterSet parameters;
  private final CCSCalculator ccsCalculator;
  private final ParameterSet ccsCalculatorParameters;
  private double percentage;
  private int totalRows;
  private int processedRows;
  private int annotatedFeatures;

  public CCSCalcTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.assumeChargeState = parameters.getParameter(CCSCalcParameters.assumeChargeStage)
        .getValue();
    this.rangeChargeMap = parameters.getParameter(CCSCalcParameters.assumeChargeStage)
        .getEmbeddedParameter().getValue();
    this.featureLists = parameters.getParameter(CCSCalcParameters.featureLists).getValue()
        .getMatchingFeatureLists();
    this.project = project;
    this.parameters = parameters;
    this.ccsCalculator = parameters.getValue(CCSCalcParameters.calibrationType);
    this.ccsCalculatorParameters = parameters.getParameter(CCSCalcParameters.calibrationType)
        .getEmbeddedParameters();

    for (ModularFeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }
    processedRows = 0;
    annotatedFeatures = 0;
  }

  @Override
  public String getTaskDescription() {
    return "Calculating CCS values.";
  }

  @Override
  public double getFinishedPercentage() {
    return percentage;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (ModularFeatureList featureList : featureLists) {

      for (FeatureListRow row : featureList.getRows()) {
        SimpleRegression ccsCalibration;

        List<RawDataFile> rawDataFiles = row.getRawDataFiles();
        for (RawDataFile file : rawDataFiles) {

          ModularFeature feature = (ModularFeature) row.getFeature(file);
          if (feature == null) {
            continue;
          }

          Float mobility = feature.getMobility();
          MobilityType mobilityType = feature.getMobilityUnit();
          double mz = feature.getMZ();

          int charge = feature.getCharge();
          if (charge == 0 && !assumeChargeState) {
            continue;
          } else if (charge == 0 && assumeChargeState) {
            Integer fallbackCharge = rangeChargeMap.get(mz);
            if (fallbackCharge == null) {
              continue;
            }
            charge = fallbackCharge;
          }

          Float ccs = ccsCalculator.calcCCS(mz, mobility, charge, ccsCalibration);
          if (ccs != null) {
            feature.setCCS(ccs);
            annotatedFeatures++;
          }
        }

        if (isCanceled()) {
          return;
        }
        processedRows++;
        percentage = totalRows / (double) processedRows;
      }

      featureList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(CCSCalcModule.class, parameters, getModuleCallDate()));
    }

    logger.info("Annotated " + annotatedFeatures + " features with CCS values.");
    setStatus(TaskStatus.FINISHED);
  }

  private List<CCSCalibrant> findCalibrants(FeatureList flist) throws IOException {
    final File file = ccsCalculatorParameters.getParameter(ReferenceCCSCalcParameters.referenceList)
        .getValue();
    final FileReader fileReader = new FileReader(file);
    final String[][] content = CSVParser.parse(fileReader);

    List<ImportType> importTypes = findLineIds(
        List.of(new ImportType(true, "mz", DataTypes.get(MZType.class)), //
            new ImportType(true, "mobility", DataTypes.get(
                io.github.mzmine.datamodel.features.types.numbers.MobilityType.class)), //
            new ImportType(true, "ccs", DataTypes.get(CCSType.class)), //
            new ImportType(true, "charge", DataTypes.get(ChargeType.class))), content[0]);

    final int mzIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("mz"))
        .findFirst().get().getColumnIndex();
    final int mobilityIndex = importTypes.stream()
        .filter(t -> t.getCsvColumnName().equals("mobility")).findFirst().get().getColumnIndex();
    final int ccsIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("ccs"))
        .findFirst().get().getColumnIndex();
    final int chargeIndex = importTypes.stream().filter(t -> t.getCsvColumnName().equals("charge"))
        .findFirst().get().getColumnIndex();

    final int numCalibrants = content.length - 1; // first line is headers
    final double[] calibrantMasses = new double[numCalibrants];
    final float[] calibrantCCS = new float[numCalibrants];
    final int[] calibrantCharges = new int[numCalibrants];
    final float[] calibrantMobilities = new float[numCalibrants];

    for (int i = 0; i < numCalibrants; i++) {
      try {
        calibrantMasses[i] = Double.parseDouble(content[i + 1][mzIndex]);
        calibrantCCS[i] = Float.parseFloat(content[i + 1][ccsIndex]);
        calibrantMobilities[i] = Float.parseFloat(content[i + 1][mobilityIndex]);
        calibrantCharges[i] = Math.abs(Integer.parseInt(content[i + 1][chargeIndex]));
      } catch (NumberFormatException e) {
        setErrorMessage("Cannot parse line " + (i + 1) + "library file.");
        setStatus(TaskStatus.ERROR);
        return null;
      }
    }

    final MobilityTolerance mobTol = ccsCalculatorParameters.getParameter(
        ReferenceCCSCalcParameters.mobTolerance).getValue();
    final MZTolerance mzTol = ccsCalculatorParameters.getParameter(
        ReferenceCCSCalcParameters.mzTolerance).getValue();
    final Range<Float> rtRange = RangeUtils.toFloatRange(
        ccsCalculatorParameters.getParameter(ReferenceCCSCalcParameters.rtRange).getValue());
    final double minHeight = ccsCalculatorParameters.getValue(ReferenceCCSCalcParameters.minHeight);

    final List<FeatureListRow> rowsByMz = flist.stream()
        .sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList();
    List<CCSCalibrant> calibrants = new ArrayList<>();
    for (int i = 0; i < numCalibrants; i++) {
      final Range<Float> mobRange = mobTol.getToleranceRange(calibrantMobilities[i]);
      final Range<Double> mzRange = mzTol.getToleranceRange(calibrantMasses[i]);
      final List<FeatureListRow> rows = FeatureListUtils.getRows(rowsByMz, rtRange, mzRange,
          mobRange, true).stream().filter(r -> r.getAverageHeight() > minHeight).toList();
      final FeatureListRow calibrantRow = FeatureListUtils.getBestRow(rows, mzRange, null,
          mobRange);
      if (calibrantRow != null) {
        final CCSCalibrant calibrant = new CCSCalibrant(calibrantRow, calibrantMasses[i],
            calibrantMobilities[i], calibrantCCS[i], calibrantCharges[i]);
        calibrants.add(calibrant);
        int finalI = i;
        logger.finest(() -> String.format("Found calibrant %d: %s", finalI, calibrant.toString()));
      }
    }

    return calibrants;
  }

  private List<ImportType> findLineIds(List<ImportType> importTypes, String[] firstLine) {

    List<ImportType> lines = new ArrayList<>();
    for (ImportType importType : importTypes) {
      if (importType.isSelected()) {
        ImportType type = new ImportType(importType.isSelected(), importType.getCsvColumnName(),
            importType.getDataType());
        lines.add(type);
      }
    }

    for (ImportType importType : lines) {
      for (int i = 0; i < firstLine.length; i++) {
        String columnName = firstLine[i];
        if (columnName.trim().equalsIgnoreCase(importType.getCsvColumnName().trim())) {
          if (importType.getColumnIndex() != -1) {
            setErrorMessage("Library file contains two columns called \"" + columnName + "\".");
            setStatus(TaskStatus.ERROR);
          }
          importType.setColumnIndex(i);
        }
      }
    }
    final List<ImportType> nullMappings = lines.stream().filter(val -> val.getColumnIndex() == -1)
        .toList();
    if (!nullMappings.isEmpty()) {
      setErrorMessage("Did not find specified column " + Arrays.toString(
          nullMappings.stream().map(ImportType::getCsvColumnName).toArray()) + " in file.");
      setStatus(TaskStatus.ERROR);
    }

    return lines;
  }

  private SimpleRegression getDriftTimeMzRegression(List<CCSCalibrant> calibrants) {
    if (calibrants.size() < 2) {
      return null;
    }

    // https://pubs.rsc.org/en/content/articlelanding/2015/AN/C5AN00991J
    // t_D = ß * gamma * ccs + t_fix
    // gamma = 1/charge * sqrt(ionmass /(ionmass + m_driftgas))

    // to do: determine ß and tfix by linear regression of drifttime vs gamma*ccs
    final SimpleRegression dtMzRegression = new SimpleRegression();
    for (CCSCalibrant calibrant : calibrants) {
      dtMzRegression.addData(calibrant.getN2Gamma() * calibrant.libraryCCS(),
          calibrant.row().getAverageMobility());
    }

    return dtMzRegression;
  }

}
