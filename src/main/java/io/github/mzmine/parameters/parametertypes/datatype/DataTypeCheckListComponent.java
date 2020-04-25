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

  public List<String> getItems() {
    return dataTypes;
  }

  public void setValue(Map<String, Boolean> map) {
    dataTypes.clear();
    map.keySet().forEach(dt -> dataTypes.add(dt));
    map.forEach((dt, b) -> {
      if (b) {
        checkList.getCheckModel().check(dt);
      }
    });
  }
}
