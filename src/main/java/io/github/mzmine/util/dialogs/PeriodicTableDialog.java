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

import java.util.EventObject;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.event.ICDKChangeListener;
import org.openscience.cdk.interfaces.IIsotope;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PeriodicTableDialog extends Stage implements ICDKChangeListener {

  private PeriodicTableDialogController periodicTable;
  private IIsotope selectedIsotope;

  public PeriodicTableDialog() {
    try {
      Parent root = FXMLLoader.load(getClass().getResource("PeriodicTableDialog.fxml"));
      Scene scene = new Scene(root, 700, 400);
      super.setScene(scene);
      super.setTitle("Choose an element...");
    }

    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
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
  }

  public IIsotope getSelectedIsotope() {
    return selectedIsotope;
  }

}

