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

package io.github.mzmine.modules.visualization.infovisualizer;

import java.text.NumberFormat;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

class InfoVisualizerWindow extends Stage {

  private final Scene mainScene;
  private final GridPane mainPane;

  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  Range<Double> rtRange, mzRange;
  int numOfIdentities;

  InfoVisualizerWindow(PeakList peakList) {

    setTitle(peakList.getName() + " information");

    mainPane = new GridPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    setMinWidth(400.0);
    setMinHeight(300.0);

    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    // setBackground(Color.white);

    this.getInfoRange(peakList);

    if (peakList.getNumberOfRows() == 0) {
      mzRange = Range.singleton(0.0);
      rtRange = Range.singleton(0.0);
    }

    int row = 0;
    Label label, value;

    // Raw data file list
    label = new Label("List of raw data files");
    ListView<RawDataFile> rawDataFileList =
        new ListView<RawDataFile>(FXCollections.observableArrayList(peakList.getRawDataFiles()));
    // rawDataFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // rawDataFileList.setLayoutOrientation(JList.VERTICAL);
    label.setLabelFor(rawDataFileList);
    mainPane.add(label, 0, row);
    mainPane.add(rawDataFileList, 1, row);

    // rawlistScroller.setPreferredSize(new Dimension(250, 60));
    // rawlistScroller.setAlignmentX(LEFT_ALIGNMENT);
    // rawPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    // Applied methods list
    ListView<PeakListAppliedMethod> appliedMethodList =
        new ListView<>(FXCollections.observableArrayList(peakList.getAppliedMethods()));
    // appliedMethodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // appliedMethodList.setLayoutOrientation(JList.VERTICAL);



    // JLabel label = new JLabel("List of applied methods");
    // label.setLabelFor(processInfoList);
    label = new Label("List of applied methods");
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(appliedMethodList, 1, row);

    // Panels
    label = new Label("Name:");
    value = new Label(peakList.getName());
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);


    label = new Label("Created (yyyy/MM/dd HH:mm:ss):");
    value = new Label(String.valueOf(((SimplePeakList) peakList).getDateCreated()));
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);

    label = new Label("Number of rows:");
    value = new Label(String.valueOf(peakList.getNumberOfRows()));
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);

    label = new Label("m/z range:");
    value = new Label(mzFormat.format(mzRange.lowerEndpoint()) + " - "
        + mzFormat.format(mzRange.upperEndpoint()));
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);

    label = new Label("RT range:");
    value = new Label(rtFormat.format(rtRange.lowerEndpoint()) + " - "
        + rtFormat.format(rtRange.upperEndpoint()));
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);

    label = new Label("Number of identified peaks:");
    value = new Label(String.valueOf(numOfIdentities));
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);

    setResizable(false);
    sizeToScene();

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);

    // pack();

    // get the window settings parameter
    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(InfoVisualizerModule.class);
    WindowSettingsParameter settings =
        paramSet.getParameter(InfoVisualizerParameters.windowSettings);

    // update the window and listen for changes
    // settings.applySettingsToWindow(this);
    // this.addComponentListener(settings);

  }


  /*
   * public String getToolTipText(MouseEvent e) {
   * 
   * int index = locationToIndex(e.getPoint()); if (index > -1) { parameters =
   * ((PeakListAppliedMethod) getModel().getElementAt(index)).getParameters(); } if (parameters !=
   * null) { String toolTipText = parameters.toString().replace(", ", "\n"); return toolTipText; }
   * else return null; }
   */

  void getInfoRange(PeakList peakList) {
    PeakListRow[] rows = peakList.getRows();

    mzRange = peakList.getRowsMZRange();
    rtRange = peakList.getRowsRTRange();
    for (PeakListRow row : rows) {
      if (row.getPreferredPeakIdentity() != null)
        numOfIdentities++;
    }

  }
}
