package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.SimpleXYLineChart;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.project.impl.StorableFrame;
import java.util.ArrayList;
import java.util.Collection;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class MobilogramVisualizerController {

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


  @FXML
  public void initialize() {
    rawDataFiles = FXCollections.observableArrayList(new ArrayList<>());
    frames = FXCollections.observableArrayList(new ArrayList<>());
    mobilograms = FXCollections.observableArrayList(new ArrayList<>());

    rawDataFileSelector.setItems(rawDataFiles);
    frameSelector.setItems(frames);
    mobilogramSelector.setItems(mobilograms);
  }

  public void setRawDataFiles(Collection<RawDataFile> rawDataFiles) {
    rawDataFileSelector.getItems().clear();
    rawDataFileSelector.getItems().addAll(rawDataFiles);
  }

  public void onRawDataFileSelectionChanged(ActionEvent actionEvent) {
    RawDataFile selectedFile = rawDataFileSelector.getValue();
    frameSelector.getItems().clear();
    if(!(selectedFile instanceof IMSRawDataFile)) {
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
    if(selectedFrame instanceof StorableFrame) { // simple frame cannot have mobilograms
      mobilogramSelector.getItems().addAll(selectedFrame.getMobilograms());
    }
  }

  public void onMobilogramSelectionChanged(ActionEvent actionEvent) {
    Mobilogram selectedMobilogram = mobilogramSelector.getValue();
    if(selectedMobilogram != null) {
      mobilogramChart.addDataset(selectedMobilogram);
    }
  }

  public ObservableList<RawDataFile> getRawDataFiles() {
    return rawDataFiles;
  }
}
