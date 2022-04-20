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

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.AreaBarType;
import io.github.mzmine.datamodel.features.types.AreaShareType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.FeatureShapeIonMobilityRetentionTimeHeatMapType;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.RdbeType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.ConsensusFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaMassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.formula.SimpleFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonNetworkIDType;
import io.github.mzmine.datamodel.features.types.annotations.iin.MsMsMultimerVerifiedType;
import io.github.mzmine.datamodel.features.types.annotations.iin.PartnerIdsType;
import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.SizeType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberRangeType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CombinedScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CosineScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MsMsScoreType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JavaFX FeatureTable based on {@link FeatureListRow} and {@link DataType}
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FeatureTableFX extends TreeTableView<ModularFeatureListRow> implements
    ListChangeListener<FeatureListRow> {

  private static final Logger logger = Logger.getLogger(FeatureTableFX.class.getName());
  private final FilteredList<TreeItem<ModularFeatureListRow>> filteredRowItems;
  private final ObservableList<TreeItem<ModularFeatureListRow>> rowItems;
  // parameters
  private final ParameterSet parameters;
  private final DataTypeCheckListParameter rowTypesParameter;
  private final DataTypeCheckListParameter featureTypesParameter;

  // column map to keep track of columns
  private final Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> newColumnMap;
  private final ObjectProperty<ModularFeatureList> featureListProperty = new SimpleObjectProperty<>();

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

    initFeatureListListener();

    parameters = MZmineCore.getConfiguration().getModuleParameters(FeatureTableFXModule.class);
    rowTypesParameter = parameters.getParameter(FeatureTableFXParameters.showRowTypeColumns);
    featureTypesParameter = parameters.getParameter(
        FeatureTableFXParameters.showFeatureTypeColumns);

    rowItems = FXCollections.observableArrayList();
    filteredRowItems = new FilteredList<>(rowItems);
    newColumnMap = new HashMap<>();
    initHandleDoubleClicks();
    setContextMenu(new FeatureTableContextMenu(this));

    // create custom button context menu to select columns
    FeatureTableColumnMenuHelper contextMenuHelper = new FeatureTableColumnMenuHelper(this);
    // Adding additional menu options
    addContextMenuItem(contextMenuHelper, "Compact LC/GC-MS",
        e -> showCompactChromatographyColumns());
    addContextMenuItem(contextMenuHelper, "Toggle Ion Identities", e -> toggleIonIdentities());
    addContextMenuItem(contextMenuHelper, "Toggle Library Matches", e -> toggleLibraryMatches());

    final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C,
        KeyCombination.CONTROL_ANY);
    setOnKeyPressed(event -> {
      if (keyCodeCopy.match(event)) {
        copySelectionToClipboard(this);
      }
    });
  }

  private void toggleIonIdentities() {
    final var columnEntry = getColumnEntry(IonIdentityListType.class);
    if (columnEntry == null) {
      return;
    }

    final ColumnID mainColumn = columnEntry.getValue();
    final boolean toggledState = !rowTypesParameter.isDataTypeVisible(mainColumn);
    rowTypesParameter.setDataTypeVisible(mainColumn, toggledState);
    final String parentHeader = mainColumn.getCombinedHeaderString();

    // basic
    setVisible(ColumnType.ROW_TYPE, parentHeader, IonNetworkIDType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, IonIdentityListType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, SizeType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, NeutralMassType.class, toggledState);

    // always false
    setVisible(ColumnType.ROW_TYPE, parentHeader, PartnerIdsType.class, false);
    setVisible(ColumnType.ROW_TYPE, parentHeader, MsMsMultimerVerifiedType.class, false);

    // formula
    final ModularFeatureList flist = getFeatureList();
    boolean hasFormula =
        flist != null && flist.stream().flatMap(row -> row.getIonIdentities().stream())
            .anyMatch(ion -> ion.getMolFormulas().size() > 0);
    setVisible(ColumnType.ROW_TYPE, parentHeader, ConsensusFormulaListType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, SimpleFormulaListType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, FormulaMassType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, RdbeType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, MZType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, MzPpmDifferenceType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, MzAbsoluteDifferenceType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, IsotopePatternScoreType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, MsMsScoreType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentHeader, CombinedScoreType.class,
        toggledState && hasFormula);
    //    for(var subCol : columnEntry.getKey().getColumns()) {
    //      final ColumnID columnID = newColumnMap.get(subCol);
    //      rowTypesParameter.setDataTypeVisible(columnID, toggledState);
    //    }
    // keep states of all row types but check graphical columns
    applyVisibilityParametersToAllColumns();
  }

  private void toggleLibraryMatches() {
    final var columnEntry = getColumnEntry(SpectralLibraryMatchesType.class);
    if (columnEntry == null) {
      return;
    }

    final ColumnID mainColumn = columnEntry.getValue();
    final boolean toggledState = !rowTypesParameter.isDataTypeVisible(mainColumn);
    rowTypesParameter.setDataTypeVisible(mainColumn, toggledState);
    final String parentHeader = mainColumn.getCombinedHeaderString();

    // basic
    setVisible(ColumnType.ROW_TYPE, parentHeader, SpectralLibraryMatchesType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, IonAdductType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, FormulaType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, SmilesStructureType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, InChIStructureType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, PrecursorMZType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, NeutralMassType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, CosineScoreType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentHeader, MatchingSignalsType.class, toggledState);

    // always false
    setVisible(ColumnType.ROW_TYPE, parentHeader, CompoundNameType.class, false);
    setVisible(ColumnType.ROW_TYPE, parentHeader, CCSType.class, false);
    setVisible(ColumnType.ROW_TYPE, parentHeader, CCSRelativeErrorType.class, false);

    applyVisibilityParametersToAllColumns();
  }

  private void addContextMenuItem(FeatureTableColumnMenuHelper contextMenuHelper, String title,
      EventHandler<ActionEvent> action) {
    MenuItem item = new MenuItem(title);
    item.setOnAction(action);
    contextMenuHelper.getAdditionalMenuItems().add(item);
  }

  private void setTableEditable(boolean state) {
    this.setEditable(true);// when character or numbers pressed it will start edit in editable
    // fields

    this.setOnKeyPressed(event -> {
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
    TreeTablePosition<ModularFeatureListRow, ?> focusedCell = this.focusModelProperty().get()
        .focusedCellProperty().get();
    this.edit(focusedCell.getRow(), focusedCell.getTableColumn());
  }


  /**
   * Listens to update the table if a row is added/removed to/from the feature list.
   */
  public void onChanged(final Change<? extends FeatureListRow> c) {
    c.next();
    if (!(c.wasAdded() || c.wasRemoved())) {
      return;
    }

    MZmineCore.runLater(() -> {
      getRoot().getChildren().clear();
      rowItems.clear();
      // add rows
      for (FeatureListRow row : featureListProperty.get().getRows()) {
        final ModularFeatureListRow mrow = (ModularFeatureListRow) row;
        rowItems.add(new TreeItem<>(mrow));
      }
      getRoot().getChildren().addAll(filteredRowItems);
      this.sort();
    });
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
    featureList.getRowTypes().values().stream().filter(t -> !(t instanceof FeaturesType))
        .forEach(this::addColumn);
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
    if (getFeatureList() == null) {
      return;
    }

    // Is feature type?
    if (dataType.getClass().equals(FeaturesType.class)) {
      addFeaturesColumns();
    } else {
      var col = dataType.createColumn(null, null);
      if (col == null) {
        return;
      }

      if (dataType instanceof ExpandableType) {
        setupExpandableColumn(dataType, col, ColumnType.ROW_TYPE, null);
      }

      // Add column
      this.getColumns().add(col);

      registerColumn(col, ColumnType.ROW_TYPE, dataType, null);
      if (!(dataType instanceof ExpandableType)) {
        // Hide area bars and area share columns, if there is only one raw data file in the feature list
        if ((dataType instanceof AreaBarType || dataType instanceof AreaShareType)
            && getFeatureList().getNumberOfRawDataFiles() == 1) {
          col.setVisible(false);
        } else {
          recursivelyApplyVisibilityParameterToColumn(col);
        }
      }
    }
  }

  private Entry<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> getColumnEntry(
      Class<? extends DataType> dtClass) {
    for (var col : newColumnMap.entrySet()) {
      if (col.getValue().getDataType().getClass().equals(dtClass)) {
        return col;
      }
    }
    return null;
  }

  /**
   * Registers a data type column and all it's sub colums to the {@link
   * FeatureTableFX#newColumnMap}.
   */
  private void registerColumn(@NotNull TreeTableColumn<ModularFeatureListRow, ?> column,
      @NotNull ColumnType type, @NotNull DataType<?> dataType, @Nullable RawDataFile file) {
    newColumnMap.put(column, new ColumnID(dataType, type, file, -1));

    // add all sub columns to the list (not for range types - no need to only show one)
    // the main data type is set to subcolumns as data type.
    if (dataType instanceof SubColumnsFactory && !column.getColumns().isEmpty()
        && !(dataType instanceof NumberRangeType)) {
      int i = 0;
      for (TreeTableColumn<ModularFeatureListRow, ?> subCol : column.getColumns()) {
        newColumnMap.put(subCol, new ColumnID(dataType, type, file, i));
        i++;
      }
    }
  }

  private void setupExpandableColumn(DataType<?> dataType,
      TreeTableColumn<ModularFeatureListRow, ?> col, ColumnType colType, RawDataFile dataFile) {
    // Initialize buddy(expanded/hidden for hidden/expanded respectively) column and it's data type
    TreeTableColumn<ModularFeatureListRow, ?> buddyCol = null;
    DataType<?> buddyDataType = null;
    // Find column's buddy
    for (Entry<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> entry : newColumnMap.entrySet()) {
      if (Objects.equals(entry.getValue().getDataType().getClass(),
          ((ExpandableType) dataType).getBuddyTypeClass()) && Objects.equals(
          entry.getValue().getType(), colType) && Objects.equals(entry.getValue().getRaw(),
          dataFile)) {
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

  @NotNull
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
    if (getFeatureList() == null) {
      return;
    }

    // Update visibility parameters
    rowTypesParameter.setDataTypesAndVisibility(rowVisibilityMap);
    featureTypesParameter.setDataTypesAndVisibility(featureVisibilityMap);
    applyVisibilityParametersToAllColumns();
  }

  /**
   * Applies visibility settings to the column and it's child columns.
   *
   * @param column
   */
  private void recursivelyApplyVisibilityParameterToColumn(TreeTableColumn column) {
    ColumnID id = newColumnMap.get(column);
    column.getColumns()
        .forEach(col -> recursivelyApplyVisibilityParameterToColumn((TreeTableColumn) col));

    if (id == null) {
      return;
    }

    if (id.getType() == ColumnType.FEATURE_TYPE) {
      column.setVisible(featureTypesParameter.isDataTypeVisible(id));
    } else {
      column.setVisible(rowTypesParameter.isDataTypeVisible(id));
    }
  }

  public void applyVisibilityParametersToAllColumns() {
    this.getColumns().forEach(this::recursivelyApplyVisibilityParameterToColumn);
  }

  private void addFeaturesColumns() {
    if (getFeatureList() == null) {
      return;
    }

    // Add feature columns for each raw file
    for (RawDataFile dataFile : getFeatureList().getRawDataFiles()) {
      TreeTableColumn<ModularFeatureListRow, String> sampleCol = new TreeTableColumn<>();

      // Add raw data file label
      Label headerLabel = new Label(dataFile.getName());
      headerLabel.setTextFill(dataFile.getColor());
      headerLabel.setGraphic(new ImageView(FxIconUtil.getFileIcon(dataFile.getColor())));
      sampleCol.setGraphic(headerLabel);

      // Add sub columns of feature
      for (DataType ftype : getFeatureList().getFeatureTypes().values()) {
        if (ftype instanceof ImageType && !(dataFile instanceof ImagingRawDataFile)) {
          // non-imaging files don't need a image column
          continue;
        } else if (ftype instanceof FeatureShapeIonMobilityRetentionTimeHeatMapType && (
            !(dataFile instanceof IMSRawDataFile) || dataFile instanceof ImagingRawDataFile)) {
          // non ims files or ims-imaging files don't need a ims trace column
          continue;
        }

        TreeTableColumn<ModularFeatureListRow, ?> subCol = ftype.createColumn(dataFile, null);
        if (subCol != null) {
          if (ftype instanceof ExpandableType) {
            setupExpandableColumn(ftype, subCol, ColumnType.FEATURE_TYPE, dataFile);
          }
          sampleCol.getColumns().add(subCol);
          registerColumn(subCol, ColumnType.FEATURE_TYPE, ftype, dataFile);
          //          newColumnMap.put(subCol, new ColumnID(ftype, ColumnType.FEATURE_TYPE, dataFile));
          if (!(ftype instanceof ExpandableType)) {
            recursivelyApplyVisibilityParameterToColumn(subCol);
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
      if (e.getClickCount() >= 2 && e.getButton() == MouseButton.PRIMARY) {
        if (getFeatureList() == null) {
          return;
        }

        e.consume();
        logger.finest(() -> "Double click on " + e.getSource());

        TreeTablePosition<ModularFeatureListRow, ?> focusedCell = getFocusModel().getFocusedCell();
        TreeTableColumn<ModularFeatureListRow, ?> tableColumn = focusedCell.getTableColumn();
        if (tableColumn == null) {
          // double click on header (happens when sorting)
          return;
        }
        Object userData = tableColumn.getUserData();
        final ObservableValue<?> observableValue = tableColumn.getCellObservableValue(
            focusedCell.getTreeItem());
        final Object cellValue = observableValue.getValue();

        if (userData instanceof DataType<?> dataType) {
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

          // if the data type is equal to the super type, it's not a subcolumn. If it's not equal,
          // it's a subcolumn.
          final DataType<?> superDataType =
              id.getDataType().equals(dataType) ? null : id.getDataType();

          final ModularFeatureListRow row = getSelectionModel().getSelectedItem().getValue();
          final Runnable runnable = (dataType.getDoubleClickAction(row, files, superDataType,
              cellValue));
          if (runnable != null) {
            MZmineCore.getTaskController().addTask(
                new FeatureTableDoubleClickTask(runnable, getFeatureList(),
                    (DataType<?>) userData));
          }
        }
      }
    });
  }

  public List<ModularFeatureListRow> getSelectedRows() {
    return getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue)
        .collect(Collectors.toList());
  }

  @Nullable
  public ModularFeatureListRow getSelectedRow() {
    return getSelectionModel().getSelectedItem() != null ? getSelectionModel().getSelectedItem()
        .getValue() : null;
  }

  /**
   * @return A set of selected data types. Does not contain duplicates if multiple cells of the same
   * type were selected. Does not contain null.
   */
  public Set<DataType<?>> getSelectedDataTypes(@NotNull ColumnType columnType) {
    ObservableList<TreeTablePosition<ModularFeatureListRow, ?>> selectedCells = getSelectionModel().getSelectedCells();

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
    ObservableList<TreeTablePosition<ModularFeatureListRow, ?>> selectedCells = getSelectionModel().getSelectedCells();

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
    ObservableList<TreeTablePosition<ModularFeatureListRow, ?>> selectedCells = getSelectionModel().getSelectedCells();

    // HashSet so we don't have to bother with duplicates.
    Set<ModularFeature> features = new LinkedHashSet<>();
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
    return Collections.unmodifiableList(new ArrayList<>(features));
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

  @Nullable
  public ModularFeatureList getFeatureList() {
    return featureListProperty.get();
  }

  public void setFeatureList(ModularFeatureList featureList) {
    this.featureListProperty.set(featureList);
  }

  public ObjectProperty<ModularFeatureList> featureListProperty() {
    return featureListProperty;
  }

  /**
   * Initialises a listener to update the tables' contents to the current feature list. Also adds
   * and removes the row changed listener.
   */
  private void initFeatureListListener() {
    featureListProperty().addListener((observable, oldValue, newValue) -> {
      MZmineCore.runLater(() -> {
        // Clear old rows and old columns
        getRoot().getChildren().clear();
        getColumns().clear();
        rowItems.clear();

        // remove the old listener
        if (oldValue != null) {
          oldValue.getRows().removeListener(this);
        }
        addColumns(newValue);
        // first check if feature list is too large
        if (newValue.getNumberOfRawDataFiles() > 10) {
          showCompactChromatographyColumns();
        }

        // add rows
        for (FeatureListRow row : newValue.getRows()) {
          final ModularFeatureListRow mrow = (ModularFeatureListRow) row;
          rowItems.add(new TreeItem<>(mrow));
        }

        TreeItem<ModularFeatureListRow> root = getRoot();
        root.getChildren().addAll(filteredRowItems);

        // reflect the changes to the feature list in the table
        newValue.getRows().addListener(this);
      });
    });
  }

  private void showCompactChromatographyColumns() {
    // disable all feature types but height
    featureTypesParameter.setAll(false);
    setVisible(ColumnType.FEATURE_TYPE, HeightType.class, true);

    // keep states of all row types but check graphical columns
    setVisible(ColumnType.ROW_TYPE, FeatureShapeType.class,
        getFeatureList().getNumberOfRawDataFiles() <= 10);
    setVisible(ColumnType.ROW_TYPE, FeaturesType.class, true);

    applyVisibilityParametersToAllColumns();
  }

  private void setVisible(ColumnType columnType, Class clazz, boolean visible) {
    setVisible(columnType, "", clazz, visible);
  }

  private void setVisible(ColumnType columnType, String parentHeader, Class clazz,
      boolean visible) {
    final DataType type = DataTypes.get(clazz);
    String key = (parentHeader != null && parentHeader.isBlank() ? parentHeader + ":" : "")
        + type.getHeaderString();
    if (columnType == ColumnType.ROW_TYPE) {
      rowTypesParameter.setDataTypeVisible(key, visible);
    } else {
      featureTypesParameter.setDataTypeVisible("Feature:" + type.getHeaderString(), visible);
    }
  }

  public void closeTable() {
    final ModularFeatureList flist = featureListProperty.get();
    if (flist == null) {
      return;
    }
    flist.getRows().removeListener(this);
    flist.modularStream().forEach(ModularFeatureListRow::clearBufferedColCharts);
    flist.streamFeatures().forEach(ModularFeature::clearBufferedColCharts);
  }

  public DataTypeCheckListParameter getRowTypesParameter() {
    return rowTypesParameter;
  }

  public DataTypeCheckListParameter getFeatureTypesParameter() {
    return featureTypesParameter;
  }

  public Map<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> getNewColumnMap() {
    return newColumnMap;
  }

  /**
   * https://stackoverflow.com/a/48126059
   */
  @SuppressWarnings("rawtypes")
  public void copySelectionToClipboard(final TreeTableView<?> table) {
    final Set<Integer> rows = new TreeSet<>();

    Set<TreeTableColumn> columns = new HashSet<>();
    for (final TreeTablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
      rows.add(tablePosition.getRow());
      final TreeTableColumn column = tablePosition.getTableColumn();
      if (column.getUserData() instanceof DataType data) {
        columns.add(column);
      }
    }

    final StringBuilder strb = new StringBuilder();
    boolean firstRow = true;
    // use columns from table to maintain the visible order
    final List<TreeTableColumn<?, ?>> tableColumns = getVisibleColumsRecursive(table.getColumns());

    for (TreeTableColumn<?, ?> tableColumn : tableColumns) {
      if (columns.contains(tableColumn)) {
        if (newColumnMap.get(tableColumn).getRaw() != null) {
          strb.append(newColumnMap.get(tableColumn).getRaw().getName()).append(":");
        }
        strb.append(((DataType) tableColumn.getUserData()).getHeaderString()).append('\t');
      }
    }

    strb.append('\n');

    for (final Integer row : rows) {
      if (!firstRow) {
        strb.append('\n');
      }
      firstRow = false;
      boolean firstCol = true;
      // use columns from table to maintain the visible order
      for (final TreeTableColumn<?, ?> column : tableColumns) {
        if (!columns.contains(column)) {
          continue;
        }
        if (!firstCol) {
          strb.append('\t');
        }
        firstCol = false;
        final Object cellData = column.getCellData(row);
        strb.append(
            cellData == null ? "" : ((DataType) column.getUserData()).getFormattedString(cellData));
      }
    }
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(strb.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
    logger.info(() -> "Copied selection to clipboard.");
  }

  public List<TreeTableColumn<?, ?>> getVisibleColumsRecursive(
      ObservableList<? extends TreeTableColumn<?, ?>> cols) {
    List<TreeTableColumn<?, ?>> columns = new ArrayList<>();

    for (TreeTableColumn<?, ?> col : cols) {
      if (col.getUserData() != null) {
        columns.add(col);
      }
      if (!col.getColumns().isEmpty()) {
        columns.addAll(getVisibleColumsRecursive(col.getColumns()));
      }
    }
    return columns;
  }
}
