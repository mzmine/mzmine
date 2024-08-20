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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.Nullable;

public class OtherFeatureSelectionPane extends GridPane {

  private final ReadOnlyObjectWrapper<@Nullable OtherFeature> featureProperty = new ReadOnlyObjectWrapper<>();

  public OtherFeatureSelectionPane() {
    setVgap(FxLayout.DEFAULT_SPACE);
    setHgap(FxLayout.DEFAULT_SPACE);
    setAlignment(Pos.TOP_LEFT);

    final ObjectProperty<RawDataFile> file = new SimpleObjectProperty<>();
    final ComboBox<RawDataFile> rawFileBox = FxComboBox.createComboBox("Select a raw data file.",
        new ArrayList<>(ProjectService.getProject().getCurrentRawDataFiles()), file);
    rawFileBox.setMinWidth(100);

    final ListProperty<OtherDataFile> otherFiles = new SimpleListProperty<>(
        FXCollections.observableArrayList());
    final ObjectProperty<OtherDataFile> otherFile = new SimpleObjectProperty<>();
    final ComboBox<OtherDataFile> otherFileBox = FxComboBox.createComboBox(
        "Select a collection of traces", otherFiles, otherFile);
    otherFileBox.setMinWidth(100);

    final ObjectProperty<OtherRawOrProcessed> rawOrProcessed = new SimpleObjectProperty<>(
        OtherRawOrProcessed.RAW);
    final ComboBox<OtherRawOrProcessed> rawOrProcessedBox = FxComboBox.createComboBox(
        "Select the time series type", List.of(OtherRawOrProcessed.values()), rawOrProcessed);
    rawOrProcessedBox.setMinWidth(40);

    final SortableOtherFeatureComboBox otherFeatureCombo = new SortableOtherFeatureComboBox();
    featureProperty.bind(otherFeatureCombo.selectedFeatureProperty());

    add(FxLabels.newLabel("1. MS data file:"), 0, 0);
    add(rawFileBox, 1, 0);
    add(FxLabels.newLabel("2. Other data:"), 2, 0);
    add(otherFileBox, 3, 0);
    add(rawOrProcessedBox, 4, 0);
    add(FxLabels.newLabel("3. Feature:"), 0, 1);
    add(otherFeatureCombo, 1, 1, 3, 1);

    getColumnConstraints().add(new ColumnConstraints(80));
    getColumnConstraints().add(
        new ColumnConstraints(80, USE_PREF_SIZE, 800, Priority.SOMETIMES, HPos.LEFT, true));
    getColumnConstraints().add(new ColumnConstraints(80));
    getColumnConstraints().add(
        new ColumnConstraints(80, USE_PREF_SIZE, 800, Priority.SOMETIMES, HPos.LEFT, true));

    // need to use listeners to the actual properties. the bound properties do not trigger the listener
    rawFileBox.valueProperty().addListener((_, _, f) -> {
      if (f == null) {
        otherFiles.clear();
        return;
      }
      otherFiles.setAll(
          f.getOtherDataFiles().stream().filter(OtherDataFile::hasTimeSeries).toList());
    });

    otherFileBox.valueProperty().addListener((_, _, f) -> {
      if (f == null || !f.hasTimeSeries()) {
        otherFeatureCombo.setItems(List.of());
        return;
      }

      otherFeatureCombo.setItems(
          rawOrProcessed.get().streamMatching(f.getOtherTimeSeries()).toList());
    });

    rawOrProcessedBox.valueProperty().addListener((_, _, rop) -> {
      if (rop == null) {
        return;
      }

      if (otherFileBox.getValue() != null) {
        otherFeatureCombo.setItems(
            rawOrProcessed.get().streamMatching(otherFileBox.getValue().getOtherTimeSeries())
                .toList());
      }
    });
  }

  @Nullable
  public OtherFeature getFeature() {
    return featureProperty.get();
  }

  public ReadOnlyObjectProperty<@Nullable OtherFeature> featureProperty() {
    return featureProperty.getReadOnlyProperty();
  }
}
