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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import javafx.scene.layout.BorderPane;

/**
 * General pane that updates if the feature list rows change based on an ObservableList that is
 * passed by the parent
 */
public abstract class AbstractFeatureListRowsPane extends BorderPane implements
    FeatureListRowsPane {

  private final ParentFeatureListPaneGroup parentGroup;

  public AbstractFeatureListRowsPane(final ParentFeatureListPaneGroup parentGroup) {
    this.parentGroup = parentGroup;
    parentGroup.addChildren(this);
  }

  public ParentFeatureListPaneGroup getParentGroup() {
    return parentGroup;
  }

  public void setFeatureTable(final FeatureTableFX table) {
    parentGroup.featureTableFXProperty().set(table);
  }

  /**
   * If feature table is linked to this pane - the featurelist will be set there
   *
   * @param featureList
   */
  public void setFeatureList(final FeatureList featureList) {
    FeatureTableFX table = parentGroup.featureTableFXProperty().get();
    if (table != null) {
      if (featureList instanceof ModularFeatureList mod) {
        // listener will set the featurelist to this pane
        table.setFeatureList(mod);
        return;
      } else {
        table.setFeatureList(null);
      }
    }
    parentGroup.featureListProperty().set(featureList);
  }
}
