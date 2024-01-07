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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * A tree structure to temporarily capture a precursor and all its fragment scans (MSn)
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
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

  /**
   * Stream the whole tree. {@link PrecursorIonTreeNode#streamWholeTree()}
   *
   * @return stream of the tree nodes
   */
  @NotNull
  public Stream<PrecursorIonTreeNode> stream() {
    return root.streamWholeTree();
  }

  public int getMaxMSLevel() {
    return root.getMaxMSLevel();
  }

  public double getPrecursorMz() {
    return root.getPrecursorMZ();
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

  public List<Scan> getAllFragmentScans() {
    return root.getAllFragmentScans();
  }

  /**
   * maps the MS level to the nodes
   */
  public @NotNull Map<Integer, List<PrecursorIonTreeNode>> groupByMsLevel() {
    return stream().collect(Collectors.groupingBy(PrecursorIonTreeNode::getMsLevel));
  }
}
