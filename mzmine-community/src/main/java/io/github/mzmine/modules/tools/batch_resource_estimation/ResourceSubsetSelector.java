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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

/**
 * Orders the files of a batch so that every prefix of the order is a representative subset: the
 * sample types follow the proportions of the whole dataset and the file sizes are spread over the
 * size range. Subsets are therefore nested (the small subset is part of the larger ones), which
 * keeps the differences between subset runs small and the scaling fit stable.
 * <p>
 * Deterministic, so that repeated estimations of the same batch run on the same files.
 */
final class ResourceSubsetSelector {

  private ResourceSubsetSelector() {
  }

  /**
   * @return all files in the order in which they are added to the subsets
   */
  static @NotNull List<EstimationInputFile> orderForSubsets(
      @NotNull final List<EstimationInputFile> files) {
    final Map<SampleType, List<EstimationInputFile>> groups = new EnumMap<>(SampleType.class);
    for (final EstimationInputFile file : files) {
      groups.computeIfAbsent(file.sampleType(), _ -> new ArrayList<>()).add(file);
    }
    final Map<SampleType, List<EstimationInputFile>> spreadGroups = new EnumMap<>(SampleType.class);
    groups.forEach((type, group) -> spreadGroups.put(type, spreadBySize(group)));

    // decision: D'Hondt apportionment keeps every prefix proportional to the group sizes, so
    // blanks and QCs are part of the subsets as often as they are part of the dataset
    final Map<SampleType, Integer> picked = new EnumMap<>(SampleType.class);
    final List<EstimationInputFile> order = new ArrayList<>(files.size());
    while (order.size() < files.size()) {
      SampleType best = null;
      double bestQuotient = -1;
      // EnumMap iterates in enum order, which makes ties deterministic
      for (final var entry : spreadGroups.entrySet()) {
        final int groupSize = entry.getValue().size();
        final int pickedOfGroup = picked.getOrDefault(entry.getKey(), 0);
        if (pickedOfGroup >= groupSize) {
          continue;
        }
        final double quotient = groupSize / (pickedOfGroup + 1d);
        if (quotient > bestQuotient) {
          bestQuotient = quotient;
          best = entry.getKey();
        }
      }
      final int index = picked.merge(best, 1, Integer::sum) - 1;
      order.add(spreadGroups.get(best).get(index));
    }
    return order;
  }

  /**
   * Orders the files by size so that every prefix covers the size range: median first, then the
   * medians of both halves and so on.
   */
  static @NotNull List<EstimationInputFile> spreadBySize(
      @NotNull final List<EstimationInputFile> files) {
    final List<EstimationInputFile> sorted = files.stream().sorted(
        Comparator.comparingLong(EstimationInputFile::sizeBytes)
            .thenComparing(f -> f.file().getName())).toList();

    final List<EstimationInputFile> spread = new ArrayList<>(sorted.size());
    // breadth first over [from, to) intervals visits the median of each interval
    final ArrayDeque<int[]> intervals = new ArrayDeque<>();
    intervals.add(new int[]{0, sorted.size()});
    while (!intervals.isEmpty()) {
      final int[] interval = intervals.poll();
      if (interval[0] >= interval[1]) {
        continue;
      }
      final int mid = (interval[0] + interval[1]) / 2;
      spread.add(sorted.get(mid));
      intervals.add(new int[]{interval[0], mid});
      intervals.add(new int[]{mid + 1, interval[1]});
    }
    return spread;
  }

  /**
   * @param smallestSubset number of files in the smallest subset
   * @param levels         number of subset sizes, each doubles the previous one
   * @return the number of files that the batch needs for these subsets and the validation run
   */
  static int requiredFiles(final int smallestSubset, final int levels) {
    return validationSize(smallestSubset << (levels - 1));
  }

  /**
   * @param largestSubset files in the largest subset of the extrapolation
   * @return files of the validation run that checks the extrapolation
   */
  static int validationSize(final int largestSubset) {
    return largestSubset * 2;
  }

  /**
   * @param totalFiles     number of files in the batch
   * @param smallestSubset number of files in the smallest subset
   * @param levels         number of subset sizes, each doubles the previous one
   * @return distinct ascending subset sizes, capped at the number of files
   */
  static @NotNull List<Integer> subsetSizes(final int totalFiles, final int smallestSubset,
      final int levels) {
    return IntStream.range(0, levels)
        .map(level -> (int) Math.min((long) smallestSubset << level, totalFiles)).distinct()
        .filter(size -> size > 0).boxed().toList();
  }
}
