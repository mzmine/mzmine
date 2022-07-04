/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.test_visualizer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class openSetupDialog extends Stage {
  static int NodeValue=0;
  static String GeneratingAlgo="";
  private  enum GA {
    RandomGenerator, BarabasiAlbertGenerator, SquareGridGenerator, RandomEuclideanGenerator, DorogovtsevMendesGenerator
  }
  public openSetupDialog() {
    Stage s = new Stage();
    s.initModality(Modality.APPLICATION_MODAL);
    TextField tf = new TextField();
    ComboBox cb = new ComboBox();
    cb.getItems().setAll(GA.values());
    cb.getSelectionModel().select(GA.RandomGenerator);
    Button btn = new Button("Generate Graph");
    btn.setOnAction(e -> {
      NodeValue = Integer.parseInt(tf.getText());
      GeneratingAlgo = cb.getValue()+"";
      s.close();
    });
    Label lb1 = new Label("Enter the minimum no. of nodes:");
    Label lb2 = new Label("Generating algorithm:");
    GridPane layout = new GridPane();
    layout.setPadding(new Insets(10, 10, 10, 10));
    layout.setVgap(5);
    layout.setHgap(5);
    layout.add(tf, 1, 1);
    layout.add(cb, 1, 2);
    layout.add(btn, 1, 3);
    layout.add(lb1, 0, 1);
    layout.add(lb2, 0, 2);
    Scene scene = new Scene(layout, 450, 150);
    s.setTitle("Genarate a Graph");
    s.setScene(scene);
    s.setResizable(false);
    s.showAndWait();
  }
  public String getGeneratingAlgo()
  {
    return GeneratingAlgo;
  }
  public int getNodeValue()
  {
    return NodeValue;
  }
}
