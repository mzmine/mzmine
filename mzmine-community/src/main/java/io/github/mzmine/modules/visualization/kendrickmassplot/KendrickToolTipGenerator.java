/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ToolTipTextProvider;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class KendrickToolTipGenerator implements XYToolTipGenerator {

  private final String xAxisLabel;
  private final String yAxisLabel;
  private final String colorScaleLabel;
  private final String bubbleScaleLabel;

  public KendrickToolTipGenerator(String xAxisLabel, String yAxisLabel, String colorScaleLabel,
      String bubbleScaleLabel) {
    super();

    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.colorScaleLabel = colorScaleLabel;
    this.bubbleScaleLabel = bubbleScaleLabel;
  }

  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {
    if (dataset instanceof KendrickMassPlotXYZDataset kendrickDataset) {
      FeatureListRow selectedRow = kendrickDataset.getSelectedRow(item);
      StringBuilder tooltip = new StringBuilder();

      // Format m/z and retention time values
      Integer id = selectedRow.getID();
      tooltip.append("Feature list ID: ").append(id);
      final KendrickPlotDataTypes xDataType = kendrickDataset.getxKendrickDataType();
      final KendrickPlotDataTypes yDataType = kendrickDataset.getyKendrickDataType();
      tooltip.append("\n").append(xAxisLabel).append(": ")
          .append(identifyNumberFormat(xDataType).format(kendrickDataset.getXValue(series, item)));
      tooltip.append("\n").append(yAxisLabel).append(": ")
          .append(identifyNumberFormat(yDataType).format(kendrickDataset.getYValue(series, item)));
      tooltip.append("\n").append(colorScaleLabel).append(": ").append(
          identifyNumberFormat(kendrickDataset.getColorKendrickDataType()).format(
              kendrickDataset.getZValue(series, item)));
      tooltip.append("\n").append(bubbleScaleLabel).append(": ").append(
          identifyNumberFormat(kendrickDataset.getBubbleKendrickDataType()).format(
              kendrickDataset.getBubbleSizeValue(series, item)));
      if (!(xDataType == KendrickPlotDataTypes.MZ || yDataType == KendrickPlotDataTypes.MZ)) {
        tooltip.append("\nm/z: ")
            .append(ConfigService.getGuiFormats().mz(selectedRow.getAverageMZ()));
      }

      // Add annotation information if available
      String preferredAnnotationName = selectedRow.getPreferredAnnotationName();
      if (preferredAnnotationName != null) {
        tooltip.append("\n").append(preferredAnnotationName);
      }

      return tooltip.toString();
    }
    return ((ToolTipTextProvider) dataset).getToolTipText(item);
  }

  private NumberFormat identifyNumberFormat(KendrickPlotDataTypes bubbleKendrickDataType) {
    switch (bubbleKendrickDataType) {
      case MZ, KENDRICK_MASS -> {
        return MZmineCore.getConfiguration().getMZFormat();
      }
      case KENDRICK_MASS_DEFECT, REMAINDER_OF_KENDRICK_MASS, RETENTION_TIME, TAILING_FACTOR,
           ASYMMETRY_FACTOR, FWHM -> {
        return MZmineCore.getConfiguration().getRTFormat();
      }
      case MOBILITY -> {
        return MZmineCore.getConfiguration().getMobilityFormat();
      }
      case INTENSITY, AREA -> {
        return MZmineCore.getConfiguration().getIntensityFormat();
      }
    }
    return MZmineCore.getConfiguration().getRTFormat();
  }

}
