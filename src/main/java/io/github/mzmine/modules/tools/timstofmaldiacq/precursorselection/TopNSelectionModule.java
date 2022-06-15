/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection;

import com.google.common.collect.Range;
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
      ParameterSet parameters) {
    final int numPrecursors = parameters.getValue(TopNSelectionParameters.numPrecursors);

    // get the top N precursors
    final List<MaldiTimsPrecursor> precursorSorted = precursors.stream()
        .sorted((p1, p2) -> -1 * Float.compare(p1.feature().getHeight(), p2.feature().getHeight()))
        .toList();
    final List<MaldiTimsPrecursor> topN = precursorSorted.subList(0,
        Math.min(numPrecursors, precursorSorted.size()));

    // arrange the topN precursors into non overlapping lists
    final Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps = findOverlaps(topN);
    return findRampsIterative(overlaps);
  }

  public static Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> findOverlaps(
      List<MaldiTimsPrecursor> precursors) {

    Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlapsMap = new HashMap<>();
    for (final MaldiTimsPrecursor precursor : precursors) {
      final List<MaldiTimsPrecursor> overlapping = overlapsMap.computeIfAbsent(precursor,
          k -> new ArrayList<>());

      for (MaldiTimsPrecursor precursor2 : precursors) {
        if (precursor.equals(precursor2)) {
          continue;
        }

        if (overlaps(precursor, precursor2)) {
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

    /*for (List<MaldiTimsPrecursor> ramp : allRamps) {
      for (MaldiTimsPrecursor p1 : ramp) {
        for (MaldiTimsPrecursor p2 : ramp) {
          if(p1 == p2) {
            continue;
          }
          if(overlaps(p1, p2)) {
            throw new RuntimeException("Overlapping precursors in ramp");
          }
        }
      }
    }*/
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
    final int digits = 2;
    final double digitMult = Math.pow(10d, digits);

    final Range<Integer> r1 = Range.closed(
        (int) Math.round(p1.oneOverK0().lowerEndpoint() * digitMult),
        (int) Math.round(p1.oneOverK0().upperEndpoint() * digitMult));

    final Range<Integer> r2 = Range.closed(
        (int) Math.round(p2.oneOverK0().lowerEndpoint() * digitMult),
        (int) Math.round(p2.oneOverK0().upperEndpoint() * digitMult));

    return r1.isConnected(r2);
  }
}
