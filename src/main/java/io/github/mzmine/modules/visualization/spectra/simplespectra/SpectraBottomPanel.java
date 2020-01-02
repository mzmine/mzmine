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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.event.TreeModelListener;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.util.GUIUtils;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 * Spectra visualizer's bottom panel
 */
class SpectraBottomPanel extends BorderPane implements TreeModelListener {

  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // Get arrow characters by their UTF16 code
  public static final String leftArrow = new String(new char[] {'\u2190'});
  public static final String rightArrow = new String(new char[] {'\u2192'});

  public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

  private FlowPane topPanel, bottomPanel;
  private ComboBox<String> msmsSelector;
  private ComboBox<PeakList> peakListSelector;
  private CheckBox processingCbx;
  private Button processingParametersBtn;

  private RawDataFile dataFile;
  private SpectraVisualizerWindow masterFrame;

  // Last time the data set was redrawn.
  private static long lastRebuildTime = System.currentTimeMillis();

  // Refresh interval (in milliseconds).
  private static final long REDRAW_INTERVAL = 1000L;

  SpectraBottomPanel(SpectraVisualizerWindow masterFrame, RawDataFile dataFile) {

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
    // prevScanBtn.setBackground(Color.white);
    // prevScanBtn.setFont(smallFont);

    // topPanel.add(Box.createHorizontalGlue());

    GUIUtils.addLabel(topPanel, "Feature list: ", SwingConstants.RIGHT);

    peakListSelector = new ComboBox<PeakList>(
        MZmineCore.getProjectManager().getCurrentProject().getFeatureLists());
    // peakListSelector.setBackground(Color.white);
    // peakListSelector.setFont(smallFont);
    peakListSelector.addActionListener(masterFrame);
    peakListSelector.setActionCommand("PEAKLIST_CHANGE");
    topPanel.add(peakListSelector);

    processingCbx = GUIUtils.addCheckbox(topPanel, "Enable Processing", masterFrame,
        "ENABLE_PROCESSING", "Enables quick scan processing.");
    processingCbx.setBackground(Color.white);
    processingCbx.setFont(smallFont);
    updateProcessingCheckbox();

    processingParametersBtn = GUIUtils.addButton(topPanel, "Spectra processing", null, masterFrame,
        "SET_PROCESSING_PARAMETERS", "Set the parameters for quick spectra processing.");
    processingParametersBtn.setBackground(Color.white);
    processingParametersBtn.setFont(smallFont);
    updateProcessingButton();

    topPanel.add(Box.createHorizontalGlue());

    JButton nextScanBtn = GUIUtils.addButton(topPanel, rightArrow, null, masterFrame, "NEXT_SCAN");
    nextScanBtn.setBackground(Color.white);
    nextScanBtn.setFont(smallFont);

    topPanel.add(Box.createHorizontalStrut(10));

    bottomPanel = new FlowPane();
    // bottomPanel.setBackground(Color.white);
    // bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
    setBottom(bottomPanel);

    bottomPanel.add(Box.createHorizontalGlue());

    GUIUtils.addLabel(bottomPanel, "MS/MS: ", SwingConstants.RIGHT);

    msmsSelector = new JComboBox<String>();
    msmsSelector.setBackground(Color.white);
    msmsSelector.setFont(smallFont);
    bottomPanel.add(msmsSelector);

    Button showButton = GUIUtils.addButton(bottomPanel, "Show", null, masterFrame, "SHOW_MSMS");
    // showButton.setBackground(Color.white);
    showButton.setFont(smallFont);

    // bottomPanel.add(Box.createHorizontalGlue());

  }

  JComboBox<String> getMSMSSelector() {
    return msmsSelector;
  }

  void setMSMSSelectorVisible(boolean visible) {
    bottomPanel.setVisible(visible);
  }

  /**
   * Returns selected feature list
   */
  PeakList getSelectedPeakList() {
    PeakList selectedPeakList = (PeakList) peakListSelector.getSelectedItem();
    return selectedPeakList;
  }



  public void updateProcessingCheckbox() {
    processingCbx.setSelected(DataPointProcessingManager.getInst().isEnabled());
  }

  public void updateProcessingButton() {
    processingParametersBtn.setEnabled(DataPointProcessingManager.getInst().isEnabled());
  }
}
