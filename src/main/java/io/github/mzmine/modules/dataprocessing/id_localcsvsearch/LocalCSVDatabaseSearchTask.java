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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

class LocalCSVDatabaseSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MobilityTolerance mobTolerance;
  private final Double ccsTolerance;
  private final File dataBaseFile;
  private final String fieldSeparator;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final ParameterSet parameters;
  private final List<ImportType> importTypes;

  private String[][] databaseValues;
  private int finishedLines = 0;
  private FeatureList peakList;

  LocalCSVDatabaseSearchTask(FeatureList peakList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.peakList = peakList;
    this.parameters = parameters;

    dataBaseFile = parameters.getParameter(LocalCSVDatabaseSearchParameters.dataBaseFile)
        .getValue();
    fieldSeparator = parameters.getParameter(LocalCSVDatabaseSearchParameters.fieldSeparator)
        .getValue();
    importTypes = parameters.getParameter(LocalCSVDatabaseSearchParameters.columns).getValue();
    mzTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.rtTolerance).getValue();
    mobTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.mobTolerance)
        .getValue();
    ccsTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.ccsTolerance)
        .getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (databaseValues == null) {
      return 0;
    }
    return ((double) finishedLines) / databaseValues.length;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Peak identification of " + peakList + " using database " + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {
      // read database contents in memory
      FileReader dbFileReader = new FileReader(dataBaseFile);
      databaseValues = CSVParser.parse(dbFileReader, fieldSeparator.charAt(0));

      List<ImportType> lineIds = findLineIds(importTypes, databaseValues[0]);

//      peakList.addRowType(new CompoundDatabaseMatchesType());

      for (; finishedLines < databaseValues.length; finishedLines++) {
        if (isCanceled()) {
          dbFileReader.close();
          return;
        }
        try {
          processOneLine(databaseValues[finishedLines], lineIds);
        } catch (Exception e) {
          // ignore incorrect lines
        }
      }
      dbFileReader.close();

    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }

    // Add task description to peakList
    peakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Peak identification using database " + dataBaseFile,
            LocalCSVDatabaseSearchModule.class, parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

  }

  private void processOneLine(String values[], List<ImportType> linesWithIndices) {

    var formulaType = new FormulaType();
    var compoundNameType = new CompoundNameType();
    var commentType = new CommentType();
    var mzType = new MZType();
    var rtType = new RTType();
    var mobType = new MobilityType();
    var ccsType = new CCSType();
    var smilesType = new SmilesStructureType();
    var adductType = new IonAdductType();

    final Map<DataType<?>, String> entry = new HashMap<>();

    for (int i = 0; i < linesWithIndices.size(); i++) {
      var type = linesWithIndices.get(i);
      entry.put(type.getDataType(), values[type.getColumnIndex()]);
    }

//    lineID = entry.get();
    String lineName = entry.get(compoundNameType);
    String lineFormula = entry.get(formulaType);
    String lineComment = entry.get(commentType);
    String lineAdduct = entry.get(adductType);
    Double lineMZ = (entry.get(mzType) != null) ? Double.parseDouble(entry.get(mzType)) : null;
    Double lineRT = (entry.get(rtType) != null) ? Double.parseDouble(entry.get(rtType)) : null;
    Double lineMob = (entry.get(mobType) != null) ? Double.parseDouble(entry.get(mobType)) : null;
    Double lineCCS = (entry.get(ccsType) != null) ? Double.parseDouble(entry.get(ccsType)) : null;
    String smiles = entry.get(smilesType);

    Range<Double> mzRange =
        lineMZ != null && Double.compare(lineMZ, 0d) != 0 ? mzTolerance.getToleranceRange(lineMZ)
            : null;
    Range<Float> rtRange =
        lineRT != null && Double.compare(lineRT, 0d) != 0 ? rtTolerance.getToleranceRange(
            lineRT.floatValue()) : null;
    Range<Float> mobRange =
        lineMob != null && Double.compare(lineMob, 0d) != 0 ? mobTolerance.getToleranceRange(
            lineMob.floatValue()) : null;
    Range<Float> ccsRange = lineCCS != null && Double.compare(lineCCS, 0d) != 0 ? Range.closed(
        (float) (lineCCS - lineCCS * ccsTolerance), (float) (lineCCS + lineCCS * ccsTolerance))
        : null;

    for (FeatureListRow peakRow : peakList.getRows()) {

      boolean mzMatches = mzRange == null || mzRange.contains(peakRow.getAverageMZ());
      boolean rtMatches = rtRange == null || rtRange.contains(peakRow.getAverageRT());
      boolean ccsMatches =
          ccsRange == null || (peakRow.getAverageCCS() != null && ccsRange.contains(
              peakRow.getAverageCCS()));
      boolean mobMatches =
          mobRange == null || (peakRow.getAverageMobility() != null && mobRange.contains(
              peakRow.getAverageMobility()));

      if (mzMatches && rtMatches && mobMatches && ccsMatches) {

        logger.finest("Found compound " + lineName + " (m/z " + lineMZ + ", RT " + lineRT + ")");

        final CompoundDBIdentity newIdentity = new CompoundDBIdentity(lineName, lineFormula,
            dataBaseFile.getName(), null);
        newIdentity.setPropertyValue(FeatureIdentity.PROPERTY_SMILES, smiles);
        newIdentity.setPropertyValue(FeatureIdentity.PROPERTY_COMMENT, lineComment);
        newIdentity.setPropertyValue(FeatureIdentity.PROPERTY_METHOD,
            LocalCSVDatabaseSearchModule.MODULE_NAME);
        newIdentity.setPropertyValue(FeatureIdentity.PROPERTY_ADDUCT, lineAdduct);
        newIdentity.setPropertyValue(FeatureIdentity.PROPERTY_CCS,
            lineCCS != null ? String.valueOf(lineCCS) : null);
        newIdentity.setPropertyValue(FeatureIdentity.PROPERTY_MOBILITY,
            lineMob != null ? String.valueOf(lineMob) : null);
        // add new identity to the row
        peakRow.addCompoundAnnotation(newIdentity);
      }
    }
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
        if (columnName.equals(importType.getCsvColumnName())) {
          if (importType.getColumnIndex() != -1) {
            setErrorMessage(
                "Library file " + dataBaseFile.getAbsolutePath() + " contains two columns called \""
                    + columnName + "\".");
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
          nullMappings.stream().map(ImportType::getCsvColumnName).toArray()) + " in file "
          + dataBaseFile.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
    }

    return lines;
  }
}
