/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

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
  protected int groupID = 0;
  protected List<FeatureListRow> rows;
  // visualization
  private int lastViewedRow = 0;
  private int lastViewedRawFile = 0;
  // center RT values for each sample
  private float[] rtSum;
  private int[] rtValues;
  private float[] min, max;

  public RowGroup(final List<RawDataFile> raw, int groupID) {
    super();
    rows = new ArrayList<>();
    this.raw = raw;
    this.groupID = groupID;
    this.min = new float[raw.size()];
    this.max = new float[raw.size()];
    Arrays.fill(min, Float.POSITIVE_INFINITY);
    Arrays.fill(max, Float.NEGATIVE_INFINITY);
    this.rtSum = new float[raw.size()];
    this.rtValues = new int[raw.size()];
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
      if (f != null) {
        rtSum[i] = (rtSum[i] + f.getRT());
        rtValues[i]++;
        // min max
        if (f.getRT() < min[i]) {
          min[i] = f.getRT();
        }
        if (f.getRT() > max[i]) {
          max[i] = f.getRT();
        }
      }
    }
    // last position
    return rows.add(e);
  }

  public int size() {
    return rows.size();
  }

  public Stream<FeatureListRow> stream() {
    return rows.stream();
  }

  public List<RawDataFile> getRaw() {
    return raw;
  }

  /**
   * checks for the same ID
   *
   * @param row
   * @return
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
   * @param rawi
   * @return
   */
  public float getCenterRT(int rawi) {
    if (rtValues[rawi] == 0) {
      return -1;
    }
    return rtSum[rawi] / rtValues[rawi];
  }

  /**
   * center retention time of this group
   *
   * @return
   */
  public double getCenterRT() {
    double center = 0;
    int counter = 0;
    for (int i = 0; i < rtSum.length; i++) {
      if (rtValues[i] > 0) {
        center += rtSum[i] / rtValues[i];
        counter++;
      }
    }
    return center / counter;
  }

  /**
   * checks if a feature is in range either between min and max or in range of avg+-tolerance
   *
   * @return
   */
  public boolean isInRange(int rawi, Feature f, RTTolerance tol) {
    return hasFeature(rawi) && ((f.getRT() >= min[rawi] && f.getRT() <= max[rawi])
                                || (tol.checkWithinTolerance(getCenterRT(rawi), f.getRT())));
  }

  /**
   * checks if this group has a feature in rawfile[i]
   */
  public boolean hasFeature(int rawi) {
    return rtValues[rawi] > 0;
  }

  public int getGroupID() {
    return groupID;
  }

  public void setGroupID(int groupID) {
    this.groupID = groupID;
  }

  // ###########################################
  // for visuals

  public int getLastViewedRowI() {
    return lastViewedRow;
  }

  public void setLastViewedRowI(int lastViewedRow) {
    this.lastViewedRow = lastViewedRow;
  }

  public FeatureListRow getLastViewedRow() {
    return get(lastViewedRow);
  }

  public int getLastViewedRawFileI() {
    return lastViewedRawFile;
  }

  public void setLastViewedRawFileI(int lastViewedRawFile) {
    if (lastViewedRawFile < 0) {
      lastViewedRawFile = 0;
    } else if (lastViewedRawFile >= raw.size()) {
      lastViewedRawFile = raw.size() - 1;
    }
    this.lastViewedRawFile = lastViewedRawFile;
  }

  public RawDataFile getLastViewedRawFile() {
    return raw.get(lastViewedRawFile);
  }

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   *
   * @param i index in group
   * @param k index in group
   * @return
   */
  public boolean isCorrelated(int i, int k) {
    if (i == -1 || k == -1) {
      return false;
    }
    return true;
  }

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   *
   * @param a
   * @param b
   * @return
   */
  public boolean isCorrelated(FeatureListRow a, FeatureListRow b) {
    int ia = indexOf(a);
    int ib = indexOf(b);
    if (ia == -1 || ib == -1) {
      return false;
    }
    return isCorrelated(ia, ib);
  }

  public int indexOf(FeatureListRow row) {
    return rows.indexOf(row);
  }

  public FeatureListRow get(int i) {
    return rows.get(i);
  }

  @Override
  public int compareTo(@Nonnull RowGroup g) {
    return Integer.compare(this.getGroupID(), g.getGroupID());
  }
}
