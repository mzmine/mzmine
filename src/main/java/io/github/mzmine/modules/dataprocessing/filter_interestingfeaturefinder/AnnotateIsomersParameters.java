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

package io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class AnnotateIsomersParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final PercentParameter maxMobilityChange = new PercentParameter(
      "Maximum mobility change",
      "Specifies the maximum change of mobility for a possible isomer.\nUsed to rule out fragmented multimers.",
      0.2);

  public static final OptionalParameter<MobilityToleranceParameter> multimerRecognitionTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter("Skip multimer fragments",
          "If checked, the results will be refined to not falsely annotate fragments of\n"
              + "multimers as isomeric compounds. Requires prior use of the Ion identity networking\n"
              + "module. The given tolerance will be applied between a possible multimer and the\n"
              + "multimer fragment."));

  public static final ParameterSetParameter qualityParam = new ParameterSetParameter(
      "Quality parameters", "Used to refine the results and filter out noise.",
      new IsomerQualityParameters());

  public AnnotateIsomersParameters() {
    super(new Parameter[]{featureLists, mzTolerance, rtTolerance, maxMobilityChange,
        multimerRecognitionTolerance, qualityParam});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
