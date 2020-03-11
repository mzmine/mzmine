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

package io.github.mzmine.util.dialogs;

import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.event.ICDKChangeListener;
import org.openscience.cdk.interfaces.IIsotope;

import java.util.EventObject;

public class PeriodicTableDialog extends Stage implements ICDKChangeListener {

  /**
   *
   */
  private PeriodicTablePanel periodicTable;
  private IIsotope selectedIsotope;

  @FXML
  private BorderPane BorderPaneArea;

  public void start(Stage primaryStage) throws Exception{
    Parent root = FXMLLoader.load(getClass().getResource("PeriodicTableDialog.fxml"));

    periodicTable = new PeriodicTablePanel();
    periodicTable.addCDKChangeListener(this);
    SwingNode sn = new SwingNode();
    sn.setContent(periodicTable);

    BorderPaneArea.setAlignment(sn, Pos.CENTER);
    primaryStage.setScene(new Scene(root));
    primaryStage.setTitle("Choose an element...");
    primaryStage.show();
  }

  public void stateChanged(EventObject event) {

    if (event.getSource() == periodicTable) {
      try {
        String elementSymbol = periodicTable.getSelectedElement();
        IsotopeFactory isoFac = Isotopes.getInstance();
        selectedIsotope = isoFac.getMajorIsotope(elementSymbol);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.exit(0);
    }
  }

  public IIsotope getSelectedIsotope() {
    return selectedIsotope;
  }

}
