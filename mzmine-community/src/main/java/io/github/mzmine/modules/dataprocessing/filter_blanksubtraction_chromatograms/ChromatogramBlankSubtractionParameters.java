/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChromatogramBlankSubtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

  public static final MZToleranceParameter mzTol = new MZToleranceParameter(
      ToleranceType.SAMPLE_TO_SAMPLE);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix for the new feature list.", "-blanks");

  public ChromatogramBlankSubtractionParameters() {
    super(new Parameter[]{featureLists, mzTol, suffix, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_blanksubtraction/filter_chrom_blanksubtraction.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.UNSUPPORTED;
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    boolean preconditions = true;
    if (!skipRawDataAndFeatureListParameters) {
      ModularFeatureList[] flists = getValue(featureLists).getMatchingFeatureLists();
      if (flists != null && flists.length > 0) {
        String error = checkPreconditions(flists);
        if (error != null) {
          errorMessages.add(error);
          preconditions = false;
        }
      }
    }

    boolean otherConditions = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);
    return otherConditions && preconditions;
  }

  /**
   * @return error message or null
   */
  @Nullable
  public static String checkPreconditions(final FeatureList[] featureLists) {
    for (final FeatureList flist : featureLists) {
      // only one sample
      if (flist.getNumberOfRawDataFiles() != 1) {
        return "Can only run blank removal before alignment. Needs 1 raw data file per feature list. Apply blank removal after chromatogram builder and optionally smoothing.";
      }

      // cannot apply after resolving
      boolean isResolved = flist.getAppliedMethods().stream()
          .map(FeatureListAppliedMethod::getModule).filter(MZmineRunnableModule.class::isInstance)
          .map(MZmineRunnableModule.class::cast).map(MZmineRunnableModule::getModuleCategory)
          .anyMatch(Predicate.isEqual(MZmineModuleCategory.FEATURE_RESOLVING));
      if (isResolved) {
        return "Apply blank removal before resolving, after chromatogram builder and optionally smoothing.";
      }

      // scans
      List<? extends Scan> scans = flist.getSeletedScans(flist.getRawDataFile(0));
      if (scans == null) {
        String steps = flist.getAppliedMethods().stream().map(FeatureListAppliedMethod::getModule)
            .map(MZmineModule::getName).collect(Collectors.joining("; "));
        return
            "Feature list has no scans, please report to the mzmine team with full log file. This is the list of applied steps for this feature list:\n"
                + steps;
      }
    }
    return null;
  }

}
