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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxCachedViewController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableFilterMenu.FxFeatureTableFilterMenuModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxFeatureTableController extends FxCachedViewController<FxFeatureTableModel> {

  private final FxFeatureTableViewBuilder viewBuilder;
  private final FxFeatureTableInteractor interactor;

  public FxFeatureTableController() {
    // use a clone, will set parameters to module params after tab close
    var params = ConfigService.getConfiguration().getModuleParameters(FeatureTableFXModule.class)
        .cloneParameterSet();
    super(new FxFeatureTableModel(params));

    // interactor before view is built
    interactor = new FxFeatureTableInteractor(model);
    viewBuilder = new FxFeatureTableViewBuilder(model);
  }

  @Override
  protected @NotNull FxViewBuilder<FxFeatureTableModel> getViewBuilder() {
    return viewBuilder;
  }

  @Override
  public void close() {
    super.close();
    final FeatureTableFX table = model.getFeatureTable();
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
}
