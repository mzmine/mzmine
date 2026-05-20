package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import org.jetbrains.annotations.NotNull;

public class CompoundGrouperParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final ModuleOptionsEnumComboParameter<CompoundComponentizerType> COMPONENTIZER = new ModuleOptionsEnumComboParameter<>(
      "Componentizer",
      "Strategy for grouping FeatureListRows into CompoundRows. Each strategy exposes its own "
          + "parameters (tolerances, density thresholds, etc.).",
      CompoundComponentizerType.SimpleSeeder);

  public static final ModuleOptionsEnumComboParameter<CompoundRepresentativeSelectorOption> REPRESENTATIVE_SELECTOR = new ModuleOptionsEnumComboParameter<>(
      "Representative row",
      "Strategy for picking the representative FeatureListRow of each CompoundRow. "
          + "'Annotated first' prefers (1) the highest-intensity row with a compound / spectral "
          + "library annotation, (2) then the highest-intensity row carrying an IonIdentity, "
          + "(3) and finally the highest-intensity row. 'Preferred IonType' favors clean "
          + "single-adduct forms in a tier order (M+H / M-H first), with an intensity tiebreak "
          + "and an ultimate fallback to the highest-intensity row.",
      CompoundRepresentativeSelectorOption.PREFER_ANNOTATED);

  public CompoundGrouperParameters() {
    super(FEATURE_LISTS, COMPONENTIZER, REPRESENTATIVE_SELECTOR);
  }

  /**
   * Set all parameter values explicitly on {@code param}.
   */
  public void setAll(@NotNull final FeatureListsSelection flists,
      @NotNull final CompoundComponentizerType componentizer,
      @NotNull final CompoundRepresentativeSelectorOption representativeSelector) {
    setParameter(FEATURE_LISTS, flists);
    getParameter(COMPONENTIZER).setValue(componentizer);
    getParameter(REPRESENTATIVE_SELECTOR).setValue(representativeSelector);
  }

  /**
   * Set the feature list selection and explicitly apply the default componentizer and
   * representative selector.
   */
  public void setAll(@NotNull final FeatureListsSelection flists) {
    // explicit defaults so batch wizard output is independent of parameter set defaults
    setAll(flists, CompoundComponentizerType.SimpleSeeder,
        CompoundRepresentativeSelectorOption.PREFER_ANNOTATED);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
