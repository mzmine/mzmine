package io.github.mzmine.util.javafx;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

public class SortableFeatureComboBox extends FlowPane {

  private final ComboBox<Feature> data;

  public SortableFeatureComboBox() {
    super();
    setVgap(5);

    data = new ComboBox<>();
    final ComboBox<SortingProperty> sortBox = new ComboBox<>(
        FXCollections.observableArrayList(SortingProperty.values()));

    sortBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
      if(newValue == null) {
        return;
      }
      final List<Feature> arrayList = new ArrayList<>(data.getItems());
      final FeatureSorter sorter = getSorter(sortBox.getValue());
      arrayList.sort(sorter);
      data.setItems(FXCollections.observableArrayList(arrayList));
    }));

    final FlowPane sortPane = new FlowPane(new Label("Sort by: "), sortBox);
    setHgap(5);
    getChildren().addAll(data, sortPane);
  }

  private FeatureSorter getSorter(SortingProperty property) {
    return new FeatureSorter(property, SortingDirection.Ascending);
  }

  public ComboBox<Feature> getFeatureBox() {
    return data;
  }
}
