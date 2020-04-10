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

package io.github.mzmine.modules.dataprocessing.id_sirius.table.db;

import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unijena.bioinf.chemdb.DBLink;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.SiriusCompound;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Class DBFrame Creates a new window with Database links (e.g.: Pubchem: 1520) New window is
 * created on a position of `Display DB links` button (it is parent JFrame) Opens new window in a
 * browser if entry point is known, otherwise shows dialogue window.
 */
public class DBFrame extends Stage {

  private static final Logger logger = LoggerFactory.getLogger(DBFrame.class);
  private final TableView<SiriusDBCompound> dbTable = new TableView<>();
  private final ObservableList<SiriusDBCompound> compounds = FXCollections.observableArrayList();
  final VBox vBox = new VBox();
  final Button openBrowser = new Button();

  public DBFrame(SiriusCompound compound) {
    final Label label = new Label("List of database with IDs");
    label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
    TableColumn<SiriusDBCompound, String> database = new TableColumn<>("Database");
    TableColumn<SiriusDBCompound, String> index = new TableColumn<>("Index");
    openBrowser.setText("Open Browser");

    dbTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    dbTable.getColumns().addAll(database, index);
    vBox.setMinWidth(500);
    vBox.setMinHeight(100);
    vBox.setSpacing(10);
    vBox.setPadding(new Insets(5, 10, 5, 10));
    vBox.getChildren().addAll(label, dbTable, openBrowser);
    database.setCellValueFactory(cell -> {
      String databaseValue = cell.getValue().getDB();
      if (cell.getValue().getDB() == null) {
        return new ReadOnlyObjectWrapper<>();
      }
      return new ReadOnlyObjectWrapper<>(databaseValue);
    });

    index.setCellValueFactory(cell -> {
      String indexValue = cell.getValue().getID();
      if (cell.getValue().getID() == null) {
        return new ReadOnlyObjectWrapper<>();
      }
      return new ReadOnlyObjectWrapper<>(indexValue);
    });
    dbTable.setItems(compounds);

    Platform.runLater(() -> addElement(compound));

    openBrowser.setOnAction(e -> {
      SiriusDBCompound selectedCompound = dbTable.getSelectionModel().getSelectedItem();
      if (selectedCompound == null) {
        MZmineCore.getDesktop().displayMessage(null,
            "Select one result to add as compound identity");
        return;
      }
      // Generate url
      try {
        URL url = selectedCompound.generateURL();
        if (url == null)
          throw new RuntimeException("Unsupported DB");

        // Open uri in default browser
        MZmineCore.getDesktop().openWebPage(url);

      } catch (RuntimeException f) {
        f.printStackTrace();
        MZmineCore.getDesktop().displayMessage(null, "Not supported Database");
      } catch (IOException d) {
        d.printStackTrace();
        logger.error("Error happened on opening db link for {} : {}", selectedCompound.getDB(),
            selectedCompound.getID());

      }

    });
    Scene scene = new Scene(vBox);
    this.setScene(scene);
    this.show();
  }

  public void addElement(SiriusCompound compound) {
    SiriusIonAnnotation annotation = compound.getIonAnnotation();
    DBLink[] links = annotation.getDBLinks();
    if (links == null)
      return;

    for (DBLink link : links) {
      compounds.add(new SiriusDBCompound(link.name, link.id));
    }
  }
}

