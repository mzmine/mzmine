/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package import_data.speed;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything that is measured once per iteration instead of once per batch step. Repeated on every
 * exported row of the iteration, like the number of raw data files.
 * <p>
 * The feature list numbers are the result fingerprint: a branch that is faster because it produced
 * fewer rows or fewer features is a regression, not a win, and without these columns that would not
 * be visible in the report.
 *
 * @param featureLists        number of feature lists in the project after the batch
 * @param rows                rows of the newest feature list
 * @param features            features summed over the rows of the newest feature list
 * @param tempDirFreeGBBefore usable space of the temp directory volume before the batch
 * @param tempDirFreeGBAfter  usable space of the temp directory volume after the batch
 * @param tempDirUsedGB       before - after, so how much space the batch consumed. This is the
 *                            whole volume, so other processes writing to the same disk distort it,
 *                            and temp files that mzmine released again make it smaller than the
 *                            peak.
 * @param peakHeapGB          highest used heap seen by {@link MemorySampler}, null when memory
 *                            tracking was off
 * @param gcCount             garbage collections during the iteration, null when tracking was off
 * @param gcTimeSeconds       time the collectors reported during the iteration, null when tracking
 *                            was off
 */
public record SpeedIterationStats(int featureLists, int rows, int features,
                                  double tempDirFreeGBBefore, double tempDirFreeGBAfter,
                                  double tempDirUsedGB, @Nullable Double peakHeapGB,
                                  @Nullable Long gcCount, @Nullable Double gcTimeSeconds) {

  /**
   * @param freeGBBefore the usable temp dir space measured before the batch started
   * @param memory       null when memory tracking is off
   */
  @NotNull
  public static SpeedIterationStats after(final double freeGBBefore,
      @Nullable final MemoryMeasurement memory) {
    final List<FeatureList> featureLists = ProjectService.getProject().getCurrentFeatureLists();
    // the newest feature list is the result of the last processing step
    final FeatureList newest = featureLists.isEmpty() ? null : featureLists.getLast();
    final int rows = newest == null ? 0 : newest.getNumberOfRows();
    final int features = newest == null ? 0
        : newest.getRows().stream().mapToInt(FeatureListRow::getNumberOfFeatures).sum();

    final double freeGBAfter = tempDirFreeGB();
    return new SpeedIterationStats(featureLists.size(), rows, features, freeGBBefore, freeGBAfter,
        round3(freeGBBefore - freeGBAfter), memory == null ? null : memory.peakHeapGB(),
        memory == null ? null : memory.gcCount(), memory == null ? null : memory.gcTimeSeconds());
  }

  /**
   * Usable space of the volume that holds the mzmine temp directory.
   */
  public static double tempDirFreeGB() {
    final File tempDir = FileAndPathUtil.getTempDir();
    return round3(tempDir.getUsableSpace() / 1e9);
  }

  private static double round3(final double value) {
    return Math.round(value * 1e3) / 1e3;
  }
}
