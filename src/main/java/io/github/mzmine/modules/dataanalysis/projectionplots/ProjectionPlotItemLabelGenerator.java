/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import io.github.mzmine.parameters.ParameterSet;

public class ProjectionPlotItemLabelGenerator extends StandardXYItemLabelGenerator {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private enum LabelMode {
    None, FileName, ParameterValue
  }

  private LabelMode[] labelModes;
  private int labelModeIndex = 0;

  ProjectionPlotItemLabelGenerator(ParameterSet parameters) {

    labelModes = new LabelMode[] {LabelMode.None};
    ColoringType coloringType = ColoringType.NOCOLORING;
    try {
      coloringType = parameters.getParameter(ProjectionPlotParameters.coloringType).getValue();
    } catch (IllegalArgumentException exeption) {
    }
    if (coloringType.equals(ColoringType.NOCOLORING))
      labelModes = new LabelMode[] {LabelMode.None, LabelMode.FileName};

    if (coloringType.equals(ColoringType.COLORBYFILE))
      labelModes = new LabelMode[] {LabelMode.None, LabelMode.FileName};

    if (coloringType.isByParameter())
      labelModes = new LabelMode[] {LabelMode.None, LabelMode.FileName, LabelMode.ParameterValue};

  }

  protected void cycleLabelMode() {
    labelModeIndex++;

    if (labelModeIndex >= labelModes.length)
      labelModeIndex = 0;

  }

  public String generateLabel(ProjectionPlotDataset dataset, int series, int item) {

    switch (labelModes[labelModeIndex]) {
      case None:
      default:
        return "";

      case FileName:
        return dataset.getRawDataFile(item);

      case ParameterValue:
        int groupNumber = dataset.getGroupNumber(item);
        Object paramValue = dataset.getGroupParameterValue(groupNumber);
        if (paramValue != null)
          return paramValue.toString();
        else
          return "";

    }

  }

  public String generateLabel(XYDataset dataset, int series, int item) {
    if (dataset instanceof ProjectionPlotDataset)
      return generateLabel((ProjectionPlotDataset) dataset, series, item);
    else
      return null;
  }

  public String generateLabel(XYZDataset dataset, int series, int item) {
    if (dataset instanceof ProjectionPlotDataset)
      return generateLabel((ProjectionPlotDataset) dataset, series, item);
    else
      return null;
  }

}
