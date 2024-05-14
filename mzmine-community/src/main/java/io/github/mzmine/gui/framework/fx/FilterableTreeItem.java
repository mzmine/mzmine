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

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class FilterableTreeItem<T> extends TreeItem<T> {

  private final ObservableList<FilterableTreeItem<T>> sourceChildren = FXCollections.observableArrayList();
  private TreeItemPredicate<T> predicate = TreeItemPredicate.create(t -> true);

  public FilterableTreeItem(T value) {
    super(value);
    sourceChildren.addListener(
        (ListChangeListener<? super FilterableTreeItem<T>>) c -> expandAllMatches(predicate));
  }

  public ObservableList<FilterableTreeItem<T>> getSourceChildren() {
    return sourceChildren;
  }

  public void setPredicate(final TreeItemPredicate<T> predicate) {
    this.predicate = predicate != null ? predicate : TreeItemPredicate.create(v -> true);
  }

  /**
   * Expands all matching nodes if on of their children has a match
   *
   * @param filter the sub string filter
   * @return the first matching leaf - or other node if no leaf matches
   */
  public FilterableTreeItem<T> expandAllMatches(final TreeItemPredicate<T> filter) {
    setPredicate(filter);
    // prefer leaf to other nodes
    FilterableTreeItem<T> firstMatch = null;

    getChildren().setAll(getSourceChildren());

    for (final FilterableTreeItem<T> child : getSourceChildren()) {
      var match = child.expandAllMatches(filter);
      if (match != null && (firstMatch == null || !firstMatch.isLeaf() && match.isLeaf())) {
        firstMatch = match;
      }
    }
    if (firstMatch == null && filter.test(this.getParent(), this.getValue())) {
      firstMatch = this;
    }

    this.setExpanded(firstMatch != null);

    // only show expanded
    getChildren().setAll(sourceChildren.stream().filter(TreeItem::isExpanded).toList());

    return firstMatch;
  }
}