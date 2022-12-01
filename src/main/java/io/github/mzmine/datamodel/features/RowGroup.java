/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Arrays;
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
public class RowGroup implements Comparable<RowGroup> {

  // raw files used for Feature list creation
  protected final List<RawDataFile> raw;
  // running index of groups
  protected int groupID;
  protected List<FeatureListRow> rows;
  // visualization
  private int lastViewedRowIndex = 0;
  private int lastViewedRawFileIndex = 0;
  // center RT values for each sample
  private float[] rtSum;
  private int[] numberOfFeatures;
  private float[] rtMin, rtMax;

  public RowGroup(final List<RawDataFile> raw, int groupID) {
    super();
    rows = new ArrayList<>();
    this.raw = raw;
    this.groupID = groupID;
    this.rtMin = new float[raw.size()];
    this.rtMax = new float[raw.size()];
    Arrays.fill(rtMin, Float.POSITIVE_INFINITY);
    Arrays.fill(rtMax, Float.NEGATIVE_INFINITY);
    this.rtSum = new float[raw.size()];
    this.numberOfFeatures = new int[raw.size()];
  }

  public void setGroupToAllRows() {
    this.forEach(r -> r.setGroup(this));
  }

  public void forEach(Consumer<? super FeatureListRow> action) {
    rows.forEach(action);
  }

  public List<FeatureListRow> getRows() {
    return rows;
  }

  public synchronized void addAll(FeatureListRow... rows) {
    for (FeatureListRow row : rows) {
      add(row);
    }
  }

  public synchronized void addAll(Collection<FeatureListRow> rows) {
    for (FeatureListRow row : rows) {
      add(row);
    }
  }

  /**
   * Insert sort by ascending avg mz
   */
  public synchronized boolean add(FeatureListRow e) {
    for (int i = 0; i < rtSum.length; i++) {
      Feature f = e.getFeature(raw.get(i));
      if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN) {
        rtSum[i] = (rtSum[i] + f.getRT());
        numberOfFeatures[i]++;
        // min max
        if (f.getRT() < rtMin[i]) {
          rtMin[i] = f.getRT();
        }
        if (f.getRT() > rtMax[i]) {
          rtMax[i] = f.getRT();
        }
      }
    }
    // last position
    return rows.add(e);
  }

  /**
   * Number of rows in this group
   *
   * @return number of rows
   */
  public int size() {
    return rows.size();
  }

  /**
   * Stream all rows
   *
   * @return rows stream
   */
  public Stream<FeatureListRow> stream() {
    return rows.stream();
  }

  /**
   * List of all raw data files
   *
   * @return raw list
   */
  public List<RawDataFile> getRaw() {
    return raw;
  }

  /**
   * checks for the same ID
   *
   * @param row the tested row
   * @return true if this row is part of the group
   */
  public boolean contains(FeatureListRow row) {
    for (FeatureListRow r : rows) {
      // needs to be the same instance
      if (r == row) {
        return true;
      }
    }
    return false;
  }


  /**
   * Center retention time in raw file[i]
   *
   * @param rawIndex index of the raw data file in this group
   * @return the center retention time
   */
  public float getCenterRT(int rawIndex) {
    if (numberOfFeatures[rawIndex] == 0) {
      return -1;
    }
    return rtSum[rawIndex] / numberOfFeatures[rawIndex];
  }

  /**
   * center retention time of this group
   *
   * @return center retention time across all rows
   */
  public double getCenterRT() {
    double center = 0;
    int counter = 0;
    for (int i = 0; i < rtSum.length; i++) {
      if (numberOfFeatures[i] > 0) {
        center += rtSum[i] / numberOfFeatures[i];
        counter++;
      }
    }
    return center / counter;
  }

  /**
   * checks if a feature is in range either between min and max or in range of avg+-tolerance
   *
   * @param rawIndex index of the raw data file in this group
   * @return true if feature is within rt tolerance
   */
  public boolean isInRtRange(int rawIndex, Feature f, RTTolerance tol) {
    return hasFeature(rawIndex) && ((f.getRT() >= rtMin[rawIndex] && f.getRT() <= rtMax[rawIndex])
                                    || (tol
        .checkWithinTolerance(getCenterRT(rawIndex), f.getRT())));
  }

  /**
   * checks if this group has a feature in rawfile[i]
   *
   * @param rawIndex index of the raw data file in this group
   * @return true if this group has a feature
   */
  public boolean hasFeature(int rawIndex) {
    return numberOfFeatures[rawIndex] > 0;
  }

  /**
   * The group identifier
   *
   * @return the group identifier
   */
  public int getGroupID() {
    return groupID;
  }

  /**
   * Set the group ID
   *
   * @param groupID new ID
   */
  public void setGroupID(int groupID) {
    this.groupID = groupID;
  }

  // ###########################################
  // for visuals

  /**
   * The last viewed row index (for visualization) (index in this group)
   *
   * @return the row index in this group
   */
  public int getLastViewedRowIndex() {
    return lastViewedRowIndex;
  }

  /**
   * Set the last viewed row index for visualization (index in this group)
   *
   * @param lastViewedRowIndex the new index
   */
  public void setLastViewedRowIndex(int lastViewedRowIndex) {
    this.lastViewedRowIndex = lastViewedRowIndex;
  }

  /**
   * Last viewed row
   *
   * @return row or null
   */
  @Nullable
  public FeatureListRow getLastViewedRow() {
    return get(lastViewedRowIndex);
  }


  public int getLastViewedRawFileIndex() {
    return lastViewedRawFileIndex;
  }

  /**
   * Set the last viewed raw data file index (in this group)
   *
   * @param lastViewedRawFileIndex index of the raw data file in this group
   */
  public void setLastViewedRawFileIndex(int lastViewedRawFileIndex) {
    if (lastViewedRawFileIndex < 0) {
      lastViewedRawFileIndex = 0;
    } else if (lastViewedRawFileIndex >= raw.size()) {
      lastViewedRawFileIndex = raw.size() - 1;
    }
    this.lastViewedRawFileIndex = lastViewedRawFileIndex;
  }

  /**
   * @return The last viewed raw data file or the first
   */
  public RawDataFile getLastViewedRawFile() {
    return raw.get(lastViewedRawFileIndex);
  }

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   *
   * @param i index in group
   * @param k index in group
   * @return true if the rows i and k are correlated
   */
  public boolean isCorrelated(int i, int k) {
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
  public boolean isCorrelated(FeatureListRow a, FeatureListRow b) {
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
  public int indexOf(FeatureListRow row) {
    return rows.indexOf(row);
  }

  public FeatureListRow get(int i) {
    return rows.get(i);
  }

  @Override
  public int compareTo(@NotNull RowGroup g) {
    return Integer.compare(this.getGroupID(), g.getGroupID());
  }
}
