/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
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
  private final double minIntensity;
  private final int minTraceDatapoints;
  private final boolean requireSingleRaw = true;
  private String description;
  private AtomicInteger processed = new AtomicInteger(0);
  private int totalRows;
  private final MobilityTolerance multimerRecognitionTolerance;
  private final boolean refineByIIN;


  public AnnotateIsomersTask(MemoryMapStorage storage, @NotNull MZmineProject project,
      @NotNull ParameterSet parameters, ModularFeatureList flist, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

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
    final ParameterSet qualityParam = parameters
        .getParameter(AnnotateIsomersParameters.qualityParam).getValue();
    minTraceDatapoints = qualityParam.getParameter(IsomerQualityParameters.minDataPointsInTrace)
        .getValue();
    minIntensity = qualityParam.getParameter(IsomerQualityParameters.minIntensity).getValue();

    // todo maximum mobility difference
    // todo check if it's a fragmented multimer
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return processed.get() / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final List<ModularFeatureListRow> rowsByMz = flist.modularStream()
        .sorted(Comparator.comparingDouble(ModularFeatureListRow::getAverageMZ)).toList();
    flist.addRowType(new PossibleIsomerType());

    // sort by decreasing intensity
//    final List<ModularFeatureListRow> rowsByIntensity = flist.modularStream().sorted(
//        (row1, row2) -> -1 * Double
//            .compare(row1.getMaxDataPointIntensity(), row2.getMaxDataPointIntensity())).toList();

    rowsByMz.parallelStream().forEach(row -> {
      if (isCanceled()) {
        return;
      }

      processed.getAndIncrement();

      if (row.getAverageMZ() > 212.127 && row.getAverageMZ() < 212.129) {
        logger.info("blub");
      }

      Integer maxRowDp = IonMobilityUtils.getMaxNumTraceDatapoints(row);
      if (row.getMaxDataPointIntensity() < minIntensity || maxRowDp == null
          || maxRowDp < minTraceDatapoints) {
        return;
      }

      var possibleRows = FeatureListUtils
          .getRows(rowsByMz, rtTolerance.getToleranceRange(row.getAverageRT()),
              mzTolerance.getToleranceRange(row.getAverageMZ()), true);

      float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;

      for (ModularFeatureListRow rowz : possibleRows) {
        min = Math.min(rowz.getAverageRT(), min);
        max = Math.max(rowz.getAverageRT(), max);
      }

      possibleRows.remove(row);
      if (possibleRows.isEmpty()) {
        return;
      }

      final float refMobility = row.getAverageMobility();
      final Iterator<ModularFeatureListRow> rowIterator = possibleRows.iterator();

      while (rowIterator.hasNext()) {
        ModularFeatureListRow possibleRow = rowIterator.next();
        final Float mobility = possibleRow.getAverageMobility();

        if(!rtTolerance.checkWithinTolerance(row.getAverageRT(), possibleRow.getAverageRT())) {
          logger.info("blub");
        }

        if (mobility == null) {
          continue;
        }

        final double percChange =
            1 - Math.min(mobility, refMobility) / Math.max(mobility, refMobility);
        if (percChange > maxChangePercentage) {
          rowIterator.remove();
        }
      }

      if (possibleRows.isEmpty()) {
        return;
      }

      refineByQuality(possibleRows);
      if (possibleRows.isEmpty()) {
        return;
      }

      if (refineByIIN) {
        refineByIIN(row, possibleRows);
      }

      if (possibleRows.isEmpty()) {
        return;
      }

      row.set(PossibleIsomerType.class,
          possibleRows.stream().map(ModularFeatureListRow::getID).toList());

      var isomerIds = new ArrayList<>(row.get(PossibleIsomerType.class));
      final List<ModularFeatureListRow> isomerRows = new ArrayList<>();
      isomerRows.addAll(isomerIds.stream()
          .<ModularFeatureListRow>map(id -> (ModularFeatureListRow) flist.findRowByID(id))
          .filter(r -> r != null).distinct().toList());
      if(!possibleRows.containsAll(isomerRows)) {
        logger.info("wrong");
      }
    });

    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(AnnotateIsomersModule.class, parameters, getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  private void refineByQuality(List<ModularFeatureListRow> possibleRows) {
    List<ModularFeatureListRow> rowsToRemove = new ArrayList<>();

    for (ModularFeatureListRow possibleRow : possibleRows) {
      Integer maxRowDp = IonMobilityUtils.getMaxNumTraceDatapoints(possibleRow);
      if (possibleRow.getMaxDataPointIntensity() < minIntensity || maxRowDp == null
          || maxRowDp < minTraceDatapoints) {
        rowsToRemove.add(possibleRow);
      }
    }

    possibleRows.removeAll(rowsToRemove);
  }

  private boolean isFragmentOfMultimer(@NotNull final ModularFeatureListRow row) {
    if (row.getBestIonIdentity() == null || row.getAverageMobility() == null) {
      return false;
    }

    final IonIdentity identity = row.getBestIonIdentity();
    final IonNetwork network = identity.getNetwork();
    final int rowMoleculeCount = identity.getIonType().getMolecules();
    final Float rowMobility = row.getAverageMobility();

    for (Entry<FeatureListRow, IonIdentity> entry : network.entrySet()) {
      final ModularFeatureListRow networkRow = (ModularFeatureListRow) entry.getKey();
      if (row.equals(networkRow)) {
        continue;
      }

      final IonIdentity networkIdentity = entry.getValue();
      final IonType networkIonType = networkIdentity.getIonType();

      if (networkIonType.getMolecules() > rowMoleculeCount && multimerRecognitionTolerance
          .checkWithinTolerance(rowMobility, networkRow.getAverageMobility())) {
        if (networkIdentity.getIonType().getAdduct().contains(identity.getIonType().getAdduct())) {
          logger.finest(() -> String
              .format("m/z %.4f (%s, %.4f %s) is a fragment of multimer m/z %.4f (%s, %.4f %s)",
                  row.getAverageMZ(), identity.toString(), row.getAverageMobility(),
                  row.getBestFeature().getMobilityUnit().getUnit(), networkRow.getAverageMZ(),
                  networkIdentity.toString(), networkRow.getAverageMobility(),
                  networkRow.getBestFeature().getMobilityUnit().getUnit()));
          return true;
        }
      }
    }
    return false;
  }

  private void refineByIIN(@NotNull final ModularFeatureListRow row,
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
    if (rowIdentity == null || rowMobility == null) {
      return;
    }
    final int rowMoleculeCount = rowIdentity.getIonType().getMolecules();

    final List<ModularFeatureListRow> notIsomers = new ArrayList<>();
    final IonNetwork network = rowIdentity.getNetwork();
    for (final ModularFeatureListRow isomerRow : possibleIsomery) {
      final IonIdentity isomerIdentity = network.get(isomerRow);
      if (isomerIdentity == null) {
        continue;
      }

      if (isFragmentOfMultimer(isomerRow)) {
        // is just a fragmented multimer, remove from possibleIsomers.
        notIsomers.add(isomerRow);
      }
    }
    possibleIsomery.removeAll(notIsomers);
  }

  /*private boolean checkRT(List<ModularFeatureListRow> rows) {
    float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;

    for (ModularFeatureListRow row : rows) {
      min = Math.min(row.getAverageRT(), min);
      max = Math.max(row.getAverageRT(), max);
    }

    if(max - min > 1) {
      return false;
    }
    return true;
  }*/
}
