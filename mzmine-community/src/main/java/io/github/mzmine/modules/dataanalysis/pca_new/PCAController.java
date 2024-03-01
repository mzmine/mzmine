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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.framework.fx.mvci.FxController;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import io.github.mzmine.gui.framework.fx.mvci.SelectedRowsController;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;

public class PCAController extends FxController<PCAModel> implements SelectedRowsController {

  private final FxViewBuilder<PCAModel> builder;

  protected PCAController() {
    super(new PCAModel());
    builder = new PCAViewBuilder(model);
    initListeners();
  }

  @Override
  protected @NotNull FxViewBuilder<PCAModel> getViewBuilder() {
    return builder;
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  public ObjectProperty<List<FeatureList>> featureListsProperty() {
    return model.flistsProperty();
  }

  private void initListeners() {
    model.flistsProperty()
        .addListener(_ -> onTaskThread(new PCAUpdateTask("update full dataset", model)));
    model.domainPcProperty()
        .addListener(_ -> onTaskThread(new PCAUpdateTask("update full dataset", model)));
    model.rangePcProperty()
        .addListener(_ -> onTaskThread(new PCAUpdateTask("update full dataset", model)));
    model.abundanceProperty()
        .addListener(_ -> onTaskThread(new PCAUpdateTask("update full dataset", model)));
    model.flistsProperty()
        .addListener(_ -> onTaskThread(new PCAUpdateTask("update full dataset", model)));
  }
}