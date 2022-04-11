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
