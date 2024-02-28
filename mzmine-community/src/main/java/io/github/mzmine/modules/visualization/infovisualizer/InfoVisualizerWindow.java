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

package io.github.mzmine.modules.visualization.infovisualizer;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.text.NumberFormat;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
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

  Range<Double> mzRange;
  Range<Float> rtRange;
  int numOfIdentities;

  InfoVisualizerWindow(FeatureList featureList) {

    setTitle(featureList.getName() + " information");

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

    this.getInfoRange(featureList);

    if (featureList.getNumberOfRows() == 0) {
      mzRange = Range.singleton(0.0);
      rtRange = Range.singleton(0f);
    }

    int row = 0;
    Label label, value;

    // Raw data file list
    label = new Label("List of raw data files");
    ListView<RawDataFile> rawDataFileList =
        new ListView<RawDataFile>(FXCollections.observableArrayList(featureList.getRawDataFiles()));
    // rawDataFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // rawDataFileList.setLayoutOrientation(JList.VERTICAL);
    label.setLabelFor(rawDataFileList);
    mainPane.add(label, 0, row);
    mainPane.add(rawDataFileList, 1, row);

    // rawlistScroller.setPreferredSize(new Dimension(250, 60));
    // rawlistScroller.setAlignmentX(LEFT_ALIGNMENT);
    // rawPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    // Applied methods list
    ListView<FeatureListAppliedMethod> appliedMethodList =
        new ListView<>(FXCollections.observableArrayList(featureList.getAppliedMethods()));
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
    value = new Label(featureList.getName());
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);


    label = new Label("Created (yyyy/MM/dd HH:mm:ss):");
    value = new Label(String.valueOf((featureList).getDateCreated()));
    row++;
    mainPane.add(label, 0, row);
    mainPane.add(value, 1, row);

    label = new Label("Number of rows:");
    value = new Label(String.valueOf(featureList.getNumberOfRows()));
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

  void getInfoRange(FeatureList peakList) {
    FeatureListRow[] rows = peakList.getRows().toArray(FeatureListRow[]::new);

    mzRange = peakList.getRowsMZRange();
    rtRange = peakList.getRowsRTRange();
    for (FeatureListRow row : rows) {
      if (row.getPreferredFeatureIdentity() != null)
        numOfIdentities++;
    }

  }
}
