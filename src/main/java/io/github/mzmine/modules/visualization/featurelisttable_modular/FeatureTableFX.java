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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.CommentType;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.fx.ColumnID;
import io.github.mzmine.datamodel.data.types.fx.ColumnType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javax.annotation.Nullable;

/**
 * JavaFX FeatureTable based on {@link ModularFeatureListRow} and {@link DataType}
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FeatureTableFX extends TreeTableView<ModularFeatureListRow> {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  Random rand = new Random(System.currentTimeMillis());

  // lists
  private ModularFeatureList flist;
  private final FilteredList<TreeItem<ModularFeatureListRow>> filteredRowItems;
  private final ObservableList<TreeItem<ModularFeatureListRow>> rowItems;

  // parameters
  private final ParameterSet parameters;
  private final DataTypeCheckListParameter rowTypesParameter;
  private final DataTypeCheckListParameter featureTypesParameter;

  // column map to keep track of columns
  private final Map<ColumnID, TreeTableColumn> columnMap;

  public FeatureTableFX() {
    // add dummy root
    TreeItem<ModularFeatureListRow> root = new TreeItem<>();
    root.setExpanded(true);
    this.setRoot(root);
    this.setShowRoot(false);
    this.setTableMenuButtonVisible(true);
    this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.getSelectionModel().setCellSelectionEnabled(true);
    setTableEditable(true);

    /*getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)  -> {
        int io = getRoot().getChildren().indexOf(oldValue);
        int in = getRoot().getChildren().indexOf(newValue);
    });*/

    parameters = MZmineCore.getConfiguration().getModuleParameters(FeatureTableFXModule.class);
    rowTypesParameter = parameters.getParameter(FeatureTableFXParameters.showRowTypeColumns);
    featureTypesParameter = parameters
        .getParameter(FeatureTableFXParameters.showFeatureTypeColumns);

    rowItems = FXCollections.observableArrayList();
    filteredRowItems = new FilteredList<>(rowItems);
    columnMap = new HashMap<>();
  }

  private void setTableEditable(boolean state) {
    this.setEditable(true);// when character or numbers pressed it will start edit in editable
    // fields

    // enable copy on selection
    final KeyCodeCombination keyCodeCopy =
        new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    final KeyCodeCombination keyCodeRandomComment =
        new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_ANY);
    final KeyCodeCombination keyCodeRandomMZ =
        new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_ANY);

    this.setOnKeyPressed(event -> {
      if (keyCodeCopy.match(event)) {
        copySelectionToClipboard(this, true);
      }
      if (keyCodeRandomComment.match(event)) {
        this.getSelectionModel().getSelectedItem().getValue().set(CommentType.class,
            ("Random" + rand.nextInt(100)));
        this.getSelectionModel().getSelectedItem().getValue().getFeatures().values().stream()
            .forEach(f -> f.set(CommentType.class, ("Random" + rand.nextInt(100))));
      }
      if (keyCodeRandomMZ.match(event)) {
        this.getSelectionModel().getSelectedItem().getValue().set(MZType.class,
            (rand.nextDouble() * 200d));
        this.getSelectionModel().getSelectedItem().getValue().getFeatures().values().stream()
            .forEach(f -> f.set(MZType.class, (rand.nextDouble() * 200d)));
      }

      if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
        editFocusedCell();
      } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.TAB) {
        this.getSelectionModel().selectNext();
        event.consume();
      } else if (event.getCode() == KeyCode.LEFT) {
        this.getSelectionModel().selectPrevious();
        // work around due to
        // TableView.getSelectionModel().selectPrevious() due to a bug
        // stopping it from working on
        // the first column in the last row of the table
        // selectPrevious();
        event.consume();
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void editFocusedCell() {
    TreeTablePosition<ModularFeatureListRow, ?> focusedCell =
        this.focusModelProperty().get().focusedCellProperty().get();
    this.edit(focusedCell.getRow(), focusedCell.getTableColumn());
  }


  /**
   * add row data and columns of first row
   *
   * @param flist
   */
  public void addData(ModularFeatureList flist) {
    if (flist.isEmpty()) {
      return;
    }
    this.flist = flist;

    addColumns(flist);

    // add rows
    TreeItem<ModularFeatureListRow> root = getRoot();
//    logger.info("Add rows");
    for (ModularFeatureListRow row : flist.getRows()) {
//      logger.info("Add row with id: " + row.getID());
      root.getChildren().add(new TreeItem<>(row));
    }

    rowItems.addAll(root.getChildren());
  }

  /**
   * Add all columns of {@link ModularFeatureListRow} data
   *
   * @param flist a summary RowData instance with all present {@link DataType}
   */
  public void addColumns(ModularFeatureList flist) {
//    logger.info("Adding columns to table");
    // for all data columns available in "data"
    flist.getRowTypes().values().forEach(dataType -> {
      addColumn(dataType);
    });
  }

  /**
   * Add a new column to the table
   *
   * @param dataType
   */
  public void addColumn(DataType dataType) {
    // value binding
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col = dataType.createColumn(null);
    // add to table
    if (col != null) {
      this.getColumns().add(col);
      columnMap.put(new ColumnID(dataType, ColumnType.ROW_TYPE, null), col);
      rowTypesParameter.isDataTypeVisible(dataType);
      // is feature type?
      if (dataType.getClass().equals(FeaturesType.class)) {
        // add feature columns for each raw file
        for (RawDataFile raw : flist.getRawDataFiles()) {
          // create column per name
          TreeTableColumn<ModularFeatureListRow, String> sampleCol =
              new TreeTableColumn<>(raw.getName());
          // TODO get RawDataFile -> Color and set column header
          // sampleCol.setStyle("-fx-background-color: #"+ColorsFX.getHexString(color));
          // add sub columns of feature
          for (DataType ftype : flist.getFeatureTypes().values()) {
            TreeTableColumn<ModularFeatureListRow, ?> subCol = ftype.createColumn(raw);
            if (subCol != null) {
              sampleCol.getColumns().add(subCol);
              columnMap.put(new ColumnID(ftype, ColumnType.FEATURE_TYPE, raw), subCol);
              featureTypesParameter.isDataTypeVisible(ftype);
            }
          }
          // add all
          col.getColumns().add(sampleCol);
        }
      }
    }
    applyColumnVisibility();
  }


  /**
   * Copy all rows of selected cells
   *
   * @param table
   * @param addHeader
   */
  @SuppressWarnings("rawtypes")
  public void copySelectionToClipboard(final TreeTableView<ModularFeatureListRow> table,
      boolean addHeader) {
    // final Set<Integer> rows = new TreeSet<>();
    // for (final TreeTablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
    // rows.add(tablePosition.getRow());
    // }
    // final StringBuilder strb = new StringBuilder();
    // boolean firstRow = true;
    // for (final Integer row : rows) {
    // if (!firstRow) {
    // strb.append('\n');
    // } else if (addHeader) {
    // for (final TreeTableColumn<ModularFeatureListRow, ?> column : table.getColumns()) {
    // strb.append(column.getText());
    // }
    // strb.append('\n');
    // }
    // boolean firstCol = true;
    // for (final TreeTableColumn<ModularFeatureListRow, ?> column : table.getColumns()) {
    // if (!firstCol) {
    // strb.append('\t');
    // }
    // firstCol = false;
    // final Object cellData = column.getCellData(row);
    // if (cellData == null)
    // strb.append("");
    // else if (cellData instanceof DataType<?>)
    // strb.append(((DataType<?>) cellData).getFormattedString(cellData));
    // else
    // strb.append(cellData.toString());
    // }
    // firstRow = false;
    // }
    // final ClipboardContent clipboardContent = new ClipboardContent();
    // clipboardContent.putString(strb.toString());
    // Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  @Nullable
  public ModularFeatureList getFeatureList() {
    return flist;
  }

  @Nullable
  public FilteredList<TreeItem<ModularFeatureListRow>> getFilteredRowItems() {
    return filteredRowItems;
  }

  @Nullable
  private TreeTableColumn getColumn(ColumnID id) {
    if (!columnMap.containsKey(id)) {
      logger.info(id.getFormattedString());
    }
    return columnMap.get(id);
  }

  /**
   * Sets visibility of DataType columns depending on the parameter values. Uses columnMap and
   * ColumnID.
   */
  protected void applyColumnVisibility() {
    if (flist == null) {
      return;
    }

    for (DataType dataType : flist.getRowTypes().values()) {
      TreeTableColumn col = columnMap.get(new ColumnID(dataType, ColumnType.ROW_TYPE, null));
      if (col != null) {
        col.setVisible(rowTypesParameter.isDataTypeVisible(dataType));
      }
    }

    for (RawDataFile raw : flist.getRawDataFiles()) {
      for (DataType dataType : flist.getFeatureTypes().values()) {
        TreeTableColumn col = columnMap.get(new ColumnID(dataType, ColumnType.FEATURE_TYPE, raw));
        if (col != null) {
          col.setVisible(featureTypesParameter.isDataTypeVisible(dataType));
        }
      }
    }
  }
}
