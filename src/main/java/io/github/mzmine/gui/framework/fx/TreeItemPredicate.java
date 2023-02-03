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

package io.github.mzmine.gui.framework.fx;

import java.util.Arrays;
import java.util.function.Predicate;
import javafx.scene.control.TreeItem;

/**
 * Used in {@link FilterableTreeItem}
 *
 * @param <T> the value type within the treeitem
 */
public interface TreeItemPredicate<T> {

  /**
   * Creates a TreeItemPredicate
   *
   * @param predicate
   * @param <T>       value type within treeitem
   * @return a filter
   */
  static <T> TreeItemPredicate<T> create(Predicate<T> predicate) {
    return (parent, value) -> predicate.test(value);
  }

  /**
   * Creates a TreeItemPredicate that matches a sub string within the name of the tree items
   *
   * @param subStr a sub string to match. will be stripped and toLower for matching
   * @return a sub string filter
   */
  static TreeItemPredicate<Object> createSubStringPredicate(final String subStr) {
    final String cleanSub = subStr.strip().toLowerCase();
    return create(name -> subStr.isBlank() || name.toString().toLowerCase().contains(cleanSub));
  }

  /**
   * Creates a TreeItemPredicate that matches a sub string within the name of the tree items
   *
   * @param subStrs a list of sub strings to match. will be stripped and toLower for matching
   * @return a sub string filter
   */
  static TreeItemPredicate<Object> createSubStringPredicate(final String[] subStrs) {
    final boolean noFilter =
        subStrs == null || subStrs.length == 0 || Arrays.stream(subStrs).allMatch(String::isBlank);
    String[] cleanStrs = subStrs == null ? null
        : Arrays.stream(subStrs).map(String::strip).map(String::toLowerCase).toArray(String[]::new);
    return create(actor -> {
      String title = actor.toString().toLowerCase();
      return noFilter || Arrays.stream(cleanStrs).allMatch(subStr -> title.contains(subStr));
    });
  }

  /**
   * The actual test if a tree item conforms with this predicate
   *
   * @param parent     the parent treeitem
   * @param childValue the value in within the child treeitem
   * @return true if match
   */
  boolean test(TreeItem<T> parent, T childValue);

}
