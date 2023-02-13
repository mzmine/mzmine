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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionTask.RatioType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.FeatureMeasurementType;
import org.jetbrains.annotations.NotNull;

public class FeatureListBlankSubtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter alignedPeakList = new FeatureListsParameter(
      "Aligned feature list", 1, 1);

  public static final RawDataFilesParameter blankRawDataFiles = new RawDataFilesParameter(
      "Blank/Control raw data files", 1, 100);

  public static final IntegerParameter minBlanks = new IntegerParameter(
      "Minimum # of detection in blanks",
      "Specifies in how many of the blank files a peak has to be detected.");

  public static final ComboParameter<FeatureMeasurementType> quantType = new ComboParameter<FeatureMeasurementType>(
      "Quantification", "Use either the features' height or area for the subtraction. ",
      FeatureMeasurementType.values(), FeatureMeasurementType.HEIGHT);

  public static final ComboParameter<RatioType> ratioType = new ComboParameter<RatioType>(
      "Ratio type",
      "Use either the maximum or the average blank value for calculating the blank-ratio. ",
      RatioType.values(), RatioType.MAXIMUM);

  public static final OptionalParameter<PercentParameter> foldChange = new OptionalParameter<>(
      new PercentParameter("Fold change increase",
          "Specifies a percentage of increase of the intensity of a feature. If the intensity in the list to be"
              + " filtered increases more than the given percentage to the blank, it will not be deleted from "
              + "the feature list.", 3.0, 1.0, 1E5));

  public static final BooleanParameter keepBackgroundFeatures = new BooleanParameter(
      "Keep background samples/features", """
      When this option is activated, the rational of the blank subtraction is changed slightly. Background
      features are not removed, but aligned features are sorted into those that have
      features distinguishable from the background and
      features indistinguishable from the background in all non-blank samples.
      Furthermore, if the option is activated, abundances for the blank samples will be retained.""",
      false);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix for the new feature list.", "subtracted");

  public static final BooleanParameter createDeleted = new BooleanParameter(
      "Create deleted feature list", """
      Indicates whether an additional feature list containing all non-used (deleted) background-features should be saved.
      All features removed by this step will then be saved to a new feature list with the suffix 'subtractedBackground'.""",
      false);

  public FeatureListBlankSubtractionParameters() {
    super(new Parameter[]{alignedPeakList, blankRawDataFiles, minBlanks, quantType, ratioType,
            foldChange, keepBackgroundFeatures, suffix, createDeleted},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_blanksubtraction/filter_blanksubtraction.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
