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

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatchedLipidLabelGenerator implements XYItemLabelGenerator {

  public static final int POINTS_RESERVE_X = 100;
  private static final double LABEL_X_DISTANCE_FRACTION = 0.03;
  private static final double LABEL_Y_DISTANCE_FRACTION = 0.05;
  private final Map<XYDataset, List<Pair<Double, Double>>> datasetToLabelsCoords;
  private final SpectraPlot plot;
  private final List<LipidFragment> fragments;
  private final boolean matchedSignals;
  private final List<Double> matchedMzValues;

  public MatchedLipidLabelGenerator(SpectraPlot plot, List<LipidFragment> fragments) {
    this(plot, fragments, true);
  }

  public MatchedLipidLabelGenerator(SpectraPlot plot, List<LipidFragment> fragments,
      boolean matchedSignals) {
    this.plot = plot;
    this.fragments = fragments;
    this.matchedSignals = matchedSignals;
    this.datasetToLabelsCoords = plot.getDatasetToLabelsCoords();
    this.matchedMzValues = fragments == null ? List.of()
        : fragments.stream().map(f -> f.getDataPoint().getMZ()).toList();
  }


  /**
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {
    if (!matchedSignals && (plot.getCanvas().getWidth() < 400
        || plot.getCanvas().getHeight() < 200)) {
      return null;
    }

    if (matchedSignals) {
      if (fragments == null || item >= fragments.size()) {
        return null;
      }
      return buildFragmentAnnotation(fragments.get(item), true);
    }

    final double x = dataset.getXValue(series, item);
    final double y = dataset.getYValue(series, item);
    if (!canDrawLabel(dataset, series, item, x, y)) {
      return null;
    }
    if (isMatchedMz(x)) {
      return null;
    }
    return ConfigService.getConfiguration().getMZFormat().format(x);
  }

  private boolean canDrawLabel(XYDataset dataset, int series, int item, double originalX,
      double originalY) {
    final double xLength = plot.getXYPlot().getDomainAxis().getRange().getLength();
    final double yLength = plot.getXYPlot().getRangeAxis().getRange().getLength();
    if (xLength <= 0 || yLength <= 0 || plot.getWidth() <= 0) {
      return false;
    }

    final double pixelX = xLength / plot.getWidth();
    final int itemCount = dataset.getItemCount(series);
    final double limitLeft = originalX - ((POINTS_RESERVE_X / 2d) * pixelX);
    final double limitRight = originalX + ((POINTS_RESERVE_X / 2d) * pixelX);

    for (int i = 1; (item - i > 0) || (item + i < itemCount); i++) {
      if ((item - i > 0) && (dataset.getXValue(series, item - i) < limitLeft) && (
          (item + i >= itemCount) || (dataset.getXValue(series, item + i) > limitRight))) {
        break;
      }
      if ((item + i < itemCount) && (dataset.getXValue(series, item + i) > limitRight) && (
          (item - i <= 0) || (dataset.getXValue(series, item - i) < limitLeft))) {
        break;
      }
      if ((item - i > 0) && (originalY <= dataset.getYValue(series, item - i))) {
        return false;
      }
      if ((item + i < itemCount) && (originalY <= dataset.getYValue(series, item + i))) {
        return false;
      }
    }

    for (List<Pair<Double, Double>> coords : datasetToLabelsCoords.values()) {
      for (Pair<Double, Double> coord : coords) {
        if ((Math.abs(originalX - coord.getKey()) / xLength < LABEL_X_DISTANCE_FRACTION) && (
            Math.abs(originalY - coord.getValue()) / yLength < LABEL_Y_DISTANCE_FRACTION)) {
          return false;
        }
      }
    }

    datasetToLabelsCoords.computeIfAbsent(dataset, _ -> new ArrayList<>())
        .add(new Pair<>(originalX, originalY));
    return true;
  }

  private boolean isMatchedMz(double mz) {
    for (double matchedMz : matchedMzValues) {
      if (Math.abs(mz - matchedMz) <= 0.01d) {
        return true;
      }
    }
    return false;
  }

  private @NotNull String buildFragmentAnnotation(final @NotNull LipidFragment lipidFragment,
      final boolean showAccuracy) {
    final StringBuilder sb = new StringBuilder();
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
          .append(ConfigService.getConfiguration().getMZFormat().format(lipidFragment.getMzExact()))
          .append("\n");
      // accuracy
      if (showAccuracy) {
        float ppm = (float) ((lipidFragment.getMzExact() - lipidFragment.getDataPoint().getMZ())
            / lipidFragment.getMzExact()) * 1000000;
        sb.append("Δ ").append(ConfigService.getConfiguration().getPPMFormat().format(ppm))
            .append("ppm\n");
      }
    } else {
      sb.append(lipidFragment.getRuleType().toString()).append("\n")
          .append(lipidFragment.getIonFormula()).append("\n");
      final @Nullable String nlFormula = getHeadgroupNeutralLossFormula(lipidFragment);
      if (nlFormula != null) {
        sb.append("NL: ").append(nlFormula).append("\n");
      }
      sb.append(ConfigService.getConfiguration().getMZFormat().format(lipidFragment.getMzExact()));
      // accuracy
      if (showAccuracy) {
        float ppm = (float) ((lipidFragment.getMzExact() - lipidFragment.getDataPoint().getMZ())
            / lipidFragment.getMzExact()) * 1000000;
        sb.append("Δ ").append(ConfigService.getConfiguration().getPPMFormat().format(ppm))
            .append("ppm\n");
      }
    }
    return sb.toString();
  }

  private static @Nullable String getHeadgroupNeutralLossFormula(
      final @NotNull LipidFragment lipidFragment) {
    if (lipidFragment.getRuleType() != LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL) {
      return null;
    }

    final @Nullable String formula = lipidFragment.getOriginatingRuleFormula();
    return formula == null || formula.isBlank() ? null : formula;
  }
}
