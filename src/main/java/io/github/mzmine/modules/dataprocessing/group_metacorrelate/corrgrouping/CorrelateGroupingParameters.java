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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class CorrelateGroupingParameters extends SimpleParameterSet {

  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  // GROUPING
  // sample sets
  public static final OptionalParameter<ComboParameter<Object>> GROUPSPARAMETER =
      new OptionalParameter<ComboParameter<Object>>(new ComboParameter<Object>("Sample set",
          "Paremeter defining the sample set of each sample. (Set them in Project/Set sample parameters)",
          new Object[0]), false);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Used by min samples filter and MS annotations. Minimum height to recognize a feature (important to destinguis between real peaks and minor gap-filled).",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E5);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter NOISE_LEVEL =
      new DoubleParameter("Intensity correlation threshold",
          "This intensity threshold is used to filter data points before feature shape correlation",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  /**
   * Filter out by minimum number of features in all samples and/or in at least one sample group
   * features with height>=minHeight
   */
  public static final SubModuleParameter<MinimumFeaturesFilterParameters> MIN_SAMPLES_FILTER =
      new SubModuleParameter<>("Min samples filter",
          "Filter out by min number of features in all samples and in sample groups",
          new MinimumFeaturesFilterParameters(true));

  // Sub parameters of correlation grouping
  public static final OptionalModuleParameter<FeatureShapeCorrelationParameters> FSHAPE_CORRELATION =
      new OptionalModuleParameter<>("Correlation grouping",
          "Grouping based on Pearson correlation of the feature shapes.",
          new FeatureShapeCorrelationParameters(true), true);

  public static final OptionalModuleParameter<InterSampleHeightCorrParameters> IMAX_CORRELATION =
      new OptionalModuleParameter<>("Feature height correlation",
          "Feature to feature correlation of the maximum intensities across all samples.",
          new InterSampleHeightCorrParameters(true), true);

  public static final OptionalParameter<StringParameter> SUFFIX = new OptionalParameter<>(
      new StringParameter("Suffix (or auto)", "Select suffix or deselect for auto suffix"), false);

  // Constructor
  public CorrelateGroupingParameters() {
    super(new Parameter[]{PEAK_LISTS, RT_TOLERANCE,
        // Group and minimum samples filter
        GROUPSPARAMETER,
        // feature filter
        MIN_HEIGHT, NOISE_LEVEL, MIN_SAMPLES_FILTER,
        // feature shape correlation
        FSHAPE_CORRELATION,
        // intensity max correlation
        IMAX_CORRELATION,
        // suffix or auto suffix
        SUFFIX});
  }

  public CorrelateGroupingParameters(RTTolerance rtTol, boolean useGroups, String gParam,
      double minHeight, double noiseLevel, boolean autoSuffix, String suffix,
      MinimumFeaturesFilterParameters minFFilter, boolean useFShapeCorr, boolean useImaxCorr,
      FeatureShapeCorrelationParameters fShapeParam,
      InterSampleHeightCorrParameters heightCorrParam) {
    super(new Parameter[]{RT_TOLERANCE,
        // Group and minimum samples filter
        GROUPSPARAMETER,
        // feature filter
        MIN_HEIGHT, NOISE_LEVEL, MIN_SAMPLES_FILTER,
        // feature shape correlation
        FSHAPE_CORRELATION,
        // intensity max correlation
        IMAX_CORRELATION,
        // suffix or auto suffix
        SUFFIX});
    this.getParameter(RT_TOLERANCE).setValue(rtTol);
    this.getParameter(GROUPSPARAMETER).setValue(useGroups);
    this.getParameter(GROUPSPARAMETER).getEmbeddedParameter().setValue(gParam);
    this.getParameter(MIN_HEIGHT).setValue(minHeight);
    this.getParameter(NOISE_LEVEL).setValue(noiseLevel);
    this.getParameter(SUFFIX).setValue(autoSuffix);
    this.getParameter(SUFFIX).getEmbeddedParameter().setValue(suffix);
    this.getParameter(MIN_SAMPLES_FILTER).setEmbeddedParameters(minFFilter);
    this.getParameter(FSHAPE_CORRELATION).setValue(useFShapeCorr);
    this.getParameter(FSHAPE_CORRELATION).setEmbeddedParameters(fShapeParam);
    this.getParameter(IMAX_CORRELATION).setValue(useImaxCorr);
    this.getParameter(IMAX_CORRELATION).setEmbeddedParameters(heightCorrParam);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();
    // Update the parameter choices
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
    getParameter(CorrelateGroupingParameters.GROUPSPARAMETER).getEmbeddedParameter()
        .setChoices(choices);
    if (choices.length > 1) {
      getParameter(CorrelateGroupingParameters.GROUPSPARAMETER).getEmbeddedParameter()
          .setValue(choices[1]);
    }

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
