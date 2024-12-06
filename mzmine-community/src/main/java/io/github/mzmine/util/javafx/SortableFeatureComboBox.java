/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SortableFeatureComboBox extends FlowPane {

  private static Logger logger = Logger.getLogger(SortableFeatureComboBox.class.getName());

  private final ComboBox<Feature> data;
  private final @NotNull ObjectProperty<@NotNull ObservableList<Feature>> features = new SimpleObjectProperty<>(
      FXCollections.observableArrayList());
  private final TextField searchBox = new TextField();

  private final FilteredList<Feature> filtered = new FilteredList<>(features.get());
  private final ComboBox<SortingProperty> sortBox;

  public SortableFeatureComboBox() {
    super();
    setVgap(5);

    data = new ComboBox<>(filtered);
    data.setMinWidth(100);
    // remove intensity to avoid confusion
    final List<SortingProperty> sortingProperties =new ArrayList<>(List.of(SortingProperty.values()));
    sortingProperties.remove(SortingProperty.Intensity);
    sortBox = new ComboBox<>(FXCollections.observableArrayList(sortingProperties));
    sortBox.setValue(SortingProperty.RT);
    sortBox.valueProperty().addListener(((_, _, _) -> sortBaseList()));

    final HBox sortPane = newHBox(new Label("Sort by: "), sortBox);

    searchBox.setPrefColumnCount(20);
    searchBox.textProperty().addListener((_, _, text) -> {
      filterBySearchField(text, getSelectedFeature());
    });
    final HBox searchField = newHBox(new Label("Search: "), searchBox);

    setHgap(5);
    getChildren().addAll(data, newHBox(sortPane, searchField));
    setAlignment(Pos.CENTER_LEFT);
  }

  private void sortBaseList() {
    var newValue = sortBox.getValue();
    if (newValue == null) {
      return;
    }
    if (features.get() == null) {
      return;
    }
    final FeatureSorter sorter = getSorter(newValue);
    final List<Feature> copy = new ArrayList<>(features.get());
    final Feature selectedFeature = getSelectedFeature();
    try {
      copy.sort(sorter);
    } catch (NullPointerException e) {
      features.get().setAll(copy);
      logger.log(Level.WARNING, "Error while sorting feature list.", e);
    }
    features.get().setAll(copy);
    if (selectedFeature != null && copy.contains(selectedFeature)) {
      setSelectedFeature(selectedFeature);
    }
  }

  private void filterBySearchField(String text, @Nullable Feature previouslySelected) {
    if (text == null || text.isBlank()) {
      filtered.setPredicate(null);
      data.setValue(previouslySelected);
      return;
    }
    filtered.setPredicate(feature -> feature.toString().contains(text.trim()));
    if (previouslySelected != null && filtered.contains(previouslySelected)) {
      setSelectedFeature(previouslySelected);
      return;
    }

    if (!filtered.isEmpty()) {
      data.getSelectionModel().select(0);
      return;
    }
    setSelectedFeature(null);
  }

  private FeatureSorter getSorter(SortingProperty property) {
    return new FeatureSorter(property, SortingDirection.Ascending);
  }

  public ReadOnlyObjectProperty<Feature> selectedFeatureProperty() {
    return data.getSelectionModel().selectedItemProperty();
  }

  public Feature getSelectedFeature() {
    return data.getValue();
  }

  public void setSelectedFeature(Feature f) {
    if (filtered.contains(f)) {
      // check if we contain the feature in the filtered list. otherwise it will not keep the
      // filtering properly when changing the feature list/setting new items.
      data.setValue(f);
    }
  }

  public List<Feature> getItems() {
    return features.get();
  }

  public void setItems(List<? extends Feature> items) {
    final ArrayList<Feature> copy = new ArrayList<>(items);
    features.get().setAll(copy);
    sortBaseList();
    filterBySearchField(searchBox.getText(), null);
  }

  public void setConverter(StringConverter<Feature> converter) {
    data.setConverter(converter);
  }
}
