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
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

public class OtherFeatureSelectionPane extends GridPane {

  private final ReadOnlyObjectWrapper<@Nullable OtherFeature> featureProperty = new ReadOnlyObjectWrapper<>();

  public OtherFeatureSelectionPane() {

    final ObjectProperty<RawDataFile> file = new SimpleObjectProperty<>();

    final ComboBox<RawDataFile> rawFileBox = FxComboBox.createComboBox("Select a raw data file.",
        ProjectService.getProject().getCurrentRawDataFiles(), file);

    final ListProperty<OtherDataFile> otherFiles = new SimpleListProperty<>(
        FXCollections.observableArrayList());
    final ObjectProperty<OtherDataFile> otherFile = new SimpleObjectProperty<>();
    final ComboBox<OtherDataFile> otherFileBox = FxComboBox.createComboBox(
        "Select a collection of traces", otherFiles, otherFile);

    final ListProperty<OtherTimeSeriesData> timeSeriesDataList = new SimpleListProperty<>(
        FXCollections.observableArrayList());
    final ObjectProperty<OtherTimeSeriesData> timeSeriesData = new SimpleObjectProperty<>();
    final ComboBox<OtherTimeSeriesData> timeSeriesDataBox = FxComboBox.createComboBox(
        "Select a time series collection.", timeSeriesDataList, timeSeriesData);

    final ObjectProperty<OtherRawOrProcessed> rawOrProcessed = new SimpleObjectProperty<>(
        OtherRawOrProcessed.RAW);
    final ComboBox<OtherRawOrProcessed> rawOrProcessedBox = FxComboBox.createComboBox(
        "Select the time series type", List.of(OtherRawOrProcessed.values()), rawOrProcessed);

    final SortableOtherFeatureComboBox otherFeatureCombo = new SortableOtherFeatureComboBox();
    featureProperty.bind(otherFeatureCombo.selectedFeatureProperty());

    add(FxLabels.newLabel("1. MS data file:"), 0, 0);
    add(rawFileBox, 1, 0);
    add(FxLabels.newLabel("2. Other data:"), 3, 0);
    add(otherFileBox, 4, 0);
    add(FxLabels.newLabel("3. Time series data:"), 0, 1);
    add(timeSeriesDataBox, 1, 1, 2, 1);
    add(rawOrProcessedBox, 4, 1);
    add(FxLabels.newLabel("4. Feature:"), 0, 2);
    add(otherFeatureCombo, 1, 2, 3, 1);

    file.addListener((_, _, f) -> {
      if (f == null) {
        otherFiles.clear();
        return;
      }
      otherFiles.setAll(f.getOtherDataFiles());
    });

    otherFile.addListener((_, _, f) -> {
      if (f == null || !f.hasTimeSeries()) {
        timeSeriesDataList.clear();
        return;
      }
      timeSeriesDataList.setAll(f.getOtherTimeSeries());
    });

    timeSeriesData.addListener((_, _, d) -> {
      if (d == null) {
        otherFeatureCombo.setItems(List.of());
        return;
      }

      if (rawOrProcessed.get() == OtherRawOrProcessed.RAW) {
        otherFeatureCombo.setItems(timeSeriesData.get().getRawTraces());
      } else {
        otherFeatureCombo.setItems(timeSeriesData.get().getProcessedFeatures());
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
