/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.framework.fx.features;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This pane may be derived from a {@link FeatureTableFX} or directly from {@link FeatureList}. rows
 * and selected rows are decoupled from the original feature list and table but linked by weak
 * listeners. Changes of the feature list or table are also automatically done.
 * <p>
 * This parent may be passed to its children to access properties
 */
public class ParentFeatureListPaneGroup implements FeatureListRowsPane {

  private final WeakAdapter weak = new WeakAdapter();

  // source of data - table might be null if this was directly opened for a feature list
  private final ObjectProperty<FeatureTableFX> featureTableFX = new SimpleObjectProperty<>();
  private final ObjectProperty<FeatureList> featureList = new SimpleObjectProperty<>();

  // decoupled from the original feature list to allow changes of the underlying feature list
  private final ObservableList<FeatureListRow> rows = FXCollections.observableArrayList();
  private final ObservableList<FeatureListRow> selectedRows = FXCollections.observableArrayList();

  // propagate changes to children
  private final List<FeatureListRowsPane> children = new ArrayList<>();

  public ParentFeatureListPaneGroup() {
    super();
    // first listen to internal data changes
    listenToInternalDataChanges();
    // then to external data sources
    listenToExternalDataSourceChanges();
  }


  /**
   * Changes to the rows come from outside but those lists are internally
   */
  private void listenToInternalDataChanges() {
    // listen to changes to the rows and selected rows
    // weak so that we can stop listening easily
    weak.addListChangeListener(this, rows, c -> {
      if (weak.isDisposed()) {
        return;
      }
      onRowsChanged(c.getList());
    });
    weak.addListChangeListener(this, selectedRows, c -> {
      if (weak.isDisposed()) {
        return;
      }
      onSelectedRowsChanged(c.getList());
    });
  }


  private void listenToExternalDataSourceChanges() {
    featureList.addListener((obs, oldList, newList) -> {
      weak.removeAllForParent(oldList);
      // weakly bind
      if(newList != null) {
        FeatureListUtils.bindRows(weak, newList, rows);
      }
    });

    featureTableFX.addListener((obs, oldTable, newTable) -> {
      // then change selected rows in table
      weak.removeAllForParent(oldTable);
      if (newTable == null) {
        return;
      }

      // first change active feature list
      // feature list in table might change
      weak.bind(newTable, newTable.featureListProperty(), featureList);

      // weakly bind
      FeatureListUtils.bindSelectedRows(weak, newTable, selectedRows);
    });
  }

  @Override
  public @NotNull Collection<FeatureListRowsPane> getChildFeaturePanes() {
    return children;
  }

  @Override
  public boolean hasContent() {
    return getChildFeaturePanes().stream().anyMatch(FeatureListRowsPane::hasContent);
  }

  public ObjectProperty<FeatureList> featureListProperty() {
    return featureList;
  }

  public ObjectProperty<FeatureTableFX> featureTableFXProperty() {
    return featureTableFX;
  }

  public ObservableList<FeatureListRow> getRows() {
    return rows;
  }

  public ObservableList<FeatureListRow> getSelectedRows() {
    return selectedRows;
  }

  public List<FeatureListRowsPane> getChildren() {
    return children;
  }

  public boolean addChildren(Collection<? extends FeatureListRowsPane> panes) {
    return children.addAll(panes);
  }

  public boolean addChildren(FeatureListRowsPane... panes) {
    return children.addAll(List.of(panes));
  }

  /**
   * Dispose listeners
   */
  @Override
  public void disposeListeners() {
    FeatureListRowsPane.super.disposeListeners();
    weak.dipose();
    featureList.setValue(null);
    featureTableFX.setValue(null);
  }
}
