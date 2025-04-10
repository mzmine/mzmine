/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TreeTableColumn;

/**
 * Helper class to replace default column selection popup for TableView.
 *
 * <p>
 * The original idea credeted to Roland and was found on
 * https://stackoverflow.com/questions/27739833/adapt-tableview-menu-button
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
    super(featureTable.getTable());
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
    final DataTypeCheckListParameter fParam = featureTable.getFeatureTypesParameter();
    final var colMap = featureTable.getNewColumnMap();
    // menu item for each of the available columns
    addTypeCheckList(cm, colMap, rowParam, fParam);

    return cm;
  }

  private void addTypeCheckList(ContextMenu cm,
      Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> colMap,
      DataTypeCheckListParameter rowParam, DataTypeCheckListParameter featureParam) {
    // do not add range sub columns, only add feature types once (wrapper)
    colMap.values().stream().filter(colId -> !colId.getCombinedHeaderString().contains("range:min")
            && !colId.getCombinedHeaderString().contains("range:max")).map(ColIdWrapper::new).distinct()
        .map(ColIdWrapper::unwrap).sorted(Comparator.comparing(ColumnID::getCombinedHeaderString))
        .forEach(colId -> {
          final String combinedHeader = colId.getCombinedHeaderString();

          CheckBox cb = new CheckBox(combinedHeader);
          cb.getStyleClass().add("small-check-box");
          cb.setSelected(colId.getType() == ColumnType.ROW_TYPE ? rowParam.isDataTypeVisible(colId)
              : featureParam.isDataTypeVisible(colId));

          CustomMenuItem cmi = new CustomMenuItem(cb);
          cmi.setHideOnClick(false);
          cmi.setOnAction(event -> {
            cb.setSelected(!cb.isSelected());
            event.consume();
          });
          cb.selectedProperty().addListener((_, _, _) -> {
            if (colId.getType() == ColumnType.ROW_TYPE) {
              rowParam.setDataTypeVisible(colId.getUniqueIdString(), cb.isSelected());
            } else {
              featureParam.setDataTypeVisible(colId.getUniqueIdString(), cb.isSelected());
            }
            setColsVisible(colMap, colId.getUniqueIdString(), cb.isSelected());
          });
          cm.getItems().add(cmi);
        });
  }

  private void setColsVisible(Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> colMap,
      String uniqueIdString, boolean visible) {
    colMap.entrySet().stream()
        .filter(mapEntry -> mapEntry.getValue().getUniqueIdString().equals(uniqueIdString))
        .map(Entry::getKey).forEach(col -> col.setVisible(visible));
    featureTable.applyVisibilityParametersToAllColumns();
  }

  /**
   * Wrapper for {@link ColumnID} to make use of {@link Stream#distinct()} method to narrow the
   * feature type columns to a single entry per data type. For example, without this wrapper, the
   * Feature:mz type would be added to the menu as many times as there are raw data files. This
   * class does not take the different data files into account in the {@link #equals(Object)}
   * method, but only if it is a row/feature column and the types itself.
   *
   * @param id The {@link ColumnID} to wrap
   */
  private record ColIdWrapper(ColumnID id) {

    public ColumnID unwrap() {
      return id;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ColIdWrapper w)) {
        return false;
      }
      return Objects.equals(w.id.getType(), id.getType()) && Objects.equals(
          w.id.getUniqueIdString(), id.getUniqueIdString());
    }

    @Override
    public int hashCode() {
      return Objects.hash(id.getType(), id.getUniqueIdString());
    }
  }
}
