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

package io.github.mzmine.parameters.parametertypes.datatype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.CheckListView;
import org.jetbrains.annotations.Nullable;

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

  public void setValue(@Nullable Map<String, Boolean> map) {
    dataTypes.clear();
    if (map == null) {
      return;
    }

    map.keySet().stream().sorted().forEach(dataTypes::add);
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
