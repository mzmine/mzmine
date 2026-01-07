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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidSpectrumPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidSpectrumChart extends BufferedChartNode {

  private final SpectraPlot spectraPlot = new SpectraPlot();

  public LipidSpectrumChart(@Nullable MatchedLipid match, AtomicDouble progress,
      RunOption runOption, boolean asBufferedImage, boolean showLegend) {
    super(true);
    if (match != null && match.getMatchedFragments() != null && !match.getMatchedFragments()
        .isEmpty()) {
      LipidSpectrumPlot spectrumPlot = new LipidSpectrumPlot(match, showLegend, runOption);

      if (asBufferedImage) {
        setChartCreateImage(spectrumPlot, GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH,
            GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
      } else {
        setChartCreateImage(spectrumPlot, GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH,
            GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
        showInteractiveChart();
      }
    }
  }

  public LipidSpectrumChart(@NotNull ModularFeatureListRow row, AtomicDouble progress,
      RunOption runOption, boolean asBufferedImage, boolean showLegend) {
    this(row.getLipidMatches().isEmpty() ? null : row.getLipidMatches().get(0), progress, runOption,
        asBufferedImage, showLegend);
  }

  public SpectraPlot getSpectraPlot() {
    return spectraPlot;
  }
}
