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

import io.github.mzmine.javafx.mvci.FxViewBuilder;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/**
 * Combines {@link FeatureTableFX} with {@link FxFeatureTableFilterMenu}
 */
public class FxFeatureTableViewBuilder extends FxViewBuilder<FxFeatureTableModel> {

  protected FxFeatureTableViewBuilder(FxFeatureTableModel model) {
    super(model);
  }

  @Override
  public Region build() {
    // feature table is already built in model as there are many requirements from model to table
    final BorderPane main = new BorderPane(model.getFeatureTable());
    final FxFeatureTableFilterMenu filterMenu = new FxFeatureTableFilterMenu(model);
    main.setBottom(filterMenu);

    return main;
  }
}
