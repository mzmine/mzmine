/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentMainType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
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
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
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
import io.github.mzmine.datamodel.features.types.numbers.scores.CompoundAnnotationScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MsMsScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SimilarityType;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_deleterows.DeleteRowsModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.datatype.DataTypeCheckListParameter;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
    addContextMenuItem(contextMenuHelper, "Compact table", e -> showCompactChromatographyColumns());
    addContextMenuItem(contextMenuHelper, "Toggle shape columns", e -> toggleShapeColumns());
    addContextMenuItem(contextMenuHelper, "Toggle alignment columns",
        e -> toggleAlignmentColumns());
    addContextMenuItem(contextMenuHelper, "Toggle ion identities", e -> toggleIonIdentities());
    addContextMenuItem(contextMenuHelper, "Toggle library matches", e -> toggleAnnotations());

    final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C,
        KeyCombination.CONTROL_ANY);

    setOnKeyPressed(event -> {
      if (keyCodeCopy.match(event)) {
        copySelectionToClipboard(this);
        event.consume();
      }
    });

    this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.isControlDown() && event.getCode() == KeyCode.A) {
        // selecting everything causes feature table to freeze
        event.consume();
      }
    });

    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.DELETE) {
        final List<ModularFeatureListRow> rows = getSelectedRows();
        getSelectionModel().clearSelection();
        DeleteRowsModule.deleteWithConfirmation(featureListProperty.get(), rows);
      }
    });
  }

  /**
   * @return the default height or area DataType to show in the table
   */
  public Class<? extends HeightType> getDefaultAbundanceMeasureType() {
    return switch (parameters.getValue(FeatureTableFXParameters.defaultAbundanceMeasure)) {
      case Height -> HeightType.class;
      case Area -> AreaType.class;
    };
  }

  /**
   * Applies when a new FeatureTableFX is created
   *
   * @return default visibility of shapes
   */
  public boolean getDefaultVisibilityOfShapes() {
    return parameters.getValue(FeatureTableFXParameters.defaultVisibilityOfShapes);
  }

  /**
   * Applies when a new FeatureTableFX is created
   *
   * @return default visibility of images in features
   */
  public boolean getDefaultVisibilityOfImages() {
    return parameters.getValue(FeatureTableFXParameters.defaultVisibilityOfImages);
  }

  /**
   * Applies when a new FeatureTableFX is created
   *
   * @return default visibility of ims charts in features
   */
  public boolean getDefaultVisibilityOfImsFeature() {
    return parameters.getValue(FeatureTableFXParameters.defaultVisibilityOfImsFeature);
  }

  /**
   * @return the maximum samples before deactivating the shapes columns
   */
  public int getMaximumSamplesForVisibleShapes() {
    return parameters.getValue(FeatureTableFXParameters.deactivateShapesGreaterNSamples);
  }

  private void setShapeColumnsVisible(boolean state) {
    setVisible(ColumnType.ROW_TYPE, FeatureShapeType.class, null, state);
    setVisible(ColumnType.ROW_TYPE, FeatureShapeMobilogramType.class, null, state);
    applyVisibilityParametersToAllColumns();
  }

  /**
   * toggle visibility of column and all sub columns
   */
  private void toggleColumnAndAllSubColumns(final Class<? extends DataType<?>> dataTypeClass) {
    final var columnEntry = getMainColumnEntry(dataTypeClass);
    toggleColumnVisibilityAndAllSubColumns(columnEntry);
  }

  private void toggleColumnVisibilityAndAllSubColumns(
      final Entry<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> columnEntry) {
    if (columnEntry == null) {
      return;
    }
    final ColumnID mainColumn = columnEntry.getValue();
    final boolean toggledState = !rowTypesParameter.isDataTypeVisible(mainColumn);
    setColumnVisibilityAndSubColumns(mainColumn, toggledState);
  }

  private void setColumnVisibilityAndSubColumns(final ColumnID mainColumn, final boolean visible) {
    setColumnVisibilityAndSubColumns(mainColumn, visible, true);
  }

  private void setColumnVisibilityAndSubColumns(final ColumnID mainColumn, final boolean visible,
      boolean applyVisibility) {
    rowTypesParameter.setDataTypeVisible(mainColumn, visible);
    final String parentHeader = mainColumn.getCombinedHeaderString();

    // apply to all sub columns
    if (mainColumn.getDataType() instanceof SubColumnsFactory subFact) {
      for (int i = 0; i < subFact.getNumberOfSubColumns(); i++) {
        var header = subFact.getHeader(i);
        setVisible(ColumnType.ROW_TYPE, parentHeader, header, visible);
      }
    }

    if (applyVisibility) {
      applyVisibilityParametersToAllColumns();
    }
  }

  private void toggleShapeColumns() {
    final var columnEntry = getMainColumnEntry(FeatureShapeType.class);
    if (columnEntry == null) {
      return;
    }

    final boolean toggledState = !rowTypesParameter.isDataTypeVisible(columnEntry.getValue());
    // set visibility of all types to the same
    setShapeColumnsVisible(toggledState);
  }


  private void toggleAlignmentColumns() {
    toggleColumnAndAllSubColumns(AlignmentMainType.class);
  }


  private void toggleIonIdentities() {
    final var columnEntry = getMainColumnEntry(IonIdentityListType.class);
    if (columnEntry == null) {
      return;
    }
    final ColumnID mainColumn = columnEntry.getValue();
    final boolean toggledState = !rowTypesParameter.isDataTypeVisible(mainColumn);

    setColumnVisibilityAndSubColumns(mainColumn, false, false);

    rowTypesParameter.setDataTypeVisible(mainColumn, toggledState);
    final String parentHeader = mainColumn.getUniqueIdString();
    final Class<? extends DataType<?>> parentType = (Class<? extends DataType<?>>) mainColumn.getDataType()
        .getClass();
    // basic
    setVisible(ColumnType.ROW_TYPE, parentType, IonNetworkIDType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, IonIdentityListType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, SizeType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, NeutralMassType.class, toggledState);

    // formula
    final ModularFeatureList flist = getFeatureList();
    boolean hasFormula =
        flist != null && flist.stream().flatMap(row -> row.getIonIdentities().stream())
            .anyMatch(ion -> ion.getMolFormulas().size() > 0);
    setVisible(ColumnType.ROW_TYPE, parentType, ConsensusFormulaListType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, SimpleFormulaListType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, FormulaMassType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, RdbeType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, MZType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, MzPpmDifferenceType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, MzAbsoluteDifferenceType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, IsotopePatternScoreType.class,
        toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, MsMsScoreType.class, toggledState && hasFormula);
    setVisible(ColumnType.ROW_TYPE, parentType, CombinedScoreType.class,
        toggledState && hasFormula);
    // keep states of all row types but check graphical columns
    applyVisibilityParametersToAllColumns();
  }

  private void toggleAnnotations() {
    Boolean visible = toggleSpectralLibAnnotations(false);
    visible = toggleLocalCsvLibAnnotations(visible, false);

    applyVisibilityParametersToAllColumns();
  }

  private Boolean toggleSpectralLibAnnotations(boolean applyVisibility) {
    final var columnEntry = getMainColumnEntry(SpectralLibraryMatchesType.class);
    if (columnEntry == null) {
      return null;
    }

    final ColumnID mainColumn = columnEntry.getValue();
    final boolean toggledState = !rowTypesParameter.isDataTypeVisible(mainColumn);
    // first all invisible
    setColumnVisibilityAndSubColumns(mainColumn, false, false);

    rowTypesParameter.setDataTypeVisible(mainColumn, toggledState);
    final var parentType = (Class<? extends DataType<?>>) mainColumn.getDataType().getClass();

    // basic
    setVisible(ColumnType.ROW_TYPE, parentType, SpectralLibraryMatchesType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, IonAdductType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, FormulaType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, SmilesStructureType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, PrecursorMZType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, NeutralMassType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, SimilarityType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, parentType, MatchingSignalsType.class, toggledState);

    // csv compound database

    if (applyVisibility) {
      applyVisibilityParametersToAllColumns();
    }
    return toggledState;
  }

  /**
   * @param masterState this state comes from other annotation columns that were already toggled
   *                    might be null if there was none
   */
  private Boolean toggleLocalCsvLibAnnotations(Boolean masterState, boolean applyVisibility) {
    final var columnEntry = getMainColumnEntry(CompoundDatabaseMatchesType.class);
    if (columnEntry == null) {
      return null;
    }

    final ColumnID mainColumn = columnEntry.getValue();
    final boolean toggledState = Objects.requireNonNullElse(masterState,
        !rowTypesParameter.isDataTypeVisible(mainColumn));
    // first all invisible
    setColumnVisibilityAndSubColumns(mainColumn, false, false);

    rowTypesParameter.setDataTypeVisible(mainColumn, toggledState);
    final var mainType = (Class<? extends DataType<?>>) mainColumn.getDataType().getClass();

    // csv compound database
    setVisible(ColumnType.ROW_TYPE, mainType, CompoundDatabaseMatchesType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, CompoundAnnotationScoreType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, FormulaType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, IonTypeType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, SmilesStructureType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, PrecursorMZType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, MzPpmDifferenceType.class, toggledState);
    setVisible(ColumnType.ROW_TYPE, mainType, NeutralMassType.class, toggledState);

    if (applyVisibility) {
      applyVisibilityParametersToAllColumns();
    }
    return toggledState;
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
  @Override
  public void onChanged(final Change<? extends FeatureListRow> c) {
    c.next();
    if (!(c.wasAdded() || c.wasRemoved())) {
      return;
    }

    FxThread.runLater(() -> {
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
    if (flist == null) {
      return;
    }

    // useful for debugging and seeing how many cells are empty / full
    // logTableFillingRatios(flist);

    //    logger.info("Adding columns to table");
    // for all data columns available in "data"
    assert flist instanceof ModularFeatureList : "Feature list is not modular";
    ModularFeatureList featureList = (ModularFeatureList) flist;

    // add main column for row types to show name of feature list
    TreeTableColumn<ModularFeatureListRow, String> rowCol = new TreeTableColumn<>();

    // Add raw data file label
    Label headerLabel = new Label(flist.getName());
    if (flist.getRawDataFiles().size() == 1) {
      RawDataFile raw = flist.getRawDataFiles().get(0);
      headerLabel.setTextFill(raw.getColor());
      headerLabel.setGraphic(new ImageView(FxIconUtil.getFileIcon(raw.getColor())));
    }
    rowCol.setGraphic(headerLabel);

    // add row types
    featureList.getRowTypes().stream().filter(t -> !(t instanceof FeaturesType))
        .forEach(dataType -> addColumn(rowCol, dataType));

    sortColumn(rowCol);

    // finally add row column to table
    this.getColumns().add(rowCol);

    // add features
    if (featureList.hasRowType(FeaturesType.class)) {
      addColumn(rowCol, DataTypes.get(FeaturesType.class));
    }

  }

  private static void logTableFillingRatios(final FeatureList flist) {
    long rowValues = flist.getRowTypes().stream().mapToLong(
        type -> flist.stream().map(row -> row.get(type)).filter(Objects::nonNull).count()).sum();
    long featureValues = flist.getFeatureTypes().stream().mapToLong(
            type -> flist.streamFeatures().map(f -> f.get(type)).filter(Objects::nonNull).count())
        .sum();

    long totalRowCells = (long) flist.getRowTypes().size() * flist.getNumberOfRows();
    long totalFeatureCells = (long) flist.getFeatureTypes().size() * flist.streamFeatures().count();

    logger.fine("""
        Fill stats:
        Row cells (%d types): %d / %d (%.1f)
        Feature cells (%d types): %d / %d (%.1f)""".formatted( //
        flist.getRowTypes().size(), rowValues, totalRowCells,
        (rowValues / (double) totalRowCells) * 100, //
        flist.getFeatureTypes().size(), featureValues, totalFeatureCells,
        (featureValues / (double) totalFeatureCells) * 100));
  }

  private void sortColumn(final TreeTableColumn<ModularFeatureListRow, String> parentColumn) {
    // list order of most important columns
    Map<DataType, Integer> prioMap = DataTypes.getDataTypeOrderFeatureTable();
    parentColumn.getColumns().sort(
        Comparator.comparingInt(col -> Math.min(((TreeTableColumn) col).getColumns().size(), 1))
            .thenComparingInt(col -> {
              var dataType = newColumnMap.get(col).getDataType();
              // only the important columns are listed. put rest at end
              return prioMap.getOrDefault(dataType, 99999999);
            }));
  }


  /**
   * Add a new column to the table
   *
   * @param rowCol   the row column where all rowTypes are added
   * @param dataType the new data type
   */
  public void addColumn(final TreeTableColumn<ModularFeatureListRow, String> rowCol,
      DataType dataType) {
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

      // Add row column
      rowCol.getColumns().add(col);

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

  private Entry<TreeTableColumn<ModularFeatureListRow, ?>, ColumnID> getMainColumnEntry(
      Class<? extends DataType> dtClass) {
    DataType type = DataTypes.get(dtClass);
    for (var col : newColumnMap.entrySet()) {
      var colID = col.getValue();
      if (type.equals(colID.getDataType()) && colID.getSubColIndex() == -1
          && colID.getType() == ColumnType.ROW_TYPE) {
        return col;
      }
    }
    return null;
  }

  /**
   * Registers a data type column and all it's sub colums to the
   * {@link FeatureTableFX#newColumnMap}.
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

    if (id == null) {
      column.getColumns()
          .forEach(col -> recursivelyApplyVisibilityParameterToColumn((TreeTableColumn) col));
      return;
    }

    boolean visible;
    if (id.getType() == ColumnType.FEATURE_TYPE) {
      visible = featureTypesParameter.isDataTypeVisible(id);
    } else {
      visible = rowTypesParameter.isDataTypeVisible(id);
    }
    column.setVisible(visible);
    // always hide sub columns if parent is invisible
    if (!visible) {
      for (final var sub : column.getColumns()) {
        if (sub instanceof TreeTableColumn<?, ?> subCol) {
          subCol.setVisible(false);
        }
      }
    } else {
      column.getColumns()
          .forEach(col -> recursivelyApplyVisibilityParameterToColumn((TreeTableColumn) col));
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
      for (DataType ftype : getFeatureList().getFeatureTypes()) {
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
      sortColumn(sampleCol);
      this.getColumns().add(sampleCol);
    }
  }

  private void initHandleDoubleClicks() {
    this.setOnMouseClicked(e -> {
      TreeTablePosition<ModularFeatureListRow, ?> focusedCell = getFocusModel().getFocusedCell();
      if (focusedCell == null) {
        return;
      }
      TreeTableColumn<ModularFeatureListRow, ?> tableColumn = focusedCell.getTableColumn();
      if (tableColumn == null) {
        // double click on header (happens when sorting)
        return;
      }
      handleClickOnCell(focusedCell, tableColumn, e);
    });
  }

  private void handleClickOnCell(final TreeTablePosition<ModularFeatureListRow, ?> focusedCell,
      final TreeTableColumn<ModularFeatureListRow, ?> tableColumn, final MouseEvent e) {
    logger.fine("Handle click on table cell");

    if (e.getClickCount() >= 2 && e.getButton() == MouseButton.PRIMARY) {
      if (getFeatureList() == null) {
        return;
      }

      e.consume();
      logger.finest(() -> "Double click on " + e.getSource());

      Object userData = tableColumn.getUserData();
      final ObservableValue<?> observableValue = tableColumn.getCellObservableValue(
          focusedCell.getTreeItem());
      if (observableValue == null) {
        return;
      }
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
        final Runnable runnable = (dataType.getDoubleClickAction(this, row, files, superDataType,
            cellValue));
        if (runnable != null) {
          MZmineCore.getTaskController().addTask(
              new FeatureTableDoubleClickTask(runnable, getFeatureList(), (DataType<?>) userData));
        }
      }
    }
  }

  public List<ModularFeatureListRow> getSelectedRows() {
    return getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue)
        .collect(Collectors.toList());
  }

  public ObservableList<TreeItem<ModularFeatureListRow>> getSelectedTableRows() {
    return getSelectionModel().getSelectedItems();
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
      if (cell == null) {
        return;
      }
      ColumnID id = newColumnMap.get(cell.getTableColumn());
      if (id != null) {
        RawDataFile file = id.getRaw();
        ModularFeature feature = cell.getTreeItem().getValue().getFeature(file);
        if (feature != null) {
          features.add(feature);
        }
      }
    });
    return List.copyOf(features);
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
      FxThread.runLater(() -> {
        updateFeatureList(oldValue, newValue);
      });
    });
  }

  /**
   * Removes the listener to the old feature list, clears the table and adds the new feature list,
   * builds the columns and adds required listeners.
   */
  private void updateFeatureList(@Nullable ModularFeatureList oldFeatureList,
      @Nullable ModularFeatureList newFeatureList) {
    getSelectionModel().clearSelection(); // leads to npe or index out of bound
    // Clear old rows and old columns
    getRoot().getChildren().clear();
    getColumns().clear();
    rowItems.clear();

    // remove the old listener
    if (oldFeatureList != null) {
      oldFeatureList.getRows().removeListener(this);
    }
    if (newFeatureList == null) {
      return;
    }
    addColumns(newFeatureList);
    // first check if feature list is too large
    applyDefaultColumnVisibilities();
    if (newFeatureList.getNumberOfRawDataFiles() > getMaximumSamplesForVisibleShapes()) {
      showCompactChromatographyColumns();
    }

    // add rows
    for (FeatureListRow row : newFeatureList.getRows()) {
      final ModularFeatureListRow mrow = (ModularFeatureListRow) row;
      rowItems.add(new TreeItem<>(mrow));
    }

    TreeItem<ModularFeatureListRow> root = getRoot();
    root.getChildren().addAll(filteredRowItems);

    // reflect the changes to the feature list in the table
    newFeatureList.getRows().addListener(this);
  }

  /**
   * Repopulates the feature table. Also re-creates columns, eg. after a new data type was added
   * from the gui.
   */
  public void rebuild() {
    final ModularFeatureList flist = getFeatureList();
    final ModularFeatureListRow row = getSelectedRow();
    getSelectionModel().clearSelection();
    updateFeatureList(flist, flist);
    FeatureTableFXUtil.selectAndScrollTo(row, this);
  }

  /**
   * Apply default visibility to all columns
   */
  private void applyDefaultColumnVisibilities() {
    setShapeColumnsVisible(getDefaultVisibilityOfShapes());
    // feature types only for single samples - otherwise too much performance impact
    var featureList = getFeatureList();
    if (featureList == null) {
      return;
    }
    var raws = featureList.getRawDataFiles();
    int samples = raws.size();
    long imagingFiles = raws.stream().filter(raw -> raw instanceof ImagingRawDataFile).count();
    boolean imsVisible = getDefaultVisibilityOfImsFeature() && samples == 1;
    boolean imagesVisible = getDefaultVisibilityOfImages() && imagingFiles == 1;
    setVisible(ColumnType.FEATURE_TYPE, FeatureShapeIonMobilityRetentionTimeHeatMapType.class, null,
        imsVisible);
    setVisible(ColumnType.FEATURE_TYPE, FeatureShapeMobilogramType.class, null, imagesVisible);
    applyVisibilityParametersToAllColumns();
  }

  private void showCompactChromatographyColumns() {
    var flist = getFeatureList();
    if (flist == null) {
      return;
    }

    // disable all feature types but the default abundance measure type
    featureTypesParameter.setAll(false);
    setVisible(ColumnType.FEATURE_TYPE, getDefaultAbundanceMeasureType(), null, true);

    // keep states of all row types but check graphical columns
    boolean smallDataset = flist.getNumberOfRawDataFiles() <= getMaximumSamplesForVisibleShapes();
    setVisible(ColumnType.ROW_TYPE, FeatureShapeType.class, null, smallDataset);
    setVisible(ColumnType.ROW_TYPE, FeatureShapeMobilogramType.class, null, smallDataset);
    setVisible(ColumnType.ROW_TYPE, FeaturesType.class, null, true);

    applyVisibilityParametersToAllColumns();
  }

  private void setVisible(ColumnType columnType, @NotNull String parentUniqueId,
      @Nullable String subColUniqueId, boolean visible) {
    String key = ColumnID.buildUniqueIdString(columnType, parentUniqueId, subColUniqueId);
    if (columnType == ColumnType.ROW_TYPE) {
      rowTypesParameter.setDataTypeVisible(key, visible);
    } else {
      featureTypesParameter.setDataTypeVisible(key, visible);
    }
  }

  private void setVisible(ColumnType columnType, @NotNull Class<? extends DataType<?>> parentClass,
      @Nullable Class<? extends DataType<?>> subtype, boolean visible) {
    final DataType<?> subType = subtype != null ? DataTypes.get(subtype) : null;
    final DataType<?> parentType = DataTypes.get(parentClass);
    setVisible(columnType, parentType.getUniqueID(), subType != null ? subType.getUniqueID() : null,
        visible);
  }

  public void closeTable() {
    final ModularFeatureList flist = featureListProperty.get();
    if (flist == null) {
      return;
    }
    flist.getRows().removeListener(this);
    flist.onFeatureTableFxClosed();
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
