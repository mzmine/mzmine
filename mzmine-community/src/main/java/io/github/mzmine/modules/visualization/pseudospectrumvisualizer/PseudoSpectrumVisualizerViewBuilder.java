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

import io.github.mzmine.javafx.components.factories.FxSplitPanes;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class PseudoSpectrumVisualizerViewBuilder extends
    FxViewBuilder<PseudoSpectrumVisualizerModel> {

  public PseudoSpectrumVisualizerViewBuilder(@NotNull PseudoSpectrumVisualizerModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final TICPlot ticPlot = new TICPlot();
    ticPlot.minHeight(200);
    SpectraVisualizerTab spectraVisualizerTab = new SpectraVisualizerTab(null);
    final SpectraPlot spectraPlot = spectraVisualizerTab.getSpectrumPlot();
    spectraPlot.minHeight(150);

    model.pseudoSpecProperty().subscribe((_, spec) -> {
      spectraVisualizerTab.loadRawData(spec);
//      spectraPlot.getXYPlot().getRenderer().setDefaultPaint(FxColorUtil.fxColorToAWT(color));
    });

//    TICVisualizerTab ticVisualizerTab = new TICVisualizerTab(new RawDataFile[]{rawDataFile},
//        TICPlotType.BASEPEAK, new ScanSelection(pseudoScan.getMSLevel()),
//        mzTolerance.getToleranceRange(selectedFeature.getMZ()), null, null, null);

    model.ticDatasetsProperty().subscribe((_, datasets) -> {
      ticPlot.applyWithNotifyChanges(false, () -> {
        ticPlot.setLegendVisible(false);
        if (datasets != null) {
          // single update
          ticPlot.removeAllDataSets(false);
          ticPlot.addDataSetAndRenderers(datasets);
        } else {
          ticPlot.removeAllDataSets(true); // update
        }
      });
    });

    return FxSplitPanes.newSplitPane(0.5, Orientation.VERTICAL, spectraPlot, ticPlot);
  }
}
