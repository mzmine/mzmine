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

package io.github.mzmine.util.dialogs;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class PeriodicTableDialog extends Stage /*implements ICDKChangeListener*/ {

  private static final Logger logger = Logger.getLogger(PeriodicTableDialog.class.getName());
  private PeriodicTableDialogController periodicTable;
  private IIsotope selectedIsotope;

  public PeriodicTableDialog() {
    this(false);
  }

  public PeriodicTableDialog(boolean multipleSelection) {
    BorderPane borderPane = new BorderPane();
    borderPane.setPadding(new Insets(10, 10, 10, 10));

    Scene scene = new Scene(borderPane);
    super.setScene(scene);
    super.setTitle("Periodic table");
    super.setResizable(false);

    // Add periodic table
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("PeriodicTableDialog.fxml"));
      Parent root = loader.load();
      borderPane.setCenter(root);
      periodicTable = loader.getController();
      periodicTable.setMultipleSelection(multipleSelection);
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    // Add OK button
    Button btnClose = new Button("OK");
    btnClose.setOnAction(e -> super.hide());
    ButtonBar btnBar = new ButtonBar();
    btnBar.getButtons().addAll(btnClose);
    borderPane.setBottom(btnBar);
  }

/*  @Override
  public void stateChanged(EventObject event) {

    if (event.getSource() == periodicTable) {
      try {
        IsotopeFactory isoFac = Isotopes.getInstance();
        selectedIsotope = isoFac.getMajorIsotope(periodicTable.getElementSymbol());
      } catch (Exception e) {
        e.printStackTrace();
      }
      hide();
    }
  }*/

  public IIsotope getSelectedIsotope() {

    String symbol = periodicTable.getElementSymbol();
    if (symbol == null) {
      return null;
    }

    try {
      IsotopeFactory isoFac = Isotopes.getInstance();
      selectedIsotope = isoFac.getMajorIsotope(symbol);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return selectedIsotope;
  }

  public ObservableList<Element> getSelectedElements() {
    return periodicTable.getSelectedElements();
  }

  public void setSelectedElements(List<Element> elements) {
    periodicTable.setSelectedElements(elements);
  }
}

