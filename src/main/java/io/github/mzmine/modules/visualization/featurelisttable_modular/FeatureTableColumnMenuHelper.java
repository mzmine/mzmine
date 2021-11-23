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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseEvent;

/**
 * Helper class to replace default column selection popup for TableView.
 *
 * <p>
 * The original idea credeted to Roland and was found on https://stackoverflow.com/questions/27739833/adapt-tableview-menu-button
 * </p>
 * <p>
 * This improved version targets to solve several problems:
 * <ul>
 * <li>avoid to have to assign the TableView with the new context menu after the
 * window shown (it could cause difficulty when showAndWait() should be used. It
 * solves the problem by registering the onShown event of the containing Window.
 * </li>
 * <li>corrects the mispositioning bug when clicking the + button while the menu
 * is already on.</li>
 * <li>works using keyboard</li>
 * <li>possibility to add additional menu items</li>
 * </ul>
 * </p>
 * <p>
 * Usage from your code:
 *
 * <pre>
 * contextMenuHelper = new TableViewContextMenuHelper(this);
 * // Adding additional menu items
 * MenuItem exportMenuItem = new MenuItem("Export...");
 * contextMenuHelper.getAdditionalMenuItems().add(exportMenuItem);
 * </pre>
 * </p>
 * <p>
 * https://stackoverflow.com/questions/27739833/adapt-tableview-menu-button
 *
 * @author Roland
 * @author bvissy
 */
public class FeatureTableColumnMenuHelper extends TableColumnMenuHelper {

  private final FeatureTableFX featureTable;

  public FeatureTableColumnMenuHelper(FeatureTableFX featureTable) {
    super(featureTable);
    this.featureTable = featureTable;
  }

  /**
   * Create a menu with custom items. The important thing is that the menu remains open while you
   * click on the menu items.
   */
  @Override
  protected ContextMenu createContextMenu() {

    ContextMenu cm = new ContextMenu();

    // create new context menu
    CustomMenuItem cmi;

    if (showAllColumnsOperators) {
      // select all item
      Label selectAll = new Label("Select all");
      selectAll.addEventHandler(MouseEvent.MOUSE_CLICKED, this::doSelectAll);

      cmi = new CustomMenuItem(selectAll);
      cmi.setOnAction(this::doSelectAll);
      cmi.setHideOnClick(false);
      cm.getItems().add(cmi);

      // deselect all item
      Label deselectAll = new Label("Deselect all");
      deselectAll.addEventHandler(MouseEvent.MOUSE_CLICKED, this::doDeselectAll);

      cmi = new CustomMenuItem(deselectAll);
      cmi.setOnAction(this::doDeselectAll);
      cmi.setHideOnClick(false);
      cm.getItems().add(cmi);

      // separator
      cm.getItems().add(new SeparatorMenuItem());
    }

    if (!additionalMenuItems.isEmpty()) {
      cm.getItems().addAll(additionalMenuItems);
      cm.getItems().add(new SeparatorMenuItem());
    }

    final DataTypeCheckListParameter rowParam = featureTable.getRowTypesParameter();
    final var colMap = featureTable.getNewColumnMap();
    // menu item for each of the available columns
    for (var entry : colMap.entrySet()) {
      var col = entry.getKey();
      final ColumnID value = entry.getValue();
      if (value.getType() != ColumnType.ROW_TYPE) {
        continue;
      }

      String header = value.getCombinedHeaderString();

      CheckMenuItem cb = new CheckMenuItem(header);
      cb.selectedProperty().bindBidirectional(col.visibleProperty());
      cb.setOnAction(e -> {
        cb.setSelected(!cb.isSelected());
        rowParam.setDataTypeVisible(value, cb.isSelected());

        e.consume();
      });

      cm.getItems().add(cb);
    }

    final DataTypeCheckListParameter fParam = featureTable.getFeatureTypesParameter();

    return cm;
  }

}