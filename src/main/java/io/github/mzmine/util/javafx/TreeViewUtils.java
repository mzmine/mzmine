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

package io.github.mzmine.util.javafx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.Nullable;

public class TreeViewUtils {

  /**
   * @param values The values.
   * @param treeItems The tree items.
   * @param <T> The type.
   * @return Returns a list of tree items for the requested values. May be empty.
   */
  public static <T> List<TreeItem<T>> getTreeItemsByValue(Collection<T> values,
      List<TreeItem<T>> treeItems) {
    List<TreeItem<T>> foundItems = new ArrayList<>();

    for (T value : values) {
      final TreeItem<T> item = getTreeItemByValue(value, treeItems);
      if(item != null) {
        foundItems.add(item);
      }
    }

    return foundItems;
  }

  /**
   *
   * @param value The value.
   * @param treeItems  The tree items to search.
   * @param <T> The type.
   * @return The tree item with the given value or null.
   */
  @Nullable
  public static <T> TreeItem<T> getTreeItemByValue(T value,
      List<TreeItem<T>> treeItems) {
    for (var item : treeItems) {
      if (item.getValue().equals(value)) {
        return item;
      }
    }
    return null;
  }
}
