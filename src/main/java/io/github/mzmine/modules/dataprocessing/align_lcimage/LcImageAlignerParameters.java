/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
    super(new Parameter[]{flists, mzTolerance, mzWeight, mobTolerance, mobilityWeight, name});
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
