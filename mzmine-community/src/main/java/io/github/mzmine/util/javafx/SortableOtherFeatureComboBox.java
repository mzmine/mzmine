/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.util.OtherFeatureSorter;
import io.github.mzmine.util.OtherFeatureSorter.SortingProperty;
import io.github.mzmine.util.SortingDirection;
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

public class SortableOtherFeatureComboBox extends FlowPane {

  private static Logger logger = Logger.getLogger(SortableFeatureComboBox.class.getName());

  private final ComboBox<OtherFeature> otherFeatureBox;
  private final @NotNull ObjectProperty<@NotNull ObservableList<OtherFeature>> otherFeatures = new SimpleObjectProperty<>(
      FXCollections.observableArrayList());
  private final TextField searchBox = new TextField();

  private final FilteredList<OtherFeature> filtered = new FilteredList<>(otherFeatures.get());
  private final ComboBox<SortingProperty> sortBox;

  public SortableOtherFeatureComboBox() {
    super();
    setVgap(5);

    otherFeatureBox = new ComboBox<>(filtered);
    otherFeatureBox.setMinWidth(100);
    otherFeatureBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(OtherFeature f) {
        if (f != null && f.getFeatureData() instanceof OtherTimeSeries series) {
          return series.getName();
        }
        return f != null ? f.toString() : "";
      }

      @Override
      public OtherFeature fromString(String string) {
        return null;
      }
    });

    final List<SortingProperty> sortingProperties = new ArrayList<>(
        List.of(SortingProperty.values()));
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
    getChildren().addAll(otherFeatureBox, newHBox(sortPane, searchField));
    setAlignment(Pos.CENTER_LEFT);
  }

  private void sortBaseList() {
    var newValue = sortBox.getValue();
    if (newValue == null) {
      return;
    }
    if (otherFeatures.get() == null) {
      return;
    }
    final OtherFeatureSorter sorter = getSorter(newValue);
    final List<OtherFeature> copy = new ArrayList<>(otherFeatures.get());
    final OtherFeature selectedFeature = getSelectedFeature();
    try {
      copy.sort(sorter);
    } catch (NullPointerException e) {
      otherFeatures.get().setAll(copy);
      logger.log(Level.WARNING, "Error while sorting feature list.", e);
    }
    otherFeatures.get().setAll(copy);
    if (selectedFeature != null && copy.contains(selectedFeature)) {
      setSelectedFeature(selectedFeature);
    }
  }

  private void filterBySearchField(String text, @Nullable OtherFeature previouslySelected) {
    if (text == null || text.isBlank()) {
      filtered.setPredicate(null);
      otherFeatureBox.setValue(previouslySelected);
      return;
    }
    filtered.setPredicate(feature -> feature.toString().contains(text.trim()));
    if (previouslySelected != null && filtered.contains(previouslySelected)) {
      setSelectedFeature(previouslySelected);
      return;
    }

    if (!filtered.isEmpty()) {
      otherFeatureBox.getSelectionModel().select(0);
      return;
    }
    setSelectedFeature(null);
  }

  private OtherFeatureSorter getSorter(OtherFeatureSorter.SortingProperty property) {
    return new OtherFeatureSorter(property, SortingDirection.Ascending);
  }

  public ReadOnlyObjectProperty<OtherFeature> selectedFeatureProperty() {
    return otherFeatureBox.getSelectionModel().selectedItemProperty();
  }

  public OtherFeature getSelectedFeature() {
    return otherFeatureBox.getValue();
  }

  public void setSelectedFeature(OtherFeature f) {
    if (f == null) {
      otherFeatureBox.getSelectionModel().clearSelection();
      return;
    }
    if (filtered.contains(f)) {
      // check if we contain the feature in the filtered list. otherwise it will not keep the
      // filtering properly when changing the feature list/setting new items.
      otherFeatureBox.setValue(f);
      return;
    }

    final OtherFeature rawTrace = f.get(RawTraceType.class);
    if(filtered.contains(rawTrace)) {
      otherFeatureBox.setValue(rawTrace);
      return;
    }

    final OtherFeature preProcessedFeatureForTrace = f.getOtherDataFile().getOtherTimeSeriesData()
        .getPreProcessedFeatureForTrace(rawTrace);
    if (filtered.contains(preProcessedFeatureForTrace)) {
      otherFeatureBox.setValue(preProcessedFeatureForTrace);
    }
  }

  public List<OtherFeature> getItems() {
    return otherFeatures.get();
  }

  public void setItems(List<? extends OtherFeature> items) {
    final List<OtherFeature> copy = new ArrayList<>(items);
    otherFeatures.get().setAll(copy);
    sortBaseList();
    filterBySearchField(searchBox.getText(), null);
  }

  public void setConverter(StringConverter<OtherFeature> converter) {
    otherFeatureBox.setConverter(converter);
  }
}
