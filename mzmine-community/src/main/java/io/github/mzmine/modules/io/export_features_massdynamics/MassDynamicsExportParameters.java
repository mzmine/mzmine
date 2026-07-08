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

package io.github.mzmine.modules.io.export_features_massdynamics;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class MassDynamicsExportParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

  public static final FileNameSuffixExportParameter filename = new FileNameSuffixExportParameter(
      "Filename",
      "Base filename for MassDynamics files: metabolite TSV file and experiment metadata file. Use '{}' in the file name to insert the feature list name.");

  public static final AbundanceMeasureParameter abundanceMeasure = new AbundanceMeasureParameter(
      "Abundance measure", "Feature abundance written to the MetaboliteIntensity column.",
      AbundanceMeasure.values(), AbundanceMeasure.Area);

  public MassDynamicsExportParameters() {
    super(featureLists, filename, abundanceMeasure);
  }

  @Override
  public boolean checkParameterValues(@NotNull final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    if (getValue(featureLists).getMatchingFeatureLists().length > 1 && !getValue(filename).getName()
        .contains(SiriusExportTask.MULTI_NAME_PATTERN)) {
      errorMessages.add(
          "Multiple feature lists (%d) were selected, but the file name does not contain the pattern %s.".formatted(
              getValue(featureLists).getMatchingFeatureLists().length,
              inQuotes(SiriusExportTask.MULTI_NAME_PATTERN)));
    }

    return superCheck && errorMessages.isEmpty();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
