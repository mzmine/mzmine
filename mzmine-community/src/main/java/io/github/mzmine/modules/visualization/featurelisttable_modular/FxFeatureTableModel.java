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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableFilterMenu.FxFeatureTableFilterMenuModel;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Comparator;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;

public class FxFeatureTableModel {

  // fixed values
  private final @NotNull FxFeatureTableFilterMenuModel filterModel = new FxFeatureTableFilterMenuModel();
  private final @NotNull ParameterSet parameters;
  private Runnable onOpenParameterDialogAction;
  private Runnable onQuickColumnSelectionAction;

  // for now we need to expose the feature table as it is used in many places
  // usually better to not put view classes into the model
  private final @NotNull FeatureTableFX featureTable;

  // derived properties from feature table
  private final ReadOnlyObjectWrapper<ModularFeatureList> featureList = new ReadOnlyObjectWrapper<>();

  private final ReadOnlyObjectWrapper<Range<Double>> rowsMzRange = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<Range<Float>> rowsRetentionTimeRange = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyListWrapper<DataType> rowTypes = new ReadOnlyListWrapper<>(
      FXCollections.observableArrayList());


  public FxFeatureTableModel(@NotNull ParameterSet parameters) {
    this.parameters = parameters;
    featureTable = new FeatureTableFX(parameters);

    internalBindings();
  }

  /**
   * internal bindings to read only properties
   */
  private void internalBindings() {
    featureList.bind(featureTable.featureListProperty());
    featureList.subscribe(flist -> {
      if (flist == null) {
        rowTypes.clear();
        rowsMzRange.set(null);
        rowsRetentionTimeRange.set(null);
        return;
      }

      rowTypes.setAll(flist.getRowTypes().stream().filter(type -> !(type instanceof NoTextColumn))
          .sorted(Comparator.comparing(DataType::getHeaderString)).toList());

      rowsMzRange.set(flist.getRowsMZRange());
      rowsRetentionTimeRange.set(flist.getRowsRTRange());
    });
  }

  public void setOnOpenParameterDialogAction(Runnable onOpenParameterDialogAction) {
    this.onOpenParameterDialogAction = onOpenParameterDialogAction;
  }

  public void setOnQuickColumnSelectionAction(Runnable onQuickColumnSelectionAction) {
    this.onQuickColumnSelectionAction = onQuickColumnSelectionAction;
  }

  public Runnable getOnOpenParameterDialogAction() {
    return onOpenParameterDialogAction;
  }

  public Runnable getOnQuickColumnSelectionAction() {
    return onQuickColumnSelectionAction;
  }

  @NotNull
  public FxFeatureTableFilterMenuModel getFilterModel() {
    return filterModel;
  }

  @NotNull
  public ParameterSet getParameters() {
    return parameters;
  }

  public ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public ReadOnlyObjectProperty<ModularFeatureList> featureListProperty() {
    return featureList.getReadOnlyProperty();
  }

  public ReadOnlyListProperty<DataType> getRowTypes() {
    return rowTypes.getReadOnlyProperty();
  }

  @NotNull
  public FeatureTableFX getFeatureTable() {
    return featureTable;
  }

  public ObservableList<TreeItem<ModularFeatureListRow>> getSelectedTableRows() {
    return featureTable.getSelectedTableRows();
  }

  public FilteredList<TreeItem<ModularFeatureListRow>> getFilteredRowItems() {
    return featureTable.getFilteredRowItems();
  }

  private static @NotNull Predicate<TreeItem<ModularFeatureListRow>> getTreeItemPredicate() {
    return t -> t.getValue() != null;
  }

  public Range<Double> getRowsMzRange() {
    return rowsMzRange.get();
  }

  public ReadOnlyObjectWrapper<Range<Double>> rowsMzRangeProperty() {
    return rowsMzRange;
  }

  public Range<Float> getRowsRetentionTimeRange() {
    return rowsRetentionTimeRange.get();
  }

  public ReadOnlyObjectWrapper<Range<Float>> rowsRetentionTimeRangeProperty() {
    return rowsRetentionTimeRange;
  }

}
