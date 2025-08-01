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

package io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection;

import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TopNSelectionModule implements PrecursorSelectionModule {

  private final int numPrecursors;

  // required default constructor for config
  public TopNSelectionModule() {
    numPrecursors = 5;
  }

  public TopNSelectionModule(ParameterSet parameters) {
    numPrecursors = parameters.getValue(TopNSelectionParameters.numPrecursors);
  }


  public static Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> findOverlaps(
      List<MaldiTimsPrecursor> precursors, final double mobilityGap) {

    Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlapsMap = new HashMap<>();
    for (final MaldiTimsPrecursor precursor : precursors) {
      final List<MaldiTimsPrecursor> overlapping = overlapsMap.computeIfAbsent(precursor,
          k -> new ArrayList<>());

      for (MaldiTimsPrecursor precursor2 : precursors) {
        if (precursor.equals(precursor2)) {
          continue;
        }

        if (overlaps(precursor, precursor2, mobilityGap)) {
          overlapping.add(precursor2);
        }
      }
    }
    return overlapsMap;
  }

  public static List<List<MaldiTimsPrecursor>> findRampsIterative(
      Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps) {
    final List<PrecursorOverlap> allPrecursorOverlaps = new ArrayList<>();
    overlaps.forEach((k, v) -> allPrecursorOverlaps.add(new PrecursorOverlap(k, v)));

    allPrecursorOverlaps.sort(Comparator.reverseOrder());

    List<List<MaldiTimsPrecursor>> allRamps = new ArrayList<>();
    List<MaldiTimsPrecursor> nextRamp = List.of();
    while ((nextRamp = findNextRamp(allPrecursorOverlaps)).size() != 0) {
      allRamps.add(nextRamp);
    }

    return allRamps;
  }

  private static List<MaldiTimsPrecursor> findNextRamp(
      List<PrecursorOverlap> allPrecursorOverlaps) {
    if (allPrecursorOverlaps.isEmpty()) {
      return List.of();
    }

    final List<MaldiTimsPrecursor> currentRamp = new ArrayList<>();

    PrecursorOverlap nextPrecursor = null;
    while ((nextPrecursor = findNextPrecursor(currentRamp, allPrecursorOverlaps)) != null) {
      allPrecursorOverlaps.remove(nextPrecursor);
      currentRamp.add(nextPrecursor.precursor());
      allPrecursorOverlaps.sort(Comparator.reverseOrder());
    }
    // remove overlaps only AFTER! the whole ramp was set
    currentRamp.forEach(precursor -> removePrecursorFromOverlaps(precursor, allPrecursorOverlaps));

    return currentRamp;
  }

  private static void removePrecursorFromOverlaps(MaldiTimsPrecursor precursor,
      List<PrecursorOverlap> allPrecursorOverlaps) {
    for (PrecursorOverlap allPrecursorOverlap : allPrecursorOverlaps) {
      allPrecursorOverlap.overlaps().remove(precursor);
    }
  }

  /**
   * Finds the next precursor in the allPrecursorOverlaps list to fit into the current ramp. Does
   * not alter any of the lists!
   *
   * @return The next precursor or null, if no other precursor fits.
   */
  @Nullable
  private static PrecursorOverlap findNextPrecursor(List<MaldiTimsPrecursor> currentRamp,
      List<PrecursorOverlap> allPrecursorOverlaps) {

    for (PrecursorOverlap precursorOverlap : allPrecursorOverlaps) {
      boolean noOverlap = true;
      for (MaldiTimsPrecursor maldiTimsPrecursor : currentRamp) {
        if (precursorOverlap.overlaps().contains(maldiTimsPrecursor)) {
          noOverlap = false;
          break;
        }
      }

      if (noOverlap) {
        return precursorOverlap;
      }
    }

    return null;
  }

  public static boolean overlaps(MaldiTimsPrecursor p1, MaldiTimsPrecursor p2) {
    return overlaps(p1, p2, 0.01d);
  }

  public static boolean overlaps(MaldiTimsPrecursor p1, MaldiTimsPrecursor p2,
      final double minDistance) {
    final float lowerBound = Math.min(p1.mobility().upperEndpoint(), p2.mobility().upperEndpoint());
    final float upperBound = Math.max(p1.mobility().lowerEndpoint(), p2.mobility().lowerEndpoint());

    return p1.mobility().isConnected(p2.mobility())
           || Math.abs(lowerBound - upperBound) < minDistance;
  }

  @Override
  public @NotNull String getName() {
    return "Top N Precursor selection module";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return TopNSelectionParameters.class;
  }

  @Override
  public List<List<MaldiTimsPrecursor>> getPrecursorList(Collection<MaldiTimsPrecursor> precursors,
      final double mobilityGap) {
    // get the top N precursors
    final List<MaldiTimsPrecursor> precursorSorted = new ArrayList<>(precursors.stream()
        .sorted((p1, p2) -> -1 * Float.compare(p1.feature().getHeight(), p2.feature().getHeight()))
        .toList());

    final List<MaldiTimsPrecursor> annotated = precursorSorted.stream()
        .filter(p -> p.feature().getRow().isIdentified()).toList();

    final List<MaldiTimsPrecursor> topN = new ArrayList<>();
    topN.addAll(annotated);
    precursorSorted.removeAll(topN);
    final int endIndex = Math.min(numPrecursors - topN.size(),
        precursorSorted.size() - topN.size());
    if (topN.size() < numPrecursors && endIndex > 0) {
      topN.addAll(precursorSorted.subList(0, endIndex));
    }

    // arrange the topN precursors into non overlapping lists
    final Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps = findOverlaps(topN,
        mobilityGap);
    return findRampsIterative(overlaps);
  }
}
