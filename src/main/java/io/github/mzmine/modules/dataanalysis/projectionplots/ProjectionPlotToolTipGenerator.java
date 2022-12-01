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

import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import io.github.mzmine.parameters.ParameterSet;

public class ProjectionPlotToolTipGenerator implements XYZToolTipGenerator {

  private ColoringType coloringType;

  private enum LabelMode {
    FileName, FileNameAndParameterValue
  }

  private LabelMode labelMode;

  ProjectionPlotToolTipGenerator(ParameterSet parameters) {
    try {
      coloringType = parameters.getParameter(ProjectionPlotParameters.coloringType).getValue();
    } catch (IllegalArgumentException exeption) {
      coloringType = ColoringType.NOCOLORING;
    }
    if (coloringType.equals(ColoringType.NOCOLORING))
      labelMode = LabelMode.FileName;

    if (coloringType.equals(ColoringType.COLORBYFILE))
      labelMode = LabelMode.FileName;

    if (coloringType.isByParameter())
      labelMode = LabelMode.FileNameAndParameterValue;

  }

  private String generateToolTip(ProjectionPlotDataset dataset, int item) {

    switch (labelMode) {

      case FileName:
      default:
        return dataset.getRawDataFile(item);

      case FileNameAndParameterValue:
        String ret = dataset.getRawDataFile(item) + "\n";

        ret += coloringType.getParameter().getName() + ": ";

        int groupNumber = dataset.getGroupNumber(item);
        Object paramValue = dataset.getGroupParameterValue(groupNumber);
        if (paramValue != null)
          ret += paramValue.toString();
        else
          ret += "N/A";

        return ret;
    }

  }

  public String generateToolTip(XYDataset dataset, int series, int item) {
    if (dataset instanceof ProjectionPlotDataset)
      return generateToolTip((ProjectionPlotDataset) dataset, item);
    else
      return null;
  }

  public String generateToolTip(XYZDataset dataset, int series, int item) {
    if (dataset instanceof ProjectionPlotDataset)
      return generateToolTip((ProjectionPlotDataset) dataset, item);
    else
      return null;
  }
}
