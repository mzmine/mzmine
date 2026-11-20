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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Counts the kinds of junk the shape based diagnostic cannot see.
 * <p>
 * {@link ShapeScoreDiagnostic} rejects features on their chromatographic shape, which catches noisy
 * traces and unsplit peak pairs. It is blind to the failure mode a low detection threshold actually
 * produces: a spurious feature at low intensity is a small, clean, well fitted bump. It passes the
 * R² bar, it is not a double peak, and it barely changes direction.
 * <p>
 * These counts use properties that separate signal from noise without looking at the peak shape at
 * all - whether the feature reproduces across injections, whether it has an isotope partner, and
 * how intense it is. Diagnostic only: nothing here feeds a score or a constraint. The point is to
 * find out, from runs that are happening anyway, whether the low noise optima the optimizer keeps
 * choosing are in fact full of marginal detections.
 */
public final class PrecisionDiagnostic {

  /**
   * Share of rows found in exactly one file. Noise does not reproduce between injections and real
   * signal usually does, so on a set of replicates this is the most direct measure available of how
   * much of a result list is spurious.
   */
  public static final String ATTR_SINGLE_FILE_PERCENT = "Single file rows / %";

  /**
   * Share of rows whose best feature carries no isotope partner. A real small molecule almost
   * always shows at least a 13C peak; noise does not.
   */
  public static final String ATTR_NO_ISOTOPE_PERCENT = "Rows without isotopes / %";

  /**
   * Median height of a row, reported raw rather than compared against a threshold. A threshold
   * derived from the result list would move with the very parameters being tuned, which is the trap
   * the shape rejection limit fell into.
   */
  public static final String ATTR_MEDIAN_HEIGHT = "Median row height";

  /**
   * Height at the lower end of the distribution, so a shift of the whole result list towards
   * marginal detections is visible even when the median holds up.
   */
  public static final String ATTR_LOW_HEIGHT = "Row height q10";

  private PrecisionDiagnostic() {
  }

  /**
   * @param featureList the aligned result list of one evaluation
   */
  public static @NotNull Result evaluate(@NotNull FeatureList featureList) {
    final int fileCount = featureList.getNumberOfRawDataFiles();
    int rows = 0;
    int singleFile = 0;
    int withoutIsotopes = 0;
    final double[] heights = new double[featureList.getNumberOfRows()];

    for (final FeatureListRow row : featureList.getRows()) {
      heights[rows] = row.getMaxHeight();
      rows++;

      // assumption: a row present in one file only. With a single file dataset this cannot
      // discriminate, so the share is reported as zero rather than as 100 %.
      if (fileCount > 1 && row.getNumberOfFeatures() == 1) {
        singleFile++;
      }

      final Feature best = row.getBestFeature();
      final IsotopePattern pattern = best != null ? best.getIsotopePattern() : null;
      if (pattern == null || pattern.getNumberOfDataPoints() < 2) {
        withoutIsotopes++;
      }
    }

    Arrays.sort(heights, 0, rows);
    final double median =
        rows == 0 ? 0d : MathUtils.calcQuantileSorted(Arrays.copyOf(heights, rows), 0.5);
    final double low =
        rows == 0 ? 0d : MathUtils.calcQuantileSorted(Arrays.copyOf(heights, rows), 0.1);

    return new Result(rows, fileCount, singleFile, withoutIsotopes, median, low);
  }

  /**
   * @param rows            rows in the result list
   * @param fileCount       files the list was aligned across; the single file share is only
   *                        meaningful above one
   * @param singleFile      rows detected in exactly one file
   * @param withoutIsotopes rows whose best feature has no isotope partner
   */
  public record Result(int rows, int fileCount, int singleFile, int withoutIsotopes,
                       double medianHeight, double lowHeight) {

    public double singleFilePercent() {
      return percentOf(singleFile);
    }

    public double withoutIsotopesPercent() {
      return percentOf(withoutIsotopes);
    }

    private double percentOf(int count) {
      return rows == 0 ? 0d : Math.round(1000d * count / rows) / 10d;
    }

    @Override
    public @NotNull String toString() {
      return "%d rows across %d files: %.1f %% single file, %.1f %% without isotopes, median height %.4g".formatted(
          rows, fileCount, singleFilePercent(), withoutIsotopesPercent(), medianHeight);
    }
  }
}
