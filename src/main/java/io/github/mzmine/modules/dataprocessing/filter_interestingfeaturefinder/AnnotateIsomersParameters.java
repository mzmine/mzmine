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
