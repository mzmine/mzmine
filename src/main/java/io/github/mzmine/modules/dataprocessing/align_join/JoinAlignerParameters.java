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

package io.github.mzmine.modules.dataprocessing.align_join;

import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;

public class JoinAlignerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter peakListName = new StringParameter("Feature list name",
      "Feature list name", "Aligned feature list");

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

  public static final DoubleParameter MZWeight = new DoubleParameter("Weight for m/z",
      "Score for perfectly matching m/z values");

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

  public static final DoubleParameter RTWeight = new DoubleParameter("Weight for RT",
      "Score for perfectly matching RT values");

  public static final OptionalParameter<MobilityToleranceParameter> mobilityTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter("Mobility tolerance",
          "If checked, mobility of features will be compared for alignment. This parameter then specifies the tolerance range for matching mobility values"),
      false);

  public static final DoubleParameter mobilityWeight = new DoubleParameter("Mobility weight",
      "Score for perfectly matching mobility values. Only taken into account if \"Mobility tolerance\" is activated.",
      new DecimalFormat("0.000"), 1d);

  public static final BooleanParameter SameChargeRequired = new BooleanParameter(
      "Require same charge state", "If checked, only rows having same charge state can be aligned",
      false);

  public static final BooleanParameter SameIDRequired = new BooleanParameter("Require same ID",
      "If checked, only rows having same compound identities (or no identities) can be aligned",
      false);

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> compareIsotopePattern = new OptionalModuleParameter<>(
      "Compare isotope pattern",
      "If both peaks represent an isotope pattern, add isotope pattern score to match score",
      new IsotopePatternScoreParameters(), false);

  public static final OptionalModuleParameter<JoinAlignerSpectraSimilarityScoreParameters> compareSpectraSimilarity = new OptionalModuleParameter<>(
      "Compare spectra similarity", "Compare MS1 or MS2 spectra similarity",
      new JoinAlignerSpectraSimilarityScoreParameters(), false);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      false);

  public JoinAlignerParameters() {
    super(new Parameter[]{peakLists, peakListName, MZTolerance, MZWeight, RTTolerance, RTWeight,
            mobilityTolerance, mobilityWeight, SameChargeRequired, SameIDRequired,
            compareIsotopePattern, compareSpectraSimilarity, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/join_aligner/join_aligner.html");
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
