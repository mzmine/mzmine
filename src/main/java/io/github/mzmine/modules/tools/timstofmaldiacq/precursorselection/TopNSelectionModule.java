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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    return generateTargetLists(overlaps, topN);
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

  public static List<List<MaldiTimsPrecursor>> generateTargetLists(
      Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps,
      List<MaldiTimsPrecursor> allPrecursors) {

    // copy the list, so we can sort out which precursors we still need to queue
    final List<MaldiTimsPrecursor> remainingPrecursors = new ArrayList<>(allPrecursors);

    // start with the entry with the most overlaps
    final List<MaldiTimsPrecursor> precursorsByOverlaps = overlaps.entrySet().stream()
        .sorted((e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size()) * -1)
        .map(Entry::getKey).toList();

    Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> precursorAcqLists = new HashMap<>();
    for (final MaldiTimsPrecursor precursor : precursorsByOverlaps) {
      if (!remainingPrecursors.contains(precursor)) {
        // already queued
        continue;
      }

      final List<MaldiTimsPrecursor> precursorRampList = new ArrayList<>();
      precursorRampList.add(precursor);
      remainingPrecursors.remove(precursor);
      final List<MaldiTimsPrecursor> overlapping = overlaps.get(precursor);

      // add non overlapping precursors
      for (final MaldiTimsPrecursor remaining : remainingPrecursors) {
        if (!overlapping.contains(remaining) && !precursorRampList.stream()
            .anyMatch(pre -> overlaps(pre, remaining))) {
          precursorRampList.add(remaining);
        }
      }

      // remove precursors that we added to the current ramp list
      remainingPrecursors.removeAll(precursorRampList);
      precursorAcqLists.put(precursor, precursorRampList);
    }

    return precursorAcqLists.values().stream().toList();
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
