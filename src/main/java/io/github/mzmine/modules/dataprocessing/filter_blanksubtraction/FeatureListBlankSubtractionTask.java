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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class FeatureListBlankSubtractionTask extends AbstractTask {

  private static Logger logger = Logger
      .getLogger(FeatureListBlankSubtractionTask.class.getName());
  private final int totalRows;
  private final int minBlankDetections;
  private final String suffix;
  private final boolean checkFoldChange;
  private final double foldChange;
  private final BlankIntensityType intensityType;

  private AtomicInteger processedRows = new AtomicInteger(0);
  private MZmineProject project;
  private FeatureListBlankSubtractionParameters parameters;
  private RawDataFilesSelection blankSelection;
  private List<RawDataFile> blankRaws;
  private ModularFeatureList alignedFeatureList;

  public FeatureListBlankSubtractionTask(MZmineProject project,
      FeatureListBlankSubtractionParameters parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;
    this.blankSelection =
        parameters.getParameter(FeatureListBlankSubtractionParameters.blankRawDataFiles).getValue();
    this.blankRaws = List.of(blankSelection.getMatchingRawDataFiles().clone());
    this.alignedFeatureList =
        parameters.getParameter(FeatureListBlankSubtractionParameters.alignedPeakList).getValue()
            .getMatchingFeatureLists()[0];
    this.minBlankDetections =
        parameters.getParameter(FeatureListBlankSubtractionParameters.minBlanks).getValue();
    this.suffix = parameters.getParameter(FeatureListBlankSubtractionParameters.suffix).getValue();
    checkFoldChange = parameters.getParameter(FeatureListBlankSubtractionParameters.foldChange)
        .getValue();
    foldChange = parameters.getParameter(FeatureListBlankSubtractionParameters.foldChange)
        .getEmbeddedParameter().getValue();
    intensityType = BlankIntensityType.Maximum;
    totalRows = alignedFeatureList.getNumberOfRows();

    setStatus(TaskStatus.WAITING);
    logger.setLevel(Level.FINEST);
  }

  @Override
  public String getTaskDescription() {
    return "Blank subtraction task on " + alignedFeatureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return processedRows.get() / (double)totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (!checkBlankSelection(alignedFeatureList, blankRaws)) {
      setErrorMessage("Feature list " + alignedFeatureList.getName()
          + " does no contain all selected blank raw data files.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // get the files that are not considered as blank
    final List<RawDataFile> nonBlankFiles = new ArrayList<>();
    for (RawDataFile file : alignedFeatureList.getRawDataFiles()) {
      if (!blankRaws.contains(file)) {
        nonBlankFiles.add(file);
      }
    }
    logger.finest(() -> alignedFeatureList.getName() + " contains " + nonBlankFiles.size() + " raw data files not classified as blank.");

    final ModularFeatureList result = new ModularFeatureList(
        alignedFeatureList.getName() + " " + suffix, getMemoryMapStorage(), nonBlankFiles);
    alignedFeatureList.getRowTypes().values().forEach(result::addRowType);
    nonBlankFiles.forEach(f -> result.setSelectedScans(f, alignedFeatureList.getSeletedScans(f)));

    Set<ModularFeatureListRow> filteredRows = ConcurrentHashMap.newKeySet();
    alignedFeatureList.modularStream()/*.parallel()*/.forEach(row -> {
      int numBlankDetections = 0;
      for (RawDataFile blankRaw : blankRaws) {
        if (row.hasFeature(blankRaw)) {
          numBlankDetections++;
        }
      }

      if (numBlankDetections < minBlankDetections || checkFoldChange) {
        final ModularFeatureListRow filteredRow = new ModularFeatureListRow(result, row.getID());
        final double blankIntensity =
            checkFoldChange ? getBlankIntensity(row, blankRaws, intensityType) : 1d;
        int numFeatures = 0;
        for (RawDataFile file : nonBlankFiles) {
          final ModularFeature f = row.getFeature(file);
          // check if there's actually a feature
          if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN) {
            // check validity
            if (!checkFoldChange || f.getHeight() / blankIntensity >= foldChange) {
              filteredRow.addFeature(file, new ModularFeature(result, f));
              numFeatures++;
            }
          }
        }
        // copy row types
        if(numFeatures > 0) {
//          row.stream().filter(e -> !(e.getKey() instanceof FeaturesType))
//              .forEach(entry -> filteredRow.set(entry.getKey(), entry.getValue()));
          filteredRows.add(filteredRow);
        }
      }

      processedRows.getAndIncrement();
    });

    filteredRows.forEach(result::addRow);

    result.getAppliedMethods().addAll(alignedFeatureList.getAppliedMethods());
    result.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FeatureListBlankSubtractionModule.class, parameters, getModuleCallDate()));
    project.addFeatureList(result);

    setStatus(TaskStatus.FINISHED);
  }

  private double getBlankIntensity(ModularFeatureListRow row, Collection<RawDataFile> blankRaws,
      BlankIntensityType intensityType) {
    double intensity = 0d;
    int numDetections = 0;

    for (RawDataFile file : blankRaws) {
      final ModularFeature f = row.getFeature(file);
      if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN) {
        if (intensityType == BlankIntensityType.Average) {
          intensity += f.getHeight();
          numDetections++;
        } else if (intensityType == BlankIntensityType.Maximum) {
          intensity = Math.max(f.getHeight(), intensity);
        }
      }
    }

    return intensityType == BlankIntensityType.Average && numDetections != 0 ? intensity
        / numDetections : intensity;
  }

  private boolean checkBlankSelection(FeatureList aligned, List<RawDataFile> blankRaws) {

    List<RawDataFile> flRaws = aligned.getRawDataFiles();

    for (int i = 0; i < blankRaws.size(); i++) {
      boolean contained = false;

      for (RawDataFile flRaw : flRaws) {
        if (blankRaws.get(i) == flRaw) {
          contained = true;
        }
      }

      if (!contained) {
        final int i1 = i;
        logger.info(() -> "Feature list " + aligned.getName() + " does not contain raw data files "
            + blankRaws.get(i1).getName());
        return false;
      }
    }

    logger.finest(
        () -> "Feature list " + aligned.getName() + " contains all selected blank raw data files.");
    return true;
  }

  enum BlankIntensityType {
    Average, Maximum
  }
}
