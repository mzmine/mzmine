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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class VanKrevelenToolTipGenerator implements XYToolTipGenerator {

  private final String xAxisLabel;
  private final String yAxisLabel;
  private final String colorScaleLabel;
  private final String bubbleScaleLabel;
  private final NumberFormat ratioFormat = new DecimalFormat("0.###");

  public VanKrevelenToolTipGenerator(final String xAxisLabel, final String yAxisLabel,
      final String colorScaleLabel, final String bubbleScaleLabel) {
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.colorScaleLabel = colorScaleLabel;
    this.bubbleScaleLabel = bubbleScaleLabel;
  }

  @Override
  public String generateToolTip(final XYDataset dataset, final int series, final int item) {
    if (dataset instanceof VanKrevelenDiagramXYZDataset vkDataset) {
      final FeatureListRow row = vkDataset.getSelectedRow(item);

      final StringBuilder tooltip = new StringBuilder();
      tooltip.append("Row ID: ").append(row.getID());
      tooltip.append("\n").append(xAxisLabel).append(": ")
          .append(ratioFormat.format(vkDataset.getXValue(series, item)));
      tooltip.append("\n").append(yAxisLabel).append(": ")
          .append(ratioFormat.format(vkDataset.getYValue(series, item)));

      final VanKrevelenDiagramDataTypes colorType = vkDataset.getColorVanKrevelenDataType();
      if (colorScaleLabel != null && colorType != null) {
        tooltip.append("\n").append(colorScaleLabel).append(": ").append(
            identifyNumberFormat(colorType).format(vkDataset.getZValue(series, item)));
      }

      final VanKrevelenDiagramDataTypes bubbleType = vkDataset.getBubbleVanKrevelenDataType();
      if (bubbleScaleLabel != null && bubbleType != null) {
        tooltip.append("\n").append(bubbleScaleLabel).append(": ").append(
            identifyNumberFormat(bubbleType).format(vkDataset.getBubbleSizeValue(series, item)));
      }

      final String formula = vkDataset.getFormulaString(item);
      if (formula != null) {
        tooltip.append("\nFormula: ").append(formula);
      }

      final String preferredAnnotationName = row.getPreferredAnnotationName();
      if (preferredAnnotationName != null) {
        tooltip.append("\n").append(preferredAnnotationName);
      }

      return tooltip.toString();
    }

    // Fallback: still show basic coordinates.
    return xAxisLabel + ": " + ratioFormat.format(dataset.getXValue(series, item)) + "\n"
           + yAxisLabel + ": " + ratioFormat.format(dataset.getYValue(series, item));
  }

  private NumberFormat identifyNumberFormat(final VanKrevelenDiagramDataTypes dataType) {
    return switch (dataType) {
      case MZ -> MZmineCore.getConfiguration().getMZFormat();
      case RETENTION_TIME, TAILING_FACTOR, ASYMMETRY_FACTOR, FWHM ->
          MZmineCore.getConfiguration().getRTFormat();
      case MOBILITY -> MZmineCore.getConfiguration().getMobilityFormat();
      case INTENSITY, AREA -> MZmineCore.getConfiguration().getIntensityFormat();
    };
  }
}

