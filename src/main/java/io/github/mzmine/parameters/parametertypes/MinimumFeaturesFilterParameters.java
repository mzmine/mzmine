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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeIntParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;

import java.awt.Window;
import java.util.List;

public class MinimumFeaturesFilterParameters extends SimpleParameterSet {


  private final boolean isSub;
  // sample sets
  public static final OptionalParameter<ComboParameter<Object>> GROUPSPARAMETER =
      new OptionalParameter<ComboParameter<Object>>(new ComboParameter<Object>("Sample set",
          "Paremeter defining the sample set of each sample. (Set them in Project/Set sample parameters)",
          new Object[0]));

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height to recognize a feature (important to destinguis between real peaks and minor gap-filled).",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E5);

  //
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  // minimum of samples per group (with the feature detected or filled in (min height?)
  // ... showing RT<=tolerance and height>=minHeight
  public static final AbsoluteNRelativeIntParameter MIN_SAMPLES_GROUP =
      new AbsoluteNRelativeIntParameter("Min samples in group",
          "Minimum of samples per group (with the feature detected or filled in) matching the conditions (in RT-range).",
          0, 0, AbsoluteNRelativeInt.Mode.ROUND_DOWN);

  // minimum of samples per all (with the feature detected or filled in (min height?)
  // ... showing RT<=tolerance and height>=minHeight
  public static final AbsoluteNRelativeIntParameter MIN_SAMPLES_ALL =
      new AbsoluteNRelativeIntParameter("Min samples in all",
          "Minimum of samples per group (with the feature detected or filled in) matching the conditions (in RT-range).",
          1, 0, AbsoluteNRelativeInt.Mode.ROUND_DOWN, 1);


  public static final PercentParameter MIN_INTENSITY_OVERLAP = new PercentParameter(
      "Min %-intensity overlap",
      "The smaller feature has to overlap with at least X% of its intensity with the other feature",
      0.6);


  /**
   * do not accept estimated features
   */
  public static final BooleanParameter EXCLUDE_ESTIMATED = new BooleanParameter(
      "Exclude estimated features (gap-filled)",
      "Gap-filled features might have a limited and different shape than detected features", true);

  /**
   * 
   */
  public MinimumFeaturesFilterParameters() {
    this(false);
  }

  /**
   * Sub has no grouping parameter and no RTTolerance
   * 
   * @param isSub
   */
  public MinimumFeaturesFilterParameters(boolean isSub) {
    super(isSub
        ? new Parameter[] {MIN_SAMPLES_ALL, MIN_SAMPLES_GROUP, MIN_INTENSITY_OVERLAP,
            EXCLUDE_ESTIMATED}
        : new Parameter[] {GROUPSPARAMETER, RT_TOLERANCE, MIN_HEIGHT, MIN_SAMPLES_ALL,
            MIN_SAMPLES_GROUP, MIN_INTENSITY_OVERLAP, EXCLUDE_ESTIMATED});
    this.isSub = isSub;
  }

  /**
   * Creates the filter with groups
   * 
   * @param groupingParameter
   * @param rawDataFiles
   * @param project
   * 
   * @return
   */
  public MinimumFeatureFilter createFilterWithGroups(MZmineProject project,
                                                     List<RawDataFile> rawDataFiles, String groupingParameter, double minHeight) {
    AbsoluteNRelativeInt minFInSamples = this.getParameter(MIN_SAMPLES_ALL).getValue();
    AbsoluteNRelativeInt minFInGroups = this.getParameter(MIN_SAMPLES_GROUP).getValue();
    double minIPercOverlap = this.getParameter(MIN_INTENSITY_OVERLAP).getValue();
    boolean excludeEstimated = this.getParameter(EXCLUDE_ESTIMATED).getValue();
    return new MinimumFeatureFilter(project, rawDataFiles, groupingParameter, minFInSamples,
        minFInGroups, minHeight, minIPercOverlap, excludeEstimated);
  }

  /**
   * Creates the filter without groups
   * 
   * @return
   */
  public MinimumFeatureFilter createFilter() {
    AbsoluteNRelativeInt minFInSamples = this.getParameter(MIN_SAMPLES_ALL).getValue();
    AbsoluteNRelativeInt minFInGroups = this.getParameter(MIN_SAMPLES_GROUP).getValue();
    double minFeatureHeight = this.getParameter(MIN_HEIGHT).getValue();
    double minIPercOverlap = this.getParameter(MIN_INTENSITY_OVERLAP).getValue();
    boolean excludeEstimated = this.getParameter(EXCLUDE_ESTIMATED).getValue();
    return new MinimumFeatureFilter(minFInSamples, minFInGroups, minFeatureHeight, minIPercOverlap,
        excludeEstimated);
  }


  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    // Update the parameter choices
    if (isSub)
      return super.showSetupDialog(valueCheckRequired);
    else {
      assert Platform.isFxApplicationThread();

      try {
        OptionalParameter<ComboParameter<Object>> gParam = getParameter(GROUPSPARAMETER);
        if (gParam != null) {
          UserParameter<?, ?> newChoices[] =
                  MZmineCore.getProjectManager().getCurrentProject().getParameters();
          String[] choices;
          if (newChoices == null || newChoices.length == 0) {
            choices = new String[1];
            choices[0] = "No groups";
          } else {
            choices = new String[newChoices.length + 1];
            choices[0] = "No groups";
            for (int i = 0; i < newChoices.length; i++) {
              choices[i + 1] = newChoices[i].getName();
            }
          }
          gParam.getEmbeddedParameter().setChoices(choices);
          if (choices.length > 1)
            gParam.getEmbeddedParameter().setValue(choices[1]);
        }
      } catch (Exception e) {
      }

      ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);
      dialog.showAndWait();
      return dialog.getExitCode();
    }
  }

}
