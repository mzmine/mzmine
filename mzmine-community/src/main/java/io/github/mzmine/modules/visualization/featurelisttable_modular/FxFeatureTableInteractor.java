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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableFilterMenu.FxFeatureTableFilterMenuModel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxFeatureTableInteractor extends FxInteractor<FxFeatureTableModel> {

  // weak listeners owned by this interactor; disposed via dispose() when the controller closes
  private final WeakAdapter weak = new WeakAdapter();

  // Per-pass set of interactors that have already initiated outbound propagation in the current
  // FX-thread call chain. Lets transitive A -> B -> C propagation work while preventing cycles
  // (B -> A) and re-entry (B -> B via a second listener firing on the same property).
  private static final ThreadLocal<Set<FxFeatureTableInteractor>> PROPAGATION_VISITED =
      ThreadLocal.withInitial(HashSet::new);

  // Forward toggle: when set, internal table-selection listeners do not push the new selection back
  // into the property (used while we are driving the table from a property write).
  private boolean drivingTableFromProperty = false;

  protected FxFeatureTableInteractor(FxFeatureTableModel model) {
    super(model);

    initActionEvents();
    initListeners();
    wireBindingProperties();
  }

  private void initActionEvents() {
    model.setOnOpenParameterDialogAction(this::openParametersDialog);
    model.setOnQuickColumnSelectionAction(this::openQuickColumnSelection);
  }

  private void openQuickColumnSelection() {
    model.getFeatureTable().showQuickColumnSelectionContextMenu();
  }

  private void initListeners() {
    PropertyUtils.onChange(this::updateFilterPrompts, model.rowsMzRangeProperty(),
        model.rowsRetentionTimeRangeProperty());

    model.getFilterModel().combinedRowFilterProperty().subscribe(this::applyRowsFilter);

    // bidirectional bind: toggle in filter menu ↔ compound row selection on the table
    model.getFeatureTable().compoundRowSelectionProperty()
        .bindBidirectional(model.getFilterModel().compoundRowSelectionProperty());

    // derive whether the current feature list has a valid compound list
    model.getFilterModel().compoundListAvailableProperty().bind(
        Bindings.createBooleanBinding(() -> {
          final ModularFeatureList flist = model.getFeatureList();
          return flist != null && flist.hasCompoundList();
        }, model.featureListProperty()));

    // Narrow the row-selection options to hide ALL_ISOTOPES when the compound list has no nested
    // (isotope) sub-rows; otherwise the option would yield an empty view.
    model.featureListProperty().subscribe(_ -> updateAvailableCompoundRowSelections());
  }

  /**
   * Wire the four cross-dashboard binding properties on the model to the underlying
   * {@link FeatureTableFX}: external writes drive the table, table changes update the property,
   * and changes are propagated to every active outgoing link.
   */
  private void wireBindingProperties() {
    final FeatureTableFX table = model.getFeatureTable();
    final ObjectProperty<List<FeatureList>> selectedFeatureLists = model.selectedFeatureListsProperty();
    final ObjectProperty<List<FeatureListRow>> selectedRows = model.selectedRowsProperty();
    final ObjectProperty<@Nullable CompoundRow> selectedCompoundRow = model.selectedCompoundRowProperty();

    // selectedFeatureLists <-> model.featureList
    weak.addApplyChangeListener(this, model.featureListProperty(), (_, _, flist) -> {
      final List<FeatureList> newValue = flist == null ? List.of() : List.of(flist);
      if (!Objects.equals(selectedFeatureLists.get(), newValue)) {
        selectedFeatureLists.set(newValue);
      }
    });
    weak.addChangeListener(this, selectedFeatureLists, (_, _, lists) -> {
      final FeatureList desired = lists == null || lists.isEmpty() ? null : lists.getFirst();
      if (!Objects.equals(model.getFeatureList(), desired)) {
        model.getFeatureTable().setFeatureList((ModularFeatureList) desired);
      }
      propagate(FxFeatureTableModel::selectedFeatureListsProperty, lists);
    });

    // selectedRows <-> table.selectionModel.selectedItems
    final ObservableList<TreeItem<ModularFeatureListRow>> selectedItems = table.getSelectionModel()
        .getSelectedItems();
    weak.addListChangeListener(this, selectedItems,
        (ListChangeListener<TreeItem<ModularFeatureListRow>>) _ -> {
          if (drivingTableFromProperty) {
            return;
          }
          final List<FeatureListRow> rows = selectedItems.stream()
              .filter(Objects::nonNull)
              .map(TreeItem::getValue)
              .filter(Objects::nonNull)
              .map(r -> (FeatureListRow) r)
              .toList();
          if (!Objects.equals(selectedRows.get(), rows)) {
            selectedRows.set(rows);
          }
        });
    weak.addChangeListener(this, selectedRows, (_, _, rows) -> {
      driveTableSelection(rows);
      propagate(FxFeatureTableModel::selectedRowsProperty, rows);
    });

    // selectedCompoundRow: derived from selectedRows; on external write select the matching row.
    weak.addChangeListener(this, selectedRows,
        (_, _, rows) -> updateSelectedCompoundFromRows(rows));
    weak.addChangeListener(this, selectedCompoundRow, (_, _, compound) -> {
      if (compound != null) {
        final FeatureListRow asRow = compound; // CompoundRow extends FeatureListRow
        final List<FeatureListRow> current = selectedRows.get();
        if (current == null || !current.contains(asRow)) {
          selectedRows.set(List.of(asRow));
        }
      }
      propagate(FxFeatureTableModel::selectedCompoundRowProperty, compound);
    });

  }

  private void updateSelectedCompoundFromRows(@Nullable List<FeatureListRow> rows) {
    final FeatureListRow first = rows == null || rows.isEmpty() ? null : rows.getFirst();
    final CompoundRow resolved = resolveCompoundRow(first);
    final ObjectProperty<@Nullable CompoundRow> prop = model.selectedCompoundRowProperty();
    if (prop.get() != resolved) {
      prop.set(resolved);
    }
  }

  /**
   * Drive the underlying table selection from a writable property value. Skips when the requested
   * rows are already selected. Guarded by {@link #drivingTableFromProperty} so the selection-model
   * listener does not echo the change back into the property.
   */
  private void driveTableSelection(@Nullable List<FeatureListRow> rows) {
    final FeatureTableFX table = model.getFeatureTable();
    final List<? extends FeatureListRow> current = table.getSelectedRows();
    if (Objects.equals(current, rows)) {
      return;
    }
    drivingTableFromProperty = true;
    try {
      if (rows == null || rows.isEmpty()) {
        table.getSelectionModel().clearSelection();
        return;
      }
      // First row drives single-selection scroll/focus; multi-selection beyond the first is
      // out of scope for cross-dashboard linking.
      final FeatureListRow first = rows.getFirst();
      table.getSelectionModel().clearSelection();
      FeatureTableFXUtil.selectAndScrollTo(first, table);
    } finally {
      drivingTableFromProperty = false;
    }
  }

  /**
   * Push a value to every active outgoing link's matching property. When the target's property
   * listener fires it re-enters {@code propagate}, so changes are forwarded transitively through
   * the link graph. The thread-local {@link #PROPAGATION_VISITED} set marks every interactor whose
   * outbound propagation is currently in progress, which both breaks cycles (B -> A) and prevents
   * re-entering the same interactor when several listeners on the same property update further
   * downstream state.
   *
   * @param accessor function returning the target model's property of the same kind
   * @param value    value to propagate
   */
  private <T> void propagate(@NotNull final Function<FxFeatureTableModel, Property<T>> accessor,
      @Nullable final T value) {
    final Set<FxFeatureTableInteractor> visited = PROPAGATION_VISITED.get();
    if (!visited.add(this)) {
      // Already propagating from this interactor in the current chain; bail to avoid cycles.
      return;
    }
    try {
      for (FeatureTableLink link : model.getOutgoingLinks()) {
        if (!link.active().get()) {
          continue;
        }
        final FxFeatureTableController target = link.getTarget();
        if (target == null) {
          continue;
        }
        final FxFeatureTableInteractor targetInteractor = target.getTableInteractor();
        if (targetInteractor == this || visited.contains(targetInteractor)) {
          continue;
        }
        final Property<T> targetProp = accessor.apply(target.getModel());
        if (Objects.equals(targetProp.getValue(), value)) {
          continue;
        }
        // Writing the target property fires its change listener, which calls propagate on the
        // target interactor and forwards through that controller's own outgoing links.
        targetProp.setValue(value);
      }
    } finally {
      visited.remove(this);
      if (visited.isEmpty()) {
        // Avoid leaking an empty set onto the FX thread when the chain unwinds.
        PROPAGATION_VISITED.remove();
      }
    }
  }

  /**
   * Resolve a clicked feature-list row to its parent {@link CompoundRow}: prefers the parent
   * compound when the row is a member; returns the row itself when it is already a compound;
   * otherwise null.
   */
  public static @Nullable CompoundRow resolveCompoundRow(@Nullable final FeatureListRow row) {
    if (row == null) {
      return null;
    }
    final CompoundList compoundList =
        row.getFeatureList() == null ? null : row.getFeatureList().getCompoundList();
    if (compoundList != null) {
      final List<ModularCompoundRow> owners = compoundList.findCompoundsOf(row);
      if (!owners.isEmpty()) {
        return owners.getFirst();
      }
    }
    return row instanceof CompoundRow cr ? cr : null;
  }

  /**
   * Dispose of all weak listeners owned by this interactor. Called by the controller on close.
   */
  public void dispose() {
    weak.dipose();
  }

  private void updateAvailableCompoundRowSelections() {
    final FxFeatureTableFilterMenuModel filterModel = model.getFilterModel();
    final ModularFeatureList flist = model.getFeatureList();
    final CompoundList cl = flist == null ? null : flist.getCompoundList();
    final ObservableList<CompoundRowSelection> avail = filterModel.getAvailableCompoundRowSelections();
    if (cl != null && cl.hasNestedCompoundRows()) {
      avail.setAll(CompoundRowSelection.values());
    } else {
      avail.setAll(CompoundRowSelection.COMPOUNDS, CompoundRowSelection.ALL_MAJOR_IONS);
      // reset the current selection if it just became invalid
      if (filterModel.getCompoundRowSelection() == CompoundRowSelection.ALL_ISOTOPES) {
        filterModel.setCompoundRowSelection(CompoundRowSelection.ALL_MAJOR_IONS);
      }
    }
  }

  private void applyRowsFilter(@Nullable TableFeatureListRowFilter filter) {
    // clear selection before changing predicate to avoid stale indices in the
    // TreeTableView selection model (leads to IndexOutOfBoundsException on sort)
    final FeatureListRow row = model.getFeatureTable().getSelectedRow();
    model.getFeatureTable().getSelectionModel().clearSelection();
    model.getFilteredRowItems()
        .setPredicate(item -> filter == null || filter.test(item.getValue()));
    if (row != null) {
      // try to re-select and scroll if the previously selected row is still in the filtered items.
      FeatureTableFXUtil.selectAndScrollTo(row, model.getFeatureTable());
    }
  }

  private void updateFilterPrompts() {
    final FxFeatureTableFilterMenuModel filterModel = model.getFilterModel();
    if (filterModel == null) {
      return;
    }
    if (model.getRowsMzRange() == null || model.getRowsRetentionTimeRange() == null) {
      filterModel.mzFilterPromptProperty().set(null);
      filterModel.rtFilterPromptProperty().set(null);
      return;
    }

    // reduced accuracy to not overflow size of textfield
    DecimalFormat format = new DecimalFormat("0.0");

    filterModel.mzFilterPromptProperty().set(RangeUtils.toString(model.getRowsMzRange(), format));
    filterModel.rtFilterPromptProperty()
        .set(RangeUtils.toString(model.getRowsRetentionTimeRange(), format));
  }


  public void openParametersDialog() {
    final ParameterSet param = model.getParameters();
    if (param == null) {
      return;
    }
    FxThread.runLater(() -> {
      ExitCode exitCode = param.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        updateWindowToParameterSetValues();
        // set to module
        ConfigService.getConfiguration()
            .setModuleParameters(FeatureTableFXModule.class, param.cloneParameterSet());
      }
    });
  }

  /**
   * In case the parameters are changed in the setup dialog, they are applied to the window.
   */
  public void updateWindowToParameterSetValues() {
    final FeatureTableFX table = model.getFeatureTable();
    final ParameterSet param = model.getParameters();
    if (table == null || param == null) {
      return;
    }
    model.getFeatureTable().updateColumnsVisibilityParameters(
        param.getParameter(FeatureTableFXParameters.showRowTypeColumns).getValue(),
        param.getParameter(FeatureTableFXParameters.showFeatureTypeColumns).getValue());
  }

  @Override
  public void updateModel() {

  }
}
