/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_common;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.align_gc.GCAlignerTask;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerTask;
import io.github.mzmine.modules.dataprocessing.align_join.RowVsRowScore;
import io.github.mzmine.taskcontrol.Task;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

public interface FeatureRowAlignScorer {

  static final Logger logger = Logger.getLogger(FeatureRowAlignScorer.class.getName());
  /**
   * Align rows on base rows
   *
   * @param unalignedRows  score and align these rows against base
   * @param baseRowsSorted the base rows that alignments will be added. Sorted by RT for
   *                       {@link GCAlignerTask} and by mz for {@link JoinAlignerTask}
   * @return all RowVsRowScores
   */
  default Collection<RowVsRowScore> alignRowsOnBaseRows(final Task parentTask,
      List<List<FeatureListRow>> unalignedRows, List<FeatureListRow> baseRowsSorted) {

    // key = a row to be aligned, value = all possible matches in the aligned fl and it's scores
    final ConcurrentLinkedDeque<RowVsRowScore> scoresList = new ConcurrentLinkedDeque<>();

    // stream all rows in all feature lists
    final long scoredRows = unalignedRows.stream().flatMap(Collection::stream).parallel()
        .mapToLong(rowToAdd -> {
          if (parentTask.isCanceled()) {
            return 1L;
          }

          scoreRowAgainstBaseRows(baseRowsSorted, rowToAdd, scoresList);
          return 1L;
        }).sum();

    logger.finest(() -> String.format("Scored %d/%d rows", scoredRows, unalignedRows.size()));
    return scoresList;
  }

  /**
   * @param baseRowsSorted the base rows to be scored against. sorting may differ between
   *                       implementations
   * @param rowToAdd       row to score
   * @param scoresList     scores are added here
   */
  void scoreRowAgainstBaseRows(List<FeatureListRow> baseRowsSorted, FeatureListRow rowToAdd,
      ConcurrentLinkedDeque<RowVsRowScore> scoresList);

  void calculateAlignmentScores(ModularFeatureList alignedFeatureList,
      List<FeatureList> originalFeatureLists);
}
