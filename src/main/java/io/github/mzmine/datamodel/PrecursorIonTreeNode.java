/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class PrecursorIonTreeNode {

  private static final Logger logger = Logger.getLogger(PrecursorIonTreeNode.class.getName());
  private final double precursorMZ;
  private final int msLevel;
  private final @Nullable PrecursorIonTreeNode parent;
  // can have multiple fragment scans per precursor (different energies etc)
  private final @NotNull List<Scan> fragmentScans;
  private @Nullable List<PrecursorIonTreeNode> childFragmentScans;


  public PrecursorIonTreeNode(int msLevel, double precursorMZ,
      @Nullable PrecursorIonTreeNode parent) {
    this.msLevel = msLevel;
    this.precursorMZ = precursorMZ;
    this.parent = parent;
    fragmentScans = new ArrayList<>();
  }

  public int getMsLevel() {
    return msLevel;
  }

  public double getPrecursorMZ() {
    return precursorMZ;
  }

  @Nullable
  public PrecursorIonTreeNode getParent() {
    return parent;
  }

  @NotNull
  public List<PrecursorIonTreeNode> getChildPrecursors() {
    return childFragmentScans == null ? List.of() : childFragmentScans;
  }

  @NotNull
  public List<Scan> getFragmentScans() {
    return fragmentScans;
  }

  public void addFragmentScan(Scan scan) {
    fragmentScans.add(scan);
  }

  public void addChildFragmentScan(PrecursorIonTreeNode precursor) {
    if (childFragmentScans == null) {
      childFragmentScans = new ArrayList<>();
    }
    childFragmentScans.add(precursor);
  }

  public boolean matches(double precursorMZ) {
    return (int) (this.precursorMZ * 10000) == (int) (precursorMZ * 10000);
  }

  /**
   * Navigates through the MS levels to find or create a node for the defined precursor m/z. All
   * other preceding isolation steps should already be added.
   *
   * @param scan    the scan that is added for msnInfo
   * @param msnInfo defining the MS2, MS3, MSn isolation steps
   */
  public boolean addChildFragmentScan(Scan scan, MSnInfoImpl msnInfo) {
    return addChildFragmentScan(scan, msnInfo, 1);
  }

  protected boolean addChildFragmentScan(Scan scan, MSnInfoImpl msnInfo, int msnIndex) {
    final DDAMsMsInfo prec = msnInfo.getPrecursors().get(msnIndex);
    boolean added = false;
    final int msLevel = prec.getMsLevel();
    if (msLevel != this.msLevel + 1) {
      logger.warning(() -> String.format(
          "Wrong sequence in MSn info. msLevels are off. Expected: %d  Actual:%d",
          (this.msLevel + 1), msLevel));
      return false;
    }
    final double mz = prec.getIsolationMz();
    PrecursorIonTreeNode child = getChild(mz);

    // last element reached
    if (msnInfo.getPrecursors().size() == msnIndex - 1) {
      if (child == null) {
        child = new PrecursorIonTreeNode(msLevel + 1, mz, this);
        addChildFragmentScan(child);
      }
      child.addFragmentScan(scan);
      return true;
    } else {
      if (child != null) {
        return child.addChildFragmentScan(scan, msnInfo, msnIndex + 1);
      } else {
        logger.warning(() -> String.format("Child for msLevel %d not found", (this.msLevel + 1)));
      }
    }
    return false;
  }

  @Nullable
  public PrecursorIonTreeNode getChild(double precursorMZ) {
    for (var child : getChildPrecursors()) {
      if (child.matches(precursorMZ)) {
        return child;
      }
    }
    return null;
  }
}
