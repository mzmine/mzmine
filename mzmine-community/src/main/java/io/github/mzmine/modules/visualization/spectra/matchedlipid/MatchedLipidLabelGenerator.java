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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

public class MatchedLipidLabelGenerator implements XYItemLabelGenerator {

  public static final int POINTS_RESERVE_X = 100;
  private final Map<XYDataset, List<Pair<Double, Double>>> datasetToLabelsCoords;
  private final ChartViewer plot;
  private final List<LipidFragment> fragments;

  public MatchedLipidLabelGenerator(SpectraPlot plot, List<LipidFragment> fragments) {
    this.plot = plot;
    this.fragments = fragments;
    this.datasetToLabelsCoords = plot.getDatasetToLabelsCoords();
  }


  /**
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {
    String label = null;
    
    //create label
    if (plot.getCanvas().getWidth() >= 400 && plot.getCanvas().getHeight() >= 200) {
      if (dataset.getSeriesKey(1).equals("Matched Signals")) {
        if (fragments != null) {
          label = buildFragmentAnnotation(fragments.get(item), true);
        }
      } else if (dataset.getSeriesKey(1).equals("In-silico fragments")) {
        if (fragments != null) {
          label = buildFragmentAnnotation(fragments.get(item), false);
        }
      }
    }
    return label;
  }

  private String buildFragmentAnnotation(LipidFragment lipidFragment, boolean showAccuracy) {
    StringBuilder sb = new StringBuilder();
    if (lipidFragment.getLipidFragmentInformationLevelType()
        .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
      sb.append(lipidFragment.getLipidChainType().getName()).append(" ")
          .append(lipidFragment.getChainLength()).append(":")
          .append(lipidFragment.getNumberOfDBEs());

      //add info about oxygens
      if (lipidFragment.getNumberOfOxygens() != null && lipidFragment.getNumberOfOxygens() > 0) {
        sb.append(";").append(lipidFragment.getNumberOfOxygens()).append("O");
      }
      sb.append("\n");

      sb.append(lipidFragment.getIonFormula()).append("\n")
          .append(MZmineCore.getConfiguration().getMZFormat().format(lipidFragment.getMzExact()))
          .append("\n");
      // accuracy
      if (showAccuracy) {
        float ppm = (float) ((lipidFragment.getMzExact() - lipidFragment.getDataPoint().getMZ())
            / lipidFragment.getMzExact()) * 1000000;
        sb.append("Δ ").append(MZmineCore.getConfiguration().getPPMFormat().format(ppm))
            .append("ppm\n");
      }
    } else {
      sb.append(lipidFragment.getRuleType().toString()).append("\n")
          .append(lipidFragment.getIonFormula()).append("\n")
          .append(MZmineCore.getConfiguration().getMZFormat().format(lipidFragment.getMzExact()));
      // accuracy
      if (showAccuracy) {
        float ppm = (float) ((lipidFragment.getMzExact() - lipidFragment.getDataPoint().getMZ())
            / lipidFragment.getMzExact()) * 1000000;
        sb.append("Δ ").append(MZmineCore.getConfiguration().getPPMFormat().format(ppm))
            .append("ppm\n");
      }
    }
    return sb.toString();
  }
}
