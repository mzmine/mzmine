package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.Parameter;
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
      .map(CompoundRepresentativeSelectorOption::getFullDescription).collect(Collectors.joining("\n"))),
      CompoundRepresentativeSelectorOption.PREFER_ANNOTATED);

  public CompoundGrouperSubParameters() {
    super(COMPONENTIZER, REPRESENTATIVE_SELECTOR);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  public CompoundGrouperParameters toFullParameters(
      List<? extends ModularFeatureList> featureLists) {
    final CompoundGrouperParameters params = (CompoundGrouperParameters) new CompoundGrouperParameters().cloneParameterSet();
    params.setParameter(CompoundGrouperParameters.FEATURE_LISTS,
        new FeatureListsSelection(featureLists));

    if (params.getParameters().length != this.getParameters().length + 1) {
      throw new IllegalArgumentException(
          "Mismatch in parameter count between sub and full parameters. Expected only 1 more with feature lists parameter");
    }

    for (Parameter<?> parameter : getParameters()) {
      final Parameter targetParam = params.getParameter(parameter);
      if (targetParam == null) {
        throw new IllegalArgumentException(
            "Missing required parameter in full params: " + parameter.getName());
      }
      params.setParameter(targetParam, parameter.getValue());
    }
    return params;
  }
}
