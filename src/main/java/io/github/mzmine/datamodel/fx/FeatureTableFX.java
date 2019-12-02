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

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.CommentType;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import javafx.collections.ObservableMap;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
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

  private Logger logger = Logger.getLogger(this.getClass().getName());
  Random rand = new Random(System.currentTimeMillis());

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
    setTableEditable(true);
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
   * @param data
   */
  public void addData(ModularFeatureList flist) {
    if (flist.isEmpty())
      return;

    addColumns(flist);
    TreeItem<ModularFeatureListRow> root = getRoot();
    logger.info("Add rows");
    for (ModularFeatureListRow row : flist.getRows()) {
      logger.info("Add row with id: " + row.getID());
      root.getChildren().add(new TreeItem<>(row));
    }
  }

  /**
   * Add all columns of {@link ModularFeatureListRow} data
   * 
   * @param flist a summary RowData instance with all present {@link DataType}
   */
  public void addColumns(ModularFeatureList flist) {
    logger.info("Adding columns to table");
    // for all data columns available in "data"
    flist.getRowTypes().values().forEach(dataType -> {
      addColumn(dataType, flist.getFeatureTypes());
    });
  }

  /**
   * Add a new column to the table
   * 
   * @param dataType
   * @param featureColumns
   */
  public void addColumn(DataType dataType,
      ObservableMap<Class<? extends DataType>, DataType> featureColumns) {
    // value binding
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col =
        dataType.createColumn(null, featureColumns);
    // add to table
    if (col != null)
      this.getColumns().add(col);
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
