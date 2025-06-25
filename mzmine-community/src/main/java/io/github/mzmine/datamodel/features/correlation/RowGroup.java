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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Group of row. Rows can be grouped by different criteria: Retention time, feature shape (intensity
 * profile), and intensity across samples. See {@link CorrelateGroupingModule}. IMPORTANT: Not all
 * FeatureListRows in this group are actually correlated. The actual structure is a network of
 * relationships where each row has at least one connection.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public interface RowGroup extends Comparable<RowGroup> {

  default void setGroupToAllRows() {
    getRows().forEach(r -> r.setGroup(this));
  }

  default void forEach(Consumer<? super FeatureListRow> action) {
    getRows().forEach(action);
  }

  List<FeatureListRow> getRows();

  default void addAll(FeatureListRow... rows) {
    for (FeatureListRow row : rows) {
      add(row);
    }
  }

  default void addAll(Collection<FeatureListRow> rows) {
    for (FeatureListRow row : rows) {
      add(row);
    }
  }

  /**
   * Insert sort by ascending avg mz
   */
  boolean add(FeatureListRow e);

  /**
   * Number of rows in this group
   *
   * @return number of rows
   */
  default int size() {
    return getRows().size();
  }

  /**
   * Stream all rows
   *
   * @return rows stream
   */
  default Stream<FeatureListRow> stream() {
    return getRows().stream();
  }

  /**
   * checks for the same ID
   *
   * @param row the tested row
   * @return true if this row is part of the group
   */
  default boolean contains(FeatureListRow row) {
    return getRows().contains(row);
  }


  /**
   * The group identifier
   *
   * @return the group identifier
   */
  int getGroupID();

  /**
   * Set the group ID
   *
   * @param groupID new ID
   */
  void setGroupID(int groupID);

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   *
   * @param i index in group
   * @param k index in group
   * @return true if the rows i and k are correlated
   */
  default boolean isCorrelated(int i, int k) {
    return i != -1 && k != -1 && i < size() && k < size();
  }

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   *
   * @param a row a
   * @param b row b
   * @return true if row a and b are correlated
   */
  default boolean isCorrelated(FeatureListRow a, FeatureListRow b) {
    int ia = indexOf(a);
    int ib = indexOf(b);
    if (ia == -1 || ib == -1) {
      return false;
    }
    return isCorrelated(ia, ib);
  }

  /**
   * Index of row in this group
   *
   * @param row the tested row
   * @return the index of the first occurrence of the specified element in this list, or -1 if this
   * list does not contain the element
   */
  default int indexOf(FeatureListRow row) {
    return getRows().indexOf(row);
  }

  default FeatureListRow get(int i) {
    return getRows().get(i);
  }

  @Nullable
  default Float calcAverageRetentionTime() {
    int counter = 0;
    float rt = -1;
    for (final FeatureListRow row : getRows()) {
      final Float rowRT = row.getAverageRT();
      if (rowRT != null) {
        rt += rowRT;
        counter++;
      }
    }
    return counter > 0 ? rt / counter : null;
  }

  @Override
  default int compareTo(@NotNull RowGroup g) {
    return Integer.compare(this.getGroupID(), g.getGroupID());
  }

  default int lowestRowId() {
    return getRows().stream().mapToInt(FeatureListRow::getID).min().orElse(-1);
  }
}
