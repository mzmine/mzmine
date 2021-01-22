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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.AreaBarType;
import io.github.mzmine.datamodel.features.types.AreaShareType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * JavaFX FeatureTable based on {@link FeatureListRow} and {@link DataType}
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FeatureTableFX extends TreeTableView<ModularFeatureListRow> {

  private final FilteredList<TreeItem<ModularFeatureListRow>> filteredRowItems;
  private final ObservableList<TreeItem<ModularFeatureListRow>> rowItems;
  // parameters
  private final ParameterSet parameters;
  private final DataTypeCheckListParameter rowTypesParameter;
  private final DataTypeCheckListParameter featureTypesParameter;
  // column map to keep track of columns
  private final Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> newColumnMap;
  Random rand = new Random(System.currentTimeMillis());
  private Logger logger = Logger.getLogger(this.getClass().getName());
  // lists
  private FeatureList flist;

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
    newColumnMap = new HashMap<>();
    initHandleDoubleClicks();
    setContextMenu(new FeatureTableContextMenu(this));
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
      /* TODO:
      if (keyCodeRandomComment.match(event)) {
        this.getSelectionModel().getSelectedItem().getValue().set(CommentType.class,
            ("Random" + rand.nextInt(100)));
        this.getSelectionModel().getSelectedItem().getValue().getFeatures().stream()
            .forEach(f -> f.set(CommentType.class, ("Random" + rand.nextInt(100))));
      }
      */
      if (keyCodeRandomMZ.match(event)) {
        assert this.getSelectionModel().getSelectedItem()
            .getValue() instanceof ModularFeatureListRow;
        ModularFeatureListRow modularRow = (ModularFeatureListRow)
            this.getSelectionModel().getSelectedItem().getValue();
        modularRow.set(MZType.class, (rand.nextDouble() * 200d));
        modularRow.streamFeatures().forEach(f -> f.setMZ(rand.nextDouble() * 200d));
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
  public void addData(FeatureList flist) {
    if (flist.isEmpty()) {
      return;
    }

    // Clear old rows
    getRoot().getChildren().clear();
    // Clear old columns
    getColumns().clear();

    this.flist = flist;

    addColumns(flist);

    // add rows
    TreeItem<ModularFeatureListRow> root = getRoot();
//    logger.info("Add rows");
    for (FeatureListRow row : flist.getRows()) {
      ModularFeatureListRow mrow = (ModularFeatureListRow) row;
//      logger.info("Add row with id: " + row.getID());
      root.getChildren().add(new TreeItem<>(mrow));
    }

    rowItems.addAll(root.getChildren());
  }

  /**
   * Add all columns of {@link FeatureListRow} data
   *
   * @param flist a summary RowData instance with all present {@link DataType}
   */
  public void addColumns(FeatureList flist) {
//    logger.info("Adding columns to table");
    // for all data columns available in "data"
    assert flist instanceof ModularFeatureList : "Feature list is not modular";
    ModularFeatureList featureList = (ModularFeatureList) flist;
    // add row types
    featureList.getRowTypes().values().stream()
        .filter(t -> !(t instanceof FeaturesType)).forEach(this::addColumn);
    // add features
    if (featureList.getRowTypes().containsKey(FeaturesType.class)) {
      addColumn(featureList.getRowTypes().get(FeaturesType.class));
    }
  }

  /**
   * Add a new column to the table
   *
   * @param dataType
   */
  public void addColumn(DataType dataType) {
    // Is feature type?
    if (dataType.getClass().equals(FeaturesType.class)) {
      addFeaturesColumns();
    } else {
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> col = dataType
          .createColumn(null, null);
      if (col == null) {
        return;
      }

      if (dataType instanceof ExpandableType) {
        setupExpandableColumn(dataType, col, ColumnType.ROW_TYPE, null);
      }

      // Add column
      this.getColumns().add(col);
      newColumnMap.put(col, new ColumnID(dataType, ColumnType.ROW_TYPE, null));
      if (!(dataType instanceof ExpandableType)) {
        // Hide area bars and area share columns, if there is only one raw data file in the feature list
        if ((dataType instanceof AreaBarType || dataType instanceof AreaShareType)
            && flist.getNumberOfRawDataFiles() == 1) {
          col.setVisible(false);
        } else {
          applyVisibilityParameterToColumn(col);
        }
      }
    }
  }

  private void setupExpandableColumn(DataType<?> dataType,
      TreeTableColumn<ModularFeatureListRow, ?> col,
      ColumnType colType, RawDataFile dataFile) {
    // Initialize buddy(expanded/hidden for hidden/expanded respectively) column and it's data type
    TreeTableColumn<ModularFeatureListRow, ?> buddyCol = null;
    DataType<?> buddyDataType = null;
    // Find column's buddy
    for (Entry<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> entry : newColumnMap
        .entrySet()) {
      if (Objects.equals(entry.getValue().getDataType().getClass(),
          ((ExpandableType) dataType).getBuddyTypeClass())
          && Objects.equals(entry.getValue().getType(), colType)
          && Objects.equals(entry.getValue().getRaw(), dataFile)) {
        buddyCol = entry.getKey();
        buddyDataType = entry.getValue().getDataType();
      }
    }

    // If buddyCol == null, then only one of the two buddy columns was initialized(so, do nothing)
    if (buddyCol == null) {
      return;
    }

    // Set expanding headers
    col.setGraphic(createExpandableHeader(dataType, col, buddyCol));
    buddyCol.setGraphic(createExpandableHeader(buddyDataType, buddyCol, col));

    // Set columns visibility state
    if (((ExpandableType) dataType).isExpandedType()) {
      col.setVisible(false);
    } else {
      buddyCol.setVisible(false);
    }
  }

  Node createExpandableHeader(DataType<?> dataType, TreeTableColumn<ModularFeatureListRow, ?> col,
      TreeTableColumn<ModularFeatureListRow, ?> buddyCol) {
    // Create labels to process mouse click event(text for sorting, button for expanding)
    Label headerText = new Label(dataType.getHeaderString());

    Label headerButton = new Label(" " + ((ExpandableType) dataType).getSymbol() + " ");
    headerButton.setTextFill(Color.rgb(80, 80, 80));

    HBox headerLabel = new HBox(headerButton, headerText);
    headerLabel.setAlignment(Pos.CENTER);

    // Define mouse click behaviour
    headerButton.setOnMousePressed(event -> {
      boolean visible = col.isVisible();
      col.setVisible(!visible);
      buddyCol.setVisible(visible);
    });

    // Add labels to the column headers
    col.setGraphic(headerLabel);
    col.setText("");

    return headerLabel;
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
    // for (final TreeTableColumn<FeatureListRow, ?> column : table.getColumns()) {
    // strb.append(column.getText());
    // }
    // strb.append('\n');
    // }
    // boolean firstCol = true;
    // for (final TreeTableColumn<FeatureListRow, ?> column : table.getColumns()) {
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
  public FeatureList getFeatureList() {
    return flist;
  }

  @Nonnull
  public FilteredList<TreeItem<ModularFeatureListRow>> getFilteredRowItems() {
    return filteredRowItems;
  }

  /**
   * Sets visibility of all data type columns.
   *
   * @param rowVisibilityMap     Map containing row types names and their visibility values
   * @param featureVisibilityMap Map containing feature types names and their visibility values
   */
  protected void updateColumnsVisibilityParameters(Map<String, Boolean> rowVisibilityMap,
      Map<String, Boolean> featureVisibilityMap) {
    if (flist == null) {
      return;
    }

    // Update visibility parameters
    rowTypesParameter.setDataTypesAndVisibility(rowVisibilityMap);
    featureTypesParameter.setDataTypesAndVisibility(featureVisibilityMap);
    applyVisibilityParametersToAllColumns();
  }

  private void applyVisibilityParameterToColumn(TreeTableColumn column) {
    ColumnID id = newColumnMap.get(column);
    if (id == null) {
      return;
    }

    if (id.getType() == ColumnType.ROW_TYPE) {
      column.setVisible(rowTypesParameter.isDataTypeVisible(id.getDataType()));
    } else {
      column.setVisible(featureTypesParameter.isDataTypeVisible(id.getDataType()));
    }
  }

  private void applyVisibilityParametersToAllColumns() {
    this.getColumns().forEach(this::applyVisibilityParameterToColumn);
  }

  private void addFeaturesColumns() {
    // Add feature columns for each raw file
    for (RawDataFile dataFile : flist.getRawDataFiles()) {
      TreeTableColumn<ModularFeatureListRow, String> sampleCol =
          new TreeTableColumn<>();

      // Add raw data file label
      Label headerLabel = new Label(dataFile.getName());
      headerLabel.setTextFill(dataFile.getColor());
      headerLabel.setGraphic(new ImageView("icons/fileicon.png"));
      sampleCol.setGraphic(headerLabel);

      // Add sub columns of feature
      for (DataType ftype : ((ModularFeatureList) flist).getFeatureTypes().values()) {
        TreeTableColumn<ModularFeatureListRow, ?> subCol = ftype.createColumn(dataFile, null);
        if (subCol != null) {
          if (ftype instanceof ExpandableType) {
            setupExpandableColumn(ftype, subCol, ColumnType.FEATURE_TYPE, dataFile);
          }
          sampleCol.getColumns().add(subCol);
          newColumnMap.put(subCol, new ColumnID(ftype, ColumnType.FEATURE_TYPE, dataFile));
          if (!(ftype instanceof ExpandableType)) {
            applyVisibilityParameterToColumn(subCol);
          }
        }
      }
      // Add sample column
      // NOTE: sample column is not added to the columnMap
      this.getColumns().add(sampleCol);
    }
  }

  private void initHandleDoubleClicks() {
    this.setOnMouseClicked(e -> {
      if (e.getClickCount() >= 2) {
        e.consume();
        logger.finest(() -> "Double click on " + e.getSource());

        TreeTablePosition<ModularFeatureListRow, ?> focusedCell = getFocusModel().getFocusedCell();
        TreeTableColumn<ModularFeatureListRow, ?> tableColumn = focusedCell.getTableColumn();
        Object userData = tableColumn.getUserData();

        if (userData instanceof DataType<?>) {
          List<RawDataFile> files = new ArrayList<>();
          ColumnID id = newColumnMap.get(tableColumn);
          if (id == null) {
            return;
          }
          if (id.getType() == ColumnType.ROW_TYPE) {
            files.addAll(getFeatureList().getRawDataFiles());
          } else {
            RawDataFile file = id.getRaw();
            if (file != null) {
              files.add(file);
            }
          }

          ModularFeatureListRow row = (ModularFeatureListRow) getSelectionModel().getSelectedItem()
              .getValue();
          Runnable runnable = ((DataType<?>) userData).getDoubleClickAction(row, files);
          if (runnable != null) {
            runnable.run();
          }
        }
      }
    });
  }

  public List<ModularFeatureListRow> getSelectedRows() {
    return getSelectionModel().getSelectedItems().stream().map(item -> item.getValue())
        .collect(Collectors.toList());
  }

  /**
   * @return A set of selected data types. Does not contain duplicates if multiple cells of the same
   * type were selected. Does not contain null.
   */
  public Set<DataType<?>> getSelectedDataTypes(@Nonnull ColumnType columnType) {
    ObservableList<TreeTablePosition<ModularFeatureListRow, ?>> selectedCells = getSelectionModel()
        .getSelectedCells();

    // HashSet so we don't have to bother with duplicates.
    Set<DataType<?>> dataTypes = new HashSet<>();
    selectedCells.forEach(cell -> {
      ColumnID columnID = newColumnMap.get(cell.getTableColumn());
      if (columnID != null && columnID.getType() == columnType) {
        dataTypes.add(columnID.getDataType());
      }
    });
    return Collections.unmodifiableSet(dataTypes);
  }

  /**
   * @return A set of selected data types. Does not contain duplicates if multiple cells of the same
   * file were selected. Does not contain null.
   */
  public Set<RawDataFile> getSelectedRawDataFiles() {
    ObservableList<TreeTablePosition<ModularFeatureListRow, ?>> selectedCells = getSelectionModel()
        .getSelectedCells();

    // HashSet so we don't have to bother with duplicates.
    Set<RawDataFile> rawDataFiles = new HashSet<>();
    selectedCells.forEach(cell -> {
      ColumnID columnID = newColumnMap.get(cell.getTableColumn());
      if (columnID != null && columnID.getType() == ColumnType.FEATURE_TYPE) {
        rawDataFiles.add(columnID.getRaw());
      }
    });
    return Collections.unmodifiableSet(rawDataFiles);
  }

  /**
   * @return A list of the selected features.
   */
  public List<ModularFeature> getSelectedFeatures() {
    ObservableList<TreeTablePosition<ModularFeatureListRow, ?>> selectedCells = getSelectionModel()
        .getSelectedCells();

    List<ModularFeature> features = new ArrayList<>();
    selectedCells.forEach(cell -> {
      // get file of the selected column
      ColumnID id = newColumnMap.get(cell.getTableColumn());
      if (id != null) {
        RawDataFile file = id.getRaw();
        ModularFeature feature = cell.getTreeItem().getValue().getFeature(file);
        if (feature != null) {
          features.add(feature);
        }
      }
    });
    return Collections.unmodifiableList(features);
  }

  @Nullable
  public ModularFeature getSelectedFeature() {
    TreeTablePosition<ModularFeatureListRow, ?> focusedCell = getFocusModel().getFocusedCell();
    if (focusedCell == null) {
      return null;
    }
    ColumnID id = newColumnMap.get(focusedCell.getTableColumn());
    if (id != null && id.getRaw() != null) {
      return focusedCell.getTreeItem().getValue().getFeature(id.getRaw());
    }
    return null;
  }
}
