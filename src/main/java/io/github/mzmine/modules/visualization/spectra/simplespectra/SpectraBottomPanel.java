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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import java.awt.Font;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 * Spectra visualizer's bottom panel
 */
class SpectraBottomPanel extends BorderPane {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // Get arrow characters by their UTF16 code
  public static final String leftArrow = new String(new char[] {'\u2190'});
  public static final String rightArrow = new String(new char[] {'\u2192'});

  public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

  private FlowPane topPanel, bottomPanel;
  private ComboBox<Scan> msmsSelector;
  private ComboBox<FeatureList> peakListSelector;
  private CheckBox processingCbx;
  private Button processingParametersBtn;

  private RawDataFile dataFile;
  private SpectraVisualizerTab masterFrame;

  // Last time the data set was redrawn.
  private static long lastRebuildTime = System.currentTimeMillis();

  // Refresh interval (in milliseconds).
  private static final long REDRAW_INTERVAL = 1000L;

  SpectraBottomPanel(SpectraVisualizerTab masterFrame, RawDataFile dataFile) {

    // super(new BorderLayout());
    this.dataFile = dataFile;
    this.masterFrame = masterFrame;

    // setBackground(Color.white);

    topPanel = new FlowPane();
    // topPanel.setBackground(Color.white);
    // topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
    setCenter(topPanel);

    // topPanel.add(Box.createHorizontalStrut(10));

    Button prevScanBtn = new Button(leftArrow);
    prevScanBtn.setOnAction(e -> masterFrame.loadPreviousScan());
    // prevScanBtn.setBackground(Color.white);
    // prevScanBtn.setFont(smallFont);

    // topPanel.add(Box.createHorizontalGlue());

    Label featureListLabel = new Label("Feature list: ");

    peakListSelector = new ComboBox<>(FXCollections.observableList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()));
    // peakListSelector.setBackground(Color.white);
    // peakListSelector.setFont(smallFont);
    peakListSelector.setOnAction(
        e -> masterFrame.loadPeaks(peakListSelector.getSelectionModel().getSelectedItem()));

    processingCbx = new CheckBox("Enable Processing");
    processingCbx.setTooltip(new Tooltip("Enables quick scan processing."));
    processingCbx.setOnAction(e -> masterFrame.enableProcessing());
    updateProcessingCheckbox();

    processingParametersBtn = new Button("Spectra processing");
    processingParametersBtn
        .setTooltip(new Tooltip("Set the parameters for quick spectra processing."));
    processingParametersBtn.setOnAction(e -> masterFrame.setProcessingParams());
    updateProcessingButton();

    // topPanel.add(Box.createHorizontalGlue());

    Button nextScanBtn = new Button(rightArrow);
    nextScanBtn.setOnAction(e -> masterFrame.loadNextScan());

    topPanel.getChildren().addAll(prevScanBtn, featureListLabel, peakListSelector, processingCbx,
        processingParametersBtn, nextScanBtn);

    // nextScanBtn.setBackground(Color.white);
    // nextScanBtn.setFont(smallFont);

    // topPanel.add(Box.createHorizontalStrut(10));

    bottomPanel = new FlowPane();
    // bottomPanel.setBackground(Color.white);
    // bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
    setBottom(bottomPanel);

    // bottomPanel.add(Box.createHorizontalGlue());

    Label msmsLabel = new Label("MS/MS: ");

    msmsSelector = new ComboBox<>();
    // msmsSelector.setBackground(Color.white);
    // msmsSelector.setFont(smallFont);

    Button showButton = new Button("Show");
    bottomPanel.getChildren().addAll(msmsLabel, msmsSelector, showButton);

    // showButton.setBackground(Color.white);
    showButton.setOnAction(e -> {
      Scan selectedScan = msmsSelector.getSelectionModel().getSelectedItem();
      if (selectedScan == null)
        return;

      SpectraVisualizerModule.addNewSpectrumTab(dataFile, selectedScan);
    });
    // showButton.setFont(smallFont);

    // bottomPanel.add(Box.createHorizontalGlue());

  }

  ComboBox<Scan> getMSMSSelector() {
    return msmsSelector;
  }

  void setMSMSSelectorVisible(boolean visible) {
    bottomPanel.setVisible(visible);
  }

  /**
   * Returns selected feature list
   */
  FeatureList getSelectedPeakList() {
    FeatureList selectedPeakList = peakListSelector.getSelectionModel().getSelectedItem();
    return selectedPeakList;
  }



  public void updateProcessingCheckbox() {
    processingCbx.setSelected(DataPointProcessingManager.getInst().isEnabled());
  }

  public void updateProcessingButton() {
    processingParametersBtn.setDisable(!DataPointProcessingManager.getInst().isEnabled());
  }
}
