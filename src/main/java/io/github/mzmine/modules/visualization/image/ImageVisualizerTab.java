/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;

/**
 * Combines the ImagingPlot with a spectrum
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de), Robin Schmid <a
 * href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ImageVisualizerTab extends MZmineTab {

  private final ParameterSet parameters;
  private final ImageVisualizerPaneController controller;
  private final ImagingPlot imagingPlot;
  private final EChartViewer imageHeatMapPlot;
  private SpectraVisualizerTab spectraTab;
  private ImagingRawDataFile rawDataFile;

  public ImageVisualizerTab(ParameterSet parameters) {
    super("Image viewer", false, false);
    this.parameters = parameters;
    BorderPane mainPane = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("ImageVisualizerPane.fxml")));
    try {
      mainPane = loader.load();
      logger.finest(
          "Root element of Image visualizer tab has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    // Get controller
    controller = loader.getController();

    // add empty image chart
    imagingPlot = new ImagingPlot(parameters);
    controller.getPlotPane().setCenter(imagingPlot);
    imageHeatMapPlot = imagingPlot.getChart();
    MZmineCore.getConfiguration().getDefaultChartTheme().apply(imageHeatMapPlot);
    addListenerToImage();

    setContent(mainPane);
  }

  public ImageVisualizerTab(ModularFeature feature, ParameterSet parameters) {
    this(parameters);

    setData(feature);
  }

  public ImageVisualizerTab(ImagingRawDataFile rawDataFile, ParameterSet parameters) {
    this(parameters);

    setData(rawDataFile, true);
  }

  public synchronized void setData(ImagingRawDataFile rawDataFile, boolean createImage) {
    if (spectraTab == null) {
      // spectrum plot
      spectraTab = new SpectraVisualizerTab(rawDataFile);
      getSpectrumPlot().setShowCursor(true);
      BorderPane pane = controller.getSpectrumPlotPane();
      pane.setCenter(spectraTab.getMainPane());
      // add listener to spectrum property
      getSpectrumPlot().selectedMzRangeProperty()
          .addListener((o, ov, nv) -> imagingPlot.setData(rawDataFile, nv));
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
    rawDataInfoPane.add(new Label("File name:"), 0, 0);
    rawDataInfoPane.add(new Label(rawDataInfo.name()), 1, 0);
    rawDataInfoPane.add(new Label("Number of scans:"), 0, 1);
    rawDataInfoPane.add(new Label(rawDataInfo.numberOfScans().toString()), 1, 1);
    rawDataInfoPane.add(new Label("Range m/z:"), 0, 2);
    rawDataInfoPane.add(new Label(
        MZminePreferences.mzFormat.getValue().format(rawDataInfo.dataMzRange().lowerEndpoint())
            + "-" + MZminePreferences.mzFormat.getValue()
            .format(rawDataInfo.dataMzRange().upperEndpoint())), 1, 2);
  }

  private void addImagingInfo(ImagingParameters imagingParameters) {
    GridPane imagingParametersInfoPane = controller.getImagingParameterInfoGridPane();
    imagingParametersInfoPane.add(new Label("Image dimension [μm]:"), 0, 0);
    imagingParametersInfoPane.add(new Label("X " + imagingParameters.getLateralWidth() + "  x  Y "
        + imagingParameters.getLateralHeight()), 1, 0);
    imagingParametersInfoPane.add(new Label("Total number of pixels:"), 0, 1);
    int totalNumberOfPixel =
        imagingParameters.getMaxNumberOfPixelX() * imagingParameters.getMaxNumberOfPixelY();
    imagingParametersInfoPane.add(new Label(Integer.toString(totalNumberOfPixel)), 1, 1);
    imagingParametersInfoPane.add(new Label("Spectra per pixel:"), 0, 2);
    int spectraPerPixel = imagingParameters.getSpectraPerPixel();
    imagingParametersInfoPane.add(new Label(Integer.toString(spectraPerPixel)), 1, 2);
    imagingParametersInfoPane.add(new Label("Imaging patter:"), 0, 3);
    imagingParametersInfoPane.add(new Label(imagingParameters.getPattern().toString()), 1, 3);
    imagingParametersInfoPane.add(new Label("Scan direction:"), 0, 4);
    imagingParametersInfoPane.add(new Label(imagingParameters.getScanDirection().toString()), 1, 4);
    imagingParametersInfoPane.add(new Label("Vertical start:"), 0, 5);
    imagingParametersInfoPane.add(new Label(imagingParameters.getvStart().toString()), 1, 5);
    imagingParametersInfoPane.add(new Label("Horizontal start:"), 0, 6);
    imagingParametersInfoPane.add(new Label(imagingParameters.gethStart().toString()), 1, 6);
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
