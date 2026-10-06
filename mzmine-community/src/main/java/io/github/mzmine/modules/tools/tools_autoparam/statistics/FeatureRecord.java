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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @param file a specific raw file or null. if null, {@link #getBestMatch(List)} only checks row,
 *             not raw file. If not null, {@link #getNumMatches(List)} will only return the number
 *             of matches if the feature is also present in the given file.
 */
public record FeatureRecord(@Nullable RawDataFile file, double mz, float rt,
                            @Nullable Float mobility) {

  private static final MZTolerance mzTol = new MZTolerance(0.01, 0);
  private static final RTTolerance rtTol = new RTTolerance(0.08f, Unit.MINUTES);
  private static final MobilityTolerance mobTol = new MobilityTolerance(1f);

  public FeatureRecord(Feature f) {
    this(f.getRawDataFile(), f.getMZ(), f.getRT(), f.getMobility());
  }

  public boolean isPresent(List<FeatureListRow> mzSortedRows) {
    return getBestMatch(mzSortedRows) != null;
  }

  public int getNumMatches(List<FeatureListRow> mzSortedRows) {
    final FeatureListRow best = getBestMatch(mzSortedRows);
    return best != null ? best.getNumberOfFeatures() : 0;
  }

  public @Nullable FeatureListRow getBestMatch(List<FeatureListRow> mzSortedRows) {
    final Range<Double> mzRange = mzTol.getToleranceRange(mz);
    final Range<Float> rtRange = rtTol.getToleranceRange(rt);
    final Range<Float> mobRange = mobility != null ? mobTol.getToleranceRange(mobility) : null;

    final IndexRange mzIndexRange = BinarySearch.indexRange(mz() - 0.5, mz() + 0.5, mzSortedRows,
        FeatureListRow::getAverageMZ);
    final List<FeatureListRow> sublist = new ArrayList<>(mzIndexRange.sublist(mzSortedRows, true));
    sublist.sort(Comparator.comparing(FeatureListRow::getAverageRT));

    final IndexRange rtIndexRange = BinarySearch.indexRange(rt() - 0.1, rt() + 0.1, sublist,
        FeatureListRow::getAverageRT);

    FeatureListRow best = null;
    double bestScore = 0;

    for (FeatureListRow row : rtIndexRange.sublist(sublist, true)) {
      final double score = FeatureListUtils.getAlignmentScore(row.getAverageMZ(),
          row.getAverageRT(), row.getAverageMobility(), row.getAverageCCS(), mzRange, rtRange,
          mobRange, null, 1, 1, 1, 1);
      if (score > bestScore && (file == null || row.getFeature(file) != null)) {
        bestScore = score;
        best = row;
      }
    }
    return best;
  }
}
