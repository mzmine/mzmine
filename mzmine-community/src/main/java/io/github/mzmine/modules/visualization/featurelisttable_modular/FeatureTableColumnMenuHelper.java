/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
 */
public class FeatureTableColumnMenuHelper extends TableColumnMenuHelper {

  private final FeatureTableFX featureTable;

  public FeatureTableColumnMenuHelper(FeatureTableFX featureTable) {
    super(featureTable.getTable());
    this.featureTable = featureTable;
  }

  @Override
  protected void onPopupClosed() {
    super.onPopupClosed();
    ConfigService.getConfiguration().setModuleParameters(FeatureTableFXModule.class,
        featureTable.getParameters().cloneParameterSet());
  }

  record ColumnEntry(ColumnID colId, CheckBox checkBox, CustomMenuItem menuItem) {

  }

  /**
   * Create a menu with custom items. The important thing is that the menu remains open while you
   * click on the menu items. A clearable search bar at the top filters items; arrow keys navigate
   * the filtered list.
   */
  @Override
  protected ContextMenu createContextMenu() {
    ContextMenu cm = createBaseMenu();

    final DataTypeCheckListParameter rowParam = featureTable.getRowTypesParameter();
    final DataTypeCheckListParameter fParam = featureTable.getFeatureTypesParameter();
    final var colMap = featureTable.getNewColumnMap();

    // Build sorted list of all column entries

    List<ColumnEntry> allEntries = colMap.values().stream().filter(
            colId -> !colId.getCombinedHeaderString().contains("range:min")
                && !colId.getCombinedHeaderString().contains("range:max")).map(ColIdWrapper::new)
        .distinct().map(ColIdWrapper::unwrap)
        .sorted(Comparator.comparing(ColumnID::getCombinedHeaderString)).map(colId -> {
          String header = colId.getCombinedHeaderString();
          CheckBox cb = new CheckBox(header);
          cb.getStyleClass().add("small-check-box");
          cb.setSelected(colId.getType() == ColumnType.ROW_TYPE ? rowParam.isDataTypeVisible(colId)
              : fParam.isDataTypeVisible(colId));

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
              fParam.setDataTypeVisible(colId.getUniqueIdString(), cb.isSelected());
            }
            setColsVisible(colMap, colId.getUniqueIdString(), cb.isSelected());
          });
          return new ColumnEntry(colId, cb, cmi);
        }).toList();

    // Search bar inserted at position 0 (before select all / deselect all)
    TextField searchField = new TextField();
    searchField.setPromptText("Filter columns...");
    CustomMenuItem searchMenuItem = new CustomMenuItem(searchField, false);
    cm.getItems().add(0, searchMenuItem);

    // Number of fixed items above the column list (search bar + select all + deselect all + sep)
    int fixedItemCount = cm.getItems().size();

    // Add all column items initially
    cm.getItems().addAll(allEntries.stream().map(ColumnEntry::menuItem).toList());

    // Mutable navigation state
    int[] selectedIdx = {-1};
    List<ColumnEntry>[] filtered = new List[]{new ArrayList<>(allEntries)};

    // Update visual highlight for the currently selected index
    Runnable updateVisualSelection = () -> {
      for (int i = 0; i < filtered[0].size(); i++) {
        CheckBox cb = filtered[0].get(i).checkBox();
        if (i == selectedIdx[0]) {
          if (!cb.getStyleClass().contains("column-menu-selected")) {
            cb.getStyleClass().add("column-menu-selected");
          }
          cb.setStyle(
              "-fx-background-color: -fx-selection-bar; -fx-background-radius: 3; -fx-padding: 1 3 1 3;");
        } else {
          cb.getStyleClass().remove("column-menu-selected");
          cb.setStyle("");
        }
      }
    };

    // Rebuild visible column items and keep/update selection
    Runnable applyFilter = () -> {
      String query = searchField.getText().trim().toLowerCase();

      ColumnEntry previouslySelected =
          selectedIdx[0] >= 0 && selectedIdx[0] < filtered[0].size() ? filtered[0].get(
              selectedIdx[0]) : null;

      List<ColumnEntry> newFiltered = allEntries.stream().filter(
              e -> query.isEmpty() || e.colId().getCombinedHeaderString().toLowerCase().contains(query))
          .toList();
      filtered[0] = new ArrayList<>(newFiltered);

      // Update selected index: keep if still present, else select topmost (only if was selected)
      if (previouslySelected != null) {
        int idx = filtered[0].indexOf(previouslySelected);
        selectedIdx[0] = idx >= 0 ? idx : (filtered[0].isEmpty() ? -1 : 0);
      }
      // else: no previous selection → keep selectedIdx[0] as-is (-1)

      // Rebuild column items in the menu
      cm.getItems().subList(fixedItemCount, cm.getItems().size()).clear();
      cm.getItems().addAll(filtered[0].stream().map(ColumnEntry::menuItem).toList());

      // Clear styles on hidden entries
      allEntries.forEach(e -> {
        if (!filtered[0].contains(e)) {
          e.checkBox().getStyleClass().remove("column-menu-selected");
          e.checkBox().setStyle("");
        }
      });
      updateVisualSelection.run();
    };

    // On show: reset search and focus the field
    cm.setOnShown(e -> {
      searchField.setText("");
      applyFilter.run(); // reset filter (handles case where text was already empty)
      Platform.runLater(searchField::requestFocus);
    });

    // Filter as the user types
    searchField.textProperty().addListener((_, _, _) -> applyFilter.run());

    // Arrow key navigation and Enter to toggle from the search field
    searchField.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
      if (ke.getCode() == KeyCode.DOWN) {
        if (!filtered[0].isEmpty()) {
          selectedIdx[0] = Math.min(selectedIdx[0] + 1, filtered[0].size() - 1);
        }
        updateVisualSelection.run();
        ke.consume();
      } else if (ke.getCode() == KeyCode.UP) {
        if (!filtered[0].isEmpty()) {
          selectedIdx[0] = Math.max(selectedIdx[0] - 1, 0);
        }
        updateVisualSelection.run();
        ke.consume();
      } else if (ke.getCode() == KeyCode.ENTER) {
        if (selectedIdx[0] >= 0 && selectedIdx[0] < filtered[0].size()) {
          CheckBox cb = filtered[0].get(selectedIdx[0]).checkBox();
          cb.setSelected(!cb.isSelected());
        }
        ke.consume();
      }
    });

    return cm;
  }

  private void setColsVisible(Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> colMap,
      String uniqueIdString, boolean visible) {
    colMap.entrySet().stream()
        .filter(mapEntry -> mapEntry.getValue().getUniqueIdString().equals(uniqueIdString))
        .map(Entry::getKey).forEach(col -> col.setVisible(visible));
    featureTable.applyVisibilityParametersToAllColumns();
  }

  /**
   * Wrapper for {@link ColumnID} to make use of {@link java.util.stream.Stream#distinct()} method
   * to narrow the feature type columns to a single entry per data type. For example, without this
   * wrapper, the Feature:mz type would be added to the menu as many times as there are raw data
   * files. This class does not take the different data files into account in the
   * {@link #equals(Object)} method, but only if it is a row/feature column and the types itself.
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
