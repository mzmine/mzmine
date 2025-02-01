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

package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFileType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntegrationPane extends BorderPane {

  private final UnitFormat uf = ConfigService.getGuiFormats().unitFormat();
  private final NumberFormats formats = ConfigService.getGuiFormats();
  private ObjectProperty<@Nullable RawDataFile> rawFile = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherDataFile> otherFile = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherFeature> preprocessedTrace = new SimpleObjectProperty<>();
  private final IntegrationPlotController plot = new IntegrationPlotController();
  private final BooleanProperty saveAllowedProperty = new SimpleBooleanProperty(false);

  private IntegrationPane() {
    final ComboBox<RawDataFile> rawFileCombo = FxComboBox.createComboBox(
        "Select an MS raw data file.", ProjectService.getProject().getCurrentRawDataFiles().stream()
            .filter(f -> f.getOtherDataFiles().stream().anyMatch(OtherDataFile::hasTimeSeries))
            .toList(), rawFileProperty());
    final ComboBox<@Nullable OtherDataFile> otherFileCombo = createOtherFileCombo();
    final ComboBox<@Nullable OtherFeature> timeSeriesCombo = createTimeSeriesCombo();
    final Button saveButton = createSaveButton();

    initializeListeners(otherFileCombo, timeSeriesCombo);

    final VBox vbox = FxLayout.newVBox(Pos.TOP_LEFT, rawFileCombo, otherFileCombo, timeSeriesCombo,
        saveButton);
//    rawFileCombo.prefWidthProperty().bind(vbox.widthProperty().subtract(15));
//    otherFileCombo.prefWidthProperty().bind(vbox.widthProperty().subtract(15));
//    timeSeriesCombo.prefWidthProperty().bind(vbox.widthProperty().subtract(15));

    vbox.setMinWidth(250);
    vbox.setMaxWidth(250);

    final Region plotView = plot.buildView();
    setCenter(plotView);
    setRight(vbox);

    maxHeight(400);
    minHeight(200);
  }

  private @NotNull Button createSaveButton() {
    final Button saveButton = FxButtons.createSaveButton("Save", () -> {
      if (!saveAllowedProperty.get()) {
        return;
      }
      saveAllowedProperty.set(false);
      if (preprocessedTrace.get() != null) {
        var data = preprocessedTrace.get().getFeatureData().getTimeSeriesData();
        data.replaceProcessedFeaturesForTrace(
            preprocessedTrace.get().get(RawTraceType.class) != null ? preprocessedTrace.get()
                .get(RawTraceType.class) : preprocessedTrace.get(),
            plot.getIntegratedFeatures().stream().filter(ts -> ts instanceof OtherTimeSeries)
                .map(ts -> {
                  final OtherFeature integrated = preprocessedTrace.get()
                      .createSubFeature((OtherTimeSeries) ts);
                  return integrated;
                }).toList());
      }
    });
    saveButton.disableProperty().bind(saveAllowedProperty.not());
    plot.integratedFeaturesProperty()
        .addListener((_, _, _) -> saveAllowedProperty.set(isPlotFeaturesMatchSavedFeatures()));
    return saveButton;
  }

  private boolean isPlotFeaturesMatchSavedFeatures() {
    final OtherFeature series = preprocessedTrace.get();
    if (series == null) {
      return false;
    }

    final OtherTimeSeriesData data = series.getFeatureData().getTimeSeriesData();
    final List<OtherTimeSeries> processed = data.getProcessedFeaturesForTrace(series).stream()
        .map(OtherFeature::getFeatureData).toList();
    if (processed.equals(plot.getIntegratedFeatures())) {
      return false;
    }
    return true;
  }

  public IntegrationPane(RawDataFile file) {
    this();
    setRawFile(file);
  }

  public IntegrationPane(OtherDataFile otherFile) {
    this();
    setOtherFile(otherFile);
  }

  public IntegrationPane(@NotNull OtherFeature preprocessedTrace) {
    this();
    setPreprocessedTrace(preprocessedTrace);
  }

  private @NotNull ComboBox<@Nullable OtherDataFile> createOtherFileCombo() {
    final ComboBox<@Nullable OtherDataFile> otherFileCombo = FxComboBox.createComboBox(
        "Select a detector.", List.of(), otherFile);
    otherFileCombo.setConverter(new StringConverter<OtherDataFile>() {
      @Override
      public String toString(OtherDataFile object) {
        if (object == null) {
          return "";
        }
        return object.getDescription();
      }

      @Override
      public OtherDataFile fromString(String string) {
        return null;
      }
    });
    return otherFileCombo;
  }

  private @NotNull ComboBox<@Nullable OtherFeature> createTimeSeriesCombo() {
    final ComboBox<@Nullable OtherFeature> timeSeriesCombo = FxComboBox.createComboBox(
        "Select a chromatogram.", List.of(), preprocessedTrace);
    timeSeriesCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(OtherFeature object) {
        if (object == null) {
          return "";
        }
        return object.getFeatureData().getName();
      }

      @Override
      public OtherFeature fromString(String string) {
        return null;
      }
    });
    return timeSeriesCombo;
  }

  private void initializeListeners(ComboBox<@Nullable OtherDataFile> otherFileCombo,
      ComboBox<@Nullable OtherFeature> timeSeriesCombo) {
    rawFile.addListener((_, _, file) -> {
      if (file != null) {
        otherFileCombo.setItems(FXCollections.observableList(
            file.getOtherDataFiles().stream().filter(OtherDataFile::hasTimeSeries).toList()));
        if (!otherFileCombo.getItems().isEmpty()) {
          otherFileCombo.getSelectionModel().selectFirst();
        }
      } else {
        otherFileCombo.getSelectionModel().clearSelection();
        otherFileCombo.setItems(FXCollections.emptyObservableList());
      }
    });

    otherFile.addListener((_, _, otherFile) -> {
      if (otherFile != null) {
        timeSeriesCombo.setItems(FXCollections.observableList(
            otherFile.getOtherTimeSeriesData().getPreprocessedTraces()));
        if (!timeSeriesCombo.getItems().isEmpty()) {
          timeSeriesCombo.getSelectionModel().selectFirst();
        }
      } else {
        timeSeriesCombo.getSelectionModel().clearSelection();
        timeSeriesCombo.setItems(FXCollections.emptyObservableList());
      }
    });

    otherFile.addListener((_, _, newOtherFile) -> {
      if (newOtherFile != null && newOtherFile.getCorrespondingRawDataFile() != rawFile.get()) {
        setRawFile(newOtherFile.getCorrespondingRawDataFile());
      }
    });

    preprocessedTrace.addListener((_, _, timeSeries) -> {
      if (timeSeries != null && timeSeries.get(OtherFileType.class) != otherFile.get()) {
        setOtherFile(timeSeries.get(OtherFileType.class));
      }
    });

    preprocessedTrace.addListener((_, _, ts) -> {
      plot.setIntegratedFeatures(List.of());
      if (ts != null) {
        plot.setOtherTimeSeries(ts.getFeatureData());
        plot.setIntegratedFeatures(
            ts.getFeatureData().getTimeSeriesData().getProcessedFeaturesForTrace(ts).stream()
                .map(OtherFeature::getFeatureData).toList());
      }
      saveAllowedProperty.set(isPlotFeaturesMatchSavedFeatures());
    });
  }

  public @Nullable RawDataFile getRawFile() {
    return rawFile.get();
  }

  public void setRawFile(@Nullable RawDataFile rawFile) {
    if (!rawFile.getOtherDataFiles().stream().anyMatch(OtherDataFile::hasTimeSeries)) {
      throw new RuntimeException(
          "Selected file does not have any associated other detector time series.");
    }
    this.rawFile.set(rawFile);
  }

  public ObjectProperty<@Nullable RawDataFile> rawFileProperty() {
    return rawFile;
  }

  public @Nullable OtherDataFile getOtherFile() {
    return otherFile.get();
  }

  public void setOtherFile(@Nullable OtherDataFile otherFile) {
    this.otherFile.set(otherFile);
  }

  public ObjectProperty<@Nullable OtherDataFile> otherFileProperty() {
    return otherFile;
  }

  public @Nullable OtherFeature getPreprocessedTrace() {
    return preprocessedTrace.get();
  }

  public void setPreprocessedTrace(@Nullable OtherFeature preprocessedTrace) {
    this.preprocessedTrace.set(preprocessedTrace);
  }

  public ObjectProperty<@Nullable OtherFeature> preprocessedTraceProperty() {
    return preprocessedTrace;
  }
}
