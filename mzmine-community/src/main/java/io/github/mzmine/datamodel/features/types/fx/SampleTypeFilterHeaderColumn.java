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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesModule;
import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesParameters;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.parametertypes.metadata.SampleTypeFilterComponent;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A table column that holds a {@link SampleTypeFilterComponent} in its header, which redefines the
 * sample types used for the relative standard deviation of a feature list, see
 * {@link io.github.mzmine.datamodel.features.preferences.FeatureListPreferences}.
 * <p>
 * The filter is not applied directly to the feature list but by running the
 * {@link FeatureListPreferencesModule}, so that the change shows up as an applied method and is
 * therefore visible in the feature list history and reproducible in a batch.
 * <p>
 * The column is created before it is added to a table, and the feature list is only reachable
 * through the rows of that table, therefore the displayed filter is synchronized as soon as the
 * first row appears.
 */
public class SampleTypeFilterHeaderColumn extends TreeTableColumn<ModularFeatureListRow, Object> {

  private static final Logger logger = Logger.getLogger(
      SampleTypeFilterHeaderColumn.class.getName());

  private final SampleTypeFilterComponent filterComponent;
  private final ListChangeListener<TreeItem<ModularFeatureListRow>> rowsListener = _ -> syncFromFeatureList();

  /**
   * The feature list the component currently shows the preferences of. Also used to only run the
   * module for changes the user made in the component.
   */
  private @Nullable ModularFeatureList syncedFeatureList;
  /**
   * True while the component is updated from the feature list preferences, so that this does not
   * loop back into running the module again.
   */
  private boolean updatingFromFeatureList = false;
  private @Nullable Subscription rootSubscription;
  private @Nullable TreeItem<ModularFeatureListRow> listenedRoot;

  public SampleTypeFilterHeaderColumn(@NotNull final DataType<?> dataType,
      @NotNull final SampleTypeFilter defaultFilter) {
    super();

    setUserData(dataType);
    // the main column of sub columns has no values to sort by
    setSortable(false);

    final HBox header = FxLayout.newHBox(Pos.CENTER);
    // types like the RSD main type have no title, their sub columns carry the meaning
    if (!dataType.getHeaderString().isBlank()) {
      header.getChildren().add(new Label(dataType.getHeaderString()));
    }

    filterComponent = new SampleTypeFilterComponent(defaultFilter);
    header.getChildren().add(filterComponent);
    VBox.setVgrow(filterComponent, Priority.ALWAYS);
    setGraphic(header);

    treeTableViewProperty().subscribe(this::onTableChanged);
    filterComponent.valueProperty().subscribe(this::onFilterChanged);
  }

  private void onTableChanged(@Nullable final TreeTableView<ModularFeatureListRow> table) {
    if (rootSubscription != null) {
      rootSubscription.unsubscribe();
      rootSubscription = null;
    }
    if (table == null) {
      onRootChanged(null);
      return;
    }

    // the root is usually already set when this column is added, and subscribing with two
    // arguments does not fire for the current value - therefore apply it directly
    rootSubscription = table.rootProperty().subscribe((_, newRoot) -> onRootChanged(newRoot));
    onRootChanged(table.getRoot());
  }

  private void onRootChanged(@Nullable final TreeItem<ModularFeatureListRow> root) {
    if (listenedRoot != null) {
      listenedRoot.getChildren().removeListener(rowsListener);
    }
    listenedRoot = root;
    if (root != null) {
      // the rows are added after this column was created, sync as soon as the first one appears
      root.getChildren().addListener(rowsListener);
    }
    syncFromFeatureList();
  }

  /**
   * Shows the filter of the feature list this column belongs to, as soon as that feature list is
   * known and whenever it changes.
   */
  private void syncFromFeatureList() {
    final ModularFeatureList flist = extractFeatureList();
    if (flist == null || flist == syncedFeatureList) {
      return;
    }

    syncedFeatureList = flist;
    updatingFromFeatureList = true;
    try {
      filterComponent.setValue(flist.getPreferences().getRsdSampleTypeFilter());
    } finally {
      updatingFromFeatureList = false;
    }
  }

  private void onFilterChanged(@Nullable final SampleTypeFilter filter) {
    if (updatingFromFeatureList || filter == null) {
      return;
    }
    // without a feature list there is nothing to apply the filter to. The next sync will replace
    // the value by the one of the feature list anyway
    final ModularFeatureList flist = syncedFeatureList;
    if (flist == null || filter.equals(flist.getPreferences().getRsdSampleTypeFilter())) {
      return;
    }

    logger.finest(
        () -> "Redefining RSD sample types of feature list %s to %s".formatted(flist.getName(),
            filter));

    final FeatureListPreferencesParameters parameters = FeatureListPreferencesParameters.fromPreferences(
        flist.getPreferences().withRsdSampleTypeFilter(filter));
    parameters.getParameter(FeatureListPreferencesParameters.flists)
        .setValue(FeatureListsSelectionType.SPECIFIC_FEATURELISTS, new FeatureList[]{flist});
    // run the module instead of setting the preferences directly, so that the applied method is
    // added to the feature list
    MZmineCore.runMZmineModule(FeatureListPreferencesModule.class, parameters);
  }

  /**
   * @return the feature list of the first row of the table or null if the table is not set or still
   * empty
   */
  private @Nullable ModularFeatureList extractFeatureList() {
    final TreeTableView<ModularFeatureListRow> table = getTreeTableView();
    if (table == null || table.getRoot() == null) {
      return null;
    }
    return table.getRoot().getChildren().stream().map(TreeItem::getValue).filter(Objects::nonNull)
        .findFirst().map(ModularFeatureListRow::getFeatureList).orElse(null);
  }
}
