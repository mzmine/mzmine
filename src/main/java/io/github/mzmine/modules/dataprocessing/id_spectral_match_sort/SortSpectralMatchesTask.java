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
