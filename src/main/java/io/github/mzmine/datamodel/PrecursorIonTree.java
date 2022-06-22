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

package io.github.mzmine.datamodel;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * A tree structure to temporarily capture a precursor and all its fragment scans (MSn)
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class PrecursorIonTree implements Comparable<PrecursorIonTree> {

  // root
  private final PrecursorIonTreeNode root;

  public PrecursorIonTree(PrecursorIonTreeNode root) {
    this.root = root;
  }

  public PrecursorIonTreeNode getRoot() {
    return root;
  }

  @Override
  public int compareTo(@NotNull PrecursorIonTree o) {
    // descending order
    return root.compareTo(o.getRoot());
  }

  public void sort() {
    root.sort();
  }

  @NotNull
  public Stream<PrecursorIonTreeNode> stream() {
    return root.streamWholeTree();
  }

  public int getMaxMSLevel() {
    return root.getMaxMSLevel();
  }

  public int countSpectra() {
    return stream().mapToInt(PrecursorIonTreeNode::countSpectra).sum();
  }

  public int countPrecursor() {
    return stream().mapToInt(PrecursorIonTreeNode::countPrecursors).sum();
  }

  public int countSpectra(int msLevel) {
    return stream().filter(ion -> msLevel == ion.getMsLevel())
        .mapToInt(PrecursorIonTreeNode::countSpectra).sum();
  }

  public int countPrecursor(int msLevel) {
    return stream().filter(ion -> msLevel - 1 == ion.getMsLevel())
        .mapToInt(PrecursorIonTreeNode::countPrecursors).sum();
  }
}
