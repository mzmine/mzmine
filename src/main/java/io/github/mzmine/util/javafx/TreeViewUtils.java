package io.github.mzmine.util.javafx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.TreeItem;
import javax.annotation.Nullable;

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
