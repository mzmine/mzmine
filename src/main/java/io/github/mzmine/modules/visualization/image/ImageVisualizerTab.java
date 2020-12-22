package io.github.mzmine.modules.visualization.image;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public class ImageVisualizerTab extends MZmineTab {
  private final ImageVisualizerPaneController controller;

  public ImageVisualizerTab(ParameterSet parameters, EChartViewer chartViewer,
      ImagingRawDataFile rawDataFile, ImagingParameters imagingParameters) {
    super("Image viewer", true, false);

    AnchorPane root = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("ImageVisualizerPane.fxml")));
    try {
      root = loader.load();
      logger.finest(
          "Root element of Image visualizer tab has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Get controller
    controller = loader.getController();
    controller.initialize(parameters);
    BorderPane plotPane = controller.getPlotPane();
    plotPane.setCenter(chartViewer);
    addRawDataInfo(rawDataFile);
    addImagingInfo(imagingParameters);
    setContent(root);
  }

  private void addRawDataInfo(ImagingRawDataFile rawDataFile) {
    ImagingRawDataInfo rawDataInfo = new ImagingRawDataInfo(rawDataFile);
    GridPane rawDataInfoPane = controller.getRawDataInfoGridPane();
    rawDataInfoPane.add(new Label("File name:"), 0, 0);
    rawDataInfoPane.add(new Label(rawDataInfo.getName()), 1, 0);
    rawDataInfoPane.add(new Label("Number of scans:"), 0, 1);
    rawDataInfoPane.add(new Label(rawDataInfo.getNumberOfScans().toString()), 1, 1);
    rawDataInfoPane.add(new Label("Range m/z:"), 0, 2);
    rawDataInfoPane.add(new Label(
        MZminePreferences.mzFormat.getValue().format(rawDataInfo.getDataMzRange().lowerEndpoint())
            + "-" + MZminePreferences.mzFormat.getValue()
                .format(rawDataInfo.getDataMzRange().upperEndpoint())),
        1, 2);
  }

  private void addImagingInfo(ImagingParameters imagingParameters) {
    GridPane imagingParametersInfoPane = controller.getImagingParameterInfoGridPane();
    imagingParametersInfoPane.add(new Label("Image dimension [\u00B5m]:"), 0, 0);
    imagingParametersInfoPane.add(new Label("X " + imagingParameters.getLateralWidth() + "  x  Y "
        + imagingParameters.getLateralHeight()), 1, 0);
    imagingParametersInfoPane.add(new Label("Total number of pixels:"), 0, 1);
    Integer totalNumberOfPixel =
        imagingParameters.getMaxNumberOfPixelX() * imagingParameters.getMaxNumberOfPixelY();
    imagingParametersInfoPane.add(new Label(totalNumberOfPixel.toString()), 1, 1);
    imagingParametersInfoPane.add(new Label("Spectra per pixel:"), 0, 2);
    Integer spectraPerPixel = imagingParameters.getSpectraPerPixel();
    imagingParametersInfoPane.add(new Label(spectraPerPixel.toString()), 1, 2);
    imagingParametersInfoPane.add(new Label("Imaging patter"), 0, 3);
    imagingParametersInfoPane.add(new Label(imagingParameters.getPattern().name()), 1, 3);
    imagingParametersInfoPane.add(new Label("Scan direction"), 0, 4);
    imagingParametersInfoPane.add(new Label(imagingParameters.getScanDirection().name()), 1, 4);
    imagingParametersInfoPane.add(new Label("Scan direction"), 0, 4);
    imagingParametersInfoPane.add(new Label(imagingParameters.getScanDirection().name()), 1, 4);
    imagingParametersInfoPane.add(new Label("Vertical start"), 0, 5);
    imagingParametersInfoPane.add(new Label(imagingParameters.getvStart().name()), 1, 5);
    imagingParametersInfoPane.add(new Label("Horizontal start"), 0, 6);
    imagingParametersInfoPane.add(new Label(imagingParameters.gethStart().name()), 1, 6);
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featurelists) {

  }

  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    // TODO Auto-generated method stub
    return null;
  }
}
