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

package io.github.mzmine.modules.visualization.otherdetectors.multidetector;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.OtherTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DetectorPane extends BorderPane {

  private final UnitFormat uf = ConfigService.getGuiFormats().unitFormat();
  private final NumberFormats formats = ConfigService.getGuiFormats();
  private ObjectProperty<@Nullable RawDataFile> rawFile = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherDataFile> otherFile = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherTimeSeries> timeSeries = new SimpleObjectProperty<>();
  private ChromatogramPlotController plot;

  private DetectorPane() {
    final ComboBox<RawDataFile> rawFileCombo = FxComboBox.createComboBox(
        "Select an MS raw data file.", ProjectService.getProject().getCurrentRawDataFiles().stream()
            .filter(f -> f.getOtherDataFiles().stream().anyMatch(OtherDataFile::hasTimeSeries))
            .toList(), rawFileProperty());
    final ComboBox<@Nullable OtherDataFile> otherFileCombo = createOtherFileCombo();

    final ComboBox<@Nullable OtherTimeSeries> timeSeriesCombo = createTimeSeriesCombo();

    plot = new ChromatogramPlotController();

    initializeListeners(otherFileCombo, timeSeriesCombo);

    final VBox vbox = FxLayout.newVBox(rawFileCombo, otherFileCombo, timeSeriesCombo);
//    rawFileCombo.prefWidthProperty().bind(vbox.widthProperty().subtract(15));
//    otherFileCombo.prefWidthProperty().bind(vbox.widthProperty().subtract(15));
//    timeSeriesCombo.prefWidthProperty().bind(vbox.widthProperty().subtract(15));

    vbox.minWidth(250);
    vbox.maxWidth(250);

    final Region plotView = plot.buildView();
    setCenter(plotView);
    setRight(vbox);

    maxHeight(400);
    minHeight(200);
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

  private @NotNull ComboBox<@Nullable OtherTimeSeries> createTimeSeriesCombo() {
    final ComboBox<@Nullable OtherTimeSeries> timeSeriesCombo = FxComboBox.createComboBox(
        "Select a chromatogram.", List.of(), timeSeries);
    timeSeriesCombo.setConverter(new StringConverter<>() {
      @Override
      public String toString(OtherTimeSeries object) {
        if (object == null) {
          return "";
        }
        return object.getName();
      }

      @Override
      public OtherTimeSeries fromString(String string) {
        return null;
      }
    });
    return timeSeriesCombo;
  }

  public DetectorPane(RawDataFile file) {
    this();
    setRawFile(file);
  }

  public DetectorPane(OtherDataFile otherFile) {
    this();
    setOtherFile(otherFile);
  }

  public DetectorPane(@NotNull OtherTimeSeries timeSeries) {
    this();
    setTimeSeries(timeSeries);
  }

  private void initializeListeners(ComboBox<@Nullable OtherDataFile> otherFileCombo,
      ComboBox<@Nullable OtherTimeSeries> timeSeriesCombo) {
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
        timeSeriesCombo.setItems(
            FXCollections.observableList(otherFile.getOtherTimeSeries().getTimeSeries()));
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

    timeSeries.addListener((_, _, timeSeries) -> {
      if (timeSeries != null && timeSeries.getOtherDataFile() != otherFile.get()) {
        setOtherFile(timeSeries.getOtherDataFile());
      }
    });

    timeSeries.addListener((_, _, ts) -> {
      if (ts == null) {
        plot.clearDatasets();
        return;
      }
      plot.setDataset(new ColoredXYDataset(new OtherTimeSeriesToXYProvider(ts)),
          new ColoredXYLineRenderer());
      plot.setDomainAxisLabel(
          uf.format(ts.getOtherDataFile().getOtherTimeSeries().getTimeSeriesDomainLabel(),
              ts.getOtherDataFile().getOtherTimeSeries().getTimeSeriesDomainUnit()));
      plot.setDomainAxisFormat(formats.rtFormat());

      plot.setRangeAxisLabel(
          uf.format(ts.getOtherDataFile().getOtherTimeSeries().getTimeSeriesRangeLabel(),
              ts.getOtherDataFile().getOtherTimeSeries().getTimeSeriesRangeUnit()));
      plot.setRangeAxisFormat(formats.intensityFormat());
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

  public @Nullable OtherTimeSeries getTimeSeries() {
    return timeSeries.get();
  }

  public void setTimeSeries(@Nullable OtherTimeSeries timeSeries) {
    this.timeSeries.set(timeSeries);
  }

  public ObjectProperty<@Nullable OtherTimeSeries> timeSeriesProperty() {
    return timeSeries;
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return plot.cursorPositionProperty();
  }
}
