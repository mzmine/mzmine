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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelectorModule;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
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
  ION_RANKING("Ion type ranking", "ion_type_ranking", """
      Rank by the user definable ion type ranking of the feature list preferences 
      (default: %s), ties broken by annotation quality (AQS) and then by maximum height.""".formatted(
      IonTypeRanking.createDefault().toShortSummaryString()),
      PreferredIonTypeRepresentativeSelectorModule.class);

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
