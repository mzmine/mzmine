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
    super(FEATURE_LISTS, INTENSITY_REPRESENTATION);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
