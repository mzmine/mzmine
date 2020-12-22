/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYLineChart;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing.MobilogramChangeListener;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing.PreviewMobilogram;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.project.impl.StorableFrame;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javax.annotation.Nullable;

public class MobilogramVisualizerController {

  private static final Logger logger =
      Logger.getLogger(MobilogramVisualizerController.class.getName());
  @FXML
  public BorderPane borderPane;
  private List<MobilogramChangeListener> mobilogramListeners;
  @FXML
  private SimpleXYLineChart<PreviewMobilogram> mobilogramChart;

  @FXML
  private ComboBox<RawDataFile> rawDataFileSelector;

  @FXML
  private ComboBox<Frame> frameSelector;

  @FXML
  private ComboBox<Mobilogram> mobilogramSelector;

  private ObservableList<RawDataFile> rawDataFiles;
  private ObservableList<Frame> frames;
  private ObservableList<Mobilogram> mobilograms;

  private NumberFormat rtFormat;

  @FXML
  public void initialize() {

    rawDataFiles = FXCollections.observableArrayList(new ArrayList<>());
    frames = FXCollections.observableArrayList(new ArrayList<>());
    mobilograms = FXCollections.observableArrayList(new ArrayList<>());

    rawDataFileSelector.setItems(rawDataFiles);
    frameSelector.setItems(frames);
    mobilogramSelector.setItems(mobilograms);

    rtFormat = MZmineCore.getConfiguration().getRTFormat();

    mobilogramListeners = new ArrayList<>();
    initMobilgramBox();
    initFrameBox();

    MenuItem showSpectrum = new MenuItem("Show spectrum");
    showSpectrum.setOnAction(e -> {
      int valueIndex = mobilogramChart.getCursorPosition().getValueIndex();
      if (valueIndex != -1) {
        MobilityDataPoint selectedDp =
            mobilogramSelector.getValue().getDataPoints().get(valueIndex);
        SpectraVisualizerModule.addNewSpectrumTab(frameSelector.getValue().getDataFile(),
            selectedDp.getScanNum());
      }
    });
    mobilogramChart.getContextMenu().getItems().add(showSpectrum);
    mobilogramChart.setDomainAxisLabel("Ion mobility");
    UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    String yLabel = unitFormat.format("Intensity", "cps");
    mobilogramChart.setRangeAxisLabel(yLabel);
  }

  public void onRawDataFileSelectionChanged(ActionEvent actionEvent) {
    RawDataFile selectedFile = rawDataFileSelector.getValue();
    frameSelector.getItems().clear();
    if (!(selectedFile instanceof IMSRawDataFile)) {
      return;
    }

    UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    MobilityType type = ((IMSRawDataFile) selectedFile).getMobilityType();
    String xLabel = "Ion mobility " + unitFormat.format("(" + type.getAxisLabel() + ")",
        type.getUnit());
    mobilogramChart.setDomainAxisLabel(xLabel);
    frameSelector.getItems().addAll(((IMSRawDataFile) selectedFile).getFrames());
  }

  public void onFrameSelectionChanged(ActionEvent actionEvent) {
    Frame selectedFrame = frameSelector.getValue();
    mobilogramSelector.getItems().clear();
    if (selectedFrame instanceof StorableFrame) { // simple frame cannot have mobilograms
      mobilogramSelector.getItems().addAll(selectedFrame.getMobilograms());
    }
  }

  public void onMobilogramSelectionChanged(ActionEvent actionEvent) {
    final Mobilogram selectedMobilogram = mobilogramSelector.getValue();
    mobilogramChart.removeAllDatasets();
    if (selectedMobilogram != null) {
      mobilogramChart.addDataset(
          new PreviewMobilogram(selectedMobilogram, selectedMobilogram.representativeString()));
    }

    mobilogramListeners.forEach(l -> l.change(selectedMobilogram));
  }

  public ObservableList<RawDataFile> getRawDataFiles() {
    return rawDataFiles;
  }

  public void setRawDataFiles(Collection<RawDataFile> rawDataFiles) {
    rawDataFileSelector.getItems().clear();
    rawDataFileSelector.getItems().addAll(rawDataFiles);
    if (rawDataFiles.size() > 0) {
      rawDataFileSelector.getSelectionModel().select(0);
      onRawDataFileSelectionChanged(null);
    }
  }

  private void initMobilgramBox() {
    Callback<ListView<Mobilogram>, ListCell<Mobilogram>> listViewListCellCallback =
        new Callback<>() {
          @Override
          public ListCell<Mobilogram> call(ListView<Mobilogram> param) {
            return new ListCell<Mobilogram>() {
              @Override
              protected void updateItem(Mobilogram item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                  setGraphic(null);
                } else {
                  setText(item.representativeString());
                }
              }
            };
          }
        };
    mobilogramSelector.setConverter(new StringConverter<Mobilogram>() {
      @Override
      public String toString(Mobilogram object) {
        if (object != null) {
          return object.representativeString();
        }
        return "";
      }

      @Override
      public Mobilogram fromString(String string) {
        return null;
      }
    });
    mobilogramSelector.setCellFactory(listViewListCellCallback);
  }

  private void initFrameBox() {
    Callback<ListView<Frame>, ListCell<Frame>> listViewListCellCallback =
        new Callback<>() {
          @Override
          public ListCell<Frame> call(ListView<Frame> param) {
            return new ListCell<Frame>() {
              @Override
              protected void updateItem(Frame item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                  setGraphic(null);
                } else {
                  setText(item.getFrameId() + " MS" + item.getMSLevel() + " @" +
                      rtFormat.format(item.getRetentionTime()) +
                      " min (" + item.getMobilograms().size() + ")");
                }
              }
            };
          }
        };

    frameSelector.setConverter(new StringConverter<Frame>() {
      @Override
      public String toString(Frame item) {
        if (item == null) {
          return "";
        }
        return item.getFrameId() + " MS" + item.getMSLevel() + " @" + rtFormat
            .format(item.getRetentionTime()) +
            " min (" + item.getMobilograms().size() + ")";
      }

      @Override
      public Frame fromString(String string) {
        return null;
      }
    });
    frameSelector.setCellFactory(listViewListCellCallback);
  }

  @Nullable
  public Mobilogram getSelectedMobilogram() {
    return mobilogramSelector.getValue();
  }

  @Nullable
  public Frame getSelectedFrame() {
    return frameSelector.getValue();
  }

  @Nullable
  public RawDataFile getSelectedRawDataFile() {
    return rawDataFileSelector.getValue();
  }

  public SimpleXYLineChart<PreviewMobilogram> getMobilogramChart() {
    return mobilogramChart;
  }

  public void addMobilogramSelectionListener(MobilogramChangeListener listener) {
    mobilogramListeners.add(listener);
  }
}
