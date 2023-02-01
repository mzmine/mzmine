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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

public class FilterableTreeItem<T> extends TreeItem<T> {

  private final ObservableList<FilterableTreeItem<T>> sourceChildren = FXCollections.observableArrayList();
  private final ObjectProperty<TreeItemPredicate<T>> predicate = new SimpleObjectProperty<>();

  public FilterableTreeItem(T value) {
    super(value);
    FilteredList<FilterableTreeItem<T>> filteredChildren = new FilteredList<>(sourceChildren);
    filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() -> child -> {
      if (child instanceof FilterableTreeItem<T> fchild) {
        fchild.predicateProperty().set(predicate.get());
      }
      if (predicate.get() == null || !child.getChildren().isEmpty()) {
        return true;
      }
      return this.predicate.get().test(this, child.getValue());
    }, predicate));

    Bindings.bindContent(super.getChildren(), filteredChildren);
  }

  public ObservableList<FilterableTreeItem<T>> getSourceChildren() {
    return sourceChildren;
  }

  public ObjectProperty<TreeItemPredicate<T>> predicateProperty() {
    return predicate;
  }

}