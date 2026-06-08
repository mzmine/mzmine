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

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.project.ProjectService;
import java.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents axis selected in the scatter plot visualizer. This can be either a
 * RawDataFile, or a project parameter value representing several RawDataFiles. In the second case,
 * the average feature area is calculated.
 */
public class ScatterPlotAxisSelection {

  private @NotNull RawDataFile file;

  public ScatterPlotAxisSelection(@NotNull RawDataFile file) {
    this.file = file;
  }


  @Override
  public String toString() {
    return file.getName();
  }

  public double getValue(FeatureListRow row) {
    Feature feature = row.getFeature(file);
    if (feature == null) {
      return 0;
    } else {
      return feature.getArea();
    }
  }

  static ScatterPlotAxisSelection[] generateOptionsForFeatureList(FeatureList featureList) {

    Vector<ScatterPlotAxisSelection> options = new Vector<>();

    for (RawDataFile dataFile : featureList.getRawDataFiles()) {
      ScatterPlotAxisSelection newOption = new ScatterPlotAxisSelection(dataFile);
      options.add(newOption);
    }

    return options.toArray(new ScatterPlotAxisSelection[0]);
  }

}
