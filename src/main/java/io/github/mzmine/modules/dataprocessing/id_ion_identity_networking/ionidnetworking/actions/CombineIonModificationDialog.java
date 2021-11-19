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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.actions;


import io.github.mzmine.datamodel.identities.iontype.CombinedIonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.main.MZmineCore;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CombineIonModificationDialog extends Stage {

  private final BorderPane mainPane;

  private ListView<IonModification> adducts;
  private ListView<IonModification> combine;

  // new types to be added
  private List<IonModification> newTypes = new ArrayList<>();

  public static void main(String[] args) {
    CombineIonModificationDialog d = new CombineIonModificationDialog(List.of());
    d.show();
  }

  public CombineIonModificationDialog(List<IonModification> add) {

    mainPane = new BorderPane();
    Scene scene = new Scene(mainPane);
    // Use main CSS
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);

    adducts = new ListView<>();
    ScrollPane scrollAdducts = new ScrollPane(adducts);

    adducts.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        add(adducts.getSelectionModel().getSelectedItems());
        event.consume();
      }
    });

    Button button = new Button(">");
    button.setOnAction(e -> add(adducts.getSelectionModel().getSelectedItems()));

    Button button_1 = new Button("<");
    button_1.setOnAction(e -> remove(combine.getSelectionModel().getSelectedItems()));

    Button button_2 = new Button("<<");
    button_2.setOnAction(e -> combine.getItems().clear());

    Button btnAdd = new Button("add");
    btnAdd.setOnAction(e -> createCombined());

    Button btnFinish = new Button("finish");
    btnFinish.setOnAction(e -> finish());

    VBox centerBox = new VBox();
    centerBox.getChildren().addAll(button, button_1, button_2, btnAdd, btnFinish);

    combine = new ListView<>();
    ScrollPane scrollCombine = new ScrollPane(combine);

    combine.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        remove(combine.getSelectionModel().getSelectedItems());
        event.consume();
      }
    });

    HBox hbox = new HBox(scrollAdducts, centerBox, scrollCombine);
    mainPane.setCenter(hbox);

    // add all
    adducts.setItems(FXCollections.observableArrayList(add));

    setTitle("Combine ions");

    setMinWidth(600);
    setMinHeight(600);

    centerOnScreen();
  }

  private void finish() {
    hide();
  }

  private void createCombined() {
    if(combine.getItems().size()>1) {
      IonModification nt = CombinedIonModification.create(combine.getItems());
      newTypes.add(nt);
      // add to adducts
      ObservableList<IonModification> addModel = adducts.getItems();
      addModel.add(nt);
    }
  }

  public List<IonModification> getNewTypes() {
    return newTypes;
  }

  private void add(List<IonModification> list) {
    ObservableList<IonModification> model = combine.getItems();
    model.addAll(list);
  }

  private void add(IonModification e) {
    ObservableList<IonModification> model = combine.getItems();
    model.add(e);
  }

  private void remove(List<IonModification> list) {
    ObservableList<IonModification> model = combine.getItems();
    model.removeAll(list);
  }

}
