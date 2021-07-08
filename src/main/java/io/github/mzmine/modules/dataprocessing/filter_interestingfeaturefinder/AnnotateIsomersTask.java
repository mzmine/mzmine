/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.PossibleIsomerType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class AnnotateIsomersTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(AnnotateIsomersTask.class.getName());

  private final MZmineProject project;
  private final ParameterSet parameters;
  private final ModularFeatureList flist;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final double maxChangePercentage;
  private final boolean requireSingleRaw = true;
  private String description;
  private int processed = 0;
  private int totalRows;
  private final MobilityTolerance multimerRecognitionTolerance;
  private final boolean refineByIIN;


  public AnnotateIsomersTask(MemoryMapStorage storage, @NotNull MZmineProject project,
      @NotNull ParameterSet parameters, ModularFeatureList flist) {
    super(storage);

    this.project = project;
    this.parameters = parameters;
    this.flist = flist;

    totalRows = flist.getNumberOfRows();
    description = "Searching for isomeric features in " + flist.getName() + ".";

    mzTolerance = parameters
        .getParameter(parameters.getParameter(AnnotateIsomersParameters.mzTolerance)).getValue();
    rtTolerance = parameters.getParameter(AnnotateIsomersParameters.rtTolerance).getValue();
    maxChangePercentage = parameters.getParameter(AnnotateIsomersParameters.maxMobilityChange)
        .getValue();
    refineByIIN = parameters.getParameter(AnnotateIsomersParameters.multimerRecognitionTolerance)
        .getValue();
    multimerRecognitionTolerance = parameters
        .getParameter(AnnotateIsomersParameters.multimerRecognitionTolerance).getEmbeddedParameter()
        .getValue();

    // todo maximum mobility difference
    // todo check if it's a fragmented multimer
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return processed / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final List<ModularFeatureListRow> rowsByMz = flist.modularStream()
        .sorted(Comparator.comparingDouble(ModularFeatureListRow::getAverageMZ)).toList();

    // sort by decreasing intensity
//    final List<ModularFeatureListRow> rowsByIntensity = flist.modularStream().sorted(
//        (row1, row2) -> -1 * Double
//            .compare(row1.getMaxDataPointIntensity(), row2.getMaxDataPointIntensity())).toList();

    for (final ModularFeatureListRow row : rowsByMz) {
      var possibleRows = FeatureListUtils
          .getRows(rowsByMz, rtTolerance.getToleranceRange(row.getAverageRT()),
              mzTolerance.getToleranceRange(row.getAverageMZ()), true);

      processed++;
      possibleRows.remove(row);

      if (possibleRows.isEmpty()) {
        continue;
      }

      final float refMobility = row.getAverageMobility();
      final Iterator<ModularFeatureListRow> rowIterator = possibleRows.iterator();

      while (rowIterator.hasNext()) {
        ModularFeatureListRow possibleRow = rowIterator.next();
        final float mobility = possibleRow.getAverageMobility();

        final double percChange =
            1 - Math.min(mobility, refMobility) / Math.max(mobility, refMobility);
        if (percChange > maxChangePercentage) {
          rowIterator.remove();
        }

      }

      if (refineByIIN) {
        refineResultsByIIN(row, possibleRows);
      }

      if (possibleRows.isEmpty()) {
        continue;
      }

      row.set(PossibleIsomerType.class,
          possibleRows.stream().map(ModularFeatureListRow::getID).toList());

      if(isCanceled()) {
        return;
      }
    }

    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(AnnotateIsomersModule.class, parameters));
    setStatus(TaskStatus.FINISHED);
  }

  private void refineResultsByIIN(@NotNull final ModularFeatureListRow row,
      @NotNull List<ModularFeatureListRow> possibleIsomery) {
    if (!row.hasIonIdentity()) {
      return;
    }
/*
    // Identität in einem IIN
    final IonIdentity ionIdentity = row.getBestIonIdentity();

    // [2M+ACN+Na]+
    ionIdentity.getAdduct(); // -> "[2M+ACN+Na]+"
    ionIdentity.getIonType(); // Kombiniert modification mit adduct / [2M+ACN+Na]+
    ionIdentity.getIonType().getModification(); // -> ACN
    ionIdentity.getIonType().getAdduct(); // -> Na+ / Adduct bringt Ladung auf Molekül
    ionIdentity.getIonType().getMolecules(); // 2 <- Anzahl von M

    // [2M-H+2Na]+
    ionIdentity.getIonType().getAdduct(); // -> 2Na-H
    ionIdentity.getIonType().getAdduct().contains(IonModification.NA); // -> 2Na-H*/

    final Float rowMobility = row.getAverageMobility();
    final IonIdentity rowIdentity = row.getBestIonIdentity();
    final int rowMoleculeCount = rowIdentity.getIonType().getMolecules();

    if (rowIdentity == null) {
      return;
    }

    if(("" + row.getAverageMZ()).contains("344.11")) {
      logger.finest("test");
    }
    // todo this checks the wrong thing.
    final IonNetwork network = rowIdentity.getNetwork();
    for (Entry<FeatureListRow, IonIdentity> entry : network.entrySet()) {
      final ModularFeatureListRow networkRow = (ModularFeatureListRow) entry.getKey();

      final IonIdentity networkIndentity = entry.getValue();

      final IonType networkIonType = networkIndentity.getIonType();
      if (networkIonType.getMolecules() > rowMoleculeCount && multimerRecognitionTolerance
          .checkWithinTolerance(rowMobility, networkRow.getAverageMobility())) {
        if(networkIndentity.getIonType().getAdduct()
            .contains(rowIdentity.getIonType().getAdduct())) {
          /*logger.finest(String.format("Adduct 1: %s\tAdduct 2: %s\t",
              rowIdentity.getIonType().getAdduct().getParsedName(),
              networkIndentity.getIonType().getAdduct().getParsedName()));*/
          /*logger.finest(() -> String
              .format("m/z %.4f (%s) is a monomer of m/z %.4f (%s)", row.getAverageMZ(),
                  rowIdentity.toString(), networkRow.getAverageMZ(), networkIndentity.toString()));*/
          logger.finest("Contains: " + networkIndentity.getIonType().getAdduct()
              .contains(rowIdentity.getIonType().getAdduct()));
//          logger.finest();

          if(possibleIsomery.remove(networkRow)) {
            logger.finest("Removed");
          }
        }

        var pars1 = FormulaUtils.parseFormula(rowIdentity.toString());
        var pars2 = FormulaUtils.parseFormula(networkIndentity.toString());
      }
    }
  }
}
