/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.fx;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.GraphicalCellData;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * JavaFX FeatureTable based on {@link ModularFeatureListRow} and {@link DataType}
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FeatureTableFX extends TreeTableView<ModularFeatureListRow> {

  public FeatureTableFX() {
    FeatureTableFX table = this;
    // add dummy root
    TreeItem<ModularFeatureListRow> root = new TreeItem<>();
    root.setExpanded(true);
    this.setRoot(root);
    this.setShowRoot(false);
    this.setTableMenuButtonVisible(true);
    this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.getSelectionModel().setCellSelectionEnabled(true);

    // enable copy on selection
    final KeyCodeCombination keyCodeCopy =
        new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    this.setOnKeyPressed(event -> {
      if (keyCodeCopy.match(event)) {
        copySelectionToClipboard(table, true);
      }
    });
  }


  /**
   * add row data
   * 
   * @param data
   */
  public void addData(List<ModularFeatureListRow> data) {
    TreeItem<ModularFeatureListRow> root = getRoot();
    for (ModularFeatureListRow row : data) {
      root.getChildren().add(new TreeItem<>(row));
      row.getMap().addListener((
          MapChangeListener.Change<? extends Class<? extends DataType>, ? extends DataType> change) -> {

      });
    }
  }

  /**
   * Add a new column to the table
   * 
   * @param dataType
   */
  public void addColumn(DataType dataType) {
    // value binding
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col =
        new TreeTableColumn<>(dataType.getHeaderString());

    col.setCellValueFactory(r -> {
      Optional<? extends DataType> o = r.getValue().getValue().get(dataType.getClass());
      final SimpleObjectProperty<DataType<?>> property = new SimpleObjectProperty<>(o.orElse(null));
      // listen for changes in this rows DataTypeMap
      r.getValue().getValue().getMap().addListener((
          MapChangeListener.Change<? extends Class<? extends DataType>, ? extends DataType> change) -> {
        if (dataType.getClass().equals(change.getKey())) {
          property.set(r.getValue().getValue().get(dataType.getClass()).orElse(null));
        }
      });
      return property;
    });

    // value representation
    col.setCellFactory(param -> new TreeTableCell<ModularFeatureListRow, DataType<?>>() {
      @Override
      protected void updateItem(DataType<?> item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          if (item instanceof GraphicalCellData) {
            Node node = ((GraphicalCellData) item).getCellNode(this, param);
            setGraphic(node);
            setText(null);
          } else {
            setText(item.getFormattedString());
            setGraphic(null);
          }
        }
      }
    });
    // add to table
    this.getColumns().add(col);
  }

  /**
   * Add all columns of {@link ModularFeatureListRow} data
   * 
   * @param data a summary RowData instance with all present {@link DataType}
   */
  public void addColumns(ModularFeatureListRow data) {
    // for all data columns available in "data"
    for (DataType<?> dataType : data.getMap().values()) {
      addColumn(dataType);
    }
  }

  /**
   * Copy all rows of selected cells
   * 
   * @param table
   * @param addHeader
   */
  @SuppressWarnings("rawtypes")
  public void copySelectionToClipboard(final TreeTableView<ModularFeatureListRow> table, boolean addHeader) {
    final Set<Integer> rows = new TreeSet<>();
    for (final TreeTablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
      rows.add(tablePosition.getRow());
    }
    final StringBuilder strb = new StringBuilder();
    boolean firstRow = true;
    for (final Integer row : rows) {
      if (!firstRow) {
        strb.append('\n');
      } else if (addHeader) {
        for (final TreeTableColumn<ModularFeatureListRow, ?> column : table.getColumns()) {
          strb.append(column.getText());
        }
        strb.append('\n');
      }
      boolean firstCol = true;
      for (final TreeTableColumn<ModularFeatureListRow, ?> column : table.getColumns()) {
        if (!firstCol) {
          strb.append('\t');
        }
        firstCol = false;
        final Object cellData = column.getCellData(row);
        if (cellData == null)
          strb.append("");
        else if (cellData instanceof DataType<?>)
          strb.append(((DataType<?>) cellData).getFormattedString());
        else
          strb.append(cellData.toString());
      }
      firstRow = false;
    }
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(strb.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }
}
