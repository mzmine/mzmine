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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RCorrelationData;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.correlation.RowGroupSimple;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CorrelationGroupingUtils {

  private static final Logger logger = Logger.getLogger(CorrelationGroupingUtils.class.getName());

  /**
   * Defines a stable order of the correlation edges by the two row IDs. The row IDs are unique
   * within a feature list and the pair is undirected, so the lower and the higher ID together
   * identify each edge unambiguously.
   */
  private static final Comparator<RowsRelationship> EDGE_SORTER = Comparator.comparingInt(
      CorrelationGroupingUtils::lowerRowId).thenComparingInt(CorrelationGroupingUtils::higherRowId);

  private static int lowerRowId(@NotNull final RowsRelationship edge) {
    return Math.min(edge.getRowA().getID(), edge.getRowB().getID());
  }

  private static int higherRowId(@NotNull final RowsRelationship edge) {
    return Math.max(edge.getRowA().getID(), edge.getRowB().getID());
  }

  /**
   * Create list of correlated rows (connected components) on demand from the MS1 correlation map of
   * the feature list. The resulting groups are backed by the correlation map and are not stored on
   * the feature list - generate them when a task needs the connected components and discard them
   * afterwards.
   *
   * @param flist feature list
   * @return a list of all groups within the feature list, an empty list if no correlation map
   * exists, or null on error
   */
  @Nullable
  public static List<RowGroup> createCorrGroups(FeatureList flist) {
    logger.log(Level.INFO, "Corr: Creating correlation groups for {0}", flist.getName());

    try {
      var opCorrMap = flist.getMs1CorrelationMap();
      if (opCorrMap.isEmpty()) {
        logger.log(Level.INFO,
            "Feature list ({0}) contains no grouped rows. First run a grouping module",
            flist.getName());
        return List.of();
      }
      var corrMap = opCorrMap.get();
      logger.info(
          "Creating groups for %s with %d edges".formatted(flist.getName(), corrMap.size()));

      // iterate the edges in a defined order instead of the hash order of the map.
      final List<RowsRelationship> edges = corrMap.values().stream()
          .filter(R2RCorrelationData.class::isInstance).sorted(EDGE_SORTER).toList();

      List<RowGroup> groups = new ArrayList<>();
      HashMap<Integer, RowGroup> used = new HashMap<>();

      int nextGroupID = 1;
      // add all connections
      for (final RowsRelationship r2r : edges) {
        FeatureListRow rowA = r2r.getRowA();
        FeatureListRow rowB = r2r.getRowB();
        // already added?
        RowGroup group = used.get(rowA.getID());
        RowGroup group2 = used.get(rowB.getID());
        // merge groups if both present
        if (group != null && group2 != null && group.getGroupID() != group2.getGroupID()) {
          // copy all to group1 and remove g2
          for (FeatureListRow r : group2.getRows()) {
            group.add(r);
            used.put(r.getID(), group);
          }
          groups.remove(group2);
        } else if (group == null && group2 == null) {
          // create new group with both rows
          group = new RowGroupSimple(nextGroupID, corrMap);
          // increment group - the groups are renumbered later
          nextGroupID++;
          group.addAll(rowA, rowB);
          groups.add(group);
          // mark as used
          used.put(rowA.getID(), group);
          used.put(rowB.getID(), group);
        } else if (group2 == null) {
          group.add(rowB);
          used.put(rowB.getID(), group);
        } else if (group == null) {
          group2.add(rowA);
          used.put(rowA.getID(), group2);
        }
      }

      // rows are appended in the order the edges connect them - sort them into the canonical
      // order so that consumers looping over all row pairs produce reproducible results
      for (final RowGroup group : groups) {
        group.sortRows();
      }
      // sort by retention time, group size and lowest row id to make sure it is stable
      groups.sort(Comparator.comparing(RowGroup::calcAverageRetentionTime,
              Comparator.nullsLast(Comparator.naturalOrder())).thenComparingInt(RowGroup::size)
          .thenComparing(RowGroup::lowestRowId));
      // reset index
      for (int i = 0; i < groups.size(); i++) {
        groups.get(i).setGroupID(i);
      }

      logger.info("Corr: DONE: Creating correlation groups");
      return groups;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while creating groups", e);
      return null;
    }
  }
}
