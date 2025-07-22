/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.statsdashboard;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import java.util.Collection;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class StatsDashboardTab extends SimpleTab {

  private final StatsDashboardController controller;

  public StatsDashboardTab() {
    super("Statistics dashboard", true, false);
    controller = new StatsDashboardController();
    final Region region = controller.buildView();
    setContent(region);
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return controller.selectedFeatureListsProperty().getValue();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return controller.selectedFeatureListsProperty().getValue();
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    super.onFeatureListSelectionChanged(featureLists);
    controller.selectedFeatureListsProperty().setValue(
        featureLists.stream().filter(FeatureList::isAligned).map(FeatureList.class::cast).toList());
  }

  public StatsDashboardController getController() {
    return controller;
  }
}
