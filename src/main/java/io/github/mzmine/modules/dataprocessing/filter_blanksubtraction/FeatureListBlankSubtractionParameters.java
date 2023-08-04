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

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionTask.BlankSubtractionOptions;
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
import org.jetbrains.annotations.NotNull;

public class FeatureListBlankSubtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter alignedPeakList = new FeatureListsParameter(
      "Aligned feature list", 1, 1);

  public static final RawDataFilesParameter blankRawDataFiles = new RawDataFilesParameter(
      "Blank/Control raw data files", 1, Integer.MAX_VALUE  );

  public static final IntegerParameter minBlanks = new IntegerParameter(
      "Minimum # of detection in blanks",
      "Specifies in how many of the blank files a peak has to be detected.");

  public static final ComboParameter<AbundanceMeasure> quantType = new ComboParameter<AbundanceMeasure>(
      "Quantification", "Use either the features' height or area for the subtraction. ",
      AbundanceMeasure.values(), AbundanceMeasure.Height);

  public static final ComboParameter<RatioType> ratioType = new ComboParameter<RatioType>(
      "Ratio type",
      "Use either the maximum or the average blank value for calculating the blank-ratio. ",
      RatioType.values(), RatioType.MAXIMUM);

  public static final OptionalParameter<PercentParameter> foldChange = new OptionalParameter<>(
      new PercentParameter("Fold change increase",
          "Specifies a percentage of increase of the intensity of a feature. If the intensity in the list to be"
              + " filtered increases more than the given percentage to the blank, it will not be deleted from "
              + "the feature list.", 3.0, 1.0, 1E5));

  public static final ComboParameter<BlankSubtractionOptions> keepBackgroundFeatures = new ComboParameter<BlankSubtractionOptions>(
      "Keep or remove features (of rows) below fold change", """
      For parameter optimization it might be of help to know which features were classified as background in
      the samples and which features are more abundant than the background. This option allows doing that.
      Option REMOVE (default): all features below the set fold-change are removed from the rows as are rows
      that only contain background features.
      Option KEEP: any feature that is indistinguishable from the background (i.e, below the required fold-change)
      is kept, if any feature of a particular row is more abundant than the background. Any rows with only features
      representing the background will still be completely removed though.
      The option KEEP is only meant to be used for optimizing the fold-change and other parameters of this module
      or if the background is of interest in subsequent data processing steps (i.e., statistical analysis).
      """, BlankSubtractionOptions.values(), BlankSubtractionOptions.REMOVE);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix for the new feature list.", "subtracted");

  public static final BooleanParameter createDeleted = new BooleanParameter(
      "Create secondary list of subtracted features", """
      Indicates whether an additional feature list containing all non-used (deleted) background-features should be saved.
      All features removed by this step will then be saved to a new feature list with the suffix 'subtractedBackground'.""",
      false);

  public FeatureListBlankSubtractionParameters() {
    super(new Parameter[]{alignedPeakList, blankRawDataFiles, minBlanks, quantType, ratioType,
            foldChange, keepBackgroundFeatures, createDeleted, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_blanksubtraction/filter_blanksubtraction.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
