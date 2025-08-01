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

package io.github.mzmine.modules.visualization.image;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters.Pattern;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;

/**
 * Combines the ImagingPlot with a spectrum.
 * <p>
 * Todo: refactor logic to pane
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de), Robin Schmid <a
 * href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ImageVisualizerTab extends MZmineTab {

  private final ImageVisualizerPaneController controller;
  private ParameterSet parameters;
  private ImagingPlot imagingPlot;
  private EChartViewer imageHeatMapPlot;
  private SpectraVisualizerTab spectraTab;
  private ImagingRawDataFile rawDataFile;
  private ParameterSetupPane parameterSetupPane;

  public ImageVisualizerTab(ImageVisualizerParameters parameters) {
    super("Image viewer", false, false);
    this.parameters = MZmineCore.getConfiguration().getModuleParameters(ImageVisualizerModule.class)
        .cloneParameterSet();
    BorderPane mainPane = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("ImageVisualizerPane.fxml")));
    try {
      mainPane = loader.load();
      logger.finest(
          "Root element of Image visualizer tab has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    ImageVisualizerParameters finalParameters = parameters;
    parameterSetupPane = new ParameterSetupPane(true, true, finalParameters) {
      @Override
      protected void callOkButton() {
        this.updateParameterSetFromComponents();
        refreshAllPlots(finalParameters);
      }

      @Override
      protected void parametersChanged() {
        this.updateParameterSetFromComponents();
        refreshAllPlots(finalParameters);
      }
    };
    parameters = (ImageVisualizerParameters) parameterSetupPane.getParameterSet();
    Map<String, Node> parametersAndComponents = parameterSetupPane.getParametersAndComponents();
    for (Entry<String, Node> entry : parametersAndComponents.entrySet()) {
      if (entry.getKey().equals("Raw data files")) {
        entry.getValue().setDisable(true);
      }
    }

    // Get controller
    controller = loader.getController();
    Accordion plotSettingsAccordion = new Accordion(new TitledPane("Settings", parameterSetupPane));
    controller.getPlotPane().setTop(plotSettingsAccordion);
    // add empty image chart
    imagingPlot = new ImagingPlot(parameters);
    controller.getPlotPane().setCenter(imagingPlot);
    imageHeatMapPlot = imagingPlot.getChart();
    addListenerToImage();

    setContent(mainPane);
  }

  public ImageVisualizerTab(ModularFeature feature, ImageVisualizerParameters parameters) {
    this(parameters);

    setData(feature);
  }

  public ImageVisualizerTab(List<ModularFeature> features, ImageVisualizerParameters parameters) {
    this(parameters);

    setData(features);
  }

  public ImageVisualizerTab(ImagingRawDataFile rawDataFile, ImageVisualizerParameters parameters) {
    this(parameters);
    setData(rawDataFile, true);
  }

  private void refreshAllPlots(ImageVisualizerParameters finalParameters) {
    cleanGridPane(controller.getRawDataInfoGridPane());
    cleanGridPane(controller.getImagingParameterInfoGridPane());
    imagingPlot = new ImagingPlot(finalParameters);
    imageHeatMapPlot = imagingPlot.getChart();
    controller.getPlotPane().setCenter(imagingPlot);
    addListenerToImage();
    setData(rawDataFile, true);
    parameters = finalParameters;
  }

  private void cleanGridPane(GridPane gridPane) {
    for (int columnIndex = 0; columnIndex < gridPane.getColumnCount(); columnIndex++) {
      for (Node node : new ArrayList<>(gridPane.getChildren())) {
        if (GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == columnIndex) {
          gridPane.getChildren().remove(node);
        }
      }
    }
  }

  public synchronized void setData(ImagingRawDataFile rawDataFile, boolean createImage) {
    if (spectraTab == null) {
      // spectrum plot
      spectraTab = new SpectraVisualizerTab(rawDataFile);
      spectraTab.setMzTolerance(new MZTolerance(0.005, 10));
      getSpectrumPlot().setShowCursor(true);
      BorderPane pane = controller.getSpectrumPlotPane();
      pane.setCenter(spectraTab.getMainPane());
      // add listener to spectrum property
      getSpectrumPlot().selectedMzRangeProperty().addListener((o, ov, nv) -> {
        imagingPlot.setData(rawDataFile, nv);
        parameterSetupPane.getParameterSet().setParameter(ImageVisualizerParameters.mzRange, nv);
        parameterSetupPane.setParameterValuesToComponents();
      });
    }

    this.rawDataFile = rawDataFile;
    ImagingParameters imagingParameters = rawDataFile.getImagingParam();

    addRawDataInfo(rawDataFile);
    addImagingInfo(imagingParameters);
    if (createImage) {
      imagingPlot.setData(rawDataFile);
    }
  }

  public void setData(ModularFeature feature) {
    setData((ImagingRawDataFile) feature.getRawDataFile(), false);
    imagingPlot.setData(feature);
  }

  public void setData(List<ModularFeature> features) {
    setData((ImagingRawDataFile) features.get(0).getRawDataFile(), false);
    imagingPlot.setData((Feature) features);
  }

  /**
   * @return Main component of the image visualizer in the center
   */
  public ImagingPlot getImagingPlot() {
    return imagingPlot;
  }

  private void addListenerToImage() {
    imageHeatMapPlot.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
      }

      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        XYPlot plot = (XYPlot) imageHeatMapPlot.getChart().getPlot();
        double xValue = plot.getDomainCrosshairValue();
        double yValue = plot.getRangeCrosshairValue();
        if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))) {
          Scan selectedScan = rawDataFile.getScan(xValue, yValue);
          showSpectrum(selectedScan);
        }
      }
    });
  }

  private SpectraPlot getSpectrumPlot() {
    return spectraTab.getSpectrumPlot();
  }

  private void showSpectrum(Scan selectedScan) {
    spectraTab.loadRawData(selectedScan);
  }

  private void addRawDataInfo(ImagingRawDataFile rawDataFile) {
    ImagingRawDataInfo rawDataInfo = new ImagingRawDataInfo(rawDataFile);
    GridPane rawDataInfoPane = controller.getRawDataInfoGridPane();
    Label fileNameLabel = new Label("File name:");
    fileNameLabel.setAlignment(Pos.CENTER_LEFT);
    rawDataInfoPane.add(fileNameLabel, 0, 0);

    Label fileNameValueLabel = new Label(rawDataInfo.name());
    fileNameValueLabel.setAlignment(Pos.CENTER_LEFT);
    rawDataInfoPane.add(fileNameValueLabel, 1, 0);

    Label numberOfScansLabel = new Label("Number of scans:");
    numberOfScansLabel.setAlignment(Pos.CENTER_LEFT);
    rawDataInfoPane.add(numberOfScansLabel, 0, 1);

    Label numberOfScansValueLabel = new Label(rawDataInfo.numberOfScans().toString());
    numberOfScansValueLabel.setAlignment(Pos.CENTER_LEFT);
    rawDataInfoPane.add(numberOfScansValueLabel, 1, 1);

    Label rangeMzLabel = new Label("Range m/z:");
    rangeMzLabel.setAlignment(Pos.CENTER_LEFT);
    rawDataInfoPane.add(rangeMzLabel, 0, 2);

    String mzRangeValue =
        MZminePreferences.mzFormat.getValue().format(rawDataInfo.dataMzRange().lowerEndpoint())
            + "-" + MZminePreferences.mzFormat.getValue()
            .format(rawDataInfo.dataMzRange().upperEndpoint());

    Label rangeMzValueLabel = new Label(mzRangeValue);
    rangeMzValueLabel.setAlignment(Pos.CENTER_LEFT);
    rawDataInfoPane.add(rangeMzValueLabel, 1, 2);
  }

  private void addImagingInfo(ImagingParameters imagingParameters) {
    GridPane imagingParametersInfoPane = controller.getImagingParameterInfoGridPane();

    Label dimensionLabel = new Label("Image dimension [μm]:");
    dimensionLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(dimensionLabel, 0, 0);

    String dimensionValue = "X " + imagingParameters.getLateralWidth() + "  x  Y "
        + imagingParameters.getLateralHeight();

    Label dimensionValueLabel = new Label(dimensionValue);
    dimensionValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(dimensionValueLabel, 1, 0);

    Label totalPixelsLabel = new Label("Total number of pixels:");
    totalPixelsLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(totalPixelsLabel, 0, 1);

    int totalNumberOfPixel =
        imagingParameters.getMaxNumberOfPixelX() * imagingParameters.getMaxNumberOfPixelY();
    Label totalPixelsValueLabel = new Label(Integer.toString(totalNumberOfPixel));
    totalPixelsValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(totalPixelsValueLabel, 1, 1);

    Label spectraPerPixelLabel = new Label("Spectra per pixel:");
    spectraPerPixelLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(spectraPerPixelLabel, 0, 2);

    int spectraPerPixel = imagingParameters.getSpectraPerPixel();
    Label spectraPerPixelValueLabel = new Label(Integer.toString(spectraPerPixel));
    spectraPerPixelValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(spectraPerPixelValueLabel, 1, 2);

    Label imagingPatternLabel = new Label("Imaging pattern:");
    imagingPatternLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(imagingPatternLabel, 0, 3);

    Label imagingPatternValueLabel = new Label(
        Objects.requireNonNullElse(imagingParameters.getPattern(), Pattern.UNKNOWN).getName());
    imagingPatternValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(imagingPatternValueLabel, 1, 3);

    Label scanDirectionLabel = new Label("Scan direction:");
    scanDirectionLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(scanDirectionLabel, 0, 4);

    Label scanDirectionValueLabel = new Label(imagingParameters.getScanDirection().getName());
    scanDirectionValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(scanDirectionValueLabel, 1, 4);

    Label verticalStartLabel = new Label("Vertical start:");
    verticalStartLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(verticalStartLabel, 0, 5);

    Label verticalStartValueLabel = new Label(imagingParameters.getvStart().getName());
    verticalStartValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(verticalStartValueLabel, 1, 5);

    Label horizontalStartLabel = new Label("Horizontal start:");
    horizontalStartLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(horizontalStartLabel, 0, 6);

    Label horizontalStartValueLabel = new Label(imagingParameters.gethStart().getName());
    horizontalStartValueLabel.setAlignment(Pos.CENTER_LEFT);
    imagingParametersInfoPane.add(horizontalStartValueLabel, 1, 6);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of(rawDataFile);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return List.of();
  }
}
