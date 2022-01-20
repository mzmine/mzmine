/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.image;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.parameters.ParameterSet;
import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageVisualizerTab extends MZmineTab {

  private final ImageVisualizerPaneController controller;
  private final EChartViewer imageHeatMapPlot;
  private final ImagingRawDataFile rawDataFile;

  public ImageVisualizerTab(ModularFeature feature){
    super("Image viewer", true, false);
    FeatureImageProvider prov = new FeatureImageProvider(feature);
    ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.THIS_THREAD);

    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");
    ImagingRawDataFile imagingFile = (ImagingRawDataFile) feature.getRawDataFile();
    ImagingParameters imagingParameters = imagingFile.getImagingParam();
    final boolean hideAxes = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class).getParameter(
            FeatureTableFXParameters.hideImageAxes).getValue();

    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    chart.setDataset(ds);
    axis.setInverted(true);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralHeight()));
    axis.setVisible(!hideAxes);

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralWidth()));
    axis.setVisible(!hideAxes);

    final boolean lockOnAspectRatio = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class).getParameter(
            FeatureTableFXParameters.lockImagesToAspectRatio).getValue();
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);
    this.imageHeatMapPlot = chart;
    this.rawDataFile = imagingFile;
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
    updateHeatMapPlot();
    addRawDataInfo(rawDataFile);
    addImagingInfo(imagingParameters);
    setContent(root);
  }
  public ImageVisualizerTab(EChartViewer imageHeatMapPlot,
      ImagingRawDataFile rawDataFile, ImagingParameters imagingParameters) {
    super("Image viewer", true, false);

    this.imageHeatMapPlot = imageHeatMapPlot;
    this.rawDataFile = rawDataFile;
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
    updateHeatMapPlot();
    addRawDataInfo(rawDataFile);
    addImagingInfo(imagingParameters);
    setContent(root);
  }


  public void updateHeatMapPlot() {
    BorderPane plotPane = controller.getPlotPane();
    plotPane.setCenter(imageHeatMapPlot);
    addListenerToImage(imageHeatMapPlot, rawDataFile);
  }

  private void addListenerToImage(EChartViewer imageHeatMapPlot, ImagingRawDataFile rawDataFile) {
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
          AnchorPane pane = controller.getSpectrumPlotPane();
          Node spectrum = addSpectra(rawDataFile, selectedScan);
          AnchorPane.setTopAnchor(spectrum, 0.0);
          AnchorPane.setRightAnchor(spectrum, 0.0);
          AnchorPane.setLeftAnchor(spectrum, 0.0);
          AnchorPane.setBottomAnchor(spectrum, 0.0);
          pane.getChildren().add(spectrum);
        }
      }
    });
  }

  private Node addSpectra(ImagingRawDataFile rawDataFile, Scan selectedScan) {
    SpectraVisualizerTab spectraTab = new SpectraVisualizerTab(rawDataFile);
    spectraTab.loadRawData(selectedScan);
    return spectraTab.getContent();
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
    return Collections.emptyList();
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
    // TODO Auto-generated method stub
    return null;
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    // TODO Auto-generated method stub
    return null;
  }
}
