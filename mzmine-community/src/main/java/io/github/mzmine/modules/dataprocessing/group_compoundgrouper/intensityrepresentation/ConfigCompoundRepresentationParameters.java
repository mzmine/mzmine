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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation;

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

public class ConfigCompoundRepresentationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final ComboParameter<CompoundIntensityRepresentation> INTENSITY_REPRESENTATION = new ComboParameter<>(
      "Intensity representation", """
      How each compound row's per-raw-file intensity is computed from its member rows.

      Representative row: Use the preferred (representative) row's feature directly. No new \
      compound features are created. (Default — same behavior as before this module existed.)

      Sum of all members: For every raw file, create a synthesized compound feature whose area, \
      height, normalized area and normalized height are the sums across all member rows' features.

      Sum of members with ion identity: As above, but only contributions from member rows that \
      carry an Ion Identity (i.e. real adducts of the same neutral) are included.""",
      CompoundIntensityRepresentation.values(), CompoundIntensityRepresentation.REPRESENTATIVE);

  public ConfigCompoundRepresentationParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/group_compound_grouping/config_compound_representations.html",
        FEATURE_LISTS, INTENSITY_REPRESENTATION);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
