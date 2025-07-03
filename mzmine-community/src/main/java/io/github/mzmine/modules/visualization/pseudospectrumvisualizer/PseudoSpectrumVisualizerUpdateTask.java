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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class PseudoSpectrumVisualizerUpdateTask extends
    FxUpdateTask<PseudoSpectrumVisualizerModel> {

  private final FeatureListRow row;
  private final List<DatasetAndRenderer> datasets = new ArrayList<>();
  private Scan scan;
  private RawDataFile rawFile;

  public PseudoSpectrumVisualizerUpdateTask(PseudoSpectrumVisualizerModel model) {
    super("pseudo spectrum update", model);

    row = model.getSelectedRow();
  }

  @Override
  protected void process() {
    if (row == null) {
      return;
    }
    rawFile = requireNonNullElse(model.getSelectedFile(), row.getBestFeature().getRawDataFile());

    final Feature feature = row.getFeature(rawFile);
    if (feature == null) {
      return;
    }

    scan = feature.getMostIntenseFragmentScan();
    if (scan == null) {
      return;
    }

    final MZTolerance mzTol = requireNonNullElse(model.getMzTolerance(),
        new MZTolerance(0.005, 15));

    final Color color = requireNonNullElse(model.getColor(), rawFile.getColor());
    final PseudoSpectrumFeatureDataSetCalculationTask task = new PseudoSpectrumFeatureDataSetCalculationTask(
        rawFile, scan, feature, mzTol, color);
    task.run();
    if (task.isFinished()) {
      datasets.addAll(task.getDatasets());
    }
  }

  @Override
  protected void updateGuiModel() {
    model.selectedFilesProperty().setValue(rawFile == null ? List.of() : List.of(rawFile));
    model.setTicDatasets(datasets);
    model.setPseudoSpec(scan);
  }

  @Override
  public String getTaskDescription() {
    return "Update pseudo spectrum plot";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }
}
