package io.github.mzmine.modules.dataprocessing.align_lcimage;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;

public class LcImageAlignerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter("Feature lists",
      "Select two feature lists. The image feature list and the (pre-aligned) LC feature list.", 2,
      2);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "The file-to-file tolerance for two features.", 0.005, 10);

  public static final DoubleParameter mzWeight = new DoubleParameter("m/z weight",
      "Maximum score for a perfectly matching m/z", new DecimalFormat("0.0"), 1d);

  public static final MobilityToleranceParameter mobTolerance = new MobilityToleranceParameter(
      "Mobility tolerance",
      "The file-to-file mobility tolerance. If the files don't contain mobility information, this parameter will be ignored.",
      new MobilityTolerance(0.01f));

  public static final DoubleParameter mobilityWeight = new DoubleParameter("Mobility weight",
      "Maximum score for a perfectly matching mobility", new DecimalFormat("0.0"), 1d);

  public static final StringParameter name = new StringParameter("Feature list name",
      "The name of the new feature list. Use {lc} or {img} for the name of the input feature list.",
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
        .filter(flist -> flist.getFeatureTypes().containsValue(new ImageType())).findAny()
        .orElse(null);
    final var baseList = Arrays.stream(matchingFeatureLists)
        .filter(flist -> !flist.getFeatureTypes().containsValue(new ImageType())).findAny()
        .orElse(null);

    if (imageList == null) {
      errorMessages.add("No feature list with images selected.");
    }
    if (baseList == null) {
      errorMessages.add("No LC feature list selected.");
    }

    return superCheck && errorMessages.isEmpty();
  }
}
