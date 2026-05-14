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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import java.util.Collection;

/**
 * Main-window tab that hosts the lipid annotation quality control dashboard, delegating feature
 * list selection to the underlying {@link LipidAnnotationQCDashboardController}.
 */
public class LipidAnnotationQCDashboardTab extends SimpleTab {

  private final LipidAnnotationQCDashboardController controller;

  public LipidAnnotationQCDashboardTab() {
    super("Lipid annotation QC dashboard");
    controller = new LipidAnnotationQCDashboardController();
    setContent(controller.buildView());
    setOnCloseRequest(_ -> controller.dispose());
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    super.onFeatureListSelectionChanged(featureLists);
    controller.setFeatureList(featureLists.isEmpty() ? null
        : (ModularFeatureList) featureLists.stream().toList().getFirst());
  }
}

