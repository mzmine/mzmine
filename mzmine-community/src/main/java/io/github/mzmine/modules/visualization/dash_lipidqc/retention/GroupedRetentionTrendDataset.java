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

package io.github.mzmine.modules.visualization.dash_lipidqc.retention;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Multi-series retention dataset that groups all annotations of a selected lipid class into stable,
 * sortable trend series such as DBE groups or carbon-count groups.
 */
final class GroupedRetentionTrendDataset extends AbstractXYDataset {

  private final @NotNull SeriesData[] seriesData;

  GroupedRetentionTrendDataset(final @NotNull List<FeatureListRow> rows,
      final @NotNull ILipidClass selectedClass,
      final @NotNull ToIntFunction<MatchedLipid> seriesKeyExtractor,
      final @NotNull ToIntFunction<MatchedLipid> yValueExtractor,
      final @NotNull IntFunction<String> seriesLabelFormatter) {
    final Map<Integer, List<PointData>> groupedPoints = new TreeMap<>();
    for (final FeatureListRow row : rows) {
      final Float rowRt = row.getAverageRT();
      if (rowRt == null || !Float.isFinite(rowRt)) {
        continue;
      }
      final Map<Integer, Set<Integer>> addedYValuesPerSeries = new HashMap<>();
      for (final MatchedLipid match : row.getLipidMatches()) {
        if (!match.getLipidAnnotation().getLipidClass().equals(selectedClass)) {
          continue;
        }
        final int seriesKey = seriesKeyExtractor.applyAsInt(match);
        final int yValue = yValueExtractor.applyAsInt(match);
        if (seriesKey < 0 || yValue < 0) {
          continue;
        }
        final Set<Integer> addedYValues = addedYValuesPerSeries.computeIfAbsent(seriesKey,
            ignored -> new HashSet<>());
        if (!addedYValues.add(yValue)) {
          continue;
        }
        groupedPoints.computeIfAbsent(seriesKey, ignored -> new ArrayList<>())
            .add(new PointData(rowRt.doubleValue(), yValue, match, row));
      }
    }

    seriesData = groupedPoints.entrySet().stream().map(entry -> {
      final List<PointData> points = new ArrayList<>(entry.getValue());
      points.sort(
          Comparator.comparingDouble(PointData::xValue).thenComparingInt(PointData::yValue));
      final double[] xValues = new double[points.size()];
      final double[] yValues = new double[points.size()];
      final MatchedLipid[] matchedLipids = new MatchedLipid[points.size()];
      final FeatureListRow[] matchedRows = new FeatureListRow[points.size()];
      for (int i = 0; i < points.size(); i++) {
        final PointData point = points.get(i);
        xValues[i] = point.xValue();
        yValues[i] = point.yValue();
        matchedLipids[i] = point.matchedLipid();
        matchedRows[i] = point.row();
      }
      return new SeriesData(seriesLabelFormatter.apply(entry.getKey()), xValues, yValues,
          matchedLipids, matchedRows);
    }).toArray(SeriesData[]::new);
  }

  @Override
  public int getSeriesCount() {
    return seriesData.length;
  }

  @Override
  public @NotNull Comparable<?> getSeriesKey(final int series) {
    return seriesData[series].seriesKey();
  }

  @Override
  public int getItemCount(final int series) {
    return seriesData[series].xValues().length;
  }

  @Override
  public @NotNull Number getX(final int series, final int item) {
    return seriesData[series].xValues()[item];
  }

  @Override
  public @NotNull Number getY(final int series, final int item) {
    return seriesData[series].yValues()[item];
  }

  @Nullable MatchedLipid getMatchedLipid(final int series, final int item) {
    final MatchedLipid[] matchedLipids = seriesData[series].matchedLipids();
    return item >= 0 && item < matchedLipids.length ? matchedLipids[item] : null;
  }

  @Nullable FeatureListRow getRow(final int series, final int item) {
    final FeatureListRow[] rows = seriesData[series].rows();
    return item >= 0 && item < rows.length ? rows[item] : null;
  }

  @Nullable String getTooltip(final int series, final int item,
      final @NotNull LipidAnnotationLevel level) {
    final @Nullable MatchedLipid lipid = getMatchedLipid(series, item);
    return lipid == null ? null : lipid.getLipidAnnotation().getAnnotation(level);
  }

  @Nullable String getLabel(final int series, final int item,
      final @NotNull LipidAnnotationLevel level) {
    final @Nullable MatchedLipid lipid = getMatchedLipid(series, item);
    if (lipid == null) {
      return null;
    }
    final double yValue = seriesData[series].yValues()[item];
    for (int i = 0; i < seriesData[series].yValues().length; i++) {
      if (i == item || seriesData[series].yValues()[i] != yValue) {
        continue;
      }
      final @Nullable MatchedLipid other = getMatchedLipid(series, i);
      final double score = lipid.getMsMsScore() == null ? 0d : lipid.getMsMsScore();
      final double otherScore =
          other == null || other.getMsMsScore() == null ? 0d : other.getMsMsScore();
      if (score < otherScore) {
        return null;
      }
    }
    return lipid.getLipidAnnotation().getAnnotation(level);
  }

  int getTotalItemCount() {
    int total = 0;
    for (final SeriesData seriesDatum : seriesData) {
      total += seriesDatum.xValues().length;
    }
    return total;
  }

  private record PointData(double xValue, int yValue, @NotNull MatchedLipid matchedLipid,
                           @NotNull FeatureListRow row) {

  }

  private record SeriesData(@NotNull String seriesKey, @NotNull double[] xValues,
                            @NotNull double[] yValues, @NotNull MatchedLipid[] matchedLipids,
                            @NotNull FeatureListRow[] rows) {

  }
}
