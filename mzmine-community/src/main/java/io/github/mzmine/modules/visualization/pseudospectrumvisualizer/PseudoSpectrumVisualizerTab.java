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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.javafx.WeakAdapter;
import javafx.scene.layout.Region;

public class PseudoSpectrumVisualizerTab extends SimpleTab {

  private final PseudoSpectrumVisualizerController controller;
  private final WeakAdapter weak;

  public PseudoSpectrumVisualizerTab() {
    this(null);
  }

  public PseudoSpectrumVisualizerTab(FeatureTableFX table) {
    super("Pseudo spectrum", true, true);
    controller = new PseudoSpectrumVisualizerController();
    final Region view = controller.buildView();
    super.setContent(view);

    // bind selected rows
    weak = new WeakAdapter();
    setOnCloseRequest(_ -> weak.dipose()); // dispose on shutdown

    FeatureListUtils.bindSelectedRows(weak, table, controller.selectedRowsProperty(),
        updateOnSelectionProperty());
    FeatureListUtils.bindSelectedRawDataFiles(weak, table, controller.selectedRawFilesProperty(),
        updateOnSelectionProperty());
  }

  public PseudoSpectrumVisualizerController getController() {
    return controller;
  }
}
