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

import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.SimpleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A lightweight alternative to guava's {@link TreeRangeMap} for non-overlapping, closed
 * {@link SimpleDoubleRange}s. Open bounds are not needed, because doubles are discrete: an open
 * bound {@code (b} is equivalent to the closed bound {@code [Math.nextUp(b)}. Ranges that are
 * created by {@link #putNonOverlapping(SimpleDoubleRange, Object)} are therefore seamless to their
 * neighbors.
 * <p>
 * Backed by an AVL tree keyed by the lower bound of each range. The tree is stored in parallel
 * primitive arrays, so lookups do not box keys, do not allocate and do not dereference node
 * objects.
 *
 * @param <V> the value type
 */
public class SimpleDoubleRangeMap<V> {

  private static final int NIL = -1;
  private static final int DEFAULT_CAPACITY = 16;

  // decision: custom tree instead of java.util.TreeMap (boxes every lookup key) or fastutil tree
  // maps (no floor/ceiling lookups, emulating them with head/tail maps allocates two views per
  // lookup). Nodes are array indices, the used indices are always 0 to size - 1.
  private double[] lowers;
  private double[] uppers;
  private int[] left;
  private int[] right;
  // AVL height is at most ~1.44 * log2(n), fits a byte for any array size
  private byte[] heights;
  private Object[] entries;
  private int size = 0;
  private int root = NIL;

  public SimpleDoubleRangeMap() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * @param initialCapacity the expected number of ranges, the map grows if needed
   */
  public SimpleDoubleRangeMap(final int initialCapacity) {
    final int capacity = Math.max(1, initialCapacity);
    lowers = new double[capacity];
    uppers = new double[capacity];
    left = new int[capacity];
    right = new int[capacity];
    heights = new byte[capacity];
    entries = new Object[capacity];
  }

  /**
   * @return the entry whose range contains the value or null
   */
  @Nullable
  public SimpleRangeEntry<V> getEntry(final double value) {
    final int node = floorNode(value);
    return node != NIL && uppers[node] >= value ? entry(node) : null;
  }

  /**
   * @return the value mapped to the range containing the value or null
   */
  @Nullable
  public V get(final double value) {
    final SimpleRangeEntry<V> entry = getEntry(value);
    return entry == null ? null : entry.value();
  }

  /**
   * Trims the proposed range so that it does not overlap any range in this map. The trimmed bounds
   * connect seamlessly to the neighboring ranges ({@link Math#nextUp(double)} and
   * {@link Math#nextDown(double)} of the neighbor's bound), so no gaps are introduced.
   *
   * @param proposed the proposed range. Must not fully enclose a range of this map.
   * @return the trimmed range
   * @throws IllegalStateException if the proposed range fully encloses a range of this map or if
   *                               the proposed range is completely covered by existing ranges.
   */
  @NotNull
  public SimpleDoubleRange createNonOverlappingRange(@NotNull final SimpleDoubleRange proposed) {
    double lower = proposed.lower();
    double upper = proposed.upper();

    final int leftNode = floorNode(lower);
    if (leftNode != NIL && uppers[leftNode] >= lower) {
      lower = Math.nextUp(uppers[leftNode]);
    }

    // the first range starting at or after the (trimmed) lower bound
    final int rightNode = ceilingNode(lower);
    if (rightNode != NIL && lowers[rightNode] <= upper) {
      if (uppers[rightNode] < upper) {
        // decision: the guava based SpectraMerging#createNewNonOverlappingRange only checks the
        // ranges that contain the proposed bounds and silently overwrites enclosed ranges. This
        // should never happen if all ranges are created with the same tolerance, so fail loudly.
        throw new IllegalStateException(
            "Proposed range %s encloses the existing range %s.".formatted(proposed,
                entry(rightNode).range()));
      }
      upper = Math.nextDown(lowers[rightNode]);
    }

    if (lower > upper) {
      throw new IllegalStateException(
          "Proposed range %s is completely covered by existing ranges.".formatted(proposed));
    }

    if (lower == proposed.lower() && upper == proposed.upper()) {
      return proposed;
    }
    return SimpleRange.ofDouble(lower, upper);
  }

  /**
   * Trims the proposed range with {@link #createNonOverlappingRange(SimpleDoubleRange)} and maps
   * the trimmed range to the value.
   *
   * @return the new entry with the trimmed range
   */
  @NotNull
  public SimpleRangeEntry<V> putNonOverlapping(@NotNull final SimpleDoubleRange proposed,
      final V value) {
    final SimpleRangeEntry<V> entry = new SimpleRangeEntry<>(createNonOverlappingRange(proposed),
        value);
    insert(entry);
    return entry;
  }

  /**
   * Maps the range to the value.
   *
   * @throws IllegalArgumentException if the range overlaps a range of this map
   */
  public void put(@NotNull final SimpleDoubleRange range, final V value) {
    // the range with the greatest lower bound <= upper is the only candidate for an overlap
    final int floor = floorNode(range.upper());
    if (floor != NIL && uppers[floor] >= range.lower()) {
      throw new IllegalArgumentException(
          "Range %s overlaps the existing range %s.".formatted(range, entry(floor).range()));
    }
    insert(new SimpleRangeEntry<>(range, value));
  }

  /**
   * Removes the range that contains the value.
   *
   * @return the removed entry or null if no range contains the value
   */
  @Nullable
  public SimpleRangeEntry<V> remove(final double value) {
    final int node = floorNode(value);
    if (node == NIL || uppers[node] < value) {
      return null;
    }
    final SimpleRangeEntry<V> removed = entry(node);
    root = remove(root, lowers[node]);
    compact(node);
    return removed;
  }

  /**
   * @return all entries sorted by ascending range, unmodifiable copy
   */
  @NotNull
  public List<SimpleRangeEntry<V>> entries() {
    final List<SimpleRangeEntry<V>> result = new ArrayList<>(size);
    // iterative in-order traversal, the stack never exceeds the tree height
    final int[] stack = new int[Math.max(1, maxHeight())];
    int stackSize = 0;
    int node = root;
    while (node != NIL || stackSize > 0) {
      while (node != NIL) {
        stack[stackSize++] = node;
        node = left[node];
      }
      node = stack[--stackSize];
      result.add(entry(node));
      node = right[node];
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Similar to guava's {@link TreeRangeMap#asMapOfRanges()}, but a copy instead of a view: later
   * changes to this range map are not reflected.
   *
   * @return all ranges mapped to their values, sorted by ascending range, unmodifiable copy
   */
  @NotNull
  public SequencedMap<SimpleDoubleRange, V> asMapOfRanges() {
    final LinkedHashMap<SimpleDoubleRange, V> map = LinkedHashMap.newLinkedHashMap(size);
    for (final SimpleRangeEntry<V> entry : entries()) {
      map.put(entry.range(), entry.value());
    }
    return Collections.unmodifiableSequencedMap(map);
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void clear() {
    Arrays.fill(entries, 0, size, null);
    size = 0;
    root = NIL;
  }

  /**
   * @return the node with the greatest lower bound <= key or {@link #NIL}
   */
  private int floorNode(final double key) {
    int node = root;
    int result = NIL;
    while (node != NIL) {
      if (lowers[node] <= key) {
        result = node;
        node = right[node];
      } else {
        node = left[node];
      }
    }
    return result;
  }

  /**
   * @return the node with the smallest lower bound >= key or {@link #NIL}
   */
  private int ceilingNode(final double key) {
    int node = root;
    int result = NIL;
    while (node != NIL) {
      if (lowers[node] >= key) {
        result = node;
        node = left[node];
      } else {
        node = right[node];
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @NotNull
  private SimpleRangeEntry<V> entry(final int node) {
    return (SimpleRangeEntry<V>) entries[node];
  }

  /**
   * Inserts the entry. The caller must make sure that it does not overlap an existing range.
   */
  private void insert(@NotNull final SimpleRangeEntry<V> entry) {
    ensureCapacity(size + 1);
    final int node = size++;
    lowers[node] = entry.range().lower();
    uppers[node] = entry.range().upper();
    left[node] = NIL;
    right[node] = NIL;
    heights[node] = 1;
    entries[node] = entry;
    root = insert(root, node);
  }

  /**
   * @return the new root of the subtree
   */
  private int insert(final int subtree, final int node) {
    if (subtree == NIL) {
      return node;
    }
    // assumption: lower bounds are unique, because ranges do not overlap
    if (lowers[node] < lowers[subtree]) {
      left[subtree] = insert(left[subtree], node);
    } else {
      right[subtree] = insert(right[subtree], node);
    }
    return rebalance(subtree);
  }

  /**
   * Unlinks the node with the lower bound key from the subtree. Nodes are relinked instead of
   * copying their data, so all other nodes keep their array index.
   *
   * @param key the lower bound of a node that exists in the subtree
   * @return the new root of the subtree
   */
  private int remove(final int subtree, final double key) {
    if (key < lowers[subtree]) {
      left[subtree] = remove(left[subtree], key);
    } else if (key > lowers[subtree]) {
      right[subtree] = remove(right[subtree], key);
    } else {
      if (left[subtree] == NIL) {
        return right[subtree];
      }
      if (right[subtree] == NIL) {
        return left[subtree];
      }
      // two children: the smallest node of the right subtree takes the place of the removed node
      int successor = right[subtree];
      while (left[successor] != NIL) {
        successor = left[successor];
      }
      right[successor] = removeMin(right[subtree]);
      left[successor] = left[subtree];
      return rebalance(successor);
    }
    return rebalance(subtree);
  }

  /**
   * Unlinks the smallest node of the subtree.
   *
   * @return the new root of the subtree
   */
  private int removeMin(final int subtree) {
    if (left[subtree] == NIL) {
      return right[subtree];
    }
    left[subtree] = removeMin(left[subtree]);
    return rebalance(subtree);
  }

  /**
   * Moves the last node into the slot of the removed node, so the used slots stay contiguous.
   *
   * @param removed the index of a node that is no longer linked in the tree
   */
  private void compact(final int removed) {
    final int last = size - 1;
    if (removed != last) {
      // relink the parent of the last node to its new index
      if (root == last) {
        root = removed;
      } else {
        final double key = lowers[last];
        int parent = root;
        while (true) {
          if (key < lowers[parent]) {
            if (left[parent] == last) {
              left[parent] = removed;
              break;
            }
            parent = left[parent];
          } else {
            if (right[parent] == last) {
              right[parent] = removed;
              break;
            }
            parent = right[parent];
          }
        }
      }
      lowers[removed] = lowers[last];
      uppers[removed] = uppers[last];
      left[removed] = left[last];
      right[removed] = right[last];
      heights[removed] = heights[last];
      entries[removed] = entries[last];
    }
    entries[last] = null;
    size--;
  }

  private int rebalance(final int node) {
    updateHeight(node);
    final int balance = height(left[node]) - height(right[node]);
    if (balance > 1) {
      if (height(left[left[node]]) < height(right[left[node]])) {
        left[node] = rotateLeft(left[node]);
      }
      return rotateRight(node);
    }
    if (balance < -1) {
      if (height(right[right[node]]) < height(left[right[node]])) {
        right[node] = rotateRight(right[node]);
      }
      return rotateLeft(node);
    }
    return node;
  }

  private int rotateRight(final int node) {
    final int newRoot = left[node];
    left[node] = right[newRoot];
    right[newRoot] = node;
    updateHeight(node);
    updateHeight(newRoot);
    return newRoot;
  }

  private int rotateLeft(final int node) {
    final int newRoot = right[node];
    right[node] = left[newRoot];
    left[newRoot] = node;
    updateHeight(node);
    updateHeight(newRoot);
    return newRoot;
  }

  private int height(final int node) {
    return node == NIL ? 0 : heights[node];
  }

  private int maxHeight() {
    return height(root);
  }

  private void updateHeight(final int node) {
    heights[node] = (byte) (1 + Math.max(height(left[node]), height(right[node])));
  }

  private void ensureCapacity(final int capacity) {
    if (capacity <= lowers.length) {
      return;
    }
    final int newCapacity = Math.max(capacity, lowers.length + (lowers.length >> 1));
    lowers = Arrays.copyOf(lowers, newCapacity);
    uppers = Arrays.copyOf(uppers, newCapacity);
    left = Arrays.copyOf(left, newCapacity);
    right = Arrays.copyOf(right, newCapacity);
    heights = Arrays.copyOf(heights, newCapacity);
    entries = Arrays.copyOf(entries, newCapacity);
  }
}
