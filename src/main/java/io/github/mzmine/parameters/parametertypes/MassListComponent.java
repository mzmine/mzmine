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

package io.github.mzmine.parameters.parametertypes;

import java.util.ArrayList;
import java.util.List;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class MassListComponent extends FlowPane {

  private TextField nameField;
  private Button lookupButton;
  private ContextMenu lookupMenu;

  public MassListComponent() {

    setHgap(5.0);

    lookupMenu = new ContextMenu();

    nameField = new TextField();
    nameField.setPrefColumnCount(15);

    lookupButton = new Button("Choose...");
    lookupButton.setOnAction(e -> {
      List<String> currentNames = getMassListNames();

      lookupMenu.getItems().clear();
      for (String name : currentNames) {
        MenuItem item = new MenuItem(name);
        item.setOnAction(e2 -> {
          nameField.setText(name);
        });
        lookupMenu.getItems().add(item);
      }
      final Bounds boundsInScreen = lookupButton.localToScreen(lookupButton.getBoundsInLocal());
      lookupMenu.show(lookupButton, boundsInScreen.getCenterX(), boundsInScreen.getCenterY());
//      lookupMenu.show(lookupButton, 0, 0);
    });

    getChildren().addAll(nameField, lookupButton);

  }

  public String getValue() {
    return nameField.getText();
  }

  public void setValue(String value) {
    nameField.setText(value);
  }

  public void setToolTipText(String toolTip) {
    nameField.setTooltip(new Tooltip(toolTip));
  }

  /**
   * Method returns the list of all identified MassList names in scans
   *
   * @return unique MassList names
   */
  public static List<String> getMassListNames() {
    ArrayList<String> names = new ArrayList<>();
    RawDataFile dataFiles[] = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    for (RawDataFile dataFile : dataFiles) {
      for(Scan scan : dataFile.getScans()) {
        MassList massLists[] = scan.getMassLists();
        for (MassList massList : massLists) {
          String name = massList.getName();
          if (!names.contains(name))
            names.add(name);
        }
      }
    }

    return names;
  }

}
