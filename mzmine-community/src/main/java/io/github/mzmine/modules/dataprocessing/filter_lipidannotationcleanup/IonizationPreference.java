/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single ionization preference rule in the lipid hierarchy. The user specifies at least a
 * {@link LipidCategories category}. Optionally also a {@link LipidMainClasses main class} and/or
 * {@link LipidClasses lipid class} for more specific targeting. The preferred ionization applies to
 * all classes matching the specified hierarchy scope. More specific rules override less specific
 * ones at runtime.
 */
public record IonizationPreference(@NotNull LipidCategories category,
                                   @Nullable LipidMainClasses mainClass,
                                   @Nullable LipidClasses lipidClass,
                                   @NotNull IonizationType ionizationType) {

  /**
   * Returns the most specific matching ionization type for the given lipid class, or null if no
   * preference matches.
   */
  public static @Nullable IonizationType resolve(final @NotNull ILipidClass target,
      final @NotNull List<IonizationPreference> preferences) {
    IonizationType categoryMatch = null;
    IonizationType mainClassMatch = null;
    for (final IonizationPreference pref : preferences) {
      if (pref.lipidClass() != null && pref.lipidClass() == target) {
        // exact match — most specific, return immediately
        return pref.ionizationType();
      }
      if (pref.lipidClass() == null && pref.mainClass() != null
          && pref.mainClass() == target.getMainClass()) {
        mainClassMatch = pref.ionizationType();
      }
      if (pref.lipidClass() == null && pref.mainClass() == null
          && pref.category() == target.getCoreClass()) {
        categoryMatch = pref.ionizationType();
      }
    }
    return mainClassMatch != null ? mainClassMatch : categoryMatch;
  }

  /**
   * Collects the {@link IonizationType} values that are available in <em>every</em> lipid class
   * within the given scope (intersection). This ensures the selected ion is applicable to all
   * classes the rule will match — a union would allow picking an ion that doesn't exist in some
   * sub-classes, causing that preference to silently have no effect for those classes.
   */
  public static @NotNull List<IonizationType> availableIonizations(
      final @NotNull LipidCategories category, final @Nullable LipidMainClasses mainClass,
      final @Nullable LipidClasses lipidClass) {
    if (lipidClass != null) {
      return extractIonizations(lipidClass);
    }
    final List<Set<IonizationType>> ionSetsPerClass = Arrays.stream(LipidClasses.values())
        .filter(lc -> lc.getCoreClass() == category)
        .filter(lc -> mainClass == null || lc.getMainClass() == mainClass).<Set<IonizationType>>map(
            lc -> Arrays.stream(lc.getFragmentationRules())
                .map(LipidFragmentationRule::getIonizationType)
                .collect(Collectors.toCollection(LinkedHashSet::new))).toList();
    if (ionSetsPerClass.isEmpty()) {
      return List.of();
    }
    // start with the first class's ions, then retain only those shared by all others
    final Set<IonizationType> intersection = new LinkedHashSet<>(ionSetsPerClass.getFirst());
    for (final Set<IonizationType> ions : ionSetsPerClass) {
      intersection.retainAll(ions);
    }
    return intersection.stream().sorted(Comparator.comparing(IonizationType::toString)).toList();
  }

  private static @NotNull List<IonizationType> extractIonizations(
      final @NotNull LipidClasses lipidClass) {
    return Arrays.stream(lipidClass.getFragmentationRules())
        .map(LipidFragmentationRule::getIonizationType).distinct()
        .sorted(Comparator.comparing(IonizationType::toString)).toList();
  }

  /**
   * Returns a display label for the scope, e.g. "GP", "GP > Phosphatidylcholine", or "GP >
   * Phosphatidylcholine > PC".
   */
  public @NotNull String scopeLabel() {
    final StringBuilder sb = new StringBuilder(category.getAbbreviation());
    if (mainClass != null) {
      sb.append(" > ").append(mainClass.getName());
    }
    if (lipidClass != null) {
      sb.append(" > ").append(lipidClass.getAbbr());
    }
    return sb.toString();
  }

  /**
   * Returns true if this preference targets the same hierarchy scope as {@code other}.
   */
  public boolean sameScope(final @NotNull IonizationPreference other) {
    return category == other.category && mainClass == other.mainClass
        && lipidClass == other.lipidClass;
  }
}
