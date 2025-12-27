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

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidSpectrumPlot;

public class LipidSpectrumCell extends ChartCell<LipidSpectrumPlot> {

  public LipidSpectrumCell(int id) {
    super(id);

  }

  @Override
  protected void updateItem(Object o, boolean b) {
    super.updateItem(o, b);

    if (!isValidCell()) {
      return;
    }
    if (cellHasNoData()) {
      getChart().setVisible(false);
      return;
    }

    final ModularFeatureListRow row = getTableRow().getItem();
    if (row == null || row.getLipidMatches().isEmpty()) {
      getChart().clearPlot();
      getChart().setVisible(false);
      return;
    }
    getChart().setVisible(true);

    getChart().updateLipidSpectrum(row.getLipidMatches().getFirst(), true, RunOption.THIS_THREAD);
  }

  @Override
  protected int getMinCellHeight() {
    return GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT;
  }

  @Override
  protected double getMinCellWidth() {
    return GraphicalColumType.DEFAULT_GRAPHICAL_CELL_WIDTH;
  }

  @Override
  protected LipidSpectrumPlot createChart() {
    return new LipidSpectrumPlot(null, true, RunOption.THIS_THREAD);
  }
}
