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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class PrecursorIonTreeNode implements Comparable<PrecursorIonTreeNode> {

  /**
   * use accuracy for m/z in match
   */
  private static final Logger logger = Logger.getLogger(PrecursorIonTreeNode.class.getName());
  private final double precursorMZ;
  private final int msLevel;
  private final @Nullable PrecursorIonTreeNode parent;
  // can have multiple fragment scans per precursor (different energies etc)
  private final @NotNull List<Scan> fragmentScans;
  private @Nullable List<PrecursorIonTreeNode> childPrecursors;


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
    return childPrecursors == null ? List.of() : childPrecursors;
  }

  @NotNull
  public List<Scan> getFragmentScans() {
    return fragmentScans;
  }

  @NotNull
  public List<Scan> getFragmentScans(int levelFromRoot) {
    if (levelFromRoot == 0) {
      return getFragmentScans();
    }
    return streamPrecursors(levelFromRoot).map(PrecursorIonTreeNode::getFragmentScans)
        .flatMap(List::stream).toList();
  }

  @NotNull
  public List<PrecursorIonTreeNode> getPrecursors(int levelFromRoot) {
    if (levelFromRoot == 0) {
      return List.of(this);
    } else if (levelFromRoot == 1) {
      return getChildPrecursors();
    } else {
      return streamPrecursors(levelFromRoot).toList();
    }
  }

  @NotNull
  public Stream<PrecursorIonTreeNode> streamPrecursors(int levelFromRoot) {
    if (levelFromRoot == 0) {
      return Stream.of(this);
    }
    final Stream<PrecursorIonTreeNode> stream = getChildPrecursors().stream();
    return levelFromRoot == 1 ? stream
        : stream.flatMap(node -> node.streamPrecursors(levelFromRoot - 1));
  }

  @NotNull
  public Stream<PrecursorIonTreeNode> streamWholeTree() {
    return Stream.concat(Stream.of(this),
        getChildPrecursors().stream().flatMap(PrecursorIonTreeNode::streamWholeTree));
  }

  public void addFragmentScan(Scan scan) {
    fragmentScans.add(scan);
  }

  public void addChildFragmentScan(PrecursorIonTreeNode precursor) {
    if (childPrecursors == null) {
      childPrecursors = new ArrayList<>();
    }
    childPrecursors.add(precursor);
  }

  /**
   * Uses {@link ScanUtils#DEFAULT_PRECURSOR_MZ_TOLERANCE} to compare mz values. Precursor ion
   * selection is usually not very precise.
   *
   * @param precursorMZ other m/z
   * @return true if precursor m/z matches to this precursor (within accuracy)
   */
  public boolean matches(double precursorMZ) {
    return Math.round(this.precursorMZ * ScanUtils.DEFAULT_PRECURSOR_MZ_TOLERANCE) == Math.round(
        precursorMZ * ScanUtils.DEFAULT_PRECURSOR_MZ_TOLERANCE);
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
    final int msLevel = prec.getMsLevel();
    if (msLevel != msnIndex + 2) {
      logger.warning(() -> String.format(
          "Wrong sequence in MSn info. msLevels are off. Expected: %d  Actual:%d", (msnIndex),
          msLevel));
      return false;
    }
    final double mz = prec.getIsolationMz();
    PrecursorIonTreeNode child = getChild(mz);

    // last element reached
    if (msnInfo.getPrecursors().size() - 1 == msnIndex) {
      if (child == null) {
        child = new PrecursorIonTreeNode(msLevel, mz, this);
        addChildFragmentScan(child);
      }
      child.addFragmentScan(scan);
      return true;
    } else {
      if (child != null) {
        return child.addChildFragmentScan(scan, msnInfo, msnIndex + 1);
      } else {
        logger.warning(
            () -> String.format("Child for msLevel %d not found scan#%d", (this.msLevel + 1),
                scan.getScanNumber()));
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

  /**
   * Traverses parents to find root.
   *
   * @return the root or this if this is the root (no parent)
   */
  public PrecursorIonTreeNode getRoot() {
    if (parent == null) {
      return this;
    } else {
      return parent.getRoot();
    }
  }

  @Override
  public String toString() {
    if (parent == null) {
      return "m/z " + getFormatted();
    } else {
      return parent + " ↦ " + getFormatted();
    }
  }

  public String getFormatted() {
    return MZmineCore.getConfiguration().getMZFormat().format(precursorMZ) + " (" + countSpectra()
        + ")";
  }

  /**
   * Formatted fragment path form start to this precursor
   *
   * @return formatted stings for each precursor mz and the number of scans
   */
  public String[] getFragmentPath() {
    return streamParents().map(PrecursorIonTreeNode::getFormatted).toArray(String[]::new);
  }

  public int countPrecursors() {
    return childPrecursors == null ? 0 : childPrecursors.size();
  }

  public int countSpectra() {
    return fragmentScans.size();
  }

  @Override
  public int compareTo(@NotNull PrecursorIonTreeNode o) {
    // descending order
    return Double.compare(getPrecursorMZ(), o.getPrecursorMZ()) * -1;
  }

  /**
   * Sorts the whole tree structure by descending m/z
   */
  public void sort() {
    if (childPrecursors != null) {
      Collections.sort(childPrecursors);
      childPrecursors.forEach(PrecursorIonTreeNode::sort);
    }
  }

  /**
   * All fragments scans for all levels, sorted by level and precursor mz (Descending)
   *
   * @return list of all fragment scans
   */
  @NotNull
  public List<Scan> getAllFragmentScans() {
    return streamWholeTree().map(PrecursorIonTreeNode::getFragmentScans).flatMap(Collection::stream)
        .toList();
  }

  public int getMaxMSLevel() {
    return streamWholeTree().mapToInt(PrecursorIonTreeNode::getMsLevel).max().orElse(0);
  }

  public double[] getPrecursorMZList() {
    return streamParents().mapToDouble(PrecursorIonTreeNode::getPrecursorMZ).toArray();
  }

  /**
   * Stream over this tree from root -> this
   *
   * @return stream from root to this node (including this)
   */
  public Stream<PrecursorIonTreeNode> streamParents() {
    Stream<PrecursorIonTreeNode> stream;
    PrecursorIonTreeNode p = getParent();
    if (p != null) {
      stream = p.streamParents();
      return Stream.concat(stream, Stream.of(this));
    } else {
      return Stream.of(this);
    }
  }
}
