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

package io.github.mzmine.modules.dataprocessing.filter_tracereducer;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.ReducedIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonMobilityTraceReducerTask extends AbstractTask {

  private final ModularFeatureList flist;
  private final int totalRows;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final boolean removeOriginal;
  private final String suffix;
  private int processedRows;

  public IonMobilityTraceReducerTask(@Nullable MemoryMapStorage storage,
      @NotNull Date moduleCallDate, ModularFeatureList flist, ParameterSet parameters,
      MZmineProject project) {
    super(storage, moduleCallDate);
    this.flist = flist;
    this.totalRows = flist.getNumberOfRows();
    this.parameters = parameters;
    this.project = project;
    this.processedRows = 0;
    this.suffix = parameters.getParameter(IonMobilityTraceReducerParameters.suffix).getValue();
    this.removeOriginal = parameters.getParameter(IonMobilityTraceReducerParameters.removeOriginal)
        .getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Reducign ion mobility traces in row " + processedRows + "/" + totalRows;
  }

  @Override
  public double getFinishedPercentage() {
    return (double) processedRows / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    ModularFeatureList reducedList = flist.createCopy(flist.getName() + " " + suffix,
        getMemoryMapStorage(), false);

    reducedList.modularStream().forEach(row -> {
      if (isCanceled()) {
        return;
      }
      processRow(row);
      processedRows++;
    });

    if (removeOriginal) {
      project.removeFeatureList(flist);
    }

    reducedList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(IonMobilityTraceReducerModule.class, parameters,
            getModuleCallDate()));
    project.addFeatureList(reducedList);

    setStatus(TaskStatus.FINISHED);
  }

  private static void processRow(ModularFeatureListRow row) {
    for (ModularFeature feature : row.getFeatures()) {
      if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
        IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
        if (featureData instanceof SimpleIonMobilogramTimeSeries imts) {
          IonMobilogramTimeSeries newSeries = new ReducedIonMobilogramTimeSeries(imts);
          feature.set(FeatureDataType.class, newSeries);
        }
      }
    }
  }
}
