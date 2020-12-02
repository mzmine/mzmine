package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.SimpleXYLineChart;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.DomainValueProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.project.impl.StorableFrame;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class MobilogramVisualizerController {

  private static final Logger logger =
      Logger.getLogger(MobilogramVisualizerController.class.getName());

  @FXML
  public SimpleXYLineChart<Mobilogram> mobilogramChart;

  @FXML
  public ComboBox<RawDataFile> rawDataFileSelector;

  @FXML
  public ComboBox<Frame> frameSelector;

  @FXML
  public ComboBox<Mobilogram> mobilogramSelector;

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
  }

  public void onRawDataFileSelectionChanged(ActionEvent actionEvent) {
    RawDataFile selectedFile = rawDataFileSelector.getValue();
    frameSelector.getItems().clear();
    if (!(selectedFile instanceof IMSRawDataFile)) {
      return;
    }

    UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    MobilityType type = ((IMSRawDataFile) selectedFile).getMobilityType();
    String xLabel = unitFormat.format(type.getAxisLabel(), type.getUnit());
    String yLabel = unitFormat.format("Intensity", "cps");
    mobilogramChart.setDomainAxisLabel(xLabel);
    mobilogramChart.setRangeAxisLabel(yLabel);
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
    Mobilogram selectedMobilogram = mobilogramSelector.getValue();
    mobilogramChart.removeAllDatasets();
    if (selectedMobilogram != null) {
      mobilogramChart.addDataset(selectedMobilogram);
    }
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
                  setText(item.getFrameId() + " @" + rtFormat.format(item.getRetentionTime()) +
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
        return item.getFrameId() + " @" + rtFormat.format(item.getRetentionTime()) +
            " min (" + item.getMobilograms().size() + ")";
      }

      @Override
      public Frame fromString(String string) {
        return null;
      }
    });
    frameSelector.setCellFactory(listViewListCellCallback);
  }
}
