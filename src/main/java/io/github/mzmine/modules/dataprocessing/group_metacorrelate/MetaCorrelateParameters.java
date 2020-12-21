/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.*;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;

public class MetaCorrelateParameters extends SimpleParameterSet {

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
      new DoubleParameter("Noise level", "Noise level of MS1, used by feature shape correlation",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  /**
   * Filter out by minimum number of features in all samples and/or in at least one sample group
   * features with height>=minHeight
   */
  public static final SubModuleParameter MIN_SAMPLES_FILTER =
      new SubModuleParameter("Min samples filter",
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



  // #####################################################################################
  // Intensity profile correlation
  // intra group comparison

  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final OptionalModuleParameter<IonNetworkingParameters> ADDUCT_LIBRARY =
      new OptionalModuleParameter<>("MS annotations",
          "Build adduct, in-source fragment, cluster,.. library and match all features",
          new IonNetworkingParameters(Setup.SUB), true);

  public static final BooleanParameter ANNOTATE_ONLY_GROUPED =
      new BooleanParameter("Annotate only corr grouped",
          "Only rows in a correlation group are checked for annotations", true);

  public static final OptionalParameter<StringParameter> SUFFIX = new OptionalParameter<>(
      new StringParameter("Suffix (or auto)", "Select suffix or deselect for auto suffix"), false);

  public static final OptionalModuleParameter<MS2SimilarityParameters> MS2_SIMILARITY =
      new OptionalModuleParameter<>("MS2 similarity check",
          "MS2 similarity check on all rows of the same group", new MS2SimilarityParameters(true),
          false);

  // Constructor
  public MetaCorrelateParameters() {
    super(new Parameter[] {PEAK_LISTS, RT_TOLERANCE,
        // Group and minimum samples filter
        GROUPSPARAMETER,
        // feature filter
        MIN_HEIGHT, NOISE_LEVEL, MIN_SAMPLES_FILTER,
        // feature shape correlation
        FSHAPE_CORRELATION,
        // intensity max correlation
        IMAX_CORRELATION,
        // MS2 similarity
        MS2_SIMILARITY,
        // adducts
        ADDUCT_LIBRARY, ANNOTATE_ONLY_GROUPED,
        // suffix or auto suffix
        SUFFIX});
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
    getParameter(MetaCorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
        .setChoices(choices);
    if (choices.length > 1)
      getParameter(MetaCorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
          .setValue(choices[1]);

    if ((parameters == null) || (parameters.length == 0))
      return ExitCode.OK;
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);

    // enable
    CheckBox com = dialog.getComponentForParameter(ANNOTATE_ONLY_GROUPED);
    OptionalModuleComponent adducts = dialog.getComponentForParameter(ADDUCT_LIBRARY);
    adducts.getCheckbox().setOnAction(e -> com.setEnabled(adducts.isSelected()));

    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
