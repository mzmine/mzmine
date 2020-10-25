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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.sort;

import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.datamodel.data.SimpleFeatureListAppliedMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class SortSpectralDBIdentitiesTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final FeatureList featureList;
  private int totalRows;
  private int finishedRows;

  private Boolean filterByMinScore;
  private Double minScore;

  private ParameterSet parameters;

  SortSpectralDBIdentitiesTask(FeatureList featureList, ParameterSet parameters) {
    this.featureList = featureList;
    this.parameters = parameters;
    filterByMinScore =
        parameters.getParameter(SortSpectralDBIdentitiesParameters.minScore).getValue();
    minScore = parameters.getParameter(SortSpectralDBIdentitiesParameters.minScore)
        .getEmbeddedParameter().getValue();
    if (minScore == null)
      minScore = 0d;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return finishedRows / totalRows;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Sort spectral database identities of data base search in " + featureList;
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
        "Sorted spectral database identities of DB search ", parameters));

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Sort database matches by score
   *
   * @param row
   */
  public static void sortIdentities(FeatureListRow row) {
    sortIdentities(row, false, 1.0d);
  }

  /**
   * Sort database matches by score
   *
   * @param row
   * @param filterMinSimilarity
   * @param minScore
   */
  public static void sortIdentities(FeatureListRow row, boolean filterMinSimilarity, double minScore) {
    // get all row identities
    PeakIdentity[] identities = row.getPeakIdentities();
    if (identities == null || identities.length == 0)
      return;

    // filter for SpectralDBPeakIdentity and write to map
    List<SpectralDBPeakIdentity> match = new ArrayList<>();

    for (PeakIdentity identity : identities) {
      if (identity instanceof SpectralDBPeakIdentity) {
        row.removePeakIdentity(identity);
        if (!filterMinSimilarity
            || ((SpectralDBPeakIdentity) identity).getSimilarity().getScore() >= minScore)
          match.add((SpectralDBPeakIdentity) identity);
      }
    }
    if (match.isEmpty())
      return;

    // reversed order: by similarity score
    match.sort((a, b) -> {
      return Double.compare(b.getSimilarity().getScore(), a.getSimilarity().getScore());
    });

    for (SpectralDBPeakIdentity entry : match) {
      row.addPeakIdentity(entry, false);
    }
    row.setPreferredPeakIdentity(match.get(0));
  }

}
