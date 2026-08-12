package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelectorModule;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy selection for picking a
 * {@link io.github.mzmine.datamodel.features.compoundlist.CompoundRow}'s representative
 * {@link io.github.mzmine.datamodel.features.FeatureListRow}. Each value points to a
 * {@link CompoundRepresentativeSelectorModule} that knows how to instantiate its
 * {@link io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector}.
 */
public enum CompoundRepresentativeSelectorOption implements
    ModuleOptionsEnum<CompoundRepresentativeSelectorModule> {

  PREFER_ANNOTATED("Prefer annotated highest row (default)", "prefer_annotated", """
      Prefer annotated rows > ion identities > unknowns and use maximum height row. (default option)""",
      AnnotatedFirstRepresentativeSelectorModule.class), //
  ION_RANKING("Ion type ranking",
      "ion_type_ranking", """
      Rank by commonly seen ion types:
      %s""".formatted(PreferredIonTypeRepresentativeSelector.TIER_ORDER.stream().map(IonPart::name)
      .collect(Collectors.joining(" >"))), PreferredIonTypeRepresentativeSelectorModule.class);

  private final String clearName;
  private final String stableId;
  @NotNull
  private final String description;
  private final Class<? extends CompoundRepresentativeSelectorModule> moduleClass;

  CompoundRepresentativeSelectorOption(@NotNull final String name, @NotNull final String stableId,
      @NotNull String description,
      @NotNull final Class<? extends CompoundRepresentativeSelectorModule> moduleClass) {
    this.clearName = name;
    this.stableId = stableId;
    this.description = description;
    this.moduleClass = moduleClass;
  }

  @Override
  public @NotNull Class<? extends CompoundRepresentativeSelectorModule> getModuleClass() {
    return moduleClass;
  }

  public @NotNull String getDescription() {
    return description;
  }
  public @NotNull String getFullDescription() {
    return clearName+": "+description;
  }

  @Override
  public @NotNull String getStableId() {
    return stableId;
  }

  @Override
  public String toString() {
    return clearName;
  }
}
