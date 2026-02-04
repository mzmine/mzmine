/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionTask.BlankSubtractionOptions;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListBlankSubtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter alignedPeakList = new FeatureListsParameter(
      "Aligned feature list", 1, 1);

  public static final RawDataFilesParameter blankRawDataFiles = new RawDataFilesParameter(
      "Blank/Control raw data files", RawDataFilesSelection.createBlankByMetadata());

  public static final IntegerParameter minBlanks = new IntegerParameter(
      "Minimum # of detection in blanks",
      "Specifies in how many of the blank files a peak has to be detected.", 1, 1, null);

  public static final AbundanceMeasureParameter quantType = new AbundanceMeasureParameter();

  public static final ComboParameter<RatioType> ratioType = new ComboParameter<RatioType>(
      "Blank abundance ratio type",
      "Use either the maximum or the average blank value for calculating the blank-ratio. ",
      RatioType.values(), RatioType.MAXIMUM);

  public static final OptionalParameter<PercentParameter> foldChange = new OptionalParameter<>(
      new PercentParameter("Fold change increase", """
          Specifies a percentage of intensity increase of sample features over blank features.
          To retain a sample feature it needs to be X% higher than the blanks (maximum or average, see parameter).
          If off, the default 100% will be used to require sample features to be just >= blanks.""",
          3.0, 1.0, 1E5));

  /**
   * Now that there is an option to create another feature list with all removed rows this parameter
   * either removes the whole row or features individually.
   */
  public static final ComboParameter<BlankSubtractionOptions> subtractionOption = new ComboParameter<>(
      "Check abundance of", """
      Generally features are checked if they are distinguishable from the blanks by applying a minimum fold-change.
      
      Most abundant feature (default): Only checks if the highest sample feature >= blank features and either keeps \
      or removes all sample features as whole rows.
      This option may be better for statistical analysis.
      
      Each feature: Checks each sample feature individually for >= blank features removing each feature that fails. \
      So rows may loose some features while other higher features are retained.
      This option may distort statistical analysis but can help to quickly see which sample features are greater than the blanks.""",
      BlankSubtractionOptions.values(), BlankSubtractionOptions.MAXIMUM_FEATURE);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix for the new feature list.", "subtracted");

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      false);

  public static final BooleanParameter createDeleted = new BooleanParameter(
      "Create secondary list of subtracted features", """
      Mostly used during workflow optimization: Indicates whether an additional feature list containing all non-used (deleted) background-features should be saved.
      All features removed by this step will then be saved to a new feature list with the suffix 'subtractedBackground'.""",
      false);

  public FeatureListBlankSubtractionParameters() {
    super(new Parameter[]{alignedPeakList, blankRawDataFiles, minBlanks, quantType, ratioType,
            foldChange, subtractionOption, createDeleted, suffix, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_blanksubtraction/filter_blanksubtraction.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    final Map<String, Parameter<?>> map = super.getNameParameterMap();
    map.put("Quantification", getParameter(quantType));
    map.put("Ratio type", getParameter(ratioType));
    return map;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    if (!loadedParams.containsKey(subtractionOption.getName())) {
      // in version 2 subtraction option parameter changed the function completely
      // do not map the old value from version 1, just replace with default
      setParameter(subtractionOption, BlankSubtractionOptions.MAXIMUM_FEATURE);
    }
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 -> """
          Parameter "Keep or remove features (of rows) below fold change" was removed as this option is now better covered by creating a second list of removed features.
          Parameter %s was added to provide more control over the removed features (whole rows or each individual feature).""".formatted(
          subtractionOption);
      default -> null;
    };
  }
}
