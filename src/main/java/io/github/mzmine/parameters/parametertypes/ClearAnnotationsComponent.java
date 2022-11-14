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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.features.types.DataType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.CheckTreeView;

public class ClearAnnotationsComponent extends BorderPane {

  private final CheckTreeView<DataType<?>> treeView;

  public ClearAnnotationsComponent(Map<DataType<?>, Boolean> value) {

    treeView = new CheckTreeView<>();

    final CheckBoxTreeItem<DataType<?>> root = new CheckBoxTreeItem<>();
    treeView.setRoot(root);
    root.setExpanded(true);

    setCenter(treeView);
    setValue(value);
  }

  public void setValue(Map<DataType<?>, Boolean> value) {
    treeView.getRoot().getChildren().clear();
    value.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getHeaderString()))
        .forEach(entry -> {
          final CheckBoxTreeItem<DataType<?>> item = new CheckBoxTreeItem<>(entry.getKey(), null,
              entry.getValue());
          treeView.getRoot().getChildren().add(item);
        });
  }

  public Map<DataType<?>, Boolean> getValue() {
    final var children = treeView.getRoot().getChildren();
    final var value = new LinkedHashMap<DataType<?>, Boolean>();
    for (TreeItem<DataType<?>> child : children) {
      final CheckBoxTreeItem<DataType<?>> checkItem = (CheckBoxTreeItem<DataType<?>>) child;
      value.put(checkItem.getValue(), checkItem.isSelected());
    }
    return value;
  }
}
