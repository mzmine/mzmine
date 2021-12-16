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

import com.google.common.collect.RangeMap;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
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
  private final boolean createNewFeatureList;
  private final MZmineProject project;
  private final ParameterSet parameters;
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
    this.createNewFeatureList = parameters.getParameter(CCSCalcParameters.createNewFeatureList)
        .getValue();
    this.project = project;
    this.parameters = parameters;

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

    ModularFeatureList workingFeatureList = null;
    for (ModularFeatureList featureList : featureLists) {
      if (createNewFeatureList) {
        workingFeatureList = featureList
            .createCopy(featureList.getName() + " CCS", getMemoryMapStorage(), false);
      } else {
        workingFeatureList = featureList;
      }

      for (FeatureListRow row : workingFeatureList.getRows()) {
        List<RawDataFile> rawDataFiles = row.getRawDataFiles();
        for (RawDataFile file : rawDataFiles) {

          ModularFeature feature = (ModularFeature) row.getFeature(file);
          if (feature == null) {
            continue;
          }

          Float mobility = feature.getMobility();
          MobilityType mobilityType = feature.getMobilityUnit();
          double mz = feature.getMZ();
          if (mobility == null || mobilityType == null) {
            continue;
          }

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

          Float ccs = CCSUtils.calcCCS(mz, mobility, mobilityType, charge);
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

      workingFeatureList.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(CCSCalcModule.class, parameters,
              getModuleCallDate()));
      if (workingFeatureList != featureList) {
        project.addFeatureList(workingFeatureList);
      }
    }

    logger.info("Annotated " + annotatedFeatures + " features with CCS values.");
    setStatus(TaskStatus.FINISHED);
  }
}
