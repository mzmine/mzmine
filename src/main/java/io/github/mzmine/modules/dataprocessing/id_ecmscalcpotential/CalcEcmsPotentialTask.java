/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.PotentialType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public class CalcEcmsPotentialTask extends AbstractTask {

  /**
   * tubing length in mm
   */
  private final double tubingLength;
  /**
   * tubing id in mm
   */
  private final double tubingId;
  /**
   * flow rate in uL/min
   */
  private final double flowRate;
  /**
   * potential ramp speed in mV/s
   */
  private final double potentialRampSpeed;
  private final double potentialAssignmentPercentage;
  private final ModularFeatureList[] flists;

  private final ParameterSet parameters;
  private final MZmineProject project;
  private final int numRows;
  private int processed;

  protected CalcEcmsPotentialTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.parameters = parameters;
    this.project = project;

    flists = parameters.getValue(CalcEcmsPotentialParameters.flists).getMatchingFeatureLists();
    tubingLength = parameters.getValue(CalcEcmsPotentialParameters.tubingLengthMM);
    tubingId = parameters.getValue(CalcEcmsPotentialParameters.tubingIdMM);
    flowRate = parameters.getValue(CalcEcmsPotentialParameters.flowRateMicroLiterPerMin);
    potentialRampSpeed = parameters.getValue(CalcEcmsPotentialParameters.potentialRampSpeed);
    potentialAssignmentPercentage = parameters.getValue(CalcEcmsPotentialParameters.potentialAssignmentIntensityPercentage);
    numRows = Arrays.stream(flists).mapToInt(ModularFeatureList::getNumberOfRows).sum();
  }

  @Override
  public String getTaskDescription() {
    return "Calculating compound potentials for row " + processed + "/" + numRows + ":";
  }

  @Override
  public double getFinishedPercentage() {
    return processed / (double) numRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final double tubingVolumeMicroL = EcmsUtils.getTubingVolume(tubingLength, tubingId);
    final double delayTimeSeconds = EcmsUtils.getDelayTime(flowRate / 60d, tubingVolumeMicroL);

    for (ModularFeatureList flist : flists) {
      for (FeatureListRow row : flist.getRows()) {
        final Feature f = row.getBestFeature();
        if (f == null || f.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          processed++;
          continue;
        }

        final IonTimeSeries<? extends Scan> eic = f.getFeatureData();
        final Scan bestScan = f.getRepresentativeScan();
        final int bestScanIndex = eic.getSpectra().indexOf(bestScan);
        if (bestScanIndex == -1) {
          processed++;
          continue;
        }

        final double height = eic.getIntensity(bestScanIndex);
        final double minHeight = height * potentialAssignmentPercentage;
        for (int i = bestScanIndex; i > 0; i--) {
          if (eic.getIntensity(i) < minHeight) {
//            final double rt = MathUtils.twoPointGetXForY(eic.getRetentionTime(i),
//                eic.getIntensity(i), eic.getRetentionTime(i + 1), eic.getIntensity(i + 1),
//                minHeight);

            // potential in mV
            final double potential = EcmsUtils.getPotentialAtRt(eic.getRetentionTime(i),
                delayTimeSeconds, potentialRampSpeed);
            row.set(PotentialType.class, (float) potential / 1000);
            break;
          }
        }
        processed++;
      }
      flist.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(CalcEcmsPotentialModule.class, parameters,
              getModuleCallDate()));
    }

    setStatus(TaskStatus.FINISHED);
  }
}
