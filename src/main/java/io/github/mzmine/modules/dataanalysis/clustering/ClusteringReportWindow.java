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

package io.github.mzmine.modules.dataanalysis.clustering;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ClusteringReportWindow extends Stage {

  /**
   * 
   */
  private final Scene mainScene;
  private TableView tableView;

  public ClusteringReportWindow(String[] samplesOrVariables, Integer[] clusteringData,
      String title) {
    super();
    this.setTitle(title);

    SampleClusters[] data = new SampleClusters[samplesOrVariables.length];
    for (int i = 0; i < samplesOrVariables.length; i++) {
      data[i] = new SampleClusters(samplesOrVariables[i], clusteringData[i]);
    }

    ObservableList<SampleClusters> dataList = FXCollections.observableArrayList(data);
    tableView = new TableView<SampleClusters>(dataList);

    TableColumn sampleColumn = new TableColumn("Variables");
    sampleColumn.setCellValueFactory(new PropertyValueFactory<>("sampleOrVariable"));
    TableColumn clusterColumn = new TableColumn("Cluster number");
    clusterColumn.setCellValueFactory(new PropertyValueFactory<>("cluster"));

    tableView.getColumns().addAll(sampleColumn, clusterColumn);

    mainScene = new Scene(tableView);
    setScene(mainScene);
  }
  
}
