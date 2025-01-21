/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.collections;

import it.unimi.dsi.fastutil.ints.IntList;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Collection API related utilities
 */
public class CollectionUtils {

  /**
   * Map of the object to its index to avoid indexOf. This method will take any collection as input
   * and this makes only sense if the collection has an order.
   *
   * @param list any collection
   * @param <T>  the object to be mapped
   * @return Map object to index in collection
   */
  public static <T> Map<T, Integer> indexMap(Collection<T> list) {
    Map<T, Integer> map = new LinkedHashMap<>(list.size());
    int i = 0;
    for (final T value : list) {
      map.put(value, i);
      i++;
    }
    return map;
  }

  /**
   * Map of the object to its index to avoid indexOf.
   *
   * @param <T> the object to be mapped
   * @return Map object to index in collection
   */
  public static <T> Map<T, Integer> indexMap(T[] array) {
    Map<T, Integer> map = new LinkedHashMap<>(array.length);
    for (int i = 0; i < array.length; i++) {
      map.put(array[i], i);
    }
    return map;
  }

  /**
   * drops duplicate values using a HashSet. does not retain order
   *
   * @param list list to modify
   */
  public static <T> void dropDuplicates(List<T> list) {
    Set<T> set = new HashSet<>(list);
    list.clear();
    list.addAll(set);
  }

  /**
   * drops duplicate values using a HashSet. does not retain order
   *
   * @param list list to modify
   * @param <T>
   */
  public static <T> void dropDuplicatesRetainOrder(List<T> list) {
    Set<T> set = new LinkedHashSet<>(list);
    list.clear();
    list.addAll(set);
  }

  /**
   * Converts an array of ints to array of Integers
   */
  public static Integer[] toIntegerArray(int[] array) {
    Integer[] newArray = new Integer[array.length];
    for (int i = 0; i < array.length; i++) {
      newArray[i] = Integer.valueOf(array[i]);
    }
    return newArray;
  }

  /**
   * Change the type of array of Objects to an array of objects of type newClass.
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] changeArrayType(Object[] array, Class<T> newClass) {

    ArrayList<T> newArray = new ArrayList<T>();

    for (int i = 0; i < array.length; i++) {
      // Only add those objects that can be cast to the new class
      if (newClass.isInstance(array[i])) {
        newArray.add(newClass.cast(array[i]));
      }
    }

    return newArray.toArray((T[]) Array.newInstance(newClass, 0));
  }

  /**
   * Checks if the haystack array contains all elements of needles array
   *
   * @param haystack array of ints
   * @param needles  array of ints
   * @return true if haystack contains all elements of needles
   */
  public static boolean isSubset(int[] haystack, int[] needles) {
    needleTraversal:
    for (int i = 0; i < needles.length; i++) {
      for (int j = 0; j < haystack.length; j++) {
        if (needles[i] == haystack[j]) {
          continue needleTraversal;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Checks if the haystack array contains a specified element
   *
   * @param haystack array of objects
   * @param needle   object
   * @return true if haystack contains needle
   */
  public static <T> boolean arrayContains(T[] haystack, T needle) {
    for (T test : haystack) {
      if (needle.equals(test)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Concatenate two arrays
   *
   * @param first  array of objects
   * @param second array of objects
   * @return both array of objects
   */
  public static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  /**
   * creates a stream of duplicates. Filters by hashset
   *
   * @param input the input stream
   * @return filtered stream of duplicates
   */
  public static <T> Stream<T> streamDuplicates(Stream<T> input) {
    Set<T> items = new HashSet<>();
    return input.filter(n -> !items.add(n));
  }


  /**
   * Creates a new array. Copied from {@link Arrays#copyOfRange(Object[], int, int)}
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] createArray(final T[] data, final int newLength) {
    Class<? extends T[]> newType = (Class<? extends T[]>) data.getClass();
    T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
        : (T[]) Array.newInstance(newType.getComponentType(), newLength);
    return copy;
  }

  /**
   * Operation to remove elements by indices from large lists. Avoids copies.
   *
   * @param list            changed in place
   * @param indicesToRemove list of indices to remove - sorted ascending
   * @return the input list that is changed in place
   */
  public static <T> List<T> removeIndicesInPlaceSorted(List<T> list, IntList indicesToRemove) {
    return removeIndicesInPlaceSorted(list, indicesToRemove.toIntArray());
  }

  /**
   * Operation to remove elements by indices from large lists. Avoids copies.
   *
   * @param list            changed in place
   * @param indicesToRemove list of indices to remove - sorted ascending
   * @return the input list that is changed in place
   */
  public static <T> List<T> removeIndicesInPlaceSorted(List<T> list, int[] indicesToRemove) {
    if (indicesToRemove.length == 0) {
      return list;
    }
    int size = list.size();

    int indicesToRemoveIndex = 0;
    int nextToSkip = indicesToRemove[0];
    // Overwrite elements not to remove
    int writeIndex = nextToSkip; // start writing at index of first to remove
    // start reading at first to remove+1
    for (int readIndex = writeIndex; readIndex < size; readIndex++) {
      if (nextToSkip == readIndex) {
        indicesToRemoveIndex++;
        if (indicesToRemoveIndex < indicesToRemove.length) {
          nextToSkip = indicesToRemove[indicesToRemoveIndex];
          if (nextToSkip <= readIndex) {
            throw new IllegalArgumentException("Indices to remove are not sorted ascending");
          }
        }
      } else {
        list.set(writeIndex, list.get(readIndex));
        writeIndex++;
      }
    }

    // Remove extra elements at the end
    if (list.size() > writeIndex) {
      list.subList(writeIndex, list.size()).clear();
    }
    return list;
  }

  /**
   * Operation to remove elements by indices from large lists. Avoids copies.
   *
   * @param list            changed in place
   * @param indicesToRemove list of indices to remove - can be unsorted
   * @return the input list that is changed in place
   */
  public static <T> List<T> removeIndicesInPlaceBitSet(List<T> list, IntList indicesToRemove) {
    return removeIndicesInPlaceBitSet(list, indicesToRemove.toIntArray());
  }

  /**
   * Operation to remove elements by indices from large lists. Avoids copies.
   *
   * @param list            changed in place
   * @param indicesToRemove list of indices to remove - can be unsorted
   * @return the input list that is changed in place
   */
  public static <T> List<T> removeIndicesInPlaceBitSet(List<T> list, int[] indicesToRemove) {
    if (indicesToRemove.length == 0) {
      return list;
    }

    int size = list.size();
    BitSet toRemove = new BitSet(size);

    // Mark indices to remove
    for (int index : indicesToRemove) {
      if (index >= 0 && index < size) {
        toRemove.set(index);
      }
    }

    // Overwrite elements not to remove
    int writeIndex = indicesToRemove[0]; // start writing at index of first to remove
    // start reading at first to remove+1
    for (int readIndex = writeIndex + 1; readIndex < size; readIndex++) {
      if (!toRemove.get(readIndex)) {
        list.set(writeIndex, list.get(readIndex));
        writeIndex++;
      }
    }

    // Remove extra elements at the end
    if (list.size() > writeIndex) {
      list.subList(writeIndex, list.size()).clear();
    }
    return list;
  }

  public static <T> List<T> combine(List<? extends T>... lists) {
    int size = 0;
    for (final List<? extends T> list : lists) {
      size += list.size();
    }

    final List<T> value = new ArrayList<>(size);
    for (List<? extends T> list : lists) {
      value.addAll(list);
    }
    return value;
  }

  public static int[] argsortReversed(float[] array) {
    return IntStream.range(0, array.length).boxed()
        .sorted((a, b) -> Float.compare(array[b], array[a])).mapToInt(Integer::intValue).toArray();
  }

  public static @NotNull <T> Collector<T, ?, ArrayList<T>> toArrayList() {
    return Collectors.toCollection(ArrayList::new);
  }
}
