/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class GcEiGapFillingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final IntegerParameter NUMBER_OF_TOP_SIGNALS = new IntegerParameter(
      "Number of top signals", """
      Number of signals of the pseudo spectra of a row (merged over all samples) that need to be found as co-eluting peaks
      in a sample before the row is gap-filled in that sample. Signals found in the pseudo spectra of most samples come first,
      then the most intense. The quantifier m/z of the row always needs to be found as well.""", 3, true, 1,
      null);

  public static final RTToleranceParameter SAMPLE_TO_SAMPLE_RT_TOLERANCE = new RTToleranceParameter(
      "Retention time tolerance (sample-to-sample)", """
      Maximum allowed difference between the row retention time and the apex of the quantifier peak in a sample with a gap.
      Usually the same tolerance as used for the alignment.""",
      new RTTolerance(0.1f, Unit.MINUTES));

  public static final RTToleranceParameter INTRA_SAMPLE_COELUTION_TOLERANCE = new RTToleranceParameter(
      "Co-elution tolerance (intra-sample)", """
      Maximum allowed difference between the apex of the quantifier peak and the apices of the peaks of the other top signals
      in the same sample. Usually the same tolerance as used for the GC-EI spectral deconvolution.""",
      new RTTolerance(0.04f, Unit.MINUTES));

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "gaps");

  public static final OriginalFeatureListHandlingParameter HANDLE_ORIGINAL = new OriginalFeatureListHandlingParameter(
      true);

  public GcEiGapFillingParameters() {
    super(new Parameter[]{FEATURE_LISTS, NUMBER_OF_TOP_SIGNALS, SAMPLE_TO_SAMPLE_RT_TOLERANCE,
            INTRA_SAMPLE_COELUTION_TOLERANCE, SUFFIX, HANDLE_ORIGINAL},
        "https://mzmine.github.io/mzmine_documentation/module_docs/gapfill_gc_ei/gc-ei-gap-filling.html");
  }

  /**
   * Creates a parameter set for programmatic use, e.g., in the processing wizard.
   */
  public static @NotNull GcEiGapFillingParameters create(
      @NotNull final FeatureListsSelection flists, final int numberOfTopSignals,
      @NotNull final RTTolerance sampleToSampleRtTolerance,
      @NotNull final RTTolerance intraSampleCoelutionTolerance,
      @NotNull final String suffix, @NotNull final OriginalFeatureListOption handleOriginal) {
    final GcEiGapFillingParameters params = (GcEiGapFillingParameters) new GcEiGapFillingParameters().cloneParameterSet();
    params.setParameter(FEATURE_LISTS, flists);
    params.setParameter(NUMBER_OF_TOP_SIGNALS, numberOfTopSignals);
    params.setParameter(SAMPLE_TO_SAMPLE_RT_TOLERANCE, sampleToSampleRtTolerance);
    params.setParameter(INTRA_SAMPLE_COELUTION_TOLERANCE, intraSampleCoelutionTolerance);
    params.setParameter(SUFFIX, suffix);
    params.setParameter(HANDLE_ORIGINAL, handleOriginal);
    return params;
  }
}
