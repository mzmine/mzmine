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

import java.util.ArrayList;
import java.util.List;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;

public class RowGroup extends ArrayList<FeatureListRow> {
  // visualization
  private int lastViewedRow = 0;
  private int lastViewedRawFile = 0;

  // running index of groups
  protected int groupID = 0;
  // raw files used for Feature list creation
  protected final List<RawDataFile> raw;
  // center RT values for each sample
  private float[] rtSum;
  private int[] rtValues;
  private float[] min, max;

  public RowGroup(final List<RawDataFile> raw, int groupID) {
    super();
    this.raw = raw;
    this.groupID = groupID;
    this.min = new float[raw.size()];
    this.max = new float[raw.size()];
    for (int i = 0; i < min.length; i++) {
      min[i] = Float.POSITIVE_INFINITY;
      max[i] = Float.NEGATIVE_INFINITY;
    }
    this.rtSum = new float[raw.size()];
    this.rtValues = new int[raw.size()];
  }

  public void setGroupToAllRows() {
    this.forEach(r -> r.setGroup(this));
  }

  /**
   * Insert sort by ascending avg mz
   */
  @Override
  public synchronized boolean add(FeatureListRow e) {
    for (int i = 0; i < rtSum.length; i++) {
      Feature f = e.getFeature(raw.get(i));
      if (f != null) {
        rtSum[i] = (rtSum[i] + f.getRT());
        rtValues[i]++;
        // min max
        if (f.getRT() < min[i])
          min[i] = f.getRT();
        if (f.getRT() > max[i])
          max[i] = f.getRT();
      }
    }
    // insert sort find position
    for (int i = 0; i < size(); i++) {
      if (e.getAverageMZ() <= get(i).getAverageMZ()) {
        super.add(i, e);
        return true;
      }
    }
    // last position
    return super.add(e);
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
    return contains(row.getID());
  }

  /**
   * checks for the same ID
   * 
   * @param id
   * @return
   */
  public boolean contains(int id) {
    for (FeatureListRow r : this)
      if (r.getID() == id)
        return true;
    return false;
  }

  /**
   * Center retention time in raw file[i]
   * 
   * @param rawi
   * @return
   */
  public float getCenterRT(int rawi) {
    if (rtValues[rawi] == 0)
      return -1;
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
    for (int i = 0; i < rtSum.length; i++)
      if (rtValues[i] > 0) {
        center += rtSum[i] / rtValues[i];
        counter++;
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
   * 
   * @param rawi
   * @return
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

  public FeatureListRow getLastViewedRow() {
    return get(lastViewedRow);
  }

  public void setLastViewedRowI(int lastViewedRow) {
    this.lastViewedRow = lastViewedRow;
  }

  public int getLastViewedRawFileI() {
    return lastViewedRawFile;
  }

  public RawDataFile getLastViewedRawFile() {
    return raw.get(lastViewedRawFile);
  }

  public void setLastViewedRawFileI(int lastViewedRawFile) {
    if (lastViewedRawFile < 0)
      lastViewedRawFile = 0;
    else if (lastViewedRawFile >= raw.size())
      lastViewedRawFile = raw.size() - 1;
    this.lastViewedRawFile = lastViewedRawFile;
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
    if (i == -1 || k == -1)
      return false;
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
    if (ia == -1 || ib == -1)
      return false;
    return isCorrelated(ia, ib);
  }
}
