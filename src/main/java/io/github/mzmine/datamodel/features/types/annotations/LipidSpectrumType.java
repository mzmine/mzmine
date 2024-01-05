/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.annotations;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.MatchedLipidSpectrumTab;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidSpectrumType extends LinkedGraphicalType {

  private static final Logger logger = Logger.getLogger(LipidSpectrumType.class.getName());

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "lipid_matched_signals";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Matched Lipid Signals";
  }

  @Override
  public @Nullable Node createCellContent(@NotNull ModularFeatureListRow row, Boolean cellData,
      @Nullable RawDataFile raw, AtomicDouble progress) {
    List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
    if (matchedLipids == null || matchedLipids.isEmpty()) {
      return null;
    }
    var chart = new LipidSpectrumChart(matchedLipids.get(0), progress, RunOption.THIS_THREAD);
    return chart;
  }

  @Override
  public double getColumnWidth() {
    return LARGE_GRAPHICAL_CELL_WIDTH;
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, DataType<?> superType,
      @org.jetbrains.annotations.Nullable final Object value) {
    List<MatchedLipid> matchedLipids = row.get(LipidMatchListType.class);
    if (matchedLipids != null) {
      MatchedLipidSpectrumTab matchedLipidSpectrumTab = new MatchedLipidSpectrumTab(
          matchedLipids.get(0).getLipidAnnotation().getAnnotation() + " Matched Signals",
          new LipidSpectrumChart(row, null));
      return () -> MZmineCore.getDesktop().addTab(matchedLipidSpectrumTab);
    } else {
      return null;
    }
  }

}
