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

import java.awt.Window;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.minfeaturefilter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.modules.featurelistmethods.identification.ionidentity.ionidnetworking.IonNetworkingParameters;
import net.sf.mzmine.modules.featurelistmethods.identification.ionidentity.ionidnetworking.IonNetworkingParameters.Setup;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class SimpleMetaCorrelateParameters extends SimpleParameterSet {

  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MassListParameter MS2_MASSLISTS =
      new MassListParameter("Mass lists (MS2)", "MS2 mass lists for MS/MS annotation verification");

  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between peaks");

  // MZ-tolerance: MS2
  public static final MZToleranceParameter MZ_TOLERANCE_MS2 = new MZToleranceParameter(
      "m/z tolerance MS2", "Tolerance of MS1 to MS2 and within MS2 signals");


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
   * Filter by minimum height
   */
  public static final DoubleParameter NOISE_LEVEL_MS2 = new DoubleParameter("Noise level MS2",
      "Noise level of MS2, used for MS2 verification of multimers and in-source fragments (zero should be fine, as mass lists are already noise filtered)",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  /**
   * Filter out by minimum number of features in all samples and/or in at least one sample group
   * features with height>=minHeight
   */
  public static final SubModuleParameter MIN_SAMPLES_FILTER =
      new SubModuleParameter("Min samples filter",
          "Filter out by min number of features in all samples and in sample groups",
          new MinimumFeaturesFilterParameters(true));


  public static final OptionalParameter<PercentParameter> MIN_FSHAPE_CORR =
      new OptionalParameter<>(new PercentParameter("Group by min feature shape corr",
          "Group by feature shape correlation. Minimum average feature shape correlation (Pearson) (min 5 data points and 2 on each peak edge) ",
          0.85), true);

  public static final BooleanParameter FILTER_FEATURE_HEIGHT_CORR = new BooleanParameter(
      "Feature height corr filter",
      "Filters based on Pearson correlation of the intensity distribution of two rows across all samples. "
          + "Missing values are inserted acoording to the noise level. Performs significance test for the slope beeing positive and "
          + "different from zero ",
      true);

  public static final BooleanParameter MS2_SIMILARITY_CHECK = new BooleanParameter(
      "MS2 similarity check", "Checks MS2 similarity of all rows in a group ", false);


  // #####################################################################################
  // Intensity profile correlation
  // intra group comparison

  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final OptionalModuleParameter<IonNetworkingParameters> ADDUCT_LIBRARY =
      new OptionalModuleParameter<>("MS annotations",
          "Build adduct, in-source fragment, cluster,.. library and match all features",
          new IonNetworkingParameters(Setup.SIMPLE), true);

  public static final OptionalParameter<StringParameter> SUFFIX = new OptionalParameter<>(
      new StringParameter("Suffix (or auto)", "Select suffix or deselect for auto suffix"), false);


  // Constructor
  public SimpleMetaCorrelateParameters() {
    super(new Parameter[] {PEAK_LISTS, MS2_MASSLISTS, MZ_TOLERANCE, MZ_TOLERANCE_MS2, RT_TOLERANCE,
        // Group and minimum samples filter
        GROUPSPARAMETER,
        // feature filter
        MIN_HEIGHT, NOISE_LEVEL, NOISE_LEVEL_MS2, //
        MIN_SAMPLES_FILTER,
        // feature shape correlation
        MIN_FSHAPE_CORR,
        // height correlation
        FILTER_FEATURE_HEIGHT_CORR,
        // check for MS2 similarity
        MS2_SIMILARITY_CHECK,
        // adducts
        ADDUCT_LIBRARY,
        // suffix or auto suffix
        SUFFIX});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

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
    getParameter(SimpleMetaCorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
        .setChoices(choices);
    if (choices.length > 1)
      getParameter(SimpleMetaCorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
          .setValue(choices[1]);

    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
