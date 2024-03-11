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
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsController;
import io.github.mzmine.gui.framework.fx.SelectedRowsController;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.LastUpdateProperty;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class PCAController extends FxController<PCAModel> implements SelectedRowsController,
    SelectedFeatureListsController {

  private final PauseTransition updateAccumulator = new PauseTransition(Duration.millis(50));
  private final FxViewBuilder<PCAModel> builder;

  // define last so that other properties can be bound
  private final LastUpdateProperty lastFullUpdateTrigger;

  public PCAController() {
    super(new PCAModel());
    builder = new PCAViewBuilder(model);
    //update on changes of these properties
    lastFullUpdateTrigger = new LastUpdateProperty(model.flistsProperty(), model.domainPcProperty(),
        model.rangePcProperty(), model.abundanceProperty(), model.metadataColumnProperty());

    updateAccumulator.setOnFinished(_ -> updateNow());
    lastFullUpdateTrigger.addListener((_, _, _) -> this.waitAndUpdate());
    updateNow(); // TODO could we call wait and update? or just let the flist be set and then update?
  }

  @Override
  protected @NotNull FxViewBuilder<PCAModel> getViewBuilder() {
    return builder;
  }

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  /**
   * Accumulates update calls by waiting for some time
   */
  public void waitAndUpdate() {
    updateAccumulator.playFromStart(); // accumulate update calls and then update
  }

  private void updateNow() {
    onTaskThread(new PCAUpdateTask("update full dataset", model));
  }

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.flistsProperty();
  }
}
