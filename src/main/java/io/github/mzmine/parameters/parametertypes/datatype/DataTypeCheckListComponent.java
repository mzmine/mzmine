/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes.datatype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.CheckListView;

public class DataTypeCheckListComponent extends StackPane {

  private final CheckListView<String> checkList;
  private final ObservableList<String> dataTypes;
  private final Map<String, Boolean> dataMap;

  public DataTypeCheckListComponent() {
    super();
    dataTypes = FXCollections.observableArrayList();
    checkList = new CheckListView<>();
    checkList.setItems(dataTypes);
    dataMap = new HashMap<>();

    getChildren().add(checkList);

    setPrefSize(200, 200);
  }

  public void addListener(ListChangeListener<String> listener) {
    checkList.getCheckModel().getCheckedItems().addListener(listener);
  }

  public Map<String, Boolean> getValue() {
    dataMap.clear();
    dataTypes.forEach(dt -> dataMap.put(dt, checkList.getCheckModel().isChecked(dt)));
    return dataMap;
  }

  public void setValue(Map<String, Boolean> map) {
    dataTypes.clear();
    map.keySet().stream().sorted().forEach(dt -> dataTypes.add(dt));
    map.forEach((dt, b) -> {
      if (b) {
        checkList.getCheckModel().check(dt);
      }
    });
  }

  public List<String> getItems() {
    return dataTypes;
  }
}
