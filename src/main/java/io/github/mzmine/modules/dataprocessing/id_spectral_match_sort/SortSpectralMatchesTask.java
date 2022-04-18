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

package io.github.mzmine.modules.dataprocessing.id_spectral_match_sort;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class SortSpectralMatchesTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final FeatureList featureList;
  private final Boolean filterByMinScore;
  private final ParameterSet parameters;
  private int totalRows;
  private int finishedRows;
  private Double minScore;

  SortSpectralMatchesTask(FeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureList = featureList;
    this.parameters = parameters;
    filterByMinScore =
        parameters.getParameter(SortSpectralMatchesParameters.minScore).getValue();
    minScore = parameters.getParameter(SortSpectralMatchesParameters.minScore)
        .getEmbeddedParameter().getValue();
    if (minScore == null) {
      minScore = 0d;
    }
  }

  /**
   * Sort database matches by score
   *
   * @param row
   */
  public static void sortIdentities(@NotNull FeatureListRow row) {
    sortIdentities(row, false, 0d);
  }

  /**
   * Sort database matches by score
   *
   * @param row
   * @param filterMinSimilarity
   * @param minScore
   */
  public static void sortIdentities(@NotNull FeatureListRow row, boolean filterMinSimilarity,
      double minScore) {
    // filter for SpectralDBFeatureIdentity and write to map
    List<SpectralDBAnnotation> matches = row.getSpectralLibraryMatches();
    if (matches == null || matches.isEmpty()) {
      return;
    }

    // reversed order: by similarity score
    matches = matches.stream()
        .filter(m -> !filterMinSimilarity || m.getSimilarity().getScore() >= minScore)
        .sorted(Comparator.comparingDouble(SpectralDBAnnotation::getScore).reversed())
        .collect(Collectors.toList());

    // set sorted list
    row.setSpectralLibraryMatch(matches);
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return finishedRows / (float) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Sort spectral library matches in " + featureList;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    totalRows = featureList.getNumberOfRows();
    finishedRows = 0;

    for (FeatureListRow row : featureList.getRows()) {
      if (isCanceled()) {
        return;
      }
      // sort identities of row
      sortIdentities(row, filterByMinScore, minScore);
      finishedRows++;
    }

    // Add task description to peakList
    featureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Sorted spectral library matches search ",
        SortSpectralMatchesModule.class, parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }

}
