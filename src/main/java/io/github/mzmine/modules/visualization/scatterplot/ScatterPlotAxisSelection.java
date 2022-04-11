/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Vector;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;

/**
 * This class represents axis selected in the scatter plot visualizer. This can be either a
 * RawDataFile, or a project parameter value representing several RawDataFiles. In the second case,
 * the average feature area is calculated.
 *
 */
public class ScatterPlotAxisSelection {

  private RawDataFile file;
  private UserParameter<?, ?> parameter;
  private Object parameterValue;

  public ScatterPlotAxisSelection(RawDataFile file) {
    this.file = file;
  }

  public ScatterPlotAxisSelection(UserParameter<?, ?> parameter, Object parameterValue) {
    this.parameter = parameter;
    this.parameterValue = parameterValue;
  }

  @Override
  public String toString() {
    if (file != null)
      return file.getName();
    return parameter.getName() + ": " + parameterValue;
  }

  public double getValue(FeatureListRow row) {
    if (file != null) {
      Feature feature = row.getFeature(file);
      if (feature == null)
        return 0;
      else
        return feature.getArea();
    }

    double totalArea = 0;
    int numOfFiles = 0;
    for (RawDataFile dataFile : row.getRawDataFiles()) {
      Object fileValue =
          MZmineCore.getProjectManager().getCurrentProject().getParameterValue(parameter, dataFile);
      if (fileValue == null)
        continue;
      if (fileValue.toString().equals(parameterValue.toString())) {
        Feature feature = row.getFeature(dataFile);
        if ((feature != null) && (feature.getArea() > 0)) {
          totalArea += feature.getArea();
          numOfFiles++;
        }
      }
    }
    if (numOfFiles == 0)
      return 0;
    totalArea /= numOfFiles;
    return totalArea;

  }

  static ScatterPlotAxisSelection[] generateOptionsForFeatureList(FeatureList featureList) {

    Vector<ScatterPlotAxisSelection> options = new Vector<ScatterPlotAxisSelection>();

    for (RawDataFile dataFile : featureList.getRawDataFiles()) {
      ScatterPlotAxisSelection newOption = new ScatterPlotAxisSelection(dataFile);
      options.add(newOption);
    }

    for (UserParameter<?, ?> parameter : MZmineCore.getProjectManager().getCurrentProject()
        .getParameters()) {
      if (!(parameter instanceof ComboParameter))
        continue;

      var possibleValues = ((ComboParameter<?>) parameter).getChoices();
      for (Object value : possibleValues) {
        ScatterPlotAxisSelection newOption = new ScatterPlotAxisSelection(parameter, value);
        options.add(newOption);
      }
    }

    return options.toArray(new ScatterPlotAxisSelection[0]);

  }

}
