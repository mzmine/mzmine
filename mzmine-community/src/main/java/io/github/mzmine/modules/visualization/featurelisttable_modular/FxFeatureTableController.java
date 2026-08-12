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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowBinding;
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowSelectionBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxCachedViewController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableFilterMenu.FxFeatureTableFilterMenuModel;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxFeatureTableController extends FxCachedViewController<FxFeatureTableModel> implements
    SelectedFeatureListsBinding, SelectedRowsBinding, SelectedCompoundRowBinding,
    SelectedCompoundRowSelectionBinding {

  // key under which the controller stores itself in the underlying FeatureTableFX node properties.
  private static final String CONTROLLER_PROPERTY_KEY = "fxFeatureTableController";

  private final FxFeatureTableViewBuilder viewBuilder;
  private final FxFeatureTableInteractor interactor;

  public FxFeatureTableController(FeatureTableOwner tableOwner) {
    // use a clone, will set parameters to module params after tab close
    var params = ConfigService.getConfiguration().getModuleParameters(FeatureTableFXModule.class)
        .cloneParameterSet();
    super(new FxFeatureTableModel(params, tableOwner));

    // interactor before view is built; it wires the model bindings to the FeatureTableFX.
    interactor = new FxFeatureTableInteractor(model);
    viewBuilder = new FxFeatureTableViewBuilder(model, this);

    // expose self for reverse lookup from FeatureTableFX -> controller
    model.getFeatureTable().getProperties().put(CONTROLLER_PROPERTY_KEY, this);
  }

  @Override
  protected @NotNull FxViewBuilder<FxFeatureTableModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  public void close() {
    super.close();
    interactor.dispose();
    model.getOutgoingLinks().clear();
    final FeatureTableFX table = model.getFeatureTable();
    table.getProperties().remove(CONTROLLER_PROPERTY_KEY);
    table.closeTable();
    // save parameters
    ConfigService.getConfiguration()
        .setModuleParameters(FeatureTableFXModule.class, model.getParameters());
  }

  public void setFeatureList(@Nullable FeatureList featureList) {
    model.getFeatureTable().setFeatureList((ModularFeatureList) featureList);
  }

  /**
   * Should use other methods to modify the model instead of the table directly. But still here for
   * legacy reason
   */
  @NotNull
  public FeatureTableFX getFeatureTable() {
    return model.getFeatureTable();
  }

  @Nullable
  public FeatureList getFeatureList() {
    return model.getFeatureList();
  }

  public @NotNull FxFeatureTableFilterMenuModel getFilterModel() {
    return model.getFilterModel();
  }

  /**
   * Root view used for scene-graph traversal (e.g. from the link popover to focus the owning Tab
   * or Stage). Returns null until {@link #buildView()} has been called.
   */
  public @Nullable Region getRootView() {
    return cachedView;
  }

  // --- accessors for the interactor / cross-controller propagation ---------

  /**
   * @return the model. Public so the {@link FxFeatureTableInteractor} of a peer controller can
   * read the cross-dashboard binding properties during link propagation.
   */
  public @NotNull FxFeatureTableModel getModel() {
    return model;
  }

  /**
   * @return the interactor. Public so a peer controller's interactor can flip the reentrancy
   * guard during link propagation.
   */
  public @NotNull FxFeatureTableInteractor getTableInteractor() {
    return interactor;
  }

  // --- cross-dashboard linking (delegates to model) ------------------------

  public @NotNull ObservableList<FeatureTableLink> getOutgoingLinks() {
    return model.getOutgoingLinks();
  }

  public void linkTo(@NotNull FxFeatureTableController target, boolean active) {
    if (target == this) {
      return;
    }
    model.linkTo(target, active);
    if (active) {
      // Sync the new target to our current state so it does not have to wait for the next change.
      // Writes are skipped when values already match; equality on each property short-circuits
      // downstream propagation. Order matters: feature list first (rebuilds the target's rows),
      // then row selection (drives scroll/select inside the new rows), then the derived compound.
      pushBindingTo(model.selectedFeatureListsProperty(),
          target.getModel().selectedFeatureListsProperty());
      pushBindingTo(model.selectedRowsProperty(), target.getModel().selectedRowsProperty());
      pushBindingTo(model.selectedCompoundRowProperty(),
          target.getModel().selectedCompoundRowProperty());
    }
  }

  private static <T> void pushBindingTo(@NotNull Property<T> source, @NotNull Property<T> target) {
    final T value = source.getValue();
    if (!Objects.equals(target.getValue(), value)) {
      target.setValue(value);
    }
  }

  public void unlink(@NotNull FxFeatureTableController target) {
    model.unlink(target);
  }

  public void pruneExpiredLinks() {
    model.pruneExpiredLinks();
  }

  // --- bindings (delegate to model) ----------------------------------------

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.selectedFeatureListsProperty();
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  @Override
  public ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty() {
    return model.selectedCompoundRowProperty();
  }

  @Override
  public ObjectProperty<@Nullable CompoundRowSelection> compoundRowSelectionProperty() {
    return model.getFilterModel().compoundRowSelectionProperty();
  }

  // --- static helpers ------------------------------------------------------

  /**
   * Reverse lookup: returns the {@link FxFeatureTableController} that owns the given
   * {@link FeatureTableFX}, or {@code null} if the table is not managed by a controller.
   */
  public static @Nullable FxFeatureTableController controllerFor(@Nullable FeatureTableFX table) {
    if (table == null) {
      return null;
    }
    final Object value = table.getProperties().get(CONTROLLER_PROPERTY_KEY);
    return value instanceof FxFeatureTableController ctrl ? ctrl : null;
  }

  /**
   * @see FxFeatureTableInteractor#resolveCompoundRow(FeatureListRow)
   */
  public @Nullable CompoundRow resolveCompoundRow(@Nullable final FeatureListRow row) {
    return interactor.resolveCompoundRow(row);
  }
}
