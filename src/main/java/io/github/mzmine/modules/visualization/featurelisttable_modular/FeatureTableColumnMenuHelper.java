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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TreeTableColumn;

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
    ContextMenu cm = createBaseMenu();

    final DataTypeCheckListParameter rowParam = featureTable.getRowTypesParameter();
    final var colMap = featureTable.getNewColumnMap();
    // menu item for each of the available columns
    addTypeCheckList(cm, colMap, rowParam);

    final DataTypeCheckListParameter fParam = featureTable.getFeatureTypesParameter();
    addTypeCheckList(cm, colMap, fParam);
    return cm;
  }

  private void addTypeCheckList(ContextMenu cm,
      Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> colMap,
      DataTypeCheckListParameter param) {
    // do not add range sub columns
    param.getValue().entrySet().stream()
        .filter(e -> !e.getKey().contains("range:min") && !e.getKey().contains("range:max"))
        .sorted(Comparator.comparing(Entry::getKey)).forEach(entry -> {
      final String combinedHeader = entry.getKey();

      CheckBox cb = new CheckBox(combinedHeader);
      cb.getStyleClass().add("small-check-box");
      cb.setSelected(entry.getValue());

      CustomMenuItem cmi = new CustomMenuItem(cb);
      cmi.setHideOnClick(false);
      cmi.setOnAction(event -> {
        cb.setSelected(!cb.isSelected());
        event.consume();
      });
      cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
        param.setDataTypeVisible(combinedHeader, cb.isSelected());
        setColsVisible(colMap, combinedHeader, cb.isSelected());
      });
      cm.getItems().add(cmi);

    });
  }

  private void setColsVisible(Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> colMap,
      String combinedColHeader, boolean visible) {
    colMap.entrySet().stream().filter(mapEntry -> mapEntry.getValue().typesMatch(combinedColHeader))
        .map(Entry::getKey).forEach(col -> col.setVisible(visible));
    featureTable.applyVisibilityParametersToAllColumns();
  }

}