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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceComponent;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Spectra visualizer's bottom panel
 */
class SpectraBottomPanel extends VBox {

  // Get arrow characters by their UTF16 code
  public static final String leftArrow = "←";
  public static final String rightArrow = "→";
  private static final Logger logger = Logger.getLogger(SpectraBottomPanel.class.getName());
  // Refresh interval (in milliseconds).
  private static final long REDRAW_INTERVAL = 1000L;
  // Last time the data set was redrawn.
  private static final long lastRebuildTime = System.currentTimeMillis();
  private final FlowPane topPanel;
  private final FlowPane bottomPanel;
  private final ComboBox<Scan> msmsSelector;
  private final ComboBox<FeatureList> peakListSelector;
  private final MZToleranceComponent mzTolerance;
  private final RawDataFile dataFile;
  private final SpectraVisualizerTab masterFrame;

  SpectraBottomPanel(SpectraVisualizerTab masterFrame, RawDataFile dataFile) {
    this.dataFile = dataFile;
    this.masterFrame = masterFrame;

    Button prevScanBtn = new Button(leftArrow);
    prevScanBtn.setOnAction(e -> masterFrame.loadPreviousScan());
    Label featureListLabel = new Label("Feature list: ");

    peakListSelector = new ComboBox<>(FXCollections.observableArrayList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()));
    peakListSelector.setOnAction(
        e -> masterFrame.loadPeaks(peakListSelector.getSelectionModel().getSelectedItem()));

    Button nextScanBtn = new Button(rightArrow);
    nextScanBtn.setOnAction(e -> masterFrame.loadNextScan());

    mzTolerance = new MZToleranceComponent();
    mzTolerance.setToolTipText("m/z tolerance used around selected signals");
    PauseTransition delay = new PauseTransition(Duration.seconds(1));
    delay.setOnFinished(event -> masterFrame.setMzTolerance(getMzTolerance()));
    mzTolerance.setListener(delay::playFromStart);

    topPanel = new FlowPane(4, 2);
    topPanel.getChildren().addAll(prevScanBtn, featureListLabel, peakListSelector, nextScanBtn,
        new Label("m/z tolerance"), mzTolerance);

    bottomPanel = new FlowPane(4, 2);

    this.getChildren().addAll(topPanel, bottomPanel);

    Label msmsLabel = new Label("MS/MS: ");

    msmsSelector = new ComboBox<>();

    Button showButton = new Button("Show");
    bottomPanel.getChildren().addAll(msmsLabel, msmsSelector, showButton);

    showButton.setOnAction(e -> {
      Scan selectedScan = msmsSelector.getSelectionModel().getSelectedItem();
      if (selectedScan == null) {
        return;
      }

      SpectraVisualizerModule.addNewSpectrumTab(dataFile, selectedScan);
    });

    // bind mztolerance
    masterFrame.mzToleranceProperty().addListener((o, old, newValue) -> setMzTolerance(newValue));
  }

  ComboBox<Scan> getMSMSSelector() {
    return msmsSelector;
  }

  void setMSMSSelectorVisible(boolean visible) {
    bottomPanel.setVisible(visible);
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance.getValue();
  }

  public void setMzTolerance(MZTolerance mzTol) {
    mzTolerance.setValue(mzTol);
  }
}
