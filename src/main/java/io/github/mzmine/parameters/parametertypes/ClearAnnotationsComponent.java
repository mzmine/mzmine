/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
