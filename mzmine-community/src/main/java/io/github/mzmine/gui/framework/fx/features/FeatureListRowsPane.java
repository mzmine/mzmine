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

package io.github.mzmine.gui.framework.fx.features;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkPane;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsPane;
import io.github.mzmine.util.javafx.WeakAdapter;
import java.util.Collection;
import java.util.List;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * General pane that updates if the feature list rows change based on an ObservableList that is
 * passed by the parent
 */
public interface FeatureListRowsPane {

  /**
   * Initialize the bindings to rows and selected rows
   *
   * @param weak         weak bindings to rows lists that are provided by parent, e.g.,
   *                     {@link FeatureTableFX}
   * @param rows         this list contains all feature list rows - for example all rows in a
   *                     {@link FeatureList}. The pane might choose to work with all rows or only
   *                     the selected rows.
   * @param selectedRows this list contains all selected rows, for example the selected rows in
   *                     {@link FeatureTableFX} that are used in
   *                     {@link SpectraIdentificationResultsPane} or the selected nodes in
   *                     {@link FeatureNetworkPane}.
   */
  default void init(WeakAdapter weak, ObservableList<? extends FeatureListRow> rows,
      ObservableList<? extends FeatureListRow> selectedRows) {
// TODO needed to init? when do we need to bind more than the changes that we already do
  }

  /**
   * The base rows have changed, e.g., in {@link FeatureTableFX}
   *
   * @param rows this list contains all selected rows, for example the selected rows in
   *             {@link FeatureTableFX} that are used in {@link SpectraIdentificationResultsPane} or
   *             the selected nodes in {@link FeatureNetworkPane}.
   */
  default void onRowsChanged(@NotNull List<? extends FeatureListRow> rows) {
    getChildFeaturePanes().forEach(child -> child.onRowsChanged(rows));
  }

  /**
   * The selected rows have changed.
   *
   * @param selectedRows this list contains all selected rows, for example the selected rows in
   *                     {@link FeatureTableFX} that are used in
   *                     {@link SpectraIdentificationResultsPane} or the selected nodes in
   *                     {@link FeatureNetworkPane}.
   */
  default void onSelectedRowsChanged(@NotNull List<? extends FeatureListRow> selectedRows) {
    getChildFeaturePanes().forEach(child -> child.onSelectedRowsChanged(selectedRows));
  }

  /**
   * Dispose listeners
   */
  default void disposeListeners() {
    getChildFeaturePanes().forEach(FeatureListRowsPane::disposeListeners);
  }

  /**
   * Children are used to propagate events to them
   */
  @NotNull
  default Collection<FeatureListRowsPane> getChildFeaturePanes() {
    return List.of();
  }


  /**
   * Defines if this JavaFX node has row specific content like annotations, charts, etc
   *
   * @return true if content available
   */
  boolean hasContent();

  /**
   * Defines if this JavaFX node has NO row specific content like annotations, charts, etc
   *
   * @return true if NO content available; opposite of {@link #hasContent()}
   */
  default boolean isEmptyContent() {
    return !hasContent();
  }
}
