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

package io.github.mzmine.modules.dataprocessing.align_lcimage;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class LcImageAlignerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter("Feature lists",
      "Select at least two feature lists. The image feature list(s) are aligned to a single (pre-aligned) LC feature list.",
      2, Integer.MAX_VALUE);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "The file-to-file tolerance for two features.", 0.005, 10);

  public static final DoubleParameter mzWeight = new DoubleParameter("m/z weight",
      "Maximum score for a perfectly matching m/z", new DecimalFormat("0.0"), 1d);

  public static final OptionalParameter<MobilityToleranceParameter> mobTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter("Mobility tolerance",
          "The file-to-file mobility tolerance. If the files don't contain mobility information, this parameter will be ignored.",
          new MobilityTolerance(0.01f)), false);

  public static final DoubleParameter mobilityWeight = new DoubleParameter("Mobility weight",
      "Maximum score for a perfectly matching mobility", new DecimalFormat("0.0"), 1d);

  public static final StringParameter name = new StringParameter("Feature list name",
      "The name of the new feature list. Use {lc} for the name of the input (LC/DI) feature list.",
      "{lc} img");

  public LcImageAlignerParameters() {
    super(new Parameter[]{flists, mzTolerance, mzWeight, mobTolerance, mobilityWeight, name},
        "https://mzmine.github.io/mzmine_documentation/module_docs/align_lc-image/align_lc-image.html");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    final boolean superCheck = super.checkParameterValues(errorMessages);

    final ModularFeatureList[] matchingFeatureLists = getValue(
        LcImageAlignerParameters.flists).getMatchingFeatureLists();
    final var imageList = Arrays.stream(matchingFeatureLists)
        .filter(flist -> flist.getFeatureTypes().containsValue(new ImageType())).toList();
    final var baseLists = Arrays.stream(matchingFeatureLists)
        .filter(flist -> !flist.getFeatureTypes().containsValue(new ImageType())).toList();

    if (imageList.isEmpty()) {
      errorMessages.add("No feature list with images selected.");
    }
    if (baseLists.size() != 1) {
      errorMessages.add("Select a single LC/DI feature list (may be aligned).");
    }

    return superCheck && errorMessages.isEmpty();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
