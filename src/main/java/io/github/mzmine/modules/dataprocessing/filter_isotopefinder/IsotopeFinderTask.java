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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IsotopesUtils;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 *
 */
class IsotopeFinderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IsotopeFinderTask.class.getName());
  private final ModularFeatureList featureList;
  // parameter values
  private final ParameterSet parameters;
  private final MZTolerance isotopesMzTolerance;
  private final int isotopeMaxCharge;
  private final List<Element> isotopeElements;

  private int processedRows, totalRows;

  IsotopeFinderTask(MZmineProject project, ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Date moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);

    this.featureList = featureList;
    this.parameters = parameters;

    isotopeElements = parameters.getParameter(IsotopeFinderParameters.elements).getValue();
    isotopeMaxCharge = parameters.getParameter(IsotopeFinderParameters.maxCharge).getValue();
    isotopesMzTolerance = parameters.getParameter(IsotopeFinderParameters.isotopeMzTolerance)
        .getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Isotope pattern finder on " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0d;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running isotope pattern finder on " + featureList);

    // We assume source peakList contains one datafile
    if (featureList.getRawDataFiles().size() > 1) {
      setErrorMessage("Cannot perform isotope finder on aligned feature list.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Update isotopesMzDiffs
    List<Double> isoMzDiffs = IsotopesUtils.getIsotopesMzDiffs(isotopeElements, isotopeMaxCharge);
    if (isoMzDiffs.isEmpty()) {
      setErrorMessage(
          "No isotopes found for elements: " + isotopeElements.stream().map(Objects::toString)
              .collect(
                  Collectors.joining(",")));
      setStatus(TaskStatus.ERROR);
      return;
    }
    double maxIsoMzDiff = Collections.max(isoMzDiffs);

    // Loop through all peaks
    totalRows = featureList.getNumberOfRows();
    processedRows = 0;

    RawDataFile raw = featureList.getRawDataFile(0);
    ScanDataAccess scans = EfficientDataAccess
        .of(raw, ScanDataType.CENTROID, featureList.getSeletedScans(raw));

    // find for all rows the isotope pattern
    for (FeatureListRow row : featureList.getRows()) {
      if (isCanceled()) {
        return;
      }
      // find pattern in FWHM

      processedRows++;
    }

    // Add task description to peakList
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Isotope finder module",
            IsotopeFinderModule.class, parameters, getModuleCallDate()));

    logger.info("Finished isotope pattern finder on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

}
