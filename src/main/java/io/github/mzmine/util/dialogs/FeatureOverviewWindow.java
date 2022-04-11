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

package io.github.mzmine.util.dialogs;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.javafx.WindowsMenu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * Window to show an overview of a feature displayed in a plot
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FeatureOverviewWindow extends Stage {

  private final Scene mainScene;
  private final BorderPane mainPane;

  private Feature feature;
  private RawDataFile[] rawFiles;

  public FeatureOverviewWindow(FeatureListRow row) {

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    this.feature = row.getBestFeature();
    rawFiles = row.getRawDataFiles().toArray(new RawDataFile[0]);

    // setBackground(Color.white);
    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    SplitPane splitPaneCenter = new SplitPane();
    splitPaneCenter.setOrientation(Orientation.HORIZONTAL);
    mainPane.setCenter(splitPaneCenter);

    // split pane left for plots
    SplitPane splitPaneLeftPlot = new SplitPane();
    splitPaneLeftPlot.setOrientation(Orientation.VERTICAL);

    splitPaneCenter.getItems().add(splitPaneLeftPlot);

    // add Tic plots
    splitPaneLeftPlot.getItems().add(addTicPlot(row));

    // add feature data summary
    splitPaneLeftPlot.getItems().add(addFeatureDataSummary(row));

    // split pane right
    SplitPane splitPaneRight = new SplitPane();
    splitPaneRight.setOrientation(Orientation.VERTICAL);
    splitPaneCenter.getItems().add(splitPaneRight);

    // add spectra MS1
    splitPaneRight.getItems().add(addSpectraMS1());

    // add Spectra MS2
    if (feature.getMostIntenseFragmentScan() != null) {
      splitPaneRight.getItems().add(addSpectraMS2());
    } else {
      FlowPane noMSMSPanel = new FlowPane();
      Label noMSMSScansFound = new Label("Sorry, no MS/MS scans found!");
      // noMSMSScansFound.setFont(new Font("Dialog", Font.BOLD, 16));
      // noMSMSScansFound.setForeground(Color.RED);
      noMSMSPanel.getChildren().add(noMSMSScansFound);
      splitPaneRight.getItems().add(noMSMSPanel);
    }

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);
    this.show();

  }

  private SplitPane addTicPlot(FeatureListRow row) {
    SplitPane pane = new SplitPane();
    pane.setOrientation(Orientation.HORIZONTAL);
    // labels for TIC visualizer
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(rawFiles[0].getDataRTRange(1), 1);

    // mz range
    Range<Double> mzRange = null;
    mzRange = feature.getRawDataPointsMZRange();
    // optimize output by extending the range
    double upper = mzRange.upperEndpoint();
    double lower = mzRange.lowerEndpoint();
    double fiveppm = (upper * 5E-6);
    mzRange = Range.closed(lower - fiveppm, upper + fiveppm);

    // labels
    labelsMap.put(feature, feature.toString());

    List<ModularFeature> featureSelection = row.getFeatures();

    TICVisualizerTab window = new TICVisualizerTab(rawFiles, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        featureSelection, // selected features
        labelsMap); // labels

    pane.getItems().add(window.getContent());
    return pane;
  }

  private FlowPane addFeatureDataSummary(FeatureListRow row) {
    var featureDataNode = new FlowPane(Orientation.VERTICAL);
    // featureDataSummary.setBackground(Color.WHITE);
    var featureDataSummary = featureDataNode.getChildren();
    featureDataSummary.add(new Label("Feature: " + row.getID()));
    if (row.getPreferredFeatureIdentity() != null)
      featureDataSummary.add(new Label("Identity: " + row.getPreferredFeatureIdentity().getName()));
    if (row.getComment() != null)
      featureDataSummary.add(new Label("Comment: " + row.getComment()));
    featureDataSummary.add(new Label("Raw File: " + rawFiles[0].getName()));
    featureDataSummary.add(new Label("Intensity: "
        + MZmineCore.getConfiguration().getIntensityFormat().format(feature.getHeight())));
    featureDataSummary.add(new Label(
        "Area: " + MZmineCore.getConfiguration().getIntensityFormat().format(feature.getArea())));
    featureDataSummary.add(new Label("Charge: " + feature.getCharge()));
    featureDataSummary.add(
        new Label("m/z: " + MZmineCore.getConfiguration().getMZFormat().format(feature.getMZ())));
    featureDataSummary.add(new Label(
        "Retention time: " + MZmineCore.getConfiguration().getRTFormat().format(feature.getRT())));
    featureDataSummary.add(new Label("Asymmetry factor "
        + MZmineCore.getConfiguration().getRTFormat().format(feature.getAsymmetryFactor())));
    featureDataSummary.add(new Label("Tailing Factor factor "
        + MZmineCore.getConfiguration().getRTFormat().format(feature.getTailingFactor())));
    featureDataSummary.add(new Label("Status: " + feature.getFeatureStatus()));
    return featureDataNode;
  }

  private SplitPane addSpectraMS1() {
    SplitPane pane = new SplitPane();
    pane.setOrientation(Orientation.HORIZONTAL);
    SpectraVisualizerTab spectraWindowMS1 = new SpectraVisualizerTab(rawFiles[0]);
    spectraWindowMS1.loadRawData(feature.getRepresentativeScan());
    pane.getItems().add(spectraWindowMS1.getContent());
    return pane;
  }

  private SplitPane addSpectraMS2() {
    SplitPane pane = new SplitPane();
    pane.setOrientation(Orientation.HORIZONTAL);
    SpectraVisualizerTab spectraWindowMS2 = new SpectraVisualizerTab(rawFiles[0]);
    spectraWindowMS2.loadRawData(feature.getMostIntenseFragmentScan());
    pane.getItems().add(spectraWindowMS2.getContent());
    return pane;
  }
}
