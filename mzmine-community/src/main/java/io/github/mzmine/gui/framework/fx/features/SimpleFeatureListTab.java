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
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.Collection;
import javafx.scene.Node;

/**
 * Currently only handles a single {@link FeatureList}
 */
public class SimpleFeatureListTab extends SimpleTab {

  private final ParentFeatureListPaneGroup parentGroup;

  public SimpleFeatureListTab(String title, FeatureTableFX table) {
    this(title);
    parentGroup.featureTableFXProperty().set(table);
  }

  public SimpleFeatureListTab(String title, boolean showBinding, boolean defaultBindingState) {
    super(title, showBinding, defaultBindingState);
    parentGroup = new ParentFeatureListPaneGroup();
    // remove listeners after closing tab
    setOnCloseRequest(event -> parentGroup.disposeListeners());
  }

  public SimpleFeatureListTab(String title, Node content, boolean showBinding,
      boolean defaultBindingState) {
    this(title, showBinding, defaultBindingState);
    setContent(content);
  }

  public SimpleFeatureListTab(String title, Node content) {
    this(title, content, false, false);
  }

  public SimpleFeatureListTab(String title) {
    this(title, false, false);
  }

  @Override
  public void onFeatureListSelectionChanged(final Collection<? extends FeatureList> flists) {
    super.onFeatureListSelectionChanged(flists);
    if (flists.isEmpty()) {
      parentGroup.featureListProperty().setValue(null);
      return;
    }

    FeatureList featureList = flists.iterator().next();
    parentGroup.featureListProperty().setValue(featureList);
  }

  public ParentFeatureListPaneGroup getParentGroup() {
    return parentGroup;
  }
}
