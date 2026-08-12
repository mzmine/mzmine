package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class CompoundGrouperSubParameters extends SimpleParameterSet {


  public static final ModuleOptionsEnumComboParameter<CompoundComponentizerType> COMPONENTIZER = new ModuleOptionsEnumComboParameter<>(
      "Componentizer",
      "Strategy for grouping FeatureListRows into CompoundRows. Each strategy exposes its own "
          + "parameters (tolerances, density thresholds, etc.).",
      CompoundComponentizerType.SimpleSeeder);

  public static final ModuleOptionsEnumComboParameter<CompoundRepresentativeSelectorOption> REPRESENTATIVE_SELECTOR = new ModuleOptionsEnumComboParameter<>(
      "Representative row", """
      Strategy for picking the representative row of each compound row.
      %s""".formatted(Arrays.stream(CompoundRepresentativeSelectorOption.values())
      .map(CompoundRepresentativeSelectorOption::getFullDescription)
      .collect(Collectors.joining("\n"))), CompoundRepresentativeSelectorOption.PREFER_ANNOTATED);

  public CompoundGrouperSubParameters() {
    super(COMPONENTIZER, REPRESENTATIVE_SELECTOR);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  /**
   * Set all parameter values explicitly on {@code param}.
   */
  public static void setAll(@NotNull final ParameterSet param,
      @NotNull final CompoundComponentizerType componentizer,
      @NotNull final ParameterSet componentizerParameters,
      @NotNull final CompoundRepresentativeSelectorOption representativeSelector) {
    param.getParameter(COMPONENTIZER).setValue(componentizer, componentizerParameters);
    param.getParameter(REPRESENTATIVE_SELECTOR).setValue(representativeSelector);
  }

  public @NotNull CompoundGrouperParameters toFullParameters(
      @NotNull final List<? extends ModularFeatureList> featureLists) {
    final CompoundGrouperParameters params = (CompoundGrouperParameters) new CompoundGrouperParameters().cloneParameterSet();
    final ParameterSet componentizerParameters = getParameter(COMPONENTIZER).getEmbeddedParameters()
        .cloneParameterSet();
    CompoundGrouperParameters.setAll(params, new FeatureListsSelection(featureLists),
        getValue(COMPONENTIZER), componentizerParameters, getValue(REPRESENTATIVE_SELECTOR));
    return params;
  }
}
